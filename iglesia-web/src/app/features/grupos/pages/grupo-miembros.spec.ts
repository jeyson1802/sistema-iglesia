import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GruposService } from '../services/grupos.service';
import { BETANIA, LUIS_EN_OTRO_GRUPO, OLIVOS, ROSA, TITO_SIN_GRUPO } from '../testing/grupos-prueba';
import { GrupoMiembrosComponent } from './grupo-miembros';

describe('GrupoMiembrosComponent', () => {
    let servicio: jasmine.SpyObj<GruposService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<GrupoMiembrosComponent>;
    let confirmaciones: Confirmation[];

    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...a: unknown[]) => void>;
    const senal = <T>(nombre: string): T => (fixture.componentInstance as unknown as Record<string, () => T>)[nombre]();
    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [GrupoMiembrosComponent],
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '4' })) } },
                { provide: GruposService, useValue: servicio },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: signal({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GrupoMiembrosComponent);
        const confirmacion = fixture.debugElement.injector.get(ConfirmationService);
        confirmaciones = [];
        spyOn(confirmacion, 'confirm').and.callFake((c: Confirmation) => {
            confirmaciones.push(c);
            return confirmacion;
        });
        await fixture.whenStable();
    };

    beforeEach(() => {
        permisos = new Set<Permiso>(['grupos.ver', 'grupos.administrar']);
        servicio = jasmine.createSpyObj<GruposService>('GruposService', ['obtener', 'integrantes', 'candidatos', 'agregarIntegrante', 'quitarIntegrante']);
        servicio.obtener.and.returnValue(of(OLIVOS));
        servicio.integrantes.and.returnValue(of([ROSA]));
        servicio.candidatos.and.returnValue(of([LUIS_EN_OTRO_GRUPO, TITO_SIN_GRUPO]));
        servicio.agregarIntegrante.and.callFake((_, id) => of(id === TITO_SIN_GRUPO.id ? TITO_SIN_GRUPO : LUIS_EN_OTRO_GRUPO));
        servicio.quitarIntegrante.and.returnValue(of(undefined));
    });

    it('titula el grupo por su líder y carga integrantes y candidatos', async () => {
        await crear();

        expect(html().querySelector('h1')?.textContent).toContain('Célula de Ana Ríos');
        expect(html().textContent).toContain('Jueves 19:30');
        expect(servicio.integrantes).toHaveBeenCalledWith(4);
        expect(servicio.candidatos).toHaveBeenCalledWith(4, '');
        expect(html().textContent).toContain('Rosa Quispe');
        expect(html().textContent).toContain('Tito Vera');
    });

    it('agrega directamente a quien no tiene grupo y lo saca de disponibles', async () => {
        await crear();

        componente()['agregar'](TITO_SIN_GRUPO);
        await fixture.whenStable();

        expect(confirmaciones).toEqual([]);
        expect(servicio.agregarIntegrante).toHaveBeenCalledWith(4, 33);
        expect(senal<{ id: number }[]>('integrantes').map(({ id }) => id)).toEqual([31, 33]);
        expect(senal<{ id: number }[]>('candidatos').map(({ id }) => id)).toEqual([32]);
        expect(html().textContent).toContain('Tito Vera se agregó al grupo.');
    });

    it('mover a alguien de otro grupo pide confirmación nombrando ese grupo', async () => {
        await crear();

        componente()['agregar'](LUIS_EN_OTRO_GRUPO);

        expect(servicio.agregarIntegrante).not.toHaveBeenCalled();
        expect(confirmaciones[0].header).toBe('Ya está en otro grupo');
        expect(confirmaciones[0].message).toBe('«Luis Paz» ya está en célula de Beto Soto. ¿Deseas moverlo a célula de Ana Ríos? Dejará su grupo actual.');
        confirmaciones[0].accept?.();
        expect(servicio.agregarIntegrante).toHaveBeenCalledWith(4, 32);
    });

    it('quitar saca al integrante y refresca los disponibles', async () => {
        await crear();
        servicio.candidatos.calls.reset();

        componente()['quitar'](ROSA);
        await fixture.whenStable();

        expect(servicio.quitarIntegrante).toHaveBeenCalledWith(4, 31);
        expect(senal<unknown[]>('integrantes')).toEqual([]);
        expect(servicio.candidatos).toHaveBeenCalledWith(4, '');
    });

    it('un rechazo del backend se muestra arriba de las listas', async () => {
        servicio.agregarIntegrante.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'El miembro está inactivo' } })));
        await crear();

        componente()['agregar'](TITO_SIN_GRUPO);
        await fixture.whenStable();

        expect(html().textContent).toContain('El miembro está inactivo');
    });

    it('sin permiso de administrar es de solo lectura y no busca candidatos', async () => {
        permisos = new Set<Permiso>(['grupos.ver']);
        await crear();

        expect(senal('editable')).toBeFalse();
        expect(servicio.candidatos).not.toHaveBeenCalled();
        expect(html().querySelector('button[aria-label="Quitar a Rosa Quispe del grupo"]')).toBeNull();
    });

    it('un grupo inactivo no se edita y avisa cómo reactivarlo', async () => {
        servicio.obtener.and.returnValue(of(BETANIA));
        await crear();

        expect(senal('editable')).toBeFalse();
        expect(html().textContent).toContain('El grupo está desactivado');
    });

    it('si el grupo no carga ofrece reintentar', async () => {
        servicio.obtener.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Grupo no encontrado' } })));
        await crear();

        expect(html().textContent).toContain('Grupo no encontrado');
        servicio.obtener.and.returnValue(of(OLIVOS));
        componente()['cargar']();
        await fixture.whenStable();
        expect(html().querySelector('h1')?.textContent).toContain('Célula de Ana Ríos');
    });
});
