import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Page } from '@/app/core/models/page';
import { Ronda } from '../models/consolidacion';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';
import { consolidadorPrueba, rondaPrueba } from '../testing/datos-prueba';
import { ConsolidacionInicioComponent } from './consolidacion-inicio';

const pagina = (rondas: Ronda[]): Page<Ronda> => ({ content: rondas, page: { size: 10, number: 0, totalElements: rondas.length, totalPages: 1 } });

describe('ConsolidacionInicioComponent', () => {
    let fixture: ComponentFixture<ConsolidacionInicioComponent>;
    let rondas: jasmine.SpyObj<RondasService>;
    let equipo: jasmine.SpyObj<EquipoConsolidacionService>;
    let permisos: string[];
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const crear = async (): Promise<void> => {
        fixture = TestBed.createComponent(ConsolidacionInicioComponent);
        await refrescar();
    };

    beforeEach(() => {
        permisos = ['consolidacion.administrar'];
        rondas = jasmine.createSpyObj<RondasService>('RondasService', ['listar']);
        rondas.listar.and.callFake((filtro) => of(pagina(filtro.estado === 'EN_CURSO' ? [rondaPrueba(7, { nombre: 'Noviembre', sinAsignar: 3 })] : [rondaPrueba(7, { nombre: 'Noviembre' }), rondaPrueba(3, { nombre: 'Agosto', estado: 'CERRADA' })])));
        equipo = jasmine.createSpyObj<EquipoConsolidacionService>('EquipoConsolidacionService', ['listar', 'agregar', 'cambiarEstado']);
        equipo.listar.and.returnValue(of([consolidadorPrueba(25, { nombreCompleto: 'Luis Rojas' })]));
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: RondasService, useValue: rondas },
                { provide: EquipoConsolidacionService, useValue: equipo },
                { provide: SesionService, useValue: { tienePermiso: (permiso: string) => permisos.includes(permiso) } }
            ]
        });
    });

    it('muestra las rondas en curso como tarjetas y todas las rondas en la grilla', async () => {
        await crear();
        expect(html().querySelectorAll('app-tarjeta-ronda').length).toBe(1);
        expect(html().textContent).toContain('3 sin asignar');
        expect(html().querySelector('app-tabla-rondas')?.textContent).toContain('Agosto');
    });

    it('sin rondas invita a crear la primera', async () => {
        rondas.listar.and.returnValue(of(pagina([])));
        await crear();
        expect(html().textContent).toContain('Aún no hay rondas. Crea la primera');
    });

    it('quien solo ve no puede crear rondas ni ver el equipo', async () => {
        permisos = ['consolidacion.ver'];
        await crear();
        expect(html().textContent).not.toContain('Nueva ronda');
        expect(html().querySelector('p-tablist')).toBeNull();
    });

    it('abre la ronda al tocar una tarjeta', async () => {
        await crear();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        (html().querySelector('app-tarjeta-ronda button') as HTMLElement).click();
        expect(navegar).toHaveBeenCalledWith(['/consolidacion', 'rondas', 7]);
    });

    it('carga el equipo al abrir su pestaña', async () => {
        await crear();
        expect(equipo.listar).not.toHaveBeenCalled();
        (Array.from(html().querySelectorAll('p-tab')).find((pestana) => pestana.textContent!.includes('Equipo')) as HTMLElement).click();
        await refrescar();
        expect(equipo.listar).toHaveBeenCalled();
        expect(html().textContent).toContain('Luis Rojas');
    });
});
