import { effect, signal, WritableSignal } from '@angular/core';
import { guardarLocal, leerLocal } from '@/app/shared/utils/almacen-local';
import { PERIODOS } from '@/app/shared/utils/periodo';
import { ConsultaPeriodo, PERIODO_POR_DEFECTO } from '../models/estadisticas';

const CLAVE = 'inicio.periodo';
const FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Periodo del selector, recordado en el navegador (solo una comodidad: sin almacenamiento empieza en «Mes»). Un rango
 * personalizado se recuerda con sus fechas. Debe llamarse en un contexto de inyección.
 */
export function periodoRecordado(): WritableSignal<ConsultaPeriodo> {
    const periodo = signal<ConsultaPeriodo>(leer(leerLocal(CLAVE)));
    effect(() => guardarLocal(CLAVE, JSON.stringify(periodo())));
    return periodo;
}

/** Lo guardado, si es un periodo válido; cualquier otra cosa (o un valor de una versión anterior) vuelve a «Mes». */
function leer(guardado: string | null): ConsultaPeriodo {
    try {
        const { periodo, desde, hasta } = JSON.parse(guardado ?? 'null') ?? {};
        if (periodo === 'PERSONALIZADO') {
            return FECHA_ISO.test(desde) && FECHA_ISO.test(hasta) && desde <= hasta ? { periodo, desde, hasta } : PERIODO_POR_DEFECTO;
        }
        return PERIODOS.includes(periodo) ? { periodo } : PERIODO_POR_DEFECTO;
    } catch {
        return PERIODO_POR_DEFECTO;
    }
}
