import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { GuardarRol, PermisoCatalogo, PermisoResponse, RolAcceso, RolAccesoResponse } from '../models/rol-acceso';
import { aPermisosCatalogo, aRolAcceso, aRolesAcceso } from './rol-acceso.mapper';

/** Fachada HTTP de los roles de acceso y del catálogo de permisos de iglesia-seguridad. */
@Injectable({ providedIn: 'root' })
export class RolesService {
    private readonly http = inject(HttpClient);
    private readonly rolesUrl = `${environment.seguridadUrl}/roles`;
    private readonly permisosUrl = `${environment.seguridadUrl}/permisos`;

    /** Roles base y personalizados de la iglesia del usuario. */
    listar(): Observable<RolAcceso[]> {
        return this.http.get<RolAccesoResponse[]>(this.rolesUrl).pipe(map(aRolesAcceso));
    }

    obtener(id: number): Observable<RolAcceso> {
        return this.http.get<RolAccesoResponse>(`${this.rolesUrl}/${id}`).pipe(map(aRolRequerido));
    }

    permisos(): Observable<PermisoCatalogo[]> {
        return this.http.get<PermisoResponse[]>(this.permisosUrl).pipe(map(aPermisosCatalogo));
    }

    crear({ nombre, rolBaseId, permisos }: GuardarRol): Observable<RolAcceso> {
        return this.http.post<RolAccesoResponse>(this.rolesUrl, { nombre, rolBaseId, permisos }).pipe(map(aRolRequerido));
    }

    actualizar(id: number, { nombre, permisos }: GuardarRol): Observable<RolAcceso> {
        return this.http.put<RolAccesoResponse>(`${this.rolesUrl}/${id}`, { nombre, permisos }).pipe(map(aRolRequerido));
    }

    cambiarEstado(id: number, activo: boolean): Observable<RolAcceso> {
        return this.http.patch<RolAccesoResponse>(`${this.rolesUrl}/${id}/estado`, { activo }).pipe(map(aRolRequerido));
    }
}

/** Un rol que se acaba de crear o modificar siempre tiene un rol base conocido; si no, el contrato cambió. */
function aRolRequerido(respuesta: RolAccesoResponse): RolAcceso {
    const rol = aRolAcceso(respuesta);
    if (!rol) {
        throw new Error(`Rol base desconocido: ${respuesta.rolBase}`);
    }
    return rol;
}
