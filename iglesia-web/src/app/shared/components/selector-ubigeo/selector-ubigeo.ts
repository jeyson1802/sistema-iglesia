import { ChangeDetectionStrategy, Component, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { Ubigeo } from '@/app/core/models/ubicacion';
import { UbigeoService } from './ubigeo.service';

/** En los códigos INEI el departamento son los 2 primeros dígitos del distrito y la provincia, los 4 primeros. */
const DIGITOS_DEPARTAMENTO = 2;
const DIGITOS_PROVINCIA = 4;

/**
 * Departamento, provincia y distrito del Perú en cascada. Es un control de formulario cuyo valor es el código de
 * distrito (o `null`): cambiar el departamento limpia la provincia y el distrito, y cambiar la provincia, el distrito.
 * Con `parcial` (para filtros) el valor es el código del nivel más profundo elegido: 2, 4 o 6 dígitos.
 */
@Component({
    selector: 'app-selector-ubigeo',
    imports: [FormsModule, SelectModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-ubigeo.html',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectorUbigeoComponent), multi: true }]
})
export class SelectorUbigeoComponent implements ControlValueAccessor {
    private readonly ubigeo = inject(UbigeoService);

    /** Prefijo de los `id` de los tres campos, para asociarlos a sus `label`. */
    readonly idBase = input('ubigeo');
    /** Emite también un departamento o una provincia sin distrito (para filtrar). */
    readonly parcial = input(false);

    protected readonly departamentos = signal<Ubigeo[]>([]);
    protected readonly provincias = signal<Ubigeo[]>([]);
    protected readonly distritos = signal<Ubigeo[]>([]);
    protected readonly departamento = signal<string | null>(null);
    protected readonly provincia = signal<string | null>(null);
    protected readonly distrito = signal<string | null>(null);
    protected readonly deshabilitado = signal(false);

    private alCambiar: (codigo: string | null) => void = () => undefined;
    protected alTocar: () => void = () => undefined;

    constructor() {
        this.ubigeo.departamentos().subscribe({ next: (lista) => this.departamentos.set(lista), error: () => undefined });
    }

    writeValue(codigo: string | null): void {
        if (!codigo) {
            this.limpiarDesde('departamento');
            return;
        }
        const departamento = codigo.slice(0, DIGITOS_DEPARTAMENTO);
        this.limpiarDesde('provincia');
        this.departamento.set(departamento);
        this.cargarProvincias(departamento);
        if (codigo.length > DIGITOS_DEPARTAMENTO) {
            const provincia = codigo.slice(0, DIGITOS_PROVINCIA);
            this.provincia.set(provincia);
            this.cargarDistritos(provincia);
        }
        if (codigo.length > DIGITOS_PROVINCIA) {
            this.distrito.set(codigo);
        }
    }

    registerOnChange(fn: (codigo: string | null) => void): void {
        this.alCambiar = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.alTocar = fn;
    }

    setDisabledState(deshabilitado: boolean): void {
        this.deshabilitado.set(deshabilitado);
    }

    protected elegirDepartamento(codigo: string | null): void {
        this.limpiarDesde('provincia');
        this.departamento.set(codigo);
        if (codigo) {
            this.cargarProvincias(codigo);
        }
        this.alCambiar(this.parcial() ? codigo : null);
    }

    protected elegirProvincia(codigo: string | null): void {
        this.limpiarDesde('distrito');
        this.provincia.set(codigo);
        if (codigo) {
            this.cargarDistritos(codigo);
        }
        this.alCambiar(this.parcial() ? (codigo ?? this.departamento()) : null);
    }

    protected elegirDistrito(codigo: string | null): void {
        this.distrito.set(codigo);
        this.alCambiar(this.parcial() ? (codigo ?? this.provincia()) : codigo);
    }

    private limpiarDesde(nivel: 'departamento' | 'provincia' | 'distrito'): void {
        if (nivel === 'departamento') {
            this.departamento.set(null);
        }
        if (nivel !== 'distrito') {
            this.provincia.set(null);
            this.provincias.set([]);
        }
        this.distrito.set(null);
        this.distritos.set([]);
    }

    /** Una respuesta que llega después de cambiar de departamento se descarta. */
    private cargarProvincias(departamento: string): void {
        this.ubigeo.provincias(departamento).subscribe({
            next: (lista) => this.departamento() === departamento && this.provincias.set(lista),
            error: () => undefined
        });
    }

    private cargarDistritos(provincia: string): void {
        this.ubigeo.distritos(provincia).subscribe({
            next: (lista) => this.provincia() === provincia && this.distritos.set(lista),
            error: () => undefined
        });
    }
}
