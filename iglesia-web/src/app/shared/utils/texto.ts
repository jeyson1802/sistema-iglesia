/** Texto sin espacios sobrantes, o `null` si queda vacío (los backends tratan el vacío como «no indicado»). */
export const textoOpcional = (texto: string | null | undefined): string | null => texto?.trim() || null;
