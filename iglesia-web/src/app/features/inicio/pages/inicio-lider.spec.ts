import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { InicioLiderComponent } from './inicio-lider';

describe('InicioLiderComponent', () => {
    let fixture: ComponentFixture<InicioLiderComponent>;

    beforeEach(async () => {
        const sesion = { usuarioActual: signal({ id: 1, correo: 'usuario@iglesia.test', iglesiaId: 1, miembroId: 2, roles: ['LIDER'] }) };
        await TestBed.configureTestingModule({ imports: [InicioLiderComponent], providers: [{ provide: SesionService, useValue: sesion }] }).compileComponents();
        fixture = TestBed.createComponent(InicioLiderComponent);
        await fixture.whenStable();
    });

    it('muestra el título, el saludo con el correo y la etiqueta del perfil', () => {
        const html: HTMLElement = fixture.nativeElement;

        expect(html.querySelector('h1')?.textContent).toContain('Mi red');
        expect(html.textContent).toContain('Hola, usuario@iglesia.test.');
        expect(html.querySelector('p-tag')?.textContent).toContain('Líder');
    });
});
