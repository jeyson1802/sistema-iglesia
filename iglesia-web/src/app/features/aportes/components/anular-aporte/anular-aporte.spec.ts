import { ComponentFixture, TestBed } from '@angular/core/testing';
import { aporte } from '../../testing/aportes-prueba';
import { AnularAporteComponent } from './anular-aporte';

describe('AnularAporteComponent', () => {
    let fixture: ComponentFixture<AnularAporteComponent>;
    let motivos: string[];

    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (): HTMLTextAreaElement => document.getElementById('anular-motivo') as HTMLTextAreaElement;
    const anular = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.includes('Anular aporte')) as HTMLButtonElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [AnularAporteComponent] }).compileComponents();
        fixture = TestBed.createComponent(AnularAporteComponent);
        fixture.componentRef.setInput('moneda', 'PEN');
        fixture.componentRef.setInput('aporte', aporte());
        motivos = [];
        fixture.componentInstance.anular.subscribe((motivo) => motivos.push(motivo));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('muestra qué se anula y exige el motivo', async () => {
        expect(dialogo().textContent).toContain('S/\u00a0150.50');
        expect(dialogo().textContent).toContain('Pérez Soto, Juan');
        expect(anular().disabled).toBeTrue();

        campo().value = '  Registrado dos veces ';
        campo().dispatchEvent(new Event('input'));
        await fixture.whenStable();
        anular().click();
        await fixture.whenStable();

        expect(motivos).toEqual(['Registrado dos veces']);
    });
});
