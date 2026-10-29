import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, map, Observable, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { esErrorGlobal } from '@/app/core/errores/clasificar-error';
import { erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { FiltroIglesias, Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { FiltrosIglesiasComponent } from '../components/filtros-iglesias/filtros-iglesias';
import { FormularioAdministradorComponent } from '../components/formulario-administrador/formulario-administrador';
import { FormularioIglesiaComponent } from '../components/formulario-iglesia/formulario-iglesia';
import { TablaIglesiasComponent } from '../components/tabla-iglesias/tabla-iglesias';
import { AltaIglesia, NuevoAdministrador } from '../models/administrador-iglesia';
import { filtroIglesiasAUrl, filtroIglesiasDesdeUrl } from '../models/filtro-url';
import { AdministradoresIglesiaService } from '../services/administradores-iglesia.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar las iglesias.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar la iglesia. Intenta de nuevo.';
const MENSAJE_ERROR_ADMINISTRADOR = 'No se pudo crear el administrador. Intenta de nuevo.';
const MENSAJE_ERROR_ENTRAR = 'No se pudo entrar a la iglesia. Intenta de nuevo.';
/** Detalle del aviso de alta parcial cuando el fallo ya lo avisó el toast global (5xx o sin conexión). */
const DETALLE_ERROR_INESPERADO = 'ocurrió un error inesperado';
/** Página a la que se llega al entrar a una iglesia: el inicio del administrador del sistema no depende de ella. */
const RUTA_AL_ENTRAR = '/usuarios';

/** Errores de negocio cuyo `detail` (ProblemDetail, en español) se muestra tal cual. */
const ESTADOS_CON_DETALLE: readonly number[] = [HttpStatusCode.BadRequest, HttpStatusCode.Forbidden, HttpStatusCode.NotFound, HttpStatusCode.Conflict];

/**
 * Catálogo de iglesias del administrador del sistema (contenedor): filtros en la URL y página pedida al servidor, alta
 * de una iglesia con su primer administrador (dos llamadas, a iglesia-api y a iglesia-seguridad), creación de
 * administradores y entrada a una iglesia para operarla. La edición completa vive en `/iglesias/:id`.
 */
@Component({
    selector: 'app-iglesias-lista',
    imports: [ButtonModule, ToastModule, FiltrosIglesiasComponent, FormularioAdministradorComponent, FormularioIglesiaComponent, TablaIglesiasComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './iglesias-lista.html'
})
export class IglesiasListaComponent {
    private readonly servicio = inject(IglesiasService);
    private readonly administradores = inject(AdministradoresIglesiaService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly ruta = inject(ActivatedRoute);
    private readonly mensajes = inject(MessageService);

    protected readonly filtro = toSignal(this.ruta.queryParamMap.pipe(map((parametros) => filtroIglesiasDesdeUrl(parametros))), { requireSync: true });
    protected readonly iglesias = signal<readonly Iglesia[]>([]);
    protected readonly totalRegistros = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    private carga?: Subscription;

    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});

    protected readonly iglesiaAdministrador = signal<Iglesia | null>(null);
    protected readonly administradorVisible = signal(false);
    protected readonly creandoAdministrador = signal(false);
    protected readonly errorAdministrador = signal<string | null>(null);

    protected readonly entrando = signal(false);

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('iglesias.administrar'));
    protected readonly iglesiaActualId = computed(() => (this.sesion.actuandoEnIglesia() ? (this.sesion.usuarioActual()?.iglesiaId ?? null) : null));

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        // Cada filtro nuevo (de la URL) vuelve a la primera página.
        effect(() => {
            const filtro = this.filtro();
            untracked(() => this.cargarPagina(filtro, 0));
        });
    }

    protected filtrar(filtro: FiltroIglesias): void {
        this.router.navigate([], { relativeTo: this.ruta, queryParams: filtroIglesiasAUrl(filtro) });
    }

    /** Al cambiar cuántas se ven por página se vuelve a la primera, para no quedar en una página que ya no existe. */
    protected cambiarPagina(pedida: PaginaPedida): void {
        const pagina = paginaACargar(pedida, this.tamanoPagina());
        this.tamanoPagina.set(pedida.tamano);
        this.cargarPagina(this.filtro(), pagina);
    }

    /** Vuelve a pedir la página actual (también tras un error). */
    protected cargar(): void {
        this.cargarPagina(this.filtro(), this.pagina());
    }

    protected nueva(): void {
        this.errorDialogo.set(null);
        this.erroresCampo.set({});
        this.dialogoVisible.set(true);
    }

    protected abrirAdministrador(iglesia: Iglesia): void {
        this.errorAdministrador.set(null);
        this.iglesiaAdministrador.set(iglesia);
        this.administradorVisible.set(true);
    }

    /**
     * Alta en dos pasos. Si falla la iglesia, el error se muestra en el diálogo; si la iglesia se creó pero no su
     * administrador, se cierra el diálogo (reintentar duplicaría la iglesia) y se indica cómo completarla.
     */
    protected crear({ datos, administrador }: AltaIglesia): void {
        this.guardando.set(true);
        this.errorDialogo.set(null);
        this.erroresCampo.set({});
        this.servicio.crear(datos).subscribe({
            next: (iglesia) =>
                this.administradores
                    .crear(iglesia.id, administrador)
                    .pipe(finalize(() => this.terminarAlta()))
                    .subscribe({
                        next: () => this.avisar('success', 'Iglesia creada', `${iglesia.nombreComercial} ya tiene a ${administrador.correo.toLowerCase()} como administrador.`),
                        error: (error: unknown) => {
                            const detalle = mensajeDeError(error, MENSAJE_ERROR_ADMINISTRADOR) ?? DETALLE_ERROR_INESPERADO;
                            this.avisar('warn', 'Iglesia creada sin administrador', `La iglesia se creó, pero no su administrador: ${detalle}. Usa "Crear administrador".`, true);
                        }
                    }),
            error: (error: unknown) => {
                this.guardando.set(false);
                this.errorDialogo.set(mensajeDeError(error, MENSAJE_ERROR_GUARDAR));
                this.erroresCampo.set(erroresPorCampo(error));
            }
        });
    }

    protected crearAdministrador(administrador: NuevoAdministrador): void {
        const iglesia = this.iglesiaAdministrador();
        if (!iglesia) {
            return;
        }
        this.ejecutarAdministrador(this.administradores.crear(iglesia.id, administrador), () => this.avisar('success', 'Administrador creado', `${administrador.correo.toLowerCase()} deberá cambiar su contraseña temporal al iniciar sesión.`));
    }

    protected entrar(iglesia: Iglesia): void {
        this.entrando.set(true);
        this.sesion
            .entrarAIglesia(iglesia.id)
            .pipe(finalize(() => this.entrando.set(false)))
            .subscribe({
                next: () => void this.router.navigateByUrl(RUTA_AL_ENTRAR),
                error: (error: unknown) => {
                    const mensaje = mensajeDeError(error, MENSAJE_ERROR_ENTRAR);
                    if (mensaje) {
                        this.avisar('error', 'No se pudo entrar', mensaje);
                    }
                }
            });
    }

    private ejecutarAdministrador(peticion: Observable<unknown>, alCrear: () => void): void {
        this.creandoAdministrador.set(true);
        this.errorAdministrador.set(null);
        peticion.pipe(finalize(() => this.creandoAdministrador.set(false))).subscribe({
            next: () => {
                this.administradorVisible.set(false);
                alCrear();
            },
            error: (error: unknown) => this.errorAdministrador.set(mensajeDeError(error, MENSAJE_ERROR_ADMINISTRADOR))
        });
    }

    /** La iglesia ya existe pase lo que pase con su administrador: se cierra el diálogo y se recarga la tabla. */
    private terminarAlta(): void {
        this.guardando.set(false);
        this.dialogoVisible.set(false);
        this.cargar();
    }

    /** Una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarPagina(filtro: FiltroIglesias, pagina: number): void {
        this.carga?.unsubscribe();
        this.pagina.set(pagina);
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.carga = this.servicio
            .listar(filtro, pagina, this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.iglesias.set(content);
                    this.totalRegistros.set(page.totalElements);
                },
                // La pantalla no sirve sin datos: el estado de error con «Reintentar» se muestra siempre.
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }

    private avisar(severity: 'success' | 'warn' | 'error', summary: string, detail: string, fijo = false): void {
        this.mensajes.add({ severity, summary, detail, sticky: fijo });
    }
}

/**
 * Los 400, 403, 404 y 409 traen en `detail` el motivo en español (p. ej. el nombre comercial o el correo repetido). Los 5xx y la
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
