import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AppMenu, construirMenu } from './app.menu';

describe('Menú lateral', () => {
    const GRUPOS_DEMO = ['UI Components', 'Pages', 'Hierarchy', 'Get Started'];
    const etiquetas = (menu: MenuItem[]): string[] => menu.flatMap((item) => [item.label ?? '', ...etiquetas(item.items ?? [])]);

    describe('construirMenu', () => {
        it('con el perfil Miembro muestra «Inicio» hacia su página y no muestra «Miembros»', () => {
            const menu = construirMenu('MIEMBRO', true);

            expect(etiquetas(menu)).toContain('Inicio');
            expect(etiquetas(menu)).not.toContain('Miembros');
            expect(menu[0].items?.[0].routerLink).toEqual(['/mi-perfil']);
        });

        it('quita los grupos que quedan vacíos para el perfil', () => {
            expect(etiquetas(construirMenu('LIDER', true))).not.toContain('Iglesia');
        });

        it('con el perfil Administrador de iglesia muestra «Miembros»', () => {
            const menu = construirMenu('ADMIN_IGLESIA', true);

            expect(etiquetas(menu)).toContain('Miembros');
            expect(menu[0].items?.[0].routerLink).toEqual(['/administracion']);
        });

        it('cada perfil apunta «Inicio» a su propia ruta', () => {
            const rutas: Record<Rol, string> = { ADMIN_SISTEMA: '/sistema', ADMIN_IGLESIA: '/administracion', LIDER: '/mi-red', MIEMBRO: '/mi-perfil' };

            for (const [perfil, ruta] of Object.entries(rutas) as [Rol, string][]) {
                expect(construirMenu(perfil, true)[0].items?.[0].routerLink).toEqual([ruta]);
            }
        });

        it('en producción no muestra los grupos demo de Sakai', () => {
            const grupos = construirMenu('ADMIN_IGLESIA', true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', 'Iglesia']);
        });

        it('en desarrollo agrega los grupos demo al final', () => {
            const grupos = construirMenu('MIEMBRO', false).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', ...GRUPOS_DEMO]);
        });

        it('no expone la propiedad interna de roles', () => {
            expect(JSON.stringify(construirMenu('ADMIN_IGLESIA', false))).not.toContain('"roles"');
        });
    });

    describe('AppMenu', () => {
        const perfilActivo = signal<Rol | null>('MIEMBRO');
        let fixture: ComponentFixture<AppMenu>;

        beforeEach(async () => {
            perfilActivo.set('MIEMBRO');
            await TestBed.configureTestingModule({ imports: [AppMenu], providers: [provideRouter([]), { provide: SesionService, useValue: { perfilActivo } }] }).compileComponents();
            fixture = TestBed.createComponent(AppMenu);
            await fixture.whenStable();
        });

        const texto = (): string => fixture.nativeElement.textContent;

        it('se actualiza al cambiar el perfil activo', async () => {
            expect(texto()).not.toContain('Miembros');

            perfilActivo.set('ADMIN_IGLESIA');
            await fixture.whenStable();

            expect(texto()).toContain('Miembros');
        });
    });
});
