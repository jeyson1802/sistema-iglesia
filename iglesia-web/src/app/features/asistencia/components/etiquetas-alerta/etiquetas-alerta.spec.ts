import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EtiquetasAlertaComponent } from './etiquetas-alerta';

describe('EtiquetasAlertaComponent', () => {
    let fixture: ComponentFixture<EtiquetasAlertaComponent>;

    beforeEach(() => {
        fixture = TestBed.createComponent(EtiquetasAlertaComponent);
    });

    it('escribe cuántas faltas seguidas lleva y las demás alertas por su nombre', () => {
        fixture.componentRef.setInput('alertas', ['FALTAS_SEGUIDAS', 'ASISTENCIA_EN_CAIDA']);
        fixture.componentRef.setInput('faltasSeguidas', 4);
        fixture.detectChanges();
        const textos = Array.from(fixture.nativeElement.querySelectorAll('p-tag')).map((tag) => (tag as HTMLElement).textContent?.trim());
        expect(textos).toEqual(['4 faltas seguidas', 'Asistencia en caída']);
    });

    it('sin alertas no muestra nada, salvo que pida «Al día»', () => {
        fixture.componentRef.setInput('alertas', []);
        fixture.detectChanges();
        expect(fixture.nativeElement.textContent.trim()).toBe('');
        fixture.componentRef.setInput('mostrarAlDia', true);
        fixture.detectChanges();
        expect(fixture.nativeElement.textContent).toContain('Al día');
    });
});
