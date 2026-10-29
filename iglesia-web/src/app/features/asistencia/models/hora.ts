/** `HH:mm[:ss]` → fecha de hoy a esa hora (para `p-datepicker` en modo hora). */
export function horaADate(hora: string | null): Date | null {
    if (hora === null) {
        return null;
    }
    const [horas, minutos] = hora.split(':').map(Number);
    const fecha = new Date();
    fecha.setHours(horas, minutos, 0, 0);
    return fecha;
}

/** Fecha → `HH:mm`, el formato que espera la API. */
export function dateAHora(fecha: Date | null): string | null {
    if (fecha === null) {
        return null;
    }
    const dosDigitos = (valor: number): string => String(valor).padStart(2, '0');
    return `${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`;
}
