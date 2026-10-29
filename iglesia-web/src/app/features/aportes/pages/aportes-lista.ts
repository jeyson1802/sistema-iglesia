import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { formatearMonto } from '@/app/shared/pipes/moneda.pipe';
import { AnularAporteComponent } from '../components/anular-aporte/anular-aporte';
import { DetalleAporteComponent } from '../components/detalle-aporte/detalle-aporte';
import { FiltrosAportesComponent } from '../components/filtros-aportes/filtros-aportes';
import { EnvioAporte, FormularioAporteComponent } from '../components/formulario-aporte/formulario-aporte';
import { ResumenAportesComponent } from '../components/resumen-aportes/resumen-aportes';
import { TablaAportesComponent } from '../components/tabla-aportes/tabla-aportes';
import { veAportesDeTodaLaIglesia } from '../models/ambito-aportes';
import { PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { Aporte, APORTES_POR_PAGINA, FiltroAportes, MedioAporte, ResumenAportes } from '../models/aporte';
import { CategoriaAporte } from '../models/configuracion-aportes';
import { filtroAUrl, filtroDesdeUrl } from '../models/filtro-url';
import { AportesService } from '../services/aportes.service';
import { CategoriasAporteService } from '../services/categorias-aporte.service';
import { ConfiguracionAportesService } from '../services/configuracion-aportes.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los aportes.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el aporte. Revisa los datos e intenta de nuevo.';
const MENSAJE_ERROR_ANULAR = 'No se pudo anular el aporte. Intenta de nuevo.';
const DURACION_TOAST_MS = 2500;

/**
 * Aportes de la iglesia (contenedor). Los filtros viven en la URL; el resumen y la página se piden con ellos y el
 * backend aplica el ámbito (un líder solo recibe los suyos y la página se titula «Mis aportes»). Las acciones
 * dependen de los permisos del perfil.
 */
@Component({
    selector: 'app-aportes-lista',
    imports: [ButtonModule, MessageModule, ToastModule, AnularAporteComponent, DetalleAporteComponent, FiltrosAportesComponent, FormularioAporteComponent, ResumenAportesComponent, TablaAportesComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './aportes-lista.html'
})
export class AportesListaComponent {
    private readonly router = inject(Router);
    private readonly ruta = inject(ActivatedRoute);
    private readonly servicio = inject(AportesService);
    private readonly categoriasServicio = inject(CategoriasAporteService);
    private readonly configuracion = inject(ConfiguracionAportesService);
    private readonly sesion = inject(SesionService);
    private readonly mensajes = inject(MessageService);

    protected readonly filtro = toSignal(this.ruta.queryParamMap.pipe(map((parametros) => filtroDesdeUrl(parametros))), { requireSync: true });
    protected readonly moneda = computed(() => this.configuracion.configuracion().moneda);
    protected readonly veTodaLaIglesia = computed(() => veAportesDeTodaLaIglesia(this.sesion.perfilActivo()));
    protected readonly titulo = computed(() => (this.veTodaLaIglesia() ? 'Aportes' : 'Mis aportes'));
    protected readonly puedeRegistrar = computed(() => this.sesion.tienePermiso('aportes.registrar'));
    protected readonly puedeEditar = computed(() => this.sesion.tienePermiso('aportes.editar'));
    protected readonly puedeAnular = computed(() => this.sesion.tienePermiso('aportes.anular'));

    protected readonly categorias = signal<readonly CategoriaAporte[]>([]);
    protected readonly resumen = signal<ResumenAportes | null>(null);
    protected readonly cargandoResumen = signal(true);
    protected readonly aportes = signal<readonly Aporte[]>([]);
    protected readonly totalRegistros = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(APORTES_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);

    /** Nombre del aportante filtrado, tomado de los propios resultados (la URL solo lleva su id). */
    protected readonly nombreMiembro = computed(() => {
        const miembroId = this.filtro().miembroId;
        return this.aportes().find(({ miembro }) => miembro?.id === miembroId)?.miembro?.nombre ?? null;
    });

    protected readonly seleccionado = signal<Aporte | null>(null);
    protected readonly detalleVisible = signal(false);

    protected readonly aporteEditado = signal<Aporte | null>(null);
    protected readonly formularioVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorFormulario = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});
    protected readonly registrados = signal(0);

    protected readonly anularVisible = signal(false);
    protected readonly anulando = signal(false);
    protected readonly errorAnular = signal<string | null>(null);

    private cargaPagina?: Subscription;
    private cargaResumen?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.cargaPagina?.unsubscribe();
            this.cargaResumen?.unsubscribe();
        });
        this.configuracion.asegurarCargada();
        this.cargarCategorias();
        effect(() => {
            const filtro = this.filtro();
            untracked(() => {
                this.cargarResumen(filtro);
                this.cargarPagina(filtro, 0);
            });
        });
    }

    protected filtrar(filtro: FiltroAportes): void {
        this.router.navigate([], { relativeTo: this.ruta, queryParams: filtroAUrl(filtro) });
    }

    protected filtrarCategoria(categoriaId: number): void {
        this.filtrar({ ...this.filtro(), categoriaId });
    }

    protected filtrarMedio(medio: MedioAporte): void {
        this.filtrar({ ...this.filtro(), medio });
    }

    protected recargar(): void {
        this.cargarResumen(this.filtro());
        this.cargarPagina(this.filtro(), this.pagina());
    }

    /** Al cambiar cuántos se ven por página se vuelve a la primera, para no quedar en una página que ya no existe. */
    protected cambiarPagina(pedida: PaginaPedida): void {
        const pagina = paginaACargar(pedida, this.tamanoPagina());
        this.tamanoPagina.set(pedida.tamano);
        this.cargarPagina(this.filtro(), pagina);
    }

    protected ver(aporte: Aporte): void {
        this.seleccionado.set(aporte);
        this.detalleVisible.set(true);
    }

    protected nuevo(): void {
        this.abrirFormulario(null);
    }

    protected editar(aporte: Aporte): void {
        this.detalleVisible.set(false);
        this.abrirFormulario(aporte);
    }

    protected guardar({ datos, seguir }: EnvioAporte): void {
        const editado = this.aporteEditado();
        const peticion: Observable<Aporte> = editado ? this.servicio.actualizar(editado.id, datos) : this.servicio.crear(datos);
        this.guardando.set(true);
        this.errorFormulario.set(null);
        this.erroresCampo.set({});
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (aporte) => {
                this.configuracion.marcarConAportes();
                this.avisar(aporte, editado ? 'corregido' : 'registrado');
                if (seguir) {
                    this.registrados.update((cantidad) => cantidad + 1);
                } else {
                    this.formularioVisible.set(false);
                }
                this.recargar();
            },
            error: (error: unknown) => {
                this.errorFormulario.set(detalleDeError(error, MENSAJE_ERROR_GUARDAR));
                this.erroresCampo.set(erroresPorCampo(error));
            }
        });
    }

    protected pedirAnulacion(aporte: Aporte): void {
        this.detalleVisible.set(false);
        this.seleccionado.set(aporte);
        this.errorAnular.set(null);
        this.anularVisible.set(true);
    }

    protected anular(motivo: string): void {
        const aporte = this.seleccionado();
        if (!aporte) {
            return;
        }
        this.anulando.set(true);
        this.errorAnular.set(null);
        this.servicio
            .anular(aporte.id, motivo)
            .pipe(finalize(() => this.anulando.set(false)))
            .subscribe({
                next: (anulado) => {
                    this.anularVisible.set(false);
                    this.seleccionado.set(anulado);
                    this.mensajes.add({ severity: 'info', summary: 'Aporte anulado', detail: `${formatearMonto(anulado.monto, this.moneda())} ya no cuenta en los totales.`, life: DURACION_TOAST_MS });
                    this.recargar();
                },
                error: (error: unknown) => this.errorAnular.set(detalleDeError(error, MENSAJE_ERROR_ANULAR))
            });
    }

    private abrirFormulario(aporte: Aporte | null): void {
        this.aporteEditado.set(aporte);
        this.errorFormulario.set(null);
        this.erroresCampo.set({});
        this.registrados.set(0);
        this.formularioVisible.set(true);
    }

    private avisar(aporte: Aporte, accion: 'registrado' | 'corregido'): void {
        const quien = aporte.miembro ? `de ${aporte.miembro.nombre}` : 'anónimo';
        this.mensajes.add({ severity: 'success', summary: `Aporte ${accion}`, detail: `${formatearMonto(aporte.monto, this.moneda())} ${quien}`, life: DURACION_TOAST_MS });
    }

    /** Las categorías alimentan el filtro y el formulario: si no cargan, ambos siguen sin opciones de categoría. */
    private cargarCategorias(): void {
        this.categoriasServicio
            .listar()
            .pipe(catchError(() => of([])))
            .subscribe((categorias) => this.categorias.set(categorias));
    }

    private cargarResumen(filtro: FiltroAportes): void {
        this.cargaResumen?.unsubscribe();
        this.cargandoResumen.set(true);
        this.cargaResumen = this.servicio
            .resumen(filtro)
            .pipe(
                catchError(() => of(null)),
                finalize(() => this.cargandoResumen.set(false))
            )
            .subscribe((resumen) => this.resumen.set(resumen));
    }

    /** Una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarPagina(filtro: FiltroAportes, pagina: number): void {
        this.cargaPagina?.unsubscribe();
        this.pagina.set(pagina);
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.cargaPagina = this.servicio
            .listar(filtro, pagina, this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.aportes.set(content);
                    this.totalRegistros.set(page.totalElements);
                },
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }
}
