import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';
import { resultadoPrueba } from '../testing/datos-prueba';
import { ResultadosConsolidacionComponent } from './resultados-consolidacion';

describe('ResultadosConsolidacionComponent', () => {
    const integrado = resultadoPrueba(1, 'Se integró a un grupo');
    const volvera = resultadoPrueba(2, 'Volverá a asistir');
    let servicio: jasmine.SpyObj<ResultadosConsolidacionService>;
    let fixture: ComponentFixture<ResultadosConsolidacionComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const boton = (etiqueta: string): HTMLButtonElement => html().querySelector(`button[aria-label="${etiqueta}"]`) as HTMLButtonElement;

    beforeEach(async () => {
        servicio = jasmine.createSpyObj<ResultadosConsolidacionService>('ResultadosConsolidacionService', ['listar', 'crear', 'renombrar', 'ordenar', 'cambiarEstado']);
        servicio.listar.and.returnValue(of([integrado, volvera]));
        TestBed.configureTestingModule({ providers: [{ provide: ResultadosConsolidacionService, useValue: servicio }] });
        fixture = TestBed.createComponent(ResultadosConsolidacionComponent);
        await refrescar();
    });

    it('muestra los resultados y la vista previa como botones', () => {
        expect(html().querySelectorAll('tbody tr').length).toBe(2);
        expect(html().querySelectorAll('[aria-label="Vista previa de los resultados"] [role="radio"]').length).toBe(2);
    });

    it('envía el nuevo orden al bajar un resultado', async () => {
        servicio.ordenar.and.returnValue(of([volvera, integrado]));
        boton('Bajar Se integró a un grupo').click();
        await refrescar();
        expect(servicio.ordenar).toHaveBeenCalledWith([2, 1]);
    });

    it('desactivar pide confirmación y explica que se conserva el historial', async () => {
        const confirmacion = fixture.debugElement.injector.get(ConfirmationService);
        let pedido: Confirmation | undefined;
        spyOn(confirmacion, 'confirm').and.callFake((opciones: Confirmation) => {
            pedido = opciones;
            return confirmacion;
        });
        servicio.cambiarEstado.and.returnValue(of({ ...integrado, activo: false }));
        boton('Desactivar Se integró a un grupo').click();
        expect(pedido?.message).toContain('conservan en su historial');
        pedido?.accept?.();
        await refrescar();
        expect(servicio.cambiarEstado).toHaveBeenCalledWith(1, false);
    });

    it('si falla la carga ofrece reintentar', async () => {
        servicio.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        fixture = TestBed.createComponent(ResultadosConsolidacionComponent);
        await refrescar();
        expect(html().textContent).toContain('No se pudieron cargar los resultados.');
    });
});
