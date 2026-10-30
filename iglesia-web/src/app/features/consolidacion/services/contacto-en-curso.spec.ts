import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ContactoEnCursoService } from './contacto-en-curso';

describe('ContactoEnCursoService', () => {
    let servicio: ContactoEnCursoService;
    let documento: Document;
    let visibilidad: DocumentVisibilityState;

    const cambiar = (estado: DocumentVisibilityState): void => {
        visibilidad = estado;
        documento.dispatchEvent(new Event('visibilitychange'));
    };

    beforeEach(() => {
        servicio = TestBed.inject(ContactoEnCursoService);
        documento = TestBed.inject(DOCUMENT);
        visibilidad = 'visible';
        spyOnProperty(documento, 'visibilityState', 'get').and.callFake(() => visibilidad);
    });

    it('al volver de la llamada avisa a quién se llamó y por qué medio', () => {
        servicio.iniciar({ casoId: 3, nombre: 'Ana', medio: 'LLAMADA' });
        cambiar('hidden');
        cambiar('visible');
        expect(servicio.alVolver()).toEqual({ casoId: 3, nombre: 'Ana', medio: 'LLAMADA' });
        servicio.atendido();
        expect(servicio.alVolver()).toBeNull();
    });

    it('si nunca salió del sistema no abre nada', () => {
        servicio.iniciar({ casoId: 3, nombre: 'Ana', medio: 'WHATSAPP' });
        cambiar('visible');
        expect(servicio.alVolver()).toBeNull();
    });

    it('lo no registrado queda pendiente hasta registrarlo o descartarlo', () => {
        servicio.iniciar({ casoId: 3, nombre: 'Ana', medio: 'LLAMADA' });
        expect(servicio.pendientes().has(3)).toBeTrue();
        servicio.resolver(3);
        expect(servicio.pendientes().has(3)).toBeFalse();
    });
});
