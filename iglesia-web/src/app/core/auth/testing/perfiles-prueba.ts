import { Perfil } from '../models/perfil';
import { Permiso, PERMISOS } from '../models/permiso';
import { ETIQUETA_ROL, Rol, ROLES } from '../models/rol';
import { UsuarioActual } from '../models/usuario-actual';

/** Permisos del catálogo de iglesias: solo los tiene el administrador del sistema. */
const PERMISOS_IGLESIAS: readonly Permiso[] = ['iglesias.ver', 'iglesias.administrar'];

/** Permisos que siembra Flyway para cada rol base. */
const PERMISOS_ROL_BASE: Record<Rol, readonly Permiso[]> = {
    ADMIN_SISTEMA: ['usuarios.ver', 'usuarios.administrar', 'roles.ver', ...PERMISOS_IGLESIAS],
    ADMIN_IGLESIA: PERMISOS.filter((permiso) => !PERMISOS_IGLESIAS.includes(permiso)),
    LIDER: ['miembros.ver', 'aportes.ver'],
    MIEMBRO: []
};

/** Perfil de un rol base, con el mismo id que le da Flyway (1 a 4, en el orden de `ROLES`). */
export const perfilBase = (rol: Rol): Perfil => ({
    id: ROLES.indexOf(rol) + 1,
    nombre: ETIQUETA_ROL[rol],
    rolBase: rol,
    permisos: PERMISOS_ROL_BASE[rol]
});

export const perfilPersonalizado = (id: number, nombre: string, rolBase: Rol, permisos: readonly Permiso[]): Perfil => ({
    id,
    nombre,
    rolBase,
    permisos
});

/** Usuario de prueba con esos perfiles; un `Rol` se convierte en el perfil de ese rol base. */
export const usuarioCon = (...perfiles: (Rol | Perfil)[]): UsuarioActual => ({
    id: 1,
    correo: 'u@i.test',
    iglesiaId: 1,
    miembroId: 2,
    debeCambiarContrasena: false,
    actuandoEnIglesia: false,
    perfiles: perfiles.map((perfil) => (typeof perfil === 'string' ? perfilBase(perfil) : perfil))
});
