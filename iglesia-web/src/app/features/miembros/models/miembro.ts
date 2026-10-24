export type EstadoMiembro = 'ACTIVO' | 'INACTIVO';

export interface Miembro {
    id: number;
    nombres: string;
    apellidos: string;
    telefono?: string;
    email?: string;
    fechaNacimiento?: string;
    fechaBautismo?: string;
    estado: EstadoMiembro;
}
