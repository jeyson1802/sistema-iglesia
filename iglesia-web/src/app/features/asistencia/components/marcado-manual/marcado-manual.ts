import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map, skip, startWith } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { LIMITES_ASISTENCIA, MiembroReunion } from '../../models/asistencia';

/** Espera tras la última tecla antes de buscar por nombre. */
export const ESPERA_BUSQUEDA_MS = 300;

/**
 * Marcado a mano por nombre (presentacional): busca al dejar de escribir y cada resultado tiene «Marcar» o, si ya
 * está, «Presente ✓» (que al pulsarlo pide quitarlo). Emite `''` cuando el texto es muy corto para buscar.
 */
@Component({
    selector: 'app-marcado-manual',
    imports: [ReactiveFormsModule, ButtonModule, IconFieldModule, InputIconModule, InputTextModule, TableModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './marcado-manual.html'
})
export class MarcadoManualComponent {
    readonly resultados = input.required<readonly MiembroReunion[]>();
    readonly buscando = input(false);
    /** Miembro que se está marcando o quitando. */
    readonly procesando = input<number | null>(null);
    readonly etiqueta = input('Buscar por nombre');

    readonly buscar = output<string>();
    readonly marcar = output<MiembroReunion>();
    readonly quitar = output<MiembroReunion>();

    protected readonly busqueda = new FormControl('', { nonNullable: true });
    protected readonly minimo = LIMITES_ASISTENCIA.busquedaMin;
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filas = computed(() => [...this.resultados()]);
    protected texto = '';

    constructor() {
        this.busqueda.valueChanges
            .pipe(
                debounceTime(ESPERA_BUSQUEDA_MS),
                map((texto) => texto.trim()),
                map((texto) => (texto.length >= LIMITES_ASISTENCIA.busquedaMin ? texto : '')),
                // Parte de «sin búsqueda»: no avisa un vacío si todavía no se buscó nada.
                startWith(''),
                distinctUntilChanged(),
                skip(1),
                takeUntilDestroyed(inject(DestroyRef))
            )
            .subscribe((texto) => {
                this.texto = texto;
                this.buscar.emit(texto);
            });
    }
}
