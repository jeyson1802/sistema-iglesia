import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { Aporte } from '../../models/aporte';
import { DIEZMO, OFRENDA, aporte } from '../../testing/aportes-prueba';
import { EnvioAporte, FormularioAporteComponent } from './formulario-aporte';

interface FormularioExpuesto {
    formulario: { controls: Record<string, { setValue: (valor: unknown) => void; value: unknown }>; valid: boolean };
    enviar: (seguir: boolean) => void;
    atajo: (evento: KeyboardEvent) => void;
}

describe('FormularioAporteComponent', () => {
    let fixture: ComponentFixture<FormularioAporteComponent>;
    let envios: EnvioAporte[];

    const abrir = async (editado: Aporte | null = null): Promise<void> => {
        fixture.componentRef.setInput('aporte', editado);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const expuesto = (): FormularioExpuesto => fixture.componentInstance as unknown as FormularioExpuesto;
    const fijar = async (valores: Record<string, unknown>): Promise<void> => {
        Object.entries(valores).forEach(([campo, valor]) => expuesto().formulario.controls[campo].setValue(valor));
        await fixture.whenStable();
    };

    beforeEach(async () => {
        localStorage.removeItem('aportes.ultimaSeleccion');
        await TestBed.configureTestingModule({ imports: [FormularioAporteComponent], providers: [{ provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } }] }).compileComponents();
        fixture = TestBed.createComponent(FormularioAporteComponent);
        fixture.componentRef.setInput('moneda', 'PEN');
        fixture.componentRef.setInput('categorias', [DIEZMO, OFRENDA, { id: 3, nombre: 'Antigua', exigeAportante: false, orden: 3, activo: false }]);
        envios = [];
        fixture.componentInstance.guardar.subscribe((envio) => envios.push(envio));
        await fixture.whenStable();
    });

    afterEach(() => {
        fixture.componentInstance.visible.set(false);
        localStorage.removeItem('aportes.ultimaSeleccion');
    });

    it('al registrar propone hoy, la primera categoría activa y efectivo, sin ofrecer las inactivas', async () => {
        await abrir();

        expect(dialogo().textContent).toContain('Registrar aporte');
        expect(expuesto().formulario.controls['categoriaId'].value).toBe(1);
        expect(expuesto().formulario.controls['medio'].value).toBe('EFECTIVO');
        expect((fixture.componentInstance as unknown as { opcionesCategoria: () => { nombre: string }[] }).opcionesCategoria().map(({ nombre }) => nombre)).toEqual(['Diezmo', 'Ofrenda']);
        expect(document.getElementById('aporte-categoria')).not.toBeNull();
        expect(document.getElementById('aporte-medio')).not.toBeNull();
        expect(dialogo().textContent).toContain('Guardar y registrar otro');
    });

    it('si la categoría exige aportante el campo pasa a obligatorio y lo dice', async () => {
        await abrir();
        await fijar({ monto: 50 });

        expect(dialogo().textContent).toContain('(obligatorio)');
        expect(expuesto().formulario.valid).toBeFalse();

        await fijar({ categoriaId: 2 });

        expect(dialogo().textContent).toContain('Déjalo vacío si es anónimo');
        expect(expuesto().formulario.valid).toBeTrue();
    });

    it('el número de operación solo aparece si el medio no es efectivo y no se envía con efectivo', async () => {
        await abrir();
        expect(document.getElementById('aporte-operacion')).toBeNull();

        await fijar({ medio: 'YAPE' });
        expect(dialogo().textContent).toContain('Código de operación de Yape');

        await fijar({ categoriaId: 2, monto: 20, numeroOperacion: '123', medio: 'EFECTIVO', peticion: '  Por mi familia ' });
        expuesto().enviar(false);

        expect(envios[0].datos).toEqual(jasmine.objectContaining({ categoriaId: 2, monto: 20, medio: 'EFECTIVO', numeroOperacion: null, miembroId: null, peticion: 'Por mi familia' }));
        expect(envios[0].seguir).toBeFalse();
    });

    it('Ctrl+Enter guarda y sigue; al registrar otro conserva fecha, categoría y medio', async () => {
        await abrir();
        await fijar({ categoriaId: 2, monto: 20, medio: 'PLIN', fecha: '2026-10-20' });

        expuesto().atajo(new KeyboardEvent('keydown', { key: 'Enter', ctrlKey: true }));
        expect(envios[0].seguir).toBeTrue();

        fixture.componentRef.setInput('registrados', 1);
        await fixture.whenStable();

        const controles = expuesto().formulario.controls;
        expect(controles['monto'].value).toBeNull();
        expect([controles['categoriaId'].value, controles['medio'].value, controles['fecha'].value]).toEqual([2, 'PLIN', '2026-10-20']);
        expect(dialogo().textContent).toContain('1 aporte registrado');
    });

    it('recuerda la última categoría y medio para el siguiente registro', async () => {
        await abrir();
        await fijar({ categoriaId: 2, monto: 20, medio: 'YAPE' });
        expuesto().enviar(false);
        fixture.componentInstance.visible.set(false);
        await fixture.whenStable();

        await abrir();

        expect([expuesto().formulario.controls['categoriaId'].value, expuesto().formulario.controls['medio'].value]).toEqual([2, 'YAPE']);
    });

    it('al corregir precarga el aporte, ofrece su categoría aunque esté inactiva y muestra la petición', async () => {
        await abrir(aporte({ categoria: { id: 3, nombre: 'Antigua', activo: false } }));

        expect(dialogo().textContent).toContain('Corregir aporte');
        expect(dialogo().textContent).toContain('Antigua');
        expect(dialogo().textContent).not.toContain('Guardar y registrar otro');
        expect(expuesto().formulario.controls['monto'].value).toBe(150.5);
        expect((document.getElementById('aporte-peticion') as HTMLTextAreaElement).value).toBe('Por la salud de mi familia');
    });

    it('un monto de 0 no es válido y explica cómo corregirlo', async () => {
        await abrir();
        await fijar({ categoriaId: 2, monto: 0 });
        expuesto().enviar(false);
        await fixture.whenStable();

        expect(envios).toEqual([]);
        expect(dialogo().textContent).toContain('Escribe un monto mayor a 0');
    });
});
