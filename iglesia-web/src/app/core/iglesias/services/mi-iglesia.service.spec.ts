import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { Iglesia } from '../models/iglesia';
import { iglesiaRespuesta } from '../testing/iglesia-respuesta';
import { MiIglesiaService } from './mi-iglesia.service';

describe('MiIglesiaService', () => {
    const url = `${environment.apiUrl}/mi-iglesia`;
    let servicio: MiIglesiaService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(MiIglesiaService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lee la iglesia del token sin enviar su id', () => {
        let iglesia: Iglesia | undefined;

        servicio.obtener().subscribe((respuesta) => (iglesia = respuesta));

        http.expectOne(url).flush(iglesiaRespuesta());
        expect(iglesia?.creadaEn).toEqual(new Date(Date.UTC(2026, 9, 28, 15, 30)));
    });

    it('edita datos y apariencia, y sube, lee y quita el logo', () => {
        const archivo = new Blob(['webp'], { type: 'image/webp' });

        servicio.actualizar({ nombreComercial: 'Sion', denominacion: 'Sion', direccion: 'Av. 1', distritoCodigo: '150132', telefono: null, correo: null, sitioWeb: null }).subscribe();
        expect(http.expectOne(url).request.method).toBe('PUT');

        servicio.cambiarApariencia(APARIENCIA_SISTEMA).subscribe();
        http.expectOne(`${url}/apariencia`).flush(APARIENCIA_SISTEMA);

        servicio.subirLogo(archivo).subscribe();
        const subida = http.expectOne(`${url}/logo`);
        expect(subida.request.headers.get('Content-Type')).toBe('image/webp');
        subida.flush(null, { status: 204, statusText: 'No Content' });

        servicio.logo().subscribe();
        expect(http.expectOne(`${url}/logo`).request.responseType).toBe('blob');

        servicio.quitarLogo().subscribe();
        expect(http.expectOne(`${url}/logo`).request.method).toBe('DELETE');
    });
});
