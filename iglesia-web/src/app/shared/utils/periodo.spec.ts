import { aFecha, diasDe, periodoDe, rangoDe } from './periodo';

describe('periodo', () => {
    // Miércoles 29 de octubre de 2026.
    const hoy = new Date(2026, 9, 29, 15, 30);

    it('calcula los rangos de los atajos terminando hoy', () => {
        expect(rangoDe('HOY', hoy)).toEqual({ desde: '2026-10-29', hasta: '2026-10-29' });
        expect(rangoDe('SEMANA', hoy)).toEqual({ desde: '2026-10-26', hasta: '2026-10-29' });
        expect(rangoDe('MES', hoy)).toEqual({ desde: '2026-10-01', hasta: '2026-10-29' });
        expect(rangoDe('ANIO', hoy)).toEqual({ desde: '2026-01-01', hasta: '2026-10-29' });
    });

    it('el mes anterior va completo, también al cruzar el año', () => {
        expect(rangoDe('MES_ANTERIOR', hoy)).toEqual({ desde: '2026-09-01', hasta: '2026-09-30' });
        expect(rangoDe('MES_ANTERIOR', new Date(2026, 0, 10))).toEqual({ desde: '2025-12-01', hasta: '2025-12-31' });
    });

    it('la semana empieza el lunes aunque hoy sea domingo', () => {
        expect(rangoDe('SEMANA', new Date(2026, 10, 1))).toEqual({ desde: '2026-10-26', hasta: '2026-11-01' });
    });

    it('reconoce el atajo de un rango o lo marca como personalizado', () => {
        expect(periodoDe({ desde: '2026-10-01', hasta: '2026-10-29' }, hoy)).toBe('MES');
        expect(periodoDe({ desde: '2026-10-05', hasta: '2026-10-20' }, hoy)).toBe('PERSONALIZADO');
    });

    it('cuenta los días del rango con ambos extremos y lee fechas sin desfase', () => {
        expect(diasDe({ desde: '2026-10-01', hasta: '2026-10-31' })).toBe(31);
        expect(aFecha('2026-03-05').getDate()).toBe(5);
    });
});
