import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { ToastModule } from 'primeng/toast';
import { catchError, finalize, map, of, startWith, Subscription } from 'rxjs';
import { Apariencia } from '@/app/core/apariencia/models/apariencia';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';
import { detalleDeError, erroresPorCampo } from '@/app/core/errores/detalle-de-error';
import { FuenteIglesia } from '@/app/core/iglesias/models/fuente-iglesia';
import { faltaDireccion, Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { CamposIglesiaComponent, crearGrupoIglesia, datosDelGrupo, valoresDeIglesia } from '@/app/shared/components/campos-iglesia/campos-iglesia';
import { LogoIglesiaComponent, VistaPreviaLogo } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { SelectorAparienciaComponent } from '@/app/shared/components/selector-apariencia/selector-apariencia';
import { SelectorLogoComponent } from '@/app/shared/components/selector-logo/selector-logo';

const MENSAJE_ERROR_CARGA = 'No se pudo cargar la iglesia.';
const MENSAJE_ERROR_DATOS = 'No se pudieron guardar los datos. Revisa los campos e intenta de nuevo.';
const MENSAJE_ERROR_LOGO = 'No se pudo guardar el logo. Intenta de nuevo.';
const MENSAJE_LOGO_GRANDE = 'La imagen pesa más de 1 MB; elige una de hasta 1 MB.';
const MENSAJE_ERROR_APARIENCIA = 'No se pudo guardar la apariencia. Intenta de nuevo.';
const DURACION_TOAST_MS = 2500;

/**
 * Edición de la identidad de una iglesia (contenedor): datos, logo y apariencia, cada uno con su propio guardado.
 * Trabaja con cualquier `FuenteIglesia` («Mi iglesia» o una iglesia del catálogo). La apariencia se ve en vivo y, si
 * se sale sin guardar, vuelve la que había. Si la iglesia es la de la sesión, la topbar y el footer se actualizan.
 */
@Component({
    selector: 'app-editor-iglesia',
    imports: [ReactiveFormsModule, ButtonModule, MessageModule, SkeletonModule, ToastModule, CamposIglesiaComponent, LogoIglesiaComponent, SelectorAparienciaComponent, SelectorLogoComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './editor-iglesia.html',
    host: { class: 'flex flex-col gap-6' }
})
export class EditorIglesiaComponent {
    private readonly temas = inject(AparienciaService);
    private readonly identidad = inject(IdentidadIglesiaService);
    private readonly mensajes = inject(MessageService);

    readonly fuente = input.required<FuenteIglesia>();

    protected readonly iglesia = signal<Iglesia | null>(null);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);

    protected readonly formulario = crearGrupoIglesia(inject(NonNullableFormBuilder));
    protected readonly guardandoDatos = signal(false);
    protected readonly errorDatos = signal<string | null>(null);
    protected readonly erroresCampo = signal<Readonly<Record<string, string>>>({});
    private readonly nombreEscrito = toSignal(
        this.formulario.controls.nombreComercial.valueChanges.pipe(
            startWith(''),
            map((nombre) => nombre.trim())
        ),
        { initialValue: '' }
    );

    protected readonly urlLogo = signal<string | null>(null);
    protected readonly procesandoLogo = signal(false);
    protected readonly errorLogo = signal<string | null>(null);
    private readonly selectorLogo = viewChild(SelectorLogoComponent);

    protected readonly apariencia = signal<Apariencia | null>(null);
    protected readonly guardandoApariencia = signal(false);
    protected readonly errorApariencia = signal<string | null>(null);
    /** La apariencia que había al entrar: vuelve al salir sin guardar. */
    private aparienciaAlEntrar = this.temas.actual();

    protected readonly faltaDireccion = computed(() => {
        const iglesia = this.iglesia();
        return iglesia !== null && faltaDireccion(iglesia);
    });
    protected readonly nombreVistaPrevia = computed(() => this.nombreEscrito() || this.iglesia()?.nombreComercial || 'Mi iglesia');
    protected readonly vistaPrevia = computed<VistaPreviaLogo>(() => ({ nombre: this.nombreVistaPrevia(), urlLogo: this.urlLogo() }));
    protected readonly aparienciaCambiada = computed(() => {
        const [elegida, guardada] = [this.apariencia(), this.iglesia()?.apariencia];
        return elegida !== null && guardada !== undefined && JSON.stringify(elegida) !== JSON.stringify(guardada);
    });

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.carga?.unsubscribe();
            this.cambiarLogo(null);
            this.temas.aplicar(this.aparienciaAlEntrar, false);
        });
        effect(() => {
            const fuente = this.fuente();
            untracked(() => this.cargar(fuente));
        });
        // Vista previa en vivo: toda la aplicación se pinta con la apariencia que se está eligiendo.
        effect(() => {
            const elegida = this.apariencia();
            if (elegida) {
                untracked(() => this.temas.aplicar(elegida, false));
            }
        });
    }

    protected cargar(fuente: FuenteIglesia = this.fuente()): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.carga = fuente
            .obtener()
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (iglesia) => {
                    this.mostrar(iglesia);
                    this.apariencia.set(iglesia.apariencia);
                    this.pedirLogo(iglesia.tieneLogo);
                },
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }

    protected guardarDatos(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        this.guardandoDatos.set(true);
        this.errorDatos.set(null);
        this.erroresCampo.set({});
        this.fuente()
            .actualizar(datosDelGrupo(this.formulario))
            .pipe(finalize(() => this.guardandoDatos.set(false)))
            .subscribe({
                next: (iglesia) => {
                    this.mostrar(iglesia);
                    this.identidad.refrescar(iglesia);
                    this.avisar('Datos guardados', `${iglesia.nombreComercial} quedó actualizada.`);
                },
                error: (error: unknown) => {
                    this.errorDatos.set(detalleDeError(error, MENSAJE_ERROR_DATOS));
                    this.erroresCampo.set(erroresPorCampo(error));
                }
            });
    }

    protected subirLogo(archivo: File): void {
        this.ejecutarLogo(this.fuente().subirLogo(archivo), true, 'Logo guardado');
    }

    protected quitarLogo(): void {
        this.ejecutarLogo(this.fuente().quitarLogo(), false, 'Logo quitado');
    }

    protected guardarApariencia(): void {
        const elegida = this.apariencia();
        const iglesia = this.iglesia();
        if (!elegida || !iglesia) {
            return;
        }
        this.guardandoApariencia.set(true);
        this.errorApariencia.set(null);
        this.fuente()
            .cambiarApariencia(elegida)
            .pipe(finalize(() => this.guardandoApariencia.set(false)))
            .subscribe({
                next: (guardada) => {
                    const actualizada = { ...iglesia, apariencia: guardada };
                    this.iglesia.set(actualizada);
                    if (iglesia.id === this.identidad.identidad()?.id) {
                        // Es la iglesia de la sesión: lo guardado pasa a ser su apariencia también al salir.
                        this.aparienciaAlEntrar = guardada;
                        this.identidad.refrescar(actualizada);
                    }
                    this.avisar('Apariencia guardada', 'Todos los usuarios de la iglesia la verán al iniciar sesión.');
                },
                error: (error: unknown) => this.errorApariencia.set(detalleDeError(error, MENSAJE_ERROR_APARIENCIA))
            });
    }

    protected descartarApariencia(): void {
        this.apariencia.set(this.iglesia()?.apariencia ?? null);
    }

    private mostrar(iglesia: Iglesia): void {
        this.iglesia.set(iglesia);
        this.formulario.reset(valoresDeIglesia(iglesia));
    }

    private ejecutarLogo(peticion: ReturnType<FuenteIglesia['quitarLogo']>, tieneLogo: boolean, resumen: string): void {
        const iglesia = this.iglesia();
        if (!iglesia) {
            return;
        }
        this.procesandoLogo.set(true);
        this.errorLogo.set(null);
        peticion.pipe(finalize(() => this.procesandoLogo.set(false))).subscribe({
            next: () => {
                this.selectorLogo()?.descartarElegido();
                this.iglesia.set({ ...iglesia, tieneLogo });
                this.pedirLogo(tieneLogo);
                this.identidad.recargarLogo(iglesia.id, tieneLogo);
                this.avisar(resumen, tieneLogo ? 'Se verá en la barra superior y en la pestaña del navegador.' : 'Se mostrarán las iniciales de la iglesia.');
            },
            error: (error: unknown) => this.errorLogo.set(mensajeErrorLogo(error))
        });
    }

    private pedirLogo(tieneLogo: boolean): void {
        if (!tieneLogo) {
            this.cambiarLogo(null);
            return;
        }
        this.fuente()
            .logo()
            .pipe(catchError(() => of(null)))
            .subscribe((blob) => this.cambiarLogo(blob ? URL.createObjectURL(blob) : null));
    }

    /** Libera la URL anterior para no acumular imágenes en memoria. */
    private cambiarLogo(url: string | null): void {
        const anterior = this.urlLogo();
        if (anterior) {
            URL.revokeObjectURL(anterior);
        }
        this.urlLogo.set(url);
    }

    private avisar(summary: string, detail: string): void {
        this.mensajes.add({ severity: 'success', summary, detail, life: DURACION_TOAST_MS });
    }
}

/** El 400 trae el motivo junto al campo `logo`; el 413, que el archivo es demasiado grande. */
function mensajeErrorLogo(error: unknown): string | null {
    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.PayloadTooLarge) {
        return MENSAJE_LOGO_GRANDE;
    }
    return erroresPorCampo(error)['logo'] ?? detalleDeError(error, MENSAJE_ERROR_LOGO);
}
