import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { environment } from '@/environments/environment';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { RUTA_INICIO_POR_ROL } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AppMenuitem } from './app.menuitem';

/** Entrada del menú con el permiso que exige; sin `permiso`, la ven todos. */
export interface EntradaMenu extends MenuItem {
    readonly permiso?: Permiso;
    readonly items?: EntradaMenu[];
}

/** Entradas propias de la aplicación. «Inicio» se agrega aparte porque su ruta depende del perfil. */
const ENTRADAS_APP: EntradaMenu[] = [
    {
        label: 'Iglesia',
        items: [{ label: 'Miembros', icon: 'pi pi-fw pi-users', routerLink: ['/miembros'], permiso: 'miembros.ver' }]
    },
    {
        label: 'Administración',
        items: [{ label: 'Roles', icon: 'pi pi-fw pi-shield', routerLink: ['/roles'], permiso: 'roles.ver' }]
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

/**
 * Arma el menú del perfil activo: filtra por los permisos de ese perfil (no por la unión de todos los del usuario),
 * quita grupos vacíos y en producción omite las demos. «Inicio» apunta a la página del rol base.
 */
export function construirMenu(perfil: Perfil | null, produccion: boolean): MenuItem[] {
    const inicio: EntradaMenu[] = perfil ? [{ label: 'Principal', items: [{ label: 'Inicio', icon: 'pi pi-fw pi-home', routerLink: [RUTA_INICIO_POR_ROL[perfil.rolBase]] }] }] : [];
    const entradas = [...inicio, ...ENTRADAS_APP, ...(produccion ? [] : ENTRADAS_DEMO)];
    return filtrarPorPermisos(entradas, new Set(perfil?.permisos ?? []));
}

function filtrarPorPermisos(entradas: readonly EntradaMenu[], permisos: ReadonlySet<Permiso>): MenuItem[] {
    return entradas
        .filter(({ permiso }) => !permiso || permisos.has(permiso))
        .map(({ permiso: _permiso, items, ...entrada }): MenuItem => (items ? { ...entrada, items: filtrarPorPermisos(items, permisos) } : entrada))
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

    protected readonly model = computed(() => construirMenu(this.sesion.perfilActivo(), environment.production));
}
