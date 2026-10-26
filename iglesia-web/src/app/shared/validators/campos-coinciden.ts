import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const ERROR_CAMPOS_NO_COINCIDEN = 'camposNoCoinciden';

/**
 * Validador de grupo: el campo `confirmacion` debe ser igual a `campo`. Si la confirmación está vacía no
 * reporta nada, para que la valide su propio `required` y no se muestre el error antes de tiempo.
 */
export function camposCoinciden(campo: string, confirmacion: string): ValidatorFn {
    return (grupo: AbstractControl): ValidationErrors | null => {
        const valorConfirmacion = grupo.get(confirmacion)?.value;
        if (!valorConfirmacion || valorConfirmacion === grupo.get(campo)?.value) {
            return null;
        }
        return { [ERROR_CAMPOS_NO_COINCIDEN]: true };
    };
}
