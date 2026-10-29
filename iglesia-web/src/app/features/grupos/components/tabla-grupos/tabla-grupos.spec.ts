import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Grupo } from '../../models/grupo';
import { BETANIA, OLIVOS } from '../../testing/grupos-prueba';
import { TablaGruposComponent } from './tabla-grupos';

describe('TablaGruposComponent', () => {
    let fixture: ComponentFixture<TablaGruposComponent>;

    const html = (): HTMLElement => fixture.nativeElement;
    const crear = async (grupos: Grupo[], opciones: { cargando?: boolean; puedeAdministrar?: boolean } = {}): Promise<void> => {
        fixture.componentRef.setInput('grupos', grupos);
        fixture.componentRef.setInput('cargando', opciones.cargando ?? false);
        fixture.componentRef.setInput('puedeAdministrar', opciones.puedeAdministrar ?? true);
        fixture.componentRef.setInput('nombrePlural', 'Células');
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaGruposComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaGruposComponent);
    });

    it('muestra líder, reunión, distrito, cantidad y estado', async () => {
        await crear([OLIVOS, BETANIA]);

        const [olivos, betania] = Array.from(html().querySelectorAll('tbody tr')).map((fila) => fila.textContent ?? '');
        expect(olivos).toContain('Ana Ríos');
        expect(olivos).toContain('Jueves 19:30');
        expect(olivos).toContain('San Martín de Porres');
        expect(betania).toContain('Sin líder');
        expect(betania).toContain('Inactivo');
    });

    it('sin permiso de administrar no muestra acciones', async () => {
        await crear([OLIVOS], { puedeAdministrar: false });

        expect(html().querySelector('button[aria-label="Editar Los Olivos"]')).toBeNull();
    });

    it('emite editar y cambiar estado', async () => {
        await crear([OLIVOS, BETANIA]);
        const emitidos: string[] = [];
        fixture.componentInstance.editar.subscribe((g) => emitidos.push(`editar ${g.id}`));
        fixture.componentInstance.cambiarEstado.subscribe((g) => emitidos.push(`estado ${g.id}`));

        (html().querySelector('button[aria-label="Editar Los Olivos"]') as HTMLButtonElement).click();
        (html().querySelector('button[aria-label="Reactivar Betania"]') as HTMLButtonElement).click();

        expect(emitidos).toEqual(['editar 4', 'estado 5']);
    });

    it('usa el nombre configurado en la carga y el vacío', async () => {
        await crear([], { cargando: true });
        expect(html().querySelector('[aria-label="Cargando células"]')).not.toBeNull();

        await crear([]);
        expect(html().textContent).toContain('Aún no hay células');
    });
});
