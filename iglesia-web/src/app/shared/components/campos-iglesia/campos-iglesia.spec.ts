import { Component, inject } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NonNullableFormBuilder } from '@angular/forms';
import { of } from 'rxjs';
import { iglesiaPrueba } from '@/app/core/iglesias/testing/iglesia-prueba';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { CamposIglesiaComponent, crearGrupoIglesia, datosDelGrupo, valoresDeIglesia } from './campos-iglesia';

@Component({
    imports: [CamposIglesiaComponent],
    template: '<app-campos-iglesia [grupo]="grupo" idPrefijo="prueba" [erroresCampo]="errores" />'
})
class AnfitrionComponent {
    readonly grupo = crearGrupoIglesia(inject(NonNullableFormBuilder));
    errores: Record<string, string> = {};
}

describe('CamposIglesiaComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;

    beforeEach(async () => {
        const ubigeo = jasmine.createSpyObj<UbigeoService>('UbigeoService', ['departamentos', 'provincias', 'distritos']);
        ubigeo.departamentos.and.returnValue(of([]));
        ubigeo.provincias.and.returnValue(of([]));
        ubigeo.distritos.and.returnValue(of([]));
        await TestBed.configureTestingModule({ imports: [AnfitrionComponent], providers: [{ provide: UbigeoService, useValue: ubigeo }] }).compileComponents();
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const grupo = () => fixture.componentInstance.grupo;

    it('asocia cada campo a su etiqueta y usa el autocompletado correcto', () => {
        ['prueba-nombre-comercial', 'prueba-denominacion', 'prueba-direccion', 'prueba-telefono', 'prueba-correo', 'prueba-sitio-web'].forEach((id) => {
            expect(html().querySelector(`label[for="${id}"]`))
                .withContext(id)
                .not.toBeNull();
        });
        expect(html().querySelector('#prueba-telefono')?.getAttribute('autocomplete')).toBe('tel');
        expect(html().querySelector('#prueba-direccion')?.getAttribute('autocomplete')).toBe('street-address');
    });

    it('exige nombre comercial de 2 a 60, denominación, dirección y distrito; el contacto es opcional', () => {
        expect(grupo().valid).toBeFalse();

        grupo().patchValue({ nombreComercial: 'x', denominacion: 'Iglesia Sion', direccion: 'Av. 1', distritoCodigo: '150132' });
        expect(grupo().controls.nombreComercial.hasError('minlength')).toBeTrue();

        grupo().controls.nombreComercial.setValue('Sion');
        expect(grupo().valid).toBeTrue();

        grupo().patchValue({ telefono: 'abc', correo: 'no-es-correo', sitioWeb: 'iglesia.org' });
        expect(grupo().controls.telefono.invalid).toBeTrue();
        expect(grupo().controls.correo.invalid).toBeTrue();
        expect(grupo().controls.sitioWeb.invalid).toBeTrue();
    });

    it('cuenta los caracteres del nombre comercial', async () => {
        grupo().controls.nombreComercial.setValue('  Sion  ');
        await fixture.whenStable();

        expect(html().querySelector('#prueba-nombre-comercial-ayuda')?.textContent).toContain('4/60');
    });

    it('muestra el error del backend junto a su campo', async () => {
        fixture.componentInstance.errores = { nombreComercial: 'Ya existe una iglesia con ese nombre comercial' };
        fixture.changeDetectorRef.markForCheck();
        await fixture.whenStable();

        expect(html().textContent).toContain('Ya existe una iglesia con ese nombre comercial');
    });

    it('convierte entre la iglesia y los datos de la API, con lo vacío como null', () => {
        grupo().reset(valoresDeIglesia(iglesiaPrueba({ telefono: null })));
        grupo().patchValue({ nombreComercial: '  Sion Norte ', correo: '  ' });

        expect(datosDelGrupo(grupo())).toEqual({
            nombreComercial: 'Sion Norte',
            denominacion: 'Iglesia Evangélica Sion',
            direccion: 'Av. Los Próceres 123',
            distritoCodigo: '150132',
            telefono: null,
            correo: null,
            sitioWeb: null
        });
    });
});
