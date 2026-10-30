import { digitosTelefono, enlaceLlamada, enlaceWhatsapp, telefonoInternacional, tieneTelefono } from './telefono';

describe('telefono', () => {
    it('se queda solo con los dígitos', () => {
        expect(digitosTelefono('(01) 456-7890')).toBe('014567890');
        expect(digitosTelefono(null)).toBe('');
    });

    it('antepone el código del país a un celular local y respeta uno internacional', () => {
        expect(telefonoInternacional('987 654 321')).toBe('51987654321');
        expect(telefonoInternacional('+51 987 654 321')).toBe('51987654321');
        expect(telefonoInternacional('+1 305 555 0100')).toBe('13055550100');
    });

    it('arma los enlaces de llamada y de WhatsApp', () => {
        expect(enlaceLlamada('987654321')).toBe('tel:+51987654321');
        expect(enlaceWhatsapp('987654321')).toBe('https://wa.me/51987654321');
    });

    it('un teléfono vacío o muy corto no sirve para contactar', () => {
        expect(tieneTelefono(null)).toBeFalse();
        expect(tieneTelefono('123')).toBeFalse();
        expect(tieneTelefono('987654321')).toBeTrue();
    });
});
