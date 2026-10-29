import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BETO, CARLA } from '../../testing/asistencia-prueba';
import { CambioPresencia, ListaIntegrantesComponent } from './lista-integrantes';

describe('ListaIntegrantesComponent', () => {
    let fixture: ComponentFixture<ListaIntegrantesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [ListaIntegrantesComponent] }).compileComponents();
        fixture = TestBed.createComponent(ListaIntegrantesComponent);
        fixture.componentRef.setInput('integrantes', [BETO, CARLA]);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('cuenta los presentes y el paginador siempre se ve', () => {
        expect(html().textContent).toContain('1 de 2 presentes');
        expect(html().querySelector('.p-paginator')).toBeTruthy();
    });

    it('cada interruptor emite el cambio de presencia', () => {
        const cambios: CambioPresencia[] = [];
        fixture.componentInstance.cambiar.subscribe((cambio) => cambios.push(cambio));

        (html().querySelector('#presente-13') as HTMLInputElement).click();

        expect(cambios).toEqual([{ miembro: BETO, presente: true }]);
    });

    it('«Marcar a todos» se deshabilita cuando ya están todos', () => {
        const boton = (): HTMLButtonElement => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.includes('Marcar a todos')) as HTMLButtonElement;
        expect(boton().disabled).toBeFalse();

        fixture.componentRef.setInput('integrantes', [{ ...BETO, presente: true }, CARLA]);
        fixture.detectChanges();

        expect(boton().disabled).toBeTrue();
    });
});
