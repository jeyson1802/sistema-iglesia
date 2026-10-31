import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MessageModule } from 'primeng/message';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';
import { PanoramaSistemaComponent } from '../components/panorama-sistema/panorama-sistema';
import { SelectorPeriodoComponent } from '../components/selector-periodo/selector-periodo';
import { periodoRecordado } from '../utils/periodo-recordado';
import { InicioAdministracionComponent } from './inicio-administracion';

const ROL: Rol = 'ADMIN_SISTEMA';

/**
 * Inicio del administrador del sistema (contenedor): en la vista global, el panorama de todas las iglesias; dentro
 * de una iglesia, el inicio de administración de esa iglesia con un aviso.
 */
@Component({
    selector: 'app-inicio-sistema',
    imports: [MessageModule, BienvenidaPerfilComponent, PanoramaSistemaComponent, SelectorPeriodoComponent, InicioAdministracionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-sistema.html'
})
export class InicioSistemaComponent {
    private readonly sesion = inject(SesionService);
    private readonly identidad = inject(IdentidadIglesiaService);

    protected readonly titulo = 'Panorama del sistema';
    protected readonly descripcion = 'Así crecen las iglesias y se usa el sistema, solo en cifras.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = ETIQUETA_ROL[ROL];
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    protected readonly actuandoEnIglesia = this.sesion.actuandoEnIglesia;
    protected readonly nombreIglesia = computed(() => this.identidad.nombreComercial() ?? 'la iglesia');
    protected readonly periodo = periodoRecordado();
}
