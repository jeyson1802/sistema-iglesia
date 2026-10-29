import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { ConfiguracionAportesService } from './configuracion-aportes.service';

describe('ConfiguracionAportesService', () => {
    const url = `${environment.apiUrl}/aportes/configuracion`;
    let servicio: ConfiguracionAportesService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(ConfiguracionAportesService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('empieza en soles y carga la configuración una sola vez', () => {
        expect(servicio.configuracion().moneda).toBe('PEN');

        servicio.asegurarCargada();
        http.expectOne(url).flush({ moneda: 'USD', monedaEditable: true });
        servicio.asegurarCargada();

        expect(servicio.configuracion()).toEqual({ moneda: 'USD', monedaEditable: true });
    });

    it('si la carga falla se queda en soles y lo reintenta la próxima vez', () => {
        servicio.asegurarCargada();
        http.expectOne(url).flush(null, { status: 500, statusText: 'Error' });
        servicio.asegurarCargada();
        http.expectOne(url).flush({ moneda: 'PEN', monedaEditable: false });

        expect(servicio.configuracion().monedaEditable).toBeFalse();
    });

    it('guarda la moneda y actualiza la señal', () => {
        servicio.guardar('EUR').subscribe();
        const peticion = http.expectOne({ method: 'PUT', url });
        expect(peticion.request.body).toEqual({ moneda: 'EUR' });
        peticion.flush({ moneda: 'EUR', monedaEditable: true });

        expect(servicio.configuracion().moneda).toBe('EUR');
    });

    it('lista las monedas y marca la moneda como bloqueada al haber aportes', () => {
        servicio.monedas().subscribe();
        http.expectOne(`${environment.apiUrl}/aportes/monedas`);
        servicio.marcarConAportes();

        expect(servicio.configuracion().monedaEditable).toBeFalse();
    });
});
