import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { FormularioNivelComponent } from '../components/formulario-nivel/formulario-nivel';
import { TablaNivelesComponent } from '../components/tabla-niveles/tabla-niveles';
import { Nivel } from '../models/nivel';
import { NivelesService } from '../services/niveles.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los niveles.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el nivel. Intenta de nuevo.';
const MENSAJE_ERROR_ORDEN = 'No se pudo cambiar el orden. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado del nivel. Intenta de nuevo.';

/** Desplazamiento de un nivel en el orden: -1 sube (más alto), +1 baja. */
type Movimiento = -1 | 1;

/** Catálogo de niveles de la iglesia (contenedor): carga, orden, diálogo y llamadas a la API. */
@Component({
    selector: 'app-niveles-lista',
    imports: [ButtonModule, ConfirmDialogModule, MessageModule, FormularioNivelComponent, TablaNivelesComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './niveles-lista.html'
})
export class NivelesListaComponent {
    private readonly servicio = inject(NivelesService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly niveles = signal<readonly Nivel[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly ordenando = signal(false);
    /** Error al ordenar, desactivar o reactivar. */
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly nivelEditado = signal<Nivel | null>(null);
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
            .subscribe({ next: (niveles) => this.niveles.set(niveles), error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA) });
    }

    protected nuevo(): void {
        this.abrir(null);
    }

    protected editar(nivel: Nivel): void {
        this.abrir(nivel);
    }

    protected guardar(nombre: string): void {
        const nivel = this.nivelEditado();
        const peticion: Observable<Nivel> = nivel ? this.servicio.renombrar(nivel.id, nombre) : this.servicio.crear(nombre);
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

    protected subir(nivel: Nivel): void {
        this.mover(nivel, -1);
    }

    protected bajar(nivel: Nivel): void {
        this.mover(nivel, 1);
    }

    /** Desactivar pide confirmación; reactivar no es destructivo. */
    protected cambiarEstado(nivel: Nivel): void {
        this.errorAccion.set(null);
        if (!nivel.activo) {
            this.enviarEstado(nivel, true);
            return;
        }
        this.confirmacion.confirm({
            header: 'Desactivar nivel',
            message: `¿Desactivar «${nivel.nombre}»? Quienes ya lo tienen lo conservan, pero no se podrá asignar a nadie más hasta reactivarlo.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(nivel, false)
        });
    }

    /** Intercambia el nivel con su vecino y envía el orden completo; si falla, la tabla conserva el orden anterior. */
    private mover(nivel: Nivel, movimiento: Movimiento): void {
        const ids = this.niveles().map(({ id }) => id);
        const desde = ids.indexOf(nivel.id);
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
            .subscribe({ next: (niveles) => this.niveles.set(niveles), error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ORDEN)) });
    }

    private enviarEstado(nivel: Nivel, activo: boolean): void {
        this.servicio.cambiarEstado(nivel.id, activo).subscribe({
            next: (actualizado) => this.reemplazar(actualizado),
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
        });
    }

    private abrir(nivel: Nivel | null): void {
        this.errorDialogo.set(null);
        this.nivelEditado.set(nivel);
        this.dialogoVisible.set(true);
    }

    /** Actualiza el nivel en la lista; uno nuevo se agrega al final, que es donde lo pone el backend. */
    private reemplazar(nivel: Nivel): void {
        this.niveles.update((niveles) => (niveles.some(({ id }) => id === nivel.id) ? niveles.map((actual) => (actual.id === nivel.id ? nivel : actual)) : [...niveles, nivel]));
    }
}
