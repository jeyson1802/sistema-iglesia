import { computed, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PreferenciaModoOscuroService } from '@/app/core/apariencia/services/preferencia-modo-oscuro.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AppTopbar } from './app.topbar';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { Perfil } from '@/app/core/auth/models/perfil';

describe('AppTopbar', () => {
    const tieneVariosPerfiles = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    const actuandoEnIglesia = signal(false);
    const iglesiaId = signal<number | null>(1);
    const nombreComercial = signal<string | null>(null);
    let modoOscuro: jasmine.SpyObj<PreferenciaModoOscuroService>;
    let sesion: jasmine.SpyObj<SesionService>;
    let navegar: jasmine.Spy;
    let fixture: ComponentFixture<AppTopbar>;

    beforeEach(async () => {
        tieneVariosPerfiles.set(false);
        perfilActivo.set(perfilBase('LIDER'));
        actuandoEnIglesia.set(false);
        iglesiaId.set(1);
        sesion = jasmine.createSpyObj<SesionService>('SesionService', ['cerrarSesion', 'salirDeIglesia'], {
            usuarioActual: computed(() => ({
                id: 1,
                correo: 'lider@iglesia.test',
                iglesiaId: iglesiaId(),
                miembroId: 2,
                debeCambiarContrasena: false,
                actuandoEnIglesia: actuandoEnIglesia(),
                perfiles: [perfilBase('LIDER'), perfilBase('MIEMBRO')]
            })),
            perfilActivo,
            tieneVariosPerfiles,
            actuandoEnIglesia
        });
        sesion.cerrarSesion.and.returnValue(of(undefined));
        nombreComercial.set(null);
        modoOscuro = jasmine.createSpyObj<PreferenciaModoOscuroService>('PreferenciaModoOscuroService', ['alternar']);
        const identidad = { nombreComercial: computed(() => nombreComercial()), urlLogo: signal(null), cargando: signal(false) };
        await TestBed.configureTestingModule({
            imports: [AppTopbar],
            providers: [provideRouter([]), { provide: SesionService, useValue: sesion }, { provide: IdentidadIglesiaService, useValue: identidad }, { provide: PreferenciaModoOscuroService, useValue: modoOscuro }]
        }).compileComponents();
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

    it('ya no ofrece el configurador de colores: solo el modo oscuro, que recuerda cada persona', () => {
        expect(html().querySelector('.pi-palette')).toBeNull();
        expect(html().querySelector('app-configurator')).toBeNull();

        html().querySelector<HTMLButtonElement>('button[aria-label="Usar modo oscuro"]')!.click();

        expect(modoOscuro.alternar).toHaveBeenCalledTimes(1);
    });

    it('el enlace al inicio lleva el nombre comercial, que en el móvil no se ve', async () => {
        nombreComercial.set('MMM Lima Centro');
        await fixture.whenStable();

        expect(html().querySelector('.layout-topbar-logo')?.getAttribute('aria-label')).toBe('MMM Lima Centro: ir a la página de inicio');
        expect(html().querySelector('.layout-topbar-logo')?.textContent).toContain('MMM Lima Centro');
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

    describe('administrador del sistema dentro de una iglesia', () => {
        const entrarA = async (id: number): Promise<void> => {
            iglesiaId.set(id);
            actuandoEnIglesia.set(true);
            await fixture.whenStable();
        };
        const botonSalir = (): HTMLButtonElement | null => html().querySelector('button[aria-label^="Salir de la iglesia"]');

        it('fuera de una iglesia no muestra el indicador', () => {
            expect(html().textContent).not.toContain('Operando en');
            expect(botonSalir()).toBeNull();
        });

        it('muestra «Operando en: <nombre comercial>» y el botón para salir', async () => {
            nombreComercial.set('Iglesia del Norte');
            await entrarA(5);

            expect(html().textContent).toContain('Operando en: Iglesia del Norte');
            expect(botonSalir()?.textContent).toContain('Salir de la iglesia');
        });

        it('mientras no tiene el nombre comercial muestra el número de la iglesia', async () => {
            await entrarA(5);

            expect(html().textContent).toContain('Operando en: Iglesia 5');
        });

        it('«Salir de la iglesia» vuelve a la vista global y navega al catálogo de iglesias', async () => {
            await entrarA(5);
            sesion.salirDeIglesia.and.callFake(() => {
                actuandoEnIglesia.set(false);
                return of(undefined);
            });

            botonSalir()!.click();
            await fixture.whenStable();

            expect(sesion.salirDeIglesia).toHaveBeenCalledTimes(1);
            expect(navegar).toHaveBeenCalledOnceWith('/iglesias');
            expect(html().textContent).not.toContain('Operando en');
        });

        it('si falla la salida se queda en la iglesia y vuelve a habilitar el botón', async () => {
            await entrarA(5);
            sesion.salirDeIglesia.and.returnValue(throwError(() => new Error('fallo')));

            botonSalir()!.click();
            await fixture.whenStable();

            expect(navegar).not.toHaveBeenCalled();
            expect(botonSalir()!.disabled).toBeFalse();
        });
    });
});
