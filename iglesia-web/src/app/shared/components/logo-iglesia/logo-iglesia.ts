import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Identidad neutra del sistema. La spec 02 la reemplaza por el nombre y el logo de la iglesia del usuario. */
const NOMBRE_SISTEMA = 'IGLESIA-WEB';

export type OrientacionLogo = 'horizontal' | 'vertical';

/** Logo y nombre que identifican al sistema en el login y en la topbar. */
@Component({
    selector: 'app-logo-iglesia',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './logo-iglesia.html',
    host: { '[class]': 'clasesContenedor()' }
})
export class LogoIglesia {
    /** `vertical` para pantallas de acceso; `horizontal` para la topbar, que ya define tamaños y espaciado. */
    readonly orientacion = input<OrientacionLogo>('horizontal');

    protected readonly nombre = NOMBRE_SISTEMA;
    protected readonly vertical = computed(() => this.orientacion() === 'vertical');
    protected readonly clasesContenedor = computed(() => (this.vertical() ? 'flex flex-col items-center gap-4' : 'contents'));
}
