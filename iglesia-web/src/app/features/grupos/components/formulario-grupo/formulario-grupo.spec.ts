import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { GuardarGrupo } from '../../models/grupo';
import { OLIVOS } from '../../testing/grupos-prueba';
import { FormularioGrupoComponent } from './formulario-grupo';

describe('FormularioGrupoComponent', () => {
    let fixture: ComponentFixture<FormularioGrupoComponent>;
    let guardados: GuardarGrupo[];

    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const guardar = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Guardar') as HTMLButtonElement;
    const formulario = () => fixture.componentInstance['formulario'];
    const abrir = async (grupo = OLIVOS as typeof OLIVOS | null): Promise<void> => {
        fixture.componentRef.setInput('grupo', grupo);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [FormularioGrupoComponent],
            providers: [
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(FormularioGrupoComponent);
        fixture.componentRef.setInput('nombreSingular', 'Célula');
        guardados = [];
        fixture.componentInstance.guardar.subscribe((datos) => guardados.push(datos));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('al crear usa el nombre configurado y exige solo el nombre', async () => {
        await abrir(null);

        expect(dialogo().textContent).toContain('Agregar célula');
        expect(guardar().disabled).toBeTrue();

        formulario().controls.nombre.setValue('Sion');
        await fixture.whenStable();

        expect(guardar().disabled).toBeFalse();
    });

    it('al editar precarga los datos y emite ids y textos limpios', async () => {
        await abrir();
        formulario().controls.direccion.setValue('  ');
        formulario().controls.descripcion.setValue(' Jóvenes ');

        guardar().click();
        await fixture.whenStable();

        expect(guardados).toEqual([{ nombre: 'Los Olivos', liderId: 10, anfitrionId: null, diaReunion: 'JUEVES', horaReunion: '19:30', direccion: null, distritoCodigo: '150135', descripcion: 'Jóvenes' }]);
    });

    it('muestra el error del backend junto al campo', async () => {
        fixture.componentRef.setInput('erroresCampo', { liderId: 'El líder está inactivo' });
        await abrir();

        expect(dialogo().textContent).toContain('El líder está inactivo');
    });
});
