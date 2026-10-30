import { edadDesde, fechaConDia, hoyEnPalabras, hoyIso } from './fecha';

describe('hoyIso', () => {
    it('usa la fecha local con dos dígitos', () => {
        expect(hoyIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
    });
});

describe('fechaConDia y hoyEnPalabras', () => {
    it('muestran el día de la semana en español', () => {
        expect(fechaConDia('2026-10-25')).toContain('25/10/2026');
        expect(fechaConDia('2026-10-25').toLowerCase()).toContain('dom');
        expect(hoyEnPalabras(new Date(2026, 9, 29))).toBe('jueves, 29 de octubre');
    });
});

describe('edadDesde', () => {
    it('cuenta los años cumplidos, contando el día del cumpleaños', () => {
        expect(edadDesde('1995-04-12', new Date(2026, 3, 11))).toBe(30);
        expect(edadDesde('1995-04-12', new Date(2026, 3, 12))).toBe(31);
    });
});
