import { afterNextRender, ChangeDetectionStrategy, Component, effect, ElementRef, input, output, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';

/** Tras perder el foco (p. ej. por un clic), se recupera tras este tiempo para no pelear con el clic. */
const ESPERA_RECUPERAR_FOCO_MS = 150;

/**
 * Campo para lectores de códigos 2D (USB, Bluetooth o de gatillo), que se comportan como un teclado: escriben el
 * código y un Enter. Está siempre enfocado y recupera el foco solo. También sirve para escribir el código a mano.
 */
@Component({
    selector: 'app-entrada-lector',
    imports: [ReactiveFormsModule, InputTextModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './entrada-lector.html'
})
export class EntradaLectorComponent {
    /** Sin conexión no se lee: el campo se deshabilita. */
    readonly pausado = input(false);

    readonly leido = output<string>();

    protected readonly campo = new FormControl('', { nonNullable: true });
    private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
    private espera?: ReturnType<typeof setTimeout>;

    constructor() {
        afterNextRender(() => this.enfocar());
        effect(() => {
            if (this.pausado()) {
                this.campo.disable();
            } else {
                this.campo.enable();
                this.enfocar();
            }
        });
    }

    protected enviar(evento: Event): void {
        evento.preventDefault();
        const texto = this.campo.value.trim();
        this.campo.setValue('');
        if (texto) {
            this.leido.emit(texto);
        }
    }

    protected recuperarFoco(): void {
        clearTimeout(this.espera);
        this.espera = setTimeout(() => this.enfocar(), ESPERA_RECUPERAR_FOCO_MS);
    }

    private enfocar(): void {
        // Un diálogo abierto (p. ej. la ayuda) se queda con el foco.
        if (!this.pausado() && !document.querySelector('.p-dialog-mask')) {
            this.entrada().nativeElement.focus();
        }
    }
}
