import { DiaSemana } from '@/app/core/models/dia-semana';
import { SemanaAsistencia, TipoAlerta } from './seguimiento';

export const TIPOS_REUNION = ['CULTO', 'EVENTO', 'GRUPO'] as const;
export type TipoReunion = (typeof TIPOS_REUNION)[number];

export const ETIQUETA_TIPO_REUNION: Readonly<Record<TipoReunion, string>> = { CULTO: 'Culto', EVENTO: 'Evento', GRUPO: 'Grupo' };

/** Severidad de `p-tag` por tipo, para distinguirlos de un vistazo. */
export const SEVERIDAD_TIPO_REUNION: Readonly<Record<TipoReunion, 'info' | 'warn' | 'success'>> = { CULTO: 'info', EVENTO: 'warn', GRUPO: 'success' };

export type MedioAsistencia = 'QR' | 'MANUAL';

export const DESCRIPCION_MEDIO_ASISTENCIA: Readonly<Record<MedioAsistencia, { readonly etiqueta: string; readonly icono: string }>> = {
    QR: { etiqueta: 'Con QR', icono: 'pi pi-qrcode' },
    MANUAL: { etiqueta: 'A mano', icono: 'pi pi-pencil' }
};

/** Límites de iglesia-api (`LimitesAsistencia`): se repiten aquí para validar en el formulario. */
export const LIMITES_ASISTENCIA = {
    nombreCultoMin: 2,
    nombreCultoMax: 60,
    nombreEventoMin: 2,
    nombreEventoMax: 100,
    diasEventoMax: 7,
    busquedaMin: 2,
    ultimasMarcas: 5
} as const;

/** Contrato de `CultoProgramadoResponse`. */
export interface CultoProgramado {
    readonly id: number;
    readonly nombre: string;
    readonly diaSemana: DiaSemana;
    /** `HH:mm:ss`. */
    readonly hora: string;
    readonly activo: boolean;
}

export interface GuardarCulto {
    readonly nombre: string;
    readonly diaSemana: DiaSemana;
    /** `HH:mm`. */
    readonly hora: string;
}

/** Contrato de `ReunionHoyResponse`: sin `id`, la reunión aún no se abrió. */
export interface ReunionHoy {
    readonly id: number | null;
    readonly tipo: TipoReunion;
    readonly nombre: string;
    readonly hora: string | null;
    readonly cultoProgramadoId: number | null;
    readonly grupoId: number | null;
    readonly totalAsistentes: number;
}

/** Contrato de `ReunionResponse`. */
export interface Reunion {
    readonly id: number;
    readonly tipo: TipoReunion;
    readonly nombre: string;
    /** `yyyy-MM-dd`. */
    readonly fecha: string;
    readonly hora: string | null;
    readonly cultoProgramadoId: number | null;
    readonly grupoId: number | null;
    readonly totalAsistentes: number;
}

/** Culto o grupo (uno de los dos) y la fecha de la reunión que se abre. */
export interface AbrirReunion {
    readonly cultoProgramadoId: number | null;
    readonly grupoId: number | null;
    readonly fecha: string;
}

export interface CrearEvento {
    readonly nombre: string;
    readonly fechaInicio: string;
    readonly fechaFin: string | null;
    readonly hora: string | null;
}

export interface FiltroReuniones {
    readonly desde: string;
    readonly hasta: string;
    readonly tipo: TipoReunion | null;
}

/** Contrato de `MarcaResponse`. */
export interface Marca {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
    readonly grupoId: number | null;
    readonly yaRegistrado: boolean;
    readonly registradaEn: string;
    readonly totalAsistentes: number;
}

/** Contrato de `AsistenteResponse`. */
export interface Asistente {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
    readonly grupoId: number | null;
    readonly medio: MedioAsistencia;
    readonly registradaEn: string;
}

/** Contrato de `MiembroReunionResponse`: miembro de la lista manual con si ya está presente. */
export interface MiembroReunion {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
    readonly grupoId: number | null;
    readonly integrante: boolean;
    readonly presente: boolean;
}

/** Contrato de `ReunionAsistidaResponse`. */
export interface ReunionAsistida {
    readonly id: number;
    readonly tipo: TipoReunion;
    readonly nombre: string;
    readonly fecha: string;
}

/**
 * Contrato de `AsistenciaMiembroResponse`: seguimiento de un miembro. Le corresponden los días de culto (los cultos de
 * una fecha cuentan como uno) y las reuniones de su grupo; los eventos no son falta.
 */
export interface AsistenciaMiembro {
    /** Ocasiones que le correspondían en las últimas 8 semanas. */
    readonly reunionesQueCuentan: number;
    readonly asistidas: number;
    /** Redondeado; `null` si en esas semanas no le correspondía ninguna ocasión. */
    readonly porcentaje: number | null;
    /** Porcentaje de las 8 semanas anteriores, para la tendencia. */
    readonly porcentajeAnterior: number | null;
    readonly faltasSeguidas: number;
    /** `yyyy-MM-dd`; `null` si nunca asistió. */
    readonly ultimaAsistencia: string | null;
    readonly alertas: readonly TipoAlerta[];
    /** Últimas 12 semanas, de la más antigua a la actual. */
    readonly semanas: readonly SemanaAsistencia[];
    readonly ultimas: readonly ReunionAsistida[];
}

/** `HH:mm:ss` o `HH:mm` → `HH:mm`. */
export function horaCorta(hora: string | null): string | null {
    return hora === null ? null : hora.slice(0, 5);
}
