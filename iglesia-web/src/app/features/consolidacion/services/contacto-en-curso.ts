import { DestroyRef, DOCUMENT, inject, Injectable, signal } from '@angular/core';
import { MedioBoton } from '@/app/shared/components/botones-contacto/botones-contacto';

/** Contacto que se abrió en la aplicación del teléfono y todavía no se registró. */
export interface ContactoEnCurso {
    readonly casoId: number;
    readonly nombre: string;
    readonly medio: MedioBoton;
}

/**
 * Recuerda a quién se llamó o escribió al tocar «Llamar» o «WhatsApp» y avisa cuando se vuelve al sistema (evento
 * `visibilitychange`), para abrir solo la hoja de registro. Lo que no se registra queda pendiente hasta registrarlo o
 * descartarlo.
 */
@Injectable({ providedIn: 'root' })
export class ContactoEnCursoService {
    private readonly documento = inject(DOCUMENT);
    private readonly abierto = signal<ContactoEnCurso | null>(null);
    private readonly sinRegistrar = signal<ReadonlyMap<number, ContactoEnCurso>>(new Map());
    private readonly devuelto = signal<ContactoEnCurso | null>(null);
    private salio = false;

    /** Contacto con el que se volvió al sistema: la pantalla abre la hoja y lo consume con `atendido()`. */
    readonly alVolver = this.devuelto.asReadonly();
    readonly pendientes = this.sinRegistrar.asReadonly();

    constructor() {
        const alCambiar = (): void => this.cambioDeVisibilidad();
        this.documento.addEventListener('visibilitychange', alCambiar);
        inject(DestroyRef).onDestroy(() => this.documento.removeEventListener('visibilitychange', alCambiar));
    }

    iniciar(contacto: ContactoEnCurso): void {
        this.abierto.set(contacto);
        this.salio = false;
        this.sinRegistrar.update((pendientes) => new Map(pendientes).set(contacto.casoId, contacto));
    }

    /** La hoja ya se abrió para el contacto devuelto. */
    atendido(): void {
        this.devuelto.set(null);
    }

    /** Se registró o se descartó: deja de estar pendiente. */
    resolver(casoId: number): void {
        if (this.abierto()?.casoId === casoId) {
            this.abierto.set(null);
        }
        this.sinRegistrar.update((pendientes) => {
            const restantes = new Map(pendientes);
            restantes.delete(casoId);
            return restantes;
        });
    }

    private cambioDeVisibilidad(): void {
        const contacto = this.abierto();
        if (!contacto) {
            return;
        }
        if (this.documento.visibilityState === 'hidden') {
            this.salio = true;
            return;
        }
        if (this.salio) {
            this.salio = false;
            this.abierto.set(null);
            this.devuelto.set(contacto);
        }
    }
}
