/** Contrato de `GET /api/usuarios/me` en iglesia-seguridad (solo los campos que usa la UI). */
export interface UsuarioResponse {
    readonly id: number;
    readonly correo: string;
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    readonly roles: readonly string[];
}
