/** Cuerpo de `PUT /api/usuarios/me/contrasena`. */
export interface CambioContrasena {
    readonly contrasenaActual: string;
    readonly contrasenaNueva: string;
}
