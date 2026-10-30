/** Texto sin espacios sobrantes, o `null` si queda vacío (los backends tratan el vacío como «no indicado»). */
export const textoOpcional = (texto: string | null | undefined): string | null => texto?.trim() || null;

/** Texto sin tildes y en minúsculas, para comparar búsquedas. */
export function normalizarBusqueda(texto: string): string {
    return texto
        .normalize('NFD')
        .replace(/\p{Diacritic}/gu, '')
        .toLowerCase()
        .trim();
}

/** Si `texto` contiene `busqueda` sin distinguir tildes ni mayúsculas; una búsqueda vacía coincide con todo. */
export function coincideBusqueda(texto: string, busqueda: string): boolean {
    const buscado = normalizarBusqueda(busqueda);
    return buscado === '' || normalizarBusqueda(texto).includes(buscado);
}
