import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { NotificadorErroresService } from '../notificador-errores.service';
import { ToastErroresGlobalesComponent } from './toast-errores-globales';

describe('ToastErroresGlobalesComponent', () => {
    const CODIGO = '4bf92f3577b34da6a3ce929d0e0e4736';
    let fixture: ComponentFixture<ToastErroresGlobalesComponent>;
    let notificador: NotificadorErroresService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [ToastErroresGlobalesComponent], providers: [MessageService] }).compileComponents();
        fixture = TestBed.createComponent(ToastErroresGlobalesComponent);
        notificador = TestBed.inject(NotificadorErroresService);
        await fixture.whenStable();
    });

    const host = (): HTMLElement => fixture.nativeElement;

    it('un error inesperado muestra el mensaje y el código de seguimiento con el botón «Copiar»', async () => {
        notificador.errorInesperado(CODIGO);
        await fixture.whenStable();

        expect(host().textContent).toContain('Ocurrió un error inesperado');
        expect(host().querySelector('app-codigo-seguimiento code')?.textContent).toBe(CODIGO);
        expect(host().querySelector('button[aria-label="Copiar código de seguimiento"]')).not.toBeNull();
    });

    it('sin conexión avisa sin código', async () => {
        notificador.sinConexion();
        await fixture.whenStable();

        expect(host().textContent).toContain('No se pudo conectar con el servidor');
        expect(host().querySelector('app-codigo-seguimiento')).toBeNull();
        expect(host().querySelector('.pi-exclamation-triangle')).not.toBeNull();
    });

    it('ignora los mensajes de otros toasts', async () => {
        TestBed.inject(MessageService).add({ key: 'otro', severity: 'info', summary: 'Ajeno' });
        await fixture.whenStable();

        expect(host().textContent).not.toContain('Ajeno');
    });
});
