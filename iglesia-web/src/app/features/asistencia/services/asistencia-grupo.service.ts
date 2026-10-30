import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { DetalleReunionGrupo, ReunionGrupo } from '../models/asistencia-grupo';

/** Fachada HTTP de la asistencia de un grupo reunión por reunión. */
@Injectable({ providedIn: 'root' })
export class AsistenciaGrupoService {
    private readonly http = inject(HttpClient);
    private readonly gruposUrl = `${environment.apiUrl}/grupos`;

    reuniones(grupoId: number, pagina: number, tamano: number): Observable<Page<ReunionGrupo>> {
        const params = new HttpParams().set('page', pagina).set('size', tamano);
        return this.http.get<Page<ReunionGrupo>>(`${this.gruposUrl}/${grupoId}/reuniones`, { params });
    }

    detalle(grupoId: number, reunionId: number): Observable<DetalleReunionGrupo> {
        return this.http.get<DetalleReunionGrupo>(`${this.gruposUrl}/${grupoId}/reuniones/${reunionId}`);
    }
}
