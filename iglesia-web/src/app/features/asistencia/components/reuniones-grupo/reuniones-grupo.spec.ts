import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AsistenciaGrupoService } from '../../services/asistencia-grupo.service';
import { DETALLE_REUNION_DEL_GRUPO, paginaDe, REUNION_DEL_GRUPO } from '../../testing/asistencia-prueba';
import { ReunionesGrupoComponent } from './reuniones-grupo';

describe('ReunionesGrupoComponent', () => {
    let fixture: ComponentFixture<ReunionesGrupoComponent>;
    let servicio: jasmine.SpyObj<AsistenciaGrupoService>;
    let router: Router;
    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as { ver(r: typeof REUNION_DEL_GRUPO): void; cambiarPagina(p: { pagina: number; tamano: number }): void; verPersona(p: { miembroId: number }): void };

    beforeEach(async () => {
        servicio = jasmine.createSpyObj<AsistenciaGrupoService>('AsistenciaGrupoService', ['reuniones', 'detalle']);
        servicio.reuniones.and.returnValue(of(paginaDe([REUNION_DEL_GRUPO])));
        servicio.detalle.and.returnValue(of(DETALLE_REUNION_DEL_GRUPO));
        TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AsistenciaGrupoService, useValue: servicio }] });
        router = TestBed.inject(Router);
        spyOn(router, 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(ReunionesGrupoComponent);
        fixture.componentRef.setInput('grupoId', 8);
        await fixture.whenStable();
    });

    it('carga la primera página de reuniones del grupo', () => {
        expect(servicio.reuniones).toHaveBeenCalledWith(8, 0, 10);
        expect(html().querySelector('app-tabla-reuniones-grupo')?.textContent).toContain('3 de 4');
    });

    it('al cambiar el tamaño de página vuelve a la primera', () => {
        componente().cambiarPagina({ pagina: 3, tamano: 20 });
        expect(servicio.reuniones).toHaveBeenCalledWith(8, 0, 20);
    });

    it('al elegir una reunión pide su detalle y abre el panel', async () => {
        componente().ver(REUNION_DEL_GRUPO);
        await fixture.whenStable();

        expect(servicio.detalle).toHaveBeenCalledWith(8, 70);
        expect(document.querySelector('.p-drawer')?.textContent).toContain('Carla Díaz');
    });

    it('si falla el detalle ofrece reintentar', async () => {
        servicio.detalle.and.returnValue(throwError(() => new Error('falla')));
        componente().ver(REUNION_DEL_GRUPO);
        await fixture.whenStable();

        expect(document.querySelector('.p-drawer')?.textContent).toContain('No se pudo cargar el detalle de la reunión.');
    });

    it('si falla la lista ofrece reintentar', async () => {
        servicio.reuniones.and.returnValue(throwError(() => new Error('falla')));
        (fixture.componentInstance as unknown as { cargar(): void }).cargar();
        await fixture.whenStable();

        expect(html().textContent).toContain('No se pudieron cargar las reuniones del grupo.');
    });

    it('lleva a la asistencia de la persona elegida', () => {
        componente().verPersona({ miembroId: 13 });
        expect(router.navigate).toHaveBeenCalledWith(['/miembros', 13, 'asistencia']);
    });
});
