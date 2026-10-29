import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IndicadoresAsistencia, IndicadoresAsistenciaComponent } from './indicadores-asistencia';

describe('IndicadoresAsistenciaComponent', () => {
    let fixture: ComponentFixture<IndicadoresAsistenciaComponent>;

    function con(indicadores: IndicadoresAsistencia): HTMLElement {
        fixture.componentRef.setInput('indicadores', indicadores);
        fixture.detectChanges();
        return fixture.nativeElement;
    }

    beforeEach(() => {
        fixture = TestBed.createComponent(IndicadoresAsistenciaComponent);
    });

    it('muestra el porcentaje con su tendencia, las faltas seguidas y la última vez', () => {
        const texto = con({ porcentaje: 40, porcentajeAnterior: 80, asistidas: 4, reunionesQueCuentan: 10, faltasSeguidas: 3, ultimaAsistencia: null }).textContent;
        expect(texto).toContain('40 %');
        expect(texto).toContain('Bajando');
        expect(texto).toContain('4 de 10 ocasiones');
        expect(texto).toContain('nunca');
    });

    it('sin ocasiones dice «Sin datos»', () => {
        expect(con({ porcentaje: null, porcentajeAnterior: null, asistidas: 0, reunionesQueCuentan: 0, faltasSeguidas: 0, ultimaAsistencia: null }).textContent).toContain('Sin datos');
    });
});
