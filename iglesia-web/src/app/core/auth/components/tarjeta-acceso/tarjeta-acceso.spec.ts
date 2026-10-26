import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TarjetaAccesoComponent, TonoTarjetaAcceso } from './tarjeta-acceso';
import { SIN_CONFIGURADOR_DE_TEMA } from '../../testing/sin-configurador-de-tema';

@Component({
    imports: [TarjetaAccesoComponent],
    template: `<app-tarjeta-acceso [tono]="tono"><p id="contenido">Contenido</p></app-tarjeta-acceso>`
})
class AnfitrionComponent {
    tono: TonoTarjetaAcceso = 'primario';
}

describe('TarjetaAccesoComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [AnfitrionComponent] })
            .overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA)
            .compileComponents();
        fixture = TestBed.createComponent(AnfitrionComponent);
    });

    const borde = (): HTMLElement => fixture.nativeElement.querySelector('main').parentElement;

    it('proyecta el contenido dentro de la región principal', async () => {
        await fixture.whenStable();

        expect(fixture.nativeElement.querySelector('main #contenido')?.textContent).toBe('Contenido');
    });

    it('usa el degradado del color primario por defecto', async () => {
        await fixture.whenStable();

        expect(borde().className).toContain('var(--primary-color)');
    });

    it('usa el degradado naranja en tono de advertencia', async () => {
        fixture.componentInstance.tono = 'advertencia';
        await fixture.whenStable();

        expect(borde().className).toContain('rgba(247,149,48,0.4)');
    });
});
