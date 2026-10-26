/** Rutas de las páginas de autenticación. */
export const RUTAS_AUTH = {
    login: '/auth/login',
    seleccionarPerfil: '/auth/seleccionar-perfil',
    accesoDenegado: '/auth/acceso-denegado'
} as const;

/** Valores del parámetro `motivo` de la página de login, que decide el aviso que se muestra. */
export const MOTIVO_LOGIN = {
    sesionExpirada: 'sesion-expirada',
    contrasenaCambiada: 'contrasena-cambiada'
} as const;

export interface AvisoLogin {
    readonly mensaje: string;
    readonly severidad: 'info' | 'success';
}

/** Avisos que muestra la página de login según el parámetro `motivo`. */
export const AVISO_POR_MOTIVO: Readonly<Record<string, AvisoLogin>> = {
    [MOTIVO_LOGIN.sesionExpirada]: { mensaje: 'Tu sesión expiró. Inicia sesión de nuevo.', severidad: 'info' },
    [MOTIVO_LOGIN.contrasenaCambiada]: { mensaje: 'Contraseña actualizada. Inicia sesión de nuevo.', severidad: 'success' }
};
