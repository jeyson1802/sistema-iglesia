import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { MiembrosService } from '../../services/miembros.service';
import { JUAN } from '../../testing/miembros-prueba';
import { CodigoAsistenciaMiembroComponent } from './codigo-asistencia-miembro';

describe('CodigoAsistenciaMiembroComponent', () => {
    const CODIGO = { codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' };
    const NUEVO = { codigo: 'Z9Y8X7W6V5T4', contenidoQr: 'ASIS:Z9Y8X7W6V5T4' };
    let servicio: jasmine.SpyObj<MiembrosService>;
    let confirmacion: jasmine.SpyObj<ConfirmationService>;
    let mensajes: jasmine.SpyObj<MessageService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<CodigoAsistenciaMiembroComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [CodigoAsistenciaMiembroComponent],
            providers: [
                provideRouter([]),
                { provide: MiembrosService, useValue: servicio },
                { provide: ConfirmationService, useValue: confirmacion },
                { provide: MessageService, useValue: mensajes },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } },
                { provide: IdentidadIglesiaService, useValue: { nombreComercial: signal('Iglesia Vida Nueva'), urlLogo: signal(null) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(CodigoAsistenciaMiembroComponent);
        fixture.componentRef.setInput('miembro', JUAN);
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (texto: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.includes(texto));

    beforeEach(() => {
        permisos = new Set<Permiso>(['miembros.ver', 'miembros.editar']);
        servicio = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['codigoAsistencia', 'regenerarCodigoAsistencia']);
        servicio.codigoAsistencia.and.returnValue(of(CODIGO));
        servicio.regenerarCodigoAsistencia.and.returnValue(of(NUEVO));
        confirmacion = jasmine.createSpyObj<ConfirmationService>('ConfirmationService', ['confirm']);
        mensajes = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    });

    it('muestra el fotocheck del miembro con su código', async () => {
        await crear();

        expect(servicio.codigoAsistencia).toHaveBeenCalledWith(JUAN.id);
        expect(html().textContent).toContain('7K2Q M9XD R4TB');
        expect(html().textContent).toContain(JUAN.nombres);
        expect(boton('Descargar')).toBeTruthy();
        expect(boton('Imprimir')).toBeTruthy();
    });

    it('regenera tras confirmar y muestra el código nuevo', async () => {
        await crear();
        confirmacion.confirm.and.callFake((opciones: Confirmation) => {
            expect(opciones.message).toContain('dejarán de funcionar');
            opciones.accept?.();
            return confirmacion;
        });

        boton('Regenerar código')?.click();
        fixture.detectChanges();

        expect(servicio.regenerarCodigoAsistencia).toHaveBeenCalledWith(JUAN.id);
        expect(html().textContent).toContain('Z9Y8 X7W6 V5T4');
        expect(mensajes.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('sin miembros.editar no ofrece regenerar', async () => {
        permisos.delete('miembros.editar');
        await crear();

        expect(boton('Regenerar código')).toBeUndefined();
    });

    it('«Imprimir» abre la vista de impresión de ese miembro', async () => {
        await crear();
        const abrir = spyOn(window, 'open');

        boton('Imprimir')?.click();

        expect(abrir).toHaveBeenCalledWith(`/imprimir/fotochecks?miembroId=${JUAN.id}`, '_blank', 'noopener');
    });

    it('si el código no carga ofrece reintentar', async () => {
        servicio.codigoAsistencia.and.returnValue(throwError(() => new Error('falla')));
        await crear();

        expect(html().querySelector('[role="alert"]')?.textContent).toContain('No se pudo cargar el código de asistencia.');
        expect(boton('Reintentar')).toBeTruthy();
    });
});
