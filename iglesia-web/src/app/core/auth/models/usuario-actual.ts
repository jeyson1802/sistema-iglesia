import { Rol } from './rol';

/** Usuario autenticado, adaptado de la respuesta de `GET /api/usuarios/me`. */
export interface UsuarioActual {
    readonly id: number;
    readonly correo: string;
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    readonly roles: readonly Rol[];
}
