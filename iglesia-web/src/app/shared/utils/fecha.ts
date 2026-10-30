/** Fecha local de hoy como `yyyy-MM-dd`, el formato de `<input type="date">` y de las fechas sin hora de la API. */
export function hoyIso(ahora: Date = new Date()): string {
    const dosDigitos = (valor: number): string => String(valor).padStart(2, '0');
    return `${ahora.getFullYear()}-${dosDigitos(ahora.getMonth() + 1)}-${dosDigitos(ahora.getDate())}`;
}

/** Fecha `yyyy-MM-dd` con el día de la semana en español: «dom., 25/10/2026». */
export function fechaConDia(iso: string): string {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia).toLocaleDateString('es', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' });
}

/** Fecha de hoy en palabras, en español: «jueves, 29 de octubre». */
export function hoyEnPalabras(ahora: Date = new Date()): string {
    return ahora.toLocaleDateString('es', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** Años cumplidos a una fecha `yyyy-MM-dd` (la edad no se guarda: se calcula de la fecha de nacimiento). */
export function edadDesde(nacimiento: string, ahora: Date = new Date()): number {
    const [anio, mes, dia] = nacimiento.split('-').map(Number);
    const yaCumplio = ahora.getMonth() + 1 > mes || (ahora.getMonth() + 1 === mes && ahora.getDate() >= dia);
    return ahora.getFullYear() - anio - (yaCumplio ? 0 : 1);
}
