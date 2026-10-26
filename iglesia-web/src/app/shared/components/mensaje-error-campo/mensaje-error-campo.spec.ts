import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { MensajeErrorCampoComponent } from './mensaje-error-campo';

describe('MensajeErrorCampoComponent', () => {
    let fixture: ComponentFixture<MensajeErrorCampoComponent>;
    let control: FormControl<string>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [MensajeErrorCampoComponent] }).compileComponents();
        control = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(10)] });
        fixture = TestBed.createComponent(MensajeErrorCampoComponent);
        fixture.componentRef.setInput('control', control);
        fixture.componentRef.setInput('mensajes', { required: 'Campo obligatorio', maxlength: 'Demasiado largo', email: 'Correo inválido' });
        await fixture.whenStable();
    });

    const texto = (): string => fixture.nativeElement.textContent.trim();

    it('no muestra nada mientras el campo no fue tocado', () => {
        expect(texto()).toBe('');
    });

    it('al tocar el campo muestra el mensaje del error', async () => {
        control.markAsTouched();
        await fixture.whenStable();

        expect(texto()).toBe('Campo obligatorio');
    });

    it('muestra el error de mayor prioridad según el orden de los mensajes', async () => {
        control.markAsTouched();
        control.setValue('no-es-un-correo');
        await fixture.whenStable();

        expect(texto()).toBe('Demasiado largo');
    });

    it('desaparece cuando el valor es válido', async () => {
        control.markAsTouched();
        control.setValue('a@b.co');
        await fixture.whenStable();

        expect(texto()).toBe('');
    });
});
