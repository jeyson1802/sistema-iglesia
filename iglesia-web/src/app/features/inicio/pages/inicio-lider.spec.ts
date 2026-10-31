import { ComponentFixture, DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { ReunionesService } from '@/app/features/asistencia/services/reuniones.service';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';
import { EstadisticasService } from '../services/estadisticas.service';
import { asistenciaPrueba, gruposPrueba, miembrosPrueba } from '../testing/estadisticas-prueba';
import { estadisticasServicePrueba } from '../testing/estadisticas-service-prueba';
import { sesionPrueba } from '../testing/sesion-prueba';
import { InicioLiderComponent } from './inicio-lider';

describe('InicioLiderComponent', () => {
    let fixture: ComponentFixture<InicioLiderComponent>;
    let servicio: jasmine.SpyObj<EstadisticasService>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        localStorage.removeItem('inicio.periodo');
        servicio = estadisticasServicePrueba();
        servicio.miembros.and.returnValue(of(miembrosPrueba({ red: true, activos: 25, directos: 4, sinLider: null })));
        servicio.asistencia.and.returnValue(of(asistenciaPrueba({ red: true })));
        servicio.grupos.and.returnValue(of(gruposPrueba({ red: true })));
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: SesionService, useValue: sesionPrueba(perfilBase('LIDER')) },
                { provide: EstadisticasService, useValue: servicio },
                { provide: ReunionesService, useValue: { hoy: () => of([{ id: null, tipo: 'GRUPO', nombre: 'Célula de Ana', hora: '19:30', cultoProgramadoId: null, grupoId: 3, totalAsistentes: 0 }]) } },
                { provide: CasosService, useValue: { resumenMisCasos: () => of({ paraHoy: 2, sinContactar: 1, enContacto: 2, asignados: 3, contactados: 0 }) } }
            ],
            deferBlockBehavior: DeferBlockBehavior.Manual
        });
        fixture = TestBed.createComponent(InicioLiderComponent);
        await fixture.whenStable();
        for (const bloque of await fixture.getDeferBlocks()) {
            await bloque.render(DeferBlockState.Complete);
        }
        await fixture.whenStable();
    });

    it('muestra «Mi red» con sus atajos, sus cifras y los bloques de su red', () => {
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(html().querySelector('h1')?.textContent?.replace(/\s+/g, '')).toContain('Mired');
        const atajos = Array.from(html().querySelectorAll('app-atajos-del-dia a')).map((enlace) => enlace.textContent!.replace(/\s+/g, ''));
        expect(atajos[0]).toContain('TomarasistenciadeCéluladeAna');
        expect(atajos.some((atajo) => atajo.includes('Personasdeturedconalerta(5)'))).toBeTrue();
        expect(atajos.some((atajo) => atajo.includes('Misasignados(3)'))).toBeTrue();
        expect(texto).toContain('Personasentured25');
        expect(html().querySelector('app-bloque-miembros')).not.toBeNull();
        expect(html().querySelector('app-bloque-asistencia')).not.toBeNull();
        expect(html().querySelector('app-bloque-grupos')).not.toBeNull();
    });

    it('nunca pide aportes, consolidación de la iglesia, uso ni cifras del sistema', () => {
        expect(servicio.aportes).not.toHaveBeenCalled();
        expect(servicio.consolidacion).not.toHaveBeenCalled();
        expect(servicio.uso).not.toHaveBeenCalled();
        expect(servicio.sistema).not.toHaveBeenCalled();
        expect(html().textContent).not.toMatch(/aporte|diezmo|ofrenda/i);
    });
});
