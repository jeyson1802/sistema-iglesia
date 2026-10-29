import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AppConfigurator } from './app.configurator';
import { AppTopbar } from './app.topbar';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { Perfil } from '@/app/core/auth/models/perfil';

describe('AppTopbar', () => {
    const tieneVariosPerfiles = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    let sesion: jasmine.SpyObj<SesionService>;
    let navegar: jasmine.Spy;
    let fixture: ComponentFixture<AppTopbar>;

    beforeEach(async () => {
        tieneVariosPerfiles.set(false);
        perfilActivo.set(perfilBase('LIDER'));
        sesion = jasmine.createSpyObj<SesionService>('SesionService', ['cerrarSesion'], {
            usuarioActual: signal({ id: 1, correo: 'lider@iglesia.test', iglesiaId: 1, miembroId: 2, debeCambiarContrasena: false, actuandoEnIglesia: false, perfiles: [perfilBase('LIDER'), perfilBase('MIEMBRO')] }),
            perfilActivo,
            tieneVariosPerfiles
        });
        sesion.cerrarSesion.and.returnValue(of(undefined));
        await TestBed.configureTestingModule({ imports: [AppTopbar], providers: [provideRouter([]), { provide: SesionService, useValue: sesion }] })
            // El configurador de colores de Sakai no es parte de lo que se prueba.
            .overrideComponent(AppTopbar, { remove: { imports: [AppConfigurator] }, add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] } })
            .compileComponents();
        navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
        fixture = TestBed.createComponent(AppTopbar);
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    const abrirMenuUsuario = async (): Promise<void> => {
        html().querySelector<HTMLButtonElement>('#boton-menu-usuario')!.click();
        await fixture.whenStable();
    };

    const opciones = (): string[] => Array.from(html().querySelectorAll('#menu-usuario .p-menu-item-label')).map((e) => e.textContent?.trim() ?? '');

    it('muestra el logo de la iglesia en lugar del logo de Sakai', () => {
        expect(html().querySelector('.layout-topbar-logo app-logo-iglesia')).not.toBeNull();
        expect(html().querySelector('svg[viewBox="0 0 54 40"]')).toBeNull();
    });

    it('muestra el correo y el nombre del perfil activo', () => {
        const boton = html().querySelector('#boton-menu-usuario')!;

        expect(boton.textContent).toContain('lider@iglesia.test');
        expect(boton.textContent).toContain('Líder');
    });

    it('con un perfil personalizado muestra su nombre, no el del rol base', async () => {
        perfilActivo.set(perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver']));
        await fixture.whenStable();
        const boton = html().querySelector('#boton-menu-usuario')!;

        expect(boton.textContent).toContain('Secretaría');
        expect(boton.textContent).not.toContain('Administrador de iglesia');
    });

    it('ya no muestra los botones demo de calendario, mensajes y perfil', () => {
        expect(html().textContent).not.toMatch(/Calendar|Messages|Profile/);
    });

    it('con un solo rol ofrece cambiar contraseña y cerrar sesión, sin cambiar perfil', async () => {
        await abrirMenuUsuario();

        expect(opciones()).toEqual(['Cambiar contraseña', 'Cerrar sesión']);
    });

    it('con varios roles ofrece también cambiar perfil', async () => {
        tieneVariosPerfiles.set(true);
        await abrirMenuUsuario();

        expect(opciones()).toEqual(['Cambiar perfil', 'Cambiar contraseña', 'Cerrar sesión']);
    });

    it('«Cerrar sesión» cierra la sesión y vuelve al login reemplazando el historial', async () => {
        await abrirMenuUsuario();
        const cerrar = Array.from(html().querySelectorAll<HTMLElement>('#menu-usuario .p-menu-item-content')).find((e) => e.textContent?.includes('Cerrar sesión'))!;

        cerrar.click();
        await fixture.whenStable();

        expect(sesion.cerrarSesion).toHaveBeenCalledTimes(1);
        expect(navegar).toHaveBeenCalledOnceWith('/auth/login', { replaceUrl: true });
    });
});
