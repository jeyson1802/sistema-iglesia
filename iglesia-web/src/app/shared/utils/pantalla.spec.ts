import { CONSULTA_PANTALLA_PEQUENA, esPantallaPequena } from './pantalla';

describe('pantalla', () => {
    it('consulta el ancho de celular', () => {
        const ventana = { matchMedia: jasmine.createSpy('matchMedia').and.returnValue({ matches: true }) } as unknown as Window;
        expect(esPantallaPequena(ventana)).toBeTrue();
        expect(ventana.matchMedia).toHaveBeenCalledWith(CONSULTA_PANTALLA_PEQUENA);
    });

    it('sin ventana asume escritorio', () => {
        expect(esPantallaPequena(null)).toBeFalse();
    });
});
