import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { catchError, finalize, Observable, of } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { FormularioCategoriaComponent } from '../components/formulario-categoria/formulario-categoria';
import { SelectorMonedaComponent } from '../components/selector-moneda/selector-moneda';
import { TablaCategoriasComponent } from '../components/tabla-categorias/tabla-categorias';
import { CategoriaAporte, GuardarCategoriaAporte, Moneda } from '../models/configuracion-aportes';
import { CategoriasAporteService } from '../services/categorias-aporte.service';
import { ConfiguracionAportesService } from '../services/configuracion-aportes.service';

const MENSAJE_ERROR_CARGA = 'No se pudo cargar la configuración de aportes.';
const MENSAJE_ERROR_MONEDA = 'No se pudo guardar la moneda. Intenta de nuevo.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar la categoría. Intenta de nuevo.';
const MENSAJE_ERROR_ORDEN = 'No se pudo cambiar el orden. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado de la categoría. Intenta de nuevo.';

type Movimiento = -1 | 1;

/** Configuración de aportes (contenedor): moneda de la iglesia y catálogo de categorías. */
@Component({
    selector: 'app-configuracion-aportes',
    imports: [ButtonModule, ConfirmDialogModule, DividerModule, MessageModule, FormularioCategoriaComponent, SelectorMonedaComponent, TablaCategoriasComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './configuracion-aportes.html'
})
export class ConfiguracionAportesComponent {
    private readonly configuracionServicio = inject(ConfiguracionAportesService);
    private readonly categoriasServicio = inject(CategoriasAporteService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly configuracion = this.configuracionServicio.configuracion;
    protected readonly monedas = signal<readonly Moneda[]>([]);
    protected readonly cargandoMoneda = signal(true);
    protected readonly guardandoMoneda = signal(false);
    protected readonly mensajeMoneda = signal<{ readonly severidad: 'success' | 'error'; readonly texto: string } | null>(null);

    protected readonly categorias = signal<readonly CategoriaAporte[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly ordenando = signal(false);
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly categoriaEditada = signal<CategoriaAporte | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);

    constructor() {
        this.cargarMoneda();
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.categoriasServicio
            .listar()
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (categorias) => this.categorias.set(categorias), error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA) });
    }

    protected guardarMoneda(moneda: string): void {
        this.guardandoMoneda.set(true);
        this.mensajeMoneda.set(null);
        this.configuracionServicio
            .guardar(moneda)
            .pipe(finalize(() => this.guardandoMoneda.set(false)))
            .subscribe({
                next: () => this.mensajeMoneda.set({ severidad: 'success', texto: 'Moneda guardada.' }),
                error: (error: unknown) => {
                    const texto = detalleDeError(error, MENSAJE_ERROR_MONEDA);
                    this.mensajeMoneda.set(texto ? { severidad: 'error', texto } : null);
                    this.configuracionServicio.recargar().subscribe({ error: () => undefined });
                }
            });
    }

    protected nueva(): void {
        this.abrir(null);
    }

    protected editar(categoria: CategoriaAporte): void {
        this.abrir(categoria);
    }

    protected guardar(datos: GuardarCategoriaAporte): void {
        const categoria = this.categoriaEditada();
        const peticion: Observable<CategoriaAporte> = categoria ? this.categoriasServicio.actualizar(categoria.id, datos) : this.categoriasServicio.crear(datos);
        this.guardando.set(true);
        this.errorDialogo.set(null);
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (guardada) => {
                this.reemplazar(guardada);
                this.dialogoVisible.set(false);
            },
            error: (error: unknown) => this.errorDialogo.set(detalleDeError(error, MENSAJE_ERROR_GUARDAR))
        });
    }

    protected subir(categoria: CategoriaAporte): void {
        this.mover(categoria, -1);
    }

    protected bajar(categoria: CategoriaAporte): void {
        this.mover(categoria, 1);
    }

    protected cambiarEstado(categoria: CategoriaAporte): void {
        this.errorAccion.set(null);
        if (!categoria.activo) {
            this.enviarEstado(categoria, true);
            return;
        }
        this.confirmacion.confirm({
            header: 'Desactivar categoría',
            message: `¿Desactivar «${categoria.nombre}»? Sus aportes se conservan y siguen contando en los totales, pero no se podrá usar en aportes nuevos hasta reactivarla.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(categoria, false)
        });
    }

    /** Las monedas solo sirven para elegir: si no cargan, el selector muestra la actual y queda sin opciones. */
    private cargarMoneda(): void {
        this.configuracionServicio
            .recargar()
            .pipe(catchError(() => of(null)))
            .subscribe(() => {
                this.configuracionServicio
                    .monedas()
                    .pipe(
                        catchError(() => of([])),
                        finalize(() => this.cargandoMoneda.set(false))
                    )
                    .subscribe((monedas) => this.monedas.set(monedas));
            });
    }

    private mover(categoria: CategoriaAporte, movimiento: Movimiento): void {
        const ids = this.categorias().map(({ id }) => id);
        const desde = ids.indexOf(categoria.id);
        const hasta = desde + movimiento;
        if (desde < 0 || hasta < 0 || hasta >= ids.length) {
            return;
        }
        [ids[desde], ids[hasta]] = [ids[hasta], ids[desde]];
        this.ordenando.set(true);
        this.errorAccion.set(null);
        this.categoriasServicio
            .ordenar(ids)
            .pipe(finalize(() => this.ordenando.set(false)))
            .subscribe({ next: (categorias) => this.categorias.set(categorias), error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ORDEN)) });
    }

    private enviarEstado(categoria: CategoriaAporte, activo: boolean): void {
        this.categoriasServicio.cambiarEstado(categoria.id, activo).subscribe({
            next: (actualizada) => this.reemplazar(actualizada),
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private abrir(categoria: CategoriaAporte | null): void {
        this.errorDialogo.set(null);
        this.categoriaEditada.set(categoria);
        this.dialogoVisible.set(true);
    }

    private reemplazar(categoria: CategoriaAporte): void {
        this.categorias.update((categorias) => (categorias.some(({ id }) => id === categoria.id) ? categorias.map((actual) => (actual.id === categoria.id ? categoria : actual)) : [...categorias, categoria]));
    }
}
