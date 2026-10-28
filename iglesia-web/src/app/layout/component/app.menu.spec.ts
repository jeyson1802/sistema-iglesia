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
            const menu = construirMenu(perfilBase('MIEMBRO'), true);

            expect(etiquetas(menu)).toContain('Inicio');
            expect(etiquetas(menu)).not.toContain('Miembros');
            expect(etiquetas(menu)).not.toContain('Roles');
            expect(menu[0].items?.[0].routerLink).toEqual(['/mi-perfil']);
        });

        it('quita los grupos que quedan vacíos para el perfil', () => {
            const grupos = construirMenu(perfilBase('LIDER'), true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal']);
        });

        it('con el perfil Administrador de iglesia muestra «Miembros» y «Roles»', () => {
            const menu = construirMenu(perfilBase('ADMIN_IGLESIA'), true);

            expect(etiquetas(menu)).toContain('Miembros');
            expect(etiquetas(menu)).toContain('Roles');
            expect(menu[0].items?.[0].routerLink).toEqual(['/administracion']);
        });

        it('con «Secretaría» muestra «Miembros» pero no «Roles», e «Inicio» va a la página de su rol base', () => {
            const menu = construirMenu(secretaria, true);

            expect(etiquetas(menu)).toContain('Miembros');
            expect(etiquetas(menu)).not.toContain('Roles');
            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Iglesia']);
            expect(menu[0].items?.[0].routerLink).toEqual(['/administracion']);
        });

        it('cada opción exige su propio permiso: con solo roles.ver aparece «Roles» y no «Miembros»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(11, 'Auditoría', 'ADMIN_IGLESIA', ['roles.ver']), true));

            expect(etiquetasMenu).toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('con usuarios.ver aparece «Administración › Usuarios», antes de «Roles»', () => {
            const administracion = construirMenu(perfilBase('ADMIN_IGLESIA'), true).find((grupo) => grupo.label === 'Administración');

            expect(administracion?.items?.map((item) => item.label)).toEqual(['Usuarios', 'Roles']);
            expect(administracion?.items?.[0].routerLink).toEqual(['/usuarios']);
            expect(administracion?.items?.[0].icon).toContain('pi-users');
        });

        it('sin usuarios.ver no aparece «Usuarios»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(11, 'Auditoría', 'ADMIN_IGLESIA', ['roles.ver']), true));

            expect(etiquetasMenu).not.toContain('Usuarios');
        });

        it('con solo usuarios.ver aparece «Usuarios» y no «Roles»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(12, 'Soporte', 'ADMIN_IGLESIA', ['usuarios.ver']), true));

            expect(etiquetasMenu).toContain('Usuarios');
            expect(etiquetasMenu).not.toContain('Roles');
        });

        it('el Administrador del sistema ve «Roles» (tiene roles.ver) pero no «Miembros»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilBase('ADMIN_SISTEMA'), true));

            expect(etiquetasMenu).toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('cada perfil apunta «Inicio» a su propia ruta', () => {
            const rutas: Record<Rol, string> = { ADMIN_SISTEMA: '/sistema', ADMIN_IGLESIA: '/administracion', LIDER: '/mi-red', MIEMBRO: '/mi-perfil' };

            for (const rol of ROLES) {
                expect(construirMenu(perfilBase(rol), true)[0].items?.[0].routerLink).toEqual([rutas[rol]]);
            }
        });

        it('sin perfil activo solo quedan las entradas sin permiso', () => {
            expect(construirMenu(null, true)).toEqual([]);
        });

        it('en producción no muestra los grupos demo de Sakai', () => {
            const grupos = construirMenu(perfilBase('ADMIN_IGLESIA'), true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', 'Iglesia', 'Administración']);
        });

        it('en desarrollo agrega los grupos demo al final', () => {
            const grupos = construirMenu(perfilBase('MIEMBRO'), false).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', ...GRUPOS_DEMO]);
        });

        it('no expone la propiedad interna de permiso', () => {
            expect(JSON.stringify(construirMenu(perfilBase('ADMIN_IGLESIA'), false))).not.toContain('"permiso"');
        });
    });

    describe('AppMenu', () => {
        const perfilActivo = signal<Perfil | null>(perfilBase('MIEMBRO'));
        let fixture: ComponentFixture<AppMenu>;

        beforeEach(async () => {
            perfilActivo.set(perfilBase('MIEMBRO'));
            await TestBed.configureTestingModule({ imports: [AppMenu], providers: [provideRouter([]), { provide: SesionService, useValue: { perfilActivo } }] }).compileComponents();
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
    });
});
