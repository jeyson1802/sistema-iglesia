/**
 * `localStorage` si el navegador deja usarlo; `null` en modo privado o con el almacenamiento bloqueado. Solo para
 * comodidades por usuario: quien lo use debe funcionar igual sin él.
 */
export function almacenLocal(): Storage | null {
    try {
        return globalThis.localStorage ?? null;
    } catch {
        return null;
    }
}

/** Lee una clave sin lanzar: cualquier fallo del navegador equivale a no tener nada guardado. */
export function leerLocal(clave: string, almacen: Storage | null = almacenLocal()): string | null {
    try {
        return almacen?.getItem(clave) ?? null;
    } catch {
        return null;
    }
}

/** Escribe una clave sin lanzar: si no se puede, simplemente no se recuerda. */
export function guardarLocal(clave: string, valor: string, almacen: Storage | null = almacenLocal()): void {
    try {
        almacen?.setItem(clave, valor);
    } catch {
        // Recordar es una comodidad: sin almacenamiento la aplicación sigue igual.
    }
}
