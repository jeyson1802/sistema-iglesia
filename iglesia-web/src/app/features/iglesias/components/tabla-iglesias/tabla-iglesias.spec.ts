import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { TablaIglesiasComponent } from './tabla-iglesias';

describe('TablaIglesiasComponent', () => {
    let fixture: ComponentFixture<TablaIglesiasComponent>;

    const CENTRAL: Iglesia = { id: 1, nombre: 'Iglesia Central', creadaEn: new Date(2026, 2, 15, 10, 30) };
    const NORTE: Iglesia = { id: 2, nombre: 'Iglesia del Norte', creadaEn: new Date(2026, 9, 1) };

    interface Opciones {
        readonly cargando?: boolean;
        readonly puedeAdministrar?: boolean;
        readonly iglesiaActualId?: number | null;
        readonly entrando?: boolean;
    }

    const crear = async (iglesias: Iglesia[], { cargando = false, puedeAdministrar = true, iglesiaActualId = null, entrando = false }: Opciones = {}): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [TablaIglesiasComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaIglesiasComponent);
        fixture.componentRef.setInput('iglesias', iglesias);
        fixture.componentRef.setInput('cargando', cargando);
        fixture.componentRef.setInput('puedeAdministrar', puedeAdministrar);
        fixture.componentRef.setInput('iglesiaActualId', iglesiaActualId);
        fixture.componentRef.setInput('entrando', entrando);
        await fixture.whenStable();
    };

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const botones = (fila: HTMLElement): HTMLButtonElement[] => Array.from(fila.querySelectorAll('button'));
    const etiquetas = (fila: HTMLElement): string[] => botones(fila).map((boton) => boton.getAttribute('aria-label') ?? '');

    it('muestra nombre y fecha de alta de cada iglesia', async () => {
        await crear([CENTRAL, NORTE]);

        expect(filas().length).toBe(2);
        expect(filas()[0].textContent).toContain('Iglesia Central');
        expect(filas()[0].textContent).toContain('15/03/2026');
        expect(filas()[1].textContent).toContain('Iglesia del Norte');
    });

    it('con iglesias.administrar ofrece entrar, editar el nombre y crear administrador, y emite la iglesia', async () => {
        await crear([CENTRAL]);
        const emitidas: string[] = [];
        fixture.componentInstance.entrar.subscribe(() => emitidas.push('entrar'));
        fixture.componentInstance.editar.subscribe(() => emitidas.push('editar'));
        fixture.componentInstance.crearAdministrador.subscribe((iglesia) => emitidas.push(`administrador ${iglesia.id}`));

        expect(etiquetas(filas()[0])).toEqual(['Entrar a Iglesia Central', 'Editar nombre de Iglesia Central', 'Crear administrador de Iglesia Central']);
        botones(filas()[0]).forEach((boton) => boton.click());

        expect(emitidas).toEqual(['entrar', 'editar', 'administrador 1']);
    });

    it('sin iglesias.administrar no ofrece acciones', async () => {
        await crear([CENTRAL], { puedeAdministrar: false });

        expect(botones(filas()[0])).toEqual([]);
    });

    it('marca como «Actual» la iglesia en la que se opera y no ofrece volver a entrar en ella', async () => {
        await crear([CENTRAL, NORTE], { iglesiaActualId: 2 });

        expect(filas()[0].textContent).not.toContain('Actual');
        expect(filas()[1].textContent).toContain('Actual');
        expect(etiquetas(filas()[1])).not.toContain('Entrar a Iglesia del Norte');
        expect(etiquetas(filas()[0])).toContain('Entrar a Iglesia Central');
    });

    it('mientras se entra a una iglesia deshabilita «Entrar»', async () => {
        await crear([CENTRAL], { entrando: true });

        expect(botones(filas()[0])[0].disabled).toBeTrue();
    });

    it('mientras carga muestra filas de esqueleto y marca la tabla como ocupada', async () => {
        await crear([CENTRAL], { cargando: true });

        expect(html().querySelectorAll('p-skeleton').length).toBeGreaterThan(0);
        expect(html().textContent).not.toContain('Iglesia Central');
        expect(html().querySelector('p-table')!.getAttribute('aria-busy')).toBe('true');
    });

    it('sin iglesias muestra el estado vacío', async () => {
        await crear([]);

        expect(html().textContent).toContain('Aún no hay iglesias');
    });
});
