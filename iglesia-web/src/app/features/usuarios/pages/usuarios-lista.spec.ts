import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Page } from '@/app/core/models/page';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { ROLES_BASE, SECRETARIA } from '@/app/features/roles/testing/roles-prueba';
import { EditarUsuario, UsuarioAdmin } from '../models/usuario-admin';
import { UsuariosService } from '../services/usuarios.service';
import { UsuariosListaComponent } from './usuarios-lista';

describe('UsuariosListaComponent', () => {
    let servicio: jasmine.SpyObj<UsuariosService>;
    let rolesServicio: jasmine.SpyObj<RolesService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<UsuariosListaComponent>;

    const YO: UsuarioAdmin = {
        id: 1,
        correo: 'admin@iglesia.test',
        miembroId: null,
        activo: true,
        debeCambiarContrasena: false,
        roles: [{ id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA' }],
        creadoEn: new Date(2026, 0, 1)
    };
    const ANA: UsuarioAdmin = { ...YO, id: 7, correo: 'ana@iglesia.test', miembroId: 55, roles: [{ id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA' }] };
    const FILTRO_INICIAL = { correo: '', activo: null, rolId: null };

    const pagina = (usuarios: UsuarioAdmin[], totalElements = usuarios.length, number = 0): Page<UsuarioAdmin> => ({
        content: usuarios,
        page: { size: 10, number, totalElements, totalPages: Math.ceil(totalElements / 10) }
    });
    const conError = (status: number, detail?: string): HttpErrorResponse => new HttpErrorResponse({ status, error: detail ? { detail } : null });

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [UsuariosListaComponent],
            providers: [
                { provide: UsuariosService, useValue: servicio },
                { provide: RolesService, useValue: rolesServicio },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso), usuarioActual: () => ({ id: YO.id }) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(UsuariosListaComponent);
        await fixture.whenStable();
    };

    beforeEach(() => {
        permisos = new Set<Permiso>(['usuarios.ver', 'usuarios.administrar']);
        servicio = jasmine.createSpyObj<UsuariosService>('UsuariosService', ['listar', 'crear', 'actualizar', 'restablecerContrasena']);
        servicio.listar.and.returnValue(of(pagina([YO, ANA])));
        rolesServicio = jasmine.createSpyObj<RolesService>('RolesService', ['listar']);
        rolesServicio.listar.and.returnValue(of([...ROLES_BASE, SECRETARIA]));
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta || b.getAttribute('aria-label') === etiqueta);
    const componente = () => fixture.componentInstance;
    const toast = (): MessageService => fixture.debugElement.injector.get(MessageService);

    it('carga los roles y la primera página sin filtros', async () => {
        await crear();

        expect(rolesServicio.listar).toHaveBeenCalledTimes(1);
        expect(servicio.listar).toHaveBeenCalledOnceWith(FILTRO_INICIAL, 0, 10);
        expect(filas().length).toBe(2);
        expect(html().textContent).toContain('ana@iglesia.test');
    });

    it('mientras carga muestra el esqueleto', async () => {
        servicio.listar.and.returnValue(new Subject<Page<UsuarioAdmin>>());
        await crear();

        expect(html().querySelectorAll('tbody p-skeleton').length).toBeGreaterThan(0);
    });

    it('si falla la carga muestra el error y permite reintentar', async () => {
        servicio.listar.and.returnValues(
            throwError(() => conError(500)),
            of(pagina([ANA]))
        );
        await crear();

        expect(html().querySelector('[role="alert"]')?.textContent).toContain('No se pudieron cargar los usuarios.');

        boton('Reintentar')!.click();
        await fixture.whenStable();

        expect(servicio.listar).toHaveBeenCalledTimes(2);
        expect(html().querySelector('[role="alert"]')).toBeNull();
        expect(html().textContent).toContain('ana@iglesia.test');
    });

    it('la fila del usuario autenticado muestra «Tú» y no tiene acciones', async () => {
        await crear();

        expect(filas()[0].textContent).toContain('Tú');
        expect(filas()[0].querySelectorAll('button').length).toBe(0);
        expect(boton('Editar ana@iglesia.test')).toBeDefined();
    });

    it('con solo usuarios.ver no hay «Nuevo usuario» ni acciones, y no pide los roles', async () => {
        permisos = new Set<Permiso>(['usuarios.ver']);
        await crear();

        expect(boton('Nuevo usuario')).toBeUndefined();
        expect(html().querySelectorAll('tbody button').length).toBe(0);
        expect(rolesServicio.listar).not.toHaveBeenCalled();
        expect(servicio.listar).toHaveBeenCalledTimes(1);
    });

    it('con usuarios.ver y roles.ver pide los roles para el filtro', async () => {
        permisos = new Set<Permiso>(['usuarios.ver', 'roles.ver']);
        await crear();

        expect(rolesServicio.listar).toHaveBeenCalledTimes(1);
    });

    it('un filtro nuevo vuelve a la primera página con el filtro aplicado', async () => {
        await crear();
        componente()['cambiarPagina']({ pagina: 2, tamano: 10 });
        const filtro = { correo: 'ana', activo: true, rolId: 10 };

        componente()['filtrar'](filtro);
        await fixture.whenStable();

        expect(servicio.listar.calls.mostRecent().args).toEqual([filtro, 0, 10]);
        expect(componente()['pagina']()).toBe(0);
        expect(rolesServicio.listar).toHaveBeenCalledTimes(1);
    });

    it('cambiar de página pide esa página al backend con el filtro actual', async () => {
        servicio.listar.and.returnValue(of(pagina([YO, ANA], 25)));
        await crear();
        componente()['filtrar']({ correo: 'a', activo: null, rolId: null });

        componente()['cambiarPagina']({ pagina: 2, tamano: 10 });
        await fixture.whenStable();

        expect(servicio.listar.calls.mostRecent().args).toEqual([{ correo: 'a', activo: null, rolId: null }, 2, 10]);
        expect(componente()['pagina']()).toBe(2);

        componente()['cambiarPagina']({ pagina: 2, tamano: 20 });
        await fixture.whenStable();

        expect(servicio.listar.calls.mostRecent().args).toEqual([{ correo: 'a', activo: null, rolId: null }, 0, 20]);
    });

    it('una petición nueva cancela la anterior', async () => {
        const lenta = new Subject<Page<UsuarioAdmin>>();
        await crear();
        servicio.listar.and.returnValues(lenta, of(pagina([ANA])));
        componente()['cambiarPagina']({ pagina: 1, tamano: 10 });

        componente()['filtrar']({ correo: 'ana', activo: null, rolId: null });
        await fixture.whenStable();

        expect(lenta.observed).toBeFalse();
        expect(filas().length).toBe(1);
    });

    describe('crear', () => {
        const datos = { correo: 'Nuevo@iglesia.test', contrasenaTemporal: 'Temporal1', rolIds: [10] };

        it('crea el usuario, cierra el diálogo, avisa y recarga la página actual', async () => {
            await crear();
            servicio.crear.and.returnValue(of({ ...ANA, id: 9, correo: 'nuevo@iglesia.test' }));
            const avisar = spyOn(toast(), 'add');
            boton('Nuevo usuario')!.click();

            componente()['crear'](datos);
            await fixture.whenStable();

            expect(servicio.crear).toHaveBeenCalledOnceWith(datos);
            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success', summary: 'Usuario creado' }));
            expect(servicio.listar).toHaveBeenCalledTimes(2);
        });

        it('un correo repetido muestra el 409 del backend y deja el diálogo abierto', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => conError(409, 'Ya existe un usuario con ese correo')));
            componente()['nuevo']();

            componente()['crear'](datos);
            await fixture.whenStable();

            expect(componente()['errorDialogo']()).toBe('Ya existe un usuario con ese correo');
            expect(componente()['dialogoVisible']()).toBeTrue();
            expect(componente()['guardando']()).toBeFalse();
        });

        it('un 5xx no repite en el diálogo el aviso del toast global', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => conError(500)));
            componente()['nuevo']();

            componente()['crear'](datos);

            expect(componente()['errorDialogo']()).toBeNull();
        });
    });

    describe('editar', () => {
        const cambios: EditarUsuario = { correo: 'ana@iglesia.test', rolIds: [10], activo: false };

        it('reenvía el miembro actual, actualiza la fila y avisa', async () => {
            await crear();
            servicio.actualizar.and.returnValue(of({ ...ANA, activo: false }));
            const avisar = spyOn(toast(), 'add');
            boton('Editar ana@iglesia.test')!.click();
            await fixture.whenStable();

            componente()['actualizar'](cambios);
            await fixture.whenStable();

            expect(servicio.actualizar).toHaveBeenCalledOnceWith(7, cambios, 55);
            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(filas()[1].textContent).toContain('Inactivo');
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success', summary: 'Usuario actualizado' }));
        });

        it('desactivar al último administrador muestra el 409 «sin administrador» y la fila sigue activa', async () => {
            await crear();
            const sinAdministrador = 'La iglesia debe conservar al menos un usuario activo que pueda administrar usuarios';
            servicio.actualizar.and.returnValue(throwError(() => conError(409, sinAdministrador)));
            componente()['editar'](ANA);

            componente()['actualizar'](cambios);
            await fixture.whenStable();

            expect(componente()['errorDialogo']()).toBe(sinAdministrador);
            expect(componente()['dialogoVisible']()).toBeTrue();
            expect(filas()[1].textContent).toContain('Activo');
        });

        it('un 404 muestra el detalle del backend en el diálogo', async () => {
            await crear();
            servicio.actualizar.and.returnValue(throwError(() => conError(404, 'Usuario no encontrado')));
            componente()['editar'](ANA);

            componente()['actualizar'](cambios);

            expect(componente()['errorDialogo']()).toBe('Usuario no encontrado');
        });
    });

    describe('restablecer contraseña', () => {
        it('envía la contraseña temporal, marca la fila y avisa', async () => {
            await crear();
            servicio.restablecerContrasena.and.returnValue(of(undefined));
            const avisar = spyOn(toast(), 'add');
            boton('Restablecer contraseña de ana@iglesia.test')!.click();
            await fixture.whenStable();

            componente()['restablecer']('TemporalNueva123');
            await fixture.whenStable();

            expect(servicio.restablecerContrasena).toHaveBeenCalledOnceWith(7, 'TemporalNueva123');
            expect(componente()['restablecerVisible']()).toBeFalse();
            expect(filas()[1].textContent).toContain('Debe cambiar contraseña');
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success', summary: 'Contraseña restablecida' }));
        });

        it('un 400 muestra el detalle del backend en el diálogo', async () => {
            await crear();
            servicio.restablecerContrasena.and.returnValue(throwError(() => conError(400, 'La contraseña temporal no es válida')));
            componente()['abrirRestablecer'](ANA);

            componente()['restablecer']('TemporalNueva123');
            await fixture.whenStable();

            expect(componente()['errorRestablecer']()).toBe('La contraseña temporal no es válida');
            expect(componente()['restablecerVisible']()).toBeTrue();
            expect(filas()[1].textContent).not.toContain('Debe cambiar contraseña');
        });
    });
});
