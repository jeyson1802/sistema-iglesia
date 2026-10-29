import { hoyIso } from '@/app/shared/utils/fecha';

/** Atajos de período de los filtros por rango de fechas (aportes, asistencia). */
export const PERIODOS = ['HOY', 'SEMANA', 'MES', 'MES_ANTERIOR', 'ANIO', 'PERSONALIZADO'] as const;
export type Periodo = (typeof PERIODOS)[number];

export const ETIQUETA_PERIODO: Readonly<Record<Periodo, string>> = {
    HOY: 'Hoy',
    SEMANA: 'Semana',
    MES: 'Mes',
    MES_ANTERIOR: 'Mes anterior',
    ANIO: 'Año',
    PERSONALIZADO: 'Personalizado'
};

export interface Rango {
    readonly desde: string;
    readonly hasta: string;
}

const MS_POR_DIA = 86_400_000;

/** Rango de un atajo respecto de `hoy`. La semana empieza el lunes; los rangos terminan hoy, salvo «Mes anterior». */
export function rangoDe(periodo: Exclude<Periodo, 'PERSONALIZADO'>, hoy: Date = new Date()): Rango {
    const dia = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    const hasta = hoyIso(dia);
    switch (periodo) {
        case 'HOY':
            return { desde: hasta, hasta };
        case 'SEMANA':
            return { desde: hoyIso(new Date(dia.getTime() - ((dia.getDay() + 6) % 7) * MS_POR_DIA)), hasta };
        case 'MES':
            return { desde: hoyIso(new Date(dia.getFullYear(), dia.getMonth(), 1)), hasta };
        case 'MES_ANTERIOR':
            return { desde: hoyIso(new Date(dia.getFullYear(), dia.getMonth() - 1, 1)), hasta: hoyIso(new Date(dia.getFullYear(), dia.getMonth(), 0)) };
        case 'ANIO':
            return { desde: hoyIso(new Date(dia.getFullYear(), 0, 1)), hasta };
    }
}

/** Atajo que corresponde a un rango, o «Personalizado» si no coincide con ninguno. */
export function periodoDe(rango: Rango, hoy: Date = new Date()): Periodo {
    const atajos = PERIODOS.filter((periodo): periodo is Exclude<Periodo, 'PERSONALIZADO'> => periodo !== 'PERSONALIZADO');
    return (
        atajos.find((periodo) => {
            const candidato = rangoDe(periodo, hoy);
            return candidato.desde === rango.desde && candidato.hasta === rango.hasta;
        }) ?? 'PERSONALIZADO'
    );
}

/** Convierte `yyyy-MM-dd` en una fecha local (sin desfase de zona horaria). */
export function aFecha(iso: string): Date {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia);
}

/** Días del rango, contando ambos extremos. */
export function diasDe({ desde, hasta }: Rango): number {
    return Math.round((aFecha(hasta).getTime() - aFecha(desde).getTime()) / MS_POR_DIA) + 1;
}
