import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { iniciales } from '@/app/core/iglesias/models/iglesia';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { NOMBRE_SISTEMA } from '@/app/core/pestana/titulo-iglesia.strategy';

export type OrientacionLogo = 'horizontal' | 'vertical';

/** Nombre y logo que se quieren ver en lugar de los de la iglesia en la que se opera (vista previa al editar). */
export interface VistaPreviaLogo {
    readonly nombre: string;
    readonly urlLogo: string | null;
}

/**
 * Logo y nombre comercial de la iglesia en la que se opera, con su tipografía de marca. Sin logo muestra un
 * monograma con las iniciales; sin iglesia (o en las pantallas de acceso, con `neutra`), la identidad del sistema.
 */
@Component({
    selector: 'app-logo-iglesia',
    imports: [SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './logo-iglesia.html',
    host: { '[class]': 'clasesContenedor()' }
})
export class LogoIglesiaComponent {
    private readonly identidad = inject(IdentidadIglesiaService);

    /** `vertical` para pantallas de acceso; `horizontal` para la topbar. */
    readonly orientacion = input<OrientacionLogo>('horizontal');
    /** Muestra la identidad del sistema aunque haya iglesia (antes de autenticarse no se conoce la iglesia). */
    readonly neutra = input(false);
    readonly vistaPrevia = input<VistaPreviaLogo | null>(null);

    protected readonly vertical = computed(() => this.orientacion() === 'vertical');
    protected readonly clasesContenedor = computed(() => (this.vertical() ? 'flex flex-col items-center gap-4' : 'flex items-center gap-2 min-w-0'));

    /** Nombre y logo de la iglesia que se muestra, o `null` para la identidad del sistema. */
    private readonly iglesia = computed<VistaPreviaLogo | null>(() => {
        const previa = this.vistaPrevia();
        if (previa) {
            return previa;
        }
        const nombre = this.identidad.nombreComercial();
        return this.neutra() || nombre === null ? null : { nombre, urlLogo: this.identidad.urlLogo() };
    });

    protected readonly cargando = computed(() => !this.neutra() && this.vistaPrevia() === null && this.identidad.cargando());
    protected readonly nombre = computed(() => this.iglesia()?.nombre ?? NOMBRE_SISTEMA);
    protected readonly urlLogo = computed(() => this.iglesia()?.urlLogo ?? null);
    protected readonly monograma = computed(() => {
        const iglesia = this.iglesia();
        return iglesia && !iglesia.urlLogo ? iniciales(iglesia.nombre) : null;
    });
}
