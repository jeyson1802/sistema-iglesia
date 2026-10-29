import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EslabonCadena } from '../../models/miembro';
import { CadenaLiderazgoComponent } from './cadena-liderazgo';

describe('CadenaLiderazgoComponent', () => {
    const cadena: EslabonCadena[] = [
        { id: 10, nombreCompleto: 'Ana Ríos', nivel: 'Líder', esPastorPrincipal: false, activo: true },
        { id: 1, nombreCompleto: 'Pedro Gómez', nivel: 'Pastor', esPastorPrincipal: true, activo: true }
    ];
    let fixture: ComponentFixture<CadenaLiderazgoComponent>;

    const html = (): HTMLElement => fixture.nativeElement;
    const crear = async (entradas: Record<string, unknown>): Promise<void> => {
        for (const [nombre, valor] of Object.entries({ cadena, ...entradas })) {
            fixture.componentRef.setInput(nombre, valor);
        }
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [CadenaLiderazgoComponent], providers: [provideRouter([])] }).compileComponents();
        fixture = TestBed.createComponent(CadenaLiderazgoComponent);
    });

    it('muestra la cadena en orden hasta el pastor, con enlaces', async () => {
        await crear({ cantidadLiderados: 2 });

        const items = Array.from(html().querySelectorAll('li')).map((li) => li.textContent ?? '');
        expect(items[0]).toContain('Ana Ríos');
        expect(items[0]).toContain('Líder directo');
        expect(items[1]).toContain('Pastor principal');
        expect(html().querySelector('a')?.getAttribute('href')).toBe('/miembros/10');
        expect(html().textContent).toContain('Lidera a 2 personas directamente.');
    });

    it('el pastor principal no muestra cadena', async () => {
        await crear({ esPastorPrincipal: true, cadena: [] });

        expect(html().textContent).toContain('no tiene líder');
        expect(html().querySelector('ol')).toBeNull();
    });

    it('sin líder lo indica', async () => {
        await crear({ cadena: [], cantidadLiderados: 1 });

        expect(html().textContent).toContain('Aún no tiene líder asignado.');
        expect(html().textContent).toContain('Lidera a 1 persona directamente.');
    });

    it('muestra la carga', async () => {
        await crear({ cargando: true });

        expect(html().querySelector('[aria-label="Cargando cadena de liderazgo"]')).not.toBeNull();
    });
});
