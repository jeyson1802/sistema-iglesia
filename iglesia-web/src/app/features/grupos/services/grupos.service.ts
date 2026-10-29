import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { CandidatoGrupo, Grupo, GrupoResponse, GuardarGrupo, IntegranteGrupo } from '../models/grupo';
import { aGrupo } from './grupo.mapper';

/** Fachada HTTP de los grupos pequeños de la iglesia (`/api/grupos`) y de sus integrantes. */
@Injectable({ providedIn: 'root' })
export class GruposService {
    private readonly http = inject(HttpClient);
    private readonly gruposUrl = `${environment.apiUrl}/grupos`;

    /** Todos los grupos, activos e inactivos, ordenados por el nombre de su líder. */
    listar(): Observable<Grupo[]> {
        return this.http.get<GrupoResponse[]>(this.gruposUrl).pipe(map((grupos) => grupos.map(aGrupo)));
    }

    obtener(id: number): Observable<Grupo> {
        return this.http.get<GrupoResponse>(`${this.gruposUrl}/${id}`).pipe(map(aGrupo));
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

    /** Miembros activos del grupo, ordenados por apellidos. */
    integrantes(id: number): Observable<IntegranteGrupo[]> {
        return this.http.get<IntegranteGrupo[]>(`${this.gruposUrl}/${id}/miembros`);
    }

    /** Hasta 20 miembros activos fuera del grupo cuyo nombre contiene el texto, con su grupo actual. */
    candidatos(id: number, q: string): Observable<CandidatoGrupo[]> {
        const params = q.trim() ? new HttpParams().set('q', q.trim()) : new HttpParams();
        return this.http.get<CandidatoGrupo[]>(`${this.gruposUrl}/${id}/candidatos`, { params });
    }

    /** Agrega el miembro al grupo o, si estaba en otro, lo mueve. */
    agregarIntegrante(id: number, miembroId: number): Observable<IntegranteGrupo> {
        return this.http.put<IntegranteGrupo>(`${this.gruposUrl}/${id}/miembros/${miembroId}`, null);
    }

    quitarIntegrante(id: number, miembroId: number): Observable<void> {
        return this.http.delete<void>(`${this.gruposUrl}/${id}/miembros/${miembroId}`);
    }
}
