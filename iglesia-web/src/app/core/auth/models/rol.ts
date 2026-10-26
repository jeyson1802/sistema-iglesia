/** Roles emitidos por iglesia-seguridad. El orden define la jerarquía: el primero es el mayor. */
export const ROLES = ['ADMIN_SISTEMA', 'ADMIN_IGLESIA', 'LIDER', 'MIEMBRO'] as const;
export type Rol = (typeof ROLES)[number];

export const esRol = (valor: string): valor is Rol => (ROLES as readonly string[]).includes(valor);

export const ETIQUETA_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: 'Administrador del sistema',
    ADMIN_IGLESIA: 'Administrador de iglesia',
    LIDER: 'Líder',
    MIEMBRO: 'Miembro'
};

/** Página de inicio de cada perfil después de iniciar sesión. */
export const RUTA_INICIO_POR_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: '/sistema',
    ADMIN_IGLESIA: '/administracion',
    LIDER: '/mi-red',
    MIEMBRO: '/mi-perfil'
};
