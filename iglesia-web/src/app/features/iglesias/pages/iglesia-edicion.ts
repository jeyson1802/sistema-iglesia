import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { EditorIglesiaComponent } from '@/app/shared/components/editor-iglesia/editor-iglesia';

/** Edición completa de una iglesia del catálogo (datos, logo y apariencia) por el administrador del sistema. */
@Component({
    selector: 'app-iglesia-edicion',
    imports: [ButtonModule, RouterLink, EditorIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './iglesia-edicion.html'
})
export class IglesiaEdicionComponent {
    private readonly iglesias = inject(IglesiasService);

    private readonly id = toSignal(inject(ActivatedRoute).paramMap.pipe(map((parametros) => Number(parametros.get('id')))), { requireSync: true });

    protected readonly rutaCatalogo = RUTA_IGLESIAS;
    protected readonly fuente = computed(() => this.iglesias.fuente(this.id()));
}
