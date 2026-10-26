import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';

const ROL: Rol = 'ADMIN_IGLESIA';

/** Página de inicio del administrador de iglesia. Plantilla: su contenido real llegará en specs futuras. */
@Component({
    selector: 'app-inicio-administracion',
    imports: [BienvenidaPerfilComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-administracion.html'
})
export class InicioAdministracionComponent {
    private readonly sesion = inject(SesionService);

    protected readonly titulo = 'Administración de la iglesia';
    protected readonly descripcion = 'Desde aquí administrarás los miembros y la configuración de tu iglesia.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = ETIQUETA_ROL[ROL];
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
}
