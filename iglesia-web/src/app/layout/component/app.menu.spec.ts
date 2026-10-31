import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Rol, ROLES } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
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
            const grupos = construirMenu(perfilBase('MIEMBRO'), true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal']);
        });

        it('el perfil Líder ve «Miembros», «Asistencia», «Seguimiento» y «Mis asignados» pero no los aportes, los grupos, los niveles ni la configuración', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilBase('LIDER'), true));

            expect(etiquetasMenu).toEqual(['Principal', 'Inicio', 'Iglesia', 'Miembros', 'Asistencia', 'Seguimiento', 'Mis asignados']);
        });

        it('el Administrador de iglesia ve los grupos con el nombre de su iglesia, «Aportes» y la configuración de aportes', () => {
            const menu = construirMenu(perfilBase('ADMIN_IGLESIA'), true, 'Hogares de paz');
            const iglesia = menu.find((grupo) => grupo.label === 'Iglesia');
            const administracion = menu.find((grupo) => grupo.label === 'Administración');

            expect(iglesia?.items?.map((item) => item.label)).toEqual(['Miembros', 'Hogares de paz', 'Aportes', 'Asistencia', 'Seguimiento', 'Consolidación', 'Mis asignados']);
            expect(iglesia?.items?.[1].routerLink).toEqual(['/grupos']);
            expect(iglesia?.items?.[1].icon).toContain('pi-sitemap');
            expect(iglesia?.items?.[2].routerLink).toEqual(['/aportes']);
            expect(iglesia?.items?.[2].icon).toContain('pi-wallet');
            expect(administracion?.items?.at(-3)?.label).toBe('Configuración de aportes');
            expect(administracion?.items?.at(-3)?.routerLink).toEqual(['/aportes/configuracion']);
            expect(administracion?.items?.at(-3)?.icon).toContain('pi-cog');
        });

        it('consolidación: el coordinador ve la gestión y los resultados; el consolidador, solo «Mis asignados»', () => {
            const coordinador = etiquetas(construirMenu(perfilPersonalizado(11, 'Coordinación', 'ADMIN_IGLESIA', ['consolidacion.administrar']), true));
            const consolidador = etiquetas(construirMenu(perfilPersonalizado(12, 'Consolidación', 'LIDER', ['consolidacion.contactar']), true));
            const supervisor = etiquetas(construirMenu(perfilPersonalizado(13, 'Pastoral', 'ADMIN_IGLESIA', ['consolidacion.ver']), true));

            expect(coordinador).toEqual(jasmine.arrayContaining(['Consolidación', 'Mis asignados', 'Resultados de consolidación']));
            expect(consolidador).toContain('Mis asignados');
            expect(consolidador).not.toContain('Consolidación');
            expect(supervisor).toContain('Consolidación');
            expect(supervisor).not.toContain('Mis asignados');
        });

        it('sin nombre configurado los grupos se llaman «Grupos»', () => {
            expect(etiquetas(construirMenu(perfilBase('ADMIN_IGLESIA'), true))).toContain('Grupos');
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

            expect(administracion?.items?.map((item) => item.label)).toEqual(['Mi iglesia', 'Usuarios', 'Roles', 'Niveles', 'Configuración de aportes', 'Horarios de culto', 'Resultados de consolidación']);
            expect(administracion?.items?.[1].routerLink).toEqual(['/usuarios']);
            expect(administracion?.items?.[1].icon).toContain('pi-users');
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

        it('dentro de una iglesia, el Administrador del sistema ve «Roles» (tiene roles.ver) pero no «Miembros»', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilBase('ADMIN_SISTEMA'), true));

            expect(etiquetasMenu).toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('con iglesias.ver aparece «Sistema › Iglesias», antes del grupo «Iglesia»', () => {
            const menu = construirMenu(perfilBase('ADMIN_SISTEMA'), false);
            const sistema = menu.find((grupo) => grupo.label === 'Sistema');

            expect(sistema?.items?.map((item) => item.label)).toEqual(['Iglesias']);
            expect(sistema?.items?.[0].routerLink).toEqual(['/iglesias']);
            expect(sistema?.items?.[0].icon).toContain('pi-building');
            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Sistema']);
        });

        it('sin iglesias.ver no aparece «Sistema»', () => {
            expect(construirMenu(perfilBase('ADMIN_IGLESIA'), true).map((grupo) => grupo.label)).not.toContain('Sistema');
        });

        it('sin iglesia en la que operar oculta «Miembros», «Grupos», «Niveles», «Usuarios» y «Roles» aunque haya permiso', () => {
            const etiquetasMenu = etiquetas(construirMenu(perfilPersonalizado(1, 'Administrador del sistema', 'ADMIN_SISTEMA', ['iglesias.ver', 'miembros.ver', 'grupos.ver', 'niveles.administrar', 'usuarios.ver', 'roles.ver']), false));

            expect(etiquetasMenu).toEqual(['Principal', 'Inicio', 'Sistema', 'Iglesias']);
            expect(etiquetasMenu).not.toContain('Usuarios');
            expect(etiquetasMenu).not.toContain('Roles');
            expect(etiquetasMenu).not.toContain('Miembros');
        });

        it('el Administrador del sistema dentro de una iglesia, con los permisos de esa iglesia, ve «Iglesias» y las páginas de la iglesia', () => {
            const dentro = perfilPersonalizado(1, 'Administrador del sistema', 'ADMIN_SISTEMA', ['iglesias.ver', 'iglesias.administrar', 'miembros.ver', 'usuarios.ver', 'roles.ver']);

            const menu = construirMenu(dentro, true);

            expect(menu.map((grupo) => grupo.label)).toEqual(['Principal', 'Sistema', 'Iglesia', 'Administración']);
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

        it('solo tiene opciones del sistema: las demos de Sakai no aparecen en ningún entorno', () => {
            const grupos = construirMenu(perfilBase('ADMIN_IGLESIA'), true).map((grupo) => grupo.label);

            expect(grupos).toEqual(['Principal', 'Iglesia', 'Administración']);
            GRUPOS_DEMO.forEach((demo) => expect(grupos).not.toContain(demo));
        });

        it('con iglesia.personalizar aparece «Administración › Mi iglesia»; sin él, no', () => {
            const administracion = construirMenu(perfilBase('ADMIN_IGLESIA'), true).find((grupo) => grupo.label === 'Administración');

            expect(administracion?.items?.[0]).toEqual(jasmine.objectContaining({ label: 'Mi iglesia', routerLink: ['/mi-iglesia'] }));
            expect(etiquetas(construirMenu(perfilBase('LIDER'), true))).not.toContain('Mi iglesia');
        });

        it('«Asistencia» aparece con cualquiera de sus permisos y «Horarios de culto» solo con asistencia.configurar', () => {
            const ujier = construirMenu(perfilPersonalizado(12, 'Ujier', 'ADMIN_IGLESIA', ['asistencia.registrar']), true);
            const asistencia = ujier.find((grupo) => grupo.label === 'Iglesia')?.items?.[0];

            expect(asistencia).toEqual(jasmine.objectContaining({ label: 'Asistencia', routerLink: ['/asistencia'] }));
            expect(asistencia?.icon).toContain('pi-check-square');
            expect(etiquetas(ujier)).not.toContain('Horarios de culto');
            expect(etiquetas(construirMenu(perfilPersonalizado(13, 'Consulta', 'ADMIN_IGLESIA', ['asistencia.ver']), true))).toContain('Asistencia');
            const configuracion = construirMenu(perfilPersonalizado(14, 'Agenda', 'ADMIN_IGLESIA', ['asistencia.configurar']), true);
            expect(configuracion.find((grupo) => grupo.label === 'Administración')?.items?.[0]).toEqual(jasmine.objectContaining({ label: 'Horarios de culto', routerLink: ['/asistencia/configuracion'] }));
            expect(etiquetas(configuracion)).not.toContain('Asistencia');
        });

        it('«Seguimiento» aparece con asistencia.ver o asistencia.grupo, no con solo tomar asistencia', () => {
            const consulta = construirMenu(perfilPersonalizado(15, 'Consulta', 'ADMIN_IGLESIA', ['asistencia.ver']), true);
            const seguimiento = consulta.find((grupo) => grupo.label === 'Iglesia')?.items?.find((item) => item.label === 'Seguimiento');

            expect(seguimiento).toEqual(jasmine.objectContaining({ routerLink: ['/asistencia/seguimiento'] }));
            expect(seguimiento?.icon).toContain('pi-chart-line');
            expect(etiquetas(construirMenu(perfilPersonalizado(16, 'Ujier', 'ADMIN_IGLESIA', ['asistencia.registrar']), true))).not.toContain('Seguimiento');
        });

        it('no expone las propiedades internas de permiso ni de iglesia requerida', () => {
            const json = JSON.stringify(construirMenu(perfilBase('ADMIN_IGLESIA'), true));

            expect(json).not.toContain('"permisos"');
            expect(json).not.toContain('"requiereIglesia"');
        });
    });

    describe('AppMenu', () => {
        const perfilActivo = signal<Perfil | null>(perfilBase('MIEMBRO'));
        const operaEnIglesia = signal(true);
        const nombresGrupos = signal({ nombreGrupoSingular: 'Grupo', nombreGrupoPlural: 'Grupos' });
        let fixture: ComponentFixture<AppMenu>;

        beforeEach(async () => {
            perfilActivo.set(perfilBase('MIEMBRO'));
            operaEnIglesia.set(true);
            await TestBed.configureTestingModule({
                imports: [AppMenu],
                providers: [provideRouter([]), { provide: SesionService, useValue: { perfilActivo, operaEnIglesia } }, { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos } }]
            }).compileComponents();
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

        it('se actualiza cuando la iglesia cambia el nombre de sus grupos', async () => {
            perfilActivo.set(perfilBase('ADMIN_IGLESIA'));
            nombresGrupos.set({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' });
            await fixture.whenStable();

            expect(texto()).toContain('Células');
        });
    });
});
