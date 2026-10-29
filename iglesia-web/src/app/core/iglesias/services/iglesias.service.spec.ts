import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { Page } from '@/app/core/models/page';
import { DatosIglesia, Iglesia } from '../models/iglesia';
import { iglesiaRespuesta } from '../testing/iglesia-respuesta';
import { IglesiasService } from './iglesias.service';

describe('IglesiasService', () => {
    const iglesiasUrl = `${environment.apiUrl}/iglesias`;
    const datos: DatosIglesia = { nombreComercial: 'Iglesia Central', denominacion: 'Iglesia Evangélica Central', direccion: 'Av. Lima 1', distritoCodigo: '150132', telefono: null, correo: null, sitioWeb: null };

    let servicio: IglesiasService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(IglesiasService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide una página del catálogo con los filtros que haya, ordenada por nombre comercial', () => {
        let pagina: Page<Iglesia> | undefined;

        servicio.listar({ q: 'sion', ubigeo: '15', activa: false }, 2, 20).subscribe((respuesta) => (pagina = respuesta));

        const peticion = http.expectOne((req) => req.url === iglesiasUrl);
        expect(peticion.request.params.get('q')).toBe('sion');
        expect(peticion.request.params.get('ubigeo')).toBe('15');
        expect(peticion.request.params.get('activa')).toBe('false');
        expect(peticion.request.params.get('page')).toBe('2');
        expect(peticion.request.params.get('size')).toBe('20');
        expect(peticion.request.params.get('sort')).toBe('nombreComercial,asc');
        peticion.flush({ content: [iglesiaRespuesta()], page: { size: 20, number: 2, totalElements: 41, totalPages: 3 } });
        expect(pagina?.content[0].creadaEn).toEqual(new Date(Date.UTC(2026, 9, 28, 15, 30)));
        expect(pagina?.page.totalElements).toBe(41);
    });

    it('sin filtros no envía parámetros vacíos', () => {
        servicio.listar({ q: null, ubigeo: null, activa: null }, 0, 10).subscribe();

        const peticion = http.expectOne((req) => req.url === iglesiasUrl);
        expect(peticion.request.params.keys()).toEqual(['page', 'size', 'sort']);
        peticion.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
    });

    it('obtiene, crea y edita una iglesia', () => {
        servicio.obtener(3).subscribe();
        http.expectOne(`${iglesiasUrl}/3`).flush(iglesiaRespuesta());

        servicio.crear(datos).subscribe();
        const alta = http.expectOne(iglesiasUrl);
        expect(alta.request.method).toBe('POST');
        expect(alta.request.body).toEqual(datos);
        alta.flush(iglesiaRespuesta(), { status: 201, statusText: 'Created' });

        servicio.actualizar(3, datos).subscribe();
        const edicion = http.expectOne(`${iglesiasUrl}/3`);
        expect(edicion.request.method).toBe('PUT');
        edicion.flush(iglesiaRespuesta());
    });

    it('cambia la apariencia y maneja el logo de una iglesia', () => {
        const archivo = new Blob(['png'], { type: 'image/png' });

        servicio.cambiarApariencia(3, APARIENCIA_SISTEMA).subscribe();
        const apariencia = http.expectOne(`${iglesiasUrl}/3/apariencia`);
        expect(apariencia.request.method).toBe('PUT');
        apariencia.flush(APARIENCIA_SISTEMA);

        servicio.logo(3).subscribe();
        const logo = http.expectOne(`${iglesiasUrl}/3/logo`);
        expect(logo.request.responseType).toBe('blob');
        logo.flush(archivo);

        servicio.subirLogo(3, archivo).subscribe();
        const subida = http.expectOne(`${iglesiasUrl}/3/logo`);
        expect(subida.request.method).toBe('PUT');
        expect(subida.request.headers.get('Content-Type')).toBe('image/png');
        expect(subida.request.body).toBe(archivo);
        subida.flush(null, { status: 204, statusText: 'No Content' });

        servicio.quitarLogo(3).subscribe();
        const quitar = http.expectOne(`${iglesiasUrl}/3/logo`);
        expect(quitar.request.method).toBe('DELETE');
        quitar.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('la fuente de una iglesia del catálogo opera siempre sobre su id', () => {
        const fuente = servicio.fuente(7);

        fuente.obtener().subscribe();
        http.expectOne(`${iglesiasUrl}/7`).flush(iglesiaRespuesta({ id: 7 }));
        fuente.quitarLogo().subscribe();
        http.expectOne(`${iglesiasUrl}/7/logo`).flush(null, { status: 204, statusText: 'No Content' });
    });
});
