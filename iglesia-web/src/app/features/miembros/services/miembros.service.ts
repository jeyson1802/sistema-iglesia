import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Miembro } from '../models/miembro';

@Injectable({ providedIn: 'root' })
export class MiembrosService {
    private http = inject(HttpClient);
    private url = `${environment.apiUrl}/miembros`;

    listar(): Observable<Miembro[]> {
        return this.http.get<Miembro[]>(this.url);
    }

    obtener(id: number): Observable<Miembro> {
        return this.http.get<Miembro>(`${this.url}/${id}`);
    }

    crear(miembro: Omit<Miembro, 'id'>): Observable<Miembro> {
        return this.http.post<Miembro>(this.url, miembro);
    }

    actualizar(id: number, miembro: Omit<Miembro, 'id'>): Observable<Miembro> {
        return this.http.put<Miembro>(`${this.url}/${id}`, miembro);
    }

    eliminar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/${id}`);
    }
}
