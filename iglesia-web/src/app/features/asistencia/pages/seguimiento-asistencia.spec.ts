import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NOMBRES_GRUPOS_POR_DEFECTO } from '@/app/core/configuracion/configuracion-iglesia.service';
import { Grupo } from '@/app/features/grupos/models/grupo';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { FILTRO_SEGUIMIENTO_INICIAL, PersonaSeguimiento } from '../models/seguimiento';
import { SeguimientoService } from '../services/seguimiento.service';
import { SeguimientoAsistenciaComponent } from './seguimiento-asistencia';

describe('SeguimientoAsistenciaComponent', () => {
    const PERSONA: PersonaSeguimiento = {
        miembroId: 7,
        nombreCompleto: 'Ana Quispe',
        nivel: null,
        grupoId: null,
        grupo: null,
        lider: null,
        porcentaje: 20,
        porcentajeAnterior: 70,
        faltasSeguidas: 5,
        ultimaAsistencia: null,
        alertas: ['FALTAS_SEGUIDAS']
    };
    let servicio: jasmine.SpyObj<SeguimientoService>;
    let grupos: jasmine.SpyObj<GruposService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<SeguimientoAsistenciaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [SeguimientoAsistenciaComponent],
            providers: [
                provideRouter([]),
                { provide: SeguimientoService, useValue: servicio },
                { provide: GruposService, useValue: grupos },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: () => ({ ...NOMBRES_GRUPOS_POR_DEFECTO, nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(SeguimientoAsistenciaComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        permisos = new Set<Permiso>(['asistencia.ver', 'grupos.ver']);
        servicio = jasmine.createSpyObj<SeguimientoService>('SeguimientoService', ['lista', 'conteos', 'servicios']);
        servicio.lista.and.returnValue(of({ content: [PERSONA], page: { size: 10, number: 0, totalElements: 1, totalPages: 1 } }));
        servicio.conteos.and.returnValue(of({ personas: 5, conAlertas: 1, faltasSeguidas: 1, nuevoSinVolver: 0, asistenciaEnCaida: 0 }));
        servicio.servicios.and.returnValue(of({ semanas: ['2026-10-19', '2026-10-26'], cultos: [], personasEnCultos: [10, 2], presentesEnGrupos: [5, 1], porcentajeGrupos: [80, null] }));
        grupos = jasmine.createSpyObj<GruposService>('GruposService', ['listar']);
        grupos.listar.and.returnValue(of([{ id: 4, activo: true, lider: { id: 2, nombre: 'Juan Pérez', activo: true } } as Grupo]));
    });

    it('lista a quienes tienen alertas con sus cantidades y el filtro por grupo', async () => {
        await crear();

        expect(servicio.lista).toHaveBeenCalledWith(FILTRO_SEGUIMIENTO_INICIAL, 0, 10);
        expect(html().textContent).toContain('Ana Quispe');
        expect(html().querySelector('app-tabla-seguimiento p-paginator')).not.toBeNull();
        expect(html().querySelector('#filtro-seguimiento-grupo')).not.toBeNull();
    });

    it('al cambiar el filtro vuelve a la primera página y solo recalcula los conteos si cambia el grupo o el nombre', async () => {
        await crear();
        const componente = fixture.componentInstance as unknown as { filtrar(f: object): void };
        componente.filtrar({ ...FILTRO_SEGUIMIENTO_INICIAL, alerta: 'FALTAS_SEGUIDAS' });
        expect(servicio.conteos).toHaveBeenCalledTimes(1);
        componente.filtrar({ ...FILTRO_SEGUIMIENTO_INICIAL, alerta: 'FALTAS_SEGUIDAS', q: 'ana' });
        expect(servicio.conteos).toHaveBeenCalledTimes(2);
        expect(servicio.lista).toHaveBeenCalledWith(jasmine.objectContaining({ q: 'ana' }), 0, 10);
    });

    it('al elegir a alguien abre su seguimiento', async () => {
        await crear();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        (html().querySelector('p-table tbody tr') as HTMLElement).click();
        expect(navegar).toHaveBeenCalledWith(['/miembros', 7, 'asistencia']);
    });

    it('la pestaña «Servicios» carga la evolución solo al abrirla', async () => {
        await crear();
        expect(servicio.servicios).not.toHaveBeenCalled();
        (Array.from(html().querySelectorAll('p-tab')).find((tab) => tab.textContent?.includes('Servicios')) as HTMLElement).click();
        fixture.detectChanges();
        await fixture.whenStable();
        expect(servicio.servicios).toHaveBeenCalledTimes(1);
    });

    it('un líder ve su red, sin la pestaña de servicios ni el filtro por grupo', async () => {
        permisos = new Set<Permiso>(['asistencia.grupo', 'miembros.ver']);
        await crear();

        expect(html().textContent).toContain('saber a quién llamar');
        expect(html().querySelector('p-tablist')).toBeNull();
        expect(grupos.listar).not.toHaveBeenCalled();
    });

    it('un líder ve por defecto a toda su red, también a quienes están al día', async () => {
        permisos = new Set<Permiso>(['asistencia.grupo', 'miembros.ver']);
        await crear();

        expect(servicio.lista).toHaveBeenCalledWith(jasmine.objectContaining({ alerta: null, todos: true }), 0, 10);
        expect(html().textContent).toContain('Las personas de tu red y cómo van asistiendo');
    });

    it('sin permiso para ver miembros las filas no abren el detalle (no lleva a «Acceso denegado»)', async () => {
        permisos = new Set<Permiso>(['asistencia.grupo']);
        await crear();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

        const fila = html().querySelector('p-table tbody tr') as HTMLElement;
        expect(fila.classList).not.toContain('cursor-pointer');
        fila.click();
        expect(navegar).not.toHaveBeenCalled();
    });
});
