import { ComponentFixture, TestBed } from '@angular/core/testing';
import { miPerfilPrueba } from '../../testing/estadisticas-prueba';
import { MisDatosComponent } from './mis-datos';

describe('MisDatosComponent', () => {
    let fixture: ComponentFixture<MisDatosComponent>;
    const filas = (): string[] => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('dl > div')).map((fila) => fila.textContent!.replace(/\s+/g, ''));

    beforeEach(() => {
        fixture = TestBed.createComponent(MisDatosComponent);
    });

    it('muestra su líder, su grupo con día y hora y su edad', async () => {
        fixture.componentRef.setInput('perfil', miPerfilPrueba());
        await fixture.whenStable();
        expect(filas()).toContain('MilíderLuisRojas');
        expect(filas()).toContain('MigrupoCéluladeLuis(Viernes·19:30)');
        expect(filas()).toContain('Edad30años');
        expect((fixture.nativeElement as HTMLElement).textContent!.replace(/\s+/g, '')).toContain('Pídeseloatulíder');
    });

    it('explica lo que falta', async () => {
        fixture.componentRef.setInput('perfil', miPerfilPrueba({ lider: null, grupo: null, edad: 40, edadEsAproximada: true }));
        await fixture.whenStable();
        expect(filas()).toContain('MilíderSinasignar');
        expect(filas()).toContain('MigrupoAúnnoestásenungrupo');
        expect(filas()).toContain('EdadUnos40años');
    });
});
