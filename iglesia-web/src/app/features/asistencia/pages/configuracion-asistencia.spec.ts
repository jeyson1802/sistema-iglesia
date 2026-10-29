import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CultoProgramado } from '../models/asistencia';
import { CultosProgramadosService } from '../services/cultos-programados.service';
import { SeguimientoService } from '../services/seguimiento.service';
import { ConfiguracionAsistenciaComponent } from './configuracion-asistencia';

describe('ConfiguracionAsistenciaComponent', () => {
    const DOMINICAL: CultoProgramado = { id: 1, nombre: 'Dominical', diaSemana: 'DOMINGO', hora: '10:00:00', activo: true };
    let servicio: jasmine.SpyObj<CultosProgramadosService>;
    let seguimiento: jasmine.SpyObj<SeguimientoService>;
    let fixture: ComponentFixture<ConfiguracionAsistenciaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [ConfiguracionAsistenciaComponent],
            providers: [
                { provide: CultosProgramadosService, useValue: servicio },
                { provide: SeguimientoService, useValue: seguimiento }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(ConfiguracionAsistenciaComponent);
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const pagina = () => fixture.componentInstance as unknown as { guardar: (datos: unknown) => void; editar: (culto: CultoProgramado) => void; dialogoVisible: () => boolean; errorDialogo: () => string | null };

    beforeEach(() => {
        servicio = jasmine.createSpyObj<CultosProgramadosService>('CultosProgramadosService', ['listar', 'crear', 'actualizar', 'cambiarEstado']);
        servicio.listar.and.returnValue(of([DOMINICAL]));
        seguimiento = jasmine.createSpyObj<SeguimientoService>('SeguimientoService', ['reglas', 'guardarReglas']);
        seguimiento.reglas.and.returnValue(of({ faltasSeguidas: 3, puntosCaida: 30, diasNuevo: 90 }));
    });

    it('muestra las reglas de seguimiento y avisa al guardarlas', async () => {
        seguimiento.guardarReglas.and.returnValue(of({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 }));
        await crear();
        expect(html().querySelector('app-formulario-reglas')).not.toBeNull();

        (fixture.componentInstance as unknown as { guardarReglas: (r: unknown) => void }).guardarReglas({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 });
        fixture.detectChanges();

        expect(seguimiento.guardarReglas).toHaveBeenCalledWith({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 });
        expect(html().textContent).toContain('Reglas guardadas');
    });

    it('si el servidor rechaza las reglas muestra el motivo', async () => {
        seguimiento.guardarReglas.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'Las faltas seguidas deben ir de 2 a 10.' } })));
        await crear();

        (fixture.componentInstance as unknown as { guardarReglas: (r: unknown) => void }).guardarReglas({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 });
        fixture.detectChanges();

        expect(html().textContent).toContain('Las faltas seguidas deben ir de 2 a 10.');
    });

    it('lista los horarios de culto', async () => {
        await crear();

        expect(html().textContent).toContain('Dominical');
    });

    it('crea un culto y recarga la lista', async () => {
        servicio.crear.and.returnValue(of({ ...DOMINICAL, id: 2, nombre: 'Oración' }));
        await crear();

        pagina().guardar({ nombre: 'Oración', diaSemana: 'MIERCOLES', hora: '19:30' });

        expect(servicio.crear).toHaveBeenCalledWith({ nombre: 'Oración', diaSemana: 'MIERCOLES', hora: '19:30' });
        expect(servicio.listar).toHaveBeenCalledTimes(2);
        expect(pagina().dialogoVisible()).toBeFalse();
    });

    it('un nombre repetido se muestra en el diálogo', async () => {
        servicio.actualizar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Ya existe un culto con ese nombre' } })));
        await crear();
        pagina().editar(DOMINICAL);

        pagina().guardar({ nombre: 'Oración', diaSemana: 'DOMINGO', hora: '10:00' });

        expect(servicio.actualizar).toHaveBeenCalledWith(1, jasmine.anything());
        expect(pagina().errorDialogo()).toBe('Ya existe un culto con ese nombre');
    });

    it('desactiva un culto y actualiza la fila', async () => {
        servicio.cambiarEstado.and.returnValue(of({ ...DOMINICAL, activo: false }));
        await crear();

        (html().querySelector('button[aria-label="Desactivar Dominical"]') as HTMLButtonElement).click();
        fixture.detectChanges();

        expect(servicio.cambiarEstado).toHaveBeenCalledWith(1, false);
        expect(html().textContent).toContain('Inactivo');
    });
});
