import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { EstadisticasService } from '../services/estadisticas.service';
import { estadisticasServicePrueba } from '../testing/estadisticas-service-prueba';
import { sesionPrueba } from '../testing/sesion-prueba';
import { InicioMiembroComponent } from './inicio-miembro';

describe('InicioMiembroComponent', () => {
    let fixture: ComponentFixture<InicioMiembroComponent>;
    let servicio: jasmine.SpyObj<EstadisticasService>;
    const html = (): HTMLElement => fixture.nativeElement;

    async function crear(): Promise<void> {
        TestBed.configureTestingModule({
            providers: [{ provide: SesionService, useValue: sesionPrueba(perfilBase('MIEMBRO')) }, { provide: EstadisticasService, useValue: servicio }]
        });
        fixture = TestBed.createComponent(InicioMiembroComponent);
        await fixture.whenStable();
    }

    beforeEach(() => {
        servicio = estadisticasServicePrueba();
    });

    it('saluda por su nombre y muestra su código, su asistencia y sus datos', async () => {
        await crear();
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Hola,Ana.Líder·CéluladeLuis.');
        expect(html().querySelector('app-mi-codigo-qr')).not.toBeNull();
        expect(html().querySelector('app-mi-asistencia')).not.toBeNull();
        expect(html().querySelector('app-mis-datos')).not.toBeNull();
        expect(texto).not.toContain('Estamospreparandoestasección');
    });

    it('nunca pide estadísticas ni aportes', async () => {
        await crear();
        expect(servicio.aportes).not.toHaveBeenCalled();
        expect(servicio.miembros).not.toHaveBeenCalled();
        expect(html().textContent).not.toMatch(/aporte|diezmo|ofrenda/i);
    });

    it('sin miembro vinculado explica qué hacer', async () => {
        servicio.miPerfil.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Tu usuario no está vinculado a un miembro de la iglesia' } })));
        await crear();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Tuusuarioaúnnoestávinculadoatufichademiembro');
        expect(html().querySelector('app-mi-asistencia')).toBeNull();
    });
});
