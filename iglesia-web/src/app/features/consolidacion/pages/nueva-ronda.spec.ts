import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';
import { rondaPrueba } from '../testing/datos-prueba';
import { NuevaRondaComponent } from './nueva-ronda';

describe('NuevaRondaComponent', () => {
    let fixture: ComponentFixture<NuevaRondaComponent>;
    let rondas: jasmine.SpyObj<RondasService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const boton = (texto: string): HTMLButtonElement => Array.from(html().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.trim().startsWith(texto))!;
    const componente = () => fixture.componentInstance as unknown as { paso(): number };

    beforeEach(async () => {
        rondas = jasmine.createSpyObj<RondasService>('RondasService', ['crear', 'actualizar', 'obtener', 'agregarLote', 'vistaPreviaLote', 'repartir', 'vistaPreviaReparto', 'iniciar']);
        const planificada = rondaPrueba(7, { estado: 'PLANIFICADA', personas: 0, pendientes: 0, enContacto: 0, cerrados: 0, sinAsignar: 0 });
        rondas.crear.and.returnValue(of(planificada));
        rondas.vistaPreviaLote.and.returnValue(of({ entrarian: 0, conCasoAbierto: 0, personas: [] }));
        rondas.obtener.and.returnValue(of(planificada));
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: RondasService, useValue: rondas },
                { provide: EquipoConsolidacionService, useValue: { listar: () => of([]) } },
                { provide: GruposService, useValue: { listar: () => of([]) } },
                { provide: SesionService, useValue: { tienePermiso: () => false } },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: signal({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } }
            ]
        });
        fixture = TestBed.createComponent(NuevaRondaComponent);
        await refrescar();
    });

    it('empieza en los datos con los demás pasos bloqueados', () => {
        expect(componente().paso()).toBe(1);
        expect(html().textContent).toContain('1. Datos de la ronda');
        (fixture.componentInstance as unknown as { irA(paso: number): void }).irA(3);
        expect(componente().paso()).toBe(1);
    });

    it('al guardar los datos crea la ronda planificada y pasa a las personas', async () => {
        html().querySelector('form')!.dispatchEvent(new Event('submit'));
        await refrescar();
        expect(rondas.crear).toHaveBeenCalled();
        expect(componente().paso()).toBe(2);
        expect(html().textContent).toContain('¿A quiénes vas a consolidar?');
    });

    it('si falla el guardado sigue en el paso con el error', async () => {
        rondas.crear.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Ya existe una ronda con ese nombre' } })));
        html().querySelector('form')!.dispatchEvent(new Event('submit'));
        await refrescar();
        expect(componente().paso()).toBe(1);
        expect(html().textContent).toContain('Ya existe una ronda con ese nombre');
    });

    it('sin personas no deja iniciar y «Guardar como planificada» abre la ronda', async () => {
        html().querySelector('form')!.dispatchEvent(new Event('submit'));
        await refrescar();
        (fixture.componentInstance as unknown as { irA(paso: number): void }).irA(4);
        await refrescar();
        expect(boton('Iniciar ronda').disabled).toBeTrue();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        boton('Guardar como planificada').click();
        expect(navegar).toHaveBeenCalledWith(['/consolidacion', 'rondas', 7]);
    });
});
