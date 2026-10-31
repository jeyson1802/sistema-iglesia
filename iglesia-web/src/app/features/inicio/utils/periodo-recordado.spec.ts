import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { periodoRecordado } from './periodo-recordado';

describe('periodoRecordado', () => {
    @Component({ template: '' })
    class AnfitrionComponent {
        readonly periodo = periodoRecordado();
    }

    afterEach(() => localStorage.removeItem('inicio.periodo'));

    const recordado = () => TestBed.createComponent(AnfitrionComponent).componentInstance.periodo();

    it('empieza en «Mes» y recuerda el periodo elegido', async () => {
        localStorage.removeItem('inicio.periodo');
        const fixture = TestBed.createComponent(AnfitrionComponent);
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'MES' });
        fixture.componentInstance.periodo.set({ periodo: 'MES_ANTERIOR' });
        await fixture.whenStable();
        expect(recordado()).toEqual({ periodo: 'MES_ANTERIOR' });
    });

    it('recuerda un rango personalizado con sus fechas', async () => {
        const fixture = TestBed.createComponent(AnfitrionComponent);
        fixture.componentInstance.periodo.set({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' });
        await fixture.whenStable();
        expect(recordado()).toEqual({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' });
    });

    for (const guardado of ['SIGLO', 'ANIO', '{"periodo":"TRIMESTRE"}', '{"periodo":"PERSONALIZADO","desde":"2026-09-15","hasta":"2026-09-01"}', '{"periodo":"PERSONALIZADO"}']) {
        it(`ignora un valor guardado que no es un periodo válido: ${guardado}`, () => {
            localStorage.setItem('inicio.periodo', guardado);
            expect(recordado()).toEqual({ periodo: 'MES' });
        });
    }
});
