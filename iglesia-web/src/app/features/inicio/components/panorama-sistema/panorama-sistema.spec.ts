import { ComponentFixture, DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { EstadisticasService } from '../../services/estadisticas.service';
import { estadisticasServicePrueba } from '../../testing/estadisticas-service-prueba';
import { sesionPrueba } from '../../testing/sesion-prueba';
import { PanoramaSistemaComponent } from './panorama-sistema';

describe('PanoramaSistemaComponent', () => {
    let fixture: ComponentFixture<PanoramaSistemaComponent>;
    let servicio: jasmine.SpyObj<EstadisticasService>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        servicio = estadisticasServicePrueba();
        TestBed.configureTestingModule({
            providers: [provideRouter([]), { provide: SesionService, useValue: sesionPrueba(perfilBase('ADMIN_SISTEMA')) }, { provide: EstadisticasService, useValue: servicio }],
            deferBlockBehavior: DeferBlockBehavior.Manual
        });
        fixture = TestBed.createComponent(PanoramaSistemaComponent);
        fixture.componentRef.setInput('periodo', { periodo: 'SEMANA' });
        await fixture.whenStable();
        for (const bloque of await fixture.getDeferBlocks()) {
            await bloque.render(DeferBlockState.Complete);
        }
        await fixture.whenStable();
    });

    it('muestra las cifras clave, el crecimiento y el uso del sistema del periodo', () => {
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(servicio.sistema).toHaveBeenCalledWith({ periodo: 'SEMANA' });
        expect(servicio.uso).toHaveBeenCalledWith({ periodo: 'SEMANA' });
        expect(texto).toContain('Iglesiasactivas3');
        expect(texto).toContain('Miembrosactivos450');
        expect(texto).toContain('Usuariosactivosenelmes12');
        expect(html().querySelector('app-bloque-sistema')).not.toBeNull();
        expect(html().querySelector('app-bloque-uso')).not.toBeNull();
    });

    it('nunca pide ni muestra aportes', () => {
        expect(servicio.aportes).not.toHaveBeenCalled();
        expect(html().textContent).not.toMatch(/aporte|diezmo|ofrenda/i);
    });
});
