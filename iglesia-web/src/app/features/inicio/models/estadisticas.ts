import { ConteoAlertas } from '@/app/features/asistencia/models/seguimiento';
import { Rol } from '@/app/core/auth/models/rol';
import { ResumenAportes } from '@/app/features/aportes/models/aporte';
import { Periodo } from '@/app/shared/utils/periodo';

/**
 * Periodo pedido a las estadísticas: uno de los atajos del filtro de aportes (hoy, semana, mes, mes anterior, año) o
 * «Personalizado» con su rango (`yyyy-MM-dd`, inclusivo). Las fechas de los atajos las calcula el servidor.
 */
export interface ConsultaPeriodo {
    readonly periodo: Periodo;
    readonly desde?: string;
    readonly hasta?: string;
}

export const PERIODO_POR_DEFECTO: ConsultaPeriodo = { periodo: 'MES' };

/** Días máximos de un rango personalizado (los mismos que acepta la API). */
export const DIAS_MAXIMOS_PERSONALIZADO = 366;

/** El servidor la elige según la duración: por día hasta 2 semanas, por semana hasta unos 3 meses y si no por mes. */
export type Granularidad = 'DIA' | 'SEMANA' | 'MES';

/** Contrato de `PeriodoResponse`: fechas `yyyy-MM-dd` inclusivas. */
export interface PeriodoUsado {
    readonly periodo: Periodo;
    readonly desde: string;
    readonly hasta: string;
    readonly anteriorDesde: string;
    readonly anteriorHasta: string;
    readonly granularidad: Granularidad;
}

export interface Conteo {
    readonly clave: string;
    readonly etiqueta: string;
    readonly valor: number;
}

export interface PuntoSerie {
    readonly inicio: string;
    readonly valor: number;
}

export interface EstadisticasMiembros {
    readonly periodo: PeriodoUsado;
    readonly red: boolean;
    readonly activos: number;
    readonly directos: number | null;
    readonly altas: number;
    readonly altasAnterior: number;
    readonly bajas: number;
    readonly bajasAnterior: number;
    readonly sinGrupo: number;
    readonly sinLider: number | null;
    readonly serie: readonly { readonly inicio: string; readonly altas: number; readonly bajas: number }[];
    readonly porCondicion: readonly Conteo[];
    readonly porNivel: readonly Conteo[];
    readonly porGenero: readonly Conteo[];
    readonly porEdad: readonly Conteo[];
}

export interface GrupoEstadistica {
    readonly grupoId: number;
    readonly nombre: string;
    readonly integrantes: number;
    readonly reuniones: number;
    readonly porcentaje: number | null;
}

export interface EstadisticasGrupos {
    readonly periodo: PeriodoUsado;
    readonly red: boolean;
    readonly grupos: number;
    readonly integrantesPromedio: number;
    readonly miembrosEnGrupo: number;
    readonly miembrosActivos: number;
    readonly sinReuniones: number;
    readonly reuniones: number;
    readonly reunionesAnterior: number;
    readonly porcentajeAsistencia: number | null;
    readonly porcentajeAsistenciaAnterior: number | null;
    readonly porGrupo: readonly GrupoEstadistica[];
}

export interface EstadisticasAsistencia {
    readonly periodo: PeriodoUsado;
    readonly red: boolean;
    readonly cultos: number;
    readonly cultosAnterior: number;
    readonly asistentesPromedio: number | null;
    readonly asistentesPromedioAnterior: number | null;
    readonly personasQueAsistieron: number;
    readonly personasQueAsistieronAnterior: number;
    readonly serie: readonly { readonly inicio: string; readonly cultos: number; readonly asistentesPromedio: number | null }[];
    readonly alertas: ConteoAlertas;
    readonly llegaron: number;
    readonly volvieron: number;
}

export interface EstadisticasConsolidacion {
    readonly periodo: PeriodoUsado;
    readonly rondasEnCurso: number;
    readonly casosAbiertos: number;
    readonly cerrados: number;
    readonly cerradosAnterior: number;
    readonly contactos: number;
    readonly contactosAnterior: number;
    readonly contactadas: number;
    readonly volvieron: number;
    readonly contactosPorMedio: readonly Conteo[];
    readonly cierresPorResultado: readonly Conteo[];
}

export interface EstadisticasAportes {
    readonly periodo: PeriodoUsado;
    readonly resumen: ResumenAportes;
    readonly serie: readonly { readonly inicio: string; readonly total: number; readonly cantidad: number }[];
}

/** Contrato de `EstadisticasUsoResponse` (iglesia-seguridad). */
export interface EstadisticasUso {
    readonly periodo: PeriodoUsado;
    readonly global: boolean;
    readonly usuarios: number;
    readonly usuariosNuevos: number;
    readonly activosPeriodo: number;
    readonly activosPeriodoAnterior: number;
    readonly activosDiarios: number;
    readonly activosDiariosAnterior: number;
    readonly activosSemanales: number;
    readonly activosMensuales: number;
    readonly iglesiasActivas: number | null;
    readonly iglesiasActivasAnterior: number | null;
    readonly serie: readonly PuntoSerie[];
    readonly porRolBase: readonly { readonly clave: Rol; readonly valor: number }[];
}

export interface EstadisticasSistema {
    readonly periodo: PeriodoUsado;
    readonly iglesias: number;
    readonly iglesiasNuevas: number;
    readonly iglesiasNuevasAnterior: number;
    readonly iglesiasConActividad: number;
    readonly iglesiasConActividadAnterior: number;
    readonly miembros: number;
    readonly altas: number;
    readonly altasAnterior: number;
    readonly bajas: number;
    readonly bajasAnterior: number;
    readonly serie: readonly {
        readonly inicio: string;
        readonly iglesiasNuevas: number;
        readonly altas: number;
        readonly bajas: number;
        readonly miembrosRegistrados: number;
        readonly asistenciasMarcadas: number;
        readonly contactosConsolidacion: number;
    }[];
    readonly porCondicion: readonly Conteo[];
    readonly porTamano: readonly Conteo[];
    readonly mayores: readonly { readonly iglesiaId: number; readonly nombre: string; readonly miembros: number }[];
}

/** Variación de una cifra frente al periodo anterior, para la flecha de las tarjetas. */
export interface Variacion {
    readonly tendencia: 'sube' | 'baja' | 'igual';
    /** «+12 %», «−3 %» o «Sin cambios». */
    readonly texto: string;
    /** Si el cambio es bueno: depende de si la cifra conviene que suba (asistencia) o que baje (bajas). */
    readonly favorable: boolean | null;
}

const PORCENTAJE = 100;

/**
 * Variación en % de `actual` frente a `anterior`. Sin periodo anterior con datos (nulo o cero) no hay con qué
 * comparar: devuelve `null` («Sin datos para comparar», nunca un engañoso «+100 %»).
 */
export function variacion(actual: number | null, anterior: number | null, subirEsBueno = true): Variacion | null {
    if (actual === null || anterior === null || anterior === 0) {
        return null;
    }
    const cambio = Math.round(((actual - anterior) / anterior) * PORCENTAJE);
    if (cambio === 0) {
        return { tendencia: 'igual', texto: 'Sin cambios', favorable: null };
    }
    const sube = cambio > 0;
    return { tendencia: sube ? 'sube' : 'baja', texto: `${sube ? '+' : '−'}${Math.abs(cambio)} %`, favorable: sube === subirEsBueno };
}

const FORMATO_DIA_MES = new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short', timeZone: 'UTC' });
const FORMATO_MES = new Intl.DateTimeFormat('es', { month: 'short', year: '2-digit', timeZone: 'UTC' });
const FORMATO_DIA = new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' });

const ETIQUETA_EJE: Readonly<Record<Granularidad, string>> = { DIA: 'Día', SEMANA: 'Semana', MES: 'Mes' };

/** Etiqueta del eje de una serie: «5 oct» por día o semana, «oct 26» por mes. */
export function etiquetaPunto(inicio: string, granularidad: Granularidad): string {
    const fecha = new Date(`${inicio}T00:00:00Z`);
    return (granularidad === 'MES' ? FORMATO_MES : FORMATO_DIA_MES).format(fecha).replace(/\./g, '');
}

/** Encabezado del eje de una serie, para la tabla accesible de los gráficos. */
export function etiquetaEje(granularidad: Granularidad): string {
    return ETIQUETA_EJE[granularidad];
}

/** «1 oct 2026 – 30 oct 2026», para decir a qué fechas corresponden las cifras. */
export function textoRango({ desde, hasta }: Pick<PeriodoUsado, 'desde' | 'hasta'>): string {
    const formato = (iso: string) => FORMATO_DIA.format(new Date(`${iso}T00:00:00Z`)).replace(/\./g, '');
    return `${formato(desde)} – ${formato(hasta)}`;
}

/** Número con separador de miles en español; `null` como «—». */
export function formatoNumero(valor: number | null, decimales = 0): string {
    return valor === null ? '—' : valor.toLocaleString('es-PE', { maximumFractionDigits: decimales, minimumFractionDigits: 0 });
}

/** Porcentaje entero de `parte` sobre `total`; sin total, `null`. */
export function porcentajeDe(parte: number, total: number): number | null {
    return total === 0 ? null : Math.round((parte / total) * PORCENTAJE);
}
