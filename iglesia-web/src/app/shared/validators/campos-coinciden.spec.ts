import { FormControl, FormGroup } from '@angular/forms';
import { camposCoinciden } from './campos-coinciden';

describe('camposCoinciden', () => {
    const grupo = (nueva: string, confirmacion: string): FormGroup =>
        new FormGroup({ nueva: new FormControl(nueva), confirmacion: new FormControl(confirmacion) }, { validators: camposCoinciden('nueva', 'confirmacion') });

    it('es válido si ambos valores coinciden', () => {
        expect(grupo('Clave12345', 'Clave12345').errors).toBeNull();
    });

    it('reporta el error si no coinciden', () => {
        expect(grupo('Clave12345', 'Otra12345').errors).toEqual({ camposNoCoinciden: true });
    });

    it('no reporta nada mientras la confirmación está vacía', () => {
        expect(grupo('Clave12345', '').errors).toBeNull();
    });
});
