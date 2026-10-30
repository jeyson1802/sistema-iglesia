import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RondasService } from '../../services/rondas.service';
import { consolidadorPrueba } from '../../testing/datos-prueba';
import { RepartoComponent } from './reparto';

describe('RepartoComponent', () => {
    let fixture: ComponentFixture<RepartoComponent>;
    let rondas: jasmine.SpyObj<RondasService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        jasmine.clock().install();
        jasmine.clock().mockDate(new Date(2026, 10, 10, 12));
        rondas = jasmine.createSpyObj<RondasService>('RondasService', ['vistaPreviaReparto']);
        rondas.vistaPreviaReparto.and.returnValue(
            of({
                asignaciones: [
                    { casoId: 1, persona: { miembroId: 40, nombreCompleto: 'Ana Quispe' }, consolidadorId: 25 },
                    { casoId: 2, persona: { miembroId: 41, nombreCompleto: 'Juan Pérez' }, consolidadorId: 31 }
                ],
                cargas: [
                    { consolidadorId: 25, nombreCompleto: 'Luis Rojas', cargaActual: 2, nuevos: 1 },
                    { consolidadorId: 31, nombreCompleto: 'Rosa Díaz', cargaActual: 0, nuevos: 1 }
                ]
            })
        );
        TestBed.configureTestingModule({ providers: [{ provide: RondasService, useValue: rondas }] });
        fixture = TestBed.createComponent(RepartoComponent);
        fixture.componentRef.setInput('rondaId', 7);
        fixture.componentRef.setInput('equipo', [consolidadorPrueba(25, { nombreCompleto: 'Luis Rojas' }), consolidadorPrueba(31, { nombreCompleto: 'Rosa Díaz' }), consolidadorPrueba(40, { activo: false })]);
        await refrescar();
        jasmine.clock().tick(300);
        await refrescar();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('marca a todos los activos y muestra la propuesta antes de aplicarla', () => {
        expect(rondas.vistaPreviaReparto).toHaveBeenCalledWith(7, { consolidadorIds: [25, 31], soloSinAsignar: true });
        const filas = Array.from(html().querySelectorAll('section[aria-labelledby="reparto-equipo"] tbody tr')).map((fila) => fila.textContent!.replace(/\s+/g, ' ').trim());
        expect(filas.length).toBe(2);
        expect(filas[0]).toContain('Luis Rojas');
        expect(filas[0]).toContain('+1');
        expect(html().textContent).toContain('2 de 2 consolidadores elegidos');
        expect(fixture.componentInstance.solicitud()).toEqual({ consolidadorIds: [25, 31], soloSinAsignar: true });
    });

    it('al mover a alguien envía el reparto exacto', () => {
        (fixture.componentInstance as unknown as { mover(casoId: number, consolidadorId: number): void }).mover(2, 25);
        expect(fixture.componentInstance.solicitud()?.asignaciones).toEqual([
            { casoId: 1, consolidadorId: 25 },
            { casoId: 2, consolidadorId: 25 }
        ]);
    });

    it('sin equipo explica qué hacer', async () => {
        fixture.componentRef.setInput('equipo', []);
        await refrescar();
        expect(html().textContent).toContain('Tu equipo de consolidación aún no tiene integrantes.');
    });

    it('busca en el equipo sin distinguir tildes y marca solo a los que se ven', async () => {
        const componente = fixture.componentInstance as unknown as { buscarEquipo: { set(v: string): void }; alternarVisibles(m: boolean): void; elegidos(): ReadonlySet<number> };
        componente.buscarEquipo.set('diaz');
        await refrescar();
        expect(html().querySelectorAll('section[aria-labelledby="reparto-equipo"] tbody tr').length).toBe(1);
        componente.alternarVisibles(false);
        expect([...componente.elegidos()]).toEqual([25]);
    });

    it('mueve en bloque a las seleccionadas y volver a la propuesta deshace el ajuste', () => {
        const componente = fixture.componentInstance as unknown as {
            alternarPersona(id: number, m: boolean): void;
            destinoBloque: { set(v: number): void };
            moverSeleccionadas(): void;
            mover(casoId: number, consolidadorId: number): void;
            movidas(): number;
        };
        componente.alternarPersona(1, true);
        componente.alternarPersona(2, true);
        componente.destinoBloque.set(31);
        componente.moverSeleccionadas();
        expect(componente.movidas()).toBe(1);
        expect(fixture.componentInstance.solicitud()?.asignaciones).toEqual([
            { casoId: 1, consolidadorId: 31 },
            { casoId: 2, consolidadorId: 31 }
        ]);
        componente.mover(1, 25);
        expect(componente.movidas()).toBe(0);
        expect(fixture.componentInstance.solicitud()?.asignaciones).toBeUndefined();
    });

    it('ordena las personas por consolidador y, dentro, por nombre', () => {
        const componente = fixture.componentInstance as unknown as { mover(casoId: number, consolidadorId: number): void; filasPersonas(): { nombre: string }[] };
        expect(componente.filasPersonas().map(({ nombre }) => nombre)).toEqual(['Ana Quispe', 'Juan Pérez']);
        componente.mover(2, 25);
        componente.mover(1, 31);
        expect(componente.filasPersonas().map(({ nombre }) => nombre)).toEqual(['Juan Pérez', 'Ana Quispe']);
    });

    it('muestra siempre el paginador del equipo', () => {
        expect(html().querySelector('section[aria-labelledby="reparto-equipo"] p-paginator, section[aria-labelledby="reparto-equipo"] .p-paginator')).not.toBeNull();
    });
});
