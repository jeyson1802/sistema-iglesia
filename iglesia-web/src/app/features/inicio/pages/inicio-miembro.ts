import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';
import { MiAsistenciaComponent } from '../components/mi-asistencia/mi-asistencia';
import { MiCodigoQrComponent } from '../components/mi-codigo-qr/mi-codigo-qr';
import { MisDatosComponent } from '../components/mis-datos/mis-datos';
import { EstadisticasService } from '../services/estadisticas.service';
import { cargaReactiva, SIN_PARAMETRO } from '../utils/carga-reactiva';

const ROL: Rol = 'MIEMBRO';

/**
 * Inicio del miembro (contenedor): su código QR de asistencia, su asistencia y sus datos. Todo sale del miembro
 * vinculado a su usuario (el backend lo toma del token); nunca datos de otras personas ni aportes.
 */
@Component({
    selector: 'app-inicio-miembro',
    imports: [ButtonModule, SkeletonModule, BienvenidaPerfilComponent, MiAsistenciaComponent, MiCodigoQrComponent, MisDatosComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-miembro.html'
})
export class InicioMiembroComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(EstadisticasService);

    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = computed(() => this.sesion.perfilActivo()?.nombre ?? ETIQUETA_ROL[ROL]);

    protected readonly perfil = cargaReactiva(SIN_PARAMETRO, () => this.servicio.miPerfil());
    protected readonly asistencia = cargaReactiva(SIN_PARAMETRO, () => this.servicio.miAsistencia());
    protected readonly codigo = cargaReactiva(SIN_PARAMETRO, () => this.servicio.miCodigoAsistencia());

    /** Sin miembro vinculado el backend responde 404: no hay nada propio que mostrar. */
    protected readonly sinVinculo = computed(() => {
        const fallo = this.perfil.fallo();
        return fallo instanceof HttpErrorResponse && fallo.status === HttpStatusCode.NotFound;
    });
    protected readonly nombre = computed(() => this.perfil.datos()?.nombres ?? this.sesion.usuarioActual()?.correo ?? '');
    protected readonly descripcion = computed(() => {
        const perfil = this.perfil.datos();
        const partes = [perfil?.nivel, perfil?.grupo?.nombre].filter((parte): parte is string => !!parte);
        return partes.length > 0 ? `${partes.join(' · ')}.` : 'Aquí tienes tu asistencia y tu código para registrarla.';
    });
}
