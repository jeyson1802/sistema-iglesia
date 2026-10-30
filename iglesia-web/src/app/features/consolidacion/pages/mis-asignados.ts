import { ChangeDetectionStrategy, Component, computed, DestroyRef, DOCUMENT, effect, inject, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { MessageService, ToastMessageOptions } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MeterGroupModule } from 'primeng/metergroup';
import { SkeletonModule } from 'primeng/skeleton';
import { ToastModule } from 'primeng/toast';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { CerrarCasoComponent, CierreCaso } from '../components/cerrar-caso/cerrar-caso';
import { HojaContactoComponent } from '../components/hoja-contacto/hoja-contacto';
import { ContactoPedido, SeccionCasosComponent } from '../components/seccion-casos/seccion-casos';
import { CasoResumen, DatosContacto, MedioContacto, porcentaje, ResultadoConsolidacion, ResumenMisCasos, SeccionMisCasos } from '../models/consolidacion';
import { RUTA_CONSOLIDACION } from '../models/rutas-consolidacion';
import { CasosService } from '../services/casos.service';
import { ContactoEnCursoService } from '../services/contacto-en-curso';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';

/** Tiempo para deshacer un contacto recién registrado. */
export const DURACION_DESHACER_MS = 8000;
const CLAVE_TOAST_DESHACER = 'deshacer-contacto';
const ESPERA_BUSQUEDA_MS = 300;

interface EstadoSeccion {
    readonly casos: readonly CasoResumen[];
    readonly total: number;
    readonly pagina: number;
    readonly tamano: number;
    readonly cargando: boolean;
    readonly error: boolean;
}

const ESTADO_INICIAL: EstadoSeccion = { casos: [], total: 0, pagina: 0, tamano: FILAS_POR_PAGINA, cargando: true, error: false };

/** Secciones en orden de urgencia. */
const SECCIONES: readonly { readonly seccion: SeccionMisCasos; readonly titulo: string; readonly icono: string; readonly vacio: string; readonly cantidad: keyof ResumenMisCasos }[] = [
    { seccion: 'PARA_HOY', titulo: 'Para hoy', icono: 'pi pi-bell', vacio: '¡Todo al día! No tienes contactos pendientes para hoy.', cantidad: 'paraHoy' },
    { seccion: 'SIN_CONTACTAR', titulo: 'Sin contactar', icono: 'pi pi-user-plus', vacio: 'Ya contactaste a todas tus personas nuevas.', cantidad: 'sinContactar' },
    { seccion: 'EN_CONTACTO', titulo: 'En contacto', icono: 'pi pi-comments', vacio: 'Aún no tienes seguimientos en curso.', cantidad: 'enContacto' }
];

/** Persona a la que se registra un contacto. */
interface Objetivo {
    readonly casoId: number;
    readonly nombre: string;
}

/**
 * «Mis asignados» (contenedor): la lista de tareas del consolidador. Secciones por urgencia (para hoy, sin contactar,
 * en contacto), cada una paginada en el servidor; búsqueda en todas; registro en un toque al volver de la llamada;
 * «Deshacer» durante 8 segundos y cierre del caso sin salir de la pantalla.
 */
@Component({
    selector: 'app-mis-asignados',
    imports: [FormsModule, ButtonModule, IconFieldModule, InputIconModule, InputTextModule, MeterGroupModule, SkeletonModule, ToastModule, CerrarCasoComponent, HojaContactoComponent, SeccionCasosComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mis-asignados.html'
})
export class MisAsignadosComponent implements OnInit {
    private readonly casosServicio = inject(CasosService);
    private readonly resultadosServicio = inject(ResultadosConsolidacionService);
    private readonly enCurso = inject(ContactoEnCursoService);
    private readonly mensajes = inject(MessageService);
    private readonly router = inject(Router);
    private readonly documento = inject(DOCUMENT);

    protected readonly secciones = SECCIONES;
    protected readonly claveDeshacer = CLAVE_TOAST_DESHACER;
    protected readonly estados = signal<Readonly<Record<SeccionMisCasos, EstadoSeccion>>>({ PARA_HOY: ESTADO_INICIAL, SIN_CONTACTAR: ESTADO_INICIAL, EN_CONTACTO: ESTADO_INICIAL });
    protected readonly resumen = signal<ResumenMisCasos | null>(null);
    protected readonly cargandoResumen = signal(true);
    protected readonly texto = signal('');
    protected readonly q = signal('');
    protected readonly pendientes = this.enCurso.pendientes;
    protected readonly sinAsignados = computed(() => this.resumen()?.asignados === 0 && !this.q());
    protected readonly saludo = computed(() => {
        const paraHoy = this.resumen()?.paraHoy ?? 0;
        return paraHoy === 0 ? null : paraHoy === 1 ? '1 persona para hoy' : `${paraHoy} personas para hoy`;
    });
    protected readonly medidor = computed(() => {
        const resumen = this.resumen();
        return resumen ? [{ label: `Contactadas ${resumen.contactados}`, value: porcentaje(resumen.contactados, resumen.asignados), color: 'var(--p-primary-500)' }] : [];
    });

    protected readonly objetivo = signal<Objetivo | null>(null);
    protected readonly medioInicial = signal<MedioContacto | null>(null);
    protected readonly hojaVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorHoja = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});

    protected readonly cierreVisible = signal(false);
    protected readonly resultados = signal<readonly ResultadoConsolidacion[]>([]);
    protected readonly cerrando = signal(false);
    protected readonly errorCierre = signal<string | null>(null);

    private readonly cargas = new Map<SeccionMisCasos, Subscription>();
    private espera?: ReturnType<typeof setTimeout>;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.cargas.forEach((carga) => carga.unsubscribe());
            clearTimeout(this.espera);
        });
        // Al volver de la aplicación del teléfono se abre sola la hoja de registro con el medio ya elegido.
        effect(() => {
            const contacto = this.enCurso.alVolver();
            if (contacto) {
                untracked(() => {
                    this.enCurso.atendido();
                    this.abrirHoja({ casoId: contacto.casoId, nombre: contacto.nombre }, contacto.medio);
                });
            }
        });
    }

    ngOnInit(): void {
        this.recargar();
    }

    protected estado(seccion: SeccionMisCasos): EstadoSeccion {
        return this.estados()[seccion];
    }

    protected cargarResumen(): void {
        this.cargandoResumen.set(true);
        this.casosServicio
            .resumenMisCasos()
            .pipe(finalize(() => this.cargandoResumen.set(false)))
            .subscribe({ next: (resumen) => this.resumen.set(resumen), error: () => this.resumen.set(null) });
    }

    protected cargar(seccion: SeccionMisCasos): void {
        const { pagina, tamano } = this.estado(seccion);
        this.cargas.get(seccion)?.unsubscribe();
        this.actualizar(seccion, { cargando: true, error: false });
        this.cargas.set(
            seccion,
            this.casosServicio.misCasos(seccion, this.q(), pagina, tamano).subscribe({
                next: ({ content, page }) => this.actualizar(seccion, { casos: content, total: page.totalElements, cargando: false }),
                error: () => this.actualizar(seccion, { cargando: false, error: true })
            })
        );
    }

    protected cambiarPagina(seccion: SeccionMisCasos, pedida: PaginaPedida): void {
        this.actualizar(seccion, { pagina: paginaACargar(pedida, this.estado(seccion).tamano), tamano: pedida.tamano });
        this.cargar(seccion);
    }

    protected buscar(texto: string): void {
        this.texto.set(texto);
        clearTimeout(this.espera);
        this.espera = setTimeout(() => {
            this.q.set(texto.trim());
            SECCIONES.forEach(({ seccion }) => this.actualizar(seccion, { pagina: 0 }));
            SECCIONES.forEach(({ seccion }) => this.cargar(seccion));
        }, ESPERA_BUSQUEDA_MS);
    }

    protected contactar({ caso, medio }: ContactoPedido): void {
        this.enCurso.iniciar({ casoId: caso.id, nombre: caso.persona.nombreCompleto, medio });
    }

    protected registrar(caso: CasoResumen): void {
        this.abrirHoja({ casoId: caso.id, nombre: caso.persona.nombreCompleto }, this.pendientes().get(caso.id)?.medio ?? null);
    }

    protected descartar(caso: CasoResumen): void {
        this.enCurso.resolver(caso.id);
    }

    protected ver(caso: CasoResumen): void {
        void this.router.navigate([RUTA_CONSOLIDACION, 'casos', caso.id]);
    }

    /** Al cerrar la hoja el foco vuelve al botón «Registrar» de la tarjeta que la abrió. */
    protected cambioHoja(visible: boolean): void {
        this.hojaVisible.set(visible);
        if (!visible) {
            const casoId = this.objetivo()?.casoId;
            setTimeout(() => this.documento.querySelector<HTMLElement>(`#registrar-${casoId} button`)?.focus());
        }
    }

    protected guardarContacto(datos: DatosContacto, cerrarDespues = false): void {
        const objetivo = this.objetivo();
        if (!objetivo) {
            return;
        }
        this.guardando.set(true);
        this.errorHoja.set(null);
        this.erroresCampo.set({});
        this.casosServicio
            .registrarContacto(objetivo.casoId, datos)
            .pipe(finalize(() => this.guardando.set(false)))
            .subscribe({
                next: (contacto) => {
                    this.enCurso.resolver(objetivo.casoId);
                    this.hojaVisible.set(false);
                    this.quitarDeLasListas(objetivo.casoId);
                    this.mensajes.add({ key: CLAVE_TOAST_DESHACER, severity: 'success', summary: 'Contacto registrado', detail: objetivo.nombre, life: DURACION_DESHACER_MS, data: { contactoId: contacto.id } });
                    this.recargar();
                    if (cerrarDespues) {
                        this.abrirCierre();
                    }
                },
                error: (error: unknown) => {
                    this.errorHoja.set(detalleDeError(error, 'No se pudo registrar el contacto. Tus datos siguen aquí: intenta de nuevo.'));
                    this.erroresCampo.set(erroresPorCampo(error));
                }
            });
    }

    protected deshacer(mensaje: ToastMessageOptions): void {
        const contactoId = (mensaje.data as { contactoId: number } | undefined)?.contactoId;
        this.mensajes.clear(CLAVE_TOAST_DESHACER);
        if (contactoId === undefined) {
            return;
        }
        this.casosServicio.borrarContacto(contactoId).subscribe({
            next: () => {
                this.mensajes.add({ severity: 'info', summary: 'Contacto deshecho', life: 3000 });
                this.recargar();
            },
            error: (error: unknown) => {
                const detalle = detalleDeError(error, 'No se pudo deshacer el contacto.');
                if (detalle) {
                    this.mensajes.add({ severity: 'error', summary: detalle, life: 5000 });
                }
            }
        });
    }

    protected cerrarCaso({ resultadoId, observacion }: CierreCaso): void {
        const objetivo = this.objetivo();
        if (!objetivo) {
            return;
        }
        this.cerrando.set(true);
        this.errorCierre.set(null);
        this.casosServicio
            .cerrar(objetivo.casoId, resultadoId, observacion)
            .pipe(finalize(() => this.cerrando.set(false)))
            .subscribe({
                next: () => {
                    this.cierreVisible.set(false);
                    this.mensajes.add({ severity: 'success', summary: 'Seguimiento cerrado', detail: objetivo.nombre, life: 4000 });
                    this.recargar();
                },
                error: (error: unknown) => this.errorCierre.set(detalleDeError(error, 'No se pudo cerrar el caso. Intenta de nuevo.'))
            });
    }

    private abrirHoja(objetivo: Objetivo, medio: MedioContacto | null): void {
        this.objetivo.set(objetivo);
        this.medioInicial.set(medio);
        this.errorHoja.set(null);
        this.erroresCampo.set({});
        this.hojaVisible.set(true);
    }

    private abrirCierre(): void {
        this.errorCierre.set(null);
        if (this.resultados().length === 0) {
            this.resultadosServicio.listar().subscribe({ next: (resultados) => this.resultados.set(resultados), error: () => this.errorCierre.set('No se pudieron cargar los resultados. Cierra el diálogo e intenta de nuevo.') });
        }
        this.cierreVisible.set(true);
    }

    /** Respuesta inmediata: la tarjeta sale de su sección al guardar; la recarga la pone donde corresponde. */
    private quitarDeLasListas(casoId: number): void {
        this.estados.update((estados) => {
            const nuevos = { ...estados };
            SECCIONES.forEach(({ seccion }) => (nuevos[seccion] = { ...estados[seccion], casos: estados[seccion].casos.filter(({ id }) => id !== casoId) }));
            return nuevos;
        });
    }

    private recargar(): void {
        this.cargarResumen();
        SECCIONES.forEach(({ seccion }) => this.cargar(seccion));
    }

    private actualizar(seccion: SeccionMisCasos, cambios: Partial<EstadoSeccion>): void {
        this.estados.update((estados) => ({ ...estados, [seccion]: { ...estados[seccion], ...cambios } }));
    }
}
