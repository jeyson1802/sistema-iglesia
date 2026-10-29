import { Referencia } from '@/app/core/models/referencia';
import { Ubicacion } from '@/app/core/models/ubicacion';

export const DIAS_SEMANA = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'] as const;
export type DiaSemana = (typeof DIAS_SEMANA)[number];

export const ETIQUETA_DIA: Record<DiaSemana, string> = {
    LUNES: 'Lunes',
    MARTES: 'Martes',
    MIERCOLES: 'Miércoles',
    JUEVES: 'Jueves',
    VIERNES: 'Viernes',
    SABADO: 'Sábado',
    DOMINGO: 'Domingo'
};

/** Contrato de `GrupoResponse` en iglesia-api. Un grupo no tiene nombre: es «el grupo de» su líder. */
export interface GrupoResponse {
    readonly id: number;
    readonly lider: Referencia;
    readonly anfitrion: Referencia | null;
    readonly diaReunion: DiaSemana | null;
    /** `HH:mm:ss` (LocalTime). */
    readonly horaReunion: string | null;
    readonly direccion: string | null;
    readonly distritoCodigo: string | null;
    readonly ubicacion: Ubicacion | null;
    readonly descripcion: string | null;
    readonly activo: boolean;
    readonly cantidadMiembros: number;
}

/** Grupo pequeño de la iglesia tal como lo usa la interfaz (la hora como `HH:mm`). */
export interface Grupo extends Omit<GrupoResponse, 'horaReunion'> {
    readonly horaReunion: string | null;
}

/** Cuerpo de `GuardarGrupoRequest`; los textos vacíos se envían como `null`. */
export interface GuardarGrupo {
    readonly liderId: number;
    readonly anfitrionId: number | null;
    readonly diaReunion: DiaSemana | null;
    /** `HH:mm`. */
    readonly horaReunion: string | null;
    readonly direccion: string | null;
    readonly distritoCodigo: string | null;
    readonly descripcion: string | null;
}

/** Contrato de `IntegranteGrupoResponse`: un miembro activo del grupo. */
export interface IntegranteGrupo {
    readonly id: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
}

/** Contrato de `CandidatoGrupoResponse`: un miembro que se puede agregar, con el grupo en el que está ahora. */
export interface CandidatoGrupo extends IntegranteGrupo {
    /** Grupo actual; su `nombre` es el de su líder. */
    readonly grupo: Referencia | null;
}

/** Cómo se muestra un grupo: «<nombre singular> de <líder>», p. ej. «Célula de Ana Ríos». */
export const nombreDeGrupo = (nombreSingular: string, nombreLider: string): string => `${nombreSingular} de ${nombreLider}`;

/** Límites de iglesia-api (`LimitesGrupo`). */
export const DIRECCION_GRUPO_MAX = 200;
export const DESCRIPCION_GRUPO_MAX = 500;
export const NOMBRE_TIPO_GRUPO_MAX = 40;
