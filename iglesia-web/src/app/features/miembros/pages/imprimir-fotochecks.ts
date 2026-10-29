import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, map, Observable } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { FotocheckComponent } from '@/app/shared/components/fotocheck/fotocheck';
import { Fotocheck } from '../models/miembro';
import { SeleccionFotochecks, seleccionDesdeUrl } from '../models/fotochecks-url';
import { MiembrosService } from '../services/miembros.service';

/** 2 columnas × 5 filas de credenciales CR80 en una hoja A4. */
export const FOTOCHECKS_POR_HOJA = 10;
const MENSAJE_ERROR = 'No se pudieron cargar los fotochecks. Vuelve a intentarlo.';

/**
 * Vista previa e impresión de fotochecks (contenedor), fuera del layout para que solo salgan las hojas: A4 con 10
 * fotochecks y líneas de corte. Imprime los del listado de miembros con sus filtros o el de un miembro (desde su ficha).
 */
@Component({
    selector: 'app-imprimir-fotochecks',
    imports: [ButtonModule, MessageModule, SkeletonModule, FotocheckComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './imprimir-fotochecks.html'
})
export class ImprimirFotochecksComponent {
    private readonly servicio = inject(MiembrosService);
    private readonly ubicacion = inject(Location);

    protected readonly porHoja = FOTOCHECKS_POR_HOJA;
    protected readonly fotochecks = signal<readonly Fotocheck[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal<string | null>(null);
    protected readonly hojas = computed(() => {
        const todos = this.fotochecks();
        return Array.from({ length: Math.ceil(todos.length / FOTOCHECKS_POR_HOJA) }, (_, hoja) => todos.slice(hoja * FOTOCHECKS_POR_HOJA, (hoja + 1) * FOTOCHECKS_POR_HOJA));
    });

    constructor() {
        inject(ActivatedRoute)
            .queryParams.pipe(takeUntilDestroyed())
            .subscribe((parametros) => this.cargar(seleccionDesdeUrl(parametros)));
    }

    protected imprimir(): void {
        window.print();
    }

    protected volver(): void {
        this.ubicacion.back();
    }

    private cargar(seleccion: SeleccionFotochecks): void {
        this.cargando.set(true);
        this.error.set(null);
        this.pedir(seleccion).subscribe({
            next: (fotochecks) => {
                this.fotochecks.set(fotochecks);
                this.cargando.set(false);
            },
            error: (error: unknown) => {
                this.error.set(detalleDeError(error, MENSAJE_ERROR) ?? MENSAJE_ERROR);
                this.cargando.set(false);
            }
        });
    }

    private pedir(seleccion: SeleccionFotochecks): Observable<Fotocheck[]> {
        if ('filtro' in seleccion) {
            return this.servicio.fotochecks(seleccion.filtro);
        }
        const id = seleccion.miembroId;
        return forkJoin({ miembro: this.servicio.obtener(id), codigo: this.servicio.codigoAsistencia(id) }).pipe(
            map(({ miembro, codigo }) => [{ miembroId: id, nombres: miembro.nombres, apellidos: miembro.apellidos, nivel: miembro.nivel?.nombre ?? null, ...codigo }])
        );
    }
}
