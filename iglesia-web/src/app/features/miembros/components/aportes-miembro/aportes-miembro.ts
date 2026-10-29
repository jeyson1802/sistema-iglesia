import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { veAportesDeTodaLaIglesia } from '@/app/features/aportes/models/ambito-aportes';
import { Aporte, DESCRIPCION_MEDIO, FiltroAportes, LIMITES_APORTE, MedioAporte } from '@/app/features/aportes/models/aporte';
import { filtroAUrl } from '@/app/features/aportes/models/filtro-url';
import { rangoDe } from '@/app/features/aportes/models/periodo';
import { AportesService } from '@/app/features/aportes/services/aportes.service';
import { ConfiguracionAportesService } from '@/app/features/aportes/services/configuracion-aportes.service';
import { MonedaPipe } from '@/app/shared/pipes/moneda.pipe';
import { hoyIso } from '@/app/shared/utils/fecha';

/** Aportes recientes que se muestran en la ficha. */
const ULTIMOS_APORTES = 5;
const MS_POR_DIA = 86_400_000;

/**
 * Sección «Aportes» de la ficha de un miembro (contenedor): total del año, últimos aportes y enlace al listado ya
 * filtrado. Solo aparece si el perfil puede ver esos aportes (toda la iglesia, o los propios si es su ficha).
 */
@Component({
    selector: 'app-aportes-miembro',
    imports: [DatePipe, RouterLink, ButtonModule, SkeletonModule, TagModule, MonedaPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './aportes-miembro.html',
    host: { '[class.card]': 'visible()', '[class.block]': 'visible()' }
})
export class AportesMiembroComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(AportesService);
    private readonly configuracion = inject(ConfiguracionAportesService);

    readonly miembroId = input.required<number>();

    protected readonly visible = computed(() => {
        if (!this.sesion.tienePermiso('aportes.ver')) {
            return false;
        }
        return veAportesDeTodaLaIglesia(this.sesion.perfilActivo()) || this.sesion.usuarioActual()?.miembroId === this.miembroId();
    });
    protected readonly moneda = computed(() => this.configuracion.configuracion().moneda);
    protected readonly totalAnio = signal(0);
    protected readonly ultimos = signal<readonly Aporte[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly anio = new Date().getFullYear();
    protected readonly enlaceTodos = computed(() => filtroAUrl({ ...this.rangoUltimoAnio(), categoriaId: null, medio: null, miembroId: this.miembroId(), incluirAnulados: false }));

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        effect(() => {
            if (this.visible()) {
                const miembroId = this.miembroId();
                untracked(() => {
                    this.configuracion.asegurarCargada();
                    this.cargar(miembroId);
                });
            }
        });
    }

    protected cargar(miembroId: number = this.miembroId()): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        const delMiembro = (rango: Pick<FiltroAportes, 'desde' | 'hasta'>): FiltroAportes => ({ ...rango, categoriaId: null, medio: null, miembroId, incluirAnulados: false });
        this.carga = forkJoin({ resumen: this.servicio.resumen(delMiembro(rangoDe('ANIO'))), recientes: this.servicio.listar(delMiembro(this.rangoUltimoAnio()), 0, ULTIMOS_APORTES) }).subscribe({
            next: ({ resumen, recientes }) => {
                this.totalAnio.set(resumen.total);
                this.ultimos.set(recientes.content);
                this.cargando.set(false);
            },
            error: () => {
                this.error.set(true);
                this.cargando.set(false);
            }
        });
    }

    protected medio(medio: MedioAporte): string {
        return DESCRIPCION_MEDIO[medio].etiqueta;
    }

    /** El listado admite hasta 366 días: los últimos aportes se buscan en el último año. */
    private rangoUltimoAnio(): Pick<FiltroAportes, 'desde' | 'hasta'> {
        const hoy = new Date();
        return { desde: hoyIso(new Date(hoy.getTime() - (LIMITES_APORTE.rangoMaximoDias - 1) * MS_POR_DIA)), hasta: hoyIso(hoy) };
    }
}
