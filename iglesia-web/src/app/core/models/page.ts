// Respuesta paginada de Spring Data (Page<T>)
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}
