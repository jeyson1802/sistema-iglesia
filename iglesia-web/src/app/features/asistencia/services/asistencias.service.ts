import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { Asistente, AsistenciaMiembro, Marca, MiembroReunion } from '../models/asistencia';

/** Fachada HTTP de la toma de asistencia de una reunión y de la asistencia de un miembro. */
@Injectable({ providedIn: 'root' })
export class AsistenciasService {
    private readonly http = inject(HttpClient);
    private readonly reunionesUrl = `${environment.apiUrl}/reuniones`;
    private readonly miembrosUrl = `${environment.apiUrl}/miembros`;

    /** Marca con lo leído del QR (con o sin el prefijo `ASIS:`) o escrito a mano. */
    marcarPorCodigo(reunionId: number, codigo: string): Observable<Marca> {
        return this.http.post<Marca>(`${this.reunionesUrl}/${reunionId}/asistencias/codigo`, { codigo });
    }

    marcarMiembro(reunionId: number, miembroId: number): Observable<Marca> {
        return this.http.post<Marca>(`${this.reunionesUrl}/${reunionId}/asistencias`, { miembroId });
    }

    quitar(reunionId: number, miembroId: number): Observable<void> {
        return this.http.delete<void>(`${this.reunionesUrl}/${reunionId}/asistencias/${miembroId}`);
    }

    asistentes(reunionId: number, q: string, pagina: number, tamano: number): Observable<Page<Asistente>> {
        let params = new HttpParams().set('page', pagina).set('size', tamano).set('sort', 'registradaEn,desc');
        if (q.trim()) {
            params = params.set('q', q.trim());
        }
        return this.http.get<Page<Asistente>>(`${this.reunionesUrl}/${reunionId}/asistentes`, { params });
    }

    integrantes(reunionId: number): Observable<MiembroReunion[]> {
        return this.http.get<MiembroReunion[]>(`${this.reunionesUrl}/${reunionId}/integrantes`);
    }

    buscarMiembros(reunionId: number, q: string): Observable<MiembroReunion[]> {
        return this.http.get<MiembroReunion[]>(`${this.reunionesUrl}/${reunionId}/miembros`, { params: { q: q.trim() } });
    }

    resumenMiembro(miembroId: number): Observable<AsistenciaMiembro> {
        return this.http.get<AsistenciaMiembro>(`${this.miembrosUrl}/${miembroId}/asistencia`);
    }
}
