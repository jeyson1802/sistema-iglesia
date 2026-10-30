import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { RUTA_INICIO_POR_ROL } from '@/app/core/auth/models/rol';
import { veAportesDeTodaLaIglesia } from '@/app/features/aportes/models/ambito-aportes';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NOMBRES_GRUPOS_POR_DEFECTO } from '@/app/core/configuracion/configuracion-iglesia.service';
import { RUTA_IGLESIAS, RUTA_MI_IGLESIA } from '@/app/core/iglesias/models/rutas-iglesias';
import { RUTA_SEGUIMIENTO } from '@/app/features/asistencia/models/rutas-asistencia';
import { RUTA_CONSOLIDACION, RUTA_MIS_ASIGNADOS, RUTA_RESULTADOS_CONSOLIDACION } from '@/app/features/consolidacion/models/rutas-consolidacion';
import { AppMenuitem } from './app.menuitem';

/** Entrada del menú con los permisos que exige (basta uno); sin `permisos`, la ven todos. */
export interface EntradaMenu extends MenuItem {
    readonly permisos?: readonly Permiso[];
    /** Muestra datos de una iglesia: se oculta mientras no se opere en ninguna (vista global del administrador del sistema). */
    readonly requiereIglesia?: boolean;
    readonly items?: EntradaMenu[];
}

/**
 * Entradas propias de la aplicación. «Inicio» se agrega aparte porque su ruta depende del perfil; los grupos se
 * muestran con el nombre plural que les da la iglesia (p. ej. «Células») y los aportes, como «Mis aportes» para quien
 * solo ve los suyos.
 */
const entradasApp = (nombreGrupos: string, etiquetaAportes: string): EntradaMenu[] => [
    {
        label: 'Sistema',
        items: [{ label: 'Iglesias', icon: 'pi pi-fw pi-building', routerLink: [RUTA_IGLESIAS], permisos: ['iglesias.ver'] }]
    },
    {
        label: 'Iglesia',
        items: [
            { label: 'Miembros', icon: 'pi pi-fw pi-users', routerLink: ['/miembros'], permisos: ['miembros.ver'], requiereIglesia: true },
            { label: nombreGrupos, icon: 'pi pi-fw pi-sitemap', routerLink: ['/grupos'], permisos: ['grupos.ver'], requiereIglesia: true },
            { label: etiquetaAportes, icon: 'pi pi-fw pi-wallet', routerLink: ['/aportes'], permisos: ['aportes.ver'], requiereIglesia: true },
            { label: 'Asistencia', icon: 'pi pi-fw pi-check-square', routerLink: ['/asistencia'], permisos: ['asistencia.registrar', 'asistencia.grupo', 'asistencia.ver'], requiereIglesia: true },
            { label: 'Seguimiento', icon: 'pi pi-fw pi-chart-line', routerLink: [RUTA_SEGUIMIENTO], permisos: ['asistencia.ver', 'asistencia.grupo'], requiereIglesia: true },
            { label: 'Consolidación', icon: 'pi pi-fw pi-heart', routerLink: [RUTA_CONSOLIDACION], permisos: ['consolidacion.ver', 'consolidacion.administrar'], requiereIglesia: true },
            { label: 'Mis asignados', icon: 'pi pi-fw pi-phone', routerLink: [RUTA_MIS_ASIGNADOS], permisos: ['consolidacion.contactar', 'consolidacion.administrar'], requiereIglesia: true }
        ]
    },
    {
        label: 'Administración',
        items: [
            { label: 'Mi iglesia', icon: 'pi pi-fw pi-building', routerLink: [RUTA_MI_IGLESIA], permisos: ['iglesia.personalizar'], requiereIglesia: true },
            { label: 'Usuarios', icon: 'pi pi-fw pi-users', routerLink: ['/usuarios'], permisos: ['usuarios.ver'], requiereIglesia: true },
            { label: 'Roles', icon: 'pi pi-fw pi-shield', routerLink: ['/roles'], permisos: ['roles.ver'], requiereIglesia: true },
            { label: 'Niveles', icon: 'pi pi-fw pi-sort-amount-up', routerLink: ['/niveles'], permisos: ['niveles.administrar'], requiereIglesia: true },
            { label: 'Configuración de aportes', icon: 'pi pi-fw pi-cog', routerLink: ['/aportes/configuracion'], permisos: ['aportes.configurar'], requiereIglesia: true },
            { label: 'Horarios de culto', icon: 'pi pi-fw pi-clock', routerLink: ['/asistencia/configuracion'], permisos: ['asistencia.configurar'], requiereIglesia: true },
            { label: 'Resultados de consolidación', icon: 'pi pi-fw pi-list-check', routerLink: [RUTA_RESULTADOS_CONSOLIDACION], permisos: ['consolidacion.administrar'], requiereIglesia: true }
        ]
    }
];

/** Qué entradas se pueden mostrar: permisos del perfil activo y si hay una iglesia en la que operar. */
interface AlcanceMenu {
    readonly permisos: ReadonlySet<Permiso>;
    readonly operaEnIglesia: boolean;
}

/**
 * Arma el menú del perfil activo: filtra por los permisos de ese perfil (no por la unión de todos los del usuario),
 * oculta las páginas de datos de una iglesia si no se opera en ninguna y quita grupos vacíos. Solo tiene opciones del
 * sistema: las demos de Sakai no aparecen en ningún entorno. «Inicio» apunta a la página del rol base.
 */
export function construirMenu(perfil: Perfil | null, operaEnIglesia: boolean, nombreGrupos = NOMBRES_GRUPOS_POR_DEFECTO.nombreGrupoPlural): MenuItem[] {
    const inicio: EntradaMenu[] = perfil ? [{ label: 'Principal', items: [{ label: 'Inicio', icon: 'pi pi-fw pi-home', routerLink: [RUTA_INICIO_POR_ROL[perfil.rolBase]] }] }] : [];
    const etiquetaAportes = perfil === null || veAportesDeTodaLaIglesia(perfil) ? 'Aportes' : 'Mis aportes';
    const entradas = [...inicio, ...entradasApp(nombreGrupos, etiquetaAportes)];
    return filtrar(entradas, { permisos: new Set(perfil?.permisos ?? []), operaEnIglesia });
}

function filtrar(entradas: readonly EntradaMenu[], alcance: AlcanceMenu): MenuItem[] {
    return entradas
        .filter(({ permisos, requiereIglesia }) => (!permisos || permisos.some((permiso) => alcance.permisos.has(permiso))) && (!requiereIglesia || alcance.operaEnIglesia))
        .map(({ permisos: _permisos, requiereIglesia: _requiereIglesia, items, ...entrada }): MenuItem => (items ? { ...entrada, items: filtrar(items, alcance) } : entrada))
        .filter((entrada) => !entrada.items || entrada.items.length > 0);
}

@Component({
    selector: 'app-menu',
    imports: [AppMenuitem],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.menu.html'
})
export class AppMenu {
    private readonly sesion = inject(SesionService);
    /** Al crearse con el layout, carga el nombre de los grupos de la iglesia en la que se opera. */
    private readonly configuracion = inject(ConfiguracionIglesiaService);

    protected readonly model = computed(() => construirMenu(this.sesion.perfilActivo(), this.sesion.operaEnIglesia(), this.configuracion.nombresGrupos().nombreGrupoPlural));
}
