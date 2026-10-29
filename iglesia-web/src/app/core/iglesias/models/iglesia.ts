/** Máximo de caracteres del nombre de una iglesia (columna `iglesia.nombre` de iglesia-api). */
export const NOMBRE_IGLESIA_MAX = 150;

/** Contrato de `IglesiaResponse` en iglesia-api. */
export interface IglesiaResponse {
    readonly id: number;
    readonly nombre: string;
    /** Instante ISO 8601 en UTC. */
    readonly creadaEn: string;
}

/** Iglesia del catálogo de tenants, tal como la usa la interfaz. */
export interface Iglesia {
    readonly id: number;
    readonly nombre: string;
    readonly creadaEn: Date;
}
