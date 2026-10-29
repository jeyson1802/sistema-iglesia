import { aIglesia } from './iglesia.mapper';

describe('aIglesia', () => {
    it('adapta una iglesia de la API con la fecha de alta como Date', () => {
        expect(aIglesia({ id: 3, nombre: 'Iglesia Central', creadaEn: '2026-10-28T15:30:00Z' })).toEqual({
            id: 3,
            nombre: 'Iglesia Central',
            creadaEn: new Date(Date.UTC(2026, 9, 28, 15, 30))
        });
    });
});
