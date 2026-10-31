import { etiquetaEje, etiquetaPunto, formatoNumero, porcentajeDe, textoRango, variacion } from './estadisticas';

describe('modelos de estadísticas', () => {
    it('variación: sube, baja, sin cambios y sin datos para comparar', () => {
        expect(variacion(12, 8)).toEqual({ tendencia: 'sube', texto: '+50 %', favorable: true });
        expect(variacion(6, 8, false)).toEqual({ tendencia: 'baja', texto: '−25 %', favorable: true });
        expect(variacion(8, 8)).toEqual({ tendencia: 'igual', texto: 'Sin cambios', favorable: null });
        expect(variacion(5, 0)).toBeNull();
        expect(variacion(null, 3)).toBeNull();
    });

    it('etiquetas del eje por semana o por mes', () => {
        expect(etiquetaPunto('2026-10-05', 'SEMANA')).toBe('5 oct');
        expect(etiquetaPunto('2026-10-01', 'MES')).toMatch(/^oct/);
        expect(etiquetaPunto('2026-10-07', 'DIA')).toBe('7 oct');
        expect(etiquetaEje('DIA')).toBe('Día');
        expect(etiquetaEje('SEMANA')).toBe('Semana');
        expect(etiquetaEje('MES')).toBe('Mes');
    });

    it('rango, números y porcentajes', () => {
        expect(textoRango({ desde: '2026-10-01', hasta: '2026-10-30' })).toBe('1 oct 2026 – 30 oct 2026');
        expect(formatoNumero(1234)).toBe('1,234');
        expect(formatoNumero(null)).toBe('—');
        expect(porcentajeDe(17, 120)).toBe(14);
        expect(porcentajeDe(1, 0)).toBeNull();
    });
});
