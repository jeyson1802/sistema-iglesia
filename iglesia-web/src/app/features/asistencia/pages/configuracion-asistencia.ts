import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { FormularioCultoComponent } from '../components/formulario-culto/formulario-culto';
import { FormularioReglasComponent } from '../components/formulario-reglas/formulario-reglas';
import { TablaCultosComponent } from '../components/tabla-cultos/tabla-cultos';
import { CultoProgramado, GuardarCulto } from '../models/asistencia';
import { ReglasSeguimiento } from '../models/seguimiento';
import { CultosProgramadosService } from '../services/cultos-programados.service';
import { SeguimientoService } from '../services/seguimiento.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los horarios de culto.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el culto. Intenta de nuevo.';
const MENSAJE_ERROR_ESTADO = 'No se pudo cambiar el estado del culto. Intenta de nuevo.';
const MENSAJE_ERROR_REGLAS = 'No se pudieron cargar las reglas de seguimiento.';
const MENSAJE_ERROR_GUARDAR_REGLAS = 'No se pudieron guardar las reglas. Intenta de nuevo.';

/**
 * Horarios de culto (contenedor): los cultos fijos de la iglesia con su día y hora (no se borran: se desactivan) y las
 * reglas con las que el seguimiento de asistencia da sus alertas.
 */
@Component({
    selector: 'app-configuracion-asistencia',
    imports: [ButtonModule, MessageModule, FormularioCultoComponent, FormularioReglasComponent, TablaCultosComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './configuracion-asistencia.html'
})
export class ConfiguracionAsistenciaComponent {
    private readonly servicio = inject(CultosProgramadosService);
    private readonly seguimiento = inject(SeguimientoService);

    protected readonly cultos = signal<readonly CultoProgramado[]>([]);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly errorAccion = signal<string | null>(null);
    protected readonly procesando = signal<number | null>(null);

    protected readonly cultoEditado = signal<CultoProgramado | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);

    protected readonly reglas = signal<ReglasSeguimiento | null>(null);
    protected readonly errorCargaReglas = signal<string | null>(null);
    protected readonly guardandoReglas = signal(false);
    protected readonly errorReglas = signal<string | null>(null);
    protected readonly reglasGuardadas = signal(false);

    constructor() {
        this.cargar();
        this.cargarReglas();
    }

    protected cargarReglas(): void {
        this.errorCargaReglas.set(null);
        this.seguimiento.reglas().subscribe({ next: (reglas) => this.reglas.set(reglas), error: () => this.errorCargaReglas.set(MENSAJE_ERROR_REGLAS) });
    }

    protected guardarReglas(reglas: ReglasSeguimiento): void {
        this.guardandoReglas.set(true);
        this.errorReglas.set(null);
        this.reglasGuardadas.set(false);
        this.seguimiento
            .guardarReglas(reglas)
            .pipe(finalize(() => this.guardandoReglas.set(false)))
            .subscribe({
                next: (guardadas) => {
                    this.reglas.set(guardadas);
                    this.reglasGuardadas.set(true);
                },
                error: (error: unknown) => this.errorReglas.set(detalleDeError(error, MENSAJE_ERROR_GUARDAR_REGLAS))
            });
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.servicio
            .listar()
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (cultos) => this.cultos.set(cultos), error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA) });
    }

    protected nuevo(): void {
        this.abrirDialogo(null);
    }

    protected editar(culto: CultoProgramado): void {
        this.abrirDialogo(culto);
    }

    protected guardar(datos: GuardarCulto): void {
        const editado = this.cultoEditado();
        const peticion: Observable<CultoProgramado> = editado ? this.servicio.actualizar(editado.id, datos) : this.servicio.crear(datos);
        this.guardando.set(true);
        this.errorDialogo.set(null);
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: () => {
                this.dialogoVisible.set(false);
                this.cargar();
            },
            error: (error: unknown) => this.errorDialogo.set(detalleDeError(error, MENSAJE_ERROR_GUARDAR))
        });
    }

    protected cambiarEstado(culto: CultoProgramado): void {
        this.procesando.set(culto.id);
        this.errorAccion.set(null);
        this.servicio
            .cambiarEstado(culto.id, !culto.activo)
            .pipe(finalize(() => this.procesando.set(null)))
            .subscribe({
                next: (actualizado) => this.cultos.update((cultos) => cultos.map((actual) => (actual.id === actualizado.id ? actualizado : actual))),
                error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ESTADO))
            });
    }

    private abrirDialogo(culto: CultoProgramado | null): void {
        this.cultoEditado.set(culto);
        this.errorDialogo.set(null);
        this.dialogoVisible.set(true);
    }
}
