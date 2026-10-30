import { ComponentFixture, TestBed } from '@angular/core/testing';
import { rondaPrueba } from '../../testing/datos-prueba';
import { TarjetaRondaComponent } from './tarjeta-ronda';

describe('TarjetaRondaComponent', () => {
    let fixture: ComponentFixture<TarjetaRondaComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(TarjetaRondaComponent);
        fixture.componentRef.setInput('ronda', rondaPrueba(7, { nombre: 'Noviembre', vencidos: 5, sinAsignar: 3 }));
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra el avance y los avisos accionables', () => {
        const texto = fixture.nativeElement.textContent;
        expect(texto).toContain('Noviembre');
        expect(texto).toContain('5 con contacto vencido');
        expect(texto).toContain('3 sin asignar');
    });

    it('abre la ronda al tocarla', () => {
        const visto = jasmine.createSpy('ver');
        fixture.componentInstance.ver.subscribe(visto);
        (fixture.nativeElement.querySelector('button') as HTMLElement).click();
        expect(visto).toHaveBeenCalledWith(jasmine.objectContaining({ id: 7 }));
    });
});
