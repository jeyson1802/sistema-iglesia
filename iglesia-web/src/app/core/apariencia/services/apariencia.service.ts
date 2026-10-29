import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';
import { $t } from '@primeuix/themes';
import { guardarLocal, leerLocal } from '@/app/shared/utils/almacen-local';
import { Apariencia, APARIENCIA_SISTEMA, familiaCss, tipografiaDe, TIPOGRAFIAS } from '../models/apariencia';
import { extensionPreset } from '../models/extension-preset';
import { COLORES_PRIMARIOS, NOMBRES_PRESETS, paletaPrimaria, paletaSuperficie, PRESETS, SUPERFICIES } from '../models/paletas';

/** Última apariencia aplicada: permite pintar el tema de la iglesia al instante en la siguiente visita. */
export const CLAVE_APARIENCIA = 'apariencia.iglesia';
/** Variable CSS que lee la utilidad de Tailwind `font-marca`. */
export const VARIABLE_FUENTE_MARCA = '--fuente-marca';
const URL_GOOGLE_FONTS = 'https://fonts.googleapis.com/css2';
const PREFIJO_ID_FUENTE = 'fuente-marca-';

/**
 * Aplica la apariencia de una iglesia (preset, color primario, superficie y tipografía de marca) a toda la
 * aplicación. La lógica de temas se tomó del configurador de Sakai, que ya no existe en la interfaz.
 */
@Injectable({ providedIn: 'root' })
export class AparienciaService {
    private readonly documento = inject(DOCUMENT);
    private readonly aplicada = signal<Apariencia>(APARIENCIA_SISTEMA);

    /**
     * Apariencia aplicada. Quien pinta con los colores del tema (p. ej. gráficos) la observa para repintarse; no se
     * copia al `LayoutService`, cuyo cambio dispara la transición del modo oscuro.
     */
    readonly actual = this.aplicada.asReadonly();

    /** Al arrancar: la última apariencia conocida, para no parpadear mientras llega la de la iglesia. */
    aplicarRecordada(): void {
        this.aplicar(leerRecordada() ?? APARIENCIA_SISTEMA, false);
    }

    /** Aplica una apariencia; con `recordar` queda como la de la siguiente visita. */
    aplicar(apariencia: Apariencia, recordar = true): void {
        const segura = normalizar(apariencia);
        $t()
            .preset(PRESETS[segura.preset])
            .preset(extensionPreset(segura.preset, segura.colorPrimario, paletaPrimaria(segura.preset, segura.colorPrimario)))
            .surfacePalette(paletaSuperficie(segura.colorSuperficie))
            .use({ useDefaultOptions: true });
        this.cargarFuentes([segura.tipografia]);
        this.documento.documentElement.style.setProperty(VARIABLE_FUENTE_MARCA, familiaCss(segura.tipografia));
        this.aplicada.set(segura);
        if (recordar) {
            guardarLocal(CLAVE_APARIENCIA, JSON.stringify(segura));
        }
    }

    /** Carga desde Google Fonts las fuentes que falten (una hoja por fuente, solo el peso que se usa). */
    cargarFuentes(nombres: readonly string[]): void {
        nombres.map(tipografiaDe).forEach((tipografia) => {
            const id = PREFIJO_ID_FUENTE + tipografia.nombre.replace(/\s+/g, '-').toLowerCase();
            if (this.documento.getElementById(id)) {
                return;
            }
            const enlace = this.documento.createElement('link');
            enlace.id = id;
            enlace.rel = 'stylesheet';
            enlace.href = `${URL_GOOGLE_FONTS}?family=${tipografia.familiaGoogle}&display=swap`;
            this.documento.head.appendChild(enlace);
        });
    }
}

/** Un valor desconocido (p. ej. una caché vieja) cae en el del sistema en lugar de romper el tema. */
function normalizar(apariencia: Apariencia): Apariencia {
    return {
        preset: NOMBRES_PRESETS.includes(apariencia.preset) ? apariencia.preset : APARIENCIA_SISTEMA.preset,
        colorPrimario: (COLORES_PRIMARIOS as readonly string[]).includes(apariencia.colorPrimario) ? apariencia.colorPrimario : APARIENCIA_SISTEMA.colorPrimario,
        colorSuperficie: SUPERFICIES.some(({ nombre }) => nombre === apariencia.colorSuperficie) ? apariencia.colorSuperficie : null,
        tipografia: TIPOGRAFIAS.some(({ nombre }) => nombre === apariencia.tipografia) ? apariencia.tipografia : APARIENCIA_SISTEMA.tipografia
    };
}

function leerRecordada(): Apariencia | null {
    try {
        const guardada = JSON.parse(leerLocal(CLAVE_APARIENCIA) ?? 'null') as Apariencia | null;
        return guardada && typeof guardada === 'object' ? guardada : null;
    } catch {
        return null;
    }
}
