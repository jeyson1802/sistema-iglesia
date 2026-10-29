import { Miembro, MiembroResponse, MiembroResumen } from '../models/miembro';

export const RESUMEN_JUAN: MiembroResumen = {
    id: 22,
    nombres: 'Juan Carlos',
    apellidos: 'Pérez Torres',
    condicion: 'NUEVO',
    nivel: { id: 3, nombre: 'Líder', activo: true },
    lider: { id: 10, nombre: 'Ana Ríos', activo: true },
    direccion: 'Av. Perú 123',
    ubicacion: { departamento: { codigo: '15', nombre: 'Lima' }, provincia: { codigo: '1501', nombre: 'Lima' }, distrito: { codigo: '150135', nombre: 'San Martín de Porres' } },
    telefono: '999 888 777',
    esPastorPrincipal: false,
    activo: true
};

export const RESUMEN_PASTOR: MiembroResumen = { ...RESUMEN_JUAN, id: 1, nombres: 'Pedro', apellidos: 'Gómez', lider: null, direccion: null, ubicacion: null, condicion: 'CONSOLIDADO', esPastorPrincipal: true };

export const RESPUESTA_JUAN: MiembroResponse = {
    ...RESUMEN_JUAN,
    genero: 'MASCULINO',
    fechaNacimiento: '1990-05-14',
    edad: 35,
    edadEsAproximada: false,
    edadAproximada: null,
    fechaIngreso: '2026-01-10',
    estadoCivil: 'CASADO',
    correo: 'juan@correo.test',
    distritoCodigo: '150135',
    ocupacion: null,
    profesion: null,
    nivelEstudios: null,
    centroEstudios: null,
    peticion: 'Oración por su familia',
    invitadoPor: null,
    grupo: { id: 4, nombre: 'Ana Ríos', activo: false },
    registradoPor: { id: 10, nombre: 'Ana Ríos', activo: true },
    cantidadLiderados: 2,
    creadoEn: '2026-10-01T15:00:00Z',
    actualizadoEn: '2026-10-02T15:00:00Z'
};

export const JUAN: Miembro = { ...RESPUESTA_JUAN, creadoEn: new Date(RESPUESTA_JUAN.creadoEn), actualizadoEn: new Date(RESPUESTA_JUAN.actualizadoEn) };

/** Ficha con solo nombres y apellidos. */
export const MINIMO: Miembro = {
    ...JUAN,
    id: 30,
    nombres: 'Rosa',
    apellidos: 'Flores',
    condicion: null,
    nivel: null,
    lider: null,
    grupo: null,
    telefono: null,
    genero: null,
    fechaNacimiento: null,
    edad: null,
    edadAproximada: null,
    fechaIngreso: null,
    estadoCivil: null,
    correo: null,
    direccion: null,
    distritoCodigo: null,
    ubicacion: null,
    peticion: null,
    registradoPor: null,
    cantidadLiderados: 0
};
