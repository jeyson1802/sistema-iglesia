import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { AvanceRonda, CasoResumen, DatosRonda, FiltroCasosRonda, FiltroLote, FiltroRondas, LoteAgregado, PropuestaReparto, Ronda, SolicitudReparto, VistaPreviaLote } from '../models/consolidacion';

/** Fachada HTTP de las rondas de consolidación: alta, ciclo de vida, lote, reparto, casos y avance. */
@Injectable({ providedIn: 'root' })
export class RondasService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/consolidacion/rondas`;

    listar({ estado, q }: FiltroRondas, pagina: number, tamano: number): Observable<Page<Ronda>> {
        let params = new HttpParams().set('page', pagina).set('size', tamano);
        if (estado) {
            params = params.set('estado', estado);
        }
        if (q.trim()) {
            params = params.set('q', q.trim());
        }
        return this.http.get<Page<Ronda>>(this.url, { params });
    }

    obtener(id: number): Observable<Ronda> {
        return this.http.get<Ronda>(`${this.url}/${id}`);
    }

    crear(datos: DatosRonda): Observable<Ronda> {
        return this.http.post<Ronda>(this.url, datos);
    }

    actualizar(id: number, datos: DatosRonda): Observable<Ronda> {
        return this.http.put<Ronda>(`${this.url}/${id}`, datos);
    }

    borrar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/${id}`);
    }

    iniciar(id: number): Observable<Ronda> {
        return this.http.post<Ronda>(`${this.url}/${id}/iniciar`, null);
    }

    cerrar(id: number): Observable<Ronda> {
        return this.http.post<Ronda>(`${this.url}/${id}/cerrar`, null);
    }

    vistaPreviaLote(id: number, filtro: FiltroLote): Observable<VistaPreviaLote> {
        return this.http.post<VistaPreviaLote>(`${this.url}/${id}/lote/vista-previa`, filtro);
    }

    agregarLote(id: number, filtro: FiltroLote): Observable<LoteAgregado> {
        return this.http.post<LoteAgregado>(`${this.url}/${id}/lote`, filtro);
    }

    casos(id: number, { estado, consolidadorId, sinAsignar, q }: FiltroCasosRonda, pagina: number, tamano: number): Observable<Page<CasoResumen>> {
        let params = new HttpParams().set('page', pagina).set('size', tamano);
        if (estado) {
            params = params.set('estado', estado);
        }
        if (consolidadorId !== null) {
            params = params.set('consolidadorId', consolidadorId);
        }
        if (sinAsignar) {
            params = params.set('sinAsignar', true);
        }
        if (q.trim()) {
            params = params.set('q', q.trim());
        }
        return this.http.get<Page<CasoResumen>>(`${this.url}/${id}/casos`, { params });
    }

    quitarCaso(id: number, casoId: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/${id}/casos/${casoId}`);
    }

    vistaPreviaReparto(id: number, solicitud: SolicitudReparto): Observable<PropuestaReparto> {
        return this.http.post<PropuestaReparto>(`${this.url}/${id}/repartir/vista-previa`, solicitud);
    }

    repartir(id: number, solicitud: SolicitudReparto): Observable<Ronda> {
        return this.http.post<Ronda>(`${this.url}/${id}/repartir`, solicitud);
    }

    avance(id: number): Observable<AvanceRonda> {
        return this.http.get<AvanceRonda>(`${this.url}/${id}/avance`);
    }
}
