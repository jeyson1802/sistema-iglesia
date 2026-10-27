import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { finalize, forkJoin, Observable } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { esErrorGlobal } from '@/app/core/errores/clasificar-error';
import { FormularioRolComponent, ModoFormularioRol } from '../components/formulario-rol/formulario-rol';
import { TablaRolesComponent } from '../components/tabla-roles/tabla-roles';
import { GuardarRol, PermisoCatalogo, RolAcceso } from '../models/rol-acceso';
import { RolesService } from '../services/roles.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los roles.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el rol. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado del rol. Intenta de nuevo.';

/** Errores de negocio cuyo `detail` (ProblemDetail, en español) se muestra tal cual al usuario. */
const ESTADOS_CON_DETALLE: readonly number[] = [HttpStatusCode.BadRequest, HttpStatusCode.Conflict];

interface DialogoRol {
    readonly modo: ModoFormularioRol;
    readonly rol: RolAcceso | null;
}

/** Administración de los roles de acceso de la iglesia (contenedor): carga, estado y llamadas a la API. */
@Component({
    selector: 'app-roles-lista',
    imports: [ButtonModule, ConfirmDialogModule, MessageModule, FormularioRolComponent, TablaRolesComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './roles-lista.html'
})
export class RolesListaComponent {
    private readonly servicio = inject(RolesService);
    private readonly sesion = inject(SesionService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly roles = signal<RolAcceso[]>([]);
    protected readonly catalogo = signal<readonly PermisoCatalogo[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    /** Error al desactivar o reactivar (p. ej. el 409 de un rol que dejaría usuarios sin roles activos). */
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly dialogo = signal<DialogoRol | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('roles.administrar'));
    protected readonly rolesBase = computed(() => this.roles().filter((rol) => rol.base));

    constructor() {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        forkJoin({ roles: this.servicio.listar(), catalogo: this.servicio.permisos() })
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ roles, catalogo }) => {
                    this.roles.set(roles);
                    this.catalogo.set(catalogo);
                },
                // La pantalla no sirve sin datos: el estado de error con «Reintentar» se muestra siempre.
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }

    protected nuevo(): void {
        this.abrir({ modo: 'crear', rol: null });
    }

    protected ver(rol: RolAcceso): void {
        this.abrir({ modo: 'ver', rol });
    }

    protected editar(rol: RolAcceso): void {
        this.abrir({ modo: 'editar', rol });
    }

    protected guardar(datos: GuardarRol): void {
        const dialogo = this.dialogo();
        if (!dialogo) {
            return;
        }
        const peticion: Observable<RolAcceso> = dialogo.rol ? this.servicio.actualizar(dialogo.rol.id, datos) : this.servicio.crear(datos);
        this.guardando.set(true);
        this.errorDialogo.set(null);
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (guardado) => {
                this.reemplazar(guardado);
                this.dialogoVisible.set(false);
            },
            error: (error: unknown) => this.errorDialogo.set(mensajeDeError(error, MENSAJE_ERROR_GUARDAR))
        });
    }

    /** Desactivar pide confirmación (indica a cuántos usuarios afecta); reactivar no es destructivo. */
    protected cambiarEstado(rol: RolAcceso): void {
        this.errorAccion.set(null);
        if (!rol.activo) {
            this.enviarEstado(rol, true);
            return;
        }
        this.confirmacion.confirm({
            header: 'Desactivar rol',
            message: `¿Desactivar «${rol.nombre}»? ${descripcionUsuarios(rol.cantidadUsuarios)} Dejará de aportar sus permisos desde su próximo inicio de sesión.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(rol, false)
        });
    }

    private enviarEstado(rol: RolAcceso, activo: boolean): void {
        this.servicio.cambiarEstado(rol.id, activo).subscribe({
            next: (actualizado) => this.reemplazar(actualizado),
            error: (error: unknown) => this.errorAccion.set(mensajeDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private abrir(dialogo: DialogoRol): void {
        this.errorDialogo.set(null);
        this.dialogo.set(dialogo);
        this.dialogoVisible.set(true);
    }

    /** Actualiza el rol en la lista sin recargarla; uno nuevo se agrega al final. */
    private reemplazar(rol: RolAcceso): void {
        this.roles.update((roles) => (roles.some(({ id }) => id === rol.id) ? roles.map((actual) => (actual.id === rol.id ? rol : actual)) : [...roles, rol]));
    }
}

function descripcionUsuarios(cantidad: number): string {
    if (cantidad === 0) {
        return 'Ningún usuario lo tiene asignado.';
    }
    return cantidad === 1 ? 'Lo tiene asignado 1 usuario.' : `Lo tienen asignado ${cantidad} usuarios.`;
}

/**
 * Los 400 y 409 traen en `detail` el motivo en español (p. ej. «Ya existe un rol con ese nombre»). Los 5xx y la
 * falta de conexión ya los avisa el toast global: la página no repite el mensaje.
 */
function mensajeDeError(error: unknown, generico: string): string | null {
    if (esErrorGlobal(error)) {
        return null;
    }
    if (error instanceof HttpErrorResponse && ESTADOS_CON_DETALLE.includes(error.status) && typeof error.error?.detail === 'string') {
        return error.error.detail;
    }
    return generico;
}
