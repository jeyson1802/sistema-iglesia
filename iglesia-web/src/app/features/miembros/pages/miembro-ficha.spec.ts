import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { OLIVOS } from '@/app/features/grupos/testing/grupos-prueba';
import { NivelesService } from '@/app/features/niveles/services/niveles.service';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { ROLES_BASE } from '@/app/features/roles/testing/roles-prueba';
import { UsuariosService } from '@/app/features/usuarios/services/usuarios.service';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { GuardarMiembro } from '../models/miembro';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { MiembrosService } from '../services/miembros.service';
import { JUAN, RESUMEN_JUAN, RESUMEN_PASTOR } from '../testing/miembros-prueba';
import { MiembroFichaComponent } from './miembro-ficha';

describe('MiembroFichaComponent', () => {
    const datos = { nombres: 'Juan Carlos', apellidos: 'Pérez Torres' } as GuardarMiembro;
    const usuario = { id: 40, correo: 'juan@correo.test', miembroId: 22, activo: true, debeCambiarContrasena: true, roles: [], creadoEn: new Date() };
    let servicio: jasmine.SpyObj<MiembrosService>;
    let usuarios: jasmine.SpyObj<UsuariosService>;
    let permisos: Set<Permiso>;
    let parametros: BehaviorSubject<ParamMap>;
    let fixture: ComponentFixture<MiembroFichaComponent>;
    let confirmaciones: Confirmation[];

    const crear = async (id: string | null): Promise<void> => {
        parametros = new BehaviorSubject(convertToParamMap(id ? { id } : {}));
        await TestBed.configureTestingModule({
            imports: [MiembroFichaComponent],
            providers: [
                { provide: ActivatedRoute, useValue: { paramMap: parametros } },
                { provide: MiembrosService, useValue: servicio },
                { provide: UsuariosService, useValue: usuarios },
                { provide: NivelesService, useValue: { listar: () => of([]) } },
                { provide: GruposService, useValue: { listar: () => of([OLIVOS]) } },
                { provide: RolesService, useValue: { listar: () => of(ROLES_BASE) } },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: signal({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } },
                { provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Vida Nueva'), urlLogo: signal(null) } },
                {
                    provide: AsistenciasService,
                    useValue: { resumenMiembro: () => of({ reunionesQueCuentan: 0, asistidas: 0, porcentaje: null, porcentajeAnterior: null, faltasSeguidas: 0, ultimaAsistencia: null, alertas: [], semanas: [], ultimas: [] }) }
                },
                { provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(MiembroFichaComponent);
        const confirmacion = fixture.debugElement.injector.get(ConfirmationService);
        confirmaciones = [];
        spyOn(confirmacion, 'confirm').and.callFake((c: Confirmation) => {
            confirmaciones.push(c);
            return confirmacion;
        });
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta);
    const componente = () => fixture.componentInstance as unknown as Record<string, (...a: unknown[]) => unknown>;
    const senal = <T>(nombre: string): T => (fixture.componentInstance as unknown as Record<string, () => T>)[nombre]();

    beforeEach(() => {
        permisos = new Set<Permiso>(['miembros.ver', 'miembros.crear', 'miembros.editar', 'miembros.desactivar', 'usuarios.ver', 'usuarios.administrar']);
        servicio = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['obtener', 'cadena', 'posiblesDuplicados', 'crear', 'actualizar', 'cambiarEstado', 'designarPastorPrincipal', 'pastorPrincipal', 'liderados', 'codigoAsistencia']);
        servicio.codigoAsistencia.and.returnValue(of({ codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' }));
        servicio.obtener.and.returnValue(of(JUAN));
        servicio.cadena.and.returnValue(of([{ id: 10, nombreCompleto: 'Ana Ríos', nivel: null, esPastorPrincipal: false, activo: true }]));
        servicio.liderados.and.returnValue(of([{ id: 31, nombreCompleto: 'Rosa Quispe', nivel: 'Miembro', cantidadLiderados: 0 }]));
        usuarios = jasmine.createSpyObj<UsuariosService>('UsuariosService', ['usuarioDeMiembro', 'crearParaMiembro']);
        usuarios.usuarioDeMiembro.and.returnValue(of(null));
    });

    describe('alta', () => {
        it('muestra el formulario vacío sin progreso ni cadena', async () => {
            await crear(null);

            expect(html().querySelector('h1')?.textContent).toContain('Nuevo miembro');
            expect(html().querySelector('app-progreso-ficha')).toBeNull();
            expect(servicio.obtener).not.toHaveBeenCalled();
        });

        it('sin duplicados crea y abre la ficha nueva', async () => {
            servicio.posiblesDuplicados.and.returnValue(of([]));
            servicio.crear.and.returnValue(of(JUAN));
            await crear(null);
            const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

            componente()['guardar'](datos);

            expect(servicio.crear).toHaveBeenCalledWith(datos);
            expect(navegar).toHaveBeenCalledWith(['/miembros', 22], jasmine.objectContaining({ replaceUrl: true }));
        });

        it('con posibles duplicados pide confirmar antes de crear', async () => {
            servicio.posiblesDuplicados.and.returnValue(of([RESUMEN_JUAN]));
            servicio.crear.and.returnValue(of(JUAN));
            await crear(null);
            spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

            componente()['guardar'](datos);

            expect(servicio.crear).not.toHaveBeenCalled();
            expect(confirmaciones[0].message).toContain('«Juan Carlos Pérez Torres»');
            confirmaciones[0].accept?.();
            expect(servicio.crear).toHaveBeenCalled();
        });

        it('un ciclo en el líder se muestra junto al campo', async () => {
            servicio.posiblesDuplicados.and.returnValue(of([]));
            servicio.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Ciclo', errores: { liderId: 'Forma parte de su red' } } })));
            await crear(null);

            componente()['guardar'](datos);
            await fixture.whenStable();

            expect(html().textContent).toContain('Forma parte de su red');
            expect(senal('errorGuardar')).toBeNull();
        });
    });

    describe('ficha existente', () => {
        it('muestra la ficha, el progreso, el árbol de liderazgo y el botón de dar acceso', async () => {
            await crear('22');

            expect(servicio.obtener).toHaveBeenCalledWith(22);
            expect(html().querySelector('h1')?.textContent).toContain('Juan Carlos Pérez Torres');
            expect(html().textContent).toContain('Ficha completa');
            const arbol = html().querySelector('app-arbol-liderazgo') as HTMLElement;
            expect(arbol.textContent).toContain('Ana Ríos');
            expect(arbol.textContent).toContain('Rosa Quispe');
            expect(servicio.liderados).toHaveBeenCalledWith(22);
            expect(boton('Dar acceso al sistema')).toBeDefined();
        });

        it('el selector de grupo identifica cada grupo por su líder', async () => {
            await crear('22');

            expect(senal<{ id: number; nombre: string; activo: boolean }[]>('grupos')).toEqual([{ id: 4, nombre: 'Ana Ríos', activo: true }]);
        });

        it('si ya tiene acceso muestra el correo y no el botón', async () => {
            usuarios.usuarioDeMiembro.and.returnValue(of(usuario));
            await crear('22');

            expect(html().textContent).toContain('Acceso: juan@correo.test');
            expect(boton('Dar acceso al sistema')).toBeUndefined();
        });

        it('un 404 muestra que no se encontró', async () => {
            servicio.obtener.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
            await crear('99');

            expect(html().textContent).toContain('No se encontró el miembro o no tienes acceso a su ficha.');
        });

        it('sin permiso de editar es de solo lectura y sin acciones', async () => {
            permisos = new Set<Permiso>(['miembros.ver']);
            await crear('22');

            expect(html().textContent).toContain('Solo lectura.');
            expect(boton('Guardar')).toBeUndefined();
            expect(boton('Desactivar')).toBeUndefined();
            expect(boton('Designar pastor principal')).toBeUndefined();
            expect(usuarios.usuarioDeMiembro).not.toHaveBeenCalled();
        });

        it('al guardar cambios actualiza y recarga la cadena', async () => {
            servicio.actualizar.and.returnValue(of({ ...JUAN, telefono: '111' }));
            await crear('22');

            componente()['guardar'](datos);

            expect(servicio.actualizar).toHaveBeenCalledWith(22, datos);
            expect(servicio.cadena).toHaveBeenCalledTimes(2);
        });

        it('designar pastor nombra en el aviso a quién reemplaza', async () => {
            servicio.pastorPrincipal.and.returnValue(of(RESUMEN_PASTOR));
            servicio.designarPastorPrincipal.and.returnValue(of({ ...JUAN, esPastorPrincipal: true, lider: null }));
            await crear('22');

            boton('Designar pastor principal')?.click();
            expect(confirmaciones[0].message).toContain('reemplazará a «Pedro Gómez»');
            confirmaciones[0].accept?.();
            await fixture.whenStable();

            expect(servicio.designarPastorPrincipal).toHaveBeenCalledWith(22);
            expect(html().textContent).toContain('Pastor principal');
        });

        it('desactivar pide confirmación y muestra el motivo del rechazo', async () => {
            servicio.cambiarEstado.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Lidera a miembros activos' } })));
            await crear('22');

            boton('Desactivar')?.click();
            confirmaciones[0].accept?.();
            await fixture.whenStable();

            expect(servicio.cambiarEstado).toHaveBeenCalledWith(22, false);
            expect(html().textContent).toContain('Lidera a miembros activos');
        });

        it('dar acceso ofrece Líder y Miembro y, al crear, muestra el acceso', async () => {
            usuarios.crearParaMiembro.and.returnValue(of(usuario));
            await crear('22');

            boton('Dar acceso al sistema')?.click();
            await fixture.whenStable();
            expect(senal<{ nombre: string }[]>('rolesAcceso').map(({ nombre }) => nombre)).toEqual(['Líder', 'Miembro']);

            componente()['darAcceso']({ correo: 'juan@correo.test', contrasenaTemporal: 'Temporal123', rolIds: [3] });
            await fixture.whenStable();

            expect(usuarios.crearParaMiembro).toHaveBeenCalledWith(22, { correo: 'juan@correo.test', contrasenaTemporal: 'Temporal123', rolIds: [3] });
            expect(html().textContent).toContain('Acceso: juan@correo.test');
            expect(boton('Dar acceso al sistema')).toBeUndefined();
        });

        it('al cambiar de miembro en la ruta carga el nuevo', async () => {
            await crear('22');

            parametros.next(convertToParamMap({ id: '10' }));

            expect(servicio.obtener).toHaveBeenCalledWith(10);
        });
    });
});
