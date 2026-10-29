import { hoyIso } from './fecha';

describe('hoyIso', () => {
    it('usa la fecha local con dos dígitos', () => {
        expect(hoyIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
    });
});
