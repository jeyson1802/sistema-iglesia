import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { cargaReactiva } from './carga-reactiva';

describe('cargaReactiva', () => {
    const periodo = signal('MES');
    const activa = signal(true);
    let pedir: jasmine.Spy;

    @Component({ template: '' })
    class AnfitrionComponent {
        readonly carga = cargaReactiva(periodo, (valor: string) => pedir(valor), activa);
    }

    beforeEach(() => {
        periodo.set('MES');
        activa.set(true);
        pedir = jasmine.createSpy('pedir').and.callFake((valor: string) => of(`datos de ${valor}`));
    });

    it('pide los datos al iniciar y cada vez que cambia el parámetro', async () => {
        const fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
        expect(fixture.componentInstance.carga.datos()).toBe('datos de MES');
        periodo.set('ANIO');
        await fixture.whenStable();
        expect(pedir).toHaveBeenCalledWith('ANIO');
        expect(fixture.componentInstance.carga.datos()).toBe('datos de ANIO');
    });

    it('si no está activa no hace la petición', async () => {
        activa.set(false);
        const fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
        expect(pedir).not.toHaveBeenCalled();
    });

    it('si falla guarda el error y permite reintentar', async () => {
        pedir.and.returnValue(throwError(() => new Error('falló')));
        const fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
        const { carga } = fixture.componentInstance;
        expect(carga.error()).toBeTrue();
        expect(carga.cargando()).toBeFalse();
        pedir.and.returnValue(of('otra vez'));
        carga.recargar();
        expect(carga.error()).toBeFalse();
        expect(carga.datos()).toBe('otra vez');
    });

    it('cancela la petición anterior al cambiar el parámetro', async () => {
        const lenta = new Subject<string>();
        pedir.and.returnValue(lenta);
        const fixture = TestBed.createComponent(AnfitrionComponent);
        await fixture.whenStable();
        pedir.and.returnValue(of('nueva'));
        periodo.set('ANIO');
        await fixture.whenStable();
        expect(lenta.observed).toBeFalse();
        expect(fixture.componentInstance.carga.datos()).toBe('nueva');
    });
});
