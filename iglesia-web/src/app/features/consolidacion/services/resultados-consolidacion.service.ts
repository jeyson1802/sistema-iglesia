import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { ResultadoConsolidacion } from '../models/consolidacion';

/** Fachada HTTP del catálogo de resultados de consolidación. */
@Injectable({ providedIn: 'root' })
export class ResultadosConsolidacionService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/consolidacion/resultados`;

    listar(): Observable<ResultadoConsolidacion[]> {
        return this.http.get<ResultadoConsolidacion[]>(this.url);
    }

    crear(nombre: string): Observable<ResultadoConsolidacion> {
        return this.http.post<ResultadoConsolidacion>(this.url, { nombre });
    }

    renombrar(id: number, nombre: string): Observable<ResultadoConsolidacion> {
        return this.http.put<ResultadoConsolidacion>(`${this.url}/${id}`, { nombre });
    }

    cambiarEstado(id: number, activo: boolean): Observable<ResultadoConsolidacion> {
        return this.http.patch<ResultadoConsolidacion>(`${this.url}/${id}/estado`, { activo });
    }

    ordenar(ids: readonly number[]): Observable<ResultadoConsolidacion[]> {
        return this.http.put<ResultadoConsolidacion[]>(`${this.url}/orden`, { ids });
    }
}
