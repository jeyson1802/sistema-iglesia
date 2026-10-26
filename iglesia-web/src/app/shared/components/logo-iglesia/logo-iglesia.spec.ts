import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogoIglesiaComponent } from './logo-iglesia';

describe('LogoIglesiaComponent', () => {
    let fixture: ComponentFixture<LogoIglesiaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [LogoIglesiaComponent] }).compileComponents();
        fixture = TestBed.createComponent(LogoIglesiaComponent);
    });

    const host = (): HTMLElement => fixture.nativeElement;

    it('muestra el nombre del sistema y oculta el dibujo a los lectores de pantalla', async () => {
        await fixture.whenStable();

        expect(host().textContent).toContain('IGLESIA-WEB');
        expect(host().querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    });

    it('en horizontal no impone tamaños para respetar los estilos de la topbar', async () => {
        await fixture.whenStable();

        expect(host().classList).toContain('contents');
        expect(host().querySelector('svg')?.classList).not.toContain('w-16');
    });

    it('en vertical apila el logo sobre el nombre', async () => {
        fixture.componentRef.setInput('orientacion', 'vertical');
        await fixture.whenStable();

        expect(host().classList).toContain('flex-col');
        expect(host().querySelector('svg')?.classList).toContain('w-16');
        expect(host().querySelector('span')?.classList).toContain('font-semibold');
    });
});
