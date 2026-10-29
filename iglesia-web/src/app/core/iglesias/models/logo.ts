/** Tipos de imagen admitidos para el logo (iglesia-api los verifica además por la firma del archivo). */
export const TIPOS_LOGO = ['image/png', 'image/jpeg', 'image/webp'] as const;
export const LOGO_MAX_BYTES = 1024 * 1024;
const BYTES_POR_MB = 1024 * 1024;

/** Motivo por el que no se puede subir un archivo como logo, en español; `null` si se puede. */
export function errorDeLogo(archivo: Pick<File, 'type' | 'size'>): string | null {
    if (!(TIPOS_LOGO as readonly string[]).includes(archivo.type)) {
        return 'Elige una imagen PNG, JPEG o WebP (SVG no está permitido).';
    }
    if (archivo.size > LOGO_MAX_BYTES) {
        return `La imagen pesa ${(archivo.size / BYTES_POR_MB).toFixed(1)} MB; elige una de hasta 1 MB.`;
    }
    return null;
}
