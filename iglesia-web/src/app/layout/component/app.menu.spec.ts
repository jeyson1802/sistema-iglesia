import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Rol, ROLES } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { AppMenu, construirMenu } from './app.menu';

describe('Menú lateral', () => {
    const GRUPOS_DEMO = ['UI Components', 'Pages', 'Hierarchy', 'Get Started'];
    const etiquetas = (menu: MenuItem[]): string[] => menu.flatMap((item) => [item.label ?? '', ...etiquetas(item.items ?? [])]);
    const secretaria = perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver', 'miembros.crear']);

    describe('construirMenu', () => {
        it('con el perfil Miembro muestra «Inicio» hacia su página y no muestra «Miembros» ni «Roles»', () => {
            const menu = construirMenu(perfilBase('MIEMBRO'), true, true);

            expect(etiquetas(menu)).toContain('Inicio');
            expect(etiquetas(menu)).not.toContain('Miembros');
            expect(etiquetas(menu)).not.toContain('Roles');
            expect(menu[0].items?.[0].routerLink).toEqual(['/mi-perfil']);
        });

        it('quita los grupos que quedan vacíos para el perfil', () => {
            const grupos = construirMenu(perfilBase('LIDER'), true, true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal']);
        });

        it('con el perfil Administrador de iglesia muestra «Miembros» y «Roles»', () => {
            const menu = construirMenu(perfilBase('ADMIN_IGLESIA'), true, true);

            expect(etiquetas(menu)).toContain('Miembros');
            expect(etiquetas(menu)).toContain('Roles');
            expect(menu[0].items?.[0].routerLink).toEqual(['/administracion']);
        });

        it('con «Secretaría» muestra «Miembros» pero no «Roles», e «Inicio» va a la página de su rol base', () => {
            const menu = construirMenu(secretaria, true, true);

            expect(etiquetas(menu)).toContain('Miembros');
            expect(etiquetas(menu)).not.toContain('Roles');
            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Iglesia']);
            expect(menu[0].items?.[0].routerLink).toEqual(['/administracion']);
        });

        it('cada opción exige su propio permiso: con solo roles.ver aparece «Roles» y no «Miembros»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(11, 'Auditoría', 'ADMIN_IGLESIA', ['roles.ver']), true, true));

            expect(etiquetasMenu).toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('con usuarios.ver aparece «Administración › Usuarios», antes de «Roles»', () => {
            const administracion = construirMenu(perfilBase('ADMIN_IGLESIA'), true, true).find((grupo) => grupo.label === 'Administración');

            expect(administracion?.items?.map((item) => item.label)).toEqual(['Usuarios', 'Roles']);
            expect(administracion?.items?.[0].routerLink).toEqual(['/usuarios']);
            expect(administracion?.items?.[0].icon).toContain('pi-users');
        });

        it('sin usuarios.ver no aparece «Usuarios»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(11, 'Auditoría', 'ADMIN_IGLESIA', ['roles.ver']), true, true));

            expect(etiquetasMenu).not.toContain('Usuarios');
        });

        it('con solo usuarios.ver aparece «Usuarios» y no «Roles»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(12, 'Soporte', 'ADMIN_IGLESIA', ['usuarios.ver']), true, true));

            expect(etiquetasMenu).toContain('Usuarios');
            expect(etiquetasMenu).not.toContain('Roles');
        });

        it('dentro de una iglesia, el Administrador del sistema ve «Roles» (tiene roles.ver) pero no «Miembros»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilBase('ADMIN_SISTEMA'), true, true));

            expect(etiquetasMenu).toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('con iglesias.ver aparece «Sistema › Iglesias», antes del grupo «Iglesia»', () => {
            const menu = construirMenu(perfilBase('ADMIN_SISTEMA'), false, true);
            const sistema = menu.find((grupo) => grupo.label === 'Sistema');

            expect(sistema?.items?.map((item) => item.label)).toEqual(['Iglesias']);
            expect(sistema?.items?.[0].routerLink).toEqual(['/iglesias']);
            expect(sistema?.items?.[0].icon).toContain('pi-building');
            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Sistema']);
        });

        it('sin iglesias.ver no aparece «Sistema»', () => {
            expect(construirMenu(perfilBase('ADMIN_IGLESIA'), true, true).map((grupo) => grupo.label)).not.toContain('Sistema');
        });

        it('sin iglesia en la que operar oculta «Miembros», «Usuarios» y «Roles» aunque haya permiso', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilBase('ADMIN_SISTEMA'), false, true));

            expect(etiquetasMenu).not.toContain('Usuarios');
            expect(etiquetasMenu).not.toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('el Administrador del sistema dentro de una iglesia, con los permisos de esa iglesia, ve «Iglesias» y las páginas de la iglesia', () => {
            const dentro = perfilPersonalizado(1, 'Administrador del sistema', 'ADMIN_SISTEMA', ['iglesias.ver', 'iglesias.administrar', 'miembros.ver', 'usuarios.ver', 'roles.ver']);

            const menu = construirMenu(dentro, true, true);

            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Sistema', 'Iglesia', 'Administración']);
        });

        it('cada perfil apunta «Inicio» a su propia ruta', () => {
            const rutas: Record<Rol, string> = { ADMIN_SISTEMA: '/sistema', ADMIN_IGLESIA: '/administracion', LIDER: '/mi-red', MIEMBRO: '/mi-perfil' };

            for (const rol of ROLES) {
                expect(construirMenu(perfilBase(rol), true, true)[0].items?.[0].routerLink).toEqual([rutas[rol]]);
            }
        });

        it('sin perfil activo solo quedan las entradas sin permiso', () => {
            expect(construirMenu(null, true, true)).toEqual([]);
        });

        it('en producción no muestra los grupos demo de Sakai', () => {
            const grupos = construirMenu(perfilBase('ADMIN_IGLESIA'), true, true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', 'Iglesia', 'Administración']);
        });

        it('en desarrollo agrega los grupos demo al final', () => {
            const grupos = construirMenu(perfilBase('MIEMBRO'), true, false).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', ...GRUPOS_DEMO]);
        });

        it('no expone las propiedades internas de permiso ni de iglesia requerida', () => {
            const json = JSON.stringify(construirMenu(perfilBase('ADMIN_IGLESIA'), true, false));

            expect(json).not.toContain('"permiso"');
            expect(json).not.toContain('"requiereIglesia"');
        });
    });

    describe('AppMenu', () => {
        const perfilActivo = signal<Perfil | null>(perfilBase('MIEMBRO'));
        const operaEnIglesia = signal(true);
        let fixture: ComponentFixture<AppMenu>;

        beforeEach(async () => {
            perfilActivo.set(perfilBase('MIEMBRO'));
            operaEnIglesia.set(true);
            await TestBed.configureTestingModule({ imports: [AppMenu], providers: [provideRouter([]), { provide: SesionService, useValue: { perfilActivo, operaEnIglesia } }] }).compileComponents();
            fixture = TestBed.createComponent(AppMenu);
            await fixture.whenStable();
        });

        const texto = (): string => fixture.nativeElement.textContent;

        it('se actualiza al cambiar el perfil activo', async () => {
            expect(texto()).not.toContain('Miembros');

            perfilActivo.set(secretaria);
            await fixture.whenStable();

            expect(texto()).toContain('Miembros');
            expect(texto()).not.toContain('Roles');
        });

        it('se actualiza al entrar o salir de una iglesia', async () => {
            perfilActivo.set(perfilBase('ADMIN_SISTEMA'));
            operaEnIglesia.set(false);
            await fixture.whenStable();
            expect(texto()).toContain('Iglesias');
            expect(texto()).not.toContain('Usuarios');

            operaEnIglesia.set(true);
            await fixture.whenStable();

            expect(texto()).toContain('Usuarios');
        });
    });
});
