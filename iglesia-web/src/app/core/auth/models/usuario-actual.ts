import { Perfil } from './perfil';

/** Usuario autenticado, adaptado de la respuesta de `GET /api/usuarios/me`. */
export interface UsuarioActual {
    readonly id: number;
    readonly correo: string;
    /** Iglesia en la que opera: la suya o, para el administrador del sistema, la iglesia a la que entró. */
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    /** La contraseña es temporal (la eligió un administrador): debe cambiarla antes de usar el sistema. */
    readonly debeCambiarContrasena: boolean;
    /** Es el administrador del sistema operando dentro de una iglesia (su perfil lleva los permisos de esa iglesia). */
    readonly actuandoEnIglesia: boolean;
    /** Un perfil por cada rol activo, ordenados por la jerarquía de su rol base y luego por nombre. */
    readonly perfiles: readonly Perfil[];
}
