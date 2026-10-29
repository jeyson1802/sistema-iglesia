import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable } from '@angular/core';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';

/** Ícono neutro del sistema (`public/favicon.svg`). */
export const FAVICON_SISTEMA = 'favicon.svg';
const SELECTOR_FAVICON = 'link[rel="icon"]';

/** El ícono de la pestaña es el logo de la iglesia en la que se opera; sin logo, el del sistema. */
@Injectable({ providedIn: 'root' })
export class FaviconIglesiaService {
    private readonly documento = inject(DOCUMENT);
    private readonly identidad = inject(IdentidadIglesiaService);

    /** Sigue al logo desde que se crea: basta con inyectarlo una vez al arrancar la aplicación. */
    constructor() {
        effect(() => {
            const enlace = this.enlace();
            enlace.href = this.identidad.urlLogo() ?? FAVICON_SISTEMA;
            enlace.removeAttribute('type');
        });
    }

    private enlace(): HTMLLinkElement {
        const existente = this.documento.head.querySelector<HTMLLinkElement>(SELECTOR_FAVICON);
        if (existente) {
            return existente;
        }
        const nuevo = this.documento.createElement('link');
        nuevo.rel = 'icon';
        this.documento.head.appendChild(nuevo);
        return nuevo;
    }
}
