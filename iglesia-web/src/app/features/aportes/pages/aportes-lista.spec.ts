import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { ConfiguracionAportes } from '../models/configuracion-aportes';
import { AportesService } from '../services/aportes.service';
import { CategoriasAporteService } from '../services/categorias-aporte.service';
import { ConfiguracionAportesService } from '../services/configuracion-aportes.service';
import { aporte, DIEZMO, OFRENDA, RESUMEN } from '../testing/aportes-prueba';
import { AportesListaComponent } from './aportes-lista';

describe('AportesListaComponent', () => {
    const pagina = { content: [aporte()], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } };
    let servicio: jasmine.SpyObj<AportesService>;
    let configuracion: { configuracion: ReturnType<typeof signal<ConfiguracionAportes>>; asegurarCargada: jasmine.Spy; marcarConAportes: jasmine.Spy };
    let perfil: Perfil;
    let parametros: BehaviorSubject<ParamMap>;
    let fixture: ComponentFixture<AportesListaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [AportesListaComponent],
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { queryParamMap: parametros.asObservable() } },
                { provide: AportesService, useValue: servicio },
                { provide: CategoriasAporteService, useValue: { listar: () => of([DIEZMO, OFRENDA]) } },
                { provide: ConfiguracionAportesService, useValue: configuracion },
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: SesionService, useValue: { perfilActivo: () => perfil, tienePermiso: (permiso: Permiso) => perfil.permisos.includes(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(AportesListaComponent);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;

    beforeEach(() => {
        perfil = perfilBase('ADMIN_IGLESIA');
        parametros = new BehaviorSubject<ParamMap>(convertToParamMap({ desde: '2026-10-01', hasta: '2026-10-31' }));
        servicio = jasmine.createSpyObj<AportesService>('AportesService', ['listar', 'resumen', 'crear', 'actualizar', 'anular']);
        servicio.listar.and.returnValue(of(pagina));
        servicio.resumen.and.returnValue(of(RESUMEN));
        configuracion = { configuracion: signal({ moneda: 'PEN', monedaEditable: true }), asegurarCargada: jasmine.createSpy(), marcarConAportes: jasmine.createSpy() };
    });

    it('carga el resumen y la página con los filtros de la URL', async () => {
        await crear();

        expect(servicio.listar).toHaveBeenCalledWith(jasmine.objectContaining({ desde: '2026-10-01', hasta: '2026-10-31' }), 0, 20);
        expect(servicio.resumen).toHaveBeenCalledWith(jasmine.objectContaining({ desde: '2026-10-01' }));
        expect(html().textContent).toContain('S/\u00a0150.50');
        expect(html().textContent).toContain('Registrar aporte');
        expect(configuracion.asegurarCargada).toHaveBeenCalled();
    });

    it('al cambiar la URL vuelve a cargar desde la primera página', async () => {
        await crear();
        servicio.listar.calls.reset();

        parametros.next(convertToParamMap({ desde: '2026-10-01', hasta: '2026-10-31', medio: 'YAPE' }));
        await fixture.whenStable();

        expect(servicio.listar).toHaveBeenCalledWith(jasmine.objectContaining({ medio: 'YAPE' }), 0, 20);
    });

    it('cambia de página y, al cambiar cuántos se ven, vuelve a la primera', async () => {
        await crear();
        servicio.listar.calls.reset();

        componente()['cambiarPagina']({ pagina: 1, tamano: 20 });
        expect(servicio.listar).toHaveBeenCalledWith(jasmine.anything(), 1, 20);

        componente()['cambiarPagina']({ pagina: 1, tamano: 50 });
        expect(servicio.listar).toHaveBeenCalledWith(jasmine.anything(), 0, 50);
    });

    it('filtrar escribe los filtros en la URL', async () => {
        await crear();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

        componente()['filtrarCategoria'](2);

        expect(navegar).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: jasmine.objectContaining({ categoriaId: 2, desde: '2026-10-01' }) }));
    });

    it('el líder ve «Mis aportes» en solo lectura', async () => {
        perfil = perfilBase('LIDER');
        await crear();

        expect(html().querySelector('h1')?.textContent).toContain('Mis aportes');
        expect(html().textContent).not.toContain('Registrar aporte');
        expect(html().querySelector('#filtro-miembro')).toBeNull();
    });

    it('al guardar y seguir cuenta el aporte, recarga y deja el formulario abierto', async () => {
        servicio.crear.and.returnValue(of(aporte({ id: 50 })));
        await crear();
        componente()['nuevo']();
        servicio.listar.calls.reset();

        componente()['guardar']({ datos: { fecha: '2026-10-25', miembroId: 12, categoriaId: 1, monto: 150.5, medio: 'YAPE', numeroOperacion: null, peticion: null, observacion: null }, seguir: true });
        await fixture.whenStable();

        expect(servicio.listar).toHaveBeenCalled();
        expect(configuracion.marcarConAportes).toHaveBeenCalled();
        expect((fixture.componentInstance as unknown as { registrados: () => number }).registrados()).toBe(1);
        expect((fixture.componentInstance as unknown as { formularioVisible: () => boolean }).formularioVisible()).toBeTrue();
    });

    it('muestra el error del backend en el formulario con el campo señalado', async () => {
        servicio.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Esta categoría exige indicar quién aporta', errores: { miembroId: 'Esta categoría exige indicar quién aporta' } } })));
        await crear();
        componente()['nuevo']();

        componente()['guardar']({ datos: { fecha: '2026-10-25', miembroId: null, categoriaId: 1, monto: 10, medio: 'EFECTIVO', numeroOperacion: null, peticion: null, observacion: null }, seguir: false });
        await fixture.whenStable();

        expect((fixture.componentInstance as unknown as { erroresCampo: () => Record<string, string> }).erroresCampo()).toEqual({ miembroId: 'Esta categoría exige indicar quién aporta' });
    });

    it('anula con el motivo y recarga', async () => {
        servicio.anular.and.returnValue(of(aporte({ anulado: true, motivoAnulacion: 'Duplicado' })));
        await crear();
        componente()['pedirAnulacion'](aporte());

        componente()['anular']('Duplicado');
        await fixture.whenStable();

        expect(servicio.anular).toHaveBeenCalledWith(45, 'Duplicado');
    });

    it('si la página no carga ofrece reintentar', async () => {
        servicio.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await crear();

        expect(html().textContent).toContain('No se pudieron cargar los aportes.');
        expect(html().textContent).toContain('Reintentar');
    });

    it('sin aportes invita a registrar', async () => {
        servicio.listar.and.returnValue(of({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }));
        await crear();

        expect(html().textContent).toContain('Aún no hay aportes en este período');
    });
});
