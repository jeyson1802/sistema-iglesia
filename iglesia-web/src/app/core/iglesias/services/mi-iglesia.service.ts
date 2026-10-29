import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Apariencia } from '@/app/core/apariencia/models/apariencia';
import { DatosIglesia, Iglesia, IglesiaResponse } from '../models/iglesia';
import { FuenteIglesia } from '../models/fuente-iglesia';
import { aIglesia } from './iglesia.mapper';

/** Fachada HTTP de «Mi iglesia» (`/api/mi-iglesia`): la iglesia la pone el backend a partir del token. */
@Injectable({ providedIn: 'root' })
export class MiIglesiaService implements FuenteIglesia {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/mi-iglesia`;

    obtener(): Observable<Iglesia> {
        return this.http.get<IglesiaResponse>(this.url).pipe(map(aIglesia));
    }

    actualizar(datos: DatosIglesia): Observable<Iglesia> {
        return this.http.put<IglesiaResponse>(this.url, datos).pipe(map(aIglesia));
    }

    cambiarApariencia(apariencia: Apariencia): Observable<Apariencia> {
        return this.http.put<Apariencia>(`${this.url}/apariencia`, apariencia);
    }

    logo(): Observable<Blob> {
        return this.http.get(`${this.url}/logo`, { responseType: 'blob' });
    }

    subirLogo(archivo: Blob): Observable<void> {
        return this.http.put<void>(`${this.url}/logo`, archivo, { headers: { 'Content-Type': archivo.type } });
    }

    quitarLogo(): Observable<void> {
        return this.http.delete<void>(`${this.url}/logo`);
    }
}
