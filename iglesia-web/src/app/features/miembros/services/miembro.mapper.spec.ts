import { RESPUESTA_JUAN } from '../testing/miembros-prueba';
import { aMiembro } from './miembro.mapper';

describe('aMiembro', () => {
    it('convierte los instantes en fechas y conserva el resto', () => {
        const miembro = aMiembro(RESPUESTA_JUAN);

        expect(miembro.creadoEn).toEqual(new Date('2026-10-01T15:00:00Z'));
        expect(miembro.actualizadoEn).toEqual(new Date('2026-10-02T15:00:00Z'));
        expect(miembro.fechaNacimiento).toBe('1990-05-14');
    });
});
