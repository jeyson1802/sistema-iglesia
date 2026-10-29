import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { AbrirReunion, CrearEvento, FiltroReuniones, Reunion, ReunionHoy } from '../models/asistencia';

/** Orden del historial: lo más reciente primero. */
const ORDEN_HISTORIAL = ['fecha,desc', 'hora,desc'];

/** Fachada HTTP de las reuniones (`/api/reuniones`). El backend aplica el ámbito: un líder solo recibe sus grupos. */
@Injectable({ providedIn: 'root' })
export class ReunionesService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/reuniones`;

    hoy(): Observable<ReunionHoy[]> {
        return this.http.get<ReunionHoy[]>(`${this.url}/hoy`);
    }

    /** Abre la reunión del culto o grupo en esa fecha, o devuelve la que ya existe. */
    abrir(reunion: AbrirReunion): Observable<Reunion> {
        return this.http.post<Reunion>(this.url, reunion);
    }

    crearEvento(evento: CrearEvento): Observable<Reunion[]> {
        return this.http.post<Reunion[]>(`${this.url}/eventos`, evento);
    }

    eliminarEvento(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/${id}`);
    }

    historial(filtro: FiltroReuniones, pagina: number, tamano: number): Observable<Page<Reunion>> {
        let params = new HttpParams().set('desde', filtro.desde).set('hasta', filtro.hasta).set('page', pagina).set('size', tamano);
        if (filtro.tipo !== null) {
            params = params.set('tipo', filtro.tipo);
        }
        ORDEN_HISTORIAL.forEach((orden) => (params = params.append('sort', orden)));
        return this.http.get<Page<Reunion>>(this.url, { params });
    }

    obtener(id: number): Observable<Reunion> {
        return this.http.get<Reunion>(`${this.url}/${id}`);
    }
}
