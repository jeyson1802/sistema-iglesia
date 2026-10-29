import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { Grupo, GuardarGrupo } from '../models/grupo';
import { RESPUESTA_OLIVOS } from '../testing/grupos-prueba';
import { GruposService } from './grupos.service';

describe('GruposService', () => {
    const url = `${environment.apiUrl}/grupos`;
    const datos: GuardarGrupo = { nombre: 'Los Olivos', liderId: 10, anfitrionId: null, diaReunion: 'JUEVES', horaReunion: '19:30', direccion: null, distritoCodigo: '150135', descripcion: null };
    let servicio: GruposService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(GruposService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista los grupos adaptados', () => {
        let grupos: Grupo[] | undefined;

        servicio.listar().subscribe((g) => (grupos = g));

        http.expectOne(url).flush([RESPUESTA_OLIVOS]);
        expect(grupos?.[0].horaReunion).toBe('19:30');
    });

    it('crea y actualiza con los datos del formulario', () => {
        servicio.crear(datos).subscribe();
        servicio.actualizar(4, datos).subscribe();

        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual(datos);
        expect(http.expectOne({ method: 'PUT', url: `${url}/4` }).request.body).toEqual(datos);
    });

    it('cambia el estado', () => {
        servicio.cambiarEstado(4, false).subscribe();

        expect(http.expectOne({ method: 'PATCH', url: `${url}/4/estado` }).request.body).toEqual({ activo: false });
    });
});
