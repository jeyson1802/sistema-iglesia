import {
    accionPrincipal,
    AntesDeContactar,
    CasoResumen,
    ContactosPorMedio,
    enDias,
    etiquetaResultado,
    frasesAntesDeContactar,
    iniciales,
    lineaContexto,
    plazoRonda,
    porcentaje,
    RESULTADOS_POR_MEDIO,
    resumenContactos,
    repartirEnPartesIguales,
    textoProximo,
    tiempoRestante
} from './consolidacion';

describe('modelo de consolidación', () => {
    const AHORA = new Date(2026, 10, 10, 12, 0);
    const SIN_CONTACTOS: ContactosPorMedio = { total: 0, llamadas: 0, whatsapp: 0, visitas: 0, enLaIglesia: 0, conRespuesta: 0 };

    it('ofrece solo los resultados que tienen sentido para cada medio', () => {
        expect(RESULTADOS_POR_MEDIO.LLAMADA).not.toContain('NO_ESTABA');
        expect(RESULTADOS_POR_MEDIO.VISITA).toContain('NO_ESTABA');
        expect(RESULTADOS_POR_MEDIO.EN_LA_IGLESIA).toEqual(['SE_HABLO']);
    });

    it('nombra el resultado según el medio', () => {
        expect(etiquetaResultado('NO_RESPONDIO', 'LLAMADA')).toBe('No contestó');
        expect(etiquetaResultado('NO_RESPONDIO', 'WHATSAPP')).toBe('No respondió');
        expect(etiquetaResultado('DATO_INCORRECTO', 'VISITA')).toBe('Dirección equivocada');
    });

    it('resume los contactos por medio', () => {
        expect(resumenContactos({ ...SIN_CONTACTOS, total: 3, llamadas: 2, visitas: 1 })).toBe('2 llamadas · 1 visita');
        expect(resumenContactos(SIN_CONTACTOS)).toBe('Sin contactos');
    });

    it('arma las iniciales con los dos primeros nombres', () => {
        expect(iniciales('ana  quispe ramos')).toBe('AQ');
    });

    it('describe el plazo de la ronda y el próximo contacto', () => {
        expect(plazoRonda('2026-11-15', AHORA)).toBe('faltan 5 días');
        expect(plazoRonda('2026-11-08', AHORA)).toBe('venció hace 2 días');
        expect(plazoRonda(null, AHORA)).toBeNull();
        expect(textoProximo('2026-11-08', AHORA)).toBe('vencido hace 2 días');
        expect(textoProximo('2026-11-10', AHORA)).toBe('para hoy');
        expect(textoProximo('2026-11-11', AHORA)).toBe('mañana');
    });

    it('la línea de contexto usa el último contacto o, si no hay, el resultado anterior', () => {
        const caso = { ultimoContacto: null, anterior: { resultado: { id: 1, nombre: 'No le interesa por ahora', activo: true }, cerradoEn: '2026-08-20T15:00:00Z' } } as unknown as CasoResumen;
        expect(lineaContexto(caso, AHORA)).toBe('Antes: No le interesa por ahora (ago 2026)');
        const contactado = { ...caso, ultimoContacto: { fechaHora: '2026-11-07T15:00:00Z', medio: 'LLAMADA', resultado: 'NO_RESPONDIO' } } as CasoResumen;
        expect(lineaContexto(contactado, AHORA)).toBe('Último: no contestó (hace 3 días)');
    });

    it('«Antes de contactar» cuenta la historia en lenguaje natural', () => {
        const antes: AntesDeContactar = {
            iteracion: 3,
            contactosAnteriores: { ...SIN_CONTACTOS, total: 3, llamadas: 3 },
            resultadosAnteriores: [
                { resultado: { id: 4, nombre: 'No le interesa por ahora', activo: true }, cerradoEn: '2026-08-20T15:00:00Z' },
                { resultado: null, cerradoEn: '2026-06-30T15:00:00Z' }
            ],
            asistenciasDesdeUltimoContacto: 2,
            peticion: 'oración por su familia'
        };
        expect(frasesAntesDeContactar(antes)).toEqual([
            'Es su 3.ª iteración.',
            'Antes: No le interesa por ahora (ago 2026); No concluido (jun 2026).',
            'En las iteraciones anteriores recibió 3 llamadas, sin lograr hablar con la persona.',
            'Volvió a asistir 2 veces después del último contacto.',
            'Llegó pidiendo: oración por su familia'
        ]);
        expect(frasesAntesDeContactar({ ...antes, iteracion: 1, peticion: null })).toEqual(['Es su primer seguimiento de consolidación.']);
    });

    it('calcula fechas futuras y porcentajes', () => {
        expect(enDias(7, AHORA)).toBe('2026-11-17');
        expect(porcentaje(1, 3)).toBe(33);
        expect(porcentaje(1, 0)).toBe(0);
    });
});

describe('tiempoRestante', () => {
    const AHORA = new Date('2026-11-03T12:00:00Z');

    it('dice cuánto falta en horas o minutos', () => {
        expect(tiempoRestante('2026-11-03T15:30:00Z', AHORA)).toBe('3 h');
        expect(tiempoRestante('2026-11-03T12:20:00Z', AHORA)).toBe('20 min');
        expect(tiempoRestante('2026-11-03T11:00:00Z', AHORA)).toBeNull();
    });
});

describe('acciones de la ronda', () => {
    const ronda = { id: 1, nombre: 'R', descripcion: null, fechaInicio: '2026-11-02', fechaFin: null, estado: 'PLANIFICADA', personas: 10, pendientes: 10, enContacto: 0, cerrados: 0, noConcluidos: 0, sinAsignar: 0, vencidos: 0 } as const;

    it('propone lo que sigue según el estado', () => {
        expect(accionPrincipal({ ...ronda, personas: 0 })).toBe('AGREGAR');
        expect(accionPrincipal({ ...ronda, sinAsignar: 3 })).toBe('REPARTIR');
        expect(accionPrincipal(ronda)).toBe('INICIAR');
        expect(accionPrincipal({ ...ronda, estado: 'EN_CURSO' })).toBeNull();
        expect(accionPrincipal({ ...ronda, estado: 'CERRADA', sinAsignar: 3 })).toBeNull();
    });

    it('reparte en partes iguales empezando por quien tiene menos carga', () => {
        const equipo = [
            { miembroId: 25, nombreCompleto: 'Luis', activo: true, casosAbiertos: 2, contactosRecientes: 0 },
            { miembroId: 31, nombreCompleto: 'Rosa', activo: true, casosAbiertos: 0, contactosRecientes: 0 },
            { miembroId: 40, nombreCompleto: 'Inactivo', activo: false, casosAbiertos: 0, contactosRecientes: 0 }
        ];
        expect(repartirEnPartesIguales([1, 2, 3, 4], equipo).map(({ consolidadorId }) => consolidadorId)).toEqual([31, 31, 25, 31]);
        expect(repartirEnPartesIguales([1], [])).toEqual([]);
    });
});
