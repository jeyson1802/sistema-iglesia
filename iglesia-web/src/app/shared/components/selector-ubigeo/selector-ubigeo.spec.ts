import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { SelectorUbigeoComponent } from './selector-ubigeo';
import { UbigeoService } from './ubigeo.service';

@Component({
    imports: [ReactiveFormsModule, SelectorUbigeoComponent],
    template: '<app-selector-ubigeo idBase="prueba" [formControl]="control" />'
})
class AnfitrionComponent {
    readonly control = new FormControl<string | null>(null);
}

describe('SelectorUbigeoComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    let ubigeo: jasmine.SpyObj<UbigeoService>;

    const selector = (): SelectorUbigeoComponent => fixture.debugElement.children[0].componentInstance;
    const estado = () => {
        const s = selector() as unknown as Record<string, () => unknown>;
        return { departamento: s['departamento'](), provincia: s['provincia'](), distrito: s['distrito'](), provincias: s['provincias'](), distritos: s['distritos']() };
    };

    beforeEach(async () => {
        ubigeo = jasmine.createSpyObj<UbigeoService>('UbigeoService', ['departamentos', 'provincias', 'distritos']);
        ubigeo.departamentos.and.returnValue(of([{ codigo: '15', nombre: 'Lima' }]));
        ubigeo.provincias.and.returnValue(of([{ codigo: '1501', nombre: 'Lima' }]));
        ubigeo.distritos.and.returnValue(of([{ codigo: '150135', nombre: 'San Martín de Porres' }]));
        await TestBed.configureTestingModule({ imports: [AnfitrionComponent], providers: [{ provide: UbigeoService, useValue: ubigeo }] }).compileComponents();
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    it('asocia cada campo a su etiqueta', () => {
        const etiquetas = Array.from(fixture.nativeElement.querySelectorAll('label') as NodeListOf<HTMLLabelElement>).map((label) => label.htmlFor);

        expect(etiquetas).toEqual(['prueba-departamento', 'prueba-provincia', 'prueba-distrito']);
    });

    it('al recibir un distrito deduce provincia y departamento y carga sus listas', async () => {
        fixture.componentInstance.control.setValue('150135');
        await fixture.whenStable();

        expect(ubigeo.provincias).toHaveBeenCalledWith('15');
        expect(ubigeo.distritos).toHaveBeenCalledWith('1501');
        expect(estado()).toEqual(jasmine.objectContaining({ departamento: '15', provincia: '1501', distrito: '150135' }));
    });

    it('elegir el distrito pone su código como valor del control', () => {
        selector()['elegirDepartamento']('15');
        selector()['elegirProvincia']('1501');
        selector()['elegirDistrito']('150135');

        expect(fixture.componentInstance.control.value).toBe('150135');
    });

    it('cambiar el departamento limpia la provincia, el distrito y el valor', async () => {
        fixture.componentInstance.control.setValue('150135');
        await fixture.whenStable();

        selector()['elegirDepartamento']('08');

        expect(estado()).toEqual(jasmine.objectContaining({ departamento: '08', provincia: null, distrito: null, distritos: [] }));
        expect(fixture.componentInstance.control.value).toBeNull();
    });

    it('cambiar la provincia limpia el distrito', async () => {
        fixture.componentInstance.control.setValue('150135');
        await fixture.whenStable();

        selector()['elegirProvincia']('1502');

        expect(estado()).toEqual(jasmine.objectContaining({ departamento: '15', provincia: '1502', distrito: null }));
        expect(fixture.componentInstance.control.value).toBeNull();
    });

    it('un valor vacío limpia los tres campos', async () => {
        fixture.componentInstance.control.setValue('150135');
        await fixture.whenStable();

        fixture.componentInstance.control.setValue(null);

        expect(estado()).toEqual(jasmine.objectContaining({ departamento: null, provincia: null, distrito: null, provincias: [] }));
    });
});
