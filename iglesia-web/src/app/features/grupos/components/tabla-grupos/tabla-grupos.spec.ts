import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
        fixture.componentRef.setInput('nombreSingular', 'Célula');
        fixture.componentRef.setInput('nombrePlural', 'Células');
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaGruposComponent], providers: [provideRouter([])] }).compileComponents();
        fixture = TestBed.createComponent(TablaGruposComponent);
    });

    it('nombra cada grupo por su líder y muestra reunión, distrito, cantidad y estado', async () => {
        await crear([OLIVOS, BETANIA]);

        const [olivos, betania] = Array.from(html().querySelectorAll('tbody tr')).map((fila) => fila.textContent ?? '');
        expect(html().querySelector('thead')?.textContent).not.toContain('Nombre');
        expect(olivos).toContain('Célula de Ana Ríos');
        expect(olivos).toContain('Jueves 19:30');
        expect(html().querySelector('thead')?.textContent).toContain('Dirección');
        expect(html().querySelector('thead')?.textContent).toContain('Región');
        expect(olivos).toContain('Av. Perú 123');
        expect(olivos).toContain('San Martín de Porres, Lima, Lima');
        expect(betania).toContain('Célula de Beto Soto');
        expect(betania).toContain('Líder inactivo');
        expect(betania).toContain('Inactivo');
    });

    it('enlaza cada grupo con la página de sus miembros', async () => {
        await crear([OLIVOS]);

        expect(html().querySelector('tbody a')?.getAttribute('href')).toBe('/grupos/4');
    });

    it('sin permiso de administrar no muestra acciones', async () => {
        await crear([OLIVOS], { puedeAdministrar: false });

        expect(html().querySelector('button[aria-label="Editar Célula de Ana Ríos"]')).toBeNull();
    });

    it('emite editar y cambiar estado', async () => {
        await crear([OLIVOS, BETANIA]);
        const emitidos: string[] = [];
        fixture.componentInstance.editar.subscribe((g) => emitidos.push(`editar ${g.id}`));
        fixture.componentInstance.cambiarEstado.subscribe((g) => emitidos.push(`estado ${g.id}`));

        (html().querySelector('button[aria-label="Editar Célula de Ana Ríos"]') as HTMLButtonElement).click();
        (html().querySelector('button[aria-label="Reactivar Célula de Beto Soto"]') as HTMLButtonElement).click();

        expect(emitidos).toEqual(['editar 4', 'estado 5']);
    });

    it('usa el nombre configurado en la carga y el vacío', async () => {
        await crear([], { cargando: true });
        expect(html().querySelector('[aria-label="Cargando células"]')).not.toBeNull();

        await crear([]);
        expect(html().textContent).toContain('Aún no hay células');
    });
});
