import { ComponentFixture, TestBed } from '@angular/core/testing';
import { casoPrueba } from '../../testing/datos-prueba';
import { ContactoPedido, SeccionCasosComponent } from './seccion-casos';

describe('SeccionCasosComponent', () => {
    let fixture: ComponentFixture<SeccionCasosComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(SeccionCasosComponent);
        fixture.componentRef.setInput('titulo', 'Para hoy');
        fixture.componentRef.setInput('icono', 'pi pi-bell');
        fixture.componentRef.setInput('cantidad', 2);
        fixture.componentRef.setInput('casos', [casoPrueba(1), casoPrueba(2)]);
        fixture.componentRef.setInput('totalRegistros', 2);
        fixture.componentRef.setInput('mensajeVacio', '¡Todo al día!');
        await refrescar();
    });

    it('muestra el título con su cantidad y una tarjeta por persona', () => {
        expect(html().querySelector('h2')?.textContent).toContain('Para hoy');
        expect(html().querySelector('h2')?.textContent).toContain('2');
        expect(html().querySelectorAll('app-tarjeta-caso').length).toBe(2);
    });

    it('avisa a qué persona y por qué medio se quiere contactar', () => {
        const pedidos: ContactoPedido[] = [];
        fixture.componentInstance.contactar.subscribe((pedido) => pedidos.push(pedido));
        const llamar = html().querySelector('a[href^="tel:"]') as HTMLAnchorElement;
        llamar.addEventListener('click', (evento) => evento.preventDefault());
        llamar.click();
        expect(pedidos[0].caso.id).toBe(1);
        expect(pedidos[0].medio).toBe('LLAMADA');
    });

    it('vacía muestra su mensaje y conserva el paginador', async () => {
        fixture.componentRef.setInput('casos', []);
        await refrescar();
        expect(html().textContent).toContain('¡Todo al día!');
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
