/**
 * Muestras de color del selector de apariencia: nombre en español (para `aria-label`) y clase de Tailwind escrita
 * completa para que Tailwind la genere. Los tonos son el 500 de cada paleta, como en el configurador de Sakai.
 */
export interface Muestra {
    readonly valor: string;
    readonly etiqueta: string;
    readonly clase: string;
}

export const MUESTRAS_PRIMARIAS: readonly Muestra[] = [
    { valor: 'noir', etiqueta: 'Negro', clase: 'bg-surface-950 dark:bg-surface-0' },
    { valor: 'emerald', etiqueta: 'Esmeralda', clase: 'bg-emerald-500' },
    { valor: 'green', etiqueta: 'Verde', clase: 'bg-green-500' },
    { valor: 'lime', etiqueta: 'Lima', clase: 'bg-lime-500' },
    { valor: 'orange', etiqueta: 'Naranja', clase: 'bg-orange-500' },
    { valor: 'amber', etiqueta: 'Ámbar', clase: 'bg-amber-500' },
    { valor: 'yellow', etiqueta: 'Amarillo', clase: 'bg-yellow-500' },
    { valor: 'teal', etiqueta: 'Verde azulado', clase: 'bg-teal-500' },
    { valor: 'cyan', etiqueta: 'Cian', clase: 'bg-cyan-500' },
    { valor: 'sky', etiqueta: 'Celeste', clase: 'bg-sky-500' },
    { valor: 'blue', etiqueta: 'Azul', clase: 'bg-blue-500' },
    { valor: 'indigo', etiqueta: 'Índigo', clase: 'bg-indigo-500' },
    { valor: 'violet', etiqueta: 'Violeta', clase: 'bg-violet-500' },
    { valor: 'purple', etiqueta: 'Púrpura', clase: 'bg-purple-500' },
    { valor: 'fuchsia', etiqueta: 'Fucsia', clase: 'bg-fuchsia-500' },
    { valor: 'pink', etiqueta: 'Rosa', clase: 'bg-pink-500' },
    { valor: 'rose', etiqueta: 'Rosa intenso', clase: 'bg-rose-500' }
];

export const MUESTRAS_SUPERFICIE: readonly Muestra[] = [
    { valor: 'slate', etiqueta: 'Pizarra', clase: 'bg-[#64748b]' },
    { valor: 'gray', etiqueta: 'Gris', clase: 'bg-[#6b7280]' },
    { valor: 'zinc', etiqueta: 'Cinc', clase: 'bg-[#71717a]' },
    { valor: 'neutral', etiqueta: 'Neutro', clase: 'bg-[#737373]' },
    { valor: 'stone', etiqueta: 'Piedra', clase: 'bg-[#78716c]' },
    { valor: 'soho', etiqueta: 'Soho', clase: 'bg-[#7f8084]' },
    { valor: 'viva', etiqueta: 'Viva', clase: 'bg-[#87898a]' },
    { valor: 'ocean', etiqueta: 'Océano', clase: 'bg-[#828787]' }
];
