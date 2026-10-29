import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router, TitleStrategy } from '@angular/router';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { TituloIglesiaStrategy } from './titulo-iglesia.strategy';

@Component({ template: '' })
class VaciaComponent {}

describe('TituloIglesiaStrategy', () => {
    const nombreComercial = signal<string | null>(null);

    beforeEach(() => {
        nombreComercial.set(null);
        TestBed.configureTestingModule({
            providers: [provideRouter([{ path: 'aportes', title: 'Aportes', component: VaciaComponent }]), { provide: TitleStrategy, useClass: TituloIglesiaStrategy }, { provide: IdentidadIglesiaService, useValue: { nombreComercial } }]
        });
    });

    it('sin iglesia usa el nombre del sistema', async () => {
        await TestBed.inject(Router).navigateByUrl('/aportes');
        TestBed.tick();

        expect(TestBed.inject(Title).getTitle()).toBe('Aportes · IGLESIA-WEB');
    });

    it('usa el nombre comercial y se actualiza cuando llega la identidad', async () => {
        await TestBed.inject(Router).navigateByUrl('/aportes');
        nombreComercial.set('MMM Lima Centro');
        TestBed.tick();

        expect(TestBed.inject(Title).getTitle()).toBe('Aportes · MMM Lima Centro');
    });
});
