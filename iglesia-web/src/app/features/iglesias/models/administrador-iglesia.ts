/** Datos del administrador de una iglesia que se crea desde el catálogo: tendrá el rol base «Administrador de iglesia». */
export interface NuevoAdministrador {
    readonly correo: string;
    /** El administrador deberá cambiarla al iniciar sesión. */
    readonly contrasenaTemporal: string;
}

/** Alta de una iglesia junto con su primer administrador. */
export interface AltaIglesia {
    readonly nombre: string;
    readonly administrador: NuevoAdministrador;
}

export type ModoFormularioIglesia = 'crear' | 'editar';
