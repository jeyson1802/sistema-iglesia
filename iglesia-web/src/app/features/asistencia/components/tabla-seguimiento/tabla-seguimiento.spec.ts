import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PersonaSeguimiento } from '../../models/seguimiento';
import { TablaSeguimientoComponent } from './tabla-seguimiento';

describe('TablaSeguimientoComponent', () => {
    const persona = (miembroId: number, nombreCompleto: string): PersonaSeguimiento => ({
        miembroId,
        nombreCompleto,
        nivel: 'Líder',
        grupoId: 3,
        grupo: 'Célula de Juan',
        lider: 'Juan Pérez',
        porcentaje: 40,
        porcentajeAnterior: 85,
        faltasSeguidas: 4,
        ultimaAsistencia: null,
        alertas: ['FALTAS_SEGUIDAS']
    });
    let fixture: ComponentFixture<TablaSeguimientoComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(TablaSeguimientoComponent);
        fixture.componentRef.setInput('personas', [persona(1, 'Ana Quispe'), persona(2, 'Luis Rojas')]);
        fixture.componentRef.setInput('totalRegistros', 2);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra a cada persona con su grupo, líder, porcentaje, tendencia y alertas', () => {
        const texto = fixture.nativeElement.querySelector('p-table').textContent;
        expect(texto).toContain('Ana Quispe');
        expect(texto).toContain('Líder: Juan Pérez');
        expect(texto).toContain('40 %');
        expect(texto).toContain('Bajando');
        expect(texto).toContain('4 faltas seguidas');
    });

    it('avisa a quién ver al hacer clic en la fila', () => {
        const visto = jasmine.createSpy('ver');
        fixture.componentInstance.ver.subscribe(visto);
        (fixture.nativeElement.querySelector('p-table tbody tr') as HTMLElement).click();
        expect(visto).toHaveBeenCalledWith(jasmine.objectContaining({ miembroId: 1 }));
    });

    it('muestra siempre el paginador', () => {
        expect(fixture.nativeElement.querySelector('p-paginator')).not.toBeNull();
    });

    it('paginada en el navegador muestra solo la página pedida', () => {
        fixture.componentRef.setInput('paginaEnServidor', false);
        fixture.componentRef.setInput('tamanoPagina', 1);
        fixture.componentRef.setInput('pagina', 1);
        fixture.detectChanges();
        const filas = fixture.nativeElement.querySelectorAll('p-table tbody tr');
        expect(filas.length).toBe(1);
        expect(filas[0].textContent).toContain('Luis Rojas');
    });

    it('sin personas muestra el mensaje vacío', () => {
        fixture.componentRef.setInput('personas', []);
        fixture.detectChanges();
        expect(fixture.nativeElement.textContent).toContain('Nadie necesita seguimiento');
    });
});
