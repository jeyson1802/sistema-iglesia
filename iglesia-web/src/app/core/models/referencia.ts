/**
 * Contrato de `ReferenciaResponse` en iglesia-api: otra entidad de la iglesia (nivel, líder, grupo…) asignada a un
 * registro. Si se desactivó después de asignarla, se conserva con `activo: false`.
 */
export interface Referencia {
    readonly id: number;
    readonly nombre: string;
    readonly activo: boolean;
}
