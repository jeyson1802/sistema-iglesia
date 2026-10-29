import { ComponentFixture, TestBed } from '@angular/core/testing';
import { aporte } from '../../testing/aportes-prueba';
import { DetalleAporteComponent } from './detalle-aporte';

describe('DetalleAporteComponent', () => {
    let fixture: ComponentFixture<DetalleAporteComponent>;

    const abrir = async (permisos = true, cambios = {}): Promise<void> => {
        fixture.componentRef.setInput('aporte', aporte(cambios));
        fixture.componentRef.setInput('puedeEditar', permisos);
        fixture.componentRef.setInput('puedeAnular', permisos);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };
    const panel = (): HTMLElement => document.querySelector('.p-drawer') as HTMLElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [DetalleAporteComponent] }).compileComponents();
        fixture = TestBed.createComponent(DetalleAporteComponent);
        fixture.componentRef.setInput('moneda', 'PEN');
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('muestra el monto, la petición destacada y la auditoría', async () => {
        await abrir();

        expect(panel().textContent).toContain('S/\u00a0150.50');
        expect(panel().textContent).toContain('Por la salud de mi familia');
        expect(panel().textContent).toContain('00123456');
        expect(panel().textContent).toContain('Registrado el');
        expect(panel().textContent).toContain('Editar');
    });

    it('un anulado muestra el motivo y no ofrece acciones', async () => {
        await abrir(true, { anulado: true, motivoAnulacion: 'Duplicado' });

        expect(panel().textContent).toContain('Anulado: Duplicado');
        expect(panel().textContent).not.toContain('Editar');
    });

    it('sin permisos es solo lectura', async () => {
        await abrir(false);

        expect(panel().querySelectorAll('button').length).toBeLessThanOrEqual(1);
    });
});
