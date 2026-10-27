import { Permiso } from './permiso';
import { Rol } from './rol';

/**
 * Perfil con el que navega el usuario: uno de sus roles activos, base o personalizado. Su rol base define la
 * página de inicio; sus permisos, el menú y los guards.
 */
export interface Perfil {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: Rol;
    readonly permisos: readonly Permiso[];
}
