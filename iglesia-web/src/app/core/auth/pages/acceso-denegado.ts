import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { RUTAS_AUTH } from '../models/rutas-auth';
import { SesionService } from '../services/sesion.service';

/** Se muestra cuando el perfil activo no tiene permiso para la página solicitada. */
@Component({
    selector: 'app-acceso-denegado',
    imports: [ButtonModule, RouterLink, TarjetaAccesoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './acceso-denegado.html'
})
export class AccesoDenegadoComponent {
    private readonly sesion = inject(SesionService);

    protected readonly rutaInicio = this.sesion.rutaInicio();
    protected readonly tieneVariosPerfiles = this.sesion.tieneVariosPerfiles;
    protected readonly rutaSeleccionarPerfil = RUTAS_AUTH.seleccionarPerfil;
}
