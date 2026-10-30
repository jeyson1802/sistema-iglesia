import { ComponentFixture, TestBed } from '@angular/core/testing';
import { contactoPrueba } from '../../testing/datos-prueba';
import { LineaTiempoComponent } from './linea-tiempo';

describe('LineaTiempoComponent', () => {
    let fixture: ComponentFixture<LineaTiempoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(LineaTiempoComponent);
        fixture.componentRef.setInput('contactos', [
            contactoPrueba(1, { fechaHora: '2026-11-03T19:30:00Z', medio: 'LLAMADA', resultado: 'SE_HABLO', observaciones: 'Vendrá el domingo' }),
            contactoPrueba(2, { fechaHora: '2026-11-10T15:00:00Z', medio: 'VISITA', resultado: 'NO_ESTABA', consolidador: { id: 31, nombre: 'Rosa Díaz', activo: true }, editable: false })
        ]);
        fixture.componentRef.setInput('asistencias', [{ id: 120, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-11-08' }]);
        await refrescar();
    });

    it('junta contactos y asistencias, de lo más reciente a lo más antiguo', () => {
        const textos = Array.from(html().querySelectorAll('.p-timeline-event-content')).map((evento) => evento.textContent!.trim());
        expect(textos[0]).toContain('Rosa visitó · No estaba');
        expect(textos[1]).toContain('Asistió a Culto dominical');
        expect(textos[2]).toContain('Luis llamó · Hablamos');
        expect(textos[2]).toContain('Vendrá el domingo');
    });

    it('solo los contactos corregibles ofrecen corregir y borrar', () => {
        const corregir = jasmine.createSpy('corregir');
        fixture.componentInstance.corregir.subscribe(corregir);
        const botones = Array.from(html().querySelectorAll<HTMLButtonElement>('p-button button')).filter((boton) => boton.textContent?.includes('Corregir'));
        expect(botones.length).toBe(1);
        botones[0].click();
        expect(corregir).toHaveBeenCalledWith(jasmine.objectContaining({ id: 1 }));
    });

    it('sin contactos ni asistencias lo dice', async () => {
        fixture.componentRef.setInput('contactos', []);
        fixture.componentRef.setInput('asistencias', []);
        await refrescar();
        expect(html().textContent).toContain('Aún no hay contactos registrados.');
    });
});
