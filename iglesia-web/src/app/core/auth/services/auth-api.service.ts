import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '@/environments/environment';
import { CambioContrasena } from '../models/cambio-contrasena';
import { Credenciales } from '../models/credenciales';
import { TokenAcceso } from '../models/token-acceso';
import { UsuarioActual } from '../models/usuario-actual';
import { UsuarioResponse } from '../models/usuario-response';
import { aUsuarioActual } from './usuario-actual.mapper';

/** Acceso HTTP a iglesia-seguridad. El token de refresco viaja en una cookie HttpOnly que gestiona el navegador. */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
    private readonly http = inject(HttpClient);
    private readonly authUrl = `${environment.seguridadUrl}/auth`;
    private readonly usuariosUrl = `${environment.seguridadUrl}/usuarios`;

    /** `withCredentials` hace que el navegador acepte y envíe la cookie de refresco entre orígenes. */
    private readonly conCookie = { withCredentials: true } as const;

    login(credenciales: Credenciales): Observable<TokenAcceso> {
        return this.http.post<TokenAcceso>(`${this.authUrl}/login`, credenciales, this.conCookie);
    }

    refrescar(): Observable<TokenAcceso> {
        return this.http.post<TokenAcceso>(`${this.authUrl}/refresh`, null, this.conCookie);
    }

    cerrarSesion(): Observable<void> {
        return this.http.post<void>(`${this.authUrl}/logout`, null, this.conCookie);
    }

    obtenerUsuarioActual(): Observable<UsuarioActual> {
        return this.http.get<UsuarioResponse>(`${this.usuariosUrl}/me`).pipe(map(aUsuarioActual));
    }

    cambiarContrasena(cambio: CambioContrasena): Observable<void> {
        return this.http.put<void>(`${this.usuariosUrl}/me/contrasena`, cambio);
    }
}
