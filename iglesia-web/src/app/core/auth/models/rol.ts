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

/** Ícono de PrimeIcons que representa a cada perfil en la interfaz. */
export const ICONO_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: 'pi pi-server',
    ADMIN_IGLESIA: 'pi pi-building-columns',
    LIDER: 'pi pi-sitemap',
    MIEMBRO: 'pi pi-user'
};

/** Página de inicio de cada perfil después de iniciar sesión. */
export const RUTA_INICIO_POR_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: '/sistema',
    ADMIN_IGLESIA: '/administracion',
    LIDER: '/mi-red',
    MIEMBRO: '/mi-perfil'
};
