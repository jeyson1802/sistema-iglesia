import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { CultoProgramado, GuardarCulto } from '../models/asistencia';

/** Fachada HTTP de los horarios de culto (`/api/cultos-programados`). */
@Injectable({ providedIn: 'root' })
export class CultosProgramadosService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/cultos-programados`;

    listar(): Observable<CultoProgramado[]> {
        return this.http.get<CultoProgramado[]>(this.url);
    }

    crear(culto: GuardarCulto): Observable<CultoProgramado> {
        return this.http.post<CultoProgramado>(this.url, culto);
    }

    actualizar(id: number, culto: GuardarCulto): Observable<CultoProgramado> {
        return this.http.put<CultoProgramado>(`${this.url}/${id}`, culto);
    }

    cambiarEstado(id: number, activo: boolean): Observable<CultoProgramado> {
        return this.http.patch<CultoProgramado>(`${this.url}/${id}/estado`, { activo });
    }
}
