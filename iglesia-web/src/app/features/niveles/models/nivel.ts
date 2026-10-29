/** Contrato de `NivelResponse` en iglesia-api: un rango de la iglesia, en su orden jerárquico (1 = el más alto). */
export interface Nivel {
    readonly id: number;
    readonly nombre: string;
    readonly orden: number;
    readonly activo: boolean;
}

/** Límite de iglesia-api (`LimitesNivel`). */
export const NOMBRE_NIVEL_MAX = 60;
