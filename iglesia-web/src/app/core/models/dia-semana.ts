/** Día de la semana de iglesia-api (`DiaSemana`): reunión de un grupo o culto programado. */
export const DIAS_SEMANA = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'] as const;
export type DiaSemana = (typeof DIAS_SEMANA)[number];

export const ETIQUETA_DIA: Readonly<Record<DiaSemana, string>> = {
    LUNES: 'Lunes',
    MARTES: 'Martes',
    MIERCOLES: 'Miércoles',
    JUEVES: 'Jueves',
    VIERNES: 'Viernes',
    SABADO: 'Sábado',
    DOMINGO: 'Domingo'
};

/** Día de la semana de una fecha local (`Date.getDay()` empieza en domingo). */
export function diaSemanaDe(fecha: Date): DiaSemana {
    return DIAS_SEMANA[(fecha.getDay() + 6) % 7];
}
