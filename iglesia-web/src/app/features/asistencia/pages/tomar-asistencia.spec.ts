import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AsistenciasService } from '../services/asistencias.service';
import { AvisosMarca } from '../services/avisos-marca';
import { ReunionesService } from '../services/reuniones.service';
import { BETO, CARLA, MARCA_ANA, REUNION_CULTO, REUNION_GRUPO } from '../testing/asistencia-prueba';
import { TomarAsistenciaComponent } from './tomar-asistencia';

interface Pagina {
    leer(texto: string): void;
    elegirModo(modo: string): void;
    marcarTodos(): void;
    cambiarPresencia(cambio: { miembro: typeof BETO; presente: boolean }): void;
    deshacer(marca: typeof MARCA_ANA): void;
    total(): number;
    modo(): string;
    resultado(): { tipo: string; titulo: string } | null;
    ultimas(): readonly (typeof MARCA_ANA)[];
    integrantes(): readonly (typeof BETO)[];
}

describe('TomarAsistenciaComponent', () => {
    const CODIGO = 'ASIS:7K2QM9XDR4TB';
    let reuniones: jasmine.SpyObj<ReunionesService>;
    let asistencias: jasmine.SpyObj<AsistenciasService>;
    let avisos: jasmine.SpyObj<AvisosMarca>;
    let fixture: ComponentFixture<TomarAsistenciaComponent>;

    const crear = async (): Promise<void> => {
        localStorage.removeItem('asistencia.modo.CULTO');
        localStorage.removeItem('asistencia.modo.GRUPO');
        await TestBed.configureTestingModule({
            imports: [TomarAsistenciaComponent],
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '50' }) } } },
                { provide: ReunionesService, useValue: reuniones },
                { provide: AsistenciasService, useValue: asistencias },
                { provide: AvisosMarca, useValue: avisos }
            ]
        }).compileComponents();
        spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(TomarAsistenciaComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const pagina = (): Pagina => fixture.componentInstance as unknown as Pagina;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        reuniones = jasmine.createSpyObj<ReunionesService>('ReunionesService', ['obtener']);
        reuniones.obtener.and.returnValue(of(REUNION_CULTO));
        asistencias = jasmine.createSpyObj<AsistenciasService>('AsistenciasService', ['marcarPorCodigo', 'marcarMiembro', 'quitar', 'integrantes', 'buscarMiembros']);
        asistencias.integrantes.and.returnValue(of([BETO, CARLA]));
        avisos = jasmine.createSpyObj<AvisosMarca>('AvisosMarca', ['avisar']);
    });

    afterEach(() => {
        localStorage.removeItem('asistencia.modo.CULTO');
        localStorage.removeItem('asistencia.modo.GRUPO');
    });

    it('muestra la reunión con el contador grande y en una computadora empieza en «Lector»', async () => {
        await crear();

        expect(html().querySelector('h1')?.textContent).toContain('Culto dominical');
        expect(html().textContent).toContain('128');
        expect(pagina().modo()).toBe(matchMedia('(pointer: coarse)').matches ? 'CAMARA' : 'LECTOR');
        expect(html().textContent).toContain('¿Qué equipo uso?');
    });

    it('al leer un código lo marca: franja verde, sonido, contador y últimas marcas', async () => {
        asistencias.marcarPorCodigo.and.returnValue(of(MARCA_ANA));
        await crear();

        pagina().leer(CODIGO);

        expect(asistencias.marcarPorCodigo).toHaveBeenCalledWith(50, '7K2QM9XDR4TB');
        expect(pagina().resultado()?.tipo).toBe('marcado');
        expect(avisos.avisar).toHaveBeenCalledWith('marcado');
        expect(pagina().total()).toBe(129);
        expect(pagina().ultimas()).toEqual([MARCA_ANA]);
    });

    it('la misma lectura repetida de la cámara se envía una sola vez', async () => {
        asistencias.marcarPorCodigo.and.returnValue(of(MARCA_ANA));
        await crear();

        pagina().leer(CODIGO);
        pagina().leer(CODIGO);

        expect(asistencias.marcarPorCodigo).toHaveBeenCalledTimes(1);
    });

    it('un QR que no es de asistencia da franja roja sin llamar al servidor', async () => {
        await crear();

        pagina().leer('https://otra-cosa.com');

        expect(asistencias.marcarPorCodigo).not.toHaveBeenCalled();
        expect(pagina().resultado()).toEqual(jasmine.objectContaining({ tipo: 'error', titulo: 'Este QR no es un código de asistencia' }));
        expect(avisos.avisar).toHaveBeenCalledWith('error');
    });

    it('un código de otra iglesia muestra el motivo del servidor en rojo', async () => {
        asistencias.marcarPorCodigo.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Código no encontrado en esta iglesia' } })));
        await crear();

        pagina().leer(CODIGO);

        expect(pagina().resultado()?.titulo).toBe('Código no encontrado en esta iglesia');
    });

    it('sin conexión avisa y no envía lo leído', async () => {
        await crear();

        window.dispatchEvent(new Event('offline'));
        fixture.detectChanges();
        pagina().leer(CODIGO);

        expect(html().textContent).toContain('Sin conexión: no se están guardando las asistencias');
        expect(asistencias.marcarPorCodigo).not.toHaveBeenCalled();
        window.dispatchEvent(new Event('online'));
    });

    it('«Deshacer» quita la marca, baja el contador y permite volver a leer el mismo código', async () => {
        asistencias.marcarPorCodigo.and.returnValue(of(MARCA_ANA));
        asistencias.quitar.and.returnValue(of(undefined));
        await crear();
        pagina().leer(CODIGO);

        pagina().deshacer(MARCA_ANA);
        pagina().leer(CODIGO);

        expect(asistencias.quitar).toHaveBeenCalledWith(50, 12);
        expect(asistencias.marcarPorCodigo).toHaveBeenCalledTimes(2);
    });

    it('recuerda el modo elegido para ese tipo de reunión', async () => {
        await crear();

        pagina().elegirModo('MANUAL');

        expect(localStorage.getItem('asistencia.modo.CULTO')).toBe('MANUAL');
    });

    describe('en un grupo', () => {
        beforeEach(() => reuniones.obtener.and.returnValue(of(REUNION_GRUPO)));

        it('empieza en «Manual» con la lista de integrantes', async () => {
            await crear();

            expect(pagina().modo()).toBe('MANUAL');
            expect(html().querySelector('app-lista-integrantes')?.textContent).toContain('Beto Ruiz');
        });

        it('cada interruptor guarda al instante y si falla vuelve atrás', async () => {
            asistencias.marcarMiembro.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'El miembro está inactivo' } })));
            await crear();

            pagina().cambiarPresencia({ miembro: BETO, presente: true });

            expect(asistencias.marcarMiembro).toHaveBeenCalledWith(50, 13);
            expect(
                pagina()
                    .integrantes()
                    .find(({ miembroId }) => miembroId === 13)?.presente
            ).toBeFalse();
        });

        it('«Marcar a todos» marca solo a quienes faltan', async () => {
            asistencias.marcarMiembro.and.returnValue(of({ ...MARCA_ANA, miembroId: 13, nombreCompleto: 'Beto Ruiz' }));
            await crear();

            pagina().marcarTodos();

            expect(asistencias.marcarMiembro).toHaveBeenCalledOnceWith(50, 13);
            expect(
                pagina()
                    .integrantes()
                    .every(({ presente }) => presente)
            ).toBeTrue();
        });
    });
});
