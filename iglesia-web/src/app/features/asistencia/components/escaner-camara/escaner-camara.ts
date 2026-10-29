import { afterNextRender, ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ModoAsistencia } from '../../models/modo-asistencia';
import { CamaraQr, ErrorCamara, MotivoErrorCamara, SesionCamara } from '../../services/camara-qr';

type EstadoCamara = 'iniciando' | 'activa' | MotivoErrorCamara;

/** Explicación y cómo corregirlo para cada motivo por el que la cámara no enciende. */
const EXPLICACIONES: Readonly<Record<MotivoErrorCamara, { readonly titulo: string; readonly detalle: string }>> = {
    'sin-permiso': {
        titulo: 'El navegador no dio permiso para usar la cámara',
        detalle: 'Toca el candado o el ícono de la cámara junto a la dirección de la página, permite la cámara y vuelve a intentar.'
    },
    'sin-camara': { titulo: 'No se encontró una cámara en este equipo', detalle: 'Usa un celular, o conecta un lector 2D y elige «Lector».' },
    'no-disponible': { titulo: 'No se pudo encender la cámara', detalle: 'Puede que otra aplicación la esté usando o que la página no se abrió con https. Cierra la otra aplicación y vuelve a intentar.' }
};

/**
 * Vista de la cámara que lee QR sin parar (presentacional): cámara trasera, cambiar de cámara y linterna si el equipo
 * la tiene. Si no enciende, explica por qué y ofrece pasar a «Lector» o «Manual».
 */
@Component({
    selector: 'app-escaner-camara',
    imports: [ButtonModule, ProgressSpinnerModule],
    providers: [CamaraQr],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './escaner-camara.html'
})
export class EscanerCamaraComponent {
    private readonly camara = inject(CamaraQr);

    /** Sin conexión las lecturas se ignoran y la vista se atenúa. */
    readonly pausado = input(false);

    readonly leido = output<string>();
    readonly cambiarModo = output<ModoAsistencia>();

    protected readonly estado = signal<EstadoCamara>('iniciando');
    protected readonly linternaEncendida = signal(false);
    protected readonly sesion = signal<SesionCamara | null>(null);
    protected readonly explicacion = computed(() => {
        const estado = this.estado();
        return estado === 'iniciando' || estado === 'activa' ? null : EXPLICACIONES[estado];
    });
    private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
    private destruido = false;

    constructor() {
        afterNextRender(() => void this.iniciar());
        inject(DestroyRef).onDestroy(() => {
            this.destruido = true;
            this.sesion()?.detener();
        });
    }

    protected async iniciar(): Promise<void> {
        this.estado.set('iniciando');
        try {
            const sesion = await this.camara.iniciar(this.video().nativeElement, (texto) => {
                if (!this.pausado()) {
                    this.leido.emit(texto);
                }
            });
            if (this.destruido) {
                sesion.detener();
                return;
            }
            this.sesion.set(sesion);
            this.estado.set('activa');
        } catch (error) {
            this.estado.set(error instanceof ErrorCamara ? error.motivo : 'no-disponible');
        }
    }

    protected async cambiarCamara(): Promise<void> {
        await this.sesion()?.cambiarCamara();
        this.linternaEncendida.set(false);
    }

    protected async alternarLinterna(): Promise<void> {
        const sesion = this.sesion();
        if (sesion) {
            this.linternaEncendida.set(await sesion.alternarLinterna());
        }
    }
}
