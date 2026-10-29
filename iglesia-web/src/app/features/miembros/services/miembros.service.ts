import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { EslabonCadena, FiltroMiembros, Liderado, GuardarMiembro, Miembro, MiembroResponse, MiembroResumen, PAGINA_MIEMBROS } from '../models/miembro';
import { aMiembro } from './miembro.mapper';

const ORDEN_LISTADO = ['apellidos,asc', 'nombres,asc'];
/** El pastor principal queda primero al ordenar por la marca de forma descendente. */
const ORDEN_PASTOR_PRIMERO = 'pastorPrincipal,desc';

/** Fachada HTTP del registro de miembros (`/api/miembros`). El backend filtra según el ámbito del token. */
@Injectable({ providedIn: 'root' })
export class MiembrosService {
    private readonly http = inject(HttpClient);
    private readonly miembrosUrl = `${environment.apiUrl}/miembros`;

    /** Página `pagina` (desde 0) ordenada por apellidos y nombres. Solo se envían los filtros con valor. */
    listar(filtro: FiltroMiembros, pagina: number): Observable<Page<MiembroResumen>> {
        return this.http.get<Page<MiembroResumen>>(this.miembrosUrl, { params: parametrosDeListado(filtro, pagina) });
    }

    obtener(id: number): Observable<Miembro> {
        return this.http.get<MiembroResponse>(`${this.miembrosUrl}/${id}`).pipe(map(aMiembro));
    }

    /** Líderes del miembro, del directo al pastor principal. */
    cadena(id: number): Observable<EslabonCadena[]> {
        return this.http.get<EslabonCadena[]>(`${this.miembrosUrl}/${id}/cadena`);
    }

    /** Liderados directos activos, con su nivel y cuántos lidera cada uno, para expandir el árbol de liderazgo. */
    liderados(id: number): Observable<Liderado[]> {
        return this.http.get<Liderado[]>(`${this.miembrosUrl}/${id}/liderados`);
    }

    /** Miembros con los mismos nombres y apellidos (sin distinguir mayúsculas ni espacios), para avisar antes de crear. */
    posiblesDuplicados(nombres: string, apellidos: string): Observable<MiembroResumen[]> {
        return this.http.get<MiembroResumen[]>(`${this.miembrosUrl}/posibles-duplicados`, { params: { nombres, apellidos } });
    }

    /** El pastor principal actual de la iglesia, o `null` si aún no hay. */
    pastorPrincipal(): Observable<MiembroResumen | null> {
        const parametros = new HttpParams().set('page', 0).set('size', 1).set('sort', ORDEN_PASTOR_PRIMERO);
        return this.http.get<Page<MiembroResumen>>(this.miembrosUrl, { params: parametros }).pipe(map(({ content }) => content.find(({ esPastorPrincipal }) => esPastorPrincipal) ?? null));
    }

    crear(datos: GuardarMiembro): Observable<Miembro> {
        return this.http.post<MiembroResponse>(this.miembrosUrl, datos).pipe(map(aMiembro));
    }

    actualizar(id: number, datos: GuardarMiembro): Observable<Miembro> {
        return this.http.put<MiembroResponse>(`${this.miembrosUrl}/${id}`, datos).pipe(map(aMiembro));
    }

    cambiarEstado(id: number, activo: boolean): Observable<Miembro> {
        return this.http.patch<MiembroResponse>(`${this.miembrosUrl}/${id}/estado`, { activo }).pipe(map(aMiembro));
    }

    designarPastorPrincipal(id: number): Observable<Miembro> {
        return this.http.put<MiembroResponse>(`${this.miembrosUrl}/${id}/pastor-principal`, null).pipe(map(aMiembro));
    }
}

function parametrosDeListado({ q, condicion, nivelId, liderId, ubigeo, activo }: FiltroMiembros, pagina: number): HttpParams {
    let parametros = new HttpParams().set('page', pagina).set('size', PAGINA_MIEMBROS);
    for (const orden of ORDEN_LISTADO) {
        parametros = parametros.append('sort', orden);
    }
    const opcionales: Record<string, string | number | boolean | null> = { q: q.trim() || null, condicion, nivelId, liderId, ubigeo, activo };
    for (const [nombre, valor] of Object.entries(opcionales)) {
        if (valor !== null) {
            parametros = parametros.set(nombre, valor);
        }
    }
    return parametros;
}
