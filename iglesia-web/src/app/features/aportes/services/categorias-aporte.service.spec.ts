import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { CategoriasAporteService } from './categorias-aporte.service';

describe('CategoriasAporteService', () => {
    const url = `${environment.apiUrl}/categorias-aporte`;
    let servicio: CategoriasAporteService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(CategoriasAporteService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista, crea y edita', () => {
        servicio.listar().subscribe();
        servicio.crear({ nombre: 'Misiones', exigeAportante: false }).subscribe();
        servicio.actualizar(3, { nombre: 'Misiones', exigeAportante: true }).subscribe();

        expect(http.expectOne({ method: 'GET', url })).toBeTruthy();
        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual({ nombre: 'Misiones', exigeAportante: false });
        expect(http.expectOne({ method: 'PUT', url: `${url}/3` }).request.body).toEqual({ nombre: 'Misiones', exigeAportante: true });
    });

    it('ordena y cambia el estado', () => {
        servicio.ordenar([2, 1]).subscribe();
        servicio.cambiarEstado(2, false).subscribe();

        expect(http.expectOne({ method: 'PUT', url: `${url}/orden` }).request.body).toEqual({ ids: [2, 1] });
        expect(http.expectOne({ method: 'PATCH', url: `${url}/2/estado` }).request.body).toEqual({ activo: false });
    });
});
