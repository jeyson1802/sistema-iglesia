import { FILTRO_MIEMBROS_INICIAL } from './miembro';
import { filtroFotochecksAUrl, seleccionDesdeUrl } from './fotochecks-url';

describe('fotochecks-url', () => {
    it('lleva a la URL solo los filtros con valor y los recupera igual', () => {
        const filtro = { ...FILTRO_MIEMBROS_INICIAL, q: ' ana ', nivelId: 3, ubigeo: '1501' };

        const parametros = filtroFotochecksAUrl(filtro);

        expect(parametros).toEqual({ q: 'ana', nivelId: 3, ubigeo: '1501', activo: true });
        expect(seleccionDesdeUrl({ q: 'ana', nivelId: '3', ubigeo: '1501', activo: 'true' })).toEqual({ filtro: { ...filtro, q: 'ana' } });
    });

    it('con miembroId imprime solo ese miembro', () => {
        expect(seleccionDesdeUrl({ miembroId: '12' })).toEqual({ miembroId: 12 });
    });

    it('ignora los valores que no entiende', () => {
        expect(seleccionDesdeUrl({ nivelId: 'x', ubigeo: '15A', condicion: 'OTRA', activo: 'quizas' })).toEqual({
            filtro: { ...FILTRO_MIEMBROS_INICIAL, activo: null }
        });
    });
});
