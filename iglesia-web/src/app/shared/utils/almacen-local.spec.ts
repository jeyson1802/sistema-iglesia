import { guardarLocal, leerLocal } from './almacen-local';

describe('almacen-local', () => {
    const roto = {
        getItem: () => {
            throw new Error('bloqueado');
        },
        setItem: () => {
            throw new Error('bloqueado');
        }
    } as unknown as Storage;

    it('guarda y lee una clave', () => {
        const almacen = new Map<string, string>();
        const falso = { getItem: (k: string) => almacen.get(k) ?? null, setItem: (k: string, v: string) => almacen.set(k, v) } as unknown as Storage;

        guardarLocal('clave', 'valor', falso);

        expect(leerLocal('clave', falso)).toBe('valor');
    });

    it('no lanza si el navegador bloquea el almacenamiento', () => {
        expect(() => guardarLocal('clave', 'valor', roto)).not.toThrow();
        expect(leerLocal('clave', roto)).toBeNull();
        expect(leerLocal('clave', null)).toBeNull();
    });
});
