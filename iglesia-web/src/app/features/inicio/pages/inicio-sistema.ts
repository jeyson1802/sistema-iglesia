import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';

const ROL: Rol = 'ADMIN_SISTEMA';

/** Página de inicio del administrador del sistema. Plantilla: su contenido real llegará en specs futuras. */
@Component({
    selector: 'app-inicio-sistema',
    imports: [BienvenidaPerfilComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-sistema.html'
})
export class InicioSistemaComponent {
    private readonly sesion = inject(SesionService);

    protected readonly titulo = 'Panel del sistema';
    protected readonly descripcion = 'Desde aquí administrarás las iglesias registradas en el sistema.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = ETIQUETA_ROL[ROL];
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
}
