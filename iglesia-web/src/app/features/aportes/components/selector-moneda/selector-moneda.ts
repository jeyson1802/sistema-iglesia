import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { ConfiguracionAportes, Moneda } from '../../models/configuracion-aportes';

/** Opción del selector: «S/ · Sol peruano (PEN)». */
interface OpcionMoneda {
    readonly codigo: string;
    readonly etiqueta: string;
}

/**
 * Moneda de los aportes (presentacional): un selector con búsqueda y «Guardar». Si ya hay aportes vigentes la moneda
 * queda bloqueada y se explica por qué.
 */
@Component({
    selector: 'app-selector-moneda',
    imports: [FormsModule, ButtonModule, MessageModule, SelectModule, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-moneda.html'
})
export class SelectorMonedaComponent {
    readonly configuracion = input.required<ConfiguracionAportes>();
    readonly monedas = input.required<readonly Moneda[]>();
    readonly cargando = input(false);
    readonly guardando = input(false);

    readonly guardar = output<string>();

    protected readonly seleccion = signal<string | null>(null);
    protected readonly opciones = computed<OpcionMoneda[]>(() => this.monedas().map(({ codigo, nombre, simbolo }) => ({ codigo, etiqueta: `${simbolo} · ${capitalizar(nombre)} (${codigo})` })));
    protected readonly bloqueada = computed(() => !this.configuracion().monedaEditable);
    protected readonly cambio = computed(() => this.seleccion() !== null && this.seleccion() !== this.configuracion().moneda);

    constructor() {
        effect(() => this.seleccion.set(this.configuracion().moneda));
    }

    protected enviar(): void {
        const moneda = this.seleccion();
        if (moneda && this.cambio()) {
            this.guardar.emit(moneda);
        }
    }
}

function capitalizar(texto: string): string {
    return texto.charAt(0).toLocaleUpperCase('es') + texto.slice(1);
}
