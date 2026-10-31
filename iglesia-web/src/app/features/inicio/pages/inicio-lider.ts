import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { Permiso } from '@/app/core/auth/models/permiso';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ReunionesService } from '@/app/features/asistencia/services/reuniones.service';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';
import { AtajosDelDiaComponent } from '../components/atajos-del-dia/atajos-del-dia';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';
import { BloqueAsistenciaComponent } from '../components/bloque-asistencia/bloque-asistencia';
import { BloqueGruposComponent } from '../components/bloque-grupos/bloque-grupos';
import { BloqueMiembrosComponent } from '../components/bloque-miembros/bloque-miembros';
import { SelectorPeriodoComponent } from '../components/selector-periodo/selector-periodo';
import { TarjetaIndicadorComponent } from '../components/tarjeta-indicador/tarjeta-indicador';
import { Atajo } from '../models/atajo';
import { formatoNumero, textoRango } from '../models/estadisticas';
import { EstadisticasService } from '../services/estadisticas.service';
import { cargaReactiva, SIN_PARAMETRO } from '../utils/carga-reactiva';
import { periodoRecordado } from '../utils/periodo-recordado';

const ROL: Rol = 'LIDER';

/**
 * Inicio del líder, «Mi red» (contenedor): atajos del día, cifras de su red y de sus grupos, y los bloques de su
 * red, de su asistencia y de sus grupos. El backend limita todo a su red; nunca muestra aportes ni otras redes.
 */
@Component({
    selector: 'app-inicio-lider',
    imports: [SkeletonModule, AtajosDelDiaComponent, BienvenidaPerfilComponent, BloqueAsistenciaComponent, BloqueGruposComponent, BloqueMiembrosComponent, SelectorPeriodoComponent, TarjetaIndicadorComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-lider.html'
})
export class InicioLiderComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(EstadisticasService);
    private readonly reuniones = inject(ReunionesService);
    private readonly casos = inject(CasosService);

    protected readonly titulo = 'Mi red';
    protected readonly descripcion = 'Así van las personas que lideras, directa e indirectamente.';
    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = computed(() => this.sesion.perfilActivo()?.nombre ?? ETIQUETA_ROL[ROL]);
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    protected readonly numero = formatoNumero;
    protected readonly periodo = periodoRecordado();

    private readonly puede = (...permisos: Permiso[]) => computed(() => permisos.some((permiso) => this.sesion.tienePermiso(permiso)));
    protected readonly verRed = this.puede('miembros.ver');
    protected readonly verAsistencia = this.puede('asistencia.grupo', 'asistencia.ver');
    protected readonly verGrupos = this.puede('asistencia.grupo', 'grupos.ver');
    private readonly esConsolidador = this.puede('consolidacion.contactar');
    private readonly puedeTomarAsistencia = this.puede('asistencia.registrar', 'asistencia.grupo');

    protected readonly red = cargaReactiva(this.periodo, (periodo) => this.servicio.miembros(periodo), this.verRed);
    protected readonly asistencia = cargaReactiva(this.periodo, (periodo) => this.servicio.asistencia(periodo), this.verAsistencia);
    protected readonly grupos = cargaReactiva(this.periodo, (periodo) => this.servicio.grupos(periodo), this.verGrupos);
    private readonly reunionesHoy = cargaReactiva(SIN_PARAMETRO, () => this.reuniones.hoy(), this.puedeTomarAsistencia);
    private readonly misCasos = cargaReactiva(SIN_PARAMETRO, () => this.casos.resumenMisCasos(), this.esConsolidador);

    protected readonly rango = computed(() => {
        const periodo = this.red.datos()?.periodo ?? this.asistencia.datos()?.periodo;
        return periodo ? textoRango(periodo) : null;
    });

    /** Tomar la asistencia de sus grupos de hoy, las personas con alerta y sus asignados de consolidación. */
    protected readonly atajos = computed<readonly Atajo[]>(() => {
        const atajos: Atajo[] = (this.reunionesHoy.datos() ?? [])
            .filter(({ grupoId }) => grupoId !== null)
            .map(({ nombre, totalAsistentes }) => ({
                etiqueta: `Tomar asistencia de ${nombre}`,
                descripcion: totalAsistentes > 0 ? `${totalAsistentes} ya registrados` : 'Hoy es su día de reunión',
                icono: 'pi pi-check-square',
                ruta: '/asistencia'
            }));
        const conAlertas = this.asistencia.datos()?.alertas.conAlertas ?? 0;
        if (conAlertas > 0) {
            atajos.push({ etiqueta: `Personas de tu red con alerta (${conAlertas})`, descripcion: 'Necesitan seguimiento', icono: 'pi pi-bell', ruta: '/asistencia/seguimiento' });
        }
        const casos = this.misCasos.datos();
        if (casos && casos.asignados > 0) {
            atajos.push({ etiqueta: `Mis asignados (${casos.asignados})`, descripcion: casos.paraHoy > 0 ? `${casos.paraHoy} para contactar hoy` : 'Consolidación', icono: 'pi pi-heart', ruta: '/consolidacion/mis-asignados' });
        }
        return atajos;
    });
}
