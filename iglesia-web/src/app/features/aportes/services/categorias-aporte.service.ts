import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { CategoriaAporte, GuardarCategoriaAporte } from '../models/configuracion-aportes';

/** Fachada HTTP de las categorías de aporte de la iglesia (`/api/categorias-aporte`). */
@Injectable({ providedIn: 'root' })
export class CategoriasAporteService {
    private readonly http = inject(HttpClient);
    private readonly categoriasUrl = `${environment.apiUrl}/categorias-aporte`;

    /** Todas, activas e inactivas, en su orden. */
    listar(): Observable<CategoriaAporte[]> {
        return this.http.get<CategoriaAporte[]>(this.categoriasUrl);
    }

    crear(categoria: GuardarCategoriaAporte): Observable<CategoriaAporte> {
        return this.http.post<CategoriaAporte>(this.categoriasUrl, categoria);
    }

    actualizar(id: number, categoria: GuardarCategoriaAporte): Observable<CategoriaAporte> {
        return this.http.put<CategoriaAporte>(`${this.categoriasUrl}/${id}`, categoria);
    }

    /** Nuevo orden con todos los ids de la iglesia. Devuelve la lista reordenada. */
    ordenar(ids: readonly number[]): Observable<CategoriaAporte[]> {
        return this.http.put<CategoriaAporte[]>(`${this.categoriasUrl}/orden`, { ids });
    }

    cambiarEstado(id: number, activo: boolean): Observable<CategoriaAporte> {
        return this.http.patch<CategoriaAporte>(`${this.categoriasUrl}/${id}/estado`, { activo });
    }
}
