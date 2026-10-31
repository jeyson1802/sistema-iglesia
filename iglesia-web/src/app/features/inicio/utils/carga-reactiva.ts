import { computed, DestroyRef, effect, inject, Signal, signal, untracked } from '@angular/core';
import { finalize, Observable, Subscription } from 'rxjs';

/** Estado de una carga: datos, cargando, el error (si falló) y cómo reintentar. */
export interface CargaReactiva<T> {
    readonly datos: Signal<T | null>;
    readonly cargando: Signal<boolean>;
    readonly error: Signal<boolean>;
    readonly fallo: Signal<unknown>;
    recargar(): void;
}

/**
 * Pide los datos cada vez que cambia `parametro` (p. ej. el periodo), mientras `activa` sea verdadera: sin permiso
 * no se hace la petición. Cancela la petición anterior y se limpia al destruir el componente. Cada carga es
 * independiente: si una falla, las demás siguen. Debe llamarse en un contexto de inyección.
 */
export function cargaReactiva<P, T>(parametro: Signal<P>, pedir: (valor: P) => Observable<T>, activa: Signal<boolean> = signal(true)): CargaReactiva<T> {
    const datos = signal<T | null>(null);
    const cargando = signal(false);
    const fallo = signal<unknown>(null);
    let suscripcion: Subscription | undefined;

    const cargar = (valor: P): void => {
        suscripcion?.unsubscribe();
        cargando.set(true);
        fallo.set(null);
        suscripcion = pedir(valor)
            .pipe(finalize(() => cargando.set(false)))
            .subscribe({ next: (respuesta) => datos.set(respuesta), error: (error: unknown) => fallo.set(error ?? true) });
    };

    inject(DestroyRef).onDestroy(() => suscripcion?.unsubscribe());
    effect(() => {
        if (activa()) {
            const valor = parametro();
            untracked(() => cargar(valor));
        }
    });

    return {
        datos: datos.asReadonly(),
        cargando: cargando.asReadonly(),
        error: computed(() => fallo() !== null),
        fallo: fallo.asReadonly(),
        recargar: () => cargar(untracked(parametro))
    };
}

/** Parámetro fijo, para las cargas que no dependen de nada (se piden una vez). */
export const SIN_PARAMETRO: Signal<null> = signal(null).asReadonly();
