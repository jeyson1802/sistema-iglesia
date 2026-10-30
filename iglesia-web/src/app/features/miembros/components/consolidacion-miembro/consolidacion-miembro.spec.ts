import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConsolidacionMiembro } from '@/app/features/consolidacion/models/consolidacion';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';
import { detallePrueba, SIN_CONTACTOS } from '@/app/features/consolidacion/testing/datos-prueba';
import { ConsolidacionMiembroComponent } from './consolidacion-miembro';

describe('ConsolidacionMiembroComponent', () => {
    let fixture: ComponentFixture<ConsolidacionMiembroComponent>;
    let casos: jasmine.SpyObj<CasosService>;
    let permisos: string[];
    const html = (): HTMLElement => fixture.nativeElement;
    const RESUMEN: ConsolidacionMiembro = {
        iteraciones: 3,
        contactos: { ...SIN_CONTACTOS, total: 6, llamadas: 5, visitas: 1 },
        ultimoResultado: { id: 2, nombre: 'Volverá a asistir', activo: true },
        ultimoResultadoEn: '2026-08-20T15:00:00Z',
        casoAbierto: null,
        fechasContactoRecientes: [],
        puedeIniciar: true
    };
    const crear = async (resumen = RESUMEN): Promise<void> => {
        casos.resumenDeMiembro.and.returnValue(of(resumen));
        fixture = TestBed.createComponent(ConsolidacionMiembroComponent);
        fixture.componentRef.setInput('miembroId', 40);
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(() => {
        permisos = ['consolidacion.ver'];
        casos = jasmine.createSpyObj<CasosService>('CasosService', ['resumenDeMiembro', 'abrir']);
        TestBed.configureTestingModule({
            providers: [provideRouter([]), { provide: CasosService, useValue: casos }, { provide: SesionService, useValue: { tienePermiso: (permiso: string) => permisos.includes(permiso) } }]
        });
    });

    it('resume las iteraciones, los contactos y el último resultado', async () => {
        await crear();
        expect(casos.resumenDeMiembro).toHaveBeenCalledWith(40);
        expect(html().textContent).toContain('3 iteraciones · 5 llamadas · 1 visita');
        expect(html().textContent).toContain('Volverá a asistir');
        expect(html().textContent).toContain('Ver historial');
    });

    it('sin seguimientos lo dice y ofrece iniciar uno', async () => {
        casos.abrir.and.returnValue(of(detallePrueba({ id: 500 })));
        await crear({ ...RESUMEN, iteraciones: 0, contactos: SIN_CONTACTOS, ultimoResultado: null, ultimoResultadoEn: null });
        expect(html().textContent).toContain('Aún no ha tenido seguimiento de consolidación.');
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        (Array.from(html().querySelectorAll('button')).find((b) => b.textContent!.includes('Iniciar seguimiento')) as HTMLElement).click();
        expect(casos.abrir).toHaveBeenCalledWith(40);
        expect(navegar).toHaveBeenCalledWith(['/consolidacion', 'casos', 500]);
    });

    it('con un caso abierto muestra quién lo sigue y el acceso al caso', async () => {
        await crear({ ...RESUMEN, casoAbierto: { casoId: 310, ronda: { id: 7, nombre: 'Noviembre', activo: true }, consolidador: { id: 25, nombre: 'Luis Rojas', activo: true }, estado: 'EN_CONTACTO' } });
        expect(html().textContent).toContain('Luis Rojas · Noviembre');
        expect(html().textContent).not.toContain('Iniciar seguimiento');
    });

    it('sin permisos de consolidación o fuera de su ámbito no se muestra', async () => {
        permisos = [];
        await crear();
        expect(casos.resumenDeMiembro).not.toHaveBeenCalled();
        expect(html().textContent!.trim()).toBe('');
        permisos = ['consolidacion.contactar'];
        casos.resumenDeMiembro.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
        fixture = TestBed.createComponent(ConsolidacionMiembroComponent);
        fixture.componentRef.setInput('miembroId', 40);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(html().textContent!.trim()).toBe('');
    });

    it('si falla por otra razón ofrece reintentar', async () => {
        casos.resumenDeMiembro.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
        fixture = TestBed.createComponent(ConsolidacionMiembroComponent);
        fixture.componentRef.setInput('miembroId', 40);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(html().textContent).toContain('No se pudo cargar la consolidación.');
    });
});
