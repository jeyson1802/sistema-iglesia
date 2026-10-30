import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CandidatoGrupo, IntegranteGrupo } from '../../models/grupo';
import { LUIS_EN_OTRO_GRUPO, ROSA, TITO_SIN_GRUPO } from '../../testing/grupos-prueba';
import { ESPERA_BUSQUEDA_MS, IntegrantesGrupoComponent } from './integrantes-grupo';

describe('IntegrantesGrupoComponent', () => {
    let fixture: ComponentFixture<IntegrantesGrupoComponent>;
    let agregados: CandidatoGrupo[];
    let quitados: IntegranteGrupo[];
    let busquedas: string[];

    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...a: unknown[]) => void>;
    const boton = (etiqueta: string): HTMLButtonElement | null => html().querySelector(`button[aria-label="${etiqueta}"]`);
    const crear = async (opciones: { editable?: boolean; procesando?: number[] } = {}): Promise<void> => {
        fixture.componentRef.setInput('integrantes', [ROSA]);
        fixture.componentRef.setInput('candidatos', [LUIS_EN_OTRO_GRUPO, TITO_SIN_GRUPO]);
        fixture.componentRef.setInput('editable', opciones.editable ?? true);
        fixture.componentRef.setInput('procesando', new Set(opciones.procesando ?? []));
        fixture.componentRef.setInput('nombreSingular', 'Célula');
        await fixture.whenStable();
    };

    beforeEach(async () => {
        jasmine.clock().install().mockDate();
        await TestBed.configureTestingModule({ imports: [IntegrantesGrupoComponent] }).compileComponents();
        fixture = TestBed.createComponent(IntegrantesGrupoComponent);
        agregados = [];
        quitados = [];
        busquedas = [];
        fixture.componentInstance.agregar.subscribe((c) => agregados.push(c));
        fixture.componentInstance.quitar.subscribe((i) => quitados.push(i));
        fixture.componentInstance.buscar.subscribe((q) => busquedas.push(q));
    });

    afterEach(() => jasmine.clock().uninstall());

    it('muestra las dos listas, el nivel y el grupo actual de cada candidato', async () => {
        await crear();

        expect(html().textContent).toContain('Miembros del grupo (1)');
        expect(html().textContent).toContain('Rosa Quispe');
        const [luis, tito] = Array.from(html().querySelectorAll('ul[aria-label="Últimos en llegar a la iglesia"] li')).map((fila) => fila.textContent ?? '');
        expect(luis).toContain('En Célula de Beto Soto');
        expect(tito).toContain('Tito Vera');
        expect(tito).toContain('Sin grupo');
    });

    it('agrega y quita con los botones', async () => {
        await crear();

        boton('Agregar a Tito Vera')?.click();
        boton('Quitar a Rosa Quispe del grupo')?.click();

        expect(agregados).toEqual([TITO_SIN_GRUPO]);
        expect(quitados).toEqual([ROSA]);
    });

    it('soltar un candidato en el grupo lo agrega y soltar un integrante en disponibles lo quita', async () => {
        await crear();

        componente()['empezarArrastre'](LUIS_EN_OTRO_GRUPO);
        componente()['soltarEnGrupo']();
        componente()['empezarArrastre'](ROSA);
        componente()['soltarEnDisponibles']();
        componente()['soltarEnGrupo']();

        expect(agregados).toEqual([LUIS_EN_OTRO_GRUPO]);
        expect(quitados).toEqual([ROSA]);
    });

    it('busca al dejar de escribir, sin espacios sobrantes', async () => {
        await crear();
        const control = (fixture.componentInstance as unknown as Record<string, { setValue: (v: string) => void }>)['busqueda'];

        control.setValue(' lu');
        control.setValue(' luis ');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);

        expect(busquedas).toEqual(['luis']);
    });

    it('en solo lectura no ofrece botones ni búsqueda', async () => {
        await crear({ editable: false });

        expect(boton('Agregar a Tito Vera')).toBeNull();
        expect(boton('Quitar a Rosa Quispe del grupo')).toBeNull();
        expect((html().querySelector('#buscar-candidatos') as HTMLInputElement).disabled).toBeTrue();
    });

    it('deshabilita el botón del miembro cuyo cambio está en curso', async () => {
        await crear({ procesando: [33] });

        expect(boton('Agregar a Tito Vera')?.disabled).toBeTrue();
        expect(boton('Quitar a Rosa Quispe del grupo')?.disabled).toBeFalse();
    });

    it('muestra el vacío de cada lista', async () => {
        fixture.componentRef.setInput('integrantes', []);
        fixture.componentRef.setInput('candidatos', []);
        fixture.componentRef.setInput('editable', true);
        await fixture.whenStable();

        expect(html().textContent).toContain('Aún no tiene miembros. Arrastra aquí a los disponibles.');
        expect(html().textContent).toContain('No hay miembros disponibles.');
    });

    it('sin búsqueda presenta a los últimos en llegar y, al buscar, los resultados', async () => {
        await crear();
        expect(html().textContent).toContain('Últimos en llegar a la iglesia');

        const control = (fixture.componentInstance as unknown as Record<string, { setValue: (v: string) => void }>)['busqueda'];
        control.setValue('luis');
        await fixture.whenStable();

        expect(html().textContent).toContain('Resultados de la búsqueda');
        expect(html().querySelector('ul[aria-label="Miembros disponibles encontrados"]')).not.toBeNull();
    });
});
