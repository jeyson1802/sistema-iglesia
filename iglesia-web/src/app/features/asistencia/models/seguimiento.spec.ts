import { haceCuanto, resumenSerie, semanaCorta, tendencia } from './seguimiento';

describe('seguimiento', () => {
    it('la tendencia necesita al menos 5 puntos de diferencia', () => {
        expect(tendencia(80, 60)).toBe('sube');
        expect(tendencia(50, 80)).toBe('baja');
        expect(tendencia(64, 60)).toBe('igual');
        expect(tendencia(null, 60)).toBeNull();
        expect(tendencia(60, null)).toBeNull();
    });

    it('dice hace cuánto fue en días de calendario', () => {
        const ahora = new Date(2026, 9, 29, 23, 30);
        expect(haceCuanto('2026-10-29', ahora)).toBe('hoy');
        expect(haceCuanto('2026-10-28', ahora)).toBe('ayer');
        expect(haceCuanto('2026-10-17', ahora)).toBe('hace 12 días');
        expect(haceCuanto(null, ahora)).toBe('nunca');
    });

    it('abrevia la semana con el día y el mes', () => {
        expect(semanaCorta('2026-10-26')).toBe('26 oct');
    });
});

describe('resumenSerie', () => {
    it('resume las semanas cerradas y deja fuera la actual', () => {
        expect(resumenSerie([10, 20, 30, 5])).toEqual({ promedio: 20, maximo: 30, semanaPasada: 30, variacion: 100 });
    });

    it('ignora las semanas sin dato y en porcentajes compara en puntos', () => {
        expect(resumenSerie([null, 60, 70, 40], true)).toEqual({ promedio: 65, maximo: 70, semanaPasada: 70, variacion: 10 });
    });

    it('sin datos todo queda vacío', () => {
        expect(resumenSerie([null, null])).toEqual({ promedio: null, maximo: null, semanaPasada: null, variacion: null });
    });
});
