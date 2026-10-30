import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CasoDetalle } from '../models/consolidacion';
import { CasosService } from '../services/casos.service';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';
import { contactoPrueba, detallePrueba, resultadoPrueba } from '../testing/datos-prueba';
import { CasoConsolidacionComponent } from './caso-consolidacion';

describe('CasoConsolidacionComponent', () => {
    let fixture: ComponentFixture<CasoConsolidacionComponent>;
    let casos: jasmine.SpyObj<CasosService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const crear = async (caso: CasoDetalle): Promise<void> => {
        casos.detalle.and.returnValue(of(caso));
        fixture = TestBed.createComponent(CasoConsolidacionComponent);
        await refrescar();
    };
    const boton = (texto: string): HTMLButtonElement | undefined => Array.from(document.body.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.trim() === texto);

    beforeEach(() => {
        casos = jasmine.createSpyObj<CasosService>('CasosService', ['detalle', 'registrarContacto', 'corregirContacto', 'borrarContacto', 'cerrar', 'reabrir', 'reasignar']);
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '310' }) } } },
                { provide: CasosService, useValue: casos },
                { provide: ResultadosConsolidacionService, useValue: { listar: () => of([resultadoPrueba(1, 'Volverá a asistir')]) } },
                { provide: EquipoConsolidacionService, useValue: { listar: () => of([]) } }
            ]
        });
    });

    afterEach(() => fixture.destroy());

    it('muestra quién es, «Antes de contactar» y los datos para contactar', async () => {
        await crear(detallePrueba());
        expect(casos.detalle).toHaveBeenCalledWith(310);
        expect(html().querySelector('h1')?.textContent).toContain('Ana Quispe');
        expect(html().textContent).toContain('2.ª iteración');
        expect(html().textContent).toContain('Es su 2.ª iteración.');
        expect(html().textContent).toContain('Oración por su familia');
        expect(html().querySelector('a[href^="https://www.google.com/maps"]')).not.toBeNull();
        expect(html().textContent).toContain('Asistió a Culto dominical');
        expect(html().textContent).toContain('No le interesa por ahora');
    });

    it('registra un contacto y recarga el caso', async () => {
        casos.registrarContacto.and.returnValue(of(contactoPrueba(901)));
        await crear(detallePrueba());
        (fixture.componentInstance as unknown as { guardarContacto(datos: unknown): void }).guardarContacto({ fechaHora: '2026-11-03T19:30:00Z', medio: 'VISITA', resultado: 'SE_HABLO', observaciones: null, proximoContacto: null });
        await refrescar();
        expect(casos.registrarContacto).toHaveBeenCalledWith(310, jasmine.objectContaining({ medio: 'VISITA' }));
        expect(casos.detalle).toHaveBeenCalledTimes(2);
    });

    it('un caso cerrado no ofrece registrar y permite reabrir si la ronda sigue en curso', async () => {
        await crear(detallePrueba({ estado: 'CERRADO', resultado: { id: 1, nombre: 'Volverá a asistir', activo: true } }));
        expect(boton('Registrar contacto')).toBeUndefined();
        const menu = (fixture.componentInstance as unknown as { menuMas(): { label?: string }[] }).menuMas();
        expect(menu.map(({ label }) => label)).toEqual(['Reabrir']);
    });

    it('quien no puede actuar solo lee', async () => {
        await crear(detallePrueba({ puedeActuar: false }));
        expect(boton('Registrar contacto')).toBeUndefined();
        expect(boton('Cerrar caso')).toBeUndefined();
    });

    it('cierra el caso con el resultado elegido', async () => {
        casos.cerrar.and.returnValue(of(detallePrueba({ estado: 'CERRADO' })));
        await crear(detallePrueba());
        boton('Cerrar caso')!.click();
        await refrescar();
        (fixture.componentInstance as unknown as { cerrarCaso(cierre: unknown): void }).cerrarCaso({ resultadoId: 1, observacion: null });
        await refrescar();
        expect(casos.cerrar).toHaveBeenCalledWith(310, 1, null);
        expect(html().textContent).toContain('Cerrado');
    });

    it('si el caso no existe o no le corresponde lo explica', async () => {
        casos.detalle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Caso no encontrado' } })));
        fixture = TestBed.createComponent(CasoConsolidacionComponent);
        await refrescar();
        expect(html().textContent).toContain('Caso no encontrado');
    });
});
