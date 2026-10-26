import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

/** Encabezado de bienvenida de las páginas de inicio de cada perfil (presentacional). */
@Component({
    selector: 'app-bienvenida-perfil',
    imports: [TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bienvenida-perfil.html'
})
export class BienvenidaPerfilComponent {
    readonly titulo = input.required<string>();
    readonly descripcion = input.required<string>();
    readonly icono = input.required<string>();
    readonly etiquetaPerfil = input.required<string>();
    readonly correo = input.required<string>();
}
