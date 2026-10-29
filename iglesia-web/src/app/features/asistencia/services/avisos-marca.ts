import { DOCUMENT, inject, Injectable } from '@angular/core';
import { TipoResultado } from '../models/resultado-marca';

interface Aviso {
    /** Frecuencias en Hz de cada tono, uno tras otro. */
    readonly tonos: readonly number[];
    readonly vibracion: readonly number[];
}

/** Agudo al marcar, medio si ya estaba y grave doble si hay un error, para no tener que mirar la pantalla. */
const AVISOS: Readonly<Record<TipoResultado, Aviso>> = {
    marcado: { tonos: [1320], vibracion: [80] },
    repetido: { tonos: [660, 660], vibracion: [40] },
    error: { tonos: [220], vibracion: [120, 80, 120] }
};
const DURACION_TONO_S = 0.12;
const VOLUMEN = 0.15;

/** Sonido corto (WebAudio) y vibración según el resultado de una marca. Si el navegador no los tiene, no hace nada. */
@Injectable({ providedIn: 'root' })
export class AvisosMarca {
    private readonly ventana = inject(DOCUMENT).defaultView;
    private audio: AudioContext | null = null;

    avisar(tipo: TipoResultado): void {
        const aviso = AVISOS[tipo];
        this.ventana?.navigator.vibrate?.([...aviso.vibracion]);
        this.sonar(aviso.tonos);
    }

    private sonar(tonos: readonly number[]): void {
        try {
            this.audio ??= this.ventana?.AudioContext ? new this.ventana.AudioContext() : null;
            if (!this.audio) {
                return;
            }
            const inicio = this.audio.currentTime;
            tonos.forEach((frecuencia, indice) => {
                const oscilador = this.audio!.createOscillator();
                const volumen = this.audio!.createGain();
                oscilador.frequency.value = frecuencia;
                volumen.gain.value = VOLUMEN;
                oscilador.connect(volumen).connect(this.audio!.destination);
                const desde = inicio + indice * DURACION_TONO_S * 1.5;
                oscilador.start(desde);
                oscilador.stop(desde + DURACION_TONO_S);
            });
        } catch {
            // El sonido es una ayuda: sin audio (permisos del navegador, sin altavoz) la marca sigue igual.
        }
    }
}
