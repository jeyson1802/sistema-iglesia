import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { Fotocheck } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';
import { JUAN } from '../testing/miembros-prueba';
import { ImprimirFotochecksComponent } from './imprimir-fotochecks';

describe('ImprimirFotochecksComponent', () => {
    const fotocheck = (miembroId: number): Fotocheck => ({ miembroId, nombres: `Miembro ${miembroId}`, apellidos: 'Prueba', nivel: null, codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' });
    let servicio: jasmine.SpyObj<MiembrosService>;
    let parametros: BehaviorSubject<Params>;
    let fixture: ComponentFixture<ImprimirFotochecksComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [ImprimirFotochecksComponent],
            providers: [
                { provide: MiembrosService, useValue: servicio },
                { provide: ActivatedRoute, useValue: { queryParams: parametros } },
                { provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Vida Nueva'), urlLogo: signal(null) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(ImprimirFotochecksComponent);
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        parametros = new BehaviorSubject<Params>({ nivelId: '3', activo: 'true' });
        servicio = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['fotochecks', 'obtener', 'codigoAsistencia']);
        servicio.fotochecks.and.returnValue(of(Array.from({ length: 12 }, (_, i) => fotocheck(i + 1))));
    });

    it('reparte los fotochecks del listado en hojas A4 de 10', async () => {
        await crear();

        expect(servicio.fotochecks).toHaveBeenCalledWith(jasmine.objectContaining({ nivelId: 3, activo: true }));
        expect(html().textContent).toContain('Imprimir fotochecks (12)');
        expect(html().querySelectorAll('section[aria-label^="Hoja"]').length).toBe(2);
        expect(html().querySelectorAll('app-fotocheck').length).toBe(12);
    });

    it('con miembroId imprime solo el fotocheck de ese miembro', async () => {
        parametros.next({ miembroId: String(JUAN.id) });
        servicio.obtener.and.returnValue(of(JUAN));
        servicio.codigoAsistencia.and.returnValue(of({ codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' }));
        await crear();

        expect(servicio.fotochecks).not.toHaveBeenCalled();
        expect(html().querySelectorAll('app-fotocheck').length).toBe(1);
        expect(html().textContent).toContain(JUAN.nombres);
    });

    it('«Imprimir» abre el diálogo de impresión del navegador', async () => {
        await crear();
        const imprimir = spyOn(window, 'print');

        Array.from(html().querySelectorAll('button'))
            .find((b) => b.textContent?.includes('Imprimir'))
            ?.click();

        expect(imprimir).toHaveBeenCalled();
    });

    it('con más de 500 explica que hay que filtrar', async () => {
        servicio.fotochecks.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'Hay más de 500 miembros con estos filtros: filtre más para imprimir sus fotochecks' } })));
        await crear();

        expect(html().textContent).toContain('filtre más para imprimir');
    });
});
