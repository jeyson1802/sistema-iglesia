/** Respuesta de login y refresco. El token de refresco viaja en una cookie HttpOnly, no aquí. */
export interface TokenAcceso {
    readonly tokenAcceso: string;
    readonly tipo: 'Bearer';
    readonly expiraEn: number; // segundos
}
