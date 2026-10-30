import { ComponentFixture, TestBed } from '@angular/core/testing';
import { consolidadorPrueba } from '../../testing/datos-prueba';
import { TarjetasEquipoComponent } from './tarjetas-equipo';

describe('TarjetasEquipoComponent', () => {
    let fixture: ComponentFixture<TarjetasEquipoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(TarjetasEquipoComponent);
        fixture.componentRef.setInput('equipo', [consolidadorPrueba(25, { nombreCompleto: 'Luis Rojas', casosAbiertos: 6 }), consolidadorPrueba(31, { activo: false })]);
        await refrescar();
    });

    it('muestra cada integrante con sus casos abiertos e iniciales', () => {
        expect(html().textContent).toContain('Luis Rojas');
        expect(html().textContent).toContain('LR');
        expect(html().textContent).toContain('6');
        expect(html().textContent).toContain('Inactivo');
    });

    it('solo quien administra puede activar o desactivar', async () => {
        expect(html().querySelector('p-button')).toBeNull();
        fixture.componentRef.setInput('puedeAdministrar', true);
        await refrescar();
        const cambio = jasmine.createSpy('cambiarEstado');
        fixture.componentInstance.cambiarEstado.subscribe(cambio);
        (html().querySelector('p-button button') as HTMLElement).click();
        expect(cambio).toHaveBeenCalledWith(jasmine.objectContaining({ miembroId: 25 }));
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
