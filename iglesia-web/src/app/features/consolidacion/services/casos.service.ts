import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { CasoDetalle, CasoResumen, ConsolidacionMiembro, Contacto, DatosContacto, Iteracion, ResumenMisCasos, SeccionMisCasos } from '../models/consolidacion';

/** Fachada HTTP de los casos: «Mis asignados», detalle, seguimiento individual, contactos, cierre e historial. */
@Injectable({ providedIn: 'root' })
export class CasosService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/consolidacion`;
    private readonly miembrosUrl = `${environment.apiUrl}/miembros`;

    misCasos(seccion: SeccionMisCasos, q: string, pagina: number, tamano: number): Observable<Page<CasoResumen>> {
        let params = new HttpParams().set('seccion', seccion).set('page', pagina).set('size', tamano);
        if (q.trim()) {
            params = params.set('q', q.trim());
        }
        return this.http.get<Page<CasoResumen>>(`${this.url}/mis-casos`, { params });
    }

    resumenMisCasos(): Observable<ResumenMisCasos> {
        return this.http.get<ResumenMisCasos>(`${this.url}/mis-casos/resumen`);
    }

    detalle(id: number): Observable<CasoDetalle> {
        return this.http.get<CasoDetalle>(`${this.url}/casos/${id}`);
    }

    abrir(miembroId: number, consolidadorId: number | null = null): Observable<CasoDetalle> {
        return this.http.post<CasoDetalle>(`${this.url}/casos`, { miembroId, consolidadorId });
    }

    cerrar(id: number, resultadoId: number, observacion: string | null): Observable<CasoDetalle> {
        return this.http.post<CasoDetalle>(`${this.url}/casos/${id}/cerrar`, { resultadoId, observacion });
    }

    reabrir(id: number): Observable<CasoDetalle> {
        return this.http.post<CasoDetalle>(`${this.url}/casos/${id}/reabrir`, null);
    }

    registrarContacto(casoId: number, datos: DatosContacto): Observable<Contacto> {
        return this.http.post<Contacto>(`${this.url}/casos/${casoId}/contactos`, datos);
    }

    corregirContacto(id: number, datos: DatosContacto): Observable<Contacto> {
        return this.http.put<Contacto>(`${this.url}/contactos/${id}`, datos);
    }

    borrarContacto(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url}/contactos/${id}`);
    }

    reasignar(casoIds: readonly number[], consolidadorId: number): Observable<void> {
        return casoIds.length === 1 ? this.http.patch<void>(`${this.url}/casos/${casoIds[0]}/consolidador`, { consolidadorId }) : this.http.patch<void>(`${this.url}/casos/consolidador`, { casoIds, consolidadorId });
    }

    resumenDeMiembro(miembroId: number): Observable<ConsolidacionMiembro> {
        return this.http.get<ConsolidacionMiembro>(`${this.miembrosUrl}/${miembroId}/consolidacion`);
    }

    historial(miembroId: number, pagina: number, tamano: number): Observable<Page<Iteracion>> {
        const params = new HttpParams().set('page', pagina).set('size', tamano);
        return this.http.get<Page<Iteracion>>(`${this.miembrosUrl}/${miembroId}/consolidacion/historial`, { params });
    }
}
