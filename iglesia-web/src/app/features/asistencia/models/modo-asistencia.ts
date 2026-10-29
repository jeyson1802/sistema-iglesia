import { guardarLocal, leerLocal } from '@/app/shared/utils/almacen-local';
import { TipoReunion } from './asistencia';

export const MODOS_ASISTENCIA = ['CAMARA', 'LECTOR', 'MANUAL'] as const;
export type ModoAsistencia = (typeof MODOS_ASISTENCIA)[number];

export const OPCIONES_MODO: readonly { readonly valor: ModoAsistencia; readonly etiqueta: string; readonly icono: string }[] = [
    { valor: 'CAMARA', etiqueta: 'Cámara', icono: 'pi pi-camera' },
    { valor: 'LECTOR', etiqueta: 'Lector', icono: 'pi pi-barcode' },
    { valor: 'MANUAL', etiqueta: 'Manual', icono: 'pi pi-list-check' }
];

const PREFIJO_CLAVE = 'asistencia.modo.';

const esModo = (valor: string | null): valor is ModoAsistencia => MODOS_ASISTENCIA.includes(valor as ModoAsistencia);

/**
 * Modo con que se abre la pantalla: el último elegido para ese tipo de reunión en este navegador o, si no hay,
 * «Manual» en los grupos, «Cámara» en el celular y «Lector» en una computadora.
 */
export function modoInicial(tipo: TipoReunion, esCelular: boolean, almacen?: Storage | null): ModoAsistencia {
    const recordado = leerLocal(PREFIJO_CLAVE + tipo, almacen);
    if (esModo(recordado)) {
        return recordado;
    }
    if (tipo === 'GRUPO') {
        return 'MANUAL';
    }
    return esCelular ? 'CAMARA' : 'LECTOR';
}

export function recordarModo(tipo: TipoReunion, modo: ModoAsistencia, almacen?: Storage | null): void {
    guardarLocal(PREFIJO_CLAVE + tipo, modo, almacen);
}
