import { textoOpcional } from './texto';

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
