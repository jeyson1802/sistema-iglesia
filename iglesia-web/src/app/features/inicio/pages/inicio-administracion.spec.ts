import { signal } from '@angular/core';
import { ComponentFixture, DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { ReunionesService } from '@/app/features/asistencia/services/reuniones.service';
import { EstadisticasService } from '../services/estadisticas.service';
import { estadisticasServicePrueba } from '../testing/estadisticas-service-prueba';
import { sesionPrueba } from '../testing/sesion-prueba';
import { InicioAdministracionComponent } from './inicio-administracion';

describe('InicioAdministracionComponent', () => {
    let fixture: ComponentFixture<InicioAdministracionComponent>;
    let servicio: jasmine.SpyObj<EstadisticasService>;
    let reuniones: jasmine.SpyObj<ReunionesService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const texto = (): string => html().textContent!.replace(/\s+/g, '');

    async function crear(permisos?: readonly Permiso[]): Promise<void> {
        const perfil = permisos ? perfilPersonalizado(9, 'Secretaría', 'ADMIN_IGLESIA', permisos) : perfilBase('ADMIN_IGLESIA');
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: SesionService, useValue: sesionPrueba(perfil) },
                { provide: EstadisticasService, useValue: servicio },
                { provide: ReunionesService, useValue: reuniones },
                { provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Central') } }
            ],
            deferBlockBehavior: DeferBlockBehavior.Manual
        });
        fixture = TestBed.createComponent(InicioAdministracionComponent);
        await fixture.whenStable();
        for (const bloque of await fixture.getDeferBlocks()) {
            await bloque.render(DeferBlockState.Complete);
        }
        await fixture.whenStable();
    }

    beforeEach(() => {
        localStorage.removeItem('inicio.periodo');
        servicio = estadisticasServicePrueba();
        reuniones = jasmine.createSpyObj<ReunionesService>('ReunionesService', ['hoy']);
        reuniones.hoy.and.returnValue(of([{ id: null, tipo: 'CULTO', nombre: 'Culto dominical', hora: '10:00', cultoProgramadoId: 1, grupoId: null, totalAsistentes: 0 }]));
    });

    it('con todos los permisos muestra el nombre de la iglesia, los atajos, las cifras y los seis bloques', async () => {
        await crear();
        expect(html().querySelector('h1')?.textContent?.replace(/\s+/g, '')).toContain('IglesiaCentral');
        expect(texto()).not.toContain('Estamospreparandoestasección');
        const atajos = Array.from(html().querySelectorAll('app-atajos-del-dia a')).map((enlace) => enlace.textContent!.replace(/\s+/g, ''));
        expect(atajos[0]).toContain('Tomarasistenciadehoy');
        expect(atajos.some((atajo) => atajo.includes('Personasconalerta(5)'))).toBeTrue();
        expect(atajos.some((atajo) => atajo.includes('Rondasencurso(1)'))).toBeTrue();
        expect(atajos.some((atajo) => atajo.includes('Registrarmiembro'))).toBeTrue();
        expect(texto()).toContain('Totaldeaportes');
        for (const bloque of ['miembros', 'asistencia', 'grupos', 'consolidacion', 'aportes', 'uso']) {
            expect(html().querySelector(`app-bloque-${bloque}`)).withContext(bloque).not.toBeNull();
        }
        expect(servicio.miembros).toHaveBeenCalledWith({ periodo: 'MES' });
    });

    it('un rol de administración sin aportes.ver no ve aportes ni hace la petición', async () => {
        await crear(['miembros.ver', 'asistencia.ver', 'grupos.ver', 'consolidacion.ver', 'usuarios.ver']);
        expect(servicio.aportes).not.toHaveBeenCalled();
        expect(html().querySelector('app-bloque-aportes')).toBeNull();
        expect(texto()).not.toContain('Totaldeaportes');
    });

    it('sin el permiso de un módulo no crea su bloque ni pide sus datos', async () => {
        await crear(['miembros.ver']);
        expect(html().querySelector('app-bloque-consolidacion')).toBeNull();
        expect(html().querySelector('app-bloque-uso')).toBeNull();
        expect(servicio.consolidacion).not.toHaveBeenCalled();
        expect(servicio.uso).not.toHaveBeenCalled();
        expect(reuniones.hoy).not.toHaveBeenCalled();
    });

    it('al cambiar el periodo vuelve a pedir todas las cifras con el nuevo periodo y lo recuerda', async () => {
        await crear();
        const anio = { periodo: 'ANIO' } as const;
        (fixture.componentInstance as unknown as { periodo: { set(valor: typeof anio): void } }).periodo.set(anio);
        await fixture.whenStable();
        expect(servicio.miembros).toHaveBeenCalledWith(anio);
        expect(servicio.aportes).toHaveBeenCalledWith(anio);
        expect(servicio.uso).toHaveBeenCalledWith(anio);
        expect(JSON.parse(localStorage.getItem('inicio.periodo')!)).toEqual(anio);
    });

    it('si un bloque falla, los demás siguen y ese se puede reintentar', async () => {
        servicio.miembros.and.returnValue(throwError(() => new Error('falló')));
        await crear();
        expect(html().querySelector('app-bloque-miembros [role="alert"]')).not.toBeNull();
        expect(html().querySelector('app-bloque-asistencia [role="alert"]')).toBeNull();
        expect(texto()).toContain('Asistentesporculto');
    });
});
