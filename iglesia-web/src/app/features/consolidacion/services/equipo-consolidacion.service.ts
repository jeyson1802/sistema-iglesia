import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Consolidador } from '../models/consolidacion';

/** Fachada HTTP del equipo de consolidación. */
@Injectable({ providedIn: 'root' })
export class EquipoConsolidacionService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/consolidacion/equipo`;

    listar(): Observable<Consolidador[]> {
        return this.http.get<Consolidador[]>(this.url);
    }

    agregar(miembroId: number): Observable<Consolidador> {
        return this.http.post<Consolidador>(this.url, { miembroId });
    }

    cambiarEstado(miembroId: number, activo: boolean): Observable<Consolidador> {
        return this.http.patch<Consolidador>(`${this.url}/${miembroId}/estado`, { activo });
    }
}
