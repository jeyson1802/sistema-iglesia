import { almacenLocal } from '@/app/shared/utils/almacen-local';
import { esMedioAporte, MedioAporte } from './aporte';

const CLAVE = 'aportes.ultimaSeleccion';

/** Última categoría y medio usados al registrar: comodidad por usuario que vive solo en este navegador. */
export interface UltimaSeleccion {
    readonly categoriaId: number | null;
    readonly medio: MedioAporte | null;
}

const VACIA: UltimaSeleccion = { categoriaId: null, medio: null };

/** Si el navegador no deja leer (modo privado, almacenamiento bloqueado) o el dato no se entiende, no recuerda nada. */
export function leerUltimaSeleccion(almacen: Storage | null = almacenLocal()): UltimaSeleccion {
    try {
        const guardada = JSON.parse(almacen?.getItem(CLAVE) ?? 'null') as Partial<UltimaSeleccion> | null;
        return {
            categoriaId: typeof guardada?.categoriaId === 'number' ? guardada.categoriaId : null,
            medio: esMedioAporte(guardada?.medio ?? null) ? (guardada?.medio as MedioAporte) : null
        };
    } catch {
        return VACIA;
    }
}

export function recordarSeleccion(seleccion: UltimaSeleccion, almacen: Storage | null = almacenLocal()): void {
    try {
        almacen?.setItem(CLAVE, JSON.stringify(seleccion));
    } catch {
        // Recordar es solo una comodidad: si no se puede escribir, el formulario sigue igual.
    }
}
