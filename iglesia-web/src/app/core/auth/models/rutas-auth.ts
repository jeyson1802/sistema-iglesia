/** Rutas de las páginas de autenticación. */
export const RUTAS_AUTH = {
    login: '/auth/login',
    seleccionarPerfil: '/auth/seleccionar-perfil',
    accesoDenegado: '/auth/acceso-denegado'
} as const;

/** Valores del parámetro `motivo` de la página de login, que decide el aviso que se muestra. */
export const MOTIVO_LOGIN = {
    sesionExpirada: 'sesion-expirada'
} as const;

/** Avisos que muestra la página de login según el parámetro `motivo`. */
export const AVISO_POR_MOTIVO: Readonly<Record<string, string>> = {
    [MOTIVO_LOGIN.sesionExpirada]: 'Tu sesión expiró. Inicia sesión de nuevo.'
};
