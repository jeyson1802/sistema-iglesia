import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { errorDeLogo, LOGO_MAX_BYTES, TIPOS_LOGO } from '@/app/core/iglesias/models/logo';

/**
 * Elección del logo de una iglesia (presentacional): valida tipo y tamaño antes de subir, muestra la vista previa
 * sobre fondo claro y oscuro y pide confirmación para quitarlo. El contenedor sube o quita el archivo.
 */
@Component({
    selector: 'app-selector-logo',
    imports: [ButtonModule, ConfirmPopupModule, FileUploadModule, MessageModule],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-logo.html'
})
export class SelectorLogoComponent {
    private readonly confirmacion = inject(ConfirmationService);

    /** URL del logo guardado (o `null`: la iglesia no tiene). */
    readonly urlLogo = input<string | null>(null);
    readonly procesando = input(false);
    /** Mensaje del backend (400 o 413). */
    readonly error = input<string | null>(null);

    readonly subir = output<File>();
    readonly quitar = output<void>();

    protected readonly tiposAceptados = TIPOS_LOGO.join(',');
    protected readonly tamanoMaximo = LOGO_MAX_BYTES;
    protected readonly elegido = signal<File | null>(null);
    private readonly urlElegido = signal<string | null>(null);
    protected readonly errorLocal = signal<string | null>(null);

    /** Lo que se ve: el archivo elegido sin subir o, si no hay, el logo guardado. */
    protected readonly vistaPrevia = computed(() => this.urlElegido() ?? this.urlLogo());
    protected readonly mensajeError = computed(() => this.errorLocal() ?? this.error());

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cambiarElegido(null));
    }

    protected elegir(evento: FileSelectEvent): void {
        const archivo = evento.currentFiles[0] ?? evento.files[0];
        if (!archivo) {
            return;
        }
        const error = errorDeLogo(archivo);
        this.errorLocal.set(error);
        this.cambiarElegido(error ? null : archivo);
    }

    protected confirmarSubida(): void {
        const archivo = this.elegido();
        if (archivo) {
            this.subir.emit(archivo);
        }
    }

    /** El contenedor lo llama cuando la subida terminó bien: la vista previa pasa a ser el logo guardado. */
    descartarElegido(): void {
        this.cambiarElegido(null);
    }

    protected cancelar(): void {
        this.errorLocal.set(null);
        this.cambiarElegido(null);
    }

    protected pedirQuitar(evento: Event): void {
        this.confirmacion.confirm({
            target: evento.currentTarget as EventTarget,
            message: '¿Quitar el logo? Se mostrarán las iniciales de la iglesia.',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Quitar logo',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.quitar.emit()
        });
    }

    /** Libera la URL de la vista previa anterior para no acumular imágenes en memoria. */
    private cambiarElegido(archivo: File | null): void {
        const anterior = this.urlElegido();
        if (anterior) {
            URL.revokeObjectURL(anterior);
        }
        this.elegido.set(archivo);
        this.urlElegido.set(archivo ? URL.createObjectURL(archivo) : null);
    }
}
