import { CandidatoGrupo, Grupo, GrupoResponse, IntegranteGrupo } from '../models/grupo';

export const RESPUESTA_OLIVOS: GrupoResponse = {
    id: 4,
    lider: { id: 10, nombre: 'Ana Ríos', activo: true },
    anfitrion: null,
    diaReunion: 'JUEVES',
    horaReunion: '19:30:00',
    direccion: 'Av. Perú 123',
    distritoCodigo: '150135',
    ubicacion: { departamento: { codigo: '15', nombre: 'Lima' }, provincia: { codigo: '1501', nombre: 'Lima' }, distrito: { codigo: '150135', nombre: 'San Martín de Porres' } },
    descripcion: null,
    activo: true,
    cantidadMiembros: 3
};

export const OLIVOS: Grupo = { ...RESPUESTA_OLIVOS, horaReunion: '19:30' };

export const BETANIA: Grupo = { ...OLIVOS, id: 5, lider: { id: 11, nombre: 'Beto Soto', activo: false }, diaReunion: null, horaReunion: null, distritoCodigo: null, ubicacion: null, activo: false, cantidadMiembros: 0 };

export const ROSA: IntegranteGrupo = { id: 31, nombreCompleto: 'Rosa Quispe', nivel: 'Miembro' };

/** Candidata que ya está en el grupo de Beto Soto: agregarla la mueve. */
export const LUIS_EN_OTRO_GRUPO: CandidatoGrupo = { id: 32, nombreCompleto: 'Luis Paz', nivel: null, grupo: { id: 5, nombre: 'Beto Soto', activo: false } };

export const TITO_SIN_GRUPO: CandidatoGrupo = { id: 33, nombreCompleto: 'Tito Vera', nivel: 'Líder', grupo: null };
