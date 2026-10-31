import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { EstadisticasAportes, EstadisticasAsistencia, EstadisticasConsolidacion, EstadisticasGrupos, EstadisticasMiembros, EstadisticasSistema, EstadisticasUso, PeriodoUsado } from '../models/estadisticas';
import { MiPerfil } from '../models/mi-perfil';

/** Datos de prueba de las páginas de inicio. */
export const PERIODO_PRUEBA: PeriodoUsado = { periodo: 'MES', desde: '2026-10-01', hasta: '2026-10-30', anteriorDesde: '2026-09-01', anteriorHasta: '2026-09-30', granularidad: 'SEMANA' };

export function miembrosPrueba(cambios: Partial<EstadisticasMiembros> = {}): EstadisticasMiembros {
    return {
        periodo: PERIODO_PRUEBA,
        red: false,
        activos: 120,
        directos: null,
        altas: 12,
        altasAnterior: 8,
        bajas: 2,
        bajasAnterior: 3,
        sinGrupo: 30,
        sinLider: 4,
        serie: [
            { inicio: '2026-09-28', altas: 3, bajas: 1 },
            { inicio: '2026-10-05', altas: 9, bajas: 1 }
        ],
        porCondicion: [
            { clave: 'CONSOLIDADO', etiqueta: 'Consolidado', valor: 90 },
            { clave: 'NUEVO', etiqueta: 'Nuevo', valor: 30 }
        ],
        porNivel: [{ clave: '1', etiqueta: 'Diácono', valor: 10 }],
        porGenero: [{ clave: 'FEMENINO', etiqueta: 'Mujeres', valor: 70 }],
        porEdad: [{ clave: 'JOVENES', etiqueta: 'Jóvenes (18–29)', valor: 40 }],
        ...cambios
    };
}

export function gruposPrueba(cambios: Partial<EstadisticasGrupos> = {}): EstadisticasGrupos {
    return {
        periodo: PERIODO_PRUEBA,
        red: false,
        grupos: 2,
        integrantesPromedio: 8.5,
        miembrosEnGrupo: 17,
        miembrosActivos: 120,
        sinReuniones: 1,
        reuniones: 4,
        reunionesAnterior: 5,
        porcentajeAsistencia: 75,
        porcentajeAsistenciaAnterior: 70,
        porGrupo: [
            { grupoId: 1, nombre: 'Célula de Ana', integrantes: 9, reuniones: 4, porcentaje: 75 },
            { grupoId: 2, nombre: 'Célula de Luis', integrantes: 8, reuniones: 0, porcentaje: null }
        ],
        ...cambios
    };
}

export function asistenciaPrueba(cambios: Partial<EstadisticasAsistencia> = {}): EstadisticasAsistencia {
    return {
        periodo: PERIODO_PRUEBA,
        red: false,
        cultos: 8,
        cultosAnterior: 8,
        asistentesPromedio: 85.5,
        asistentesPromedioAnterior: 80,
        personasQueAsistieron: 110,
        personasQueAsistieronAnterior: 100,
        serie: [
            { inicio: '2026-09-28', cultos: 2, asistentesPromedio: 80 },
            { inicio: '2026-10-05', cultos: 2, asistentesPromedio: 91 }
        ],
        alertas: { personas: 20, conAlertas: 5, faltasSeguidas: 3, nuevoSinVolver: 1, asistenciaEnCaida: 2 },
        llegaron: 6,
        volvieron: 4,
        ...cambios
    };
}

export function consolidacionPrueba(cambios: Partial<EstadisticasConsolidacion> = {}): EstadisticasConsolidacion {
    return {
        periodo: PERIODO_PRUEBA,
        rondasEnCurso: 1,
        casosAbiertos: 7,
        cerrados: 3,
        cerradosAnterior: 0,
        contactos: 15,
        contactosAnterior: 10,
        contactadas: 6,
        volvieron: 2,
        contactosPorMedio: [{ clave: 'LLAMADA', etiqueta: 'Llamada', valor: 15 }],
        cierresPorResultado: [{ clave: 'INTEGRADO', etiqueta: 'Integrado', valor: 3 }],
        ...cambios
    };
}

export function aportesPrueba(cambios: Partial<EstadisticasAportes> = {}): EstadisticasAportes {
    return {
        periodo: PERIODO_PRUEBA,
        resumen: {
            moneda: 'PEN',
            total: 1250,
            cantidad: 10,
            totalPeriodoAnterior: 1000,
            porCategoria: [{ categoria: { id: 1, nombre: 'Diezmo', activo: true }, total: 1000, cantidad: 6 }],
            porMedio: [{ medio: 'YAPE', total: 1250, cantidad: 10 }]
        },
        serie: [{ inicio: '2026-10-05', total: 1250, cantidad: 10 }],
        ...cambios
    };
}

export function usoPrueba(cambios: Partial<EstadisticasUso> = {}): EstadisticasUso {
    return {
        periodo: PERIODO_PRUEBA,
        global: false,
        usuarios: 20,
        usuariosNuevos: 2,
        activosPeriodo: 12,
        activosPeriodoAnterior: 10,
        activosDiarios: 3.4,
        activosDiariosAnterior: 2.9,
        activosSemanales: 8,
        activosMensuales: 12,
        iglesiasActivas: null,
        iglesiasActivasAnterior: null,
        serie: [{ inicio: '2026-10-05', valor: 9 }],
        porRolBase: [
            { clave: 'ADMIN_IGLESIA', valor: 2 },
            { clave: 'LIDER', valor: 10 }
        ],
        ...cambios
    };
}

export function sistemaPrueba(cambios: Partial<EstadisticasSistema> = {}): EstadisticasSistema {
    return {
        periodo: PERIODO_PRUEBA,
        iglesias: 3,
        iglesiasNuevas: 1,
        iglesiasNuevasAnterior: 0,
        iglesiasConActividad: 2,
        iglesiasConActividadAnterior: 2,
        miembros: 450,
        altas: 25,
        altasAnterior: 20,
        bajas: 3,
        bajasAnterior: 1,
        serie: [{ inicio: '2026-10-05', iglesiasNuevas: 1, altas: 25, bajas: 3, miembrosRegistrados: 25, asistenciasMarcadas: 300, contactosConsolidacion: 40 }],
        porCondicion: [{ clave: 'CONSOLIDADO', etiqueta: 'Consolidado', valor: 400 }],
        porTamano: [{ clave: 'HASTA_200', etiqueta: 'De 51 a 200', valor: 2 }],
        mayores: [{ iglesiaId: 1, nombre: 'Iglesia Central', miembros: 300 }],
        ...cambios
    };
}

export function miPerfilPrueba(cambios: Partial<MiPerfil> = {}): MiPerfil {
    return {
        miembroId: 40,
        nombres: 'Ana',
        apellidos: 'Quispe',
        edad: 30,
        edadEsAproximada: false,
        fechaIngreso: '2024-03-10',
        condicion: 'CONSOLIDADO',
        nivel: 'Líder',
        lider: 'Luis Rojas',
        grupo: { id: 3, nombre: 'Célula de Luis', diaReunion: 'VIERNES', horaReunion: '19:30:00' },
        ...cambios
    };
}

export function miAsistenciaPrueba(cambios: Partial<AsistenciaMiembro> = {}): AsistenciaMiembro {
    return {
        reunionesQueCuentan: 10,
        asistidas: 8,
        porcentaje: 80,
        porcentajeAnterior: 50,
        faltasSeguidas: 0,
        ultimaAsistencia: '2026-10-25',
        alertas: [],
        semanas: Array.from({ length: 12 }, (_, indice) => ({ inicio: `2026-08-${String(10 + indice).padStart(2, '0')}`, esperadas: 2, asistidas: 1, porcentaje: 50 })),
        ultimas: [{ id: 50, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-10-25' }],
        ...cambios
    };
}
