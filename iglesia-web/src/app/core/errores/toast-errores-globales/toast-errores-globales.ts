import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ToastMessageOptions } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { CodigoSeguimientoComponent } from '@/app/shared/components/codigo-seguimiento/codigo-seguimiento';
import { CLAVE_TOAST_ERRORES, DatosErrorGlobal } from '../notificador-errores.service';

/** En móvil el toast ocupa todo el ancho para que el código de seguimiento se lea completo. */
const PUNTOS_DE_QUIEBRE = { '640px': { width: 'calc(100% - 2rem)', right: '1rem', left: '1rem' } };

/** Toast único de la app para los errores inesperados y de conexión; muestra el código de seguimiento si lo hay. */
@Component({
    selector: 'app-toast-errores-globales',
    imports: [ToastModule, CodigoSeguimientoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './toast-errores-globales.html'
})
export class ToastErroresGlobalesComponent {
    protected readonly clave = CLAVE_TOAST_ERRORES;
    protected readonly puntosDeQuiebre = PUNTOS_DE_QUIEBRE;

    protected icono(mensaje: ToastMessageOptions): string {
        return mensaje.severity === 'warn' ? 'pi pi-exclamation-triangle' : 'pi pi-times-circle';
    }

    protected traceId(mensaje: ToastMessageOptions): string | null {
        return (mensaje.data as DatosErrorGlobal | undefined)?.traceId ?? null;
    }
}
