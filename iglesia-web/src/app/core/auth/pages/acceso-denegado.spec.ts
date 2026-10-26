import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { SesionService } from '../services/sesion.service';
import { SIN_CONFIGURADOR_DE_TEMA } from '../testing/sin-configurador-de-tema';
import { AccesoDenegadoComponent } from './acceso-denegado';

describe('AccesoDenegadoComponent', () => {
    let fixture: ComponentFixture<AccesoDenegadoComponent>;

    const crear = async (variosPerfiles: boolean): Promise<void> => {
        const sesion = { rutaInicio: () => '/mi-perfil', tieneVariosPerfiles: signal(variosPerfiles) };
        await TestBed.configureTestingModule({ imports: [AccesoDenegadoComponent], providers: [provideRouter([]), { provide: SesionService, useValue: sesion }] })
            .overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA)
            .compileComponents();
        fixture = TestBed.createComponent(AccesoDenegadoComponent);
        await fixture.whenStable();
    };

    const enlaces = (): HTMLAnchorElement[] => Array.from(fixture.nativeElement.querySelectorAll('a'));

    it('explica el motivo y ofrece volver a la página de inicio del perfil', async () => {
        await crear(false);

        expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Acceso denegado');
        expect(enlaces().map((a) => a.getAttribute('href'))).toEqual(['/mi-perfil']);
    });

    it('con varios perfiles ofrece también cambiar de perfil', async () => {
        await crear(true);

        expect(enlaces().map((a) => a.getAttribute('href'))).toEqual(['/mi-perfil', '/auth/seleccionar-perfil']);
    });
});
