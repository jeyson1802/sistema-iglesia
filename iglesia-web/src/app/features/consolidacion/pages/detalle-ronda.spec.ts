import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { Page } from '@/app/core/models/page';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { CasoResumen, Ronda } from '../models/consolidacion';
import { EquipoConsolidacionService } from '../services/equipo-consolidacion.service';
import { RondasService } from '../services/rondas.service';
import { casoPrueba, consolidadorPrueba, rondaPrueba } from '../testing/datos-prueba';
import { DetalleRondaComponent } from './detalle-ronda';

const pagina = (casos: CasoResumen[]): Page<CasoResumen> => ({ content: casos, page: { size: 10, number: 0, totalElements: casos.length, totalPages: 1 } });

describe('DetalleRondaComponent', () => {
    let fixture: ComponentFixture<DetalleRondaComponent>;
    let rondas: jasmine.SpyObj<RondasService>;
    let permisos: string[];
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const boton = (texto: string): HTMLButtonElement | undefined => Array.from(document.body.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.trim() === texto);
    const crear = async (ronda: Ronda): Promise<void> => {
        rondas.obtener.and.returnValue(of(ronda));
        fixture = TestBed.createComponent(DetalleRondaComponent);
        await refrescar();
    };

    beforeEach(() => {
        permisos = ['consolidacion.administrar'];
        rondas = jasmine.createSpyObj<RondasService>('RondasService', ['obtener', 'casos', 'avance', 'iniciar', 'cerrar', 'repartir', 'quitarCaso', 'vistaPreviaReparto']);
        rondas.casos.and.returnValue(of(pagina([casoPrueba(1), casoPrueba(2, { consolidador: null })])));
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
                { provide: RondasService, useValue: rondas },
                { provide: EquipoConsolidacionService, useValue: { listar: () => of([consolidadorPrueba(25, { nombreCompleto: 'Luis Rojas', casosAbiertos: 0 })]) } },
                { provide: GruposService, useValue: { listar: () => of([]) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: string) => permisos.includes(permiso) } },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: signal({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } }
            ]
        });
    });

    afterEach(() => fixture.destroy());

    it('muestra la ronda con sus personas y el aviso de las que faltan asignar', async () => {
        await crear(rondaPrueba(7, { nombre: 'Noviembre', sinAsignar: 1 }));
        expect(rondas.casos).toHaveBeenCalledWith(7, jasmine.anything(), 0, 10);
        expect(html().querySelector('h1')?.textContent).toContain('Noviembre');
        expect(html().textContent).toContain('1 persona sin asignar');
        expect(boton('Repartir')).toBeDefined();
    });

    it('la acción principal de una ronda planificada y repartida es iniciarla', async () => {
        rondas.iniciar.and.returnValue(of(rondaPrueba(7, { estado: 'EN_CURSO' })));
        await crear(rondaPrueba(7, { estado: 'PLANIFICADA' }));
        boton('Iniciar ronda')!.click();
        await refrescar();
        expect(rondas.iniciar).toHaveBeenCalledWith(7);
        expect(html().textContent).toContain('En curso');
    });

    it('al seleccionar personas aparece la barra de acciones y reparte en partes iguales', async () => {
        rondas.repartir.and.returnValue(of(rondaPrueba(7)));
        await crear(rondaPrueba(7));
        (fixture.componentInstance as unknown as { seleccion: { set(casos: CasoResumen[]): void } }).seleccion.set([casoPrueba(1), casoPrueba(2)]);
        await refrescar();
        expect(document.body.textContent).toContain('2 seleccionadas');
        boton('Repartir en partes iguales')!.click();
        await refrescar();
        expect(rondas.repartir).toHaveBeenCalledWith(7, {
            consolidadorIds: [25],
            soloSinAsignar: false,
            asignaciones: [
                { casoId: 1, consolidadorId: 25 },
                { casoId: 2, consolidadorId: 25 }
            ]
        });
    });

    it('cerrar la ronda pide confirmación y avisa los casos abiertos', async () => {
        await crear(rondaPrueba(7, { pendientes: 2, enContacto: 1 }));
        const confirmacion = fixture.debugElement.injector.get(ConfirmationService);
        let pedido: Confirmation | undefined;
        spyOn(confirmacion, 'confirm').and.callFake((opciones: Confirmation) => {
            pedido = opciones;
            return confirmacion;
        });
        const menu = (fixture.componentInstance as unknown as { menuMas(): { label?: string; command?: () => void }[] }).menuMas();
        menu.find(({ label }) => label === 'Cerrar ronda')!.command!();
        expect(pedido?.message).toContain('Quedan 3 casos abiertos');
    });

    it('quien solo ve no tiene acciones ni casillas', async () => {
        permisos = ['consolidacion.ver'];
        await crear(rondaPrueba(7, { estado: 'PLANIFICADA', sinAsignar: 2 }));
        expect(boton('Iniciar ronda')).toBeUndefined();
        expect(boton('Más')).toBeUndefined();
        expect(html().querySelector('p-tablecheckbox')).toBeNull();
    });

    it('abre el caso al hacer clic en una persona', async () => {
        await crear(rondaPrueba(7));
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        (html().querySelector('app-tabla-casos p-table tbody tr td:nth-child(2)') as HTMLElement).click();
        expect(navegar).toHaveBeenCalledWith(['/consolidacion', 'casos', 1]);
    });
});
