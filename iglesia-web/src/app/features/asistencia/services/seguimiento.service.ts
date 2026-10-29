import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { ConteoAlertas, FiltroOcasiones, FiltroSeguimiento, Ocasion, PersonaSeguimiento, ReglasSeguimiento, SeguimientoGrupo, ServiciosSeguimiento } from '../models/seguimiento';

/** Fachada HTTP del seguimiento de asistencia: por miembro, por grupo, lista de seguimiento, servicios y reglas. */
@Injectable({ providedIn: 'root' })
export class SeguimientoService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly seguimientoUrl = `${environment.apiUrl}/seguimiento-asistencia`;

    ocasiones(miembroId: number, { estado }: FiltroOcasiones, pagina: number, tamano: number): Observable<Page<Ocasion>> {
        let params = new HttpParams().set('page', pagina).set('size', tamano);
        if (estado) {
            params = params.set('estado', estado);
        }
        return this.http.get<Page<Ocasion>>(`${this.apiUrl}/miembros/${miembroId}/asistencia/ocasiones`, { params });
    }

    lista(filtro: FiltroSeguimiento, pagina: number, tamano: number): Observable<Page<PersonaSeguimiento>> {
        let params = this.paramsDe(filtro).set('page', pagina).set('size', tamano);
        if (filtro.alerta) {
            params = params.set('alerta', filtro.alerta);
        }
        if (filtro.todos) {
            params = params.set('todos', true);
        }
        return this.http.get<Page<PersonaSeguimiento>>(this.seguimientoUrl, { params });
    }

    /** Cantidades por alerta con los mismos filtros de grupo y nombre que la lista. */
    conteos(filtro: FiltroSeguimiento): Observable<ConteoAlertas> {
        return this.http.get<ConteoAlertas>(`${this.seguimientoUrl}/conteos`, { params: this.paramsDe(filtro) });
    }

    delGrupo(grupoId: number): Observable<SeguimientoGrupo> {
        return this.http.get<SeguimientoGrupo>(`${this.apiUrl}/grupos/${grupoId}/seguimiento`);
    }

    servicios(): Observable<ServiciosSeguimiento> {
        return this.http.get<ServiciosSeguimiento>(`${this.seguimientoUrl}/servicios`);
    }

    reglas(): Observable<ReglasSeguimiento> {
        return this.http.get<ReglasSeguimiento>(`${this.apiUrl}/configuracion-seguimiento`);
    }

    guardarReglas(reglas: ReglasSeguimiento): Observable<ReglasSeguimiento> {
        return this.http.put<ReglasSeguimiento>(`${this.apiUrl}/configuracion-seguimiento`, reglas);
    }

    private paramsDe({ grupoId, q }: FiltroSeguimiento): HttpParams {
        let params = new HttpParams();
        if (grupoId !== null) {
            params = params.set('grupoId', grupoId);
        }
        if (q.trim()) {
            params = params.set('q', q.trim());
        }
        return params;
    }
}
