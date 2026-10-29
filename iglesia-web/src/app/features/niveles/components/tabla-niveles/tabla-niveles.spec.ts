import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Nivel } from '../../models/nivel';
import { TablaNivelesComponent } from './tabla-niveles';

describe('TablaNivelesComponent', () => {
    const niveles: Nivel[] = [
        { id: 1, nombre: 'Pastor', orden: 1, activo: true },
        { id: 2, nombre: 'Diácono', orden: 2, activo: false }
    ];
    let fixture: ComponentFixture<TablaNivelesComponent>;

    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement => html().querySelector(`button[aria-label="${etiqueta}"]`) as HTMLButtonElement;

    const crear = async (lista: Nivel[], cargando = false): Promise<void> => {
        fixture.componentRef.setInput('niveles', lista);
        fixture.componentRef.setInput('cargando', cargando);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaNivelesComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaNivelesComponent);
    });

    it('muestra los niveles en orden con su estado', async () => {
        await crear(niveles);

        const filas = Array.from(html().querySelectorAll('tbody tr')).map((fila) => fila.textContent);
        expect(filas[0]).toContain('Pastor');
        expect(filas[1]).toContain('Diácono');
        expect(filas[1]).toContain('Inactivo');
    });

    it('no deja subir el primero ni bajar el último', async () => {
        await crear(niveles);

        expect(boton('Subir Pastor').disabled).toBeTrue();
        expect(boton('Bajar Pastor').disabled).toBeFalse();
        expect(boton('Bajar Diácono').disabled).toBeTrue();
    });

    it('emite las acciones de cada fila', async () => {
        await crear(niveles);
        const emitidos: string[] = [];
        fixture.componentInstance.bajar.subscribe((n) => emitidos.push(`bajar ${n.id}`));
        fixture.componentInstance.editar.subscribe((n) => emitidos.push(`editar ${n.id}`));
        fixture.componentInstance.cambiarEstado.subscribe((n) => emitidos.push(`estado ${n.id}`));

        boton('Bajar Pastor').click();
        boton('Renombrar Pastor').click();
        boton('Reactivar Diácono').click();

        expect(emitidos).toEqual(['bajar 1', 'editar 1', 'estado 2']);
    });

    it('mientras se ordena no se puede mover', async () => {
        await crear(niveles);
        fixture.componentRef.setInput('ordenando', true);
        await fixture.whenStable();

        expect(boton('Bajar Pastor').disabled).toBeTrue();
    });

    it('muestra el estado de carga y el vacío', async () => {
        await crear([], true);
        expect(html().querySelector('[aria-label="Cargando niveles"]')).not.toBeNull();

        await crear([]);
        expect(html().textContent).toContain('Aún no hay niveles');
    });
});
