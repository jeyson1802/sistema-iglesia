/** Algo que hacer hoy desde el inicio: lleva a una pantalla del módulo. */
export interface Atajo {
    readonly etiqueta: string;
    readonly descripcion: string;
    readonly icono: string;
    readonly ruta: string;
    readonly queryParams?: Readonly<Record<string, string>>;
}
