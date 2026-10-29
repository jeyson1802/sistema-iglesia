import { Injectable } from '@angular/core';
import { leerCodigoAsistencia } from '@/app/shared/utils/codigo-asistencia';

/** La cámara lee el mismo QR varias veces por segundo: se ignora durante este tiempo. */
export const ESPERA_MISMO_CODIGO_MS = 3000;

export type Lectura = { readonly tipo: 'enviar'; readonly codigo: string } | { readonly tipo: 'repetida' } | { readonly tipo: 'invalida' };

/**
 * Recibe lo leído por la cámara o el lector y decide qué hacer: enviar el código, ignorar una relectura del mismo
 * código (antes de {@link ESPERA_MISMO_CODIGO_MS}) o avisar que no es un código de asistencia. Se provee por pantalla.
 */
@Injectable()
export class ProcesadorCodigos {
    private ultimo: { readonly codigo: string; readonly en: number } | null = null;

    procesar(leido: string, ahora: number = Date.now()): Lectura {
        const codigo = leerCodigoAsistencia(leido);
        if (codigo === null) {
            return { tipo: 'invalida' };
        }
        if (this.ultimo?.codigo === codigo && ahora - this.ultimo.en < ESPERA_MISMO_CODIGO_MS) {
            return { tipo: 'repetida' };
        }
        this.ultimo = { codigo, en: ahora };
        return { tipo: 'enviar', codigo };
    }

    /** Tras «Deshacer», el mismo fotocheck se puede volver a leer al instante. */
    olvidar(): void {
        this.ultimo = null;
    }
}
