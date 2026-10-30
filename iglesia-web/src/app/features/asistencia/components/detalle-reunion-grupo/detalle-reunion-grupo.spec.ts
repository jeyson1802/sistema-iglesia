import { ComponentFixture, TestBed } from '@angular/core/testing';
import { hoyIso } from '@/app/shared/utils/fecha';
import { DetalleReunionGrupo, PersonaReunionGrupo } from '../../models/asistencia-grupo';
import { DETALLE_REUNION_DEL_GRUPO } from '../../testing/asistencia-prueba';
import { DetalleReunionGrupoComponent } from './detalle-reunion-grupo';

describe('DetalleReunionGrupoComponent', () => {
    let fixture: ComponentFixture<DetalleReunionGrupoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const nombres = (): string[] => Array.from(html().querySelectorAll('tbody tr td:first-child')).map((celda) => celda.textContent!.trim());
    const opcion = (texto: string): HTMLElement => Array.from(html().querySelectorAll<HTMLElement>('p-selectbutton [role="radio"], p-selectbutton button, p-togglebutton')).find((b) => b.textContent?.includes(texto))!;

    const crear = async (detalle: DetalleReunionGrupo = DETALLE_REUNION_DEL_GRUPO, puedeVerPersonas = false): Promise<void> => {
        fixture = TestBed.createComponent(DetalleReunionGrupoComponent);
        fixture.componentRef.setInput('detalle', detalle);
        fixture.componentRef.setInput('puedeVerPersonas', puedeVerPersonas);
        await fixture.whenStable();
    };

    it('muestra primero a quienes asistieron, con el total de cada lista', async () => {
        await crear();

        expect(html().textContent).toContain('Asistieron (3)');
        expect(html().textContent).toContain('No asistieron (1)');
        expect(html().textContent).toContain('Visitantes (1)');
        expect(nombres()).toEqual(['Ana Torres', 'Carla Díaz', 'Elsa Soto']);
    });

    it('al elegir «No asistieron» muestra a quienes faltaron', async () => {
        await crear();

        opcion('No asistieron').click();
        await fixture.whenStable();

        expect(nombres()).toEqual(['Beto Ruiz']);
    });

    it('en la reunión de hoy, quien falta aparece como «Sin marcar»', async () => {
        await crear({ ...DETALLE_REUNION_DEL_GRUPO, fecha: hoyIso() });
        expect(html().textContent).toContain('Sin marcar (1)');
    });

    it('si se pueden ver las personas, el nombre lleva a su asistencia', async () => {
        await crear(DETALLE_REUNION_DEL_GRUPO, true);
        const vistas: PersonaReunionGrupo[] = [];
        fixture.componentInstance.verPersona.subscribe((persona) => vistas.push(persona));

        (html().querySelector('tbody button') as HTMLButtonElement).click();

        expect(vistas[0].miembroId).toBe(12);
    });

    it('una lista vacía explica qué significa', async () => {
        await crear({ ...DETALLE_REUNION_DEL_GRUPO, faltaron: [] });

        opcion('No asistieron').click();
        await fixture.whenStable();

        expect(html().textContent).toContain('Asistieron todos los integrantes.');
    });

    it('muestra siempre el paginador', async () => {
        await crear();
        expect(html().querySelector('p-paginator, .p-paginator')).not.toBeNull();
    });
});
