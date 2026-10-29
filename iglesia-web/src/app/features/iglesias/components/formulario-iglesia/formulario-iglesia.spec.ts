import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { AltaIglesia } from '../../models/administrador-iglesia';
import { FormularioIglesiaComponent } from './formulario-iglesia';

describe('FormularioIglesiaComponent', () => {
    let fixture: ComponentFixture<FormularioIglesiaComponent>;
    let altas: AltaIglesia[];

    beforeEach(async () => {
        const ubigeo = jasmine.createSpyObj<UbigeoService>('UbigeoService', ['departamentos', 'provincias', 'distritos']);
        ubigeo.departamentos.and.returnValue(of([]));
        ubigeo.provincias.and.returnValue(of([]));
        ubigeo.distritos.and.returnValue(of([]));
        await TestBed.configureTestingModule({ imports: [FormularioIglesiaComponent], providers: [{ provide: UbigeoService, useValue: ubigeo }] }).compileComponents();
        fixture = TestBed.createComponent(FormularioIglesiaComponent);
        altas = [];
        fixture.componentInstance.crear.subscribe((alta) => altas.push(alta));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    // p-dialog puede renderizar su contenido fuera del host: se busca en todo el documento.
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement | null => document.getElementById(id) as HTMLInputElement | null;
    const guardar = (): HTMLButtonElement => dialogo().querySelector('button[type="submit"]') as HTMLButtonElement;

    const escribir = async (id: string, valor: string): Promise<void> => {
        const input = campo(id)!;
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        input.dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    const llenarAlta = async (): Promise<void> => {
        await escribir('alta-iglesia-nombre-comercial', '  Sion Norte ');
        await escribir('alta-iglesia-denominacion', 'Iglesia Evangélica Sion');
        await escribir('alta-iglesia-direccion', 'Av. Los Próceres 123');
        fixture.componentInstance['formulario'].controls.iglesia.controls.distritoCodigo.setValue('150132');
        await escribir('iglesia-administrador-correo', 'pastor@norte.org');
        await escribir('iglesia-administrador-contrasena', 'Temporal123');
    };

    it('pide la identidad de la iglesia y el primer administrador', () => {
        expect(dialogo().textContent).toContain('Nueva iglesia');
        expect(dialogo().textContent).toContain('Datos de la iglesia');
        expect(dialogo().textContent).toContain('Primer administrador');
        expect(campo('alta-iglesia-nombre-comercial')).not.toBeNull();
        expect(campo('alta-iglesia-denominacion')).not.toBeNull();
        expect(campo('iglesia-administrador-contrasena')!.getAttribute('autocomplete')).toBe('new-password');
    });

    it('«Guardar» se habilita solo con todos los datos obligatorios válidos', async () => {
        expect(guardar().disabled).toBeTrue();

        await llenarAlta();

        expect(guardar().disabled).toBeFalse();
    });

    it('emite el alta con los textos sin espacios sobrantes y el contacto vacío como null', async () => {
        await llenarAlta();

        guardar().click();
        await fixture.whenStable();

        expect(altas).toEqual([
            {
                datos: { nombreComercial: 'Sion Norte', denominacion: 'Iglesia Evangélica Sion', direccion: 'Av. Los Próceres 123', distritoCodigo: '150132', telefono: null, correo: null, sitioWeb: null },
                administrador: { correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' }
            }
        ]);
    });

    it('muestra el error del backend en el diálogo y junto al campo', async () => {
        fixture.componentRef.setInput('error', 'Revisa los datos');
        fixture.componentRef.setInput('erroresCampo', { nombreComercial: 'Ya existe una iglesia con ese nombre comercial' });
        await fixture.whenStable();

        expect(dialogo().textContent).toContain('Revisa los datos');
        expect(dialogo().textContent).toContain('Ya existe una iglesia con ese nombre comercial');
    });

    it('al volver a abrir empieza vacío', async () => {
        await escribir('alta-iglesia-nombre-comercial', 'Sion');
        fixture.componentInstance.visible.set(false);
        await fixture.whenStable();
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();

        expect(campo('alta-iglesia-nombre-comercial')!.value).toBe('');
    });
});
