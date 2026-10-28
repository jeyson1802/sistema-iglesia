import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Page } from '@/app/core/models/page';
import { CrearUsuario, EditarUsuario, FiltroUsuarios, PAGINA_USUARIOS, UsuarioAdmin, UsuarioAdminResponse } from '../models/usuario-admin';
import { aPaginaUsuarios, aUsuarioAdmin } from './usuario-admin.mapper';

const ORDEN_LISTADO = 'correo,asc';

/**
 * Fachada HTTP de la administración de usuarios de iglesia-seguridad. Esta spec no vincula usuarios con
 * miembros: al crear se envía `miembroId` nulo y al editar se reenvía el que ya tenía.
 */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
    private readonly http = inject(HttpClient);
    private readonly sesion = inject(SesionService);
    private readonly usuariosUrl = `${environment.seguridadUrl}/usuarios`;

    /** Página `pagina` (desde 0) ordenada por correo. Solo se envían los filtros con valor. */
    listar(filtro: FiltroUsuarios, pagina: number): Observable<Page<UsuarioAdmin>> {
        return this.http.get<Page<UsuarioAdminResponse>>(this.usuariosUrl, { params: parametrosDeListado(filtro, pagina) }).pipe(map(aPaginaUsuarios));
    }

    /** Crea el usuario en la iglesia del usuario autenticado, con una contraseña temporal. */
    crear({ correo, contrasenaTemporal, rolIds }: CrearUsuario): Observable<UsuarioAdmin> {
        const iglesiaId = this.sesion.usuarioActual()?.iglesiaId ?? null;
        return this.http.post<UsuarioAdminResponse>(this.usuariosUrl, { correo, contrasena: contrasenaTemporal, iglesiaId, miembroId: null, rolIds }).pipe(map(aUsuarioAdmin));
    }

    actualizar(id: number, { correo, rolIds, activo }: EditarUsuario, miembroIdActual: number | null): Observable<UsuarioAdmin> {
        return this.http.put<UsuarioAdminResponse>(`${this.usuariosUrl}/${id}`, { correo, rolIds, miembroId: miembroIdActual, activo }).pipe(map(aUsuarioAdmin));
    }

    restablecerContrasena(id: number, contrasenaTemporal: string): Observable<void> {
        return this.http.put<void>(`${this.usuariosUrl}/${id}/contrasena`, { contrasenaTemporal });
    }
}

function parametrosDeListado({ correo, activo, rolId }: FiltroUsuarios, pagina: number): HttpParams {
    let parametros = new HttpParams().set('page', pagina).set('size', PAGINA_USUARIOS).set('sort', ORDEN_LISTADO);
    const correoBuscado = correo.trim();
    if (correoBuscado) {
        parametros = parametros.set('correo', correoBuscado);
    }
    if (activo !== null) {
        parametros = parametros.set('activo', activo);
    }
    if (rolId !== null) {
        parametros = parametros.set('rolId', rolId);
    }
    return parametros;
}
