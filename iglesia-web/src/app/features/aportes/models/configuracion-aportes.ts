/** Contrato de `CategoriaAporteResponse` en iglesia-api. */
export interface CategoriaAporte {
    readonly id: number;
    readonly nombre: string;
    readonly exigeAportante: boolean;
    readonly orden: number;
    readonly activo: boolean;
}

/** Datos que se envían al crear o editar una categoría. */
export interface GuardarCategoriaAporte {
    readonly nombre: string;
    readonly exigeAportante: boolean;
}

/** Contrato de `ConfiguracionAportesResponse`. */
export interface ConfiguracionAportes {
    readonly moneda: string;
    /** `false` cuando ya hay aportes vigentes. */
    readonly monedaEditable: boolean;
}

/** Contrato de `MonedaResponse`. */
export interface Moneda {
    readonly codigo: string;
    readonly nombre: string;
    readonly simbolo: string;
}

export const MONEDA_POR_DEFECTO = 'PEN';

/** Límite de iglesia-api (`LimitesAporte.NOMBRE_CATEGORIA_MAX`). */
export const NOMBRE_CATEGORIA_MAX = 60;
