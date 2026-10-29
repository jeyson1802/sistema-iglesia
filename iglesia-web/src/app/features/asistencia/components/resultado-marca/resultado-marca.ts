import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ResultadoMarca, TipoResultado } from '../../models/resultado-marca';

interface Estilo {
    readonly clases: string;
    readonly icono: string;
}

/** Colores fuertes (con texto blanco) para leerse de lejos; siempre acompañados de ícono y texto. */
const ESTILOS: Readonly<Record<TipoResultado, Estilo>> = {
    marcado: { clases: 'bg-green-600 text-white', icono: 'pi pi-check-circle' },
    repetido: { clases: 'bg-amber-500 text-black', icono: 'pi pi-history' },
    error: { clases: 'bg-red-600 text-white', icono: 'pi pi-times-circle' }
};

/** Franja grande con el resultado de la última lectura (presentacional); se anuncia a los lectores de pantalla. */
@Component({
    selector: 'app-resultado-marca',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './resultado-marca.html'
})
export class ResultadoMarcaComponent {
    readonly resultado = input<ResultadoMarca | null>(null);

    protected readonly estilo = computed(() => {
        const resultado = this.resultado();
        return resultado ? ESTILOS[resultado.tipo] : null;
    });
}
