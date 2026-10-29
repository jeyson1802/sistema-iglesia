import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AsistenciaMiembro, ETIQUETA_TIPO_REUNION, SEVERIDAD_TIPO_REUNION, TipoReunion } from '@/app/features/asistencia/models/asistencia';
import { RUTA_ASISTENCIA } from '@/app/features/asistencia/models/rutas-asistencia';
import { EtiquetasAlertaComponent } from '@/app/features/asistencia/components/etiquetas-alerta/etiquetas-alerta';
import { IndicadoresAsistenciaComponent } from '@/app/features/asistencia/components/indicadores-asistencia/indicadores-asistencia';
import { SemanasAsistenciaComponent } from '@/app/features/asistencia/components/semanas-asistencia/semanas-asistencia';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { fechaConDia } from '@/app/shared/utils/fecha';

/**
 * Tarjeta «Asistencia» de la ficha (contenedor): porcentaje de las últimas 8 semanas con su tendencia, faltas
 * seguidas, última vez, alertas, las 12 semanas y sus últimas reuniones, con el acceso a su seguimiento completo.
 * Aparece con `asistencia.ver` o `miembros.ver` (el backend aplica el ámbito de red del líder).
 */
@Component({
    selector: 'app-asistencia-miembro',
    imports: [RouterLink, ButtonModule, SkeletonModule, TagModule, EtiquetasAlertaComponent, IndicadoresAsistenciaComponent, SemanasAsistenciaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './asistencia-miembro.html',
    host: { '[class.card]': 'visible()', '[class.block]': 'visible()' }
})
export class AsistenciaMiembroComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(AsistenciasService);

    readonly miembroId = input.required<number>();

    protected readonly visible = computed(() => this.sesion.tienePermiso('asistencia.ver') || this.sesion.tienePermiso('miembros.ver'));
    protected readonly puedeVerDetalle = computed(() => this.sesion.tienePermiso('asistencia.ver') || this.sesion.tienePermiso('asistencia.registrar') || this.sesion.tienePermiso('asistencia.grupo'));
    protected readonly resumen = signal<AsistenciaMiembro | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly rutaAsistencia = RUTA_ASISTENCIA;
    protected readonly conDia = fechaConDia;

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        effect(() => {
            if (this.visible()) {
                const miembroId = this.miembroId();
                untracked(() => this.cargar(miembroId));
            }
        });
    }

    protected cargar(miembroId: number = this.miembroId()): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.servicio
            .resumenMiembro(miembroId)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (resumen) => this.resumen.set(resumen), error: () => this.error.set(true) });
    }

    protected tipo(tipo: TipoReunion): string {
        return ETIQUETA_TIPO_REUNION[tipo];
    }

    protected severidad(tipo: TipoReunion) {
        return SEVERIDAD_TIPO_REUNION[tipo];
    }
}
