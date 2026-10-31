import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AtajosDelDiaComponent } from './atajos-del-dia';

describe('AtajosDelDiaComponent', () => {
    let fixture: ComponentFixture<AtajosDelDiaComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(AtajosDelDiaComponent);
    });

    it('destaca el primero y enlaza cada atajo', async () => {
        fixture.componentRef.setInput('atajos', [
            { etiqueta: 'Tomar asistencia', descripcion: '2 reuniones hoy', icono: 'pi pi-check-square', ruta: '/asistencia' },
            { etiqueta: 'Personas con alerta', descripcion: '5 personas', icono: 'pi pi-bell', ruta: '/asistencia/seguimiento' }
        ]);
        await fixture.whenStable();
        const enlaces = html().querySelectorAll('a');
        expect(enlaces.length).toBe(2);
        expect(enlaces[0].getAttribute('href')).toBe('/asistencia');
        expect(enlaces[0].className).toContain('bg-primary');
        expect(enlaces[1].className).not.toContain('bg-primary');
    });

    it('sin atajos no muestra nada', async () => {
        fixture.componentRef.setInput('atajos', []);
        await fixture.whenStable();
        expect(html().querySelector('nav')).toBeNull();
    });
});
