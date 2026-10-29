import { computed, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { catchError, EMPTY, map, of, Subscription } from 'rxjs';
import { APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Iglesia } from '../models/iglesia';
import { MiIglesiaService } from './mi-iglesia.service';

/**
 * Identidad de la iglesia en la que se opera (fachada con signals): la carga en cuanto hay una iglesia en la sesión,
 * aplica su apariencia y la limpia al salir o cambiar de iglesia. La usan la topbar, el footer, el título de la
 * pestaña y el favicon.
 */
@Injectable({ providedIn: 'root' })
export class IdentidadIglesiaService {
    private readonly sesion = inject(SesionService);
    private readonly miIglesia = inject(MiIglesiaService);
    private readonly apariencia = inject(AparienciaService);

    private readonly iglesia = signal<Iglesia | null>(null);
    private readonly logo = signal<string | null>(null);
    private readonly enCarga = signal(false);
    private carga?: Subscription;
    private cargaLogo?: Subscription;

    readonly identidad = this.iglesia.asReadonly();
    /** URL de objeto del logo (o `null`: se muestra el monograma). */
    readonly urlLogo = this.logo.asReadonly();
    readonly cargando = this.enCarga.asReadonly();
    readonly nombreComercial = computed(() => this.iglesia()?.nombreComercial ?? null);

    /** Sin sesión no hay usuario, así que tampoco iglesia. */
    private readonly iglesiaId = computed(() => this.sesion.usuarioActual()?.iglesiaId ?? null);

    constructor() {
        effect(() => {
            const id = this.iglesiaId();
            untracked(() => this.cargar(id));
        });
    }

    /** Tras editar los datos o la apariencia de la iglesia en la que se opera. */
    refrescar(iglesia: Iglesia): void {
        if (iglesia.id !== this.iglesia()?.id) {
            return;
        }
        this.iglesia.set(iglesia);
        this.apariencia.aplicar(iglesia.apariencia);
    }

    /** Tras subir o quitar el logo de la iglesia en la que se opera. */
    recargarLogo(iglesiaId: number, tieneLogo: boolean): void {
        const actual = this.iglesia();
        if (actual?.id !== iglesiaId) {
            return;
        }
        this.iglesia.set({ ...actual, tieneLogo });
        this.pedirLogo(tieneLogo);
    }

    private cargar(id: number | null): void {
        this.carga?.unsubscribe();
        this.cargaLogo?.unsubscribe();
        this.iglesia.set(null);
        this.cambiarLogo(null);
        if (id === null) {
            // Login o administrador del sistema sin iglesia: la apariencia del sistema, sin olvidar la recordada.
            this.enCarga.set(false);
            this.apariencia.aplicar(APARIENCIA_SISTEMA, false);
            return;
        }
        this.enCarga.set(true);
        this.carga = this.miIglesia
            .obtener()
            .pipe(
                // Sin identidad la aplicación sigue funcionando con la identidad neutra (el toast global avisa los 5xx).
                catchError(() => {
                    this.enCarga.set(false);
                    return EMPTY;
                })
            )
            .subscribe((iglesia) => {
                this.iglesia.set(iglesia);
                this.enCarga.set(false);
                this.apariencia.aplicar(iglesia.apariencia);
                this.pedirLogo(iglesia.tieneLogo);
            });
    }

    private pedirLogo(tieneLogo: boolean): void {
        this.cargaLogo?.unsubscribe();
        // Si el logo no llega se muestra el monograma: no es motivo para avisar al usuario.
        const logo$ = tieneLogo ? this.miIglesia.logo().pipe(catchError(() => of(null))) : of(null);
        this.cargaLogo = logo$.pipe(map((blob) => (blob ? URL.createObjectURL(blob) : null))).subscribe((url) => this.cambiarLogo(url));
    }

    /** Libera la URL anterior para no acumular imágenes en memoria. */
    private cambiarLogo(url: string | null): void {
        const anterior = this.logo();
        if (anterior) {
            URL.revokeObjectURL(anterior);
        }
        this.logo.set(url);
    }
}
