import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { Permiso } from '@/app/core/auth/models/permiso';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { ReunionesService } from '@/app/features/asistencia/services/reuniones.service';
import { formatearMonto } from '@/app/shared/pipes/moneda.pipe';
import { AtajosDelDiaComponent } from '../components/atajos-del-dia/atajos-del-dia';
import { BienvenidaPerfilComponent } from '../components/bienvenida-perfil/bienvenida-perfil';
import { BloqueAportesComponent } from '../components/bloque-aportes/bloque-aportes';
import { BloqueAsistenciaComponent } from '../components/bloque-asistencia/bloque-asistencia';
import { BloqueConsolidacionComponent } from '../components/bloque-consolidacion/bloque-consolidacion';
import { BloqueGruposComponent } from '../components/bloque-grupos/bloque-grupos';
import { BloqueMiembrosComponent } from '../components/bloque-miembros/bloque-miembros';
import { BloqueUsoComponent } from '../components/bloque-uso/bloque-uso';
import { SelectorPeriodoComponent } from '../components/selector-periodo/selector-periodo';
import { TarjetaIndicadorComponent } from '../components/tarjeta-indicador/tarjeta-indicador';
import { Atajo } from '../models/atajo';
import { formatoNumero, textoRango } from '../models/estadisticas';
import { EstadisticasService } from '../services/estadisticas.service';
import { cargaReactiva, SIN_PARAMETRO } from '../utils/carga-reactiva';
import { periodoRecordado } from '../utils/periodo-recordado';

const ROL: Rol = 'ADMIN_IGLESIA';

/**
 * Inicio de administración de la iglesia (contenedor): atajos del día, cifras clave y un bloque por módulo. Cada
 * bloque aparece solo con el permiso de su módulo (sin permiso no se hace la petición) y carga, falla y se reintenta
 * por su cuenta. El administrador del sistema dentro de una iglesia ve este mismo inicio.
 */
@Component({
    selector: 'app-inicio-administracion',
    imports: [
        SkeletonModule,
        AtajosDelDiaComponent,
        BienvenidaPerfilComponent,
        BloqueAportesComponent,
        BloqueAsistenciaComponent,
        BloqueConsolidacionComponent,
        BloqueGruposComponent,
        BloqueMiembrosComponent,
        BloqueUsoComponent,
        SelectorPeriodoComponent,
        TarjetaIndicadorComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './inicio-administracion.html'
})
export class InicioAdministracionComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(EstadisticasService);
    private readonly reuniones = inject(ReunionesService);
    private readonly identidad = inject(IdentidadIglesiaService);

    protected readonly icono = ICONO_ROL[ROL];
    protected readonly etiquetaPerfil = computed(() => this.sesion.perfilActivo()?.nombre ?? ETIQUETA_ROL[ROL]);
    protected readonly titulo = computed(() => this.identidad.nombreComercial() ?? 'Inicio de la iglesia');
    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    protected readonly numero = formatoNumero;
    protected readonly periodo = periodoRecordado();

    private readonly puede = (...permisos: Permiso[]) => computed(() => permisos.some((permiso) => this.sesion.tienePermiso(permiso)));
    protected readonly verMiembros = this.puede('miembros.ver');
    protected readonly verGrupos = this.puede('grupos.ver');
    protected readonly verAsistencia = this.puede('asistencia.ver');
    protected readonly verConsolidacion = this.puede('consolidacion.ver', 'consolidacion.administrar');
    protected readonly verAportes = this.puede('aportes.ver');
    protected readonly verUso = this.puede('usuarios.ver');
    private readonly puedeRegistrarMiembro = this.puede('miembros.crear');
    private readonly puedeTomarAsistencia = this.puede('asistencia.registrar', 'asistencia.grupo');

    protected readonly miembros = cargaReactiva(this.periodo, (periodo) => this.servicio.miembros(periodo), this.verMiembros);
    protected readonly asistencia = cargaReactiva(this.periodo, (periodo) => this.servicio.asistencia(periodo), this.verAsistencia);
    protected readonly grupos = cargaReactiva(this.periodo, (periodo) => this.servicio.grupos(periodo), this.verGrupos);
    protected readonly consolidacion = cargaReactiva(this.periodo, (periodo) => this.servicio.consolidacion(periodo), this.verConsolidacion);
    protected readonly aportes = cargaReactiva(this.periodo, (periodo) => this.servicio.aportes(periodo), this.verAportes);
    protected readonly uso = cargaReactiva(this.periodo, (periodo) => this.servicio.uso(periodo), this.verUso);
    private readonly reunionesHoy = cargaReactiva(SIN_PARAMETRO, () => this.reuniones.hoy(), this.puedeTomarAsistencia);

    protected readonly rango = computed(() => {
        const periodo = this.miembros.datos()?.periodo ?? this.asistencia.datos()?.periodo;
        return periodo ? textoRango(periodo) : null;
    });
    protected readonly totalAportes = computed(() => {
        const resumen = this.aportes.datos()?.resumen;
        return resumen ? formatearMonto(resumen.total, resumen.moneda) : '';
    });
    protected readonly columnasTarjetas = computed(() => (this.verAportes() ? 'xl:grid-cols-5' : 'xl:grid-cols-4'));

    /** Lo más urgente primero: tomar la asistencia de hoy; luego las alertas, las rondas y registrar un miembro. */
    protected readonly atajos = computed<readonly Atajo[]>(() => {
        const atajos: Atajo[] = [];
        const hoy = this.reunionesHoy.datos() ?? [];
        if (hoy.length > 0) {
            atajos.push({ etiqueta: 'Tomar asistencia de hoy', descripcion: hoy.length === 1 ? hoy[0].nombre : `${hoy.length} reuniones programadas`, icono: 'pi pi-check-square', ruta: '/asistencia' });
        }
        const conAlertas = this.asistencia.datos()?.alertas.conAlertas ?? 0;
        if (conAlertas > 0) {
            atajos.push({ etiqueta: `Personas con alerta (${conAlertas})`, descripcion: 'Necesitan seguimiento', icono: 'pi pi-bell', ruta: '/asistencia/seguimiento' });
        }
        const rondas = this.consolidacion.datos()?.rondasEnCurso ?? 0;
        if (rondas > 0) {
            atajos.push({ etiqueta: `Rondas en curso (${rondas})`, descripcion: 'Consolidación de los nuevos', icono: 'pi pi-heart', ruta: '/consolidacion' });
        }
        if (this.puedeRegistrarMiembro()) {
            atajos.push({ etiqueta: 'Registrar miembro', descripcion: 'Alta de una persona nueva', icono: 'pi pi-user-plus', ruta: '/miembros/nuevo' });
        }
        return atajos;
    });
}
