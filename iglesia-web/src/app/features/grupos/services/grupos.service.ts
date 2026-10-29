import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Grupo, GrupoResponse, GuardarGrupo } from '../models/grupo';
import { aGrupo } from './grupo.mapper';

/** Fachada HTTP de los grupos pequeños de la iglesia (`/api/grupos`). */
@Injectable({ providedIn: 'root' })
export class GruposService {
    private readonly http = inject(HttpClient);
    private readonly gruposUrl = `${environment.apiUrl}/grupos`;

    /** Todos los grupos, activos e inactivos, ordenados por nombre. */
    listar(): Observable<Grupo[]> {
        return this.http.get<GrupoResponse[]>(this.gruposUrl).pipe(map((grupos) => grupos.map(aGrupo)));
    }

    crear(datos: GuardarGrupo): Observable<Grupo> {
        return this.http.post<GrupoResponse>(this.gruposUrl, datos).pipe(map(aGrupo));
    }

    actualizar(id: number, datos: GuardarGrupo): Observable<Grupo> {
        return this.http.put<GrupoResponse>(`${this.gruposUrl}/${id}`, datos).pipe(map(aGrupo));
    }

    cambiarEstado(id: number, activo: boolean): Observable<Grupo> {
        return this.http.patch<GrupoResponse>(`${this.gruposUrl}/${id}/estado`, { activo }).pipe(map(aGrupo));
    }
}
