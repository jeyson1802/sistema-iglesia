import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { FiltroMiembros } from '../../models/miembro';
import { ESPERA_BUSQUEDA_MS, FiltrosMiembrosComponent } from './filtros-miembros';

describe('FiltrosMiembrosComponent', () => {
    let fixture: ComponentFixture<FiltrosMiembrosComponent>;
    let emitidos: FiltroMiembros[];

    const formulario = () => fixture.componentInstance['formulario'];

    beforeEach(async () => {
        // `debounceTime` mide con `Date.now()`: el reloj simulado debe moverlo también.
        jasmine.clock().install().mockDate();
        await TestBed.configureTestingModule({ imports: [FiltrosMiembrosComponent], providers: [{ provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } }] }).compileComponents();
        fixture = TestBed.createComponent(FiltrosMiembrosComponent);
        fixture.componentRef.setInput('niveles', [{ id: 3, nombre: 'Líder' }]);
        fixture.componentRef.setInput('grupos', [{ id: 4, nombre: 'Los Olivos' }]);
        fixture.componentRef.setInput('nombreGrupo', 'Célula');
        emitidos = [];
        fixture.componentInstance.filtrar.subscribe((f) => emitidos.push(f));
        await fixture.whenStable();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('etiqueta el filtro de grupo con el nombre configurado', () => {
        expect((fixture.nativeElement.querySelector('label[for="filtro-grupo"]') as HTMLElement).textContent).toContain('Célula');
    });

    it('busca por nombre al dejar de escribir', () => {
        formulario().controls.q.setValue(' ju');
        formulario().controls.q.setValue(' juan ');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS - 1);
        expect(emitidos).toEqual([]);

        jasmine.clock().tick(1);

        expect(emitidos.length).toBe(1);
        expect(emitidos[0]).toEqual(jasmine.objectContaining({ q: 'juan', activo: true }));
    });

    it('emite al cambiar un selector, con el id del líder elegido', () => {
        formulario().controls.nivelId.setValue(3);
        formulario().controls.lider.setValue({ id: 10, nombre: 'Ana Ríos' });

        expect(emitidos.at(-1)).toEqual({ q: '', condicion: null, nivelId: 3, grupoId: null, liderId: 10, activo: true });
    });

    it('no emite si el filtro no cambia', () => {
        formulario().controls.activo.setValue(true);

        expect(emitidos).toEqual([]);
    });
});
