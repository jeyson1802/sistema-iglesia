/** Mismo formato que `CodigoAsistencia` de iglesia-api: 12 caracteres en base32 de Crockford, con prefijo en el QR. */
export const PREFIJO_CODIGO_ASISTENCIA = 'ASIS:';
const FORMATO = /^[0-9A-HJKMNP-TV-Z]{12}$/;
const SEPARADORES = /[\s-]/g;
const LARGO_GRUPO = 4;

/**
 * Código que contiene lo leído por la cámara o el lector (o escrito a mano), con o sin prefijo, en minúsculas o con
 * espacios y guiones; `null` si no es un código de asistencia.
 */
export function leerCodigoAsistencia(leido: string): string | null {
    let codigo = leido.trim().toUpperCase();
    if (codigo.startsWith(PREFIJO_CODIGO_ASISTENCIA)) {
        codigo = codigo.slice(PREFIJO_CODIGO_ASISTENCIA.length);
    }
    codigo = codigo.replace(SEPARADORES, '');
    return FORMATO.test(codigo) ? codigo : null;
}

/** Código en grupos de cuatro, como se imprime bajo el QR: `7K2Q M9XD R4TB`. */
export function formatearCodigoAsistencia(codigo: string): string {
    return codigo.match(new RegExp(`.{1,${LARGO_GRUPO}}`, 'g'))?.join(' ') ?? codigo;
}
