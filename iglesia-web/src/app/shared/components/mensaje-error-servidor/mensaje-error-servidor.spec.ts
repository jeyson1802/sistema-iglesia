import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MensajeErrorServidorComponent } from './mensaje-error-servidor';

describe('MensajeErrorServidorComponent', () => {
    let fixture: ComponentFixture<MensajeErrorServidorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [MensajeErrorServidorComponent] }).compileComponents();
        fixture = TestBed.createComponent(MensajeErrorServidorComponent);
    });

    it('muestra el mensaje recibido', async () => {
        fixture.componentRef.setInput('mensaje', 'El líder está inactivo');
        await fixture.whenStable();

        expect(fixture.nativeElement.textContent).toContain('El líder está inactivo');
    });

    it('sin mensaje no muestra nada', async () => {
        await fixture.whenStable();

        expect(fixture.nativeElement.querySelector('p-message')).toBeNull();
    });
});
