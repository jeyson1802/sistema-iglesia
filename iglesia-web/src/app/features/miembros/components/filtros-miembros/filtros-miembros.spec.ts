import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { FiltroMiembros } from '../../models/miembro';
import { ESPERA_BUSQUEDA_MS, FiltrosMiembrosComponent } from './filtros-miembros';

describe('FiltrosMiembrosComponent', () => {
    let fixture: ComponentFixture<FiltrosMiembrosComponent>;
    let emitidos: FiltroMiembros[];

    const formulario = () => fixture.componentInstance['formulario'];

    beforeEach(async () => {
        // `debounceTime` mide con `Date.now()`: el reloj simulado debe moverlo también.
        jasmine.clock().install().mockDate();
        await TestBed.configureTestingModule({
            imports: [FiltrosMiembrosComponent],
            providers: [
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: UbigeoService, useValue: { departamentos: () => of([{ codigo: '15', nombre: 'Lima' }]), provincias: () => of([{ codigo: '1501', nombre: 'Lima' }]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(FiltrosMiembrosComponent);
        fixture.componentRef.setInput('niveles', [{ id: 3, nombre: 'Líder' }]);
        emitidos = [];
        fixture.componentInstance.filtrar.subscribe((f) => emitidos.push(f));
        await fixture.whenStable();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('no ofrece filtro de grupo y sí de departamento, provincia y distrito', () => {
        const html: HTMLElement = fixture.nativeElement;
        expect(html.querySelector('#filtro-grupo')).toBeNull();
        expect(html.textContent).toContain('Departamento');
        expect(html.textContent).toContain('Provincia');
        expect(html.textContent).toContain('Distrito');
    });

    it('filtra por el código de la ubicación más precisa elegida', () => {
        formulario().controls.ubigeo.setValue('1501');

        expect(emitidos.at(-1)).toEqual(jasmine.objectContaining({ ubigeo: '1501' }));
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

        expect(emitidos.at(-1)).toEqual({ q: '', condicion: null, nivelId: 3, liderId: 10, ubigeo: null, activo: true });
    });

    it('no emite si el filtro no cambia', () => {
        formulario().controls.activo.setValue(true);

        expect(emitidos).toEqual([]);
    });
});
