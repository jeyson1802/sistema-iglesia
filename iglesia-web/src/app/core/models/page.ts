/**
 * Página de Spring Data tal como la serializan los backends (`PageSerializationMode.VIA_DTO`): el contenido y,
 * aparte, los metadatos de paginación.
 */
export interface Page<T> {
    readonly content: readonly T[];
    readonly page: PageMetadata;
}

export interface PageMetadata {
    readonly size: number;
    /** Índice de la página, empezando en 0. */
    readonly number: number;
    readonly totalElements: number;
    readonly totalPages: number;
}
