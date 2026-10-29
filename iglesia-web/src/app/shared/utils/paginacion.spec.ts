import { paginaACargar, paginaPedida } from './paginacion';

describe('paginacion', () => {
    it('convierte el primer registro y las filas en página y tamaño', () => {
        expect(paginaPedida({ first: 40, rows: 20 }, 10)).toEqual({ pagina: 2, tamano: 20 });
        expect(paginaPedida({}, 10)).toEqual({ pagina: 0, tamano: 10 });
    });

    it('al cambiar el tamaño vuelve a la primera página', () => {
        expect(paginaACargar({ pagina: 3, tamano: 10 }, 10)).toBe(3);
        expect(paginaACargar({ pagina: 3, tamano: 50 }, 10)).toBe(0);
    });
});
