import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ConfiguracionAportes } from '../models/configuracion-aportes';
import { CategoriasAporteService } from '../services/categorias-aporte.service';
import { ConfiguracionAportesService } from '../services/configuracion-aportes.service';
import { DIEZMO, OFRENDA } from '../testing/aportes-prueba';
import { ConfiguracionAportesComponent } from './configuracion-aportes';

describe('ConfiguracionAportesComponent', () => {
    let categorias: jasmine.SpyObj<CategoriasAporteService>;
    let configuracion: jasmine.SpyObj<ConfiguracionAportesService> & { configuracion: ReturnType<typeof signal<ConfiguracionAportes>> };
    let fixture: ComponentFixture<ConfiguracionAportesComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [ConfiguracionAportesComponent],
            providers: [
                { provide: CategoriasAporteService, useValue: categorias },
                { provide: ConfiguracionAportesService, useValue: configuracion }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(ConfiguracionAportesComponent);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;

    beforeEach(() => {
        categorias = jasmine.createSpyObj<CategoriasAporteService>('CategoriasAporteService', ['listar', 'crear', 'actualizar', 'ordenar', 'cambiarEstado']);
        categorias.listar.and.returnValue(of([DIEZMO, OFRENDA]));
        configuracion = Object.assign(jasmine.createSpyObj<ConfiguracionAportesService>('ConfiguracionAportesService', ['recargar', 'monedas', 'guardar']), {
            configuracion: signal<ConfiguracionAportes>({ moneda: 'PEN', monedaEditable: true })
        });
        configuracion.recargar.and.returnValue(of({ moneda: 'PEN', monedaEditable: true }));
        configuracion.monedas.and.returnValue(of([{ codigo: 'PEN', nombre: 'sol peruano', simbolo: 'S/' }]));
    });

    it('muestra la moneda y las categorías', async () => {
        await crear();

        expect(html().textContent).toContain('Moneda de la iglesia');
        expect(html().querySelectorAll('tbody tr').length).toBe(2);
    });

    it('guarda la moneda y avisa; si el backend la rechaza muestra su motivo', async () => {
        configuracion.guardar.and.returnValue(of({ moneda: 'USD', monedaEditable: true }));
        await crear();
        componente()['guardarMoneda']('USD');
        await fixture.whenStable();
        expect(html().textContent).toContain('Moneda guardada.');

        configuracion.guardar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Ya hay aportes registrados en PEN' } })));
        componente()['guardarMoneda']('EUR');
        await fixture.whenStable();
        expect(html().textContent).toContain('Ya hay aportes registrados en PEN');
    });

    it('crea una categoría y la agrega al final', async () => {
        categorias.crear.and.returnValue(of({ id: 3, nombre: 'Misiones', exigeAportante: false, orden: 3, activo: true }));
        await crear();
        componente()['nueva']();
        componente()['guardar']({ nombre: 'Misiones', exigeAportante: false });
        await fixture.whenStable();

        expect(categorias.crear).toHaveBeenCalledWith({ nombre: 'Misiones', exigeAportante: false });
        expect(html().querySelectorAll('tbody tr').length).toBe(3);
    });

    it('bajar envía el orden completo', async () => {
        categorias.ordenar.and.returnValue(of([OFRENDA, DIEZMO]));
        await crear();
        componente()['bajar'](DIEZMO);

        expect(categorias.ordenar).toHaveBeenCalledWith([2, 1]);
    });

    it('si las categorías no cargan ofrece reintentar', async () => {
        categorias.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await crear();

        expect(html().textContent).toContain('No se pudo cargar la configuración de aportes.');
        expect(html().textContent).toContain('Reintentar');
    });
});
