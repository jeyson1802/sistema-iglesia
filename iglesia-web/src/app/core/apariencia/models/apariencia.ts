import { NombrePreset } from './paletas';

/** Contrato de `AparienciaResponse` y `GuardarAparienciaRequest` en iglesia-api. */
export interface Apariencia {
    readonly preset: NombrePreset;
    readonly colorPrimario: string;
    /** `null`: la superficie propia del preset. */
    readonly colorSuperficie: string | null;
    readonly tipografia: NombreTipografia;
}

export interface Tipografia {
    readonly nombre: string;
    /** Familia y peso en la sintaxis de Google Fonts (`css2?family=…`). */
    readonly familiaGoogle: string;
    /** Familia genérica de respaldo mientras carga o si no hay conexión. */
    readonly respaldo: 'serif' | 'sans-serif';
    /** Clase de Tailwind con la fuente fija, para la vista previa de cada fuente en el selector. */
    readonly clase: string;
}

/** Las cinco fuentes de marca admitidas (las mismas que valida iglesia-api); solo se carga el peso que se usa. */
export const TIPOGRAFIAS = [
    { nombre: 'Playfair Display', familiaGoogle: 'Playfair+Display:wght@600', respaldo: 'serif', clase: "font-['Playfair_Display',serif]" },
    { nombre: 'Cinzel', familiaGoogle: 'Cinzel:wght@600', respaldo: 'serif', clase: "font-['Cinzel',serif]" },
    { nombre: 'Montserrat', familiaGoogle: 'Montserrat:wght@600', respaldo: 'sans-serif', clase: "font-['Montserrat',sans-serif]" },
    { nombre: 'Merriweather', familiaGoogle: 'Merriweather:wght@700', respaldo: 'serif', clase: "font-['Merriweather',serif]" },
    { nombre: 'Poppins', familiaGoogle: 'Poppins:wght@600', respaldo: 'sans-serif', clase: "font-['Poppins',sans-serif]" }
] as const satisfies readonly Tipografia[];
export type NombreTipografia = (typeof TIPOGRAFIAS)[number]['nombre'];

/** Apariencia del sistema: la del login, la del administrador del sistema sin iglesia y la de una iglesia nueva. */
export const APARIENCIA_SISTEMA: Apariencia = { preset: 'Aura', colorPrimario: 'emerald', colorSuperficie: null, tipografia: 'Playfair Display' };

export const tipografiaDe = (nombre: string): Tipografia => TIPOGRAFIAS.find((tipografia) => tipografia.nombre === nombre) ?? TIPOGRAFIAS[0];

/** Valor CSS de `font-family` con su respaldo, p. ej. `'Playfair Display', serif`. */
export const familiaCss = (nombre: string): string => {
    const tipografia = tipografiaDe(nombre);
    return `'${tipografia.nombre}', ${tipografia.respaldo}`;
};
