import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { RUTA_INICIO_POR_ROL } from '@/app/core/auth/models/rol';
import { veAportesDeTodaLaIglesia } from '@/app/features/aportes/models/ambito-aportes';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NOMBRES_GRUPOS_POR_DEFECTO } from '@/app/core/configuracion/configuracion-iglesia.service';
import { RUTA_IGLESIAS, RUTA_MI_IGLESIA } from '@/app/core/iglesias/models/rutas-iglesias';
import { AppMenuitem } from './app.menuitem';

/** Entrada del menú con el permiso que exige; sin `permiso`, la ven todos. */
export interface EntradaMenu extends MenuItem {
    readonly permiso?: Permiso;
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
        items: [{ label: 'Iglesias', icon: 'pi pi-fw pi-building', routerLink: [RUTA_IGLESIAS], permiso: 'iglesias.ver' }]
    },
    {
        label: 'Iglesia',
        items: [
            { label: 'Miembros', icon: 'pi pi-fw pi-users', routerLink: ['/miembros'], permiso: 'miembros.ver', requiereIglesia: true },
            { label: nombreGrupos, icon: 'pi pi-fw pi-sitemap', routerLink: ['/grupos'], permiso: 'grupos.ver', requiereIglesia: true },
            { label: etiquetaAportes, icon: 'pi pi-fw pi-wallet', routerLink: ['/aportes'], permiso: 'aportes.ver', requiereIglesia: true }
        ]
    },
    {
        label: 'Administración',
        items: [
            { label: 'Mi iglesia', icon: 'pi pi-fw pi-building', routerLink: [RUTA_MI_IGLESIA], permiso: 'iglesia.personalizar', requiereIglesia: true },
            { label: 'Usuarios', icon: 'pi pi-fw pi-users', routerLink: ['/usuarios'], permiso: 'usuarios.ver', requiereIglesia: true },
            { label: 'Roles', icon: 'pi pi-fw pi-shield', routerLink: ['/roles'], permiso: 'roles.ver', requiereIglesia: true },
            { label: 'Niveles', icon: 'pi pi-fw pi-sort-amount-up', routerLink: ['/niveles'], permiso: 'niveles.administrar', requiereIglesia: true },
            { label: 'Configuración de aportes', icon: 'pi pi-fw pi-cog', routerLink: ['/aportes/configuracion'], permiso: 'aportes.configurar', requiereIglesia: true }
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
        .filter(({ permiso, requiereIglesia }) => (!permiso || alcance.permisos.has(permiso)) && (!requiereIglesia || alcance.operaEnIglesia))
        .map(({ permiso: _permiso, requiereIglesia: _requiereIglesia, items, ...entrada }): MenuItem => (items ? { ...entrada, items: filtrar(items, alcance) } : entrada))
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
