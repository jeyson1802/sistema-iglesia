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
