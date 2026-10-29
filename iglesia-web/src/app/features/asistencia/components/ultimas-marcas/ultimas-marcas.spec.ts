import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Marca } from '../../models/asistencia';
import { MARCA_ANA } from '../../testing/asistencia-prueba';
import { UltimasMarcasComponent } from './ultimas-marcas';

describe('UltimasMarcasComponent', () => {
    let fixture: ComponentFixture<UltimasMarcasComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [UltimasMarcasComponent] }).compileComponents();
        fixture = TestBed.createComponent(UltimasMarcasComponent);
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('lista las marcas y emite la que se quiere deshacer', () => {
        const deshechas: Marca[] = [];
        fixture.componentRef.setInput('marcas', [MARCA_ANA]);
        fixture.componentInstance.deshacer.subscribe((marca) => deshechas.push(marca));
        fixture.detectChanges();

        expect(html().textContent).toContain('Ana Torres');
        (html().querySelector('button[aria-label="Deshacer la marca de Ana Torres"]') as HTMLButtonElement).click();
        expect(deshechas).toEqual([MARCA_ANA]);
    });

    it('sin marcas explica qué aparecerá', () => {
        fixture.componentRef.setInput('marcas', []);
        fixture.detectChanges();

        expect(html().textContent).toContain('Aquí aparecerán las personas que marques.');
    });
});
