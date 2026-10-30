import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Page } from '@/app/core/models/page';
import { CasoResumen, SeccionMisCasos } from '../models/consolidacion';
import { CasosService } from '../services/casos.service';
import { ContactoEnCursoService } from '../services/contacto-en-curso';
import { ResultadosConsolidacionService } from '../services/resultados-consolidacion.service';
import { casoPrueba, contactoPrueba } from '../testing/datos-prueba';
import { MisAsignadosComponent } from './mis-asignados';

const pagina = (casos: CasoResumen[]): Page<CasoResumen> => ({ content: casos, page: { size: 10, number: 0, totalElements: casos.length, totalPages: 1 } });

describe('MisAsignadosComponent', () => {
    let fixture: ComponentFixture<MisAsignadosComponent>;
    let casos: jasmine.SpyObj<CasosService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const porSeccion: Record<SeccionMisCasos, CasoResumen[]> = {
        PARA_HOY: [casoPrueba(1, { proximoContacto: '2026-11-01', estado: 'EN_CONTACTO' })],
        SIN_CONTACTAR: [casoPrueba(2), casoPrueba(3)],
        EN_CONTACTO: []
    };

    beforeEach(async () => {
        casos = jasmine.createSpyObj<CasosService>('CasosService', ['misCasos', 'resumenMisCasos', 'registrarContacto', 'borrarContacto', 'cerrar']);
        casos.misCasos.and.callFake((seccion) => of(pagina(porSeccion[seccion])));
        casos.resumenMisCasos.and.returnValue(of({ paraHoy: 1, sinContactar: 2, enContacto: 0, asignados: 3, contactados: 1 }));
        TestBed.configureTestingModule({
            providers: [provideRouter([]), { provide: CasosService, useValue: casos }, { provide: ResultadosConsolidacionService, useValue: { listar: () => of([]) } }]
        });
        fixture = TestBed.createComponent(MisAsignadosComponent);
        await refrescar();
    });

    afterEach(() => fixture.destroy());

    it('saluda con las personas para hoy y muestra las secciones por urgencia', () => {
        expect(html().textContent).toContain('Tienes 1 persona para hoy');
        expect(html().textContent).toContain('1 de 3 contactadas');
        const titulos = Array.from(html().querySelectorAll('app-seccion-casos h2')).map((titulo) => titulo.textContent!.trim());
        expect(titulos[0]).toContain('Para hoy');
        expect(titulos[1]).toContain('Sin contactar');
        expect(titulos[2]).toContain('En contacto');
        expect(html().querySelectorAll('app-tarjeta-caso').length).toBe(3);
    });

    it('sin asignados explica cuándo aparecerán', async () => {
        casos.resumenMisCasos.and.returnValue(of({ paraHoy: 0, sinContactar: 0, enContacto: 0, asignados: 0, contactados: 0 }));
        fixture = TestBed.createComponent(MisAsignadosComponent);
        await refrescar();
        expect(html().textContent).toContain('No tienes personas asignadas por ahora.');
    });

    it('al volver de la llamada abre la hoja con el medio ya elegido', async () => {
        const enCurso = TestBed.inject(ContactoEnCursoService);
        spyOnProperty(document, 'visibilityState', 'get').and.returnValues('hidden', 'visible');
        enCurso.iniciar({ casoId: 2, nombre: 'Persona 2', medio: 'LLAMADA' });
        document.dispatchEvent(new Event('visibilitychange'));
        document.dispatchEvent(new Event('visibilitychange'));
        await refrescar();
        expect(document.body.textContent).toContain('¿Cómo te fue con la llamada a Persona?');
    });

    it('al registrar saca la tarjeta, ofrece deshacer y recarga', async () => {
        casos.registrarContacto.and.returnValue(of(contactoPrueba(900, { casoId: 2 })));
        const mensajes = fixture.debugElement.injector.get(MessageService);
        const avisos = spyOn(mensajes, 'add').and.callThrough();
        const componente = fixture.componentInstance as unknown as { registrar(caso: CasoResumen): void; guardarContacto(datos: unknown): void };
        componente.registrar(porSeccion.SIN_CONTACTAR[0]);
        componente.guardarContacto({ fechaHora: '2026-11-03T19:30:00Z', medio: 'LLAMADA', resultado: 'NO_RESPONDIO', observaciones: null, proximoContacto: null });
        await refrescar();
        expect(casos.registrarContacto).toHaveBeenCalledWith(2, jasmine.objectContaining({ resultado: 'NO_RESPONDIO' }));
        expect(avisos).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Contacto registrado', life: 8000, data: { contactoId: 900 } }));
        expect(casos.misCasos.calls.count()).toBe(6);
    });

    it('si falla el guardado la hoja sigue abierta con el error', async () => {
        casos.registrarContacto.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'La fecha del contacto no puede ser futura' } })));
        const componente = fixture.componentInstance as unknown as { registrar(caso: CasoResumen): void; guardarContacto(datos: unknown): void };
        componente.registrar(porSeccion.SIN_CONTACTAR[0]);
        await refrescar();
        componente.guardarContacto({ fechaHora: '2026-11-03T19:30:00Z', medio: 'LLAMADA', resultado: 'SE_HABLO', observaciones: 'Hola', proximoContacto: null });
        await refrescar();
        expect(document.body.textContent).toContain('La fecha del contacto no puede ser futura');
        expect(document.body.querySelector('p-dialog form')).not.toBeNull();
    });
});
