import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvanceRonda } from '../../models/consolidacion';
import { AvanceRondaComponent } from './avance-ronda';

describe('AvanceRondaComponent', () => {
    let fixture: ComponentFixture<AvanceRondaComponent>;
    const AVANCE: AvanceRonda = {
        personas: 40,
        contactadas: 30,
        pendientes: 10,
        enContacto: 18,
        cerrados: 12,
        noConcluidos: 0,
        volvieronAAsistir: 6,
        horasHastaPrimerContacto: 52,
        porResultado: [
            { resultado: { id: 1, nombre: 'Volverá a asistir', activo: true }, cantidad: 9 },
            { resultado: null, cantidad: 3 }
        ],
        porConsolidador: [{ consolidador: { id: 25, nombre: 'Luis Rojas', activo: true }, asignados: 20, contactados: 15, cerrados: 6 }]
    };
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(AvanceRondaComponent);
        fixture.componentRef.setInput('avance', AVANCE);
        await refrescar();
    });

    it('muestra el embudo con el porcentaje entre etapas', () => {
        const texto = fixture.nativeElement.textContent;
        expect(texto).toContain('Contactadas');
        expect(texto).toContain('75 % de la etapa anterior');
        expect(texto).toContain('Volvieron a asistir');
        expect(texto).toContain('50 % de la etapa anterior');
    });

    it('resume resultados, consolidadores y el tiempo hasta el primer contacto', () => {
        const texto = fixture.nativeElement.textContent;
        expect(texto).toContain('Volverá a asistir');
        expect(texto).toContain('9 (75 %)');
        expect(texto).toContain('No concluido');
        expect(texto).toContain('Luis Rojas');
        expect(texto).toContain('2 días');
    });

    it('sin contactos aún no inventa un tiempo', async () => {
        fixture.componentRef.setInput('avance', { ...AVANCE, horasHastaPrimerContacto: null, porResultado: [], porConsolidador: [] });
        await refrescar();
        expect(fixture.nativeElement.textContent).toContain('Aún no hay casos cerrados.');
        expect(fixture.nativeElement.textContent).toContain('Aún no se repartió a nadie.');
    });
});
