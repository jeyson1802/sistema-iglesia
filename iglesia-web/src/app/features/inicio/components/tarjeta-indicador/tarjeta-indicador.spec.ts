import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TarjetaIndicadorComponent } from './tarjeta-indicador';

@Component({
    imports: [TarjetaIndicadorComponent],
    template: `<dl><div appTarjetaIndicador etiqueta="Bajas" valor="6" [actual]="actual()" [anterior]="anterior()" [subirEsBueno]="subirEsBueno()"></div></dl>`
})
class AnfitrionComponent {
    readonly actual = signal<number | null>(6);
    readonly anterior = signal<number | null | undefined>(4);
    readonly subirEsBueno = signal(true);
}

describe('TarjetaIndicadorComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
    });

    it('muestra la cifra y la variación frente al periodo anterior', () => {
        expect(html().querySelector('dt')?.textContent?.replace(/\s+/g, '')).toContain('Bajas');
        expect(html().querySelector('dd')?.textContent?.replace(/\s+/g, '')).toContain('+50%');
        expect(html().querySelector('dd .text-green-600')).not.toBeNull();
    });

    it('si conviene que baje, una subida se pinta como desfavorable', async () => {
        fixture.componentInstance.subirEsBueno.set(false);
        await fixture.whenStable();
        expect(html().querySelector('dd .text-red-600')).not.toBeNull();
    });

    it('sin periodo anterior con datos dice que no hay con qué comparar', async () => {
        fixture.componentInstance.anterior.set(0);
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Sindatosparacomparar');
    });

    it('sin anterior no compara', async () => {
        fixture.componentInstance.anterior.set(undefined);
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).not.toContain('Sindatosparacomparar');
        expect(html().textContent!.replace(/\s+/g, '')).not.toContain('%');
    });

    it('destacada usa el fondo de tarjeta; dentro de un bloque, solo el borde', () => {
        const tarjeta = html().querySelector('[appTarjetaIndicador]')!;
        expect(tarjeta.classList).toContain('border-surface');
        expect(tarjeta.classList).not.toContain('card');
    });
});
