import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TreeNode } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { EslabonCadena, Liderado, Miembro } from '../../models/miembro';
import { MiembrosService } from '../../services/miembros.service';
import { JUAN } from '../../testing/miembros-prueba';
import { ArbolLiderazgoComponent, DatosNodo } from './arbol-liderazgo';

describe('ArbolLiderazgoComponent', () => {
    const cadena: EslabonCadena[] = [
        { id: 10, nombreCompleto: 'Ana Ríos', nivel: 'Líder', esPastorPrincipal: false, activo: true },
        { id: 1, nombreCompleto: 'Pedro Gómez', nivel: 'Pastor', esPastorPrincipal: true, activo: true }
    ];
    const rosa: Liderado = { id: 31, nombreCompleto: 'Rosa Quispe', nivel: 'Miembro', cantidadLiderados: 1 };
    const tito: Liderado = { id: 33, nombreCompleto: 'Tito Vera', nivel: null, cantidadLiderados: 0 };
    let servicio: jasmine.SpyObj<MiembrosService>;
    let fixture: ComponentFixture<ArbolLiderazgoComponent>;

    const html = (): HTMLElement => fixture.nativeElement;
    const nodos = (): TreeNode<DatosNodo>[] => (fixture.componentInstance as unknown as Record<string, () => TreeNode<DatosNodo>[]>)['nodos']();
    const componente = () => fixture.componentInstance as unknown as Record<string, (nodo: TreeNode<DatosNodo>) => void>;
    /** Baja por la única rama de superiores hasta el miembro de la ficha. */
    const nodoActual = (): TreeNode<DatosNodo> => nodos()[0].children![0].children![0];
    const crear = async (miembro: Miembro = JUAN, eslabones = cadena): Promise<void> => {
        fixture = TestBed.createComponent(ArbolLiderazgoComponent);
        fixture.componentRef.setInput('miembro', miembro);
        fixture.componentRef.setInput('cadena', eslabones);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        servicio = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['liderados']);
        servicio.liderados.and.callFake((id: number) => of(id === JUAN.id ? [rosa, tito] : [{ id: 40, nombreCompleto: 'Nora Paz', nivel: null, cantidadLiderados: 0 }]));
        await TestBed.configureTestingModule({ imports: [ArbolLiderazgoComponent], providers: [provideRouter([]), { provide: MiembrosService, useValue: servicio }] }).compileComponents();
    });

    it('muestra los superiores del pastor hacia abajo, el miembro y sus liderados con su nivel', async () => {
        await crear();

        expect(nodos()[0].data?.nombre).toBe('Pedro Gómez');
        expect(nodos()[0].children![0].data?.nombre).toBe('Ana Ríos');
        expect(nodoActual().data).toEqual(jasmine.objectContaining({ id: 22, rol: 'actual' }));
        expect(nodoActual().children!.map(({ data }) => data?.nombre)).toEqual(['Rosa Quispe', 'Tito Vera']);
        expect(servicio.liderados).toHaveBeenCalledOnceWith(22);

        const texto = html().textContent ?? '';
        expect(texto).toContain('Superior · Pastor');
        expect(texto).toContain('Líder directo · Líder');
        expect(texto).toContain('Liderado · Miembro');
        expect(texto).toContain('Liderado · Sin nivel');
        expect(texto).toContain('Lidera a 2 personas directamente.');
        expect(html().querySelector('[aria-label="Lidera a 1 persona"]')).not.toBeNull();
        expect(html().querySelector('[aria-label="Pastor principal"]')).not.toBeNull();
    });

    it('resalta al miembro con sus iniciales y al elegir a otra persona abre su ficha', async () => {
        await crear();
        const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

        expect(html().querySelector('[aria-current="page"]')?.textContent).toContain('Juan Carlos Pérez Torres');
        expect(html().textContent).toContain('JC');
        expect(nodoActual().selectable).toBeFalse();

        componente()['abrir'](nodos()[0].children![0]);
        componente()['abrir'](nodoActual());

        expect(navegar).toHaveBeenCalledOnceWith(['/miembros', 10]);
    });

    it('carga los liderados de un liderado al expandirlo, una sola vez', async () => {
        await crear();
        const nodoRosa = nodoActual().children![0];
        expect(nodoRosa.leaf).toBeFalse();
        expect(nodoRosa.expanded).toBeFalse();

        componente()['expandir'](nodoRosa);
        componente()['colapsar'](nodoActual().children![0]);
        componente()['expandir'](nodoActual().children![0]);

        expect(servicio.liderados).toHaveBeenCalledWith(31);
        expect(servicio.liderados).toHaveBeenCalledTimes(2);
        expect(nodoActual().children![0].expanded).toBeTrue();
        expect(nodoActual().children![0].children!.map(({ data }) => data?.nombre)).toEqual(['Nora Paz']);
        expect(nodoActual().children![1].leaf).toBeTrue();
    });

    it('quien no lidera a nadie no pide liderados', async () => {
        await crear({ ...JUAN, cantidadLiderados: 0 });

        expect(servicio.liderados).not.toHaveBeenCalled();
        expect(nodoActual().leaf).toBeTrue();
        expect(html().textContent).toContain('No lidera a nadie directamente.');
    });

    it('al pastor principal lo muestra como raíz', async () => {
        await crear({ ...JUAN, esPastorPrincipal: true, lider: null }, []);

        expect(nodos()[0].data?.rol).toBe('actual');
        expect(html().textContent).toContain('Es el pastor principal');
    });

    it('sin líder lo indica', async () => {
        await crear(JUAN, []);

        expect(html().textContent).toContain('Aún no tiene líder asignado.');
    });

    it('si los liderados no cargan avisa y el árbol sigue visible', async () => {
        servicio.liderados.and.returnValue(throwError(() => new Error('sin conexión')));
        await crear();

        expect(html().textContent).toContain('No se pudieron cargar algunos liderados');
        expect(html().textContent).toContain('Ana Ríos');
    });

    it('muestra skeletons mientras carga la cadena', async () => {
        fixture = TestBed.createComponent(ArbolLiderazgoComponent);
        fixture.componentRef.setInput('miembro', JUAN);
        fixture.componentRef.setInput('cadena', []);
        fixture.componentRef.setInput('cargando', true);
        await fixture.whenStable();

        expect(html().querySelector('[aria-label="Cargando el árbol de liderazgo"]')).not.toBeNull();
        expect(html().querySelector('p-tree')).toBeNull();
    });
});
