import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { Nivel } from '../models/nivel';
import { NivelesService } from '../services/niveles.service';
import { NivelesListaComponent } from './niveles-lista';

describe('NivelesListaComponent', () => {
    const pastor: Nivel = { id: 1, nombre: 'Pastor', orden: 1, activo: true };
    const diacono: Nivel = { id: 2, nombre: 'Diácono', orden: 2, activo: true };
    let servicio: jasmine.SpyObj<NivelesService>;
    let fixture: ComponentFixture<NivelesListaComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [NivelesListaComponent], providers: [{ provide: NivelesService, useValue: servicio }] }).compileComponents();
        fixture = TestBed.createComponent(NivelesListaComponent);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta || b.getAttribute('aria-label') === etiqueta);
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;

    beforeEach(() => {
        servicio = jasmine.createSpyObj<NivelesService>('NivelesService', ['listar', 'crear', 'renombrar', 'ordenar', 'cambiarEstado']);
        servicio.listar.and.returnValue(of([pastor, diacono]));
    });

    it('carga y muestra los niveles', async () => {
        await crear();

        expect(html().querySelectorAll('tbody tr').length).toBe(2);
    });

    it('muestra la carga y el error con reintento', async () => {
        servicio.listar.and.returnValue(new Subject<Nivel[]>());
        await crear();
        expect(html().querySelector('[aria-label="Cargando niveles"]')).not.toBeNull();

        servicio.listar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        componente()['cargar']();
        await fixture.whenStable();
        expect(html().textContent).toContain('No se pudieron cargar los niveles.');
        expect(boton('Reintentar')).toBeDefined();
    });

    it('bajar envía el orden completo con el nivel intercambiado', async () => {
        servicio.ordenar.and.returnValue(
            of([
                { ...diacono, orden: 1 },
                { ...pastor, orden: 2 }
            ])
        );
        await crear();

        boton('Bajar Pastor')?.click();
        await fixture.whenStable();

        expect(servicio.ordenar).toHaveBeenCalledWith([2, 1]);
        expect(html().querySelector('tbody tr')?.textContent).toContain('Diácono');
    });

    it('si el orden falla lo avisa y conserva el anterior', async () => {
        servicio.ordenar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'Faltan niveles' } })));
        await crear();

        boton('Subir Diácono')?.click();
        await fixture.whenStable();

        expect(html().textContent).toContain('Faltan niveles');
        expect(html().querySelector('tbody tr')?.textContent).toContain('Pastor');
    });

    it('crea un nivel y lo agrega al final', async () => {
        servicio.crear.and.returnValue(of({ id: 3, nombre: 'Líder', orden: 3, activo: true }));
        await crear();

        componente()['nuevo']();
        componente()['guardar']('Líder');
        await fixture.whenStable();

        expect(servicio.crear).toHaveBeenCalledWith('Líder');
        expect(html().querySelectorAll('tbody tr').length).toBe(3);
    });

    it('un nombre repetido se muestra en el diálogo', async () => {
        servicio.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Ya existe un nivel con ese nombre' } })));
        await crear();

        componente()['nuevo']();
        componente()['guardar']('Pastor');
        await fixture.whenStable();

        expect((fixture.componentInstance as unknown as { errorDialogo: () => string }).errorDialogo()).toBe('Ya existe un nivel con ese nombre');
    });

    it('desactivar pide confirmación', async () => {
        servicio.cambiarEstado.and.returnValue(of({ ...diacono, activo: false }));
        await crear();
        const confirmacion = fixture.debugElement.injector.get(ConfirmationService);
        let pedida: Confirmation | undefined;
        spyOn(confirmacion, 'confirm').and.callFake((c: Confirmation) => {
            pedida = c;
            return confirmacion;
        });

        boton('Desactivar Diácono')?.click();
        expect(servicio.cambiarEstado).not.toHaveBeenCalled();
        pedida?.accept?.();
        await fixture.whenStable();

        expect(servicio.cambiarEstado).toHaveBeenCalledWith(2, false);
        expect(html().textContent).toContain('Inactivo');
    });
});
