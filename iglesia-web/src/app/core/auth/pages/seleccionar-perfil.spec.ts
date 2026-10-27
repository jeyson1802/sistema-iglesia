import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { Rol } from '../models/rol';
import { UsuarioActual } from '../models/usuario-actual';
import { SesionService } from '../services/sesion.service';
import { perfilBase, perfilPersonalizado, usuarioCon } from '../testing/perfiles-prueba';
import { SIN_CONFIGURADOR_DE_TEMA } from '../testing/sin-configurador-de-tema';
import { SeleccionarPerfilComponent } from './seleccionar-perfil';
import { Perfil } from '@/app/core/auth/models/perfil';

describe('SeleccionarPerfilComponent', () => {
    const usuarioActual = signal<UsuarioActual | null>(null);
    const perfilActivo = signal<Perfil | null>(null);
    let sesion: jasmine.SpyObj<SesionService>;
    let navegar: jasmine.Spy;
    let fixture: ComponentFixture<SeleccionarPerfilComponent>;

    const secretaria = perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver', 'miembros.crear']);

    const crear = async (perfiles: (Rol | Perfil)[], perfil: Rol | Perfil | null = null): Promise<void> => {
        usuarioActual.set(usuarioCon(...perfiles));
        perfilActivo.set(typeof perfil === 'string' ? perfilBase(perfil) : perfil);
        sesion = jasmine.createSpyObj<SesionService>('SesionService', ['seleccionarPerfil', 'rutaInicio', 'cerrarSesion'], { usuarioActual, perfilActivo });
        await TestBed.configureTestingModule({ imports: [SeleccionarPerfilComponent], providers: [provideRouter([]), { provide: SesionService, useValue: sesion }] })
            .overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA)
            .compileComponents();
        navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
        fixture = TestBed.createComponent(SeleccionarPerfilComponent);
        await fixture.whenStable();
    };

    const opciones = (): HTMLButtonElement[] => Array.from(fixture.nativeElement.querySelectorAll('ul button'));

    it('muestra una opción por cada perfil del usuario con su nombre', async () => {
        await crear(['LIDER', 'MIEMBRO']);

        expect(opciones().length).toBe(2);
        expect(opciones()[0].textContent).toContain('Líder');
        expect(opciones()[1].textContent).toContain('Miembro');
    });

    it('un perfil base lleva como subtítulo la descripción de lo que permite', async () => {
        await crear(['LIDER', 'MIEMBRO']);

        expect(opciones()[1].textContent).toContain('Consulta tu información personal');
    });

    it('un perfil personalizado lleva su nombre como título y su rol base como subtítulo', async () => {
        await crear([secretaria, 'MIEMBRO']);

        expect(opciones().length).toBe(2);
        expect(opciones()[0].textContent).toContain('Secretaría');
        expect(opciones()[0].textContent).toContain('Administrador de iglesia');
        expect(opciones()[0].querySelector('i.pi-building-columns')).not.toBeNull();
        expect(opciones()[1].textContent).toContain('Miembro');
    });

    it('dos perfiles con el mismo rol base son dos opciones distintas', async () => {
        const tesoreria = perfilPersonalizado(11, 'Tesorería', 'ADMIN_IGLESIA', ['usuarios.ver']);
        await crear([secretaria, tesoreria]);

        expect(opciones().map((opcion) => opcion.querySelector('.text-lg')?.textContent?.trim())).toEqual(['Secretaría', 'Tesorería']);

        opciones()[1].click();

        expect(sesion.seleccionarPerfil).toHaveBeenCalledOnceWith(11);
    });

    it('marca como actual solo el perfil personalizado activo', async () => {
        await crear([secretaria, 'ADMIN_IGLESIA'], secretaria);

        expect(opciones()[0].getAttribute('aria-current')).toBe('true');
        expect(opciones()[1].hasAttribute('aria-current')).toBeFalse();
    });

    it('al elegir un perfil lo guarda y navega a su página de inicio', async () => {
        await crear(['LIDER', 'MIEMBRO']);
        sesion.rutaInicio.and.returnValue('/mi-red');

        opciones()[0].click();

        expect(sesion.seleccionarPerfil).toHaveBeenCalledOnceWith(3);
        expect(navegar).toHaveBeenCalledOnceWith('/mi-red');
    });

    it('marca el perfil activo cuando el usuario viene a cambiarlo', async () => {
        await crear(['LIDER', 'MIEMBRO'], 'MIEMBRO');

        expect(opciones()[1].getAttribute('aria-current')).toBe('true');
        expect(opciones()[1].textContent).toContain('Actual');
        expect(opciones()[0].hasAttribute('aria-current')).toBeFalse();
    });

    it('permite cerrar sesión y vuelve al login', async () => {
        await crear(['LIDER', 'MIEMBRO']);
        sesion.cerrarSesion.and.returnValue(of(undefined));
        const botonSalir: HTMLButtonElement = fixture.nativeElement.querySelector('p-button button');

        botonSalir.click();
        await fixture.whenStable();

        expect(sesion.cerrarSesion).toHaveBeenCalledTimes(1);
        expect(navegar).toHaveBeenCalledOnceWith('/auth/login');
    });
});
