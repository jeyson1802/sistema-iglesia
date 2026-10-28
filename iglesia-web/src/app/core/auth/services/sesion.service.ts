import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, defer, finalize, firstValueFrom, map, Observable, of, shareReplay, switchMap, tap } from 'rxjs';
import { CambioContrasena } from '../models/cambio-contrasena';
import { Credenciales } from '../models/credenciales';
import { Perfil } from '../models/perfil';
import { Permiso } from '../models/permiso';
import { RUTA_INICIO_POR_ROL } from '../models/rol';
import { RUTAS_AUTH } from '../models/rutas-auth';
import { UsuarioActual } from '../models/usuario-actual';
import { AuthApiService } from './auth-api.service';

/** Clave de sessionStorage: solo guarda el id del rol del perfil activo, nunca tokens ni datos personales. */
const CLAVE_PERFIL = 'iglesia.perfilActivo';

/**
 * Fachada de la sesión. El token de acceso vive solo en memoria; el de refresco, en una cookie HttpOnly
 * que la UI no puede leer. El perfil activo limita la navegación (menú y guards usan solo sus permisos); el
 * backend autoriza con todos los permisos del token.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
    private readonly authApi = inject(AuthApiService);

    private readonly token = signal<string | null>(null);
    private readonly usuario = signal<UsuarioActual | null>(null);
    private readonly perfil = signal<Perfil | null>(null);

    /** Petición de refresco en curso, compartida para que varios 401 simultáneos hagan un solo `POST /refresh`. */
    private refrescoEnCurso: Observable<string> | null = null;

    readonly tokenAcceso = this.token.asReadonly();
    readonly usuarioActual = this.usuario.asReadonly();
    readonly perfilActivo = this.perfil.asReadonly();
    readonly autenticado = computed(() => this.token() !== null && this.usuario() !== null);
    readonly permisosActivos = computed<ReadonlySet<Permiso>>(() => new Set(this.perfil()?.permisos ?? []));
    readonly tieneVariosPerfiles = computed(() => (this.usuario()?.perfiles.length ?? 0) > 1);
    /** Con una contraseña temporal, el backend rechaza todo salvo consultar el usuario y cambiarla. */
    readonly debeCambiarContrasena = computed(() => this.usuario()?.debeCambiarContrasena ?? false);

    /** Con un solo perfil, queda seleccionado; con varios, el usuario debe elegirlo. */
    iniciarSesion(credenciales: Credenciales): Observable<void> {
        return this.authApi.login(credenciales).pipe(
            tap(({ tokenAcceso }) => this.token.set(tokenAcceso)),
            switchMap(() => this.cargarUsuario(null)),
            catchError((error: unknown) => {
                this.limpiar();
                throw error;
            })
        );
    }

    /** Recupera la sesión al recargar la página usando la cookie de refresco. Nunca falla: sin sesión, la deja vacía. */
    restaurar(): Promise<void> {
        return firstValueFrom(
            this.refrescar().pipe(
                switchMap(() => this.cargarUsuario(leerPerfilGuardado())),
                catchError(() => {
                    // Sin cookie válida no hay sesión que restaurar: es el caso normal de un visitante.
                    this.limpiar();
                    return of(undefined);
                })
            )
        );
    }

    /** Obtiene un token de acceso nuevo. Las llamadas simultáneas comparten la misma petición. */
    refrescar(): Observable<string> {
        this.refrescoEnCurso ??= this.authApi.refrescar().pipe(
            map(({ tokenAcceso }) => tokenAcceso),
            tap((tokenAcceso) => this.token.set(tokenAcceso)),
            finalize(() => (this.refrescoEnCurso = null)),
            shareReplay({ bufferSize: 1, refCount: false })
        );
        return this.refrescoEnCurso;
    }

    /** ¿El perfil activo tiene el permiso? Decide qué se muestra y a dónde se navega, no lo que autoriza el backend. */
    tienePermiso(permiso: Permiso): boolean {
        return this.permisosActivos().has(permiso);
    }

    seleccionarPerfil(id: number): void {
        const perfil = buscarPerfil(this.usuario(), id);
        if (!perfil) {
            throw new Error(`El usuario no tiene el perfil ${id}`);
        }
        this.establecerPerfil(perfil);
    }

    /**
     * Limpia la sesión local de inmediato y pide al backend revocar el token de refresco. Si el backend no
     * responde, la sesión local ya está cerrada y el token caduca solo, así que la operación siempre completa.
     */
    cerrarSesion(): Observable<void> {
        return defer(() => {
            this.limpiar();
            return this.authApi.cerrarSesion().pipe(catchError(() => of(undefined)));
        });
    }

    /** El backend revoca todas las sesiones al cambiar la contraseña, así que también se cierra la local. */
    cambiarContrasena(cambio: CambioContrasena): Observable<void> {
        return this.authApi.cambiarContrasena(cambio).pipe(switchMap(() => this.cerrarSesion()));
    }

    /** Página a la que debe ir el usuario según el estado de la sesión. */
    rutaInicio(): string {
        const perfil = this.perfil();
        if (!this.autenticado()) {
            return RUTAS_AUTH.login;
        }
        return perfil ? RUTA_INICIO_POR_ROL[perfil.rolBase] : RUTAS_AUTH.seleccionarPerfil;
    }

    private cargarUsuario(perfilPreferido: number | null): Observable<void> {
        return this.authApi.obtenerUsuarioActual().pipe(
            map((usuario) => {
                this.usuario.set(usuario);
                this.establecerPerfil(elegirPerfil(usuario, perfilPreferido));
            })
        );
    }

    private establecerPerfil(perfil: Perfil | null): void {
        this.perfil.set(perfil);
        guardarPerfil(perfil);
    }

    private limpiar(): void {
        this.token.set(null);
        this.usuario.set(null);
        this.establecerPerfil(null);
    }
}

function buscarPerfil(usuario: UsuarioActual | null, id: number | null): Perfil | null {
    return usuario?.perfiles.find((perfil) => perfil.id === id) ?? null;
}

/** El perfil guardado solo vale si el usuario aún tiene ese rol activo; con un único perfil, se selecciona solo. */
function elegirPerfil(usuario: UsuarioActual, preferido: number | null): Perfil | null {
    const { perfiles } = usuario;
    return buscarPerfil(usuario, preferido) ?? (perfiles.length === 1 ? perfiles[0] : null);
}

// sessionStorage puede lanzar excepciones (modo privado o almacenamiento bloqueado). Sin él, la sesión
// funciona igual y el usuario con varios perfiles vuelve a elegir al recargar.

/** Un valor que no es un id (p. ej. el nombre de rol que guardaba la SPEC 01) se descarta. */
function leerPerfilGuardado(): number | null {
    try {
        const id = Number(sessionStorage.getItem(CLAVE_PERFIL) ?? Number.NaN);
        return Number.isSafeInteger(id) && id > 0 ? id : null;
    } catch {
        return null;
    }
}

function guardarPerfil(perfil: Perfil | null): void {
    try {
        if (perfil) {
            sessionStorage.setItem(CLAVE_PERFIL, String(perfil.id));
        } else {
            sessionStorage.removeItem(CLAVE_PERFIL);
        }
    } catch {
        // Almacenamiento no disponible: el perfil solo se conserva en memoria.
    }
}
