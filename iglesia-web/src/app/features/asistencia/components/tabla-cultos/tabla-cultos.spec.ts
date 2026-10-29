import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CultoProgramado } from '../../models/asistencia';
import { TablaCultosComponent } from './tabla-cultos';

describe('TablaCultosComponent', () => {
    const DOMINICAL: CultoProgramado = { id: 1, nombre: 'Dominical', diaSemana: 'DOMINGO', hora: '10:00:00', activo: true };
    const ORACION: CultoProgramado = { id: 2, nombre: 'Oración', diaSemana: 'MIERCOLES', hora: '19:30:00', activo: false };
    let fixture: ComponentFixture<TablaCultosComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaCultosComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaCultosComponent);
        fixture.componentRef.setInput('cultos', [DOMINICAL, ORACION]);
        fixture.detectChanges();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('muestra nombre, día, hora y estado', () => {
        expect(html().textContent).toContain('Dominical');
        expect(html().textContent).toContain('Miércoles');
        expect(html().textContent).toContain('19:30');
        expect(html().textContent).toContain('Inactivo');
    });

    it('el paginador siempre se ve', () => {
        expect(html().querySelector('.p-paginator')?.textContent).toContain('1–2 de 2');
    });

    it('emite editar y cambiar estado', () => {
        const editados: CultoProgramado[] = [];
        const cambiados: CultoProgramado[] = [];
        fixture.componentInstance.editar.subscribe((culto) => editados.push(culto));
        fixture.componentInstance.cambiarEstado.subscribe((culto) => cambiados.push(culto));

        (html().querySelector('button[aria-label="Editar Dominical"]') as HTMLButtonElement).click();
        (html().querySelector('button[aria-label="Activar Oración"]') as HTMLButtonElement).click();

        expect(editados).toEqual([DOMINICAL]);
        expect(cambiados).toEqual([ORACION]);
    });
});
