import { Page } from '@/app/core/models/page';
import { Marca, MiembroReunion, Reunion, ReunionHoy } from '../models/asistencia';
import { DetalleReunionGrupo, ReunionGrupo } from '../models/asistencia-grupo';

export const CULTO_HOY_POR_ABRIR: ReunionHoy = { id: null, tipo: 'CULTO', nombre: 'Culto de oración', hora: '19:30:00', cultoProgramadoId: 1, grupoId: null, totalAsistentes: 0 };
export const GRUPO_HOY_ABIERTO: ReunionHoy = { id: 70, tipo: 'GRUPO', nombre: 'Célula de Ana Torres', hora: '20:00:00', cultoProgramadoId: null, grupoId: 8, totalAsistentes: 5 };

export const REUNION_CULTO: Reunion = { id: 50, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-10-25', hora: '10:00:00', cultoProgramadoId: 1, grupoId: null, totalAsistentes: 128 };
export const REUNION_GRUPO: Reunion = { id: 70, tipo: 'GRUPO', nombre: 'Célula de Ana Torres', fecha: '2026-10-29', hora: '20:00:00', cultoProgramadoId: null, grupoId: 8, totalAsistentes: 1 };
export const REUNION_EVENTO: Reunion = { id: 60, tipo: 'EVENTO', nombre: 'Vigilia', fecha: '2026-10-29', hora: null, cultoProgramadoId: null, grupoId: null, totalAsistentes: 0 };

export const MARCA_ANA: Marca = { miembroId: 12, nombreCompleto: 'Ana Torres', nivel: 'Líder', grupoId: 8, yaRegistrado: false, registradaEn: '2026-10-29T15:02:00Z', totalAsistentes: 129 };

export const BETO: MiembroReunion = { miembroId: 13, nombreCompleto: 'Beto Ruiz', nivel: null, grupoId: 8, integrante: true, presente: false };
export const CARLA: MiembroReunion = { miembroId: 14, nombreCompleto: 'Carla Díaz', nivel: 'Diácono', grupoId: 8, integrante: true, presente: true };

export const paginaDe = <T>(contenido: readonly T[], total = contenido.length): Page<T> => ({ content: contenido, page: { size: 10, number: 0, totalElements: total, totalPages: 1 } });

export const REUNION_DEL_GRUPO: ReunionGrupo = { reunionId: 70, fecha: '2026-10-29', hora: '20:00:00', esperados: 4, asistieron: 3, faltaron: 1, visitantes: 1, porcentaje: 75 };
export const DETALLE_REUNION_DEL_GRUPO: DetalleReunionGrupo = {
    reunionId: 70,
    fecha: '2026-10-29',
    hora: '20:00:00',
    asistieron: [
        { miembroId: 12, nombreCompleto: 'Ana Torres', nivel: 'Líder' },
        { miembroId: 14, nombreCompleto: 'Carla Díaz', nivel: 'Diácono' },
        { miembroId: 16, nombreCompleto: 'Elsa Soto', nivel: null }
    ],
    faltaron: [{ miembroId: 13, nombreCompleto: 'Beto Ruiz', nivel: null }],
    visitantes: [{ miembroId: 20, nombreCompleto: 'Luis Rojas', nivel: null }]
};
