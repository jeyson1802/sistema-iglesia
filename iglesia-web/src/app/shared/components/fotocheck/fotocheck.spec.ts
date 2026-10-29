import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { DatosFotocheck, FotocheckComponent } from './fotocheck';

describe('FotocheckComponent', () => {
    const ANA: DatosFotocheck = { nombres: 'Ana María', apellidos: 'Torres Ruiz', nivel: 'Líder', codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' };
    const urlLogo = signal<string | null>(null);
    let fixture: ComponentFixture<FotocheckComponent>;

    beforeEach(async () => {
        urlLogo.set(null);
        await TestBed.configureTestingModule({
            imports: [FotocheckComponent],
            providers: [{ provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Vida Nueva'), urlLogo } }]
        }).compileComponents();
        fixture = TestBed.createComponent(FotocheckComponent);
        fixture.componentRef.setInput('datos', ANA);
        fixture.detectChanges();
    });

    const texto = (): string => fixture.nativeElement.textContent;

    it('muestra la iglesia, el miembro, su nivel y el código en grupos de cuatro', () => {
        expect(texto()).toContain('Iglesia Vida Nueva');
        expect(texto()).toContain('Ana María');
        expect(texto()).toContain('Torres Ruiz');
        expect(texto()).toContain('Líder');
        expect(texto()).toContain('7K2Q M9XD R4TB');
        expect(fixture.nativeElement.querySelector('article').getAttribute('aria-label')).toBe('Fotocheck de Ana María Torres Ruiz');
    });

    it('dibuja el QR en el navegador', async () => {
        await fixture.whenStable();
        await new Promise((resolver) => setTimeout(resolver, 50));
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('img[alt="Código QR de asistencia"]').src).toMatch(/^data:image\/png/);
    });

    it('sin logo muestra el monograma de la iglesia', () => {
        expect(texto()).toContain('IV');
    });
});
