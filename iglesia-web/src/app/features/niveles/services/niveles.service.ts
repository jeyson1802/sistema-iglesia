import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Nivel } from '../models/nivel';

/** Fachada HTTP del catálogo de niveles de la iglesia (`/api/niveles`). El contrato coincide con el modelo. */
@Injectable({ providedIn: 'root' })
export class NivelesService {
    private readonly http = inject(HttpClient);
    private readonly nivelesUrl = `${environment.apiUrl}/niveles`;

    /** Todos los niveles, activos e inactivos, ordenados por `orden`. */
    listar(): Observable<Nivel[]> {
        return this.http.get<Nivel[]>(this.nivelesUrl);
    }

    crear(nombre: string): Observable<Nivel> {
        return this.http.post<Nivel>(this.nivelesUrl, { nombre });
    }

    renombrar(id: number, nombre: string): Observable<Nivel> {
        return this.http.put<Nivel>(`${this.nivelesUrl}/${id}`, { nombre });
    }

    /** Nuevo orden con todos los ids de la iglesia, del más alto al más bajo. Devuelve la lista reordenada. */
    ordenar(ids: readonly number[]): Observable<Nivel[]> {
        return this.http.put<Nivel[]>(`${this.nivelesUrl}/orden`, { ids });
    }

    cambiarEstado(id: number, activo: boolean): Observable<Nivel> {
        return this.http.patch<Nivel>(`${this.nivelesUrl}/${id}/estado`, { activo });
    }
}
