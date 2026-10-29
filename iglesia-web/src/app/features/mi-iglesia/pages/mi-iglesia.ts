import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MiIglesiaService } from '@/app/core/iglesias/services/mi-iglesia.service';
import { EditorIglesiaComponent } from '@/app/shared/components/editor-iglesia/editor-iglesia';

/** Identidad y apariencia de la iglesia en la que se opera; la iglesia la toma el backend del token. */
@Component({
    selector: 'app-mi-iglesia',
    imports: [EditorIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mi-iglesia.html'
})
export class MiIglesiaComponent {
    protected readonly fuente = inject(MiIglesiaService);
}
