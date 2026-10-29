import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { GuardarMiembro, Miembro } from '../../models/miembro';
import { JUAN } from '../../testing/miembros-prueba';
import { FormularioMiembroComponent } from './formulario-miembro';

describe('FormularioMiembroComponent', () => {
    const niveles = [
        { id: 1, nombre: 'Pastor', activo: true },
        { id: 3, nombre: 'Líder', activo: true },
        { id: 5, nombre: 'Antiguo', activo: false }
    ];
    /** Un grupo no tiene nombre: la ficha lo recibe con el nombre de su líder. */
    const grupos = [{ id: 7, nombre: 'Sara Díaz', activo: true }];
    let fixture: ComponentFixture<FormularioMiembroComponent>;
    let guardados: GuardarMiembro[];

    const html = (): HTMLElement => fixture.nativeElement;
    const campo = (id: string): HTMLInputElement => html().querySelector(`#${id}`) as HTMLInputElement;
    const guardar = (): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Guardar');
    const formulario = () => fixture.componentInstance['formulario'];
    const opcion = <T>(nombre: string): T => (fixture.componentInstance as unknown as Record<string, () => T>)[nombre]();
    const escribir = async (id: string, texto: string): Promise<void> => {
        campo(id).value = texto;
        campo(id).dispatchEvent(new Event('input'));
        campo(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };
    const crear = async (miembro: Miembro | null, soloLectura = false): Promise<void> => {
        fixture.componentRef.setInput('miembro', miembro);
        fixture.componentRef.setInput('soloLectura', soloLectura);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [FormularioMiembroComponent],
            providers: [
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(FormularioMiembroComponent);
        fixture.componentRef.setInput('niveles', niveles);
        fixture.componentRef.setInput('grupos', grupos);
        fixture.componentRef.setInput('nombreGrupo', 'Célula');
        guardados = [];
        fixture.componentInstance.guardar.subscribe((d) => guardados.push(d));
    });

    it('«Guardar» se habilita con solo nombres y apellidos y emite el resto vacío', async () => {
        await crear(null);
        expect(guardar()?.disabled).toBeTrue();

        await escribir('miembro-nombres', ' Rosa ');
        await escribir('miembro-apellidos', 'Flores');
        expect(guardar()?.disabled).toBeFalse();

        guardar()?.click();
        await fixture.whenStable();

        expect(guardados.length).toBe(1);
        expect(guardados[0]).toEqual(jasmine.objectContaining({ nombres: 'Rosa', apellidos: 'Flores', fechaNacimiento: null, edadAproximada: null, telefono: null, liderId: null, nivelId: null }));
    });

    it('sin nombres explica cómo corregirlo', async () => {
        await crear(null);
        await escribir('miembro-nombres', '   ');

        expect(html().textContent).toContain('Escribe los nombres');
    });

    it('la fecha de nacimiento y la edad aproximada son independientes y se envían las dos', async () => {
        await crear(null);
        await escribir('miembro-nombres', 'Rosa');
        await escribir('miembro-apellidos', 'Flores');

        await escribir('miembro-fecha-nacimiento', '1990-05-14');
        formulario().controls.edadAproximada.setValue(30);
        await fixture.whenStable();
        expect(campo('miembro-edad').disabled).toBeFalse();
        expect(campo('miembro-fecha-nacimiento').disabled).toBeFalse();

        guardar()?.click();
        await fixture.whenStable();

        expect(guardados[0]).toEqual(jasmine.objectContaining({ fechaNacimiento: '1990-05-14', edadAproximada: 30 }));
    });

    it('rechaza una fecha de nacimiento futura', async () => {
        await crear(null);

        await escribir('miembro-fecha-nacimiento', '2999-01-01');

        expect(html().textContent).toContain('La fecha de nacimiento no puede ser futura');
    });

    it('al editar precarga la ficha y emite los ids de las referencias', async () => {
        await crear(JUAN);
        expect(campo('miembro-nombres').value).toBe('Juan Carlos');

        guardar()?.click();
        await fixture.whenStable();

        expect(guardados[0]).toEqual(jasmine.objectContaining({ fechaNacimiento: '1990-05-14', edadAproximada: null, nivelId: 3, liderId: 10, grupoId: 4, registradoPorId: 10, distritoCodigo: '150135' }));
    });

    it('precarga la edad aproximada guardada junto con la fecha', async () => {
        await crear({ ...JUAN, edadAproximada: 36 });

        expect(formulario().controls.edadAproximada.value).toBe(36);
        expect(campo('miembro-fecha-nacimiento').value).toBe('1990-05-14');
    });

    it('ofrece los niveles activos y conserva el asignado aunque esté inactivo', async () => {
        await crear({ ...JUAN, nivel: { id: 5, nombre: 'Antiguo', activo: false } });

        expect(opcion<{ etiqueta: string }[]>('opcionesNivel').map(({ etiqueta }) => etiqueta)).toEqual(['Pastor', 'Líder', 'Antiguo (inactivo)']);
        expect(opcion<{ etiqueta: string }[]>('opcionesGrupo').map(({ etiqueta }) => etiqueta)).toEqual(['Célula de Sara Díaz', 'Célula de Ana Ríos (inactivo)']);
    });

    it('el pastor principal no puede tener líder', async () => {
        await crear({ ...JUAN, esPastorPrincipal: true, lider: null });

        expect(formulario().controls.lider.disabled).toBeTrue();
    });

    it('en solo lectura deshabilita todo y no muestra «Guardar»', async () => {
        await crear(JUAN, true);

        expect(campo('miembro-nombres').disabled).toBeTrue();
        expect(guardar()).toBeUndefined();
    });

    it('muestra junto al campo el error del backend', async () => {
        fixture.componentRef.setInput('erroresCampo', { liderId: 'Ese líder forma parte de su red' });
        await crear(JUAN);

        expect(html().textContent).toContain('Ese líder forma parte de su red');
    });

    it('usa el nombre configurado del grupo', async () => {
        await crear(null);

        expect((html().querySelector('label[for="miembro-grupo"]') as HTMLElement).textContent).toContain('Célula');
    });
});
