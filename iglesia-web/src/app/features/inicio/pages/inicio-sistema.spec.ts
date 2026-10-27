import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { InicioSistemaComponent } from './inicio-sistema';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';

describe('InicioSistemaComponent', () => {
    let fixture: ComponentFixture<InicioSistemaComponent>;

    beforeEach(async () => {
        const sesion = { usuarioActual: signal({ id: 1, correo: 'usuario@iglesia.test', iglesiaId: 1, miembroId: 2, perfiles: [perfilBase('ADMIN_SISTEMA')] }) };
        await TestBed.configureTestingModule({ imports: [InicioSistemaComponent], providers: [{ provide: SesionService, useValue: sesion }] }).compileComponents();
        fixture = TestBed.createComponent(InicioSistemaComponent);
        await fixture.whenStable();
    });

    it('muestra el título, el saludo con el correo y la etiqueta del perfil', () => {
        const html: HTMLElement = fixture.nativeElement;

        expect(html.querySelector('h1')?.textContent).toContain('Panel del sistema');
        expect(html.textContent).toContain('Hola, usuario@iglesia.test.');
        expect(html.querySelector('p-tag')?.textContent).toContain('Administrador del sistema');
    });
});
