import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BienvenidaPerfilComponent } from './bienvenida-perfil';

@Component({
    imports: [BienvenidaPerfilComponent],
    template: `<app-bienvenida-perfil titulo="Mi red" descripcion="Así va tu red." icono="pi pi-users" etiquetaPerfil="Líder" nombre="Ana"><span id="proyectado">Periodo</span></app-bienvenida-perfil>`
})
class AnfitrionComponent {}

describe('BienvenidaPerfilComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    it('muestra el perfil, el título, el saludo y lo proyectado, sin el aviso de «en preparación»', () => {
        const html: HTMLElement = fixture.nativeElement;
        expect(html.querySelector('h1')?.textContent?.replace(/\s+/g, '')).toContain('Mired');
        expect(html.querySelector('p-tag')?.textContent?.replace(/\s+/g, '')).toContain('Líder');
        expect(html.textContent!.replace(/\s+/g, '')).toContain('Hola,Ana.Asívatured.');
        expect(html.querySelector('#proyectado')).not.toBeNull();
        expect(html.textContent!.replace(/\s+/g, '')).not.toContain('Estamospreparandoestasección');
    });
});
