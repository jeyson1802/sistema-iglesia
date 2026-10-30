/** Código de país que se antepone a los números locales para WhatsApp (los distritos del sistema son del Perú). */
export const CODIGO_PAIS_POR_DEFECTO = '51';

/** Largo de un número local sin código de país. */
const DIGITOS_NUMERO_LOCAL = 9;

/** Solo los dígitos del teléfono; vacío si no tiene ninguno. */
export function digitosTelefono(telefono: string | null | undefined): string {
    return (telefono ?? '').replace(/\D/g, '');
}

/** Si el teléfono tiene dígitos suficientes para llamar o escribir. */
export function tieneTelefono(telefono: string | null | undefined): boolean {
    return digitosTelefono(telefono).length >= DIGITOS_NUMERO_LOCAL - 2;
}

/** Número internacional sin «+»: a un número local de 9 dígitos se le antepone el código del país. */
export function telefonoInternacional(telefono: string | null | undefined): string {
    const digitos = digitosTelefono(telefono);
    return digitos.length === DIGITOS_NUMERO_LOCAL ? `${CODIGO_PAIS_POR_DEFECTO}${digitos}` : digitos;
}

/** Enlace `tel:` para llamar desde el celular. */
export function enlaceLlamada(telefono: string | null | undefined): string {
    return `tel:+${telefonoInternacional(telefono)}`;
}

/** Enlace de WhatsApp (`https://wa.me/<número>`). */
export function enlaceWhatsapp(telefono: string | null | undefined): string {
    return `https://wa.me/${telefonoInternacional(telefono)}`;
}
