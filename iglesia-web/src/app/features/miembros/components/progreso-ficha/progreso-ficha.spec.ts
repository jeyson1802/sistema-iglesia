import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MINIMO } from '../../testing/miembros-prueba';
import { ProgresoFichaComponent } from './progreso-ficha';

describe('ProgresoFichaComponent', () => {
    let fixture: ComponentFixture<ProgresoFichaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [ProgresoFichaComponent] }).compileComponents();
        fixture = TestBed.createComponent(ProgresoFichaComponent);
        fixture.componentRef.setInput('miembro', MINIMO);
        fixture.componentRef.setInput('nombreGrupo', 'Célula');
        await fixture.whenStable();
    });

    it('muestra el porcentaje y los datos que faltan', () => {
        const texto = fixture.nativeElement.textContent;

        expect(texto).toContain('11 %');
        expect(texto).toContain('Teléfono');
        expect(texto).toContain('Célula');
    });
});
