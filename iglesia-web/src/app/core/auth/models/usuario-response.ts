/** Contrato de un perfil en `GET /api/usuarios/me` (`PerfilResponse` en iglesia-seguridad). */
export interface PerfilResponse {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: string;
    readonly permisos: readonly string[];
}

/** Contrato de `GET /api/usuarios/me` en iglesia-seguridad (solo los campos que usa la UI). */
export interface UsuarioResponse {
    readonly id: number;
    readonly correo: string;
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    readonly debeCambiarContrasena: boolean;
    readonly actuandoEnIglesia: boolean;
    readonly perfiles: readonly PerfilResponse[];
}
