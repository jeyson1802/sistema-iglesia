import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { environment } from '@/environments/environment';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { RUTA_INICIO_POR_ROL } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService, NOMBRES_GRUPOS_POR_DEFECTO } from '@/app/core/configuracion/configuracion-iglesia.service';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
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
 * muestran con el nombre plural que les da la iglesia (p. ej. «Células»).
 */
const entradasApp = (nombreGrupos: string): EntradaMenu[] => [
    {
        label: 'Sistema',
        items: [{ label: 'Iglesias', icon: 'pi pi-fw pi-building', routerLink: [RUTA_IGLESIAS], permiso: 'iglesias.ver' }]
    },
    {
        label: 'Iglesia',
        items: [
            { label: 'Miembros', icon: 'pi pi-fw pi-users', routerLink: ['/miembros'], permiso: 'miembros.ver', requiereIglesia: true },
            { label: nombreGrupos, icon: 'pi pi-fw pi-sitemap', routerLink: ['/grupos'], permiso: 'grupos.ver', requiereIglesia: true }
        ]
    },
    {
        label: 'Administración',
        items: [
            { label: 'Usuarios', icon: 'pi pi-fw pi-users', routerLink: ['/usuarios'], permiso: 'usuarios.ver', requiereIglesia: true },
            { label: 'Roles', icon: 'pi pi-fw pi-shield', routerLink: ['/roles'], permiso: 'roles.ver', requiereIglesia: true },
            { label: 'Niveles', icon: 'pi pi-fw pi-sort-amount-up', routerLink: ['/niveles'], permiso: 'niveles.administrar', requiereIglesia: true }
        ]
    }
];

/** Demos de Sakai: referencia de UI que solo aparece en desarrollo. */
const ENTRADAS_DEMO: EntradaMenu[] = [
    {
        label: 'UI Components',
        items: [
            { label: 'Form Layout', icon: 'pi pi-fw pi-id-card', routerLink: ['/uikit/formlayout'] },
            { label: 'Input', icon: 'pi pi-fw pi-check-square', routerLink: ['/uikit/input'] },
            { label: 'Button', icon: 'pi pi-fw pi-mobile', class: 'rotated-icon', routerLink: ['/uikit/button'] },
            { label: 'Table', icon: 'pi pi-fw pi-table', routerLink: ['/uikit/table'] },
            { label: 'List', icon: 'pi pi-fw pi-list', routerLink: ['/uikit/list'] },
            { label: 'Tree', icon: 'pi pi-fw pi-share-alt', routerLink: ['/uikit/tree'] },
            { label: 'Panel', icon: 'pi pi-fw pi-tablet', routerLink: ['/uikit/panel'] },
            { label: 'Overlay', icon: 'pi pi-fw pi-clone', routerLink: ['/uikit/overlay'] },
            { label: 'Media', icon: 'pi pi-fw pi-image', routerLink: ['/uikit/media'] },
            { label: 'Menu', icon: 'pi pi-fw pi-bars', routerLink: ['/uikit/menu'] },
            { label: 'Message', icon: 'pi pi-fw pi-comment', routerLink: ['/uikit/message'] },
            { label: 'File', icon: 'pi pi-fw pi-file', routerLink: ['/uikit/file'] },
            { label: 'Chart', icon: 'pi pi-fw pi-chart-bar', routerLink: ['/uikit/charts'] },
            { label: 'Timeline', icon: 'pi pi-fw pi-calendar', routerLink: ['/uikit/timeline'] },
            { label: 'Misc', icon: 'pi pi-fw pi-circle', routerLink: ['/uikit/misc'] }
        ]
    },
    {
        label: 'Pages',
        icon: 'pi pi-fw pi-briefcase',
        path: '/pages',
        items: [
            {
                label: 'Landing',
                icon: 'pi pi-fw pi-globe',
                routerLink: ['/landing']
            },
            {
                label: 'Crud',
                icon: 'pi pi-fw pi-pencil',
                routerLink: ['/pages/crud']
            },
            {
                label: 'Not Found',
                icon: 'pi pi-fw pi-exclamation-circle',
                routerLink: ['/pages/notfound']
            },
            {
                label: 'Empty',
                icon: 'pi pi-fw pi-circle-off',
                routerLink: ['/pages/empty']
            }
        ]
    },
    {
        label: 'Hierarchy',
        path: '/hierarchy',
        items: [
            {
                label: 'Submenu 1',
                icon: 'pi pi-fw pi-bookmark',
                path: '/hierarchy/submenu_1',
                items: [
                    {
                        label: 'Submenu 1.1',
                        icon: 'pi pi-fw pi-bookmark',
                        path: '/hierarchy/submenu_1/submenu_1_1',
                        items: [
                            { label: 'Submenu 1.1.1', icon: 'pi pi-fw pi-bookmark' },
                            { label: 'Submenu 1.1.2', icon: 'pi pi-fw pi-bookmark' },
                            { label: 'Submenu 1.1.3', icon: 'pi pi-fw pi-bookmark' }
                        ]
                    },
                    {
                        label: 'Submenu 1.2',
                        icon: 'pi pi-fw pi-bookmark',
                        path: '/hierarchy/submenu_1/submenu_1_2',
                        items: [{ label: 'Submenu 1.2.1', icon: 'pi pi-fw pi-bookmark' }]
                    }
                ]
            },
            {
                label: 'Submenu 2',
                icon: 'pi pi-fw pi-bookmark',
                path: '/hierarchy/submenu_2',
                items: [
                    {
                        label: 'Submenu 2.1',
                        icon: 'pi pi-fw pi-bookmark',
                        path: '/hierarchy/submenu_2/submenu_2_1',
                        items: [
                            { label: 'Submenu 2.1.1', icon: 'pi pi-fw pi-bookmark' },
                            { label: 'Submenu 2.1.2', icon: 'pi pi-fw pi-bookmark' }
                        ]
                    },
                    {
                        label: 'Submenu 2.2',
                        icon: 'pi pi-fw pi-bookmark',
                        path: '/hierarchy/submenu_2/submenu_2_2',
                        items: [{ label: 'Submenu 2.2.1', icon: 'pi pi-fw pi-bookmark' }]
                    }
                ]
            }
        ]
    },
    {
        label: 'Get Started',
        items: [
            { label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/dashboard'] },
            {
                label: 'Documentation',
                icon: 'pi pi-fw pi-book',
                routerLink: ['/documentation']
            },
            {
                label: 'View Source',
                icon: 'pi pi-fw pi-github',
                url: 'https://github.com/primefaces/sakai-ng',
                target: '_blank'
            }
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
 * oculta las páginas de datos de una iglesia si no se opera en ninguna, quita grupos vacíos y en producción omite las
 * demos. «Inicio» apunta a la página del rol base.
 */
export function construirMenu(perfil: Perfil | null, operaEnIglesia: boolean, produccion: boolean, nombreGrupos = NOMBRES_GRUPOS_POR_DEFECTO.nombreGrupoPlural): MenuItem[] {
    const inicio: EntradaMenu[] = perfil ? [{ label: 'Principal', items: [{ label: 'Inicio', icon: 'pi pi-fw pi-home', routerLink: [RUTA_INICIO_POR_ROL[perfil.rolBase]] }] }] : [];
    const entradas = [...inicio, ...entradasApp(nombreGrupos), ...(produccion ? [] : ENTRADAS_DEMO)];
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

    protected readonly model = computed(() => construirMenu(this.sesion.perfilActivo(), this.sesion.operaEnIglesia(), environment.production, this.configuracion.nombresGrupos().nombreGrupoPlural));
}
