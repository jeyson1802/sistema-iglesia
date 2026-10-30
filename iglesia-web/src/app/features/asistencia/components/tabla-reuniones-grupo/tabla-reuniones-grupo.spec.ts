import { ComponentFixture, TestBed } from '@angular/core/testing';
import { hoyIso } from '@/app/shared/utils/fecha';
import { ReunionGrupo } from '../../models/asistencia-grupo';
import { REUNION_DEL_GRUPO } from '../../testing/asistencia-prueba';
import { TablaReunionesGrupoComponent } from './tabla-reuniones-grupo';

describe('TablaReunionesGrupoComponent', () => {
    let fixture: ComponentFixture<TablaReunionesGrupoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    const crear = async (reuniones: readonly ReunionGrupo[], cargando = false): Promise<void> => {
        fixture = TestBed.createComponent(TablaReunionesGrupoComponent);
        fixture.componentRef.setInput('reuniones', reuniones);
        fixture.componentRef.setInput('totalRegistros', reuniones.length);
        fixture.componentRef.setInput('cargando', cargando);
        await fixture.whenStable();
    };

    it('muestra cuántos asistieron de los esperados, los que faltaron y los visitantes', async () => {
        await crear([REUNION_DEL_GRUPO]);

        const fila = html().querySelector('tbody tr')!.textContent!.replace(/\s+/g, ' ');
        expect(fila).toContain('3 de 4');
        expect(fila).toContain('75 %');
        expect(fila).toContain('20:00');
    });

    it('en la reunión de hoy, quien falta aparece «sin marcar»', async () => {
        await crear([{ ...REUNION_DEL_GRUPO, fecha: hoyIso() }]);

        expect(html().querySelector('tbody tr')!.textContent).toContain('sin marcar');
        expect(html().querySelector('tbody tr')!.textContent).toContain('Hoy');
    });

    it('al elegir una reunión la emite', async () => {
        await crear([REUNION_DEL_GRUPO]);
        const vistas: ReunionGrupo[] = [];
        fixture.componentInstance.ver.subscribe((reunion) => vistas.push(reunion));

        (html().querySelector('tbody tr') as HTMLElement).click();

        expect(vistas).toEqual([REUNION_DEL_GRUPO]);
    });

    it('sin reuniones lo dice y mientras carga no muestra la tabla', async () => {
        await crear([]);
        expect(html().textContent).toContain('Aún no hay reuniones registradas para este grupo.');

        await crear([], true);
        expect(html().querySelector('[role="status"]')).not.toBeNull();
    });

    it('muestra siempre el paginador', async () => {
        await crear([REUNION_DEL_GRUPO]);
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
