import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';

/** Miembro que se puede elegir en un selector: activo y dentro del ámbito de quien busca. */
export interface OpcionMiembro {
    readonly id: number;
    readonly nombre: string;
}

/** Contrato de `OpcionMiembroResponse` en iglesia-api. */
interface OpcionMiembroResponse {
    readonly id: number;
    readonly nombreCompleto: string;
}

/** Fachada HTTP de `GET /api/miembros/opciones`: hasta 20 miembros activos cuyo nombre contiene el texto. */
@Injectable({ providedIn: 'root' })
export class OpcionesMiembroService {
    private readonly http = inject(HttpClient);
    private readonly opcionesUrl = `${environment.apiUrl}/miembros/opciones`;

    buscar(texto: string): Observable<OpcionMiembro[]> {
        const parametros = texto.trim() ? new HttpParams().set('q', texto.trim()) : new HttpParams();
        return this.http.get<OpcionMiembroResponse[]>(this.opcionesUrl, { params: parametros }).pipe(map((opciones) => opciones.map(({ id, nombreCompleto }) => ({ id, nombre: nombreCompleto }))));
    }
}
