import { ChangeDetectionStrategy, Component, computed, ElementRef, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Severidad } from '../../models/consolidacion';

/** Una opción del grupo: siempre con texto (el color y el ícono solo acompañan). */
export interface OpcionGrupo<T> {
    readonly valor: T;
    readonly etiqueta: string;
    readonly icono?: string;
    readonly severidad?: Severidad;
}

const COLOR_ICONO: Readonly<Record<Severidad, string>> = {
    success: 'text-green-600 dark:text-green-400',
    info: 'text-sky-600 dark:text-sky-400',
    warn: 'text-orange-600 dark:text-orange-400',
    danger: 'text-red-600 dark:text-red-400',
    secondary: 'text-muted-color',
    contrast: 'text-surface-900 dark:text-surface-0'
};

const TECLAS_SIGUIENTE = ['ArrowRight', 'ArrowDown'];
const TECLAS_ANTERIOR = ['ArrowLeft', 'ArrowUp'];

/**
 * Botones grandes de opción única (control de formulario) con el patrón `radiogroup`: un solo tabulador entra al grupo
 * y las flechas mueven y eligen, como los radios nativos. Reemplaza a las listas desplegables donde las opciones son
 * pocas (medio, resultado del contacto, resultado del cierre).
 */
@Component({
    selector: 'app-grupo-opciones',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './grupo-opciones.html',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => GrupoOpcionesComponent), multi: true }]
})
export class GrupoOpcionesComponent<T> implements ControlValueAccessor {
    private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

    readonly opciones = input.required<readonly OpcionGrupo<T>[]>();
    /** Nombre accesible del grupo (p. ej. «Medio»). */
    readonly etiqueta = input.required<string>();
    /** Clases de la grilla de botones (Tailwind). */
    readonly columnas = input('grid-cols-2 sm:grid-cols-4');
    /** Botones bajos, con el ícono al lado del texto. */
    readonly compacto = input(false);

    protected readonly valor = signal<T | null>(null);
    protected readonly deshabilitado = signal(false);
    /** El botón que recibe el tabulador: el elegido o, sin elección, el primero. */
    protected readonly enfocable = computed(() =>
        Math.max(
            0,
            this.opciones().findIndex(({ valor }) => valor === this.valor())
        )
    );

    private alCambiar: (valor: T | null) => void = () => undefined;
    protected alTocar: () => void = () => undefined;

    writeValue(valor: T | null): void {
        this.valor.set(valor);
    }

    registerOnChange(fn: (valor: T | null) => void): void {
        this.alCambiar = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.alTocar = fn;
    }

    setDisabledState(deshabilitado: boolean): void {
        this.deshabilitado.set(deshabilitado);
    }

    protected elegir(valor: T): void {
        if (this.deshabilitado()) {
            return;
        }
        this.valor.set(valor);
        this.alCambiar(valor);
        this.alTocar();
    }

    protected mover(evento: KeyboardEvent, indice: number): void {
        const paso = TECLAS_SIGUIENTE.includes(evento.key) ? 1 : TECLAS_ANTERIOR.includes(evento.key) ? -1 : 0;
        if (paso === 0) {
            return;
        }
        evento.preventDefault();
        const total = this.opciones().length;
        const siguiente = (indice + paso + total) % total;
        this.elegir(this.opciones()[siguiente].valor);
        this.elemento.nativeElement.querySelectorAll<HTMLButtonElement>('[role="radio"]')[siguiente]?.focus();
    }

    protected colorIcono(opcion: OpcionGrupo<T>): string {
        return COLOR_ICONO[opcion.severidad ?? 'secondary'];
    }
}
