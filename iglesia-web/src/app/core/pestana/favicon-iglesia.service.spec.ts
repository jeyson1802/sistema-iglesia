import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { FAVICON_SISTEMA, FaviconIglesiaService } from './favicon-iglesia.service';

describe('FaviconIglesiaService', () => {
    const urlLogo = signal<string | null>(null);

    beforeEach(() => {
        urlLogo.set(null);
        TestBed.configureTestingModule({ providers: [{ provide: IdentidadIglesiaService, useValue: { urlLogo } }] });
        TestBed.inject(FaviconIglesiaService);
    });

    const favicon = (): HTMLLinkElement => document.head.querySelector('link[rel="icon"]')!;

    it('sin logo usa el ícono del sistema y con logo, el de la iglesia', () => {
        TestBed.tick();
        expect(favicon().getAttribute('href')).toBe(FAVICON_SISTEMA);

        urlLogo.set('blob:logo');
        TestBed.tick();
        expect(favicon().getAttribute('href')).toBe('blob:logo');
    });
});
