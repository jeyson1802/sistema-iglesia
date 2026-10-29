/** Fecha local de hoy como `yyyy-MM-dd`, el formato de `<input type="date">` y de las fechas sin hora de la API. */
export function hoyIso(ahora: Date = new Date()): string {
    const dosDigitos = (valor: number): string => String(valor).padStart(2, '0');
    return `${ahora.getFullYear()}-${dosDigitos(ahora.getMonth() + 1)}-${dosDigitos(ahora.getDate())}`;
}
