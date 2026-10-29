import { TestBed } from '@angular/core/testing';
import { LayoutService } from '@/app/layout/service/layout.service';
import { CLAVE_MODO_OSCURO, PreferenciaModoOscuroService } from './preferencia-modo-oscuro.service';

describe('PreferenciaModoOscuroService', () => {
    let servicio: PreferenciaModoOscuroService;
    let layout: LayoutService;

    beforeEach(() => {
        localStorage.removeItem(CLAVE_MODO_OSCURO);
        TestBed.configureTestingModule({});
        servicio = TestBed.inject(PreferenciaModoOscuroService);
        layout = TestBed.inject(LayoutService);
    });

    afterEach(() => {
        localStorage.removeItem(CLAVE_MODO_OSCURO);
        document.documentElement.classList.remove('app-dark');
    });

    it('sin preferencia guardada sigue la del sistema operativo', () => {
        spyOn(window, 'matchMedia').and.returnValue({ matches: true } as MediaQueryList);

        servicio.aplicarInicial();

        expect(layout.isDarkTheme()).toBeTrue();
        expect(document.documentElement.classList).toContain('app-dark');
    });

    it('la preferencia guardada manda sobre la del sistema', () => {
        localStorage.setItem(CLAVE_MODO_OSCURO, 'false');
        spyOn(window, 'matchMedia').and.returnValue({ matches: true } as MediaQueryList);

        servicio.aplicarInicial();

        expect(servicio.oscuro()).toBeFalse();
    });

    it('al alternar recuerda la elección en el navegador', () => {
        layout.layoutConfig.update((config) => ({ ...config, darkTheme: false }));

        servicio.alternar();

        expect(servicio.oscuro()).toBeTrue();
        expect(localStorage.getItem(CLAVE_MODO_OSCURO)).toBe('true');
    });
});
