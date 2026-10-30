import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { StepperModule } from 'primeng/stepper';
import { Referencia } from '@/app/core/models/referencia';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FormularioRondaComponent } from '../components/formulario-ronda/formulario-ronda';
import { RepartoComponent } from '../components/reparto/reparto';
import { SelectorLoteComponent } from '../components/selector-lote/selector-lote';
import { Consolidador, DatosRonda, FILTRO_LOTE_INICIAL, FiltroLote, LIMITES_CONSOLIDACION, Ronda, SolicitudReparto } from '../models/consolidacion';
import { RUTA_CONSOLIDACION } from '../models/rutas-consolidacion';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';

/** Pasos del asistente, en orden. */
const PASOS = [
    { valor: 1, etiqueta: 'Datos' },
    { valor: 2, etiqueta: 'Personas' },
    { valor: 3, etiqueta: 'Reparto' },
    { valor: 4, etiqueta: 'Revisar' }
] as const;
type Paso = (typeof PASOS)[number]['valor'];

/**
 * Asistente «Nueva ronda» (contenedor) en 4 pasos: datos, personas (lote con vista previa), reparto y revisión. La ronda
 * se guarda como planificada en el primer paso, así se puede salir en cualquier momento sin perder lo hecho; al final
 * se inicia o se deja planificada.
 */
@Component({
    selector: 'app-nueva-ronda',
    imports: [ButtonModule, MessageModule, StepperModule, FormularioRondaComponent, RepartoComponent, SelectorLoteComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './nueva-ronda.html'
})
export class NuevaRondaComponent implements OnInit {
    private readonly rondas = inject(RondasService);
    private readonly equipoServicio = inject(EquipoConsolidacionService);
    private readonly grupos = inject(GruposService);
    private readonly sesion = inject(SesionService);
    private readonly configuracion = inject(ConfiguracionIglesiaService);
    private readonly router = inject(Router);

    protected readonly pasos = PASOS;
    protected readonly paso = signal<Paso>(1);
    protected readonly ronda = signal<Ronda | null>(null);
    protected readonly datos = computed<DatosRonda | null>(() => {
        const ronda = this.ronda();
        return ronda ? { nombre: ronda.nombre, descripcion: ronda.descripcion, fechaInicio: ronda.fechaInicio, fechaFin: ronda.fechaFin } : null;
    });
    protected readonly procesando = signal(false);
    protected readonly error = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});

    protected readonly filtroLote = signal<FiltroLote>(FILTRO_LOTE_INICIAL);
    protected readonly entrarian = signal(0);
    protected readonly loteMax = LIMITES_CONSOLIDACION.loteMax;
    protected readonly opcionesGrupo = signal<readonly Referencia[]>([]);
    protected readonly nombreGrupo = computed(() => this.configuracion.nombresGrupos().nombreGrupoSingular);
    protected readonly ultimoLote = signal<string | null>(null);

    protected readonly equipo = signal<readonly Consolidador[]>([]);
    protected readonly solicitudReparto = signal<SolicitudReparto | null>(null);

    protected readonly conDia = fechaConDia;

    ngOnInit(): void {
        this.equipoServicio.listar().subscribe({ next: (equipo) => this.equipo.set(equipo), error: () => this.equipo.set([]) });
        if (this.sesion.tienePermiso('grupos.ver')) {
            this.grupos.listar().subscribe({
                next: (grupos) => this.opcionesGrupo.set(grupos.filter(({ activo }) => activo).map(({ id, lider }) => ({ id, nombre: `${this.nombreGrupo()} de ${lider.nombre}`, activo: true }))),
                error: () => this.opcionesGrupo.set([])
            });
        }
    }

    /** Solo se puede saltar a un paso ya alcanzable: sin ronda guardada, únicamente al primero. */
    protected irA(valor: number | undefined): void {
        if (valor && (valor === 1 || this.ronda())) {
            this.limpiarError();
            this.paso.set(valor as Paso);
        }
    }

    protected guardarDatos(datos: DatosRonda): void {
        const ronda = this.ronda();
        const peticion: Observable<Ronda> = ronda ? this.rondas.actualizar(ronda.id, datos) : this.rondas.crear(datos);
        this.ejecutar(peticion, (guardada) => {
            this.ronda.set(guardada);
            this.paso.set(2);
        });
    }

    protected agregarLote(): void {
        const ronda = this.ronda()!;
        this.ejecutar(this.rondas.agregarLote(ronda.id, this.filtroLote()), ({ agregadas }) => {
            this.ultimoLote.set(agregadas === 1 ? 'Se agregó 1 persona.' : `Se agregaron ${agregadas} personas.`);
            this.refrescarRonda(() => this.paso.set(3));
        });
    }

    protected repartir(): void {
        const solicitud = this.solicitudReparto();
        if (!solicitud) {
            this.paso.set(4);
            return;
        }
        this.ejecutar(this.rondas.repartir(this.ronda()!.id, solicitud), (ronda) => {
            this.ronda.set(ronda);
            this.paso.set(4);
        });
    }

    protected dejarSinAsignar(): void {
        this.limpiarError();
        this.paso.set(4);
    }

    protected omitirPersonas(): void {
        this.limpiarError();
        this.refrescarRonda(() => this.paso.set(3));
    }

    protected iniciar(): void {
        this.ejecutar(this.rondas.iniciar(this.ronda()!.id), (ronda) => this.abrirRonda(ronda));
    }

    protected guardarPlanificada(): void {
        this.abrirRonda(this.ronda()!);
    }

    protected salir(): void {
        const ronda = this.ronda();
        void (ronda ? this.abrirRonda(ronda) : this.router.navigateByUrl(RUTA_CONSOLIDACION));
    }

    private abrirRonda(ronda: Ronda): void {
        void this.router.navigate([RUTA_CONSOLIDACION, 'rondas', ronda.id]);
    }

    private refrescarRonda(despues: () => void): void {
        this.ejecutar(this.rondas.obtener(this.ronda()!.id), (ronda) => {
            this.ronda.set(ronda);
            despues();
        });
    }

    /** Ejecuta un paso con el estado de «procesando» y deja el error junto al formulario si falla. */
    private ejecutar<T>(peticion: Observable<T>, alTerminar: (resultado: T) => void): void {
        this.procesando.set(true);
        this.limpiarError();
        peticion.pipe(finalize(() => this.procesando.set(false))).subscribe({
            next: alTerminar,
            error: (error: unknown) => {
                this.error.set(detalleDeError(error, 'No se pudo guardar. Intenta de nuevo.'));
                this.erroresCampo.set(erroresPorCampo(error));
            }
        });
    }

    private limpiarError(): void {
        this.error.set(null);
        this.erroresCampo.set({});
    }
}
