import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { CasoDetalle, CasoResumen, Consolidador, Contacto, ContactosPorMedio, Iteracion, ResultadoConsolidacion, Ronda } from '../models/consolidacion';

/** Datos de consolidación para las pruebas unitarias, con valores válidos que cada prueba ajusta. */
export const SIN_CONTACTOS: ContactosPorMedio = { total: 0, llamadas: 0, whatsapp: 0, visitas: 0, enLaIglesia: 0, conRespuesta: 0 };

export const rondaPrueba = (id: number, cambios: Partial<Ronda> = {}): Ronda => ({
    id,
    nombre: `Ronda ${id}`,
    descripcion: null,
    fechaInicio: '2026-11-02',
    fechaFin: '2026-11-30',
    estado: 'EN_CURSO',
    personas: 10,
    pendientes: 5,
    enContacto: 3,
    cerrados: 2,
    noConcluidos: 0,
    sinAsignar: 0,
    vencidos: 0,
    ...cambios
});

export const casoPrueba = (id: number, cambios: Partial<CasoResumen> = {}): CasoResumen => ({
    id,
    ronda: { id: 7, nombre: 'Ronda 7', activo: true },
    persona: { miembroId: 40 + id, nombreCompleto: `Persona ${id}`, telefono: '987654321', condicion: 'NUEVO', fechaIngreso: '2026-10-05', activo: true },
    consolidador: { id: 25, nombre: 'Luis Rojas', activo: true },
    estado: 'PENDIENTE',
    contactos: SIN_CONTACTOS,
    ultimoContacto: null,
    proximoContacto: null,
    resultado: null,
    iteracion: 1,
    anterior: null,
    ...cambios
});

export const contactoPrueba = (id: number, cambios: Partial<Contacto> = {}): Contacto => ({
    id,
    casoId: 310,
    fechaHora: '2026-11-03T19:30:00Z',
    medio: 'LLAMADA',
    resultado: 'SE_HABLO',
    observaciones: null,
    proximoContacto: null,
    consolidador: { id: 25, nombre: 'Luis Rojas', activo: true },
    registradoPorMi: true,
    editableHasta: '2026-11-04T19:30:00Z',
    editable: true,
    ...cambios
});

export const ASISTENCIA_PRUEBA: AsistenciaMiembro = {
    reunionesQueCuentan: 8,
    asistidas: 4,
    porcentaje: 50,
    porcentajeAnterior: null,
    faltasSeguidas: 1,
    ultimaAsistencia: '2026-11-01',
    alertas: [],
    semanas: [],
    ultimas: []
};

export const iteracionPrueba = (casoId: number, cambios: Partial<Iteracion> = {}): Iteracion => ({
    casoId,
    ronda: { id: 3, nombre: 'Agosto', activo: false },
    estado: 'CERRADO',
    desde: '2026-08-01T13:00:00Z',
    cerradoEn: '2026-08-20T15:00:00Z',
    consolidador: { id: 31, nombre: 'Rosa Díaz', activo: true },
    contactos: { ...SIN_CONTACTOS, total: 2, llamadas: 2, conRespuesta: 1 },
    resultado: { id: 4, nombre: 'No le interesa por ahora', activo: true },
    observacionCierre: 'Dijo que por ahora no puede venir',
    detalle: [contactoPrueba(800, { casoId, editable: false })],
    ...cambios
});

export const detallePrueba = (cambios: Partial<CasoDetalle> = {}): CasoDetalle => ({
    id: 310,
    ronda: { id: 7, nombre: 'Noviembre', activo: true },
    estadoRonda: 'EN_CURSO',
    estado: 'EN_CONTACTO',
    consolidador: { id: 25, nombre: 'Luis Rojas', activo: true },
    resultado: null,
    observacionCierre: null,
    proximoContacto: '2026-11-10',
    creadoEn: '2026-11-02T13:00:00Z',
    cerradoEn: null,
    persona: {
        miembroId: 40,
        nombreCompleto: 'Ana Quispe',
        telefono: '987654321',
        correo: null,
        direccion: 'Av. Los Olivos 123',
        ubicacion: 'LOS OLIVOS, LIMA',
        condicion: 'NUEVO',
        fechaIngreso: '2026-10-05',
        fechaNacimiento: '1995-04-12',
        invitadoPor: 'Rosa Díaz',
        lider: 'Juan Pérez',
        grupo: 'Célula de Juan',
        peticion: 'Oración por su familia',
        activo: true
    },
    antes: {
        iteracion: 2,
        contactosAnteriores: { ...SIN_CONTACTOS, total: 2, llamadas: 2, conRespuesta: 1 },
        resultadosAnteriores: [{ resultado: { id: 4, nombre: 'No le interesa por ahora', activo: true }, cerradoEn: '2026-08-20T15:00:00Z' }],
        asistenciasDesdeUltimoContacto: 1,
        peticion: 'Oración por su familia'
    },
    contactos: [contactoPrueba(900)],
    asistencias: [{ id: 120, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-11-08' }],
    asistencia: ASISTENCIA_PRUEBA,
    anteriores: [iteracionPrueba(205)],
    puedeActuar: true,
    puedeReasignar: false,
    ...cambios
});

export const consolidadorPrueba = (miembroId: number, cambios: Partial<Consolidador> = {}): Consolidador => ({
    miembroId,
    nombreCompleto: `Consolidador ${miembroId}`,
    activo: true,
    casosAbiertos: 2,
    contactosRecientes: 5,
    ...cambios
});

export const resultadoPrueba = (id: number, nombre: string, cambios: Partial<ResultadoConsolidacion> = {}): ResultadoConsolidacion => ({ id, nombre, orden: id, activo: true, ...cambios });
