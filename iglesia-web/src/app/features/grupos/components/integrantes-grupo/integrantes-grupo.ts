import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DragDropModule } from 'primeng/dragdrop';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { CandidatoGrupo, IntegranteGrupo, nombreDeGrupo } from '../../models/grupo';

/** Espera tras la última tecla antes de buscar candidatos. */
export const ESPERA_BUSQUEDA_MS = 300;

/** Ámbitos de arrastre: un candidato solo se suelta en el grupo, y un integrante solo en «Disponibles». */
const AMBITO_AGREGAR = 'agregar-al-grupo';
const AMBITO_QUITAR = 'quitar-del-grupo';

const FILAS_CARGA = [0, 1, 2];

/**
 * Dos listas para administrar los integrantes de un grupo (presentacional): «Disponibles» (sin búsqueda, los últimos
 * en llegar a la iglesia; con búsqueda, los que coinciden) y «Miembros
 * del grupo». Se arrastra de una a otra y cada fila tiene además un botón equivalente, para teclado y pantallas
 * táctiles. Emite la intención; quien la usa confirma, llama a la API y actualiza las listas.
 */
@Component({
    selector: 'app-integrantes-grupo',
    imports: [ReactiveFormsModule, ButtonModule, DragDropModule, IconFieldModule, InputIconModule, InputTextModule, SkeletonModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './integrantes-grupo.html'
})
export class IntegrantesGrupoComponent {
    readonly integrantes = input.required<readonly IntegranteGrupo[]>();
    readonly candidatos = input.required<readonly CandidatoGrupo[]>();
    readonly cargandoIntegrantes = input(false);
    readonly buscando = input(false);
    /** Sin permiso o con el grupo desactivado, las listas son de solo lectura. */
    readonly editable = input(false);
    /** Ids de los miembros cuyo cambio está en curso: sus botones se deshabilitan. */
    readonly procesando = input<ReadonlySet<number>>(new Set());
    /** Nombre singular de los grupos en la iglesia, para mostrar el grupo actual de un candidato. */
    readonly nombreSingular = input('Grupo');

    readonly buscar = output<string>();
    readonly agregar = output<CandidatoGrupo>();
    readonly quitar = output<IntegranteGrupo>();

    protected readonly ambitoAgregar = AMBITO_AGREGAR;
    protected readonly ambitoQuitar = AMBITO_QUITAR;
    protected readonly filasCarga = FILAS_CARGA;
    /** Zona sobre la que se está arrastrando, para resaltarla. */
    protected readonly zonaActiva = signal<'grupo' | 'disponibles' | null>(null);

    private arrastrado: CandidatoGrupo | IntegranteGrupo | null = null;
    protected readonly busqueda = inject(NonNullableFormBuilder).control('');
    /** Si hay texto buscado: sin él, «Disponibles» muestra a los últimos en llegar a la iglesia. */
    protected readonly hayBusqueda = toSignal(this.busqueda.valueChanges.pipe(map((texto) => texto.trim() !== '')), { initialValue: false });

    constructor() {
        effect(() => (this.editable() ? this.busqueda.enable({ emitEvent: false }) : this.busqueda.disable({ emitEvent: false })));
        this.busqueda.valueChanges
            .pipe(
                debounceTime(ESPERA_BUSQUEDA_MS),
                map((texto) => texto.trim()),
                distinctUntilChanged(),
                takeUntilDestroyed()
            )
            .subscribe((texto) => this.buscar.emit(texto));
    }

    /** El grupo actual de un candidato, como «Célula de Beto Soto». */
    protected grupoActual(candidato: CandidatoGrupo): string | null {
        return candidato.grupo ? nombreDeGrupo(this.nombreSingular(), candidato.grupo.nombre) : null;
    }

    protected empezarArrastre(miembro: CandidatoGrupo | IntegranteGrupo): void {
        this.arrastrado = miembro;
    }

    protected terminarArrastre(): void {
        this.arrastrado = null;
        this.zonaActiva.set(null);
    }

    protected soltarEnGrupo(): void {
        const candidato = this.arrastrado as CandidatoGrupo | null;
        this.terminarArrastre();
        if (candidato) {
            this.agregar.emit(candidato);
        }
    }

    protected soltarEnDisponibles(): void {
        const integrante = this.arrastrado as IntegranteGrupo | null;
        this.terminarArrastre();
        if (integrante) {
            this.quitar.emit(integrante);
        }
    }
}
