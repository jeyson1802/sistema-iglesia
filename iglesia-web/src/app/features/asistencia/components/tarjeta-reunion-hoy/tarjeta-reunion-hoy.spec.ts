import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReunionHoy } from '../../models/asistencia';
import { TarjetaReunionHoyComponent } from './tarjeta-reunion-hoy';

describe('TarjetaReunionHoyComponent', () => {
    const ABIERTA: ReunionHoy = { id: 50, tipo: 'CULTO', nombre: 'Culto dominical', hora: '10:00:00', cultoProgramadoId: 1, grupoId: null, totalAsistentes: 128 };
    let fixture: ComponentFixture<TarjetaReunionHoyComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TarjetaReunionHoyComponent] }).compileComponents();
        fixture = TestBed.createComponent(TarjetaReunionHoyComponent);
    });

    const boton = (): HTMLButtonElement => fixture.nativeElement.querySelector('button');

    it('muestra tipo, hora, nombre y presentes, y toda la tarjeta toma asistencia', () => {
        const tomadas: ReunionHoy[] = [];
        fixture.componentRef.setInput('reunion', ABIERTA);
        fixture.componentInstance.tomar.subscribe((reunion) => tomadas.push(reunion));
        fixture.detectChanges();

        expect(boton().textContent).toContain('Culto');
        expect(boton().textContent).toContain('10:00');
        expect(boton().textContent).toContain('128');
        expect(boton().getAttribute('aria-label')).toBe('Tomar asistencia: Culto dominical a las 10:00, 128 presentes');
        boton().click();
        expect(tomadas).toEqual([ABIERTA]);
    });

    it('una reunión sin abrir aún no tiene asistencia y mientras se abre se deshabilita', () => {
        fixture.componentRef.setInput('reunion', { ...ABIERTA, id: null, totalAsistentes: 0 });
        fixture.componentRef.setInput('abriendo', true);
        fixture.detectChanges();

        expect(boton().textContent).toContain('Aún sin asistencia');
        expect(boton().disabled).toBeTrue();
    });
});
