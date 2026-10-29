import { DatosIglesia } from '@/app/core/iglesias/models/iglesia';

/** Datos del administrador de una iglesia que se crea desde el catálogo: tendrá el rol base «Administrador de iglesia». */
export interface NuevoAdministrador {
    readonly correo: string;
    /** El administrador deberá cambiarla al iniciar sesión. */
    readonly contrasenaTemporal: string;
}

/** Alta de una iglesia con su identidad junto con su primer administrador. */
export interface AltaIglesia {
    readonly datos: DatosIglesia;
    readonly administrador: NuevoAdministrador;
}
