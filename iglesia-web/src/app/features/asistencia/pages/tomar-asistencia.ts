import { ChangeDetectionStrategy, Component, computed, DestroyRef, DOCUMENT, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, Observable, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { AyudaEquiposComponent } from '../components/ayuda-equipos/ayuda-equipos';
import { EntradaLectorComponent } from '../components/entrada-lector/entrada-lector';
import { EscanerCamaraComponent } from '../components/escaner-camara/escaner-camara';
import { CambioPresencia, ListaIntegrantesComponent } from '../components/lista-integrantes/lista-integrantes';
import { MarcadoManualComponent } from '../components/marcado-manual/marcado-manual';
import { ResultadoMarcaComponent } from '../components/resultado-marca/resultado-marca';
import { UltimasMarcasComponent } from '../components/ultimas-marcas/ultimas-marcas';
import { ETIQUETA_TIPO_REUNION, horaCorta, LIMITES_ASISTENCIA, Marca, MiembroReunion, Reunion, SEVERIDAD_TIPO_REUNION } from '../models/asistencia';
import { ModoAsistencia, modoInicial, OPCIONES_MODO, recordarModo } from '../models/modo-asistencia';
import { DURACION_RESULTADO_MS, NO_ES_CODIGO, ResultadoMarca, resultadoDeError, resultadoDeMarca } from '../models/resultado-marca';
import { RUTA_ASISTENCIA } from '../models/rutas-asistencia';
import { AsistenciasService } from '../services/asistencias.service';
import { AvisosMarca } from '../services/avisos-marca';
import { EstadoConexion } from '../services/estado-conexion';
import { PantallaEncendida } from '../services/pantalla-encendida';
import { ProcesadorCodigos } from '../services/procesador-codigos';
import { ReunionesService } from '../services/reuniones.service';

const MENSAJE_ERROR_REUNION = 'No se encontró la reunión o no tienes acceso a ella.';
const MENSAJE_ERROR_MARCA = 'No se pudo registrar la asistencia. Intenta de nuevo.';
const MENSAJE_ERROR_QUITAR = 'No se pudo quitar la asistencia. Intenta de nuevo.';
const MENSAJE_ERROR_BUSQUEDA = 'No se pudo buscar. Intenta de nuevo.';
/** Celulares y tabletas: pantalla táctil como puntero principal. */
const CONSULTA_CELULAR = '(pointer: coarse)';

/**
 * Tomar asistencia (contenedor): una reunión con tres modos (cámara, lector y manual), el contador en vivo, la franja
 * con el resultado de cada lectura (con sonido y vibración) y las últimas marcas con «Deshacer». En los grupos, la
 * lista de integrantes con interruptores. Sin conexión se pausa. La pantalla se mantiene encendida.
 */
@Component({
    selector: 'app-tomar-asistencia',
    imports: [
        FormsModule,
        RouterLink,
        ButtonModule,
        ConfirmDialogModule,
        SelectButtonModule,
        SkeletonModule,
        TagModule,
        ToastModule,
        AyudaEquiposComponent,
        EntradaLectorComponent,
        EscanerCamaraComponent,
        ListaIntegrantesComponent,
        MarcadoManualComponent,
        ResultadoMarcaComponent,
        UltimasMarcasComponent
    ],
    providers: [ConfirmationService, MessageService, ProcesadorCodigos, PantallaEncendida, EstadoConexion],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tomar-asistencia.html'
})
export class TomarAsistenciaComponent implements OnInit {
    private readonly reunionesServicio = inject(ReunionesService);
    private readonly asistenciasServicio = inject(AsistenciasService);
    private readonly procesador = inject(ProcesadorCodigos);
    private readonly avisos = inject(AvisosMarca);
    private readonly pantalla = inject(PantallaEncendida);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);
    private readonly ventana = inject(DOCUMENT).defaultView;
    private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

    protected readonly enLinea = inject(EstadoConexion).enLinea;
    protected readonly rutaInicio = RUTA_ASISTENCIA;
    protected readonly opcionesModo = [...OPCIONES_MODO];

    protected readonly reunion = signal<Reunion | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal<string | null>(null);
    protected readonly total = signal(0);
    protected readonly modo = signal<ModoAsistencia>('MANUAL');
    protected readonly resultado = signal<ResultadoMarca | null>(null);
    protected readonly ultimas = signal<readonly Marca[]>([]);
    protected readonly deshaciendo = signal<number | null>(null);
    protected readonly ayudaVisible = signal(false);

    protected readonly resultados = signal<readonly MiembroReunion[]>([]);
    protected readonly buscando = signal(false);
    protected readonly procesandoManual = signal<number | null>(null);
    protected readonly integrantes = signal<readonly MiembroReunion[]>([]);
    protected readonly cargandoIntegrantes = signal(false);
    protected readonly procesandoIntegrantes = signal<ReadonlySet<number>>(new Set());

    protected readonly esGrupo = computed(() => this.reunion()?.tipo === 'GRUPO');
    protected readonly tipo = computed(() => {
        const reunion = this.reunion();
        return reunion ? { etiqueta: ETIQUETA_TIPO_REUNION[reunion.tipo], severidad: SEVERIDAD_TIPO_REUNION[reunion.tipo] } : null;
    });
    protected readonly cuando = computed(() => {
        const reunion = this.reunion();
        if (!reunion) {
            return '';
        }
        const hora = horaCorta(reunion.hora);
        return hora ? `${fechaConDia(reunion.fecha)} · ${hora}` : fechaConDia(reunion.fecha);
    });

    private temporizador?: ReturnType<typeof setTimeout>;
    private busqueda?: Subscription;

    ngOnInit(): void {
        this.destroyRef.onDestroy(() => {
            clearTimeout(this.temporizador);
            this.busqueda?.unsubscribe();
        });
        this.pantalla.mantener();
        this.reunionesServicio
            .obtener(this.id)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (reunion) => {
                    this.reunion.set(reunion);
                    this.total.set(reunion.totalAsistentes);
                    this.modo.set(modoInicial(reunion.tipo, this.esCelular()));
                    if (reunion.tipo === 'GRUPO') {
                        this.cargarIntegrantes();
                    }
                },
                error: () => this.error.set(MENSAJE_ERROR_REUNION)
            });
    }

    protected elegirModo(modo: ModoAsistencia | null): void {
        const reunion = this.reunion();
        if (!modo || !reunion) {
            return;
        }
        this.modo.set(modo);
        this.resultado.set(null);
        recordarModo(reunion.tipo, modo);
    }

    protected terminar(): void {
        this.router.navigate([RUTA_ASISTENCIA]);
    }

    /** Lo leído por la cámara o el lector. Sin conexión no se envía nada. */
    protected leer(texto: string): void {
        if (!this.enLinea()) {
            return;
        }
        const lectura = this.procesador.procesar(texto);
        if (lectura.tipo === 'invalida') {
            this.mostrar(resultadoDeError(NO_ES_CODIGO));
        } else if (lectura.tipo === 'enviar') {
            this.asistenciasServicio.marcarPorCodigo(this.id, lectura.codigo).subscribe({
                next: (marca) => this.alMarcar(marca),
                error: (error: unknown) => this.mostrar(resultadoDeError(detalleDeError(error, MENSAJE_ERROR_MARCA) ?? MENSAJE_ERROR_MARCA))
            });
        }
    }

    protected deshacer(marca: Marca): void {
        this.deshaciendo.set(marca.miembroId);
        this.asistenciasServicio
            .quitar(this.id, marca.miembroId)
            .pipe(finalize(() => this.deshaciendo.set(null)))
            .subscribe({
                next: () => {
                    this.alQuitar(marca.miembroId);
                    this.procesador.olvidar();
                },
                error: (error: unknown) => this.avisarError(detalleDeError(error, MENSAJE_ERROR_QUITAR))
            });
    }

    protected buscar(texto: string): void {
        this.busqueda?.unsubscribe();
        if (!texto) {
            this.resultados.set([]);
            this.buscando.set(false);
            return;
        }
        this.buscando.set(true);
        this.busqueda = this.asistenciasServicio
            .buscarMiembros(this.id, texto)
            .pipe(finalize(() => this.buscando.set(false)))
            .subscribe({ next: (miembros) => this.resultados.set(miembros), error: (error: unknown) => this.avisarError(detalleDeError(error, MENSAJE_ERROR_BUSQUEDA)) });
    }

    protected marcarManual(miembro: MiembroReunion): void {
        this.procesandoManual.set(miembro.miembroId);
        this.asistenciasServicio
            .marcarMiembro(this.id, miembro.miembroId)
            .pipe(finalize(() => this.procesandoManual.set(null)))
            .subscribe({ next: (marca) => this.alMarcar(marca), error: (error: unknown) => this.avisarError(detalleDeError(error, MENSAJE_ERROR_MARCA)) });
    }

    protected quitarManual(miembro: MiembroReunion): void {
        this.confirmacion.confirm({
            header: 'Quitar asistencia',
            message: `¿Quitar la asistencia de ${miembro.nombreCompleto} en esta reunión?`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Quitar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => {
                this.procesandoManual.set(miembro.miembroId);
                this.asistenciasServicio
                    .quitar(this.id, miembro.miembroId)
                    .pipe(finalize(() => this.procesandoManual.set(null)))
                    .subscribe({ next: () => this.alQuitar(miembro.miembroId), error: (error: unknown) => this.avisarError(detalleDeError(error, MENSAJE_ERROR_QUITAR)) });
            }
        });
    }

    /** Cada interruptor guarda al instante; si falla, vuelve a como estaba y lo avisa. */
    protected cambiarPresencia({ miembro, presente }: CambioPresencia): void {
        const id = miembro.miembroId;
        this.actualizarPresencia(id, presente);
        this.procesandoIntegrantes.update((ids) => new Set([...ids, id]));
        const peticion: Observable<Marca | void> = presente ? this.asistenciasServicio.marcarMiembro(this.id, id) : this.asistenciasServicio.quitar(this.id, id);
        peticion.pipe(finalize(() => this.procesandoIntegrantes.update((ids) => new Set([...ids].filter((actual) => actual !== id))))).subscribe({
            next: (marca) => (marca ? this.alMarcar(marca) : this.alQuitar(id)),
            error: (error: unknown) => {
                this.actualizarPresencia(id, !presente);
                this.avisarError(detalleDeError(error, presente ? MENSAJE_ERROR_MARCA : MENSAJE_ERROR_QUITAR));
            }
        });
    }

    protected marcarTodos(): void {
        this.integrantes()
            .filter(({ presente }) => !presente)
            .forEach((miembro) => this.cambiarPresencia({ miembro, presente: true }));
    }

    private alMarcar(marca: Marca): void {
        this.mostrar(resultadoDeMarca(marca));
        this.total.set(marca.totalAsistentes);
        this.actualizarPresencia(marca.miembroId, true);
        if (!marca.yaRegistrado) {
            this.ultimas.update((marcas) => [marca, ...marcas.filter(({ miembroId }) => miembroId !== marca.miembroId)].slice(0, LIMITES_ASISTENCIA.ultimasMarcas));
        }
    }

    private alQuitar(miembroId: number): void {
        this.total.update((total) => Math.max(0, total - 1));
        this.ultimas.update((marcas) => marcas.filter((marca) => marca.miembroId !== miembroId));
        this.actualizarPresencia(miembroId, false);
    }

    private actualizarPresencia(miembroId: number, presente: boolean): void {
        const cambiar = (miembros: readonly MiembroReunion[]) => miembros.map((miembro) => (miembro.miembroId === miembroId ? { ...miembro, presente } : miembro));
        this.integrantes.update(cambiar);
        this.resultados.update(cambiar);
    }

    /** La franja verde o ámbar se va sola; la roja se queda hasta la siguiente lectura. */
    private mostrar(resultado: ResultadoMarca): void {
        clearTimeout(this.temporizador);
        this.resultado.set(resultado);
        this.avisos.avisar(resultado.tipo);
        if (resultado.tipo !== 'error') {
            this.temporizador = setTimeout(() => this.resultado.set(null), DURACION_RESULTADO_MS);
        }
    }

    private avisarError(detalle: string | null): void {
        if (detalle) {
            this.mensajes.add({ severity: 'error', summary: 'No se guardó', detail: detalle });
        }
    }

    private cargarIntegrantes(): void {
        this.cargandoIntegrantes.set(true);
        this.asistenciasServicio
            .integrantes(this.id)
            .pipe(finalize(() => this.cargandoIntegrantes.set(false)))
            .subscribe({ next: (integrantes) => this.integrantes.set(integrantes), error: (error: unknown) => this.avisarError(detalleDeError(error, MENSAJE_ERROR_REUNION)) });
    }

    private esCelular(): boolean {
        return this.ventana?.matchMedia?.(CONSULTA_CELULAR).matches ?? false;
    }
}
