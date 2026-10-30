import { ComponentFixture, TestBed } from '@angular/core/testing';
import { casoPrueba, SIN_CONTACTOS } from '../../testing/datos-prueba';
import { TablaCasosComponent } from './tabla-casos';

describe('TablaCasosComponent', () => {
    let fixture: ComponentFixture<TablaCasosComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(TablaCasosComponent);
        fixture.componentRef.setInput('casos', [
            casoPrueba(1, { estado: 'EN_CONTACTO', contactos: { ...SIN_CONTACTOS, total: 3, llamadas: 2, visitas: 1 }, ultimoContacto: { fechaHora: '2026-11-03T19:30:00Z', medio: 'LLAMADA', resultado: 'NO_RESPONDIO' } }),
            casoPrueba(2, { consolidador: null })
        ]);
        fixture.componentRef.setInput('totalRegistros', 2);
        await refrescar();
    });

    it('muestra consolidador, estado y contactos por medio', () => {
        const texto = html().querySelector('p-table')!.textContent!;
        expect(texto).toContain('Persona 1');
        expect(texto).toContain('En contacto');
        expect(texto).toContain('2 llamadas · 1 visita');
        expect(texto).toContain('No contestó');
        expect(texto).toContain('Sin asignar');
    });

    it('abre el caso al hacer clic en la fila', () => {
        const ver = jasmine.createSpy('ver');
        fixture.componentInstance.ver.subscribe(ver);
        (html().querySelector('p-table tbody tr') as HTMLElement).click();
        expect(ver).toHaveBeenCalledWith(jasmine.objectContaining({ id: 1 }));
    });

    it('solo muestra casillas si es seleccionable', async () => {
        expect(html().querySelector('p-tablecheckbox')).toBeNull();
        fixture.componentRef.setInput('seleccionable', true);
        await refrescar();
        expect(html().querySelectorAll('p-tablecheckbox').length).toBe(2);
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
