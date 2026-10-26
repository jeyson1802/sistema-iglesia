import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { InicioMiembroComponent } from './inicio-miembro';

describe('InicioMiembroComponent', () => {
    let fixture: ComponentFixture<InicioMiembroComponent>;

    beforeEach(async () => {
        const sesion = { usuarioActual: signal({ id: 1, correo: 'usuario@iglesia.test', iglesiaId: 1, miembroId: 2, roles: ['MIEMBRO'] }) };
        await TestBed.configureTestingModule({ imports: [InicioMiembroComponent], providers: [{ provide: SesionService, useValue: sesion }] }).compileComponents();
        fixture = TestBed.createComponent(InicioMiembroComponent);
        await fixture.whenStable();
    });

    it('muestra el título, el saludo con el correo y la etiqueta del perfil', () => {
        const html: HTMLElement = fixture.nativeElement;

        expect(html.querySelector('h1')?.textContent).toContain('Mi perfil');
        expect(html.textContent).toContain('Hola, usuario@iglesia.test.');
        expect(html.querySelector('p-tag')?.textContent).toContain('Miembro');
    });
});
