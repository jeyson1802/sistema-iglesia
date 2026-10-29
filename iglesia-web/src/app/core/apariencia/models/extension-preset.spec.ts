import { extensionPreset } from './extension-preset';
import { COLORES_PRIMARIOS, paletaPrimaria, paletaSuperficie, SUPERFICIES } from './paletas';

describe('paletas y extensión del preset', () => {
    it('cada color primario admitido tiene paleta en los tres presets, salvo noir', () => {
        COLORES_PRIMARIOS.filter((color) => color !== 'noir').forEach((color) => {
            (['Aura', 'Lara', 'Nora'] as const).forEach((preset) => expect(paletaPrimaria(preset, color)[500]).withContext(`${preset} ${color}`).toBeDefined());
        });
        expect(paletaPrimaria('Aura', 'noir')).toEqual({});
    });

    it('hay ocho superficies y sin elegir ninguna no hay paleta', () => {
        expect(SUPERFICIES.length).toBe(8);
        expect(paletaSuperficie('zinc')?.[500]).toBe('#71717a');
        expect(paletaSuperficie(null)).toBeUndefined();
    });

    it('noir usa la superficie como primario y Nora tonos más intensos', () => {
        const noir = extensionPreset('Aura', 'noir', {}) as { semantic: { primary: Record<number, string> } };
        const nora = extensionPreset('Nora', 'rose', { 500: '#f43f5e' }) as { semantic: { colorScheme: { light: { primary: { color: string } } } } };
        const aura = extensionPreset('Aura', 'rose', { 500: '#f43f5e' }) as { semantic: { colorScheme: { light: { primary: { color: string } } } } };

        expect(noir.semantic.primary[500]).toBe('{surface.500}');
        expect(nora.semantic.colorScheme.light.primary.color).toBe('{primary.600}');
        expect(aura.semantic.colorScheme.light.primary.color).toBe('{primary.500}');
    });
});
