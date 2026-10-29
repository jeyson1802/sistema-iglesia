import { faltaDireccion, iniciales } from './iglesia';
import { errorDeLogo, LOGO_MAX_BYTES } from './logo';

describe('modelo de iglesia', () => {
    it('saca las iniciales de las dos primeras palabras del nombre comercial', () => {
        expect(iniciales('MMM Lima Centro')).toBe('ML');
        expect(iniciales('  sion  ')).toBe('S');
        expect(iniciales('Ágape - Norte')).toBe('ÁN');
    });

    it('sabe si a una iglesia le falta la dirección', () => {
        expect(faltaDireccion({ direccion: null, distritoCodigo: '150132' })).toBeTrue();
        expect(faltaDireccion({ direccion: 'Av. 1', distritoCodigo: null })).toBeTrue();
        expect(faltaDireccion({ direccion: 'Av. 1', distritoCodigo: '150132' })).toBeFalse();
    });

    it('valida el logo antes de subirlo', () => {
        expect(errorDeLogo({ type: 'image/png', size: LOGO_MAX_BYTES })).toBeNull();
        expect(errorDeLogo({ type: 'image/svg+xml', size: 10 })).toContain('SVG no está permitido');
        expect(errorDeLogo({ type: 'image/jpeg', size: 2.3 * 1024 * 1024 })).toBe('La imagen pesa 2.3 MB; elige una de hasta 1 MB.');
    });
});
