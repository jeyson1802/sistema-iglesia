import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, forkJoin, map, Observable, of, Subscription } from 'rxjs';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DrawerModule } from 'primeng/drawer';
import { MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Referencia } from '@/app/core/models/referencia';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { AvanceRondaComponent } from '../components/avance-ronda/avance-ronda';
import { FiltrosCasosComponent } from '../components/filtros-casos/filtros-casos';
import { FormularioRondaComponent } from '../components/formulario-ronda/formulario-ronda';
import { MedidorRondaComponent } from '../components/medidor-ronda/medidor-ronda';
import { RepartoComponent } from '../components/reparto/reparto';
import { SelectorLoteComponent } from '../components/selector-lote/selector-lote';
import { TablaCasosComponent } from '../components/tabla-casos/tabla-casos';
import {
    accionPrincipal,
    AvanceRonda,
    CasoResumen,
    Consolidador,
    DatosRonda,
    ETIQUETA_ESTADO_RONDA,
    FILTRO_CASOS_RONDA_INICIAL,
    FILTRO_LOTE_INICIAL,
    FiltroCasosRonda,
    FiltroLote,
    ICONO_ESTADO_RONDA,
    LIMITES_CONSOLIDACION,
    plazoRonda,
    repartirEnPartesIguales,
    Ronda,
    SEVERIDAD_ESTADO_RONDA,
    SolicitudReparto
} from '../models/consolidacion';
import { RUTA_CONSOLIDACION } from '../models/rutas-consolidacion';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';

type Pestana = 'personas' | 'avance';

/**
 * Detalle de una ronda (contenedor). Encabezado con estado, plazo, avance y la acción que sigue; avisos accionables;
 * pestaña «Personas» con filtros, grilla paginada en el servidor y acciones masivas sobre la selección, y pestaña
 * «Avance». Agregar personas, repartir y editar se hacen en paneles sin salir de la pantalla.
 */
@Component({
    selector: 'app-detalle-ronda',
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        DialogModule,
        DrawerModule,
        MenuModule,
        MessageModule,
        SelectModule,
        SkeletonModule,
        TabsModule,
        TagModule,
        ToastModule,
        AvanceRondaComponent,
        FiltrosCasosComponent,
        FormularioRondaComponent,
        MedidorRondaComponent,
        RepartoComponent,
        SelectorLoteComponent,
        TablaCasosComponent
    ],
    providers: [ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './detalle-ronda.html'
})
export class DetalleRondaComponent implements OnInit {
    private readonly rondas = inject(RondasService);
    private readonly equipoServicio = inject(EquipoConsolidacionService);
    private readonly grupos = inject(GruposService);
    private readonly sesion = inject(SesionService);
    private readonly configuracion = inject(ConfiguracionIglesiaService);
    private readonly router = inject(Router);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);

    protected readonly rondaId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('consolidacion.administrar'));
    protected readonly ronda = signal<Ronda | null>(null);
    protected readonly cargandoRonda = signal(true);
    protected readonly errorRonda = signal(false);
    protected readonly editable = computed(() => this.puedeAdministrar() && this.ronda()?.estado !== 'CERRADA');
    protected readonly accion = computed(() => {
        const ronda = this.ronda();
        return ronda && this.puedeAdministrar() ? accionPrincipal(ronda) : null;
    });
    protected readonly plazo = computed(() => (this.ronda()?.estado === 'CERRADA' ? null : plazoRonda(this.ronda()?.fechaFin ?? null)));
    protected readonly datos = computed<DatosRonda | null>(() => {
        const ronda = this.ronda();
        return ronda ? { nombre: ronda.nombre, descripcion: ronda.descripcion, fechaInicio: ronda.fechaInicio, fechaFin: ronda.fechaFin } : null;
    });
    protected readonly menuMas = computed<MenuItem[]>(() => {
        const ronda = this.ronda();
        if (!ronda || !this.puedeAdministrar()) {
            return [];
        }
        const opciones: MenuItem[] = [];
        if (ronda.estado !== 'CERRADA') {
            opciones.push({ label: 'Editar datos', icon: 'pi pi-pencil', command: () => this.abrirEdicion() }, { label: 'Agregar personas', icon: 'pi pi-user-plus', command: () => this.abrirLote() });
        }
        if (ronda.estado === 'PLANIFICADA' && this.accion() !== 'INICIAR' && ronda.personas > 0) {
            opciones.push({ label: 'Iniciar ronda', icon: 'pi pi-play', command: () => this.iniciar() });
        }
        if (ronda.estado === 'EN_CURSO') {
            opciones.push({ separator: true }, { label: 'Cerrar ronda', icon: 'pi pi-lock', command: () => this.confirmarCierre() });
        }
        if (ronda.estado === 'PLANIFICADA') {
            opciones.push({ separator: true }, { label: 'Borrar ronda', icon: 'pi pi-trash', styleClass: 'text-red-600 dark:text-red-400', command: () => this.confirmarBorrado() });
        }
        return opciones;
    });
    protected readonly etiquetaEstado = ETIQUETA_ESTADO_RONDA;
    protected readonly severidadEstado = SEVERIDAD_ESTADO_RONDA;
    protected readonly iconoEstado = ICONO_ESTADO_RONDA;
    protected readonly conDia = fechaConDia;
    protected readonly pestana = signal<Pestana>('personas');

    protected readonly filtro = signal<FiltroCasosRonda>(FILTRO_CASOS_RONDA_INICIAL);
    protected readonly casos = signal<readonly CasoResumen[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargandoCasos = signal(true);
    protected readonly errorCasos = signal(false);
    protected readonly seleccion = signal<CasoResumen[]>([]);
    protected readonly seleccionAbierta = computed(() => this.seleccion().filter(({ estado }) => estado !== 'CERRADO'));

    protected readonly equipo = signal<readonly Consolidador[]>([]);
    protected readonly consolidadores = computed<readonly Referencia[]>(() => this.equipo().map(({ miembroId, nombreCompleto, activo }) => ({ id: miembroId, nombre: nombreCompleto, activo })));
    protected readonly activos = computed(() => this.consolidadores().filter(({ activo }) => activo));
    protected readonly asignarA = signal<number | null>(null);

    protected readonly avance = signal<AvanceRonda | null>(null);
    protected readonly cargandoAvance = signal(false);
    protected readonly errorAvance = signal(false);

    protected readonly procesando = signal(false);
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly loteVisible = signal(false);
    protected readonly filtroLote = signal<FiltroLote>(FILTRO_LOTE_INICIAL);
    protected readonly entrarian = signal(0);
    protected readonly loteMax = LIMITES_CONSOLIDACION.loteMax;
    protected readonly opcionesGrupo = signal<readonly Referencia[]>([]);
    protected readonly nombreGrupo = computed(() => this.configuracion.nombresGrupos().nombreGrupoSingular);
    protected readonly errorPanel = signal<string | null>(null);

    protected readonly repartoVisible = signal(false);
    protected readonly solicitudReparto = signal<SolicitudReparto | null>(null);

    protected readonly edicionVisible = signal(false);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});

    private cargaCasos?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cargaCasos?.unsubscribe());
    }

    ngOnInit(): void {
        this.cargarRonda();
        this.cargarCasos();
        if (this.puedeAdministrar()) {
            this.equipoServicio.listar().subscribe({ next: (equipo) => this.equipo.set(equipo), error: () => this.equipo.set([]) });
            if (this.sesion.tienePermiso('grupos.ver')) {
                this.grupos.listar().subscribe({
                    next: (grupos) => this.opcionesGrupo.set(grupos.filter(({ activo }) => activo).map(({ id, lider }) => ({ id, nombre: `${this.nombreGrupo()} de ${lider.nombre}`, activo: true }))),
                    error: () => this.opcionesGrupo.set([])
                });
            }
        }
    }

    protected cargarRonda(): void {
        this.cargandoRonda.set(true);
        this.errorRonda.set(false);
        this.rondas
            .obtener(this.rondaId)
            .pipe(finalize(() => this.cargandoRonda.set(false)))
            .subscribe({ next: (ronda) => this.ronda.set(ronda), error: () => this.errorRonda.set(true) });
    }

    protected cargarCasos(): void {
        this.cargaCasos?.unsubscribe();
        this.cargandoCasos.set(true);
        this.errorCasos.set(false);
        this.cargaCasos = this.rondas
            .casos(this.rondaId, this.filtro(), this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargandoCasos.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.casos.set(content);
                    this.total.set(page.totalElements);
                },
                error: () => this.errorCasos.set(true)
            });
    }

    protected cargarAvance(): void {
        this.cargandoAvance.set(true);
        this.errorAvance.set(false);
        this.rondas
            .avance(this.rondaId)
            .pipe(finalize(() => this.cargandoAvance.set(false)))
            .subscribe({ next: (avance) => this.avance.set(avance), error: () => this.errorAvance.set(true) });
    }

    protected cambiarPestana(valor: string | number | undefined): void {
        const pestana = valor === 'avance' ? 'avance' : 'personas';
        this.pestana.set(pestana);
        if (pestana === 'avance') {
            this.cargarAvance();
        }
    }

    protected filtrar(filtro: FiltroCasosRonda): void {
        this.filtro.set(filtro);
        this.pagina.set(0);
        this.seleccion.set([]);
        this.cargarCasos();
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargarCasos();
    }

    protected verCaso(caso: CasoResumen): void {
        void this.router.navigate([RUTA_CONSOLIDACION, 'casos', caso.id]);
    }

    protected ejecutarAccion(): void {
        switch (this.accion()) {
            case 'AGREGAR':
                this.abrirLote();
                break;
            case 'REPARTIR':
                this.abrirReparto();
                break;
            case 'INICIAR':
                this.iniciar();
                break;
        }
    }

    protected verSinAsignar(): void {
        this.pestana.set('personas');
        this.filtrar({ ...FILTRO_CASOS_RONDA_INICIAL, sinAsignar: true });
    }

    protected abrirLote(): void {
        this.errorPanel.set(null);
        this.filtroLote.set(FILTRO_LOTE_INICIAL);
        this.loteVisible.set(true);
    }

    protected agregarLote(): void {
        this.ejecutarEnPanel(this.rondas.agregarLote(this.rondaId, this.filtroLote()), ({ agregadas }) => {
            this.loteVisible.set(false);
            this.avisar(agregadas === 1 ? 'Se agregó 1 persona' : `Se agregaron ${agregadas} personas`);
        });
    }

    protected abrirReparto(): void {
        this.errorPanel.set(null);
        this.solicitudReparto.set(null);
        this.repartoVisible.set(true);
    }

    protected repartir(): void {
        const solicitud = this.solicitudReparto();
        if (solicitud) {
            this.ejecutarEnPanel(this.rondas.repartir(this.rondaId, solicitud), () => {
                this.repartoVisible.set(false);
                this.avisar('Reparto guardado');
            });
        }
    }

    protected abrirEdicion(): void {
        this.errorPanel.set(null);
        this.erroresCampo.set({});
        this.edicionVisible.set(true);
    }

    protected guardarDatos(datos: DatosRonda): void {
        this.procesando.set(true);
        this.errorPanel.set(null);
        this.rondas
            .actualizar(this.rondaId, datos)
            .pipe(finalize(() => this.procesando.set(false)))
            .subscribe({
                next: (ronda) => {
                    this.ronda.set(ronda);
                    this.edicionVisible.set(false);
                    this.avisar('Datos guardados');
                },
                error: (error: unknown) => {
                    this.errorPanel.set(detalleDeError(error, 'No se pudieron guardar los datos. Intenta de nuevo.'));
                    this.erroresCampo.set(erroresPorCampo(error));
                }
            });
    }

    protected iniciar(): void {
        this.ejecutar(this.rondas.iniciar(this.rondaId), (ronda) => {
            this.ronda.set(ronda);
            this.avisar('Ronda iniciada: tu equipo ya ve a sus personas');
        });
    }

    protected asignarSeleccion(): void {
        const consolidadorId = this.asignarA();
        const ids = this.seleccionAbierta().map(({ id }) => id);
        if (consolidadorId === null || ids.length === 0) {
            return;
        }
        this.ejecutar(this.rondas.repartir(this.rondaId, { consolidadorIds: [consolidadorId], soloSinAsignar: false, asignaciones: ids.map((casoId) => ({ casoId, consolidadorId })) }), () => {
            this.avisar(ids.length === 1 ? 'Persona asignada' : `${ids.length} personas asignadas`);
            this.asignarA.set(null);
            this.refrescar();
        });
    }

    protected repartirSeleccion(): void {
        const asignaciones = repartirEnPartesIguales(
            this.seleccionAbierta().map(({ id }) => id),
            this.equipo()
        );
        if (asignaciones.length === 0) {
            this.errorAccion.set('Agrega integrantes activos al equipo para repartir.');
            return;
        }
        const consolidadorIds = [...new Set(asignaciones.map(({ consolidadorId }) => consolidadorId))];
        this.ejecutar(this.rondas.repartir(this.rondaId, { consolidadorIds, soloSinAsignar: false, asignaciones }), () => {
            this.avisar(`${asignaciones.length} personas repartidas en partes iguales`);
            this.refrescar();
        });
    }

    protected quitarSeleccion(): void {
        const seleccion = this.seleccion();
        this.confirmacion.confirm({
            header: 'Quitar del lote',
            message: `¿Quitar ${seleccion.length === 1 ? `a ${seleccion[0].persona.nombreCompleto}` : `a ${seleccion.length} personas`} de esta ronda? Quien ya tiene contactos registrados no se puede quitar.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Quitar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.quitar(seleccion)
        });
    }

    private quitar(seleccion: readonly CasoResumen[]): void {
        const peticiones = seleccion.map(({ id }) =>
            this.rondas.quitarCaso(this.rondaId, id).pipe(
                map(() => true),
                catchError(() => of(false))
            )
        );
        this.procesando.set(true);
        this.errorAccion.set(null);
        forkJoin(peticiones)
            .pipe(finalize(() => this.procesando.set(false)))
            .subscribe((resultados) => {
                const quitadas = resultados.filter(Boolean).length;
                const fallidas = resultados.length - quitadas;
                if (quitadas > 0) {
                    this.avisar(quitadas === 1 ? 'Se quitó 1 persona' : `Se quitaron ${quitadas} personas`);
                }
                if (fallidas > 0) {
                    this.errorAccion.set(fallidas === 1 ? 'No se pudo quitar a 1 persona: ya tiene contactos registrados.' : `No se pudo quitar a ${fallidas} personas: ya tienen contactos registrados.`);
                }
                this.refrescar();
            });
    }

    private confirmarCierre(): void {
        const abiertos = (this.ronda()?.pendientes ?? 0) + (this.ronda()?.enContacto ?? 0);
        this.confirmacion.confirm({
            header: 'Cerrar ronda',
            message: abiertos > 0 ? `Quedan ${abiertos} casos abiertos: se cerrarán como «No concluido» y la ronda pasará a ser de solo lectura. ¿Cerrarla?` : '¿Cerrar la ronda? Pasará a ser de solo lectura.',
            icon: 'pi pi-lock',
            acceptLabel: 'Cerrar ronda',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () =>
                this.ejecutar(this.rondas.cerrar(this.rondaId), (ronda) => {
                    this.ronda.set(ronda);
                    this.avisar('Ronda cerrada');
                    this.cargarCasos();
                })
        });
    }

    private confirmarBorrado(): void {
        this.confirmacion.confirm({
            header: 'Borrar ronda',
            message: `¿Borrar «${this.ronda()?.nombre}» y su lote? Esta acción no se puede deshacer.`,
            icon: 'pi pi-trash',
            acceptLabel: 'Borrar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.ejecutar(this.rondas.borrar(this.rondaId), () => void this.router.navigateByUrl(RUTA_CONSOLIDACION))
        });
    }

    private ejecutar<T>(peticion: Observable<T>, alTerminar: (resultado: T) => void): void {
        this.procesando.set(true);
        this.errorAccion.set(null);
        peticion.pipe(finalize(() => this.procesando.set(false))).subscribe({
            next: alTerminar,
            error: (error: unknown) => this.errorAccion.set(detalleDeError(error, 'No se pudo completar la acción. Intenta de nuevo.'))
        });
    }

    private ejecutarEnPanel<T>(peticion: Observable<T>, alTerminar: (resultado: T) => void): void {
        this.procesando.set(true);
        this.errorPanel.set(null);
        peticion.pipe(finalize(() => this.procesando.set(false))).subscribe({
            next: (resultado) => {
                alTerminar(resultado);
                this.refrescar();
            },
            error: (error: unknown) => this.errorPanel.set(detalleDeError(error, 'No se pudo guardar. Intenta de nuevo.'))
        });
    }

    /** Tras un cambio masivo se recargan los contadores, la grilla y, si está a la vista, el avance. */
    private refrescar(): void {
        this.seleccion.set([]);
        this.cargarRonda();
        this.cargarCasos();
        if (this.pestana() === 'avance') {
            this.cargarAvance();
        }
        if (this.puedeAdministrar()) {
            this.equipoServicio.listar().subscribe({ next: (equipo) => this.equipo.set(equipo), error: () => undefined });
        }
    }

    private avisar(resumen: string): void {
        this.mensajes.add({ severity: 'success', summary: resumen, life: 4000 });
    }
}
