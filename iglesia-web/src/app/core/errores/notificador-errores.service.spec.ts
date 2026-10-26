import { TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { CLAVE_TOAST_ERRORES, DETALLE_ERROR_CON_CODIGO, DETALLE_ERROR_SIN_CODIGO, DETALLE_SIN_CONEXION, NotificadorErroresService } from './notificador-errores.service';

describe('NotificadorErroresService', () => {
    let servicio: NotificadorErroresService;
    let mensajes: jasmine.SpyObj<MessageService>;

    beforeEach(() => {
        mensajes = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
        TestBed.configureTestingModule({ providers: [{ provide: MessageService, useValue: mensajes }] });
        servicio = TestBed.inject(NotificadorErroresService);
    });

    it('un error inesperado con código queda fijo en el toast global con el traceId', () => {
        servicio.errorInesperado('4bf92f3577b34da6a3ce929d0e0e4736');

        expect(mensajes.add).toHaveBeenCalledOnceWith(
            jasmine.objectContaining({
                key: CLAVE_TOAST_ERRORES,
                severity: 'error',
                detail: DETALLE_ERROR_CON_CODIGO,
                data: { traceId: '4bf92f3577b34da6a3ce929d0e0e4736' },
                sticky: true
            })
        );
    });

    it('sin código no pide comunicarlo a soporte', () => {
        servicio.errorInesperado(null);

        expect(mensajes.add).toHaveBeenCalledOnceWith(jasmine.objectContaining({ detail: DETALLE_ERROR_SIN_CODIGO, data: { traceId: null } }));
    });

    it('la falta de conexión avisa sin código y se cierra sola', () => {
        servicio.sinConexion();

        const mensaje = mensajes.add.calls.mostRecent().args[0];
        expect(mensaje).toEqual(jasmine.objectContaining({ key: CLAVE_TOAST_ERRORES, severity: 'warn', detail: DETALLE_SIN_CONEXION, data: { traceId: null } }));
        expect(mensaje.sticky).toBeFalsy();
        expect(mensaje.life).toBeGreaterThan(0);
    });
});
