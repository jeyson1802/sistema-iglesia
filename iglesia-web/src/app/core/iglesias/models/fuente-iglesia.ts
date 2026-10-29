import { Observable } from 'rxjs';
import { Apariencia } from '@/app/core/apariencia/models/apariencia';
import { DatosIglesia, Iglesia } from './iglesia';

/**
 * Dónde se lee y se guarda la identidad de una iglesia (Strategy): «Mi iglesia» usa la del token y el catálogo, la
 * de un id. El editor de la identidad trabaja igual con cualquiera de las dos.
 */
export interface FuenteIglesia {
    obtener(): Observable<Iglesia>;
    actualizar(datos: DatosIglesia): Observable<Iglesia>;
    cambiarApariencia(apariencia: Apariencia): Observable<Apariencia>;
    logo(): Observable<Blob>;
    subirLogo(archivo: Blob): Observable<void>;
    quitarLogo(): Observable<void>;
}
