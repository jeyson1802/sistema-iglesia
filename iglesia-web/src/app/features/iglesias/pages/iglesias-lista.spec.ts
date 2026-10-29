import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { UsuarioAdmin } from '@/app/features/usuarios/models/usuario-admin';
import { AdministradoresIglesiaService } from '../services/administradores-iglesia.service';
import { IglesiasListaComponent } from './iglesias-lista';

describe('IglesiasListaComponent', () => {
    let servicio: jasmine.SpyObj<IglesiasService>;
    let administradores: jasmine.SpyObj<AdministradoresIglesiaService>;
    let sesion: { tienePermiso: (permiso: Permiso) => boolean; actuandoEnIglesia: ReturnType<typeof signal<boolean>>; usuarioActual: () => { iglesiaId: number | null }; entrarAIglesia: jasmine.Spy };
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<IglesiasListaComponent>;

    const CENTRAL: Iglesia = { id: 1, nombre: 'Iglesia Central', creadaEn: new Date(2026, 0, 1) };
    const NORTE: Iglesia = { id: 2, nombre: 'Iglesia del Norte', creadaEn: new Date(2026, 9, 1) };
    const ADMINISTRADOR: UsuarioAdmin = { id: 30, correo: 'pastor@norte.org', miembroId: null, activo: true, debeCambiarContrasena: true, roles: [], creadoEn: new Date(2026, 9, 1) };
    const ALTA = { nombre: 'Iglesia del Norte', administrador: { correo: 'Pastor@norte.org', contrasenaTemporal: 'Temporal123' } };

    const conError = (status: number, detail?: string): HttpErrorResponse => new HttpErrorResponse({ status, error: detail ? { detail } : null });

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [IglesiasListaComponent],
            providers: [provideRouter([]), { provide: IglesiasService, useValue: servicio }, { provide: AdministradoresIglesiaService, useValue: administradores }, { provide: SesionService, useValue: sesion }]
        }).compileComponents();
        fixture = TestBed.createComponent(IglesiasListaComponent);
        await fixture.whenStable();
    };

    beforeEach(() => {
        permisos = new Set<Permiso>(['iglesias.ver', 'iglesias.administrar']);
        servicio = jasmine.createSpyObj<IglesiasService>('IglesiasService', ['listar', 'crear', 'renombrar']);
        servicio.listar.and.returnValue(of([CENTRAL, NORTE]));
        administradores = jasmine.createSpyObj<AdministradoresIglesiaService>('AdministradoresIglesiaService', ['crear']);
        sesion = {
            tienePermiso: (permiso: Permiso) => permisos.has(permiso),
            actuandoEnIglesia: signal(false),
            usuarioActual: () => ({ iglesiaId: null }),
            entrarAIglesia: jasmine.createSpy('entrarAIglesia')
        };
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta || b.getAttribute('aria-label') === etiqueta);
    const componente = () => fixture.componentInstance;
    const toast = (): MessageService => fixture.debugElement.injector.get(MessageService);

    it('carga el catálogo de iglesias', async () => {
        await crear();

        expect(servicio.listar).toHaveBeenCalledTimes(1);
        expect(filas().length).toBe(2);
        expect(html().textContent).toContain('Iglesia del Norte');
    });

    it('mientras carga muestra el esqueleto', async () => {
        servicio.listar.and.returnValue(new Subject<Iglesia[]>());
        await crear();

        expect(html().querySelectorAll('tbody p-skeleton').length).toBeGreaterThan(0);
    });

    it('si falla la carga muestra el error y permite reintentar', async () => {
        servicio.listar.and.returnValues(
            throwError(() => conError(500)),
            of([CENTRAL])
        );
        await crear();

        expect(html().querySelector('[role="alert"]')?.textContent).toContain('No se pudieron cargar las iglesias.');

        boton('Reintentar')!.click();
        await fixture.whenStable();

        expect(servicio.listar).toHaveBeenCalledTimes(2);
        expect(html().textContent).toContain('Iglesia Central');
    });

    it('con solo iglesias.ver no hay «Nueva iglesia» ni acciones', async () => {
        permisos = new Set<Permiso>(['iglesias.ver']);
        await crear();

        expect(boton('Nueva iglesia')).toBeUndefined();
        expect(html().querySelectorAll('tbody button').length).toBe(0);
    });

    it('dentro de una iglesia la marca como «Actual»', async () => {
        sesion.actuandoEnIglesia.set(true);
        sesion.usuarioActual = () => ({ iglesiaId: 2 });
        await crear();

        expect(filas()[1].textContent).toContain('Actual');
        expect(filas()[0].textContent).not.toContain('Actual');
    });

    describe('alta', () => {
        it('crea la iglesia y luego su administrador, cierra el diálogo, avisa y recarga', async () => {
            await crear();
            servicio.crear.and.returnValue(of(NORTE));
            administradores.crear.and.returnValue(of(ADMINISTRADOR));
            const avisar = spyOn(toast(), 'add');
            boton('Nueva iglesia')!.click();

            componente()['crear'](ALTA);
            await fixture.whenStable();

            expect(servicio.crear).toHaveBeenCalledOnceWith('Iglesia del Norte');
            expect(administradores.crear).toHaveBeenCalledOnceWith(2, ALTA.administrador);
            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(componente()['guardando']()).toBeFalse();
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success', summary: 'Iglesia creada' }));
            expect(servicio.listar).toHaveBeenCalledTimes(2);
        });

        it('si falla el administrador, cierra el diálogo, recarga y explica cómo completarla', async () => {
            await crear();
            servicio.crear.and.returnValue(of(NORTE));
            administradores.crear.and.returnValue(throwError(() => conError(409, 'Ya existe un usuario con ese correo')));
            const avisar = spyOn(toast(), 'add');
            componente()['nueva']();

            componente()['crear'](ALTA);
            await fixture.whenStable();

            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(servicio.listar).toHaveBeenCalledTimes(2);
            expect(avisar).toHaveBeenCalledWith(
                jasmine.objectContaining({
                    severity: 'warn',
                    sticky: true,
                    detail: 'La iglesia se creó, pero no su administrador: Ya existe un usuario con ese correo. Usa "Crear administrador".'
                })
            );
        });

        it('si el administrador falla con un 5xx, el aviso no repite el detalle técnico', async () => {
            await crear();
            servicio.crear.and.returnValue(of(NORTE));
            administradores.crear.and.returnValue(throwError(() => conError(500)));
            const avisar = spyOn(toast(), 'add');

            componente()['crear'](ALTA);

            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ detail: 'La iglesia se creó, pero no su administrador: ocurrió un error inesperado. Usa "Crear administrador".' }));
        });

        it('un nombre repetido muestra el 409 dentro del diálogo, que sigue abierto, y no crea el administrador', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => conError(409, 'Ya existe una iglesia con ese nombre')));
            componente()['nueva']();

            componente()['crear'](ALTA);
            await fixture.whenStable();

            expect(componente()['errorDialogo']()).toBe('Ya existe una iglesia con ese nombre');
            expect(componente()['dialogoVisible']()).toBeTrue();
            expect(componente()['guardando']()).toBeFalse();
            expect(administradores.crear).not.toHaveBeenCalled();
        });

        it('un 5xx al crear la iglesia no repite en el diálogo el aviso del toast global', async () => {
            await crear();
            servicio.crear.and.returnValue(throwError(() => conError(500)));
            componente()['nueva']();

            componente()['crear'](ALTA);

            expect(componente()['errorDialogo']()).toBeNull();
        });
    });

    describe('renombrar', () => {
        it('renombra la iglesia, actualiza la fila y avisa', async () => {
            await crear();
            servicio.renombrar.and.returnValue(of({ ...CENTRAL, nombre: 'Iglesia Central de Lima' }));
            const avisar = spyOn(toast(), 'add');
            boton('Editar nombre de Iglesia Central')!.click();

            componente()['renombrar']('Iglesia Central de Lima');
            await fixture.whenStable();

            expect(servicio.renombrar).toHaveBeenCalledOnceWith(1, 'Iglesia Central de Lima');
            expect(componente()['dialogoVisible']()).toBeFalse();
            expect(filas()[0].textContent).toContain('Iglesia Central de Lima');
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
        });

        it('un nombre repetido muestra el 409 en el diálogo', async () => {
            await crear();
            servicio.renombrar.and.returnValue(throwError(() => conError(409, 'Ya existe una iglesia con ese nombre')));
            componente()['editar'](CENTRAL);

            componente()['renombrar']('Iglesia del Norte');

            expect(componente()['errorDialogo']()).toBe('Ya existe una iglesia con ese nombre');
            expect(componente()['dialogoVisible']()).toBeTrue();
        });
    });

    describe('crear administrador', () => {
        it('crea el administrador en la iglesia elegida, cierra el diálogo y avisa', async () => {
            await crear();
            administradores.crear.and.returnValue(of(ADMINISTRADOR));
            const avisar = spyOn(toast(), 'add');
            boton('Crear administrador de Iglesia del Norte')!.click();

            componente()['crearAdministrador']({ correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' });
            await fixture.whenStable();

            expect(administradores.crear).toHaveBeenCalledOnceWith(2, { correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' });
            expect(componente()['administradorVisible']()).toBeFalse();
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success', summary: 'Administrador creado' }));
        });

        it('un correo repetido muestra el 409 en el diálogo', async () => {
            await crear();
            administradores.crear.and.returnValue(throwError(() => conError(409, 'Ya existe un usuario con ese correo')));
            componente()['abrirAdministrador'](NORTE);

            componente()['crearAdministrador']({ correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' });

            expect(componente()['errorAdministrador']()).toBe('Ya existe un usuario con ese correo');
            expect(componente()['administradorVisible']()).toBeTrue();
        });
    });

    describe('entrar', () => {
        it('entra a la iglesia y navega a /usuarios', async () => {
            await crear();
            sesion.entrarAIglesia.and.returnValue(of(undefined));
            const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);

            boton('Entrar a Iglesia del Norte')!.click();
            await fixture.whenStable();

            expect(sesion.entrarAIglesia).toHaveBeenCalledOnceWith(2);
            expect(navegar).toHaveBeenCalledOnceWith('/usuarios');
            expect(componente()['entrando']()).toBeFalse();
        });

        it('si no puede entrar avisa el motivo y se queda en el catálogo', async () => {
            await crear();
            sesion.entrarAIglesia.and.returnValue(throwError(() => conError(403, 'No tiene permiso para esta operación')));
            const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl');
            const avisar = spyOn(toast(), 'add');

            componente()['entrar'](NORTE);

            expect(navegar).not.toHaveBeenCalled();
            expect(avisar).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error', detail: 'No tiene permiso para esta operación' }));
            expect(componente()['entrando']()).toBeFalse();
        });
    });
});
