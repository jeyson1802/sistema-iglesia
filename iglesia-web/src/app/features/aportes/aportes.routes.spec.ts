import rutas from './aportes.routes';

describe('rutas de aportes', () => {
    it('el listado exige ver aportes y la configuración, configurarlos', () => {
        expect(rutas.map(({ path, title }) => [path, title])).toEqual([
            ['', 'Aportes'],
            ['configuracion', 'Configuración de aportes']
        ]);
        expect(rutas.every(({ canActivate }) => canActivate?.length === 1)).toBeTrue();
    });
});
