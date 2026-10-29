import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EntradaLectorComponent } from './entrada-lector';

describe('EntradaLectorComponent', () => {
    let fixture: ComponentFixture<EntradaLectorComponent>;
    let leidos: string[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [EntradaLectorComponent] }).compileComponents();
        fixture = TestBed.createComponent(EntradaLectorComponent);
        leidos = [];
        fixture.componentInstance.leido.subscribe((texto) => leidos.push(texto));
        document.body.appendChild(fixture.nativeElement);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    afterEach(() => fixture.nativeElement.remove());

    const campo = (): HTMLInputElement => fixture.nativeElement.querySelector('input');
    const escribirYEnter = (texto: string): void => {
        campo().value = texto;
        campo().dispatchEvent(new Event('input'));
        campo().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    };

    it('empieza enfocado', () => {
        expect(document.activeElement).toBe(campo());
    });

    it('con Enter emite lo leído y limpia el campo para el siguiente', () => {
        escribirYEnter(' ASIS:7K2QM9XDR4TB ');

        expect(leidos).toEqual(['ASIS:7K2QM9XDR4TB']);
        expect(campo().value).toBe('');
    });

    it('un Enter vacío no emite', () => {
        escribirYEnter('   ');

        expect(leidos).toEqual([]);
    });

    it('si pierde el foco lo recupera solo', async () => {
        campo().blur();
        await new Promise((resolver) => setTimeout(resolver, 200));

        expect(document.activeElement).toBe(campo());
    });

    it('en pausa se deshabilita', () => {
        fixture.componentRef.setInput('pausado', true);
        fixture.detectChanges();

        expect(campo().disabled).toBeTrue();
    });
});
