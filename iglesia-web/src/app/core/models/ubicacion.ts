/** Contrato de `UbigeoResponse` en iglesia-api: un departamento, provincia o distrito del INEI. */
export interface Ubigeo {
    readonly codigo: string;
    readonly nombre: string;
}

/** Contrato de `UbicacionResponse`: el distrito con la provincia y el departamento que lo contienen. */
export interface Ubicacion {
    readonly departamento: Ubigeo;
    readonly provincia: Ubigeo;
    readonly distrito: Ubigeo;
}

/** Texto «Distrito, Provincia, Departamento» de una ubicación; vacío si no hay. */
export const describirUbicacion = (ubicacion: Ubicacion | null): string => (ubicacion ? [ubicacion.distrito.nombre, ubicacion.provincia.nombre, ubicacion.departamento.nombre].join(', ') : '');
