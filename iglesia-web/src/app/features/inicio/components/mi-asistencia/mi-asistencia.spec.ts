import { ComponentFixture, TestBed } from '@angular/core/testing';
import { miAsistenciaPrueba } from '../../testing/estadisticas-prueba';
import { MiAsistenciaComponent } from './mi-asistencia';

describe('MiAsistenciaComponent', () => {
    let fixture: ComponentFixture<MiAsistenciaComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        fixture = TestBed.createComponent(MiAsistenciaComponent);
    });

    it('muestra sus indicadores, sus semanas y sus últimas reuniones', async () => {
        fixture.componentRef.setInput('asistencia', miAsistenciaPrueba());
        await fixture.whenStable();
        expect(html().querySelector('app-indicadores-asistencia')).not.toBeNull();
        expect(html().querySelector('app-semanas-asistencia')).not.toBeNull();
        expect(html().querySelector('tbody')?.textContent?.replace(/\s+/g, '')).toContain('Cultodominical');
    });

    it('muestra siempre el paginador de sus últimas reuniones', async () => {
        fixture.componentRef.setInput('asistencia', miAsistenciaPrueba());
        await fixture.whenStable();
        expect(html().querySelector('.p-paginator, p-paginator')).not.toBeNull();
    });

    it('sin asistencias lo explica', async () => {
        fixture.componentRef.setInput('asistencia', miAsistenciaPrueba({ ultimas: [] }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Aúnnotienesasistenciasregistradas.');
    });

    it('si falla permite reintentar', async () => {
        fixture.componentRef.setInput('error', true);
        await fixture.whenStable();
        let reintentos = 0;
        fixture.componentInstance.reintentar.subscribe(() => reintentos++);
        html().querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
        expect(reintentos).toBe(1);
    });
});
