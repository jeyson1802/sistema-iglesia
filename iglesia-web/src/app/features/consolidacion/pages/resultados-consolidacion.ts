import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { FormularioResultadoComponent } from '../components/formulario-resultado/formulario-resultado';
import { GrupoOpcionesComponent, OpcionGrupo } from '../components/grupo-opciones/grupo-opciones';
import { TablaResultadosComponent } from '../components/tabla-resultados/tabla-resultados';
import { ResultadoConsolidacion } from '../models/consolidacion';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los resultados.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el resultado. Intenta de nuevo.';
const MENSAJE_ERROR_ORDEN = 'No se pudo cambiar el orden. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado del resultado. Intenta de nuevo.';

/** Desplazamiento de un resultado en el orden: -1 sube (más alto), +1 baja. */
type Movimiento = -1 | 1;

/** Catálogo de resultados de consolidación (contenedor): carga, orden, diálogo y llamadas a la API. El orden es el de
 * los botones al cerrar un caso; desactivar conserva el historial. */
@Component({
    selector: 'app-resultados-consolidacion',
    imports: [ButtonModule, ConfirmDialogModule, MessageModule, FormularioResultadoComponent, GrupoOpcionesComponent, TablaResultadosComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './resultados-consolidacion.html'
})
export class ResultadosConsolidacionComponent {
    private readonly servicio = inject(ResultadosConsolidacionService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly resultados = signal<readonly ResultadoConsolidacion[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly ordenando = signal(false);
    /** Error al ordenar, desactivar o reactivar. */
    protected readonly errorAccion = signal<string | null>(null);

    /** Cómo verá el consolidador los resultados activos al cerrar un caso. */
    protected readonly vistaPrevia = computed<readonly OpcionGrupo<number>[]>(() =>
        this.resultados()
            .filter(({ activo }) => activo)
            .map(({ id, nombre }) => ({ valor: id, etiqueta: nombre }))
    );

    protected readonly resultadoEditado = signal<ResultadoConsolidacion | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);

    constructor() {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.servicio
            .listar()
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (resultados) => this.resultados.set(resultados), error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA) });
    }

    protected nuevo(): void {
        this.abrir(null);
    }

    protected editar(resultado: ResultadoConsolidacion): void {
        this.abrir(resultado);
    }

    protected guardar(nombre: string): void {
        const resultado = this.resultadoEditado();
        const peticion: Observable<ResultadoConsolidacion> = resultado ? this.servicio.renombrar(resultado.id, nombre) : this.servicio.crear(nombre);
        this.guardando.set(true);
        this.errorDialogo.set(null);
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (guardado) => {
                this.reemplazar(guardado);
                this.dialogoVisible.set(false);
            },
            error: (error: unknown) => this.errorDialogo.set(detalleDeError(error, MENSAJE_ERROR_GUARDAR))
        });
    }

    protected subir(resultado: ResultadoConsolidacion): void {
        this.mover(resultado, -1);
    }

    protected bajar(resultado: ResultadoConsolidacion): void {
        this.mover(resultado, 1);
    }

    /** Desactivar pide confirmación; reactivar no es destructivo. */
    protected cambiarEstado(resultado: ResultadoConsolidacion): void {
        this.errorAccion.set(null);
        if (!resultado.activo) {
            this.enviarEstado(resultado, true);
            return;
        }
        this.confirmacion.confirm({
            header: 'Desactivar resultado',
            message: `¿Desactivar «${resultado.nombre}»? Los casos que ya cerraron con él lo conservan en su historial, pero no se ofrecerá al cerrar nuevos casos hasta reactivarlo.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(resultado, false)
        });
    }

    /** Intercambia el resultado con su vecino y envía el orden completo; si falla, la tabla conserva el orden anterior. */
    private mover(resultado: ResultadoConsolidacion, movimiento: Movimiento): void {
        const ids = this.resultados().map(({ id }) => id);
        const desde = ids.indexOf(resultado.id);
        const hasta = desde + movimiento;
        if (desde < 0 || hasta < 0 || hasta >= ids.length) {
            return;
        }
        [ids[desde], ids[hasta]] = [ids[hasta], ids[desde]];
        this.ordenando.set(true);
        this.errorAccion.set(null);
        this.servicio
            .ordenar(ids)
            .pipe(finalize(() => this.ordenando.set(false)))
            .subscribe({ next: (resultados) => this.resultados.set(resultados), error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ORDEN)) });
    }

    private enviarEstado(resultado: ResultadoConsolidacion, activo: boolean): void {
        this.servicio.cambiarEstado(resultado.id, activo).subscribe({
            next: (actualizado) => this.reemplazar(actualizado),
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private abrir(resultado: ResultadoConsolidacion | null): void {
        this.errorDialogo.set(null);
        this.resultadoEditado.set(resultado);
        this.dialogoVisible.set(true);
    }

    /** Actualiza el resultado en la lista; uno nuevo se agrega al final, que es donde lo pone el backend. */
    private reemplazar(resultado: ResultadoConsolidacion): void {
        this.resultados.update((resultados) => (resultados.some(({ id }) => id === resultado.id) ? resultados.map((actual) => (actual.id === resultado.id ? resultado : actual)) : [...resultados, resultado]));
    }
}
