import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { CodigoAsistencia } from '@/app/features/miembros/models/miembro';
import {
    ConsultaPeriodo,
    EstadisticasAportes,
    EstadisticasAsistencia,
    EstadisticasConsolidacion,
    EstadisticasGrupos,
    EstadisticasMiembros,
    EstadisticasSistema,
    EstadisticasUso
} from '../models/estadisticas';
import { MiPerfil } from '../models/mi-perfil';

/**
 * Fachada HTTP de las páginas de inicio: estadísticas por módulo de iglesia-api (con el alcance del token), uso del
 * sistema de iglesia-seguridad y la información propia del miembro.
 */
@Injectable({ providedIn: 'root' })
export class EstadisticasService {
    private readonly http = inject(HttpClient);
    private readonly url = `${environment.apiUrl}/estadisticas`;
    private readonly perfilUrl = `${environment.apiUrl}/mi-perfil`;
    private readonly usoUrl = `${environment.seguridadUrl}/estadisticas/uso`;

    miembros(periodo: ConsultaPeriodo): Observable<EstadisticasMiembros> {
        return this.http.get<EstadisticasMiembros>(`${this.url}/miembros`, { params: this.params(periodo) });
    }

    grupos(periodo: ConsultaPeriodo): Observable<EstadisticasGrupos> {
        return this.http.get<EstadisticasGrupos>(`${this.url}/grupos`, { params: this.params(periodo) });
    }

    asistencia(periodo: ConsultaPeriodo): Observable<EstadisticasAsistencia> {
        return this.http.get<EstadisticasAsistencia>(`${this.url}/asistencia`, { params: this.params(periodo) });
    }

    consolidacion(periodo: ConsultaPeriodo): Observable<EstadisticasConsolidacion> {
        return this.http.get<EstadisticasConsolidacion>(`${this.url}/consolidacion`, { params: this.params(periodo) });
    }

    aportes(periodo: ConsultaPeriodo): Observable<EstadisticasAportes> {
        return this.http.get<EstadisticasAportes>(`${this.url}/aportes`, { params: this.params(periodo) });
    }

    sistema(periodo: ConsultaPeriodo): Observable<EstadisticasSistema> {
        return this.http.get<EstadisticasSistema>(`${this.url}/sistema`, { params: this.params(periodo) });
    }

    uso(periodo: ConsultaPeriodo): Observable<EstadisticasUso> {
        return this.http.get<EstadisticasUso>(this.usoUrl, { params: this.params(periodo) });
    }

    miPerfil(): Observable<MiPerfil> {
        return this.http.get<MiPerfil>(this.perfilUrl);
    }

    miAsistencia(): Observable<AsistenciaMiembro> {
        return this.http.get<AsistenciaMiembro>(`${this.perfilUrl}/asistencia`);
    }

    miCodigoAsistencia(): Observable<CodigoAsistencia> {
        return this.http.get<CodigoAsistencia>(`${this.perfilUrl}/codigo-asistencia`);
    }

    /** Las fechas solo viajan con «Personalizado»: las de los atajos las calcula el servidor con su reloj. */
    private params({ periodo, desde, hasta }: ConsultaPeriodo): HttpParams {
        const params = new HttpParams().set('periodo', periodo);
        return periodo === 'PERSONALIZADO' && desde && hasta ? params.set('desde', desde).set('hasta', hasta) : params;
    }
}
