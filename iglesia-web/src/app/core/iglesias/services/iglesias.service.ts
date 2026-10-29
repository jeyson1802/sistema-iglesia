import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Apariencia } from '@/app/core/apariencia/models/apariencia';
import { Page } from '@/app/core/models/page';
import { DatosIglesia, FiltroIglesias, Iglesia, IglesiaResponse } from '../models/iglesia';
import { FuenteIglesia } from '../models/fuente-iglesia';
import { aIglesia } from './iglesia.mapper';

const ORDEN_CATALOGO = 'nombreComercial,asc';

/**
 * Fachada HTTP del catálogo de iglesias de iglesia-api (administrador del sistema). Está en `core` porque la usan
 * el catálogo y la edición de una iglesia, que comparte el editor de identidad con «Mi iglesia».
 */
@Injectable({ providedIn: 'root' })
export class IglesiasService {
    private readonly http = inject(HttpClient);
    private readonly iglesiasUrl = `${environment.apiUrl}/iglesias`;

    listar(filtro: FiltroIglesias, pagina: number, tamano: number): Observable<Page<Iglesia>> {
        let params = new HttpParams().set('page', pagina).set('size', tamano).set('sort', ORDEN_CATALOGO);
        if (filtro.q) {
            params = params.set('q', filtro.q);
        }
        if (filtro.ubigeo) {
            params = params.set('ubigeo', filtro.ubigeo);
        }
        if (filtro.activa !== null) {
            params = params.set('activa', filtro.activa);
        }
        return this.http.get<Page<IglesiaResponse>>(this.iglesiasUrl, { params }).pipe(map(({ content, page }) => ({ content: content.map(aIglesia), page })));
    }

    obtener(id: number): Observable<Iglesia> {
        return this.http.get<IglesiaResponse>(this.url(id)).pipe(map(aIglesia));
    }

    crear(datos: DatosIglesia): Observable<Iglesia> {
        return this.http.post<IglesiaResponse>(this.iglesiasUrl, datos).pipe(map(aIglesia));
    }

    actualizar(id: number, datos: DatosIglesia): Observable<Iglesia> {
        return this.http.put<IglesiaResponse>(this.url(id), datos).pipe(map(aIglesia));
    }

    cambiarApariencia(id: number, apariencia: Apariencia): Observable<Apariencia> {
        return this.http.put<Apariencia>(`${this.url(id)}/apariencia`, apariencia);
    }

    /** El logo viaja con el token (no es una URL pública): se pide como `Blob`. */
    logo(id: number): Observable<Blob> {
        return this.http.get(`${this.url(id)}/logo`, { responseType: 'blob' });
    }

    subirLogo(id: number, archivo: Blob): Observable<void> {
        return this.http.put<void>(`${this.url(id)}/logo`, archivo, { headers: { 'Content-Type': archivo.type } });
    }

    quitarLogo(id: number): Observable<void> {
        return this.http.delete<void>(`${this.url(id)}/logo`);
    }

    /** La identidad de una iglesia del catálogo, para el editor compartido con «Mi iglesia». */
    fuente(id: number): FuenteIglesia {
        return {
            obtener: () => this.obtener(id),
            actualizar: (datos) => this.actualizar(id, datos),
            cambiarApariencia: (apariencia) => this.cambiarApariencia(id, apariencia),
            logo: () => this.logo(id),
            subirLogo: (archivo) => this.subirLogo(id, archivo),
            quitarLogo: () => this.quitarLogo(id)
        };
    }

    private url(id: number): string {
        return `${this.iglesiasUrl}/${id}`;
    }
}
