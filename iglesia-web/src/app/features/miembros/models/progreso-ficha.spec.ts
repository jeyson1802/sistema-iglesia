import { JUAN, MINIMO } from '../testing/miembros-prueba';
import { calcularProgreso } from './progreso-ficha';

describe('calcularProgreso', () => {
    it('con solo nombres y apellidos lista todo lo demás como faltante', () => {
        const progreso = calcularProgreso(MINIMO, 'Célula');

        expect(progreso.porcentaje).toBe(11);
        expect(progreso.faltan[0]).toBe('Género');
        expect(progreso.faltan).toContain('Célula');
        expect(progreso.faltan).not.toContain('Nombres');
    });

    it('una ficha completa llega al 100 %', () => {
        const completa = { ...JUAN, direccion: 'Av. Perú 123', invitadoPor: JUAN.lider, nivelEstudios: 'SECUNDARIA' as const, centroEstudios: 'Colegio', ocupacion: 'Chofer', profesion: 'Mecánico' };

        expect(calcularProgreso(completa)).toEqual({ porcentaje: 100, faltan: [] });
    });

    it('al pastor principal no le falta el líder', () => {
        expect(calcularProgreso({ ...MINIMO, esPastorPrincipal: true }).faltan).not.toContain('Líder');
        expect(calcularProgreso(MINIMO).faltan).toContain('Líder');
    });
});
