import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { iglesiaPrueba } from '@/app/core/iglesias/testing/iglesia-prueba';
import { AppFooter } from './app.footer';

describe('AppFooter', () => {
    const identidad = signal<Iglesia | null>(null);
    let fixture: ComponentFixture<AppFooter>;

    beforeEach(async () => {
        identidad.set(null);
        await TestBed.configureTestingModule({
            imports: [AppFooter],
            providers: [{ provide: IdentidadIglesiaService, useValue: { identidad } }]
        }).compileComponents();
        fixture = TestBed.createComponent(AppFooter);
    });

    const texto = (): string => (fixture.nativeElement as HTMLElement).textContent ?? '';
    const enlace = (prefijo: string): HTMLAnchorElement | null => (fixture.nativeElement as HTMLElement).querySelector(`a[href^="${prefijo}"]`);

    it('sin iglesia muestra el nombre del sistema y el año', async () => {
        await fixture.whenStable();

        expect(texto()).toContain(`© ${new Date().getFullYear()} IGLESIA-WEB`);
        expect(texto()).not.toContain('PrimeNG');
    });

    it('muestra los datos de la iglesia con su dirección completa y su contacto como enlaces', async () => {
        identidad.set(iglesiaPrueba({ telefono: '+51 (1) 234-5678', correo: 'hola@sion.org', sitioWeb: 'https://sion.org' }));
        await fixture.whenStable();

        expect(texto()).toContain('Iglesia Evangélica Sion');
        expect(texto()).toContain('Av. Los Próceres 123, San Juan de Lurigancho, Lima, Lima');
        expect(enlace('tel:')?.getAttribute('href')).toBe('tel:+5112345678');
        expect(enlace('mailto:')?.textContent).toContain('hola@sion.org');
        expect(enlace('https://sion.org')?.getAttribute('target')).toBe('_blank');
        expect(texto()).toContain(`© ${new Date().getFullYear()} Sion`);
    });

    it('no muestra lo que falta', async () => {
        identidad.set(iglesiaPrueba({ direccion: null, distritoCodigo: null, ubicacion: null }));
        await fixture.whenStable();

        expect(enlace('tel:')).toBeNull();
        expect(enlace('mailto:')).toBeNull();
        expect((fixture.nativeElement as HTMLElement).querySelector('.pi-map-marker')).toBeNull();
    });
});
