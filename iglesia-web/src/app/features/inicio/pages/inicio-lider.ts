import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';

const ROL: Rol = 'LIDER';

/** Página de inicio del líder. Plantilla: su contenido real llegará en specs futuras. */
@Component({
    selector: 'app-inicio-lider',
    imports: [BienvenidaPerfilComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-lider.html'
})
export class InicioLiderComponent {
    private readonly sesion = inject(SesionService);

    protected readonly titulo = 'Mi red';
    protected readonly descripcion = 'Desde aquí darás seguimiento a las personas que lideras.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = ETIQUETA_ROL[ROL];
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
}
