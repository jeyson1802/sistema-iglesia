import { coincideBusqueda, normalizarBusqueda, textoOpcional } from './texto';

describe('textoOpcional', () => {
    it('quita los espacios sobrantes', () => {
        expect(textoOpcional('  Av. Perú ')).toBe('Av. Perú');
    });

    it('un texto vacío o en blanco es nulo', () => {
        expect(textoOpcional('')).toBeNull();
        expect(textoOpcional('   ')).toBeNull();
        expect(textoOpcional(null)).toBeNull();
    });
});

describe('búsqueda sin tildes', () => {
    it('normaliza quitando tildes, mayúsculas y espacios de los extremos', () => {
        expect(normalizarBusqueda('  Óscar Díaz ')).toBe('oscar diaz');
    });

    it('coincide sin distinguir tildes ni mayúsculas y con búsqueda vacía', () => {
        expect(coincideBusqueda('Raquel Cordero Palomino', 'palómino')).toBeTrue();
        expect(coincideBusqueda('Raquel Cordero', 'luis')).toBeFalse();
        expect(coincideBusqueda('Raquel Cordero', '  ')).toBeTrue();
    });
});
