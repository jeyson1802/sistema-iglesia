import { TipoReunion } from './asistencia';

/** Alertas de seguimiento, de la más grave a la menos grave (mismo orden que el backend). */
export const TIPOS_ALERTA = ['FALTAS_SEGUIDAS', 'NUEVO_SIN_VOLVER', 'ASISTENCIA_EN_CAIDA'] as const;
export type TipoAlerta = (typeof TIPOS_ALERTA)[number];

export interface DescripcionAlerta {
    readonly etiqueta: string;
    readonly severidad: 'danger' | 'warn' | 'info';
    readonly icono: string;
    readonly ayuda: string;
}

export const DESCRIPCION_ALERTA: Readonly<Record<TipoAlerta, DescripcionAlerta>> = {
    FALTAS_SEGUIDAS: { etiqueta: 'Faltas seguidas', severidad: 'danger', icono: 'pi pi-exclamation-circle', ayuda: 'Faltó a sus últimas ocasiones seguidas.' },
    NUEVO_SIN_VOLVER: { etiqueta: 'Nuevo sin volver', severidad: 'warn', icono: 'pi pi-user-minus', ayuda: 'Es nuevo, vino pocas veces y no volvió en las últimas dos semanas.' },
    ASISTENCIA_EN_CAIDA: { etiqueta: 'Asistencia en caída', severidad: 'warn', icono: 'pi pi-arrow-down-right', ayuda: 'Su asistencia de las últimas 4 semanas bajó mucho frente a las anteriores.' }
};

export const ESTADOS_OCASION = ['ASISTIO', 'FALTO', 'EXTRA'] as const;
export type EstadoOcasion = (typeof ESTADOS_OCASION)[number];

export const DESCRIPCION_ESTADO_OCASION: Readonly<Record<EstadoOcasion, { readonly etiqueta: string; readonly severidad: 'success' | 'danger' | 'info'; readonly icono: string }>> = {
    ASISTIO: { etiqueta: 'Asistió', severidad: 'success', icono: 'pi pi-check' },
    FALTO: { etiqueta: 'Faltó', severidad: 'danger', icono: 'pi pi-times' },
    EXTRA: { etiqueta: 'Evento', severidad: 'info', icono: 'pi pi-star' }
};

/** Contrato de `SemanaAsistenciaResponse`. */
export interface SemanaAsistencia {
    /** Lunes de la semana, `yyyy-MM-dd`. */
    readonly inicio: string;
    readonly esperadas: number;
    readonly asistidas: number;
    readonly porcentaje: number | null;
}

/** Contrato de `OcasionResponse`. */
export interface Ocasion {
    readonly fecha: string;
    readonly hora: string | null;
    /** `CULTO` es un día de culto: puede agrupar varias reuniones. */
    readonly tipo: TipoReunion;
    readonly descripcion: string;
    readonly asistioA: string | null;
    readonly estado: EstadoOcasion;
}

export interface FiltroOcasiones {
    readonly estado: EstadoOcasion | null;
}

/** Contrato de `PersonaSeguimientoResponse`. */
export interface PersonaSeguimiento {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
    readonly grupoId: number | null;
    readonly grupo: string | null;
    readonly lider: string | null;
    readonly porcentaje: number | null;
    readonly porcentajeAnterior: number | null;
    readonly faltasSeguidas: number;
    readonly ultimaAsistencia: string | null;
    readonly alertas: readonly TipoAlerta[];
}

export interface FiltroSeguimiento {
    /** `null`: cualquier alerta. */
    readonly alerta: TipoAlerta | null;
    readonly grupoId: number | null;
    readonly q: string;
    /** Incluye también a quienes no tienen alertas. */
    readonly todos: boolean;
}

export const FILTRO_SEGUIMIENTO_INICIAL: FiltroSeguimiento = { alerta: null, grupoId: null, q: '', todos: false };

/** Contrato de `ConteoAlertasResponse`. */
export interface ConteoAlertas {
    readonly conAlertas: number;
    readonly faltasSeguidas: number;
    readonly nuevoSinVolver: number;
    readonly asistenciaEnCaida: number;
}

export const CAMPO_CONTEO: Readonly<Record<TipoAlerta, keyof ConteoAlertas>> = {
    FALTAS_SEGUIDAS: 'faltasSeguidas',
    NUEVO_SIN_VOLVER: 'nuevoSinVolver',
    ASISTENCIA_EN_CAIDA: 'asistenciaEnCaida'
};

/** Contrato de `SemanaGrupoResponse`. */
export interface SemanaGrupo {
    readonly inicio: string;
    readonly reuniones: number;
    readonly asistentes: number;
    readonly porcentaje: number | null;
}

/** Contrato de `SeguimientoGrupoResponse`. */
export interface SeguimientoGrupo {
    readonly semanas: readonly SemanaGrupo[];
    readonly integrantes: readonly PersonaSeguimiento[];
}

/** Contrato de `SerieCultoResponse`. */
export interface SerieCulto {
    readonly cultoProgramadoId: number;
    readonly nombre: string;
    /** Alineada con las semanas; `null` si esa semana no hubo reunión. */
    readonly asistentes: readonly (number | null)[];
}

/** Contrato de `ServiciosSeguimientoResponse`. */
export interface ServiciosSeguimiento {
    readonly semanas: readonly string[];
    readonly cultos: readonly SerieCulto[];
    readonly personasEnCultos: readonly number[];
    readonly presentesEnGrupos: readonly number[];
    readonly porcentajeGrupos: readonly (number | null)[];
}

/** Contrato de `ReglasSeguimientoDto`. */
export interface ReglasSeguimiento {
    readonly faltasSeguidas: number;
    readonly puntosCaida: number;
    readonly diasNuevo: number;
}

/** Límites de iglesia-api (`LimitesSeguimiento`): se repiten aquí para validar en el formulario. */
export const LIMITES_REGLAS = {
    faltasSeguidas: { min: 2, max: 10 },
    puntosCaida: { min: 10, max: 80 },
    diasNuevo: { min: 30, max: 180 }
} as const;

export type Tendencia = 'sube' | 'baja' | 'igual';

/** Diferencia mínima de puntos para hablar de subida o bajada; menos que eso es estable. */
export const PUNTOS_TENDENCIA = 5;

export const DESCRIPCION_TENDENCIA: Readonly<Record<Tendencia, { readonly etiqueta: string; readonly icono: string; readonly severidad: 'success' | 'danger' | 'secondary' }>> = {
    sube: { etiqueta: 'Subiendo', icono: 'pi pi-arrow-up-right', severidad: 'success' },
    baja: { etiqueta: 'Bajando', icono: 'pi pi-arrow-down-right', severidad: 'danger' },
    igual: { etiqueta: 'Estable', icono: 'pi pi-arrow-right', severidad: 'secondary' }
};

/** Tendencia del porcentaje actual frente al anterior; sin uno de los dos, `null`. */
export function tendencia(actual: number | null, anterior: number | null): Tendencia | null {
    if (actual === null || anterior === null) {
        return null;
    }
    const diferencia = actual - anterior;
    return Math.abs(diferencia) < PUNTOS_TENDENCIA ? 'igual' : diferencia > 0 ? 'sube' : 'baja';
}

const MS_POR_DIA = 86_400_000;

/** «hoy», «ayer», «hace 12 días»; sin fecha, «nunca». Cuenta días de calendario locales. */
export function haceCuanto(iso: string | null, ahora: Date = new Date()): string {
    if (iso === null) {
        return 'nunca';
    }
    const [anio, mes, dia] = iso.split('-').map(Number);
    const dias = Math.round((Date.UTC(ahora.getFullYear(), ahora.getMonth(), ahora.getDate()) - Date.UTC(anio, mes - 1, dia)) / MS_POR_DIA);
    return dias <= 0 ? 'hoy' : dias === 1 ? 'ayer' : `hace ${dias} días`;
}

const FORMATO_SEMANA = new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short', timeZone: 'UTC' });

/** `2026-10-26` → «26 oct», para las etiquetas de las semanas. */
export function semanaCorta(iso: string): string {
    return FORMATO_SEMANA.format(new Date(`${iso}T00:00:00Z`)).replace('.', '');
}

/** Resumen de una serie semanal sobre las semanas cerradas (la actual va por la mitad y se deja fuera). */
export interface ResumenSerie {
    readonly promedio: number | null;
    readonly maximo: number | null;
    readonly semanaPasada: number | null;
    /** Semana pasada frente al promedio de las anteriores: en % o, si la serie es un porcentaje, en puntos. */
    readonly variacion: number | null;
}

const PORCENTAJE = 100;

export function resumenSerie(valores: readonly (number | null)[], esPorcentaje = false): ResumenSerie {
    const cerradas = valores.slice(0, -1);
    const conDato = cerradas.filter((valor): valor is number => valor !== null);
    const semanaPasada = cerradas.at(-1) ?? null;
    const anteriores = cerradas.slice(0, -1).filter((valor): valor is number => valor !== null);
    const promedioDe = (lista: readonly number[]) => (lista.length === 0 ? null : lista.reduce((suma, valor) => suma + valor, 0) / lista.length);
    const base = promedioDe(anteriores);
    let variacion: number | null = null;
    if (semanaPasada !== null && base !== null) {
        variacion = esPorcentaje ? Math.round(semanaPasada - base) : base === 0 ? null : Math.round(((semanaPasada - base) / base) * PORCENTAJE);
    }
    const promedio = promedioDe(conDato);
    return { promedio: promedio === null ? null : Math.round(promedio), maximo: conDato.length === 0 ? null : Math.max(...conDato), semanaPasada, variacion };
}
