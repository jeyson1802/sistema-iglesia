import { convertToParamMap } from '@angular/router';
import { filtroAUrl, filtroDesdeUrl, filtroInicial } from './filtro-url';

describe('filtro en la URL', () => {
    const hoy = new Date(2026, 9, 29);

    it('sin parámetros es el mes en curso sin más filtros', () => {
        expect(filtroInicial(hoy)).toEqual({ desde: '2026-10-01', hasta: '2026-10-29', categoriaId: null, medio: null, miembroId: null, incluirAnulados: false });
    });

    it('lee todos los filtros', () => {
        const filtro = filtroDesdeUrl(convertToParamMap({ desde: '2026-09-01', hasta: '2026-09-30', categoriaId: '2', medio: 'YAPE', miembroId: '12', incluirAnulados: 'true' }), hoy);

        expect(filtro).toEqual({ desde: '2026-09-01', hasta: '2026-09-30', categoriaId: 2, medio: 'YAPE', miembroId: 12, incluirAnulados: true });
    });

    it('descarta valores inválidos y un rango invertido', () => {
        const filtro = filtroDesdeUrl(convertToParamMap({ desde: '2026-10-20', hasta: '2026-10-01', categoriaId: 'x', medio: 'BITCOIN', miembroId: '-3' }), hoy);

        expect(filtro).toEqual(filtroInicial(hoy));
    });

    it('un enlace con solo el miembro conserva el mes en curso', () => {
        expect(filtroDesdeUrl(convertToParamMap({ miembroId: '12' }), hoy).miembroId).toBe(12);
    });

    it('escribe solo los filtros con valor', () => {
        expect(filtroAUrl({ desde: '2026-10-01', hasta: '2026-10-29', categoriaId: 1, medio: null, miembroId: null, incluirAnulados: false })).toEqual({
            desde: '2026-10-01',
            hasta: '2026-10-29',
            categoriaId: 1,
            medio: undefined,
            miembroId: undefined,
            incluirAnulados: undefined
        });
    });
});
