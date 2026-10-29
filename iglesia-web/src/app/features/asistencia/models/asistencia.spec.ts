import { horaCorta } from './asistencia';

describe('horaCorta', () => {
    it('deja la hora en horas y minutos', () => {
        expect(horaCorta('19:30:00')).toBe('19:30');
        expect(horaCorta('07:05')).toBe('07:05');
        expect(horaCorta(null)).toBeNull();
    });
});
