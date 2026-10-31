import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

/**
 * Encabezado compacto de las páginas de inicio (presentacional): perfil, título, saludo y, debajo y alineado a la
 * izquierda como los filtros de aportes, lo que se proyecte (p. ej. el selector de periodo).
 */
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
    /** Cómo se saluda: el nombre del miembro o, si no se conoce, el correo. */
    readonly nombre = input.required<string>();
}
