import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { Aporte, APORTES_POR_PAGINA, FiltroAportes, GuardarAporte, ResumenAportes } from '../models/aporte';

/** Orden del listado: lo más reciente primero (fecha y luego momento del registro). */
const ORDEN_LISTADO = ['fecha,desc', 'registradoEn,desc'];

/** Fachada HTTP de los aportes (`/api/aportes`). El backend aplica el ámbito: un líder solo recibe los suyos. */
@Injectable({ providedIn: 'root' })
export class AportesService {
    private readonly http = inject(HttpClient);
    private readonly aportesUrl = `${environment.apiUrl}/aportes`;

    listar(filtro: FiltroAportes, pagina: number, tamano: number = APORTES_POR_PAGINA): Observable<Page<Aporte>> {
        let params = parametros(filtro).set('page', pagina).set('size', tamano);
        ORDEN_LISTADO.forEach((orden) => (params = params.append('sort', orden)));
        if (filtro.incluirAnulados) {
            params = params.set('incluirAnulados', true);
        }
        return this.http.get<Page<Aporte>>(this.aportesUrl, { params });
    }

    resumen(filtro: FiltroAportes): Observable<ResumenAportes> {
        return this.http.get<ResumenAportes>(`${this.aportesUrl}/resumen`, { params: parametros(filtro) });
    }

    obtener(id: number): Observable<Aporte> {
        return this.http.get<Aporte>(`${this.aportesUrl}/${id}`);
    }

    crear(aporte: GuardarAporte): Observable<Aporte> {
        return this.http.post<Aporte>(this.aportesUrl, aporte);
    }

    actualizar(id: number, aporte: GuardarAporte): Observable<Aporte> {
        return this.http.put<Aporte>(`${this.aportesUrl}/${id}`, aporte);
    }

    anular(id: number, motivo: string): Observable<Aporte> {
        return this.http.patch<Aporte>(`${this.aportesUrl}/${id}/anulacion`, { motivo });
    }
}

/** Rango y filtros opcionales; los vacíos no se envían. */
function parametros(filtro: FiltroAportes): HttpParams {
    let params = new HttpParams().set('desde', filtro.desde).set('hasta', filtro.hasta);
    if (filtro.categoriaId !== null) {
        params = params.set('categoriaId', filtro.categoriaId);
    }
    if (filtro.medio !== null) {
        params = params.set('medio', filtro.medio);
    }
    if (filtro.miembroId !== null) {
        params = params.set('miembroId', filtro.miembroId);
    }
    return params;
}
