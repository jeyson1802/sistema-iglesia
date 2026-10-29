import { Injectable } from '@angular/core';

/** Lecturas por segundo: suficiente para una fila de personas sin agotar la batería. */
const LECTURAS_POR_SEGUNDO = 8;

/** Por qué no se pudo encender la cámara. */
export type MotivoErrorCamara = 'sin-permiso' | 'sin-camara' | 'no-disponible';

export class ErrorCamara extends Error {
    constructor(readonly motivo: MotivoErrorCamara) {
        super(motivo);
    }
}

/** Cámara encendida que lee QR sin parar. */
export interface SesionCamara {
    readonly variasCamaras: boolean;
    readonly tieneLinterna: boolean;
    cambiarCamara(): Promise<void>;
    /** Devuelve si la linterna quedó encendida. */
    alternarLinterna(): Promise<boolean>;
    detener(): void;
}

/**
 * Adaptador de `qr-scanner`: enciende la cámara trasera sobre un `<video>` y avisa cada texto leído. La librería se
 * descarga solo al usarla. Se provee por componente para poder reemplazarla en las pruebas.
 */
@Injectable()
export class CamaraQr {
    async iniciar(video: HTMLVideoElement, alLeer: (texto: string) => void): Promise<SesionCamara> {
        const { default: QrScanner } = await import('qr-scanner');
        const escaner = new QrScanner(video, ({ data }) => alLeer(data), { preferredCamera: 'environment', maxScansPerSecond: LECTURAS_POR_SEGUNDO, highlightScanRegion: true, returnDetailedScanResult: true });
        try {
            await escaner.start();
        } catch (error) {
            escaner.destroy();
            throw new ErrorCamara(motivoDe(error));
        }
        const [camaras, tieneLinterna] = await Promise.all([QrScanner.listCameras().catch(() => []), escaner.hasFlash().catch(() => false)]);
        let trasera = true;
        return {
            variasCamaras: camaras.length > 1,
            tieneLinterna,
            cambiarCamara: async () => {
                trasera = !trasera;
                await escaner.setCamera(trasera ? 'environment' : 'user');
            },
            alternarLinterna: async () => {
                await escaner.toggleFlash();
                return escaner.isFlashOn();
            },
            detener: () => escaner.destroy()
        };
    }
}

function motivoDe(error: unknown): MotivoErrorCamara {
    const nombre = error instanceof DOMException ? error.name : String(error);
    if (nombre === 'NotAllowedError' || nombre === 'SecurityError') {
        return 'sin-permiso';
    }
    return /not found|NotFoundError|OverconstrainedError/i.test(nombre) ? 'sin-camara' : 'no-disponible';
}
