import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { Page } from '@/app/core/models/page';
import { CodigoAsistencia, EslabonCadena, FiltroMiembros, Fotocheck, Liderado, GuardarMiembro, Miembro, MiembroResponse, MiembroResumen, PAGINA_MIEMBROS } from '../models/miembro';
import { aMiembro } from './miembro.mapper';

const ORDEN_LISTADO = ['apellidos,asc', 'nombres,asc'];
/** El pastor principal queda primero al ordenar por la marca de forma descendente. */
const ORDEN_PASTOR_PRIMERO = 'pastorPrincipal,desc';

/** Fachada HTTP del registro de miembros (`/api/miembros`). El backend filtra según el ámbito del token. */
@Injectable({ providedIn: 'root' })
export class MiembrosService {
    private readonly http = inject(HttpClient);
    private readonly miembrosUrl = `${environment.apiUrl}/miembros`;

    /** Página `pagina` (desde 0) de `tamano` registros, ordenada por apellidos y nombres. Solo se envían los filtros con valor. */
    listar(filtro: FiltroMiembros, pagina: number, tamano: number = PAGINA_MIEMBROS): Observable<Page<MiembroResumen>> {
        return this.http.get<Page<MiembroResumen>>(this.miembrosUrl, { params: parametrosDeListado(filtro, pagina, tamano) });
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

    /** Código del QR de asistencia (el backend lo genera si el miembro aún no tiene). */
    codigoAsistencia(id: number): Observable<CodigoAsistencia> {
        return this.http.get<CodigoAsistencia>(`${this.miembrosUrl}/${id}/codigo-asistencia`);
    }

    /** Reemplaza el código: el fotocheck anterior deja de valer. */
    regenerarCodigoAsistencia(id: number): Observable<CodigoAsistencia> {
        return this.http.post<CodigoAsistencia>(`${this.miembrosUrl}/${id}/codigo-asistencia`, null);
    }

    /** Fotochecks de los miembros que cumplen el filtro del listado (hasta 500), para imprimir en lote. */
    fotochecks(filtro: FiltroMiembros): Observable<Fotocheck[]> {
        return this.http.get<Fotocheck[]>(`${this.miembrosUrl}/fotochecks`, { params: parametrosDeFiltro(filtro) });
    }
}

function parametrosDeListado(filtro: FiltroMiembros, pagina: number, tamano: number): HttpParams {
    let parametros = parametrosDeFiltro(filtro).set('page', pagina).set('size', tamano);
    for (const orden of ORDEN_LISTADO) {
        parametros = parametros.append('sort', orden);
    }
    return parametros;
}

/** Solo los filtros con valor. */
function parametrosDeFiltro({ q, condicion, nivelId, liderId, ubigeo, activo }: FiltroMiembros): HttpParams {
    let parametros = new HttpParams();
    const opcionales: Record<string, string | number | boolean | null> = { q: q.trim() || null, condicion, nivelId, liderId, ubigeo, activo };
    for (const [nombre, valor] of Object.entries(opcionales)) {
        if (valor !== null) {
            parametros = parametros.set(nombre, valor);
        }
    }
    return parametros;
}
