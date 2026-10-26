import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';

const ROL: Rol = 'MIEMBRO';

/** Página de inicio del miembro. Plantilla: su contenido real llegará en specs futuras. */
@Component({
    selector: 'app-inicio-miembro',
    imports: [BienvenidaPerfilComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-miembro.html'
})
export class InicioMiembroComponent {
    private readonly sesion = inject(SesionService);

    protected readonly titulo = 'Mi perfil';
    protected readonly descripcion = 'Desde aquí consultarás tu información personal y tu asistencia.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = ETIQUETA_ROL[ROL];
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
}
