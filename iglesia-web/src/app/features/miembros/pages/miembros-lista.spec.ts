import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { NivelesService } from '@/app/features/niveles/services/niveles.service';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { FILTRO_MIEMBROS_INICIAL } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';
import { RESUMEN_JUAN } from '../testing/miembros-prueba';
import { MiembrosListaComponent } from './miembros-lista';

describe('MiembrosListaComponent', () => {
    const pagina = { content: [RESUMEN_JUAN], page: { size: 10, number: 0, totalElements: 1, totalPages: 1 } };
    let servicio: jasmine.SpyObj<MiembrosService>;
    let niveles: jasmine.SpyObj<NivelesService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<MiembrosListaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [MiembrosListaComponent],
            providers: [
                provideRouter([]),
                { provide: MiembrosService, useValue: servicio },
                { provide: NivelesService, useValue: niveles },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } },
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(MiembrosListaComponent);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const senal = <T>(nombre: string): T => (fixture.componentInstance as unknown as Record<string, () => T>)[nombre]();

    beforeEach(() => {
        permisos = new Set<Permiso>(['miembros.ver', 'miembros.crear']);
        servicio = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['listar']);
        servicio.listar.and.returnValue(of(pagina));
        niveles = jasmine.createSpyObj<NivelesService>('NivelesService', ['listar']);
        niveles.listar.and.returnValue(
            of([
                { id: 3, nombre: 'Líder', orden: 1, activo: true },
                { id: 4, nombre: 'Viejo', orden: 2, activo: false }
            ])
        );
    });

    it('carga la primera página con solo los activos y la muestra', async () => {
        await crear();

        expect(servicio.listar).toHaveBeenCalledWith(FILTRO_MIEMBROS_INICIAL, 0, 10);
        expect(html().textContent).toContain('Pérez Torres, Juan Carlos');
    });

    it('ofrece en los filtros solo los niveles activos', async () => {
        await crear();

        expect(senal<{ id: number }[]>('niveles').map(({ id }) => id)).toEqual([3]);
    });

    it('si los niveles no cargan la tabla sigue funcionando', async () => {
        niveles.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await crear();

        expect(senal<unknown[]>('niveles')).toEqual([]);
        expect(html().textContent).toContain('Pérez Torres, Juan Carlos');
    });

    it('«Nuevo miembro» solo con permiso de crear', async () => {
        permisos = new Set<Permiso>(['miembros.ver']);
        await crear();

        expect(html().textContent).not.toContain('Nuevo miembro');
    });

    it('al filtrar vuelve a la primera página', async () => {
        await crear();
        const componente = fixture.componentInstance as unknown as Record<string, (...a: unknown[]) => void>;
        componente['cambiarPagina']({ pagina: 3, tamano: 10 });

        componente['filtrar']({ ...FILTRO_MIEMBROS_INICIAL, q: 'ana' });

        expect(servicio.listar).toHaveBeenCalledWith({ ...FILTRO_MIEMBROS_INICIAL, q: 'ana' }, 0, 10);
    });

    it('al cambiar cuántos se ven por página vuelve a la primera con el nuevo tamaño', async () => {
        await crear();
        const componente = fixture.componentInstance as unknown as Record<string, (...a: unknown[]) => void>;

        componente['cambiarPagina']({ pagina: 2, tamano: 50 });

        expect(servicio.listar).toHaveBeenCalledWith(FILTRO_MIEMBROS_INICIAL, 0, 50);
    });

    it('muestra el error de carga con reintento', async () => {
        servicio.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await crear();

        expect(html().textContent).toContain('No se pudieron cargar los miembros.');
        expect(html().textContent).toContain('Reintentar');
    });
});
