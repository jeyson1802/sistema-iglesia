import { dateAHora, horaADate } from './hora';

describe('hora', () => {
    it('convierte la hora de la API en fecha y de vuelta', () => {
        const fecha = horaADate('19:30:00');

        expect(fecha?.getHours()).toBe(19);
        expect(fecha?.getMinutes()).toBe(30);
        expect(dateAHora(fecha)).toBe('19:30');
    });

    it('sin hora queda nulo', () => {
        expect(horaADate(null)).toBeNull();
        expect(dateAHora(null)).toBeNull();
    });
});
