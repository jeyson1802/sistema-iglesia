import { leerUltimaSeleccion, recordarSeleccion } from './ultima-seleccion';

describe('última selección', () => {
    const almacen = (): Storage => {
        const datos = new Map<string, string>();
        return { getItem: (clave: string) => datos.get(clave) ?? null, setItem: (clave: string, valor: string) => void datos.set(clave, valor) } as Storage;
    };

    it('recuerda la categoría y el medio', () => {
        const local = almacen();
        recordarSeleccion({ categoriaId: 2, medio: 'YAPE' }, local);

        expect(leerUltimaSeleccion(local)).toEqual({ categoriaId: 2, medio: 'YAPE' });
    });

    it('sin datos, con datos dañados o sin almacenamiento no recuerda nada', () => {
        const dañado = { getItem: () => '{no es json', setItem: () => undefined } as unknown as Storage;
        const bloqueado = {
            getItem: () => {
                throw new Error('bloqueado');
            },
            setItem: () => {
                throw new Error('bloqueado');
            }
        } as unknown as Storage;

        expect(leerUltimaSeleccion(almacen())).toEqual({ categoriaId: null, medio: null });
        expect(leerUltimaSeleccion(dañado)).toEqual({ categoriaId: null, medio: null });
        expect(leerUltimaSeleccion(bloqueado)).toEqual({ categoriaId: null, medio: null });
        expect(() => recordarSeleccion({ categoriaId: 1, medio: 'EFECTIVO' }, bloqueado)).not.toThrow();
    });

    it('ignora un medio que ya no existe', () => {
        const local = almacen();
        local.setItem('aportes.ultimaSeleccion', JSON.stringify({ categoriaId: 1, medio: 'BITCOIN' }));

        expect(leerUltimaSeleccion(local)).toEqual({ categoriaId: 1, medio: null });
    });
});
