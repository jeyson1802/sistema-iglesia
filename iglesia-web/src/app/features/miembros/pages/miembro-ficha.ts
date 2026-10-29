import { afterNextRender, ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, Observable, of, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { Grupo } from '@/app/features/grupos/models/grupo';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { Nivel } from '@/app/features/niveles/models/nivel';
import { NivelesService } from '@/app/features/niveles/services/niveles.service';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { CrearUsuario, UsuarioAdmin } from '@/app/features/usuarios/models/usuario-admin';
import { UsuariosService } from '@/app/features/usuarios/services/usuarios.service';
import { Rol } from '@/app/core/auth/models/rol';
import { CadenaLiderazgoComponent } from '../components/cadena-liderazgo/cadena-liderazgo';
import { DarAccesoComponent, RolAccesoMiembro } from '../components/dar-acceso/dar-acceso';
import { FormularioMiembroComponent } from '../components/formulario-miembro/formulario-miembro';
import { ProgresoFichaComponent } from '../components/progreso-ficha/progreso-ficha';
import { EslabonCadena, GuardarMiembro, Miembro, MiembroResumen, nombreCompleto } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';

const RUTA_MIEMBROS = '/miembros';
/** Estado de navegación con el que el alta abre la ficha recién creada, para avisar que se registró. */
const AVISO_REGISTRADO = 'registrado';
/** Roles base que se pueden dar desde la ficha: los dos que exigen un miembro vinculado. */
const ROLES_CON_MIEMBRO: readonly Rol[] = ['LIDER', 'MIEMBRO'];
/** Cuántos posibles duplicados se nombran en el aviso. */
const DUPLICADOS_NOMBRADOS = 3;

const MENSAJE_NO_ENCONTRADO = 'No se encontró el miembro o no tienes acceso a su ficha.';
const MENSAJE_ERROR_CARGA = 'No se pudo cargar la ficha.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar la ficha. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado. Intenta de nuevo.';
const MENSAJE_ERROR_PASTOR = 'No se pudo designar al pastor principal. Intenta de nuevo.';
const MENSAJE_ERROR_ACCESO = 'No se pudo dar acceso al sistema. Intenta de nuevo.';
const MENSAJE_ERROR_ROLES = 'No se pudieron cargar los roles para dar acceso.';

const BOTONES_CONFIRMACION = {
    rejectLabel: 'Cancelar',
    rejectButtonProps: { severity: 'secondary', outlined: true },
    icon: 'pi pi-exclamation-triangle'
} as const;

/**
 * Ficha de un miembro (contenedor) para `/miembros/nuevo` y `/miembros/:id`: alta con aviso de duplicados, edición
 * (o solo lectura sin permiso), desactivar o reactivar, designar pastor principal y dar acceso al sistema.
 */
@Component({
    selector: 'app-miembro-ficha',
    imports: [RouterLink, ButtonModule, ConfirmDialogModule, MessageModule, SkeletonModule, TagModule, ToastModule, CadenaLiderazgoComponent, DarAccesoComponent, FormularioMiembroComponent, ProgresoFichaComponent],
    providers: [ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './miembro-ficha.html'
})
export class MiembroFichaComponent {
    private readonly servicio = inject(MiembrosService);
    private readonly nivelesServicio = inject(NivelesService);
    private readonly gruposServicio = inject(GruposService);
    private readonly usuarios = inject(UsuariosService);
    private readonly roles = inject(RolesService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);

    protected readonly nombresGrupos = inject(ConfiguracionIglesiaService).nombresGrupos;
    protected readonly esNuevo = signal(false);
    protected readonly miembro = signal<Miembro | null>(null);
    protected readonly cargando = signal(false);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly niveles = signal<readonly Nivel[]>([]);
    protected readonly grupos = signal<readonly Grupo[]>([]);
    protected readonly cadena = signal<readonly EslabonCadena[]>([]);
    protected readonly cargandoCadena = signal(false);
    /** Usuario vinculado; `undefined` mientras no se sabe (o no se puede consultar). */
    protected readonly acceso = signal<UsuarioAdmin | null | undefined>(undefined);

    protected readonly guardando = signal(false);
    protected readonly errorGuardar = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly accesoVisible = signal(false);
    protected readonly rolesAcceso = signal<readonly RolAccesoMiembro[]>([]);
    protected readonly guardandoAcceso = signal(false);
    protected readonly errorAcceso = signal<string | null>(null);

    protected readonly soloLectura = computed(() => !this.sesion.tienePermiso(this.esNuevo() ? 'miembros.crear' : 'miembros.editar'));
    protected readonly titulo = computed(() => {
        const miembro = this.miembro();
        return miembro ? nombreCompleto(miembro) : 'Nuevo miembro';
    });
    protected readonly puedeCambiarEstado = computed(() => this.sesion.tienePermiso('miembros.desactivar'));
    protected readonly puedeDesignarPastor = computed(() => {
        const miembro = this.miembro();
        return !!miembro && miembro.activo && !miembro.esPastorPrincipal && this.sesion.tienePermiso('miembros.editar');
    });
    protected readonly puedeDarAcceso = computed(() => !!this.miembro()?.activo && this.acceso() === null && this.sesion.tienePermiso('usuarios.administrar'));

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        if (this.router.currentNavigation()?.extras.state?.[AVISO_REGISTRADO]) {
            // El toast se suscribe al renderizar: el aviso se agrega después.
            afterNextRender(() => this.avisar('Miembro registrado', 'Completa su ficha cuando tengas más datos.'));
        }
        this.cargarCatalogos();
        inject(ActivatedRoute)
            .paramMap.pipe(takeUntilDestroyed())
            .subscribe((parametros) => this.abrir(parametros.get('id')));
    }

    protected guardar(datos: GuardarMiembro): void {
        const miembro = this.miembro();
        if (miembro) {
            this.enviar(this.servicio.actualizar(miembro.id, datos), (actualizado) => {
                this.miembro.set(actualizado);
                this.cargarCadena(actualizado.id);
                this.avisar('Cambios guardados', `Se actualizó la ficha de ${nombreCompleto(actualizado)}.`);
            });
            return;
        }
        this.guardando.set(true);
        this.servicio.posiblesDuplicados(datos.nombres, datos.apellidos).subscribe({
            next: (duplicados) => (duplicados.length ? this.confirmarDuplicados(datos, duplicados) : this.crear(datos)),
            // Si la comprobación falla no se bloquea el alta: el aviso es una ayuda.
            error: () => this.crear(datos)
        });
    }

    protected cancelar(): void {
        void this.router.navigate([RUTA_MIEMBROS]);
    }

    /** Desactivar pide confirmación; el backend lo rechaza (409) si lidera a alguien activo o es el pastor principal. */
    protected cambiarEstado(): void {
        const miembro = this.miembro();
        if (!miembro) {
            return;
        }
        this.errorAccion.set(null);
        if (!miembro.activo) {
            this.enviarEstado(miembro, true);
            return;
        }
        this.confirmacion.confirm({
            ...BOTONES_CONFIRMACION,
            header: 'Desactivar miembro',
            message: `¿Desactivar a «${nombreCompleto(miembro)}»? Su ficha se conserva, pero no se podrá asignar como líder, invitador ni registrador.`,
            acceptLabel: 'Desactivar',
            acceptButtonProps: { severity: 'danger' },
            accept: () => this.enviarEstado(miembro, false)
        });
    }

    /** Pide confirmar nombrando al pastor que se reemplaza, si lo hay. */
    protected designarPastor(): void {
        const miembro = this.miembro();
        if (!miembro) {
            return;
        }
        this.errorAccion.set(null);
        this.servicio.pastorPrincipal().subscribe({
            next: (anterior) =>
                this.confirmacion.confirm({
                    ...BOTONES_CONFIRMACION,
                    header: 'Designar pastor principal',
                    message: mensajePastor(miembro, anterior),
                    acceptLabel: 'Designar',
                    acceptButtonProps: { severity: 'primary' },
                    accept: () => this.enviarPastor(miembro)
                }),
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_PASTOR))
        });
    }

    /** Los roles se piden la primera vez que se abre el diálogo. */
    protected abrirDarAcceso(): void {
        this.errorAccion.set(null);
        this.errorAcceso.set(null);
        if (this.rolesAcceso().length) {
            this.accesoVisible.set(true);
            return;
        }
        this.roles.listar().subscribe({
            next: (roles) => {
                this.rolesAcceso.set(roles.filter((rol) => rol.base && rol.activo && ROLES_CON_MIEMBRO.includes(rol.rolBase)).map(({ id, nombre }) => ({ id, nombre })));
                this.accesoVisible.set(true);
            },
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ROLES))
        });
    }

    protected darAcceso(datos: CrearUsuario): void {
        const miembro = this.miembro();
        if (!miembro) {
            return;
        }
        this.guardandoAcceso.set(true);
        this.errorAcceso.set(null);
        this.usuarios
            .crearParaMiembro(miembro.id, datos)
            .pipe(finalize(() => this.guardandoAcceso.set(false)))
            .subscribe({
                next: (usuario) => {
                    this.acceso.set(usuario);
                    this.accesoVisible.set(false);
                    this.avisar('Acceso creado', `${usuario.correo} deberá cambiar su contraseña temporal al iniciar sesión.`);
                },
                error: (error: unknown) => this.errorAcceso.set(detalleDeError(error, MENSAJE_ERROR_ACCESO))
            });
    }

    /** Carga la ficha de la ruta; `null` (ruta `nuevo`) abre el alta. */
    private abrir(id: string | null): void {
        this.carga?.unsubscribe();
        this.miembro.set(null);
        this.cadena.set([]);
        this.acceso.set(undefined);
        this.errorCarga.set(null);
        this.errorGuardar.set(null);
        this.errorAccion.set(null);
        this.erroresCampo.set({});
        this.esNuevo.set(id === null);
        if (id === null) {
            return;
        }
        const miembroId = Number(id);
        if (!Number.isInteger(miembroId)) {
            this.errorCarga.set(MENSAJE_NO_ENCONTRADO);
            return;
        }
        this.cargando.set(true);
        this.carga = this.servicio
            .obtener(miembroId)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (miembro) => {
                    this.miembro.set(miembro);
                    this.cargarCadena(miembro.id);
                    this.cargarAcceso(miembro.id);
                },
                error: (error: unknown) => this.errorCarga.set(error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound ? MENSAJE_NO_ENCONTRADO : MENSAJE_ERROR_CARGA)
            });
    }

    /** Los selectores de nivel y grupo son una ayuda: si no cargan, quedan vacíos y la ficha sigue funcionando. */
    private cargarCatalogos(): void {
        forkJoin({
            niveles: this.nivelesServicio.listar().pipe(catchError(() => of([]))),
            grupos: this.gruposServicio.listar().pipe(catchError(() => of([])))
        }).subscribe(({ niveles, grupos }) => {
            this.niveles.set(niveles);
            this.grupos.set(grupos);
        });
    }

    private cargarCadena(id: number): void {
        this.cargandoCadena.set(true);
        this.servicio
            .cadena(id)
            .pipe(finalize(() => this.cargandoCadena.set(false)))
            .subscribe({ next: (cadena) => this.cadena.set(cadena), error: () => this.cadena.set([]) });
    }

    /** Solo quien puede ver usuarios sabe si el miembro tiene acceso; sin ese permiso no se muestra nada. */
    private cargarAcceso(id: number): void {
        if (!this.sesion.tienePermiso('usuarios.ver')) {
            return;
        }
        this.usuarios.usuarioDeMiembro(id).subscribe({ next: (usuario) => this.acceso.set(usuario), error: () => this.acceso.set(undefined) });
    }

    private confirmarDuplicados(datos: GuardarMiembro, duplicados: readonly MiembroResumen[]): void {
        this.guardando.set(false);
        const nombres = duplicados
            .slice(0, DUPLICADOS_NOMBRADOS)
            .map((duplicado) => `«${nombreCompleto(duplicado)}»${duplicado.activo ? '' : ' (inactivo)'}`)
            .join(', ');
        const otros = duplicados.length > DUPLICADOS_NOMBRADOS ? ` y ${duplicados.length - DUPLICADOS_NOMBRADOS} más` : '';
        this.confirmacion.confirm({
            ...BOTONES_CONFIRMACION,
            header: 'Posible miembro repetido',
            message: `Ya existe alguien con los mismos nombres y apellidos: ${nombres}${otros}. ¿Registrar de todas formas?`,
            acceptLabel: 'Registrar de todas formas',
            acceptButtonProps: { severity: 'primary' },
            accept: () => this.crear(datos)
        });
    }

    private crear(datos: GuardarMiembro): void {
        this.enviar(this.servicio.crear(datos), (creado) => void this.router.navigate([RUTA_MIEMBROS, creado.id], { replaceUrl: true, state: { [AVISO_REGISTRADO]: true } }));
    }

    private enviar(peticion: Observable<Miembro>, alGuardar: (miembro: Miembro) => void): void {
        this.guardando.set(true);
        this.errorGuardar.set(null);
        this.erroresCampo.set({});
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: alGuardar,
            error: (error: unknown) => {
                const porCampo = erroresPorCampo(error);
                this.erroresCampo.set(porCampo);
                // Si el backend señaló el campo (p. ej. un ciclo en el líder), el mensaje sale junto a él.
                this.errorGuardar.set(Object.keys(porCampo).length ? null : detalleDeError(error, MENSAJE_ERROR_GUARDAR));
            }
        });
    }

    private enviarEstado(miembro: Miembro, activo: boolean): void {
        this.servicio.cambiarEstado(miembro.id, activo).subscribe({
            next: (actualizado) => {
                this.miembro.set(actualizado);
                this.avisar(activo ? 'Miembro reactivado' : 'Miembro desactivado', nombreCompleto(actualizado));
            },
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private enviarPastor(miembro: Miembro): void {
        this.servicio.designarPastorPrincipal(miembro.id).subscribe({
            next: (actualizado) => {
                this.miembro.set(actualizado);
                this.cadena.set([]);
                this.avisar('Pastor principal designado', nombreCompleto(actualizado));
            },
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_PASTOR))
        });
    }

    private avisar(summary: string, detail: string): void {
        this.mensajes.add({ severity: 'success', summary, detail });
    }
}

function mensajePastor(miembro: Miembro, anterior: MiembroResumen | null): string {
    const nuevo = `«${nombreCompleto(miembro)}»`;
    const sinLider = miembro.lider ? ` Dejará de tener líder (hoy es ${miembro.lider.nombre}).` : '';
    if (anterior && anterior.id !== miembro.id) {
        return `${nuevo} reemplazará a «${nombreCompleto(anterior)}» como pastor principal.${sinLider} ¿Continuar?`;
    }
    return `${nuevo} será el pastor principal de la iglesia.${sinLider} ¿Continuar?`;
}
