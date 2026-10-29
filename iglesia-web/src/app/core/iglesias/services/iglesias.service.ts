import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Iglesia, IglesiaResponse } from '../models/iglesia';
import { aIglesia } from './iglesia.mapper';

/**
 * Fachada HTTP del catálogo de iglesias de iglesia-api. Está en `core` porque la usan la pantalla `/iglesias` y la
 * barra superior (nombre de la iglesia en la que opera el administrador del sistema).
 */
@Injectable({ providedIn: 'root' })
export class IglesiasService {
    private readonly http = inject(HttpClient);
    private readonly iglesiasUrl = `${environment.apiUrl}/iglesias`;

    /** Todas las iglesias, ordenadas por nombre por el backend. */
    listar(): Observable<Iglesia[]> {
        return this.http.get<IglesiaResponse[]>(this.iglesiasUrl).pipe(map((iglesias) => iglesias.map(aIglesia)));
    }

    obtener(id: number): Observable<Iglesia> {
        return this.http.get<IglesiaResponse>(`${this.iglesiasUrl}/${id}`).pipe(map(aIglesia));
    }

    crear(nombre: string): Observable<Iglesia> {
        return this.http.post<IglesiaResponse>(this.iglesiasUrl, { nombre }).pipe(map(aIglesia));
    }

    renombrar(id: number, nombre: string): Observable<Iglesia> {
        return this.http.put<IglesiaResponse>(`${this.iglesiasUrl}/${id}`, { nombre }).pipe(map(aIglesia));
    }
}
