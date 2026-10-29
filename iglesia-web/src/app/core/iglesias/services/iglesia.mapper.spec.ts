import { iglesiaRespuesta } from '../testing/iglesia-respuesta';
import { aIglesia } from './iglesia.mapper';

describe('aIglesia', () => {
    it('adapta una iglesia de la API con las fechas como Date', () => {
        const iglesia = aIglesia(iglesiaRespuesta({ tieneLogo: true, logoActualizadoEn: '2026-10-29T10:00:00Z' }));

        expect(iglesia.creadaEn).toEqual(new Date(Date.UTC(2026, 9, 28, 15, 30)));
        expect(iglesia.logoActualizadoEn).toEqual(new Date(Date.UTC(2026, 9, 29, 10)));
        expect(iglesia.nombreComercial).toBe('Iglesia Central');
    });

    it('sin logo deja la fecha del logo en null', () => {
        expect(aIglesia(iglesiaRespuesta()).logoActualizadoEn).toBeNull();
    });
});
