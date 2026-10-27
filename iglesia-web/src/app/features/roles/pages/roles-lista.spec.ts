import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { RolAcceso } from '../models/rol-acceso';
import { RolesService } from '../services/roles.service';
import { CATALOGO, ROLES_BASE, SECRETARIA } from '../testing/roles-prueba';
import { RolesListaComponent } from './roles-lista';

describe('RolesListaComponent', () => {
    let servicio: jasmine.SpyObj<RolesService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<RolesListaComponent>;

    const conflicto = (detail: string): HttpErrorResponse => new HttpErrorResponse({ status: 409, error: { detail } });

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [RolesListaComponent],
            providers: [
                { provide: RolesService, useValue: servicio },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(RolesListaComponent);
        await fixture.whenStable();
    };

    beforeEach(() => {
        permisos = new Set<Permiso>(['roles.ver', 'roles.administrar']);
        servicio = jasmine.createSpyObj<RolesService>('RolesService', ['listar', 'permisos', 'crear', 'actualizar', 'cambiarEstado']);
        servicio.listar.and.returnValue(of([...ROLES_BASE, SECRETARIA]));
        servicio.permisos.and.returnValue(of(CATALOGO));
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined =>
        Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta || b.getAttribute('aria-label') === etiqueta);
    const componente = () => fixture.componentInstance;
    const confirmacion = (): ConfirmationService => fixture.debugElement.injector.get(ConfirmationService);

    it('carga los roles y el catálogo y los muestra en la tabla', async () => {
        await crear();

        expect(servicio.listar).toHaveBeenCalledTimes(1);
        expect(servicio.permisos).toHaveBeenCalledTimes(1);
        expect(html().querySelectorAll('tbody tr').length).toBe(5);
        expect(html().textContent).toContain('Secretaría');
    });

    it('mientras carga muestra el estado de carga', async () => {
        servicio.listar.and.returnValue(new Subject<RolAcceso[]>());
        await crear();

        expect(html().querySelector('[aria-label="Cargando roles"]')).not.toBeNull();
        expect(boton('Nuevo rol')?.disabled).toBeTrue();
    });

    it('si falla la carga muestra el error y permite reintentar', async () => {
        servicio.listar.and.returnValues(throwError(() => new HttpErrorResponse({ status: 500 })), of([SECRETARIA]));
        await crear();

        expect(html().querySelector('[role="alert"]')?.textContent).toContain('No se pudieron cargar los roles.');
        expect(boton('Nuevo rol')).toBeUndefined();

        boton('Reintentar')!.click();
        await fixture.whenStable();

        expect(servicio.listar).toHaveBeenCalledTimes(2);
        expect(html().querySelector('[role="alert"]')).toBeNull();
        expect(html().textContent).toContain('Secretaría');
    });

    it('«Nuevo rol» solo aparece con roles.administrar', async () => {
        permisos = new Set<Permiso>(['roles.ver']);
        await crear();

        expect(boton('Nuevo rol')).toBeUndefined();
        expect(boton('Editar Secretaría')).toBeUndefined();
    });

    it('«Nuevo rol» abre el diálogo de creación', async () => {
        await crear();

        boton('Nuevo rol')!.click();
        await fixture.whenStable();

        expect(componente()['dialogoVisible']()).toBeTrue();
        expect(componente()['dialogo']()).toEqual({ modo: 'crear', rol: null });
    });

    it('un rol base se abre en modo solo lectura', async () => {
        await crear();

        boton('Ver permisos')!.click();
        await fixture.whenStable();

        expect(componente()['dialogo']()).toEqual({ modo: 'ver', rol: ROLES_BASE[0] });
    });

    describe('guardar', () => {
        const datos = { nombre: 'Tesorería', rolBaseId: 2, permisos: ['usuarios.ver'] as Permiso[] };
        const tesoreria: RolAcceso = { ...SECRETARIA, id: 11, nombre: 'Tesorería', permisos: ['usuarios.ver'], cantidadUsuarios: 0 };

        it('crea el rol, lo agrega a la tabla y cierra el diálogo', async () => {
            await crear();
            servicio.crear.and.returnValue(of(tesoreria));
            componente()['nuevo']();

            componente()['guardar'](datos);
            await fixture.whenStable();

            expect(servicio.crear).toHaveBeenCalledOnceWith(datos);
            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(html().textContent).toContain('Tesorería');
        });

        it('al editar actualiza el rol en la tabla', async () => {
            await crear();
            servicio.actualizar.and.returnValue(of({ ...SECRETARIA, nombre: 'Secretaría general' }));
            componente()['editar'](SECRETARIA);

            componente()['guardar']({ ...datos, nombre: 'Secretaría general', rolBaseId: null });
            await fixture.whenStable();

            expect(servicio.actualizar).toHaveBeenCalledOnceWith(10, { ...datos, nombre: 'Secretaría general', rolBaseId: null });
            expect(html().querySelectorAll('tbody tr').length).toBe(5);
            expect(html().textContent).toContain('Secretaría general');
        });

        it('un nombre repetido muestra el mensaje 409 del backend y deja el diálogo abierto', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => conflicto('Ya existe un rol con el nombre «Tesorería»')));
            componente()['nuevo']();

            componente()['guardar'](datos);
            await fixture.whenStable();

            expect(componente()['errorDialogo']()).toBe('Ya existe un rol con el nombre «Tesorería»');
            expect(componente()['dialogoVisible']()).toBeTrue();
            expect(componente()['guardando']()).toBeFalse();
        });

        it('un 5xx no repite en el diálogo el aviso del toast global', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
            componente()['nuevo']();

            componente()['guardar'](datos);

            expect(componente()['errorDialogo']()).toBeNull();
        });
    });

    describe('cambiar estado', () => {
        it('desactivar pide confirmación indicando cuántos usuarios tienen el rol', async () => {
            await crear();
            const confirmar = spyOn(confirmacion(), 'confirm');

            boton('Desactivar Secretaría')!.click();

            const opciones = confirmar.calls.mostRecent().args[0] as Confirmation;
            expect(opciones.message).toContain('Secretaría');
            expect(opciones.message).toContain('Lo tienen asignado 3 usuarios.');
            expect(servicio.cambiarEstado).not.toHaveBeenCalled();
        });

        it('al confirmar desactiva el rol y lo actualiza en la tabla', async () => {
            await crear();
            servicio.cambiarEstado.and.returnValue(of({ ...SECRETARIA, activo: false }));
            spyOn(confirmacion(), 'confirm').and.callFake((opciones: Confirmation) => {
                opciones.accept?.();
                return confirmacion();
            });

            boton('Desactivar Secretaría')!.click();
            await fixture.whenStable();

            expect(servicio.cambiarEstado).toHaveBeenCalledOnceWith(10, false);
            expect(boton('Reactivar Secretaría')).toBeDefined();
        });

        it('un 409 muestra el mensaje del backend y el rol sigue activo', async () => {
            await crear();
            servicio.cambiarEstado.and.returnValue(throwError(() => conflicto('Hay usuarios que quedarían sin roles activos')));
            spyOn(confirmacion(), 'confirm').and.callFake((opciones: Confirmation) => {
                opciones.accept?.();
                return confirmacion();
            });

            boton('Desactivar Secretaría')!.click();
            await fixture.whenStable();

            expect(html().textContent).toContain('Hay usuarios que quedarían sin roles activos');
            expect(boton('Desactivar Secretaría')).toBeDefined();
        });

        it('reactivar no pide confirmación', async () => {
            servicio.listar.and.returnValue(of([{ ...SECRETARIA, activo: false }]));
            servicio.cambiarEstado.and.returnValue(of(SECRETARIA));
            await crear();
            const confirmar = spyOn(confirmacion(), 'confirm');

            boton('Reactivar Secretaría')!.click();
            await fixture.whenStable();

            expect(confirmar).not.toHaveBeenCalled();
            expect(servicio.cambiarEstado).toHaveBeenCalledOnceWith(10, true);
            expect(boton('Desactivar Secretaría')).toBeDefined();
        });
    });
});
