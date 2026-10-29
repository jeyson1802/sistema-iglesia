import { toDataURL } from 'qrcode';

/**
 * El QR siempre va en negro sobre blanco, con su margen de 4 módulos, aunque la iglesia use modo oscuro: así lo lee
 * cualquier lector y cualquier cámara. Se genera en el navegador; el backend solo entrega el texto.
 */
const NEGRO = '#000000';
const BLANCO = '#ffffff';
const MARGEN_MODULOS = 4;
/** Ancho en píxeles: suficiente para imprimir a 300 ppp un QR de 25 mm o más. */
export const ANCHO_QR_PX = 600;

export function generarQr(contenido: string, ancho: number = ANCHO_QR_PX): Promise<string> {
    return toDataURL(contenido, { errorCorrectionLevel: 'M', margin: MARGEN_MODULOS, width: ancho, color: { dark: NEGRO, light: BLANCO } });
}
