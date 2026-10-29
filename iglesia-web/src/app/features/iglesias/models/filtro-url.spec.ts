import { convertToParamMap } from '@angular/router';
import { filtroIglesiasAUrl, filtroIglesiasDesdeUrl, mismoFiltroIglesias } from './filtro-url';

describe('filtro de iglesias en la URL', () => {
    it('lee texto, ubigeo y estado', () => {
        expect(filtroIglesiasDesdeUrl(convertToParamMap({ q: ' sion ', ubigeo: '1501', activa: 'false' }))).toEqual({ q: 'sion', ubigeo: '1501', activa: false });
    });

    it('ignora lo que no entiende', () => {
        expect(filtroIglesiasDesdeUrl(convertToParamMap({ q: '  ', ubigeo: '1', activa: 'quizas' }))).toEqual({ q: null, ubigeo: null, activa: null });
    });

    it('escribe solo lo que filtra y se puede volver a leer igual', () => {
        const filtro = { q: 'betel', ubigeo: null, activa: true };
        const parametros = filtroIglesiasAUrl(filtro);

        expect(parametros).toEqual({ q: 'betel', ubigeo: undefined, activa: 'true' });
        expect(mismoFiltroIglesias(filtroIglesiasDesdeUrl(convertToParamMap({ q: 'betel', activa: 'true' })), filtro)).toBeTrue();
    });
});
