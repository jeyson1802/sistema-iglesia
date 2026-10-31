import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BloqueEstadisticoComponent } from './bloque-estadistico';

@Component({
    imports: [BloqueEstadisticoComponent],
    template: `<app-bloque-estadistico titulo="Miembros" idTitulo="t-miembros" ruta="/miembros" [cargando]="cargando()" [error]="error()" (reintentar)="reintentos = reintentos + 1"><p id="contenido">Cifras</p></app-bloque-estadistico>`
})
class AnfitrionComponent {
    readonly cargando = signal(false);
    readonly error = signal(false);
    reintentos = 0;
}

describe('BloqueEstadisticoComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    it('con datos muestra el título, el enlace a su módulo y el contenido', () => {
        expect(html().querySelector('h2#t-miembros')?.textContent?.replace(/\s+/g, '')).toContain('Miembros');
        expect(html().querySelector('header p-button')?.textContent).toContain('Ver más');
        expect(html().querySelector('#contenido')).not.toBeNull();
    });

    it('mientras carga muestra esqueletos y no el contenido', async () => {
        fixture.componentInstance.cargando.set(true);
        await fixture.whenStable();
        expect(html().querySelector('p-skeleton')).not.toBeNull();
        expect(html().querySelector('#contenido')).toBeNull();
    });

    it('si falla avisa y permite reintentar', async () => {
        fixture.componentInstance.error.set(true);
        await fixture.whenStable();
        expect(html().querySelector('[role="alert"]')?.textContent?.replace(/\s+/g, '')).toContain('Nosepudieroncargar');
        html().querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
        expect(fixture.componentInstance.reintentos).toBe(1);
    });
});
