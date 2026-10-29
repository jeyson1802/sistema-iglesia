import { Referencia } from '@/app/core/models/referencia';
import { Ubicacion } from '@/app/core/models/ubicacion';

export const GENEROS = ['MASCULINO', 'FEMENINO'] as const;
export type Genero = (typeof GENEROS)[number];
export const ETIQUETA_GENERO: Record<Genero, string> = { MASCULINO: 'Masculino', FEMENINO: 'Femenino' };

export const CONDICIONES = ['ANTIGUO', 'NUEVO', 'RECUPERADO'] as const;
export type Condicion = (typeof CONDICIONES)[number];
export const ETIQUETA_CONDICION: Record<Condicion, string> = { ANTIGUO: 'Antiguo', NUEVO: 'Nuevo', RECUPERADO: 'Recuperado' };

export const ESTADOS_CIVILES = ['SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO'] as const;
export type EstadoCivil = (typeof ESTADOS_CIVILES)[number];
export const ETIQUETA_ESTADO_CIVIL: Record<EstadoCivil, string> = { SOLTERO: 'Soltero(a)', CASADO: 'Casado(a)', DIVORCIADO: 'Divorciado(a)', VIUDO: 'Viudo(a)' };

export const NIVELES_ESTUDIOS = ['SIN_ESTUDIOS', 'PRIMARIA', 'SECUNDARIA', 'TECNICO', 'UNIVERSITARIO', 'POSGRADO'] as const;
export type NivelEstudios = (typeof NIVELES_ESTUDIOS)[number];
export const ETIQUETA_NIVEL_ESTUDIOS: Record<NivelEstudios, string> = {
    SIN_ESTUDIOS: 'Sin estudios',
    PRIMARIA: 'Primaria',
    SECUNDARIA: 'Secundaria',
    TECNICO: 'Técnico',
    UNIVERSITARIO: 'Universitario',
    POSGRADO: 'Posgrado'
};

/** Contrato de `MiembroResumenResponse` en iglesia-api: una fila del listado. */
export interface MiembroResumen {
    readonly id: number;
    readonly nombres: string;
    readonly apellidos: string;
    readonly condicion: Condicion | null;
    readonly nivel: Referencia | null;
    readonly lider: Referencia | null;
    readonly grupo: Referencia | null;
    readonly telefono: string | null;
    readonly esPastorPrincipal: boolean;
    readonly activo: boolean;
}

/** Contrato de `MiembroResponse` en iglesia-api. Las fechas sin hora vienen como `yyyy-MM-dd`. */
export interface MiembroResponse extends MiembroResumen {
    readonly genero: Genero | null;
    readonly fechaNacimiento: string | null;
    readonly edad: number | null;
    readonly edadEsAproximada: boolean;
    readonly fechaIngreso: string | null;
    readonly estadoCivil: EstadoCivil | null;
    readonly correo: string | null;
    readonly direccion: string | null;
    readonly distritoCodigo: string | null;
    readonly ubicacion: Ubicacion | null;
    readonly ocupacion: string | null;
    readonly profesion: string | null;
    readonly nivelEstudios: NivelEstudios | null;
    readonly centroEstudios: string | null;
    readonly peticion: string | null;
    readonly invitadoPor: Referencia | null;
    readonly registradoPor: Referencia | null;
    readonly cantidadLiderados: number;
    /** Instantes ISO 8601 en UTC. */
    readonly creadoEn: string;
    readonly actualizadoEn: string;
}

/** Ficha del miembro tal como la usa la interfaz. */
export interface Miembro extends Omit<MiembroResponse, 'creadoEn' | 'actualizadoEn'> {
    readonly creadoEn: Date;
    readonly actualizadoEn: Date;
}

/** Cuerpo de `GuardarMiembroRequest`; los textos vacíos se envían como `null`. */
export interface GuardarMiembro {
    readonly nombres: string;
    readonly apellidos: string;
    readonly genero: Genero | null;
    readonly fechaNacimiento: string | null;
    readonly edadAproximada: number | null;
    readonly fechaIngreso: string | null;
    readonly condicion: Condicion | null;
    readonly estadoCivil: EstadoCivil | null;
    readonly telefono: string | null;
    readonly correo: string | null;
    readonly direccion: string | null;
    readonly distritoCodigo: string | null;
    readonly ocupacion: string | null;
    readonly profesion: string | null;
    readonly nivelEstudios: NivelEstudios | null;
    readonly centroEstudios: string | null;
    readonly peticion: string | null;
    readonly nivelId: number | null;
    readonly liderId: number | null;
    readonly grupoId: number | null;
    readonly invitadoPorId: number | null;
    readonly registradoPorId: number | null;
}

/** Contrato de `EslabonCadenaResponse`: un líder de la cadena, del directo al pastor principal. */
export interface EslabonCadena {
    readonly id: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
    readonly esPastorPrincipal: boolean;
    readonly activo: boolean;
}

export interface FiltroMiembros {
    /** Parte de los nombres y apellidos; vacío = sin filtro. */
    readonly q: string;
    readonly condicion: Condicion | null;
    readonly nivelId: number | null;
    readonly grupoId: number | null;
    /** Solo los liderados directamente por este miembro. */
    readonly liderId: number | null;
    /** `null` = todos. */
    readonly activo: boolean | null;
}

/** Por defecto se listan solo los activos. */
export const FILTRO_MIEMBROS_INICIAL: FiltroMiembros = { q: '', condicion: null, nivelId: null, grupoId: null, liderId: null, activo: true };

/** Miembros por página en la tabla. */
export const PAGINA_MIEMBROS = 10;

/** Límites de iglesia-api (`LimitesMiembro`). */
export const LIMITES_MIEMBRO = {
    nombres: 100,
    apellidos: 100,
    telefono: 20,
    correo: 254,
    direccion: 200,
    ocupacion: 100,
    profesion: 100,
    centroEstudios: 150,
    peticion: 1000,
    edad: 120
} as const;

/** Mismo patrón que `LimitesMiembro.TELEFONO_PATRON`. */
export const PATRON_TELEFONO = /^[0-9 +-]*$/;

export const nombreCompleto = ({ nombres, apellidos }: Pick<MiembroResumen, 'nombres' | 'apellidos'>): string => `${nombres} ${apellidos}`;
