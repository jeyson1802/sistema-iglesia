import { effect, inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';

/** Nombre del sistema cuando no hay iglesia (login o administrador del sistema sin iglesia). */
export const NOMBRE_SISTEMA = 'IGLESIA-WEB';
const SEPARADOR = ' · ';

/**
 * Título de la pestaña «Página · Nombre comercial». Se rehace al navegar y también cuando llega o cambia la
 * identidad de la iglesia, que puede cargar después de la navegación.
 */
@Injectable()
export class TituloIglesiaStrategy extends TitleStrategy {
    private readonly titulo = inject(Title);
    private readonly identidad = inject(IdentidadIglesiaService);
    private readonly pagina = signal<string | undefined>(undefined);

    constructor() {
        super();
        effect(() => {
            const marca = this.identidad.nombreComercial() ?? NOMBRE_SISTEMA;
            const pagina = this.pagina();
            this.titulo.setTitle(pagina ? `${pagina}${SEPARADOR}${marca}` : marca);
        });
    }

    override updateTitle(estado: RouterStateSnapshot): void {
        this.pagina.set(this.buildTitle(estado));
    }
}
