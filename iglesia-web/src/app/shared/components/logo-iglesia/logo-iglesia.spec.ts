import { computed, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { LogoIglesiaComponent } from './logo-iglesia';

describe('LogoIglesiaComponent', () => {
    const nombreComercial = signal<string | null>(null);
    const urlLogo = signal<string | null>(null);
    const cargando = signal(false);
    let fixture: ComponentFixture<LogoIglesiaComponent>;

    beforeEach(async () => {
        nombreComercial.set(null);
        urlLogo.set(null);
        cargando.set(false);
        await TestBed.configureTestingModule({
            imports: [LogoIglesiaComponent],
            providers: [{ provide: IdentidadIglesiaService, useValue: { nombreComercial: computed(() => nombreComercial()), urlLogo, cargando } }]
        }).compileComponents();
        fixture = TestBed.createComponent(LogoIglesiaComponent);
    });

    const host = (): HTMLElement => fixture.nativeElement;

    it('sin iglesia muestra la identidad del sistema y oculta el dibujo a los lectores de pantalla', async () => {
        await fixture.whenStable();

        expect(host().textContent).toContain('IGLESIA-WEB');
        expect(host().querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    });

    it('muestra el nombre comercial con la tipografía de marca y el monograma si no hay logo', async () => {
        nombreComercial.set('MMM Lima Centro');
        await fixture.whenStable();

        expect(host().querySelector('.font-marca.truncate')?.textContent).toContain('MMM Lima Centro');
        expect(host().querySelector('.rounded-full')?.textContent?.trim()).toBe('ML');
        expect(host().querySelector('svg')).toBeNull();
    });

    it('muestra el logo de la iglesia cuando lo tiene', async () => {
        nombreComercial.set('Sion');
        urlLogo.set('blob:logo');
        await fixture.whenStable();

        expect(host().querySelector('img')?.getAttribute('src')).toBe('blob:logo');
        expect(host().querySelector('.rounded-full')).toBeNull();
    });

    it('con `neutra` muestra el sistema aunque haya iglesia (pantallas de acceso)', async () => {
        nombreComercial.set('Sion');
        fixture.componentRef.setInput('neutra', true);
        fixture.componentRef.setInput('orientacion', 'vertical');
        await fixture.whenStable();

        expect(host().textContent).toContain('IGLESIA-WEB');
        expect(host().classList).toContain('flex-col');
        expect(host().querySelector('svg')?.classList).toContain('w-16');
    });

    it('la vista previa reemplaza a la iglesia en la que se opera', async () => {
        nombreComercial.set('Sion');
        fixture.componentRef.setInput('vistaPrevia', { nombre: 'Betel Norte', urlLogo: null });
        await fixture.whenStable();

        expect(host().textContent).toContain('Betel Norte');
        expect(host().querySelector('.rounded-full')?.textContent?.trim()).toBe('BN');
    });

    it('muestra un esqueleto mientras carga la identidad', async () => {
        cargando.set(true);
        await fixture.whenStable();

        expect(host().querySelector('p-skeleton')).not.toBeNull();
        expect(host().textContent).not.toContain('IGLESIA-WEB');
    });
});
