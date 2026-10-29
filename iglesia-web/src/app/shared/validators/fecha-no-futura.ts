import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { hoyIso } from '@/app/shared/utils/fecha';

export const ERROR_FECHA_FUTURA = 'fechaFutura';

/** Rechaza una fecha `yyyy-MM-dd` posterior a hoy (las fechas ISO se comparan bien como texto). Vacía es válida. */
export const fechaNoFutura =
    (hoy: () => string = hoyIso): ValidatorFn =>
    (control: AbstractControl<string | null>): ValidationErrors | null =>
        control.value && control.value > hoy() ? { [ERROR_FECHA_FUTURA]: true } : null;
