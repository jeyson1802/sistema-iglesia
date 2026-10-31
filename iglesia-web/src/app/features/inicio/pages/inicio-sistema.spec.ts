import { signal } from '@angular/core';
import { ComponentFixture, DeferBlockBehavior, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { ReunionesService } from '@/app/features/asistencia/services/reuniones.service';
import { EstadisticasService } from '../services/estadisticas.service';
import { estadisticasServicePrueba } from '../testing/estadisticas-service-prueba';
import { sesionPrueba } from '../testing/sesion-prueba';
import { InicioSistemaComponent } from './inicio-sistema';

describe('InicioSistemaComponent', () => {
    let fixture: ComponentFixture<InicioSistemaComponent>;
    let servicio: jasmine.SpyObj<EstadisticasService>;
    const html = (): HTMLElement => fixture.nativeElement;

    async function crear(actuandoEnIglesia: boolean): Promise<void> {
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: SesionService, useValue: sesionPrueba(perfilBase('ADMIN_SISTEMA'), actuandoEnIglesia) },
                { provide: EstadisticasService, useValue: servicio },
                { provide: ReunionesService, useValue: { hoy: () => of([]) } },
                { provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Central') } }
            ],
            deferBlockBehavior: DeferBlockBehavior.Manual
        });
        fixture = TestBed.createComponent(InicioSistemaComponent);
        await fixture.whenStable();
    }

    beforeEach(() => {
        localStorage.removeItem('inicio.periodo');
        servicio = estadisticasServicePrueba();
    });

    it('en la vista global muestra el panorama del sistema con sus cifras', async () => {
        await crear(false);
        expect(html().querySelector('h1')?.textContent?.replace(/\s+/g, '')).toContain('Panoramadelsistema');
        expect(html().querySelector('app-panorama-sistema')).not.toBeNull();
        expect(html().querySelector('app-inicio-administracion')).toBeNull();
        expect(servicio.sistema).toHaveBeenCalledWith({ periodo: 'MES' });
        expect(html().textContent!.replace(/\s+/g, '')).not.toContain('Estamospreparandoestasección');
    });

    it('dentro de una iglesia muestra el inicio de administración con el aviso y no pide las cifras globales', async () => {
        await crear(true);
        expect(html().textContent!.replace(/\s+/g, '')).toContain('EstásviendoeliniciodeIglesiaCentral.');
        expect(html().querySelector('app-inicio-administracion')).not.toBeNull();
        expect(servicio.sistema).not.toHaveBeenCalled();
    });
});
