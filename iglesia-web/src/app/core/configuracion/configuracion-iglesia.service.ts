import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '@/environments/environment';
import { SesionService } from '@/app/core/auth/services/sesion.service';

/** Contrato de `ConfiguracionIglesiaResponse` en iglesia-api: cómo llama la iglesia a sus grupos pequeños. */
export interface NombresGrupos {
    readonly nombreGrupoSingular: string;
    readonly nombreGrupoPlural: string;
}

/** Valores de iglesia-api (`LimitesGrupo`) cuando la iglesia aún no los configuró. */
export const NOMBRES_GRUPOS_POR_DEFECTO: NombresGrupos = { nombreGrupoSingular: 'Grupo', nombreGrupoPlural: 'Grupos' };

/**
 * Configuración de la iglesia en la que se opera. Carga el nombre de los grupos en cuanto hay una iglesia en la sesión
 * (al entrar al layout, que muestra el menú) y lo comparte con el menú, los títulos y las etiquetas.
 */
@Injectable({ providedIn: 'root' })
export class ConfiguracionIglesiaService {
    private readonly http = inject(HttpClient);
    private readonly sesion = inject(SesionService);
    private readonly url = `${environment.apiUrl}/configuracion-iglesia`;

    private readonly nombres = signal<NombresGrupos>(NOMBRES_GRUPOS_POR_DEFECTO);
    readonly nombresGrupos = this.nombres.asReadonly();

    constructor() {
        // Al cambiar de iglesia (o salir de ella) se vuelve al valor por defecto hasta que llega el de la nueva.
        effect(() => {
            const iglesiaId = this.sesion.usuarioActual()?.iglesiaId ?? null;
            this.nombres.set(NOMBRES_GRUPOS_POR_DEFECTO);
            if (iglesiaId !== null) {
                // Si falla se quedan los nombres por defecto; el toast global avisa los 5xx.
                this.http.get<NombresGrupos>(this.url).subscribe({ next: (nombres) => this.nombres.set(nombres), error: () => undefined });
            }
        });
    }

    guardar(nombres: NombresGrupos): Observable<NombresGrupos> {
        return this.http.put<NombresGrupos>(this.url, nombres).pipe(tap((guardados) => this.nombres.set(guardados)));
    }
}
