import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { OpcionesMiembroService, OpcionMiembro } from './opciones-miembro.service';
import { SelectorMiembroComponent } from './selector-miembro';

@Component({
    imports: [ReactiveFormsModule, SelectorMiembroComponent],
    template: '<app-selector-miembro inputId="lider" [excluirId]="5" [formControl]="control" />'
})
class AnfitrionComponent {
    readonly control = new FormControl<OpcionMiembro | null>(null);
}

describe('SelectorMiembroComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    let opciones: jasmine.SpyObj<OpcionesMiembroService>;

    const selector = (): SelectorMiembroComponent => fixture.debugElement.children[0].componentInstance;

    beforeEach(async () => {
        opciones = jasmine.createSpyObj<OpcionesMiembroService>('OpcionesMiembroService', ['buscar']);
        opciones.buscar.and.returnValue(
            of([
                { id: 5, nombre: 'Yo Mismo' },
                { id: 7, nombre: 'Ana Ríos' }
            ])
        );
        await TestBed.configureTestingModule({ imports: [AnfitrionComponent], providers: [{ provide: OpcionesMiembroService, useValue: opciones }] }).compileComponents();
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    it('usa el id recibido en el campo', () => {
        expect(fixture.nativeElement.querySelector('input#lider')).not.toBeNull();
    });

    it('no sugiere al miembro excluido', () => {
        selector()['buscar']({ query: 'a', originalEvent: new Event('input') });

        expect(opciones.buscar).toHaveBeenCalledWith('a');
        expect(selector()['sugerencias']()).toEqual([{ id: 7, nombre: 'Ana Ríos' }]);
    });

    it('al elegir una opción la pone como valor y mientras se escribe el valor es nulo', () => {
        selector()['cambiar']({ id: 7, nombre: 'Ana Ríos' });
        expect(fixture.componentInstance.control.value).toEqual({ id: 7, nombre: 'Ana Ríos' });

        selector()['cambiar']('An');
        expect(fixture.componentInstance.control.value).toBeNull();
    });

    it('muestra la opción que llega desde el formulario', async () => {
        fixture.componentInstance.control.setValue({ id: 7, nombre: 'Ana Ríos' });
        await fixture.whenStable();

        expect((fixture.nativeElement.querySelector('input#lider') as HTMLInputElement).value).toBe('Ana Ríos');
    });

    it('se deshabilita con el control', async () => {
        fixture.componentInstance.control.disable();
        await fixture.whenStable();

        expect((fixture.nativeElement.querySelector('input#lider') as HTMLInputElement).disabled).toBeTrue();
    });
});
