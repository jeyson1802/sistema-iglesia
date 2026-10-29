import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { DatosFotocheck, FotocheckComponent } from '@/app/shared/components/fotocheck/fotocheck';
import { crearImagenFotocheck, nombreArchivoFotocheck } from '@/app/shared/utils/imagen-fotocheck';
import { CodigoAsistencia, Miembro } from '../../models/miembro';
import { RUTA_IMPRIMIR_FOTOCHECKS } from '../../models/fotochecks-url';
import { MiembrosService } from '../../services/miembros.service';

const MENSAJE_ERROR_CARGA = 'No se pudo cargar el código de asistencia.';
const MENSAJE_ERROR_IMAGEN = 'No se pudo crear la imagen del fotocheck. Intenta de nuevo.';
const MENSAJE_ERROR_REGENERAR = 'No se pudo regenerar el código. Intenta de nuevo.';

/**
 * Tarjeta «Código de asistencia» de la ficha (contenedor): vista previa del fotocheck con el QR personal y las acciones
 * descargar, compartir, imprimir y regenerar. El QR y la imagen se generan en el navegador.
 */
@Component({
    selector: 'app-codigo-asistencia-miembro',
    imports: [ButtonModule, SkeletonModule, FotocheckComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './codigo-asistencia-miembro.html',
    host: { class: 'card block' }
})
export class CodigoAsistenciaMiembroComponent {
    private readonly servicio = inject(MiembrosService);
    private readonly sesion = inject(SesionService);
    private readonly identidad = inject(IdentidadIglesiaService);
    private readonly router = inject(Router);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly mensajes = inject(MessageService);

    readonly miembro = input.required<Miembro>();

    protected readonly mensajeErrorCarga = MENSAJE_ERROR_CARGA;
    protected readonly codigo = signal<CodigoAsistencia | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly preparando = signal(false);
    protected readonly regenerando = signal(false);
    protected readonly puedeRegenerar = computed(() => this.sesion.tienePermiso('miembros.editar'));
    /** Solo se ofrece «Compartir» si el navegador sabe compartir archivos (celulares); si no, basta «Descargar». */
    protected readonly puedeCompartir = typeof navigator !== 'undefined' && typeof navigator.canShare === 'function';
    protected readonly datos = computed<DatosFotocheck | null>(() => {
        const codigo = this.codigo();
        const miembro = this.miembro();
        return codigo ? { nombres: miembro.nombres, apellidos: miembro.apellidos, nivel: miembro.nivel?.nombre ?? null, ...codigo } : null;
    });

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        effect(() => {
            const id = this.miembro().id;
            untracked(() => this.cargar(id));
        });
    }

    protected cargar(id: number = this.miembro().id): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.servicio
            .codigoAsistencia(id)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({ next: (codigo) => this.codigo.set(codigo), error: () => this.error.set(true) });
    }

    protected async descargar(): Promise<void> {
        const imagen = await this.imagen();
        if (imagen) {
            descargarArchivo(imagen);
        }
    }

    protected async compartir(): Promise<void> {
        const imagen = await this.imagen();
        if (!imagen) {
            return;
        }
        const datos: ShareData = { files: [imagen], text: `Tu código de asistencia de ${this.identidad.nombreComercial() ?? 'tu iglesia'}. Guárdalo y muéstralo al llegar.` };
        if (!navigator.canShare(datos)) {
            descargarArchivo(imagen);
            this.mensajes.add({ severity: 'info', summary: 'Imagen descargada', detail: 'Este navegador no permite compartir archivos: envíala desde tus descargas.' });
            return;
        }
        // Cerrar el menú de compartir sin elegir nada rechaza la promesa: no es un error.
        await navigator.share(datos).catch(() => undefined);
    }

    protected imprimir(): void {
        const url = this.router.serializeUrl(this.router.createUrlTree([RUTA_IMPRIMIR_FOTOCHECKS], { queryParams: { miembroId: this.miembro().id } }));
        window.open(url, '_blank', 'noopener');
    }

    protected regenerar(): void {
        this.confirmacion.confirm({
            header: 'Regenerar código',
            message: 'El fotocheck y la imagen actuales dejarán de funcionar. Habrá que volver a descargarlo o imprimirlo.',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Regenerar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarRegenerar()
        });
    }

    private enviarRegenerar(): void {
        this.regenerando.set(true);
        this.servicio
            .regenerarCodigoAsistencia(this.miembro().id)
            .pipe(finalize(() => this.regenerando.set(false)))
            .subscribe({
                next: (codigo) => {
                    this.codigo.set(codigo);
                    this.mensajes.add({ severity: 'success', summary: 'Código regenerado', detail: 'El fotocheck anterior ya no sirve: descarga o imprime el nuevo.' });
                },
                error: () => this.mensajes.add({ severity: 'error', summary: 'Error', detail: MENSAJE_ERROR_REGENERAR })
            });
    }

    /** PNG del fotocheck como archivo, o `null` (con aviso) si no se pudo crear. */
    private async imagen(): Promise<File | null> {
        const datos = this.datos();
        if (!datos) {
            return null;
        }
        this.preparando.set(true);
        try {
            const blob = await crearImagenFotocheck({ ...datos, nombreIglesia: this.identidad.nombreComercial() ?? '', urlLogo: this.identidad.urlLogo() });
            return new File([blob], nombreArchivoFotocheck(datos.nombres, datos.apellidos), { type: blob.type });
        } catch {
            this.mensajes.add({ severity: 'error', summary: 'Error', detail: MENSAJE_ERROR_IMAGEN });
            return null;
        } finally {
            this.preparando.set(false);
        }
    }
}

function descargarArchivo(archivo: File): void {
    const url = URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = archivo.name;
    enlace.click();
    URL.revokeObjectURL(url);
}
