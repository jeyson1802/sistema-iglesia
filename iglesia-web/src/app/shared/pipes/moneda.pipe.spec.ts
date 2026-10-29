import { formatearMonto, MonedaPipe } from './moneda.pipe';

describe('MonedaPipe', () => {
    const pipe = new MonedaPipe();

    it('formatea soles con símbolo, separador de miles y 2 decimales', () => {
        expect(pipe.transform(1250, 'PEN')).toBe('S/\u00a01,250.00');
        expect(pipe.transform(0.5, 'PEN')).toBe('S/\u00a00.50');
    });

    it('usa el símbolo o código de cada moneda (según el navegador)', () => {
        expect(formatearMonto(80, 'USD')).toContain('80.00');
        expect(formatearMonto(80, 'USD')).toMatch(/US\$|USD/);
        expect(formatearMonto(80, 'EUR')).toMatch(/€|EUR/);
        expect(formatearMonto(80, 'USD')).not.toContain('S/');
    });

    it('un monto vacío no muestra nada', () => {
        expect(pipe.transform(null, 'PEN')).toBe('');
        expect(pipe.transform(undefined, 'PEN')).toBe('');
    });
});
