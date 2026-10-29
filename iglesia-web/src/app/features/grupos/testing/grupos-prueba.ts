import { Grupo, GrupoResponse } from '../models/grupo';

export const RESPUESTA_OLIVOS: GrupoResponse = {
    id: 4,
    nombre: 'Los Olivos',
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

export const BETANIA: Grupo = { ...OLIVOS, id: 5, nombre: 'Betania', lider: null, diaReunion: null, horaReunion: null, distritoCodigo: null, ubicacion: null, activo: false, cantidadMiembros: 0 };
