import { COLOR_NOIR, NombrePreset, Paleta } from './paletas';

const TONOS = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950] as const;

/** Con `noir` el primario es la propia superficie: negro en modo claro y blanco en modo oscuro. */
const EXTENSION_NOIR = {
    semantic: {
        primary: Object.fromEntries(TONOS.map((tono) => [tono, `{surface.${tono}}`])),
        colorScheme: {
            light: {
                primary: { color: '{primary.950}', contrastColor: '#ffffff', hoverColor: '{primary.800}', activeColor: '{primary.700}' },
                highlight: { background: '{primary.950}', focusBackground: '{primary.700}', color: '#ffffff', focusColor: '#ffffff' }
            },
            dark: {
                primary: { color: '{primary.50}', contrastColor: '{primary.950}', hoverColor: '{primary.200}', activeColor: '{primary.300}' },
                highlight: { background: '{primary.50}', focusBackground: '{primary.300}', color: '{primary.950}', focusColor: '{primary.950}' }
            }
        }
    }
};

/** Nora usa tonos más intensos del primario que Aura y Lara. */
const ESQUEMA_NORA = {
    light: {
        primary: { color: '{primary.600}', contrastColor: '#ffffff', hoverColor: '{primary.700}', activeColor: '{primary.800}' },
        highlight: { background: '{primary.600}', focusBackground: '{primary.700}', color: '#ffffff', focusColor: '#ffffff' }
    },
    dark: {
        primary: { color: '{primary.500}', contrastColor: '{surface.900}', hoverColor: '{primary.400}', activeColor: '{primary.300}' },
        highlight: { background: '{primary.500}', focusBackground: '{primary.400}', color: '{surface.900}', focusColor: '{surface.900}' }
    }
};

const ESQUEMA_GENERAL = {
    light: {
        primary: { color: '{primary.500}', contrastColor: '#ffffff', hoverColor: '{primary.600}', activeColor: '{primary.700}' },
        highlight: { background: '{primary.50}', focusBackground: '{primary.100}', color: '{primary.700}', focusColor: '{primary.800}' }
    },
    dark: {
        primary: { color: '{primary.400}', contrastColor: '{surface.900}', hoverColor: '{primary.300}', activeColor: '{primary.200}' },
        highlight: {
            background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
            focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
            color: 'rgba(255,255,255,.87)',
            focusColor: 'rgba(255,255,255,.87)'
        }
    }
};

/** Tokens semánticos que pintan el color primario sobre un preset (lógica tomada del configurador de Sakai). */
export function extensionPreset(preset: NombrePreset, color: string, paleta: Paleta): object {
    if (color === COLOR_NOIR) {
        return EXTENSION_NOIR;
    }
    return { semantic: { primary: paleta, colorScheme: preset === 'Nora' ? ESQUEMA_NORA : ESQUEMA_GENERAL } };
}
