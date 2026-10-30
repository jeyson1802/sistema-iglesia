/** Pantallas por debajo del punto `md` de Tailwind: ahí las hojas salen desde abajo. */
export const CONSULTA_PANTALLA_PEQUENA = '(max-width: 767px)';

/** Si la pantalla es de celular; sin `matchMedia` (pruebas, servidor) se asume escritorio. */
export function esPantallaPequena(ventana: Window | null = globalThis.window ?? null): boolean {
    return ventana?.matchMedia?.(CONSULTA_PANTALLA_PEQUENA).matches ?? false;
}
