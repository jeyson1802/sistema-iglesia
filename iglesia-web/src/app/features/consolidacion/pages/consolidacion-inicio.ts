import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { TablaRondasComponent } from '../components/tabla-rondas/tabla-rondas';
import { TarjetaRondaComponent } from '../components/tarjeta-ronda/tarjeta-ronda';
import { TarjetasEquipoComponent } from '../components/tarjetas-equipo/tarjetas-equipo';
import { Consolidador, ETIQUETA_ESTADO_RONDA, EstadoRonda, FILTRO_RONDAS_INICIAL, FiltroRondas, Ronda } from '../models/consolidacion';
import { RUTA_CONSOLIDACION, RUTA_NUEVA_RONDA } from '../models/rutas-consolidacion';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';

type Pestana = 'rondas' | 'equipo';

/** Rondas en curso que se muestran como tarjetas arriba de la lista. */
const RONDAS_EN_CURSO_VISIBLES = 6;
const ESPERA_BUSQUEDA_MS = 300;

/**
 * Inicio de consolidación (contenedor). «Rondas»: tarjetas de las rondas en curso con su avance y avisos accionables y,
 * debajo, todas las rondas paginadas en el servidor con búsqueda y filtro por estado. «Equipo» (con
 * `consolidacion.administrar`): integrantes con su carga, agregar y activar o desactivar.
 */
@Component({
    selector: 'app-consolidacion-inicio',
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MessageModule,
        SelectModule,
        SkeletonModule,
        TabsModule,
        ToastModule,
        SelectorMiembroComponent,
        TablaRondasComponent,
        TarjetaRondaComponent,
        TarjetasEquipoComponent
    ],
    providers: [ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './consolidacion-inicio.html'
})
export class ConsolidacionInicioComponent implements OnInit {
    private readonly rondas = inject(RondasService);
    private readonly equipoServicio = inject(EquipoConsolidacionService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('consolidacion.administrar'));
    protected readonly pestana = signal<Pestana>('rondas');
    protected readonly estados = (Object.keys(ETIQUETA_ESTADO_RONDA) as EstadoRonda[]).map((estado) => ({ valor: estado, etiqueta: ETIQUETA_ESTADO_RONDA[estado] }));

    protected readonly enCurso = signal<readonly Ronda[]>([]);
    protected readonly cargandoEnCurso = signal(true);

    protected readonly filtro = signal<FiltroRondas>(FILTRO_RONDAS_INICIAL);
    protected readonly texto = signal('');
    protected readonly lista = signal<readonly Ronda[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    /** Sin rondas y sin filtros: la iglesia aún no empezó a usar el módulo. */
    protected readonly sinRondas = computed(() => !this.cargando() && !this.error() && this.total() === 0 && this.filtro().estado === null && !this.filtro().q.trim());

    protected readonly equipo = signal<readonly Consolidador[]>([]);
    protected readonly cargandoEquipo = signal(false);
    protected readonly errorEquipo = signal<string | null>(null);
    protected readonly nuevoIntegrante = signal<OpcionMiembro | null>(null);
    protected readonly agregando = signal(false);
    protected readonly procesando = signal<number | null>(null);

    private carga?: Subscription;
    private espera?: ReturnType<typeof setTimeout>;
    private equipoCargado = false;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.carga?.unsubscribe();
            clearTimeout(this.espera);
        });
    }

    ngOnInit(): void {
        this.cargarEnCurso();
        this.cargar();
    }

    protected cambiarPestana(valor: string | number | undefined): void {
        const pestana = valor === 'equipo' ? 'equipo' : 'rondas';
        this.pestana.set(pestana);
        if (pestana === 'equipo' && !this.equipoCargado) {
            this.cargarEquipo();
        }
    }

    protected cargarEnCurso(): void {
        this.cargandoEnCurso.set(true);
        this.rondas
            .listar({ estado: 'EN_CURSO', q: '' }, 0, RONDAS_EN_CURSO_VISIBLES)
            .pipe(finalize(() => this.cargandoEnCurso.set(false)))
            .subscribe({ next: ({ content }) => this.enCurso.set(content), error: () => this.enCurso.set([]) });
    }

    protected cargar(): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.rondas
            .listar(this.filtro(), this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.lista.set(content);
                    this.total.set(page.totalElements);
                },
                error: () => this.error.set(true)
            });
    }

    protected filtrarEstado(estado: EstadoRonda | null): void {
        this.filtro.update((filtro) => ({ ...filtro, estado }));
        this.pagina.set(0);
        this.cargar();
    }

    protected buscar(texto: string): void {
        this.texto.set(texto);
        clearTimeout(this.espera);
        this.espera = setTimeout(() => {
            this.filtro.update((filtro) => ({ ...filtro, q: texto }));
            this.pagina.set(0);
            this.cargar();
        }, ESPERA_BUSQUEDA_MS);
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargar();
    }

    protected ver(ronda: Ronda): void {
        void this.router.navigate([RUTA_CONSOLIDACION, 'rondas', ronda.id]);
    }

    protected nuevaRonda(): void {
        void this.router.navigateByUrl(RUTA_NUEVA_RONDA);
    }

    protected cargarEquipo(): void {
        this.cargandoEquipo.set(true);
        this.errorEquipo.set(null);
        this.equipoServicio
            .listar()
            .pipe(finalize(() => this.cargandoEquipo.set(false)))
            .subscribe({
                next: (equipo) => {
                    this.equipoCargado = true;
                    this.equipo.set(equipo);
                },
                error: () => this.errorEquipo.set('No se pudo cargar el equipo.')
            });
    }

    protected agregar(): void {
        const persona = this.nuevoIntegrante();
        if (!persona) {
            return;
        }
        this.agregando.set(true);
        this.errorEquipo.set(null);
        this.equipoServicio
            .agregar(persona.id)
            .pipe(finalize(() => this.agregando.set(false)))
            .subscribe({
                next: (consolidador) => {
                    this.reemplazar(consolidador);
                    this.nuevoIntegrante.set(null);
                    this.mensajes.add({ severity: 'success', summary: `${consolidador.nombreCompleto} se unió al equipo`, life: 4000 });
                },
                error: (error: unknown) => this.errorEquipo.set(detalleDeError(error, 'No se pudo agregar al equipo. Intenta de nuevo.'))
            });
    }

    /** Desactivar avisa los casos abiertos que hay que reasignar; activar no es destructivo. */
    protected cambiarEstado(consolidador: Consolidador): void {
        if (!consolidador.activo) {
            this.enviarEstado(consolidador, true);
            return;
        }
        const abiertos = consolidador.casosAbiertos;
        this.confirmacion.confirm({
            header: 'Desactivar del equipo',
            message:
                abiertos > 0
                    ? `${consolidador.nombreCompleto} tiene ${abiertos} ${abiertos === 1 ? 'caso abierto' : 'casos abiertos'}: reasígnalos desde cada ronda. ¿Desactivarlo igual? No recibirá personas nuevas.`
                    : `¿Desactivar a ${consolidador.nombreCompleto}? No recibirá personas nuevas en los repartos.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEstado(consolidador, false)
        });
    }

    private enviarEstado(consolidador: Consolidador, activo: boolean): void {
        this.procesando.set(consolidador.miembroId);
        this.errorEquipo.set(null);
        this.equipoServicio
            .cambiarEstado(consolidador.miembroId, activo)
            .pipe(finalize(() => this.procesando.set(null)))
            .subscribe({
                next: (actualizado) => this.reemplazar(actualizado),
                error: (error: unknown) => this.errorEquipo.set(detalleDeError(error, 'No se pudo cambiar el estado. Intenta de nuevo.'))
            });
    }

    private reemplazar(consolidador: Consolidador): void {
        this.equipo.update((equipo) => (equipo.some(({ miembroId }) => miembroId === consolidador.miembroId) ? equipo.map((actual) => (actual.miembroId === consolidador.miembroId ? consolidador : actual)) : [...equipo, consolidador]));
    }
}
