import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NombresGrupos } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { FormularioGrupoComponent } from '../components/formulario-grupo/formulario-grupo';
import { NombresGruposComponent } from '../components/nombres-grupos/nombres-grupos';
import { TablaGruposComponent } from '../components/tabla-grupos/tabla-grupos';
import { Grupo, GuardarGrupo } from '../models/grupo';
import { GruposService } from '../services/grupos.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los grupos.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado. Intenta de nuevo.';
const MENSAJE_ERROR_NOMBRES = 'No se pudieron guardar los nombres. Intenta de nuevo.';

/** Grupos pequeños de la iglesia (contenedor): carga, diálogos de grupo y de nombres, y llamadas a la API. */
@Component({
    selector: 'app-grupos-lista',
    imports: [ButtonModule, ConfirmDialogModule, MessageModule, FormularioGrupoComponent, NombresGruposComponent, TablaGruposComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './grupos-lista.html'
})
export class GruposListaComponent {
    private readonly servicio = inject(GruposService);
    private readonly configuracion = inject(ConfiguracionIglesiaService);
    private readonly sesion = inject(SesionService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly nombres = this.configuracion.nombresGrupos;
    protected readonly grupos = signal<readonly Grupo[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly grupoEditado = signal<Grupo | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});

    protected readonly nombresVisible = signal(false);
    protected readonly guardandoNombres = signal(false);
    protected readonly errorNombres = signal<string | null>(null);

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('grupos.administrar'));

    constructor() {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.servicio
            .listar()
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (grupos) => this.grupos.set(grupos), error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA) });
    }

    protected nuevo(): void {
        this.abrir(null);
    }

    protected editar(grupo: Grupo): void {
        this.abrir(grupo);
    }

    protected guardar(datos: GuardarGrupo): void {
        const grupo = this.grupoEditado();
        const peticion: Observable<Grupo> = grupo ? this.servicio.actualizar(grupo.id, datos) : this.servicio.crear(datos);
        this.guardando.set(true);
        this.errorDialogo.set(null);
        this.erroresCampo.set({});
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (guardado) => {
                this.reemplazar(guardado);
                this.dialogoVisible.set(false);
            },
            error: (error: unknown) => {
                const porCampo = erroresPorCampo(error);
                this.erroresCampo.set(porCampo);
                // Si el backend señaló el campo, el mensaje sale junto a él y no se repite arriba.
                this.errorDialogo.set(Object.keys(porCampo).length ? null : detalleDeError(error, MENSAJE_ERROR_GUARDAR));
            }
        });
    }

    protected abrirNombres(): void {
        this.errorNombres.set(null);
        this.nombresVisible.set(true);
    }

    protected guardarNombres(nombres: NombresGrupos): void {
        this.guardandoNombres.set(true);
        this.errorNombres.set(null);
        this.configuracion
            .guardar(nombres)
            .pipe(finalize(() => this.guardandoNombres.set(false)))
            .subscribe({ next: () => this.nombresVisible.set(false), error: (error: unknown) => this.errorNombres.set(detalleDeError(error, MENSAJE_ERROR_NOMBRES)) });
    }

    /** Desactivar pide confirmación; el backend lo rechaza (409) si aún tiene miembros activos. */
    protected cambiarEstado(grupo: Grupo): void {
        this.errorAccion.set(null);
        if (!grupo.activo) {
            this.enviarEstado(grupo, true);
            return;
        }
        this.confirmacion.confirm({
            header: `Desactivar ${this.nombres().nombreGrupoSingular.toLowerCase()}`,
            message: `¿Desactivar «${grupo.nombre}»? No se podrá asignar a nadie hasta reactivarlo. Antes debe quedar sin miembros activos.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(grupo, false)
        });
    }

    private enviarEstado(grupo: Grupo, activo: boolean): void {
        this.servicio.cambiarEstado(grupo.id, activo).subscribe({
            next: (actualizado) => this.reemplazar(actualizado),
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private abrir(grupo: Grupo | null): void {
        this.errorDialogo.set(null);
        this.erroresCampo.set({});
        this.grupoEditado.set(grupo);
        this.dialogoVisible.set(true);
    }

    /** Actualiza el grupo en la lista, que se mantiene ordenada por nombre como la devuelve el backend. */
    private reemplazar(grupo: Grupo): void {
        this.grupos.update((grupos) => [...grupos.filter(({ id }) => id !== grupo.id), grupo].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')));
    }
}
