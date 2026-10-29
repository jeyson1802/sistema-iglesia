import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { catchError, of, Subscription } from 'rxjs';
import { Iglesia, iniciales } from '@/app/core/iglesias/models/iglesia';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';

/** Miniatura del logo de una iglesia del catálogo; sin logo (o si no carga), su monograma. */
@Component({
    selector: 'app-miniatura-iglesia',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './miniatura-iglesia.html'
})
export class MiniaturaIglesiaComponent {
    private readonly iglesias = inject(IglesiasService);

    readonly iglesia = input.required<Iglesia>();

    protected readonly url = signal<string | null>(null);
    protected readonly monograma = computed(() => iniciales(this.iglesia().nombreComercial));
    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.carga?.unsubscribe();
            this.cambiar(null);
        });
        effect(() => {
            const { id, tieneLogo } = this.iglesia();
            untracked(() => this.cargar(id, tieneLogo));
        });
    }

    private cargar(id: number, tieneLogo: boolean): void {
        this.carga?.unsubscribe();
        this.cambiar(null);
        if (tieneLogo) {
            this.carga = this.iglesias
                .logo(id)
                .pipe(catchError(() => of(null)))
                .subscribe((blob) => this.cambiar(blob ? URL.createObjectURL(blob) : null));
        }
    }

    /** Libera la URL anterior para no acumular imágenes en memoria. */
    private cambiar(url: string | null): void {
        const anterior = this.url();
        if (anterior) {
            URL.revokeObjectURL(anterior);
        }
        this.url.set(url);
    }
}
