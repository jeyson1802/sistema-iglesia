import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Asistente } from '../models/asistencia';
import { AsistenciasService } from '../services/asistencias.service';
import { ReunionesService } from '../services/reuniones.service';
import { paginaDe, REUNION_CULTO, REUNION_EVENTO } from '../testing/asistencia-prueba';
import { DetalleReunionComponent } from './detalle-reunion';

describe('DetalleReunionComponent', () => {
    const ANA: Asistente = { miembroId: 12, nombreCompleto: 'Ana Torres', nivel: 'Líder', grupoId: 8, medio: 'QR', registradaEn: '2026-10-25T15:02:00Z' };
    let reuniones: jasmine.SpyObj<ReunionesService>;
    let asistencias: jasmine.SpyObj<AsistenciasService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<DetalleReunionComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [DetalleReunionComponent],
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '50' }) } } },
                { provide: ReunionesService, useValue: reuniones },
                { provide: AsistenciasService, useValue: asistencias },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(DetalleReunionComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        permisos = new Set<Permiso>(['asistencia.ver', 'asistencia.registrar', 'asistencia.configurar']);
        reuniones = jasmine.createSpyObj<ReunionesService>('ReunionesService', ['obtener', 'eliminarEvento']);
        reuniones.obtener.and.returnValue(of(REUNION_CULTO));
        asistencias = jasmine.createSpyObj<AsistenciasService>('AsistenciasService', ['asistentes']);
        asistencias.asistentes.and.returnValue(of(paginaDe([ANA], 128)));
    });

    it('muestra la reunión, su total y los asistentes con paginador', async () => {
        await crear();

        expect(html().querySelector('h1')?.textContent).toContain('Culto dominical');
        expect(html().textContent).toContain('128');
        expect(html().textContent).toContain('Ana Torres');
        expect(html().querySelector('p-paginator')).toBeTruthy();
        expect(html().textContent).toContain('Tomar asistencia');
    });

    it('busca por nombre al dejar de escribir', async () => {
        await crear();
        const pagina = fixture.componentInstance as unknown as { busqueda: { setValue: (texto: string) => void } };
        jasmine.clock().install().mockDate();

        try {
            pagina.busqueda.setValue(' ana ');
            jasmine.clock().tick(300);

            expect(asistencias.asistentes).toHaveBeenCalledWith(50, 'ana', 0, jasmine.any(Number));
        } finally {
            jasmine.clock().uninstall();
        }
    });

    it('solo un evento sin asistentes se puede eliminar', async () => {
        reuniones.obtener.and.returnValue(of(REUNION_EVENTO));
        asistencias.asistentes.and.returnValue(of(paginaDe([])));
        await crear();

        expect(html().textContent).toContain('Eliminar evento');
        expect(html().textContent).toContain('Aún no hay asistentes en esta reunión.');
    });

    it('un culto no ofrece eliminar', async () => {
        await crear();

        expect(html().textContent).not.toContain('Eliminar evento');
    });

    it('quien solo consulta no ve «Tomar asistencia»', async () => {
        permisos = new Set<Permiso>(['asistencia.ver']);
        await crear();

        expect(html().textContent).not.toContain('Tomar asistencia');
    });
});
