import { ChangeDetectionStrategy, Component, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { OpcionesMiembroService, OpcionMiembro } from './opciones-miembro.service';

/** Espera tras la última tecla antes de buscar. */
export const ESPERA_BUSQUEDA_MIEMBRO_MS = 300;

/**
 * Autocompletado de un miembro de la iglesia. Es un control de formulario cuyo valor es la opción elegida
 * (`{ id, nombre }`) o `null`; mientras se escribe sin elegir, el valor es `null`. `excluirId` quita de las
 * sugerencias a un miembro (p. ej. el propio, para que no sea su propio líder).
 */
@Component({
    selector: 'app-selector-miembro',
    imports: [FormsModule, AutoCompleteModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-miembro.html',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectorMiembroComponent), multi: true }]
})
export class SelectorMiembroComponent implements ControlValueAccessor {
    private readonly opciones = inject(OpcionesMiembroService);

    readonly inputId = input.required<string>();
    readonly placeholder = input('Busca por nombre');
    readonly excluirId = input<number | null>(null);

    protected readonly espera = ESPERA_BUSQUEDA_MIEMBRO_MS;
    protected readonly sugerencias = signal<OpcionMiembro[]>([]);
    protected readonly seleccion = signal<OpcionMiembro | string | null>(null);
    protected readonly deshabilitado = signal(false);

    private alCambiar: (opcion: OpcionMiembro | null) => void = () => undefined;
    protected alTocar: () => void = () => undefined;

    writeValue(opcion: OpcionMiembro | null): void {
        this.seleccion.set(opcion ? { id: opcion.id, nombre: opcion.nombre } : null);
    }

    registerOnChange(fn: (opcion: OpcionMiembro | null) => void): void {
        this.alCambiar = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.alTocar = fn;
    }

    setDisabledState(deshabilitado: boolean): void {
        this.deshabilitado.set(deshabilitado);
    }

    protected buscar({ query }: AutoCompleteCompleteEvent): void {
        this.opciones.buscar(query).subscribe({
            next: (opciones) => this.sugerencias.set(opciones.filter(({ id }) => id !== this.excluirId())),
            error: () => this.sugerencias.set([])
        });
    }

    /** El autocompletado entrega el texto mientras se escribe y la opción al elegirla: solo la opción es un valor. */
    protected cambiar(valor: OpcionMiembro | string | null): void {
        this.seleccion.set(valor);
        this.alCambiar(valor && typeof valor === 'object' ? valor : null);
    }
}
