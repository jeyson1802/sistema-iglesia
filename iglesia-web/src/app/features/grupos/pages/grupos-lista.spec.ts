import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NombresGrupos } from '@/app/core/configuracion/configuracion-iglesia.service';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { Grupo, GuardarGrupo } from '../models/grupo';
import { GruposService } from '../services/grupos.service';
import { BETANIA, OLIVOS } from '../testing/grupos-prueba';
import { GruposListaComponent } from './grupos-lista';

describe('GruposListaComponent', () => {
    const datos: GuardarGrupo = { liderId: 99, anfitrionId: null, diaReunion: null, horaReunion: null, direccion: null, distritoCodigo: null, descripcion: null };
    let servicio: jasmine.SpyObj<GruposService>;
    let configuracion: { nombresGrupos: ReturnType<typeof signal<NombresGrupos>>; guardar: jasmine.Spy };
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<GruposListaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [GruposListaComponent],
            providers: [
                provideRouter([]),
                { provide: GruposService, useValue: servicio },
                { provide: ConfiguracionIglesiaService, useValue: configuracion },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } },
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GruposListaComponent);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta || b.getAttribute('aria-label') === etiqueta);
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;
    const senal = <T>(nombre: string): T => (fixture.componentInstance as unknown as Record<string, () => T>)[nombre]();

    beforeEach(() => {
        permisos = new Set<Permiso>(['grupos.ver', 'grupos.administrar']);
        servicio = jasmine.createSpyObj<GruposService>('GruposService', ['listar', 'crear', 'actualizar', 'cambiarEstado']);
        servicio.listar.and.returnValue(of([BETANIA, OLIVOS]));
        configuracion = { nombresGrupos: signal({ nombreGrupoSingular: 'Hogar de paz', nombreGrupoPlural: 'Hogares de paz' }), guardar: jasmine.createSpy('guardar') };
    });

    afterEach(() => fixture?.destroy());

    it('usa el nombre configurado en el título y la acción principal', async () => {
        await crear();

        expect(html().querySelector('h1')?.textContent).toContain('Hogares de paz');
        expect(boton('Agregar hogar de paz')).toBeDefined();
        expect(html().querySelectorAll('tbody tr').length).toBe(2);
    });

    it('sin permiso de administrar solo muestra la tabla', async () => {
        permisos = new Set<Permiso>(['grupos.ver']);
        await crear();

        expect(boton('Agregar hogar de paz')).toBeUndefined();
        expect(boton('Cómo se llaman')).toBeUndefined();
    });

    it('muestra el error de carga con reintento', async () => {
        servicio.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await crear();

        expect(html().textContent).toContain('No se pudieron cargar los grupos.');
    });

    it('crea un grupo y lo agrega en orden alfabético por líder', async () => {
        servicio.crear.and.returnValue(of({ ...OLIVOS, id: 9, lider: { id: 99, nombre: 'Bárbara Luna', activo: true } }));
        await crear();

        componente()['nuevo']();
        componente()['guardar'](datos);
        await fixture.whenStable();

        expect(senal<Grupo[]>('grupos').map(({ lider }) => lider.nombre)).toEqual(['Ana Ríos', 'Bárbara Luna', 'Beto Soto']);
    });

    it('un error por campo se muestra junto al campo y no arriba', async () => {
        servicio.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Inactivo', errores: { liderId: 'El líder está inactivo' } } })));
        await crear();

        componente()['nuevo']();
        componente()['guardar'](datos);

        expect(senal('erroresCampo')).toEqual({ liderId: 'El líder está inactivo' });
        expect(senal('errorDialogo')).toBeNull();
    });

    it('desactivar un grupo con miembros activos muestra el motivo', async () => {
        servicio.cambiarEstado.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Tiene miembros activos' } })));
        await crear();
        spyOn(fixture.debugElement.injector.get(ConfirmationService), 'confirm').and.callFake((c) => {
            c.accept?.();
            return fixture.debugElement.injector.get(ConfirmationService);
        });

        boton('Desactivar Hogar de paz de Ana Ríos')?.click();
        await fixture.whenStable();

        expect(html().textContent).toContain('Tiene miembros activos');
    });

    it('guarda los nombres de los grupos', async () => {
        configuracion.guardar.and.returnValue(of({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }));
        await crear();

        componente()['abrirNombres']();
        componente()['guardarNombres']({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' });

        expect(configuracion.guardar).toHaveBeenCalledWith({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' });
        expect(senal('nombresVisible')).toBeFalse();
    });
});
