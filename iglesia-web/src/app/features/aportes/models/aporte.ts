import { Referencia } from '@/app/core/models/referencia';

/** Medios de entrega de un aporte: lista fija de iglesia-api (`MedioAporte`). */
export const MEDIOS_APORTE = ['EFECTIVO', 'TRANSFERENCIA', 'YAPE', 'PLIN', 'CHEQUE', 'OTRO'] as const;
export type MedioAporte = (typeof MEDIOS_APORTE)[number];

export interface DescripcionMedio {
    readonly etiqueta: string;
    readonly icono: string;
    /** Ayuda del número de operación; `null` si el medio no lo usa. */
    readonly ayudaOperacion: string | null;
}

export const DESCRIPCION_MEDIO: Readonly<Record<MedioAporte, DescripcionMedio>> = {
    EFECTIVO: { etiqueta: 'Efectivo', icono: 'pi pi-money-bill', ayudaOperacion: null },
    TRANSFERENCIA: { etiqueta: 'Transferencia', icono: 'pi pi-building-columns', ayudaOperacion: 'Número de operación de la transferencia' },
    YAPE: { etiqueta: 'Yape', icono: 'pi pi-mobile', ayudaOperacion: 'Código de operación de Yape' },
    PLIN: { etiqueta: 'Plin', icono: 'pi pi-mobile', ayudaOperacion: 'Código de operación de Plin' },
    CHEQUE: { etiqueta: 'Cheque', icono: 'pi pi-file-edit', ayudaOperacion: 'Número del cheque' },
    OTRO: { etiqueta: 'Otro', icono: 'pi pi-ellipsis-h', ayudaOperacion: 'Referencia de la operación (opcional)' }
};

export const esMedioAporte = (valor: string | null): valor is MedioAporte => valor !== null && (MEDIOS_APORTE as readonly string[]).includes(valor);

/** Contrato de `AporteResponse` en iglesia-api. El monto llega como número decimal. */
export interface Aporte {
    readonly id: number;
    /** `yyyy-MM-dd`. */
    readonly fecha: string;
    /** `null` si es anónimo. */
    readonly miembro: Referencia | null;
    readonly categoria: Referencia;
    readonly monto: number;
    readonly medio: MedioAporte;
    readonly numeroOperacion: string | null;
    readonly peticion: string | null;
    readonly observacion: string | null;
    readonly anulado: boolean;
    readonly motivoAnulacion: string | null;
    readonly registradoEn: string;
    readonly modificadoEn: string | null;
}

/** Contrato de `GuardarAporteRequest`. */
export interface GuardarAporte {
    readonly fecha: string;
    readonly miembroId: number | null;
    readonly categoriaId: number;
    readonly monto: number;
    readonly medio: MedioAporte;
    readonly numeroOperacion: string | null;
    readonly peticion: string | null;
    readonly observacion: string | null;
}

/** Filtros del listado y del resumen; viven en la URL de `/aportes`. */
export interface FiltroAportes {
    readonly desde: string;
    readonly hasta: string;
    readonly categoriaId: number | null;
    readonly medio: MedioAporte | null;
    readonly miembroId: number | null;
    readonly incluirAnulados: boolean;
}

export interface TotalCategoria {
    readonly categoria: Referencia;
    readonly total: number;
    readonly cantidad: number;
}

export interface TotalMedio {
    readonly medio: MedioAporte;
    readonly total: number;
    readonly cantidad: number;
}

/** Contrato de `ResumenAportesResponse`: solo aportes vigentes (sin anulados). */
export interface ResumenAportes {
    readonly moneda: string;
    readonly total: number;
    readonly cantidad: number;
    readonly totalPeriodoAnterior: number;
    readonly porCategoria: readonly TotalCategoria[];
    readonly porMedio: readonly TotalMedio[];
}

/** Límites de iglesia-api (`LimitesAporte`). */
export const LIMITES_APORTE = {
    montoMaximo: 9_999_999_999.99,
    numeroOperacion: 40,
    peticion: 1000,
    observacion: 500,
    motivoAnulacion: 300,
    rangoMaximoDias: 366
} as const;

/** Aportes por página al abrir el listado (las opciones del paginador son las comunes). */
export const APORTES_POR_PAGINA = 20;
