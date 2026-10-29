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
 * Fachada HTTP de la administración de usuarios de iglesia-seguridad. Desde `/usuarios` se crean sin miembro
 * (`miembroId` nulo) y al editar se reenvía el que ya tenía; la ficha del miembro crea el usuario vinculado.
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
    crear(datos: CrearUsuario): Observable<UsuarioAdmin> {
        return this.crearEnIglesia(this.sesion.usuarioActual()?.iglesiaId ?? null, datos);
    }

    /** Crea el usuario en la iglesia indicada; solo el administrador del sistema en su vista global elige otra. */
    crearEnIglesia(iglesiaId: number | null, datos: CrearUsuario): Observable<UsuarioAdmin> {
        return this.enviarAlta(iglesiaId, null, datos);
    }

    /** Crea, en la iglesia del usuario autenticado, el usuario vinculado a ese miembro (único por iglesia: 409 si ya tiene). */
    crearParaMiembro(miembroId: number, datos: CrearUsuario): Observable<UsuarioAdmin> {
        return this.enviarAlta(this.sesion.usuarioActual()?.iglesiaId ?? null, miembroId, datos);
    }

    /** El usuario vinculado al miembro, o `null` si aún no tiene acceso al sistema. */
    usuarioDeMiembro(miembroId: number): Observable<UsuarioAdmin | null> {
        const parametros = new HttpParams().set('miembroId', miembroId).set('page', 0).set('size', 1);
        return this.http.get<Page<UsuarioAdminResponse>>(this.usuariosUrl, { params: parametros }).pipe(map(({ content }) => (content.length ? aUsuarioAdmin(content[0]) : null)));
    }

    actualizar(id: number, { correo, rolIds, activo }: EditarUsuario, miembroIdActual: number | null): Observable<UsuarioAdmin> {
        return this.http.put<UsuarioAdminResponse>(`${this.usuariosUrl}/${id}`, { correo, rolIds, miembroId: miembroIdActual, activo }).pipe(map(aUsuarioAdmin));
    }

    restablecerContrasena(id: number, contrasenaTemporal: string): Observable<void> {
        return this.http.put<void>(`${this.usuariosUrl}/${id}/contrasena`, { contrasenaTemporal });
    }

    private enviarAlta(iglesiaId: number | null, miembroId: number | null, { correo, contrasenaTemporal, rolIds }: CrearUsuario): Observable<UsuarioAdmin> {
        return this.http.post<UsuarioAdminResponse>(this.usuariosUrl, { correo, contrasena: contrasenaTemporal, iglesiaId, miembroId, rolIds }).pipe(map(aUsuarioAdmin));
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
