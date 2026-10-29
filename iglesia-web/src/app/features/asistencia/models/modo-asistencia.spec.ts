import { modoInicial, recordarModo } from './modo-asistencia';

describe('modo-asistencia', () => {
    const almacenVacio = (): Storage => {
        const datos = new Map<string, string>();
        return { getItem: (clave: string) => datos.get(clave) ?? null, setItem: (clave: string, valor: string) => datos.set(clave, valor) } as unknown as Storage;
    };

    it('sin nada recordado: manual en grupos, cámara en el celular y lector en la computadora', () => {
        expect(modoInicial('GRUPO', true, almacenVacio())).toBe('MANUAL');
        expect(modoInicial('CULTO', true, almacenVacio())).toBe('CAMARA');
        expect(modoInicial('EVENTO', false, almacenVacio())).toBe('LECTOR');
    });

    it('recuerda el último modo por tipo de reunión', () => {
        const almacen = almacenVacio();

        recordarModo('CULTO', 'MANUAL', almacen);

        expect(modoInicial('CULTO', true, almacen)).toBe('MANUAL');
        expect(modoInicial('EVENTO', true, almacen)).toBe('CAMARA');
    });

    it('sin almacenamiento o con un valor extraño usa el modo por defecto', () => {
        const roto = {
            getItem: () => {
                throw new Error('bloqueado');
            }
        } as unknown as Storage;

        expect(modoInicial('CULTO', false, roto)).toBe('LECTOR');
        expect(modoInicial('CULTO', false, { getItem: () => 'OTRO' } as unknown as Storage)).toBe('LECTOR');
        expect(() => recordarModo('CULTO', 'CAMARA', null)).not.toThrow();
    });
});
