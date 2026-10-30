import { Referencia } from '@/app/core/models/referencia';
import { AsistenciaMiembro, ReunionAsistida } from '@/app/features/asistencia/models/asistencia';
import { TipoAlerta } from '@/app/features/asistencia/models/seguimiento';
import { Condicion } from '@/app/features/miembros/models/miembro';

export type EstadoRonda = 'PLANIFICADA' | 'EN_CURSO' | 'CERRADA';
export type EstadoCaso = 'PENDIENTE' | 'EN_CONTACTO' | 'CERRADO';
export type MedioContacto = 'LLAMADA' | 'WHATSAPP' | 'VISITA' | 'EN_LA_IGLESIA';
export type ResultadoContacto = 'SE_HABLO' | 'NO_RESPONDIO' | 'NO_ESTABA' | 'DATO_INCORRECTO';
export type SeccionMisCasos = 'PARA_HOY' | 'SIN_CONTACTAR' | 'EN_CONTACTO';
export type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

/** Contrato de `ResultadoConsolidacionResponse`: resultado de una iteración, del catálogo de la iglesia. */
export interface ResultadoConsolidacion {
    readonly id: number;
    readonly nombre: string;
    readonly orden: number;
    readonly activo: boolean;
}

export interface Consolidador {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly activo: boolean;
    readonly casosAbiertos: number;
    readonly contactosRecientes: number;
}

export interface Ronda {
    readonly id: number;
    readonly nombre: string;
    readonly descripcion: string | null;
    readonly fechaInicio: string;
    readonly fechaFin: string | null;
    readonly estado: EstadoRonda;
    readonly personas: number;
    readonly pendientes: number;
    readonly enContacto: number;
    readonly cerrados: number;
    readonly noConcluidos: number;
    readonly sinAsignar: number;
    readonly vencidos: number;
}

export interface DatosRonda {
    readonly nombre: string;
    readonly descripcion: string | null;
    readonly fechaInicio: string;
    readonly fechaFin: string | null;
}

export interface FiltroRondas {
    readonly estado: EstadoRonda | null;
    readonly q: string;
}

export const FILTRO_RONDAS_INICIAL: FiltroRondas = { estado: null, q: '' };

export interface FiltroLote {
    readonly condicion: Condicion | null;
    readonly ingresoDesde: string | null;
    readonly ingresoHasta: string | null;
    readonly alerta: TipoAlerta | null;
    readonly grupoId: number | null;
    readonly sinGrupo: boolean;
    readonly nuncaConsolidados: boolean;
    readonly miembroIds: readonly number[] | null;
}

export const FILTRO_LOTE_INICIAL: FiltroLote = {
    condicion: null,
    ingresoDesde: null,
    ingresoHasta: null,
    alerta: null,
    grupoId: null,
    sinGrupo: false,
    nuncaConsolidados: false,
    miembroIds: null
};

export interface PersonaBreve {
    readonly miembroId: number;
    readonly nombreCompleto: string;
}

export interface VistaPreviaLote {
    readonly entrarian: number;
    readonly conCasoAbierto: number;
    /** Todas las que se agregarían, por apellidos (hasta el tope de un lote). */
    readonly personas: readonly PersonaBreve[];
}

export interface LoteAgregado {
    readonly agregadas: number;
    readonly conCasoAbierto: number;
}

export interface Asignacion {
    readonly casoId: number;
    readonly consolidadorId: number;
}

export interface SolicitudReparto {
    readonly consolidadorIds: readonly number[];
    readonly soloSinAsignar: boolean;
    readonly asignaciones?: readonly Asignacion[];
}

export interface AsignacionPropuesta {
    readonly casoId: number;
    readonly persona: PersonaBreve;
    readonly consolidadorId: number;
}

export interface CargaConsolidador {
    readonly consolidadorId: number;
    readonly nombreCompleto: string;
    readonly cargaActual: number;
    readonly nuevos: number;
}

export interface PropuestaReparto {
    readonly asignaciones: readonly AsignacionPropuesta[];
    readonly cargas: readonly CargaConsolidador[];
}

export interface ContactosPorMedio {
    readonly total: number;
    readonly llamadas: number;
    readonly whatsapp: number;
    readonly visitas: number;
    readonly enLaIglesia: number;
    readonly conRespuesta: number;
}

export interface UltimoContacto {
    readonly fechaHora: string;
    readonly medio: MedioContacto;
    readonly resultado: ResultadoContacto;
}

export interface IteracionAnterior {
    readonly resultado: Referencia | null;
    readonly cerradoEn: string | null;
}

export interface PersonaCaso {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly telefono: string | null;
    readonly condicion: Condicion | null;
    readonly fechaIngreso: string | null;
    readonly activo: boolean;
}

/** Contrato de `CasoResumenResponse`: un caso en las listas (ronda y «Mis asignados»). */
export interface CasoResumen {
    readonly id: number;
    readonly ronda: Referencia | null;
    readonly persona: PersonaCaso;
    readonly consolidador: Referencia | null;
    readonly estado: EstadoCaso;
    readonly contactos: ContactosPorMedio;
    readonly ultimoContacto: UltimoContacto | null;
    readonly proximoContacto: string | null;
    readonly resultado: Referencia | null;
    readonly iteracion: number;
    readonly anterior: IteracionAnterior | null;
}

export interface FiltroCasosRonda {
    readonly estado: EstadoCaso | null;
    readonly consolidadorId: number | null;
    readonly sinAsignar: boolean;
    readonly q: string;
}

export const FILTRO_CASOS_RONDA_INICIAL: FiltroCasosRonda = { estado: null, consolidadorId: null, sinAsignar: false, q: '' };

export interface ResumenMisCasos {
    readonly paraHoy: number;
    readonly sinContactar: number;
    readonly enContacto: number;
    readonly asignados: number;
    readonly contactados: number;
}

export interface Contacto {
    readonly id: number;
    readonly casoId: number;
    readonly fechaHora: string;
    readonly medio: MedioContacto;
    readonly resultado: ResultadoContacto;
    readonly observaciones: string | null;
    readonly proximoContacto: string | null;
    readonly consolidador: Referencia | null;
    readonly registradoPorMi: boolean;
    readonly editableHasta: string;
    readonly editable: boolean;
}

export interface DatosContacto {
    readonly fechaHora: string;
    readonly medio: MedioContacto;
    readonly resultado: ResultadoContacto;
    readonly observaciones: string | null;
    readonly proximoContacto: string | null;
    readonly consolidadorId?: number | null;
}

export interface Iteracion {
    readonly casoId: number;
    readonly ronda: Referencia | null;
    readonly estado: EstadoCaso;
    readonly desde: string;
    readonly cerradoEn: string | null;
    readonly consolidador: Referencia | null;
    readonly contactos: ContactosPorMedio;
    readonly resultado: Referencia | null;
    readonly observacionCierre: string | null;
    readonly detalle: readonly Contacto[];
}

export interface AntesDeContactar {
    readonly iteracion: number;
    readonly contactosAnteriores: ContactosPorMedio;
    readonly resultadosAnteriores: readonly IteracionAnterior[];
    readonly asistenciasDesdeUltimoContacto: number;
    readonly peticion: string | null;
}

export interface ContactoPersona {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly telefono: string | null;
    readonly correo: string | null;
    readonly direccion: string | null;
    readonly ubicacion: string | null;
    readonly condicion: Condicion | null;
    readonly fechaIngreso: string | null;
    readonly fechaNacimiento: string | null;
    readonly invitadoPor: string | null;
    readonly lider: string | null;
    readonly grupo: string | null;
    readonly peticion: string | null;
    readonly activo: boolean;
}

/** Contrato de `CasoDetalleResponse`: todo lo necesario para contactar a la persona. */
export interface CasoDetalle {
    readonly id: number;
    readonly ronda: Referencia | null;
    readonly estadoRonda: EstadoRonda | null;
    readonly estado: EstadoCaso;
    readonly consolidador: Referencia | null;
    readonly resultado: Referencia | null;
    readonly observacionCierre: string | null;
    readonly proximoContacto: string | null;
    readonly creadoEn: string;
    readonly cerradoEn: string | null;
    readonly persona: ContactoPersona;
    readonly antes: AntesDeContactar;
    readonly contactos: readonly Contacto[];
    readonly asistencias: readonly ReunionAsistida[];
    readonly asistencia: AsistenciaMiembro;
    readonly anteriores: readonly Iteracion[];
    readonly puedeActuar: boolean;
    readonly puedeReasignar: boolean;
}

export interface CasoAbierto {
    readonly casoId: number;
    readonly ronda: Referencia | null;
    readonly consolidador: Referencia | null;
    readonly estado: EstadoCaso;
}

export interface ConsolidacionMiembro {
    readonly iteraciones: number;
    readonly contactos: ContactosPorMedio;
    readonly ultimoResultado: Referencia | null;
    readonly ultimoResultadoEn: string | null;
    readonly casoAbierto: CasoAbierto | null;
    readonly fechasContactoRecientes: readonly string[];
    readonly puedeIniciar: boolean;
}

export interface ConteoResultado {
    readonly resultado: Referencia | null;
    readonly cantidad: number;
}

export interface AvanceConsolidador {
    readonly consolidador: Referencia | null;
    readonly asignados: number;
    readonly contactados: number;
    readonly cerrados: number;
}

export interface AvanceRonda {
    readonly personas: number;
    readonly contactadas: number;
    readonly pendientes: number;
    readonly enContacto: number;
    readonly cerrados: number;
    readonly noConcluidos: number;
    readonly volvieronAAsistir: number;
    readonly horasHastaPrimerContacto: number | null;
    readonly porResultado: readonly ConteoResultado[];
    readonly porConsolidador: readonly AvanceConsolidador[];
}

// ---------------------------------------------------------------- Textos y reglas de la interfaz

export const ETIQUETA_ESTADO_RONDA: Readonly<Record<EstadoRonda, string>> = { PLANIFICADA: 'Planificada', EN_CURSO: 'En curso', CERRADA: 'Cerrada' };
export const SEVERIDAD_ESTADO_RONDA: Readonly<Record<EstadoRonda, Severidad>> = { PLANIFICADA: 'info', EN_CURSO: 'success', CERRADA: 'secondary' };
export const ICONO_ESTADO_RONDA: Readonly<Record<EstadoRonda, string>> = { PLANIFICADA: 'pi pi-calendar', EN_CURSO: 'pi pi-play', CERRADA: 'pi pi-lock' };

export const ETIQUETA_ESTADO_CASO: Readonly<Record<EstadoCaso, string>> = { PENDIENTE: 'Pendiente', EN_CONTACTO: 'En contacto', CERRADO: 'Cerrado' };
export const SEVERIDAD_ESTADO_CASO: Readonly<Record<EstadoCaso, Severidad>> = { PENDIENTE: 'warn', EN_CONTACTO: 'info', CERRADO: 'success' };

export const ETIQUETA_MEDIO: Readonly<Record<MedioContacto, string>> = { LLAMADA: 'Llamada', WHATSAPP: 'WhatsApp', VISITA: 'Visita', EN_LA_IGLESIA: 'En la iglesia' };
export const ICONO_MEDIO: Readonly<Record<MedioContacto, string>> = { LLAMADA: 'pi pi-phone', WHATSAPP: 'pi pi-whatsapp', VISITA: 'pi pi-home', EN_LA_IGLESIA: 'pi pi-building' };
export const MEDIOS: readonly MedioContacto[] = ['LLAMADA', 'WHATSAPP', 'VISITA', 'EN_LA_IGLESIA'];

/** Qué resultados tienen sentido para cada medio (la misma regla que valida el backend). */
export const RESULTADOS_POR_MEDIO: Readonly<Record<MedioContacto, readonly ResultadoContacto[]>> = {
    LLAMADA: ['SE_HABLO', 'NO_RESPONDIO', 'DATO_INCORRECTO'],
    WHATSAPP: ['SE_HABLO', 'NO_RESPONDIO', 'DATO_INCORRECTO'],
    VISITA: ['SE_HABLO', 'NO_ESTABA', 'DATO_INCORRECTO'],
    EN_LA_IGLESIA: ['SE_HABLO']
};

export const SEVERIDAD_RESULTADO: Readonly<Record<ResultadoContacto, Severidad>> = { SE_HABLO: 'success', NO_RESPONDIO: 'warn', NO_ESTABA: 'warn', DATO_INCORRECTO: 'danger' };
export const ICONO_RESULTADO: Readonly<Record<ResultadoContacto, string>> = { SE_HABLO: 'pi pi-comments', NO_RESPONDIO: 'pi pi-clock', NO_ESTABA: 'pi pi-times', DATO_INCORRECTO: 'pi pi-ban' };

/** Texto del resultado según el medio: «No contestó» en una llamada, «No respondió» en un mensaje. */
export function etiquetaResultado(resultado: ResultadoContacto, medio: MedioContacto): string {
    switch (resultado) {
        case 'SE_HABLO':
            return 'Hablamos';
        case 'NO_RESPONDIO':
            return medio === 'LLAMADA' ? 'No contestó' : 'No respondió';
        case 'NO_ESTABA':
            return 'No estaba';
        case 'DATO_INCORRECTO':
            return medio === 'VISITA' ? 'Dirección equivocada' : 'Número equivocado';
    }
}

/** «2 llamadas · 1 visita»; sin contactos, «Sin contactos». */
export function resumenContactos({ llamadas, whatsapp, visitas, enLaIglesia }: ContactosPorMedio): string {
    const partes = [plural(llamadas, 'llamada', 'llamadas'), plural(whatsapp, 'WhatsApp', 'WhatsApp'), plural(visitas, 'visita', 'visitas'), plural(enLaIglesia, 'en la iglesia', 'en la iglesia')].filter((parte) => parte !== null);
    return partes.length === 0 ? 'Sin contactos' : partes.join(' · ');
}

function plural(cantidad: number, singular: string, varios: string): string | null {
    return cantidad === 0 ? null : `${cantidad} ${cantidad === 1 ? singular : varios}`;
}

/** Iniciales para el avatar: «Ana Quispe Ramos» → «AQ». */
export function iniciales(nombre: string): string {
    return nombre
        .split(/\s+/)
        .filter((parte) => parte.length > 0)
        .slice(0, 2)
        .map((parte) => parte[0].toUpperCase())
        .join('');
}

/** «2.ª iteración». */
export function textoIteracion(iteracion: number): string {
    return `${iteracion}.ª iteración`;
}

const MS_POR_DIA = 86_400_000;
const FORMATO_MES = new Intl.DateTimeFormat('es', { month: 'short', year: 'numeric' });
const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es', { dateStyle: 'long', timeStyle: 'short' });
const FORMATO_FECHA = new Intl.DateTimeFormat('es', { dateStyle: 'long', timeZone: 'UTC' });

function diasEntre(desde: Date, hasta: Date): number {
    return Math.round((Date.UTC(hasta.getFullYear(), hasta.getMonth(), hasta.getDate()) - Date.UTC(desde.getFullYear(), desde.getMonth(), desde.getDate())) / MS_POR_DIA);
}

/** Fecha y hora relativas para un instante: «hoy», «ayer», «hace 3 días». */
export function haceDias(instante: string, ahora: Date = new Date()): string {
    const dias = diasEntre(new Date(instante), ahora);
    return dias <= 0 ? 'hoy' : dias === 1 ? 'ayer' : `hace ${dias} días`;
}

/** Fecha completa, para el `title` de las fechas relativas: «3 de noviembre de 2026, 19:30». */
export function fechaHoraCompleta(instante: string): string {
    return FORMATO_FECHA_HORA.format(new Date(instante));
}

/** `yyyy-MM-dd` completo: «10 de noviembre de 2026». */
export function fechaCompleta(iso: string): string {
    return FORMATO_FECHA.format(new Date(`${iso}T00:00:00Z`));
}

/** «ago 2026», para referirse a una iteración pasada. */
export function mesAnio(instante: string): string {
    return FORMATO_MES.format(new Date(instante)).replace('.', '');
}

/** Días desde hoy hasta una fecha `yyyy-MM-dd` (negativo si ya pasó). */
export function diasHasta(iso: string, ahora: Date = new Date()): number {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return diasEntre(ahora, new Date(anio, mes - 1, dia));
}

/** «faltan 5 días», «termina hoy», «venció hace 2 días»; sin fecha de fin, `null`. */
export function plazoRonda(fechaFin: string | null, ahora: Date = new Date()): string | null {
    if (fechaFin === null) {
        return null;
    }
    const dias = diasHasta(fechaFin, ahora);
    if (dias === 0) {
        return 'termina hoy';
    }
    if (dias > 0) {
        return dias === 1 ? 'falta 1 día' : `faltan ${dias} días`;
    }
    return dias === -1 ? 'venció ayer' : `venció hace ${-dias} días`;
}

/** Próximo contacto: «vencido hace 2 días», «hoy», «en 3 días». */
export function textoProximo(iso: string, ahora: Date = new Date()): string {
    const dias = diasHasta(iso, ahora);
    if (dias === 0) {
        return 'para hoy';
    }
    if (dias < 0) {
        return dias === -1 ? 'vencido ayer' : `vencido hace ${-dias} días`;
    }
    return dias === 1 ? 'mañana' : `en ${dias} días`;
}

/**
 * Línea de contexto de una tarjeta: el último contacto de esta iteración o, si aún no tiene, el resultado anterior.
 * P. ej. «Último: no contestó (hace 3 días)» o «Antes: No le interesa por ahora (ago 2026)».
 */
export function lineaContexto(caso: CasoResumen, ahora: Date = new Date()): string | null {
    if (caso.ultimoContacto) {
        const { resultado, medio, fechaHora } = caso.ultimoContacto;
        return `Último: ${etiquetaResultado(resultado, medio).toLowerCase()} (${haceDias(fechaHora, ahora)})`;
    }
    if (caso.anterior) {
        const resultado = caso.anterior.resultado?.nombre ?? 'No concluido';
        return caso.anterior.cerradoEn ? `Antes: ${resultado} (${mesAnio(caso.anterior.cerradoEn)})` : `Antes: ${resultado}`;
    }
    return null;
}

/**
 * «Antes de contactar» en lenguaje natural, armado con los datos (sin IA). P. ej. «Es su 3.ª iteración. Antes:
 * No le interesa por ahora (ago 2026); No concluido (jun 2026). En las iteraciones anteriores recibió 3 llamadas.
 * Volvió a asistir 2 veces después del último contacto. Llegó pidiendo: oración por su familia».
 */
export function frasesAntesDeContactar(antes: AntesDeContactar): readonly string[] {
    if (antes.iteracion === 1) {
        const frases = ['Es su primer seguimiento de consolidación.'];
        return antes.peticion ? [...frases, `Llegó pidiendo: ${antes.peticion}`] : frases;
    }
    const frases = [`Es su ${textoIteracion(antes.iteracion)}.`];
    if (antes.resultadosAnteriores.length > 0) {
        const resultados = antes.resultadosAnteriores.map(({ resultado, cerradoEn }) => `${resultado?.nombre ?? 'No concluido'}${cerradoEn ? ` (${mesAnio(cerradoEn)})` : ''}`);
        frases.push(`Antes: ${resultados.join('; ')}.`);
    }
    if (antes.contactosAnteriores.total > 0) {
        frases.push(`En las iteraciones anteriores recibió ${resumenContactos(antes.contactosAnteriores).toLowerCase()}${antes.contactosAnteriores.conRespuesta === 0 ? ', sin lograr hablar con la persona' : ''}.`);
    }
    frases.push(
        antes.asistenciasDesdeUltimoContacto > 0
            ? `Volvió a asistir ${antes.asistenciasDesdeUltimoContacto} ${antes.asistenciasDesdeUltimoContacto === 1 ? 'vez' : 'veces'} después del último contacto.`
            : 'No volvió a asistir después del último contacto.'
    );
    if (antes.peticion) {
        frases.push(`Llegó pidiendo: ${antes.peticion}`);
    }
    return frases;
}

/** Fecha `yyyy-MM-dd` dentro de `dias` días, para los atajos del próximo contacto y de las fechas de la ronda. */
export function enDias(dias: number, desde: Date = new Date()): string {
    const fecha = new Date(desde.getFullYear(), desde.getMonth(), desde.getDate() + dias);
    const dosDigitos = (valor: number): string => String(valor).padStart(2, '0');
    return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;
}

/** Sugerencia de nombre para una ronda nueva: «Consolidación noviembre 2026». */
export function nombreSugerido(ahora: Date = new Date()): string {
    return `Consolidación ${ahora.toLocaleDateString('es', { month: 'long', year: 'numeric' }).replace(' de ', ' ')}`;
}

/** Porcentaje entero de `parte` sobre `total`; con total 0, 0. */
export function porcentaje(parte: number, total: number): number {
    return total === 0 ? 0 : Math.round((parte * 100) / total);
}

/** Límites de la API de consolidación (Bean Validation de iglesia-api). */
export const LIMITES_CONSOLIDACION = {
    nombreRondaMin: 3,
    nombreRondaMax: 80,
    descripcionMax: 500,
    nombreResultadoMin: 2,
    nombreResultadoMax: 60,
    observacionesMax: 1000,
    loteMax: 1000
} as const;

/** Verbo del contacto para la línea de tiempo: «Luis llamó», «Rosa visitó». */
export const VERBO_MEDIO: Readonly<Record<MedioContacto, string>> = { LLAMADA: 'llamó', WHATSAPP: 'escribió por WhatsApp', VISITA: 'visitó', EN_LA_IGLESIA: 'habló en la iglesia' };

const MS_POR_MINUTO = 60_000;
const MINUTOS_POR_HORA = 60;

/** Cuánto falta para que venza la corrección: «3 h», «20 min»; `null` si ya venció. */
export function tiempoRestante(hasta: string, ahora: Date = new Date()): string | null {
    const minutos = Math.floor((new Date(hasta).getTime() - ahora.getTime()) / MS_POR_MINUTO);
    if (minutos <= 0) {
        return null;
    }
    return minutos < MINUTOS_POR_HORA ? `${minutos} min` : `${Math.floor(minutos / MINUTOS_POR_HORA)} h`;
}

/** La acción que sigue en una ronda, según su estado: «¿Qué sigue?» siempre visible. */
export type AccionRonda = 'AGREGAR' | 'REPARTIR' | 'INICIAR' | null;

export function accionPrincipal({ estado, personas, sinAsignar }: Ronda): AccionRonda {
    if (estado === 'CERRADA') {
        return null;
    }
    if (personas === 0) {
        return 'AGREGAR';
    }
    if (sinAsignar > 0) {
        return 'REPARTIR';
    }
    return estado === 'PLANIFICADA' ? 'INICIAR' : null;
}

/**
 * Reparte casos en partes iguales entre los consolidadores activos, empezando por quien tiene menos casos abiertos
 * (la misma idea que el reparto balanceado del backend, para una selección hecha a mano).
 */
export function repartirEnPartesIguales(casoIds: readonly number[], equipo: readonly Consolidador[]): Asignacion[] {
    const activos = equipo.filter(({ activo }) => activo);
    if (activos.length === 0) {
        return [];
    }
    const carga = new Map(activos.map(({ miembroId, casosAbiertos }) => [miembroId, casosAbiertos]));
    return casoIds.map((casoId) => {
        const [consolidadorId] = [...carga.entries()].reduce((menor, actual) => (actual[1] < menor[1] ? actual : menor));
        carga.set(consolidadorId, carga.get(consolidadorId)! + 1);
        return { casoId, consolidadorId };
    });
}
