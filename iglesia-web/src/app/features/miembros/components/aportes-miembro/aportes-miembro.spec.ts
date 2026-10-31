import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { AportesService } from '@/app/features/aportes/services/aportes.service';
import { ConfiguracionAportesService } from '@/app/features/aportes/services/configuracion-aportes.service';
import { aporte, RESUMEN } from '@/app/features/aportes/testing/aportes-prueba';
import { AportesMiembroComponent } from './aportes-miembro';

describe('AportesMiembroComponent', () => {
    let servicio: jasmine.SpyObj<AportesService>;
    let perfil: Perfil;
    let miembroPropio: number;
    let fixture: ComponentFixture<AportesMiembroComponent>;

    const crear = async (miembroId = 12): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [AportesMiembroComponent],
            providers: [
                provideRouter([]),
                { provide: AportesService, useValue: servicio },
                { provide: ConfiguracionAportesService, useValue: { configuracion: signal({ moneda: 'PEN', monedaEditable: true }), asegurarCargada: () => undefined } },
                { provide: SesionService, useValue: { perfilActivo: () => perfil, usuarioActual: () => ({ miembroId: miembroPropio }), tienePermiso: (permiso: Permiso) => perfil.permisos.includes(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(AportesMiembroComponent);
        fixture.componentRef.setInput('miembroId', miembroId);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        perfil = perfilBase('ADMIN_IGLESIA');
        miembroPropio = 2;
        servicio = jasmine.createSpyObj<AportesService>('AportesService', ['resumen', 'listar']);
        servicio.resumen.and.returnValue(of({ ...RESUMEN, total: 1200 }));
        servicio.listar.and.returnValue(of({ content: [aporte()], page: { size: 5, number: 0, totalElements: 1, totalPages: 1 } }));
    });

    it('muestra el total del año, los últimos aportes y el enlace filtrado por el miembro', async () => {
        await crear();

        expect(html().textContent).toContain('S/\u00a01,200.00');
        expect(html().textContent).toContain('Diezmo');
        expect(servicio.listar).toHaveBeenCalledWith(jasmine.objectContaining({ miembroId: 12 }), 0, 5);
        expect(html().querySelector('a')?.getAttribute('href')).toContain('miembroId=12');
        expect(html().classList).toContain('card');
    });

    it('un líder solo ve la sección en su propia ficha', async () => {
        perfil = perfilBase('LIDER');
        await crear(12);

        expect(html().textContent?.trim()).toBe('');
        expect(html().classList).not.toContain('card');
        expect(servicio.resumen).not.toHaveBeenCalled();
    });

    it('el líder no ve aportes, ni siquiera en su propia ficha', async () => {
        perfil = perfilBase('LIDER');
        miembroPropio = 12;
        await crear(12);

        expect(html().textContent).not.toContain('Aportes');
        expect(servicio.resumen).not.toHaveBeenCalled();
    });

    it('si no carga ofrece reintentar', async () => {
        servicio.resumen.and.returnValue(throwError(() => new Error('falla')));
        await crear();

        expect(html().textContent).toContain('No se pudieron cargar los aportes.');
    });
});
