import { diaSemanaDe, ETIQUETA_DIA } from './dia-semana';

describe('diaSemanaDe', () => {
    it('convierte el día de una fecha local, empezando la semana en lunes', () => {
        expect(diaSemanaDe(new Date(2026, 9, 26))).toBe('LUNES');
        expect(diaSemanaDe(new Date(2026, 9, 29))).toBe('JUEVES');
        expect(diaSemanaDe(new Date(2026, 10, 1))).toBe('DOMINGO');
    });

    it('tiene la etiqueta con tilde', () => {
        expect(ETIQUETA_DIA.MIERCOLES).toBe('Miércoles');
    });
});
