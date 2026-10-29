import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { describirUbicacion } from '@/app/core/models/ubicacion';
import { NOMBRE_SISTEMA } from '@/app/core/pestana/titulo-iglesia.strategy';

/**
 * Pie con los datos de la iglesia en la que se opera: nombre comercial con su tipografía, denominación, dirección,
 * contacto y año. Lo que falta no se muestra; sin iglesia, el nombre del sistema.
 */
@Component({
    selector: 'app-footer',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.footer.html'
})
export class AppFooter {
    private readonly identidad = inject(IdentidadIglesiaService);

    protected readonly anio = new Date().getFullYear();
    protected readonly iglesia = this.identidad.identidad;
    protected readonly marca = computed(() => this.iglesia()?.nombreComercial ?? NOMBRE_SISTEMA);
    /** «Av. Los Próceres 123, San Juan de Lurigancho, Lima, Lima», sin partes vacías. */
    protected readonly direccion = computed(() => {
        const iglesia = this.iglesia();
        return iglesia ? [iglesia.direccion, describirUbicacion(iglesia.ubicacion)].filter(Boolean).join(', ') : '';
    });
    protected readonly enlaceTelefono = computed(() => `tel:${(this.iglesia()?.telefono ?? '').replace(/[^\d+]/g, '')}`);
}
