import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, defer, finalize, firstValueFrom, map, Observable, of, shareReplay, switchMap, tap } from 'rxjs';
import { CambioContrasena } from '../models/cambio-contrasena';
import { Credenciales } from '../models/credenciales';
import { esRol, Rol, RUTA_INICIO_POR_ROL } from '../models/rol';
import { RUTAS_AUTH } from '../models/rutas-auth';
import { UsuarioActual } from '../models/usuario-actual';
import { AuthApiService } from './auth-api.service';

/** Clave de sessionStorage: solo guarda el nombre del rol, nunca tokens ni datos personales. */
const CLAVE_PERFIL = 'iglesia.perfilActivo';

/**
 * Fachada de la sesión. El token de acceso vive solo en memoria; el de refresco, en una cookie HttpOnly
 * que la UI no puede leer. El perfil activo limita la navegación, no los permisos del backend.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
    private readonly authApi = inject(AuthApiService);

    private readonly token = signal<string | null>(null);
    private readonly usuario = signal<UsuarioActual | null>(null);
    private readonly perfil = signal<Rol | null>(null);

    /** Petición de refresco en curso, compartida para que varios 401 simultáneos hagan un solo `POST /refresh`. */
    private refrescoEnCurso: Observable<string> | null = null;

    readonly tokenAcceso = this.token.asReadonly();
    readonly usuarioActual = this.usuario.asReadonly();
    readonly perfilActivo = this.perfil.asReadonly();
    readonly autenticado = computed(() => this.token() !== null && this.usuario() !== null);
    readonly tieneVariosPerfiles = computed(() => (this.usuario()?.roles.length ?? 0) > 1);

    /** Con un solo rol, el perfil queda seleccionado; con varios, el usuario debe elegirlo. */
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

    seleccionarPerfil(rol: Rol): void {
        if (!this.usuario()?.roles.includes(rol)) {
            throw new Error(`El usuario no tiene el perfil ${rol}`);
        }
        this.establecerPerfil(rol);
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
        return perfil ? RUTA_INICIO_POR_ROL[perfil] : RUTAS_AUTH.seleccionarPerfil;
    }

    private cargarUsuario(perfilPreferido: Rol | null): Observable<void> {
        return this.authApi.obtenerUsuarioActual().pipe(
            map((usuario) => {
                this.usuario.set(usuario);
                this.establecerPerfil(elegirPerfil(usuario, perfilPreferido));
            })
        );
    }

    private establecerPerfil(rol: Rol | null): void {
        this.perfil.set(rol);
        guardarPerfil(rol);
    }

    private limpiar(): void {
        this.token.set(null);
        this.usuario.set(null);
        this.establecerPerfil(null);
    }
}

/** El perfil guardado solo vale si el usuario aún tiene ese rol; con un único rol, se selecciona solo. */
function elegirPerfil(usuario: UsuarioActual, preferido: Rol | null): Rol | null {
    if (preferido && usuario.roles.includes(preferido)) {
        return preferido;
    }
    return usuario.roles.length === 1 ? usuario.roles[0] : null;
}

// sessionStorage puede lanzar excepciones (modo privado o almacenamiento bloqueado). Sin él, la sesión
// funciona igual y el usuario con varios roles vuelve a elegir perfil al recargar.

function leerPerfilGuardado(): Rol | null {
    try {
        const valor = sessionStorage.getItem(CLAVE_PERFIL);
        return valor !== null && esRol(valor) ? valor : null;
    } catch {
        return null;
    }
}

function guardarPerfil(rol: Rol | null): void {
    try {
        if (rol) {
            sessionStorage.setItem(CLAVE_PERFIL, rol);
        } else {
            sessionStorage.removeItem(CLAVE_PERFIL);
        }
    } catch {
        // Almacenamiento no disponible: el perfil solo se conserva en memoria.
    }
}
