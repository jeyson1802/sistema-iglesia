import { Pipe, PipeTransform } from '@angular/core';

/** Idioma de los montos: separadores y símbolos como se leen en Perú (p. ej. «S/ 1,250.00»). */
const LOCALE_MONTOS = 'es-PE';

const formatos = new Map<string, Intl.NumberFormat>();

/** Formatea un monto en la moneda indicada (código ISO 4217), siempre con 2 decimales. */
export function formatearMonto(monto: number | null | undefined, moneda: string): string {
    if (monto === null || monto === undefined || Number.isNaN(Number(monto))) {
        return '';
    }
    let formato = formatos.get(moneda);
    if (!formato) {
        formato = new Intl.NumberFormat(LOCALE_MONTOS, { style: 'currency', currency: moneda, minimumFractionDigits: 2, maximumFractionDigits: 2 });
        formatos.set(moneda, formato);
    }
    return formato.format(Number(monto));
}

/** `monto | moneda: codigo` → «S/ 1,250.00», «US$ 80.00». */
@Pipe({ name: 'moneda' })
export class MonedaPipe implements PipeTransform {
    transform(monto: number | null | undefined, moneda: string): string {
        return formatearMonto(monto, moneda);
    }
}
