import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Referencia } from '@/app/core/models/referencia';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { EtiquetasAlertaComponent } from '@/app/features/asistencia/components/etiquetas-alerta/etiquetas-alerta';
import { IndicadoresAsistenciaComponent } from '@/app/features/asistencia/components/indicadores-asistencia/indicadores-asistencia';
import { SemanasAsistenciaComponent } from '@/app/features/asistencia/components/semanas-asistencia/semanas-asistencia';
import { ETIQUETA_CONDICION } from '@/app/features/miembros/models/miembro';
import { BotonesContactoComponent, MedioBoton } from '@/app/shared/components/botones-contacto/botones-contacto';
import { edadDesde, fechaConDia } from '@/app/shared/utils/fecha';
import { tieneTelefono } from '@/app/shared/utils/telefono';
import { CerrarCasoComponent, CierreCaso } from '../components/cerrar-caso/cerrar-caso';
import { HojaContactoComponent } from '../components/hoja-contacto/hoja-contacto';
import { IteracionesAnterioresComponent } from '../components/iteraciones-anteriores/iteraciones-anteriores';
import { LineaTiempoComponent } from '../components/linea-tiempo/linea-tiempo';
import { CasoDetalle, Contacto, DatosContacto, ETIQUETA_ESTADO_CASO, frasesAntesDeContactar, iniciales, MedioContacto, ResultadoConsolidacion, SEVERIDAD_ESTADO_CASO, textoIteracion, textoProximo } from '../models/consolidacion';
import { RUTA_CONSOLIDACION, RUTA_MIS_ASIGNADOS } from '../models/rutas-consolidacion';
import { CasosService } from '../services/casos.service';
import { ContactoEnCursoService } from '../services/contacto-en-curso';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';

/** Tiempo que se muestra «Copiado» tras copiar el teléfono. */
const AVISO_COPIADO_MS = 2000;
const URL_MAPAS = 'https://www.google.com/maps/search/?api=1&query=';

/**
 * Caso de consolidación (contenedor): quién es la persona, «Antes de contactar» en lenguaje natural, datos para
 * contactar, asistencia, línea de tiempo (contactos y asistencias juntos) e iteraciones anteriores. Registra, corrige y
 * borra contactos, cierra, reabre y (el coordinador) reasigna. En el celular, las acciones quedan en una barra fija.
 */
@Component({
    selector: 'app-caso-consolidacion',
    imports: [
        FormsModule,
        RouterLink,
        ButtonModule,
        ConfirmDialogModule,
        DialogModule,
        MenuModule,
        MessageModule,
        SelectModule,
        SkeletonModule,
        TagModule,
        ToastModule,
        EtiquetasAlertaComponent,
        IndicadoresAsistenciaComponent,
        SemanasAsistenciaComponent,
        BotonesContactoComponent,
        CerrarCasoComponent,
        HojaContactoComponent,
        IteracionesAnterioresComponent,
        LineaTiempoComponent
    ],
    providers: [ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './caso-consolidacion.html'
})
export class CasoConsolidacionComponent implements OnInit {
    private readonly casos = inject(CasosService);
    private readonly resultadosServicio = inject(ResultadosConsolidacionService);
    private readonly equipoServicio = inject(EquipoConsolidacionService);
    private readonly enCurso = inject(ContactoEnCursoService);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);

    protected readonly casoId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
    protected readonly caso = signal<CasoDetalle | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal<string | null>(null);
    protected readonly errorAccion = signal<string | null>(null);

    protected readonly rutaMisAsignados = RUTA_MIS_ASIGNADOS;
    protected readonly rutaConsolidacion = RUTA_CONSOLIDACION;
    protected readonly conDia = fechaConDia;
    protected readonly proximo = textoProximo;
    protected readonly etiquetaEstado = ETIQUETA_ESTADO_CASO;
    protected readonly severidadEstado = SEVERIDAD_ESTADO_CASO;

    protected readonly iniciales = computed(() => iniciales(this.caso()?.persona.nombreCompleto ?? ''));
    protected readonly edad = computed(() => {
        const nacimiento = this.caso()?.persona.fechaNacimiento;
        return nacimiento ? edadDesde(nacimiento) : null;
    });
    protected readonly condicion = computed(() => {
        const condicion = this.caso()?.persona.condicion;
        return condicion ? ETIQUETA_CONDICION[condicion] : null;
    });
    protected readonly iteracion = computed(() => textoIteracion(this.caso()?.antes.iteracion ?? 1));
    protected readonly antes = computed(() => {
        const caso = this.caso();
        return caso ? frasesAntesDeContactar(caso.antes) : [];
    });
    /** Se puede registrar mientras el caso esté abierto y su ronda (si tiene) esté en curso. */
    protected readonly trabajable = computed(() => {
        const caso = this.caso();
        return !!caso && caso.puedeActuar && caso.estado !== 'CERRADO' && (caso.estadoRonda === null || caso.estadoRonda === 'EN_CURSO');
    });
    protected readonly conTelefono = computed(() => tieneTelefono(this.caso()?.persona.telefono));
    protected readonly mapa = computed(() => {
        const persona = this.caso()?.persona;
        const lugar = [persona?.direccion, persona?.ubicacion].filter(Boolean).join(', ');
        return lugar ? `${URL_MAPAS}${encodeURIComponent(lugar)}` : null;
    });
    protected readonly menuMas = computed<MenuItem[]>(() => {
        const caso = this.caso();
        if (!caso) {
            return [];
        }
        const opciones: MenuItem[] = [];
        if (caso.puedeReasignar && caso.estado !== 'CERRADO') {
            opciones.push({ label: 'Reasignar', icon: 'pi pi-user-edit', command: () => this.abrirReasignar() });
        }
        if (caso.puedeActuar && caso.estado === 'CERRADO' && (caso.estadoRonda === null || caso.estadoRonda === 'EN_CURSO')) {
            opciones.push({ label: 'Reabrir', icon: 'pi pi-replay', command: () => this.reabrir() });
        }
        return opciones;
    });
    protected readonly copiado = signal(false);

    protected readonly hojaVisible = signal(false);
    protected readonly medioInicial = signal<MedioContacto | null>(null);
    protected readonly contactoEditado = signal<Contacto | null>(null);
    protected readonly guardando = signal(false);
    protected readonly errorHoja = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});
    /** El coordinador puede registrar en nombre de un integrante del equipo. */
    protected readonly equipo = signal<readonly Referencia[]>([]);
    protected readonly opcionesEquipo = computed(() => [...this.equipo()]);

    protected readonly cierreVisible = signal(false);
    protected readonly resultados = signal<readonly ResultadoConsolidacion[]>([]);
    protected readonly cerrando = signal(false);
    protected readonly errorCierre = signal<string | null>(null);

    protected readonly reasignarVisible = signal(false);
    protected readonly nuevoConsolidador = signal<number | null>(null);

    ngOnInit(): void {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set(null);
        this.casos
            .detalle(this.casoId)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (caso) => {
                    this.caso.set(caso);
                    if (caso.puedeReasignar && this.equipo().length === 0) {
                        this.cargarEquipo();
                    }
                },
                error: (error: unknown) => this.error.set(detalleDeError(error, 'No se pudo cargar el caso.') ?? 'No se pudo cargar el caso.')
            });
    }

    protected contactar(medio: MedioBoton): void {
        const caso = this.caso();
        if (caso) {
            this.enCurso.iniciar({ casoId: caso.id, nombre: caso.persona.nombreCompleto, medio });
            // Desde el caso no hace falta esperar a volver: la hoja queda lista con el medio elegido.
            this.abrirHoja(medio);
        }
    }

    protected abrirHoja(medio: MedioContacto | null = null): void {
        this.contactoEditado.set(null);
        this.medioInicial.set(medio);
        this.limpiarErroresHoja();
        this.hojaVisible.set(true);
    }

    protected corregir(contacto: Contacto): void {
        this.contactoEditado.set(contacto);
        this.medioInicial.set(contacto.medio);
        this.limpiarErroresHoja();
        this.hojaVisible.set(true);
    }

    protected guardarContacto(datos: DatosContacto, cerrarDespues = false): void {
        const editado = this.contactoEditado();
        const peticion: Observable<Contacto> = editado ? this.casos.corregirContacto(editado.id, datos) : this.casos.registrarContacto(this.casoId, datos);
        this.guardando.set(true);
        this.limpiarErroresHoja();
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: () => {
                this.enCurso.resolver(this.casoId);
                this.hojaVisible.set(false);
                this.mensajes.add({ severity: 'success', summary: editado ? 'Contacto corregido' : 'Contacto registrado', life: 4000 });
                this.cargar();
                if (cerrarDespues) {
                    this.abrirCierre();
                }
            },
            error: (error: unknown) => {
                this.errorHoja.set(detalleDeError(error, 'No se pudo guardar el contacto. Tus datos siguen aquí: intenta de nuevo.'));
                this.erroresCampo.set(erroresPorCampo(error));
            }
        });
    }

    protected borrar(contacto: Contacto): void {
        this.confirmacion.confirm({
            header: 'Borrar contacto',
            message: '¿Borrar este contacto? Se quitará de la línea de tiempo.',
            icon: 'pi pi-trash',
            acceptLabel: 'Borrar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () =>
                this.ejecutar(this.casos.borrarContacto(contacto.id), () => {
                    this.mensajes.add({ severity: 'success', summary: 'Contacto borrado', life: 4000 });
                    this.cargar();
                })
        });
    }

    protected abrirCierre(): void {
        this.errorCierre.set(null);
        if (this.resultados().length === 0) {
            this.resultadosServicio.listar().subscribe({ next: (resultados) => this.resultados.set(resultados), error: () => this.errorCierre.set('No se pudieron cargar los resultados. Cierra el diálogo e intenta de nuevo.') });
        }
        this.cierreVisible.set(true);
    }

    protected cerrarCaso({ resultadoId, observacion }: CierreCaso): void {
        this.cerrando.set(true);
        this.errorCierre.set(null);
        this.casos
            .cerrar(this.casoId, resultadoId, observacion)
            .pipe(finalize(() => this.cerrando.set(false)))
            .subscribe({
                next: (caso) => {
                    this.caso.set(caso);
                    this.cierreVisible.set(false);
                    this.mensajes.add({ severity: 'success', summary: 'Seguimiento cerrado', life: 4000 });
                },
                error: (error: unknown) => this.errorCierre.set(detalleDeError(error, 'No se pudo cerrar el caso. Intenta de nuevo.'))
            });
    }

    protected reasignar(): void {
        const consolidadorId = this.nuevoConsolidador();
        if (consolidadorId === null) {
            return;
        }
        this.ejecutar(this.casos.reasignar([this.casoId], consolidadorId), () => {
            this.reasignarVisible.set(false);
            this.mensajes.add({ severity: 'success', summary: 'Caso reasignado', life: 4000 });
            this.cargar();
        });
    }

    protected async copiarTelefono(): Promise<void> {
        const telefono = this.caso()?.persona.telefono;
        if (!telefono) {
            return;
        }
        try {
            await navigator.clipboard.writeText(telefono);
            this.copiado.set(true);
            setTimeout(() => this.copiado.set(false), AVISO_COPIADO_MS);
        } catch {
            this.mensajes.add({ severity: 'warn', summary: 'No se pudo copiar: selecciona el número a mano.', life: 4000 });
        }
    }

    private reabrir(): void {
        this.ejecutar(this.casos.reabrir(this.casoId), (caso) => {
            this.caso.set(caso);
            this.mensajes.add({ severity: 'success', summary: 'Caso reabierto', life: 4000 });
        });
    }

    private abrirReasignar(): void {
        this.nuevoConsolidador.set(this.caso()?.consolidador?.id ?? null);
        this.reasignarVisible.set(true);
    }

    private cargarEquipo(): void {
        this.equipoServicio.listar().subscribe({
            next: (equipo) => this.equipo.set(equipo.filter(({ activo }) => activo).map(({ miembroId, nombreCompleto }) => ({ id: miembroId, nombre: nombreCompleto, activo: true }))),
            error: () => this.equipo.set([])
        });
    }

    private ejecutar<T>(peticion: Observable<T>, alTerminar: (resultado: T) => void): void {
        this.errorAccion.set(null);
        peticion.subscribe({ next: alTerminar, error: (error: unknown) => this.errorAccion.set(detalleDeError(error, 'No se pudo completar la acción. Intenta de nuevo.')) });
    }

    private limpiarErroresHoja(): void {
        this.errorHoja.set(null);
        this.erroresCampo.set({});
    }
}
