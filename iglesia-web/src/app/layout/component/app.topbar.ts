import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';
import { RUTAS_AUTH } from '@/app/core/auth/models/rutas-auth';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
import { PreferenciaModoOscuroService } from '@/app/core/apariencia/services/preferencia-modo-oscuro.service';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { LayoutService } from '@/app/layout/service/layout.service';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';

const RUTA_CAMBIAR_CONTRASENA = '/cuenta/contrasena';

@Component({
    selector: 'app-topbar',
    imports: [RouterModule, AvatarModule, ButtonModule, MenuModule, TagModule, LogoIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.topbar.html'
})
export class AppTopbar {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly identidad = inject(IdentidadIglesiaService);
    private readonly modoOscuro = inject(PreferenciaModoOscuroService);

    readonly layoutService = inject(LayoutService);

    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    /** Nombre del perfil activo: el del rol personalizado («Secretaría») o el del rol base. */
    protected readonly nombrePerfil = computed(() => this.sesion.perfilActivo()?.nombre ?? '');

    /** Iglesia en la que opera el administrador del sistema; `null` fuera de ella (y para cualquier otro usuario). */
    private readonly iglesiaActuandoId = computed(() => (this.sesion.actuandoEnIglesia() ? (this.sesion.usuarioActual()?.iglesiaId ?? null) : null));

    /** Nombre comercial de esa iglesia; mientras carga (o si no se pudo obtener), su número. */
    protected readonly nombreIglesia = computed(() => (this.iglesiaActuandoId() === null ? null : (this.identidad.nombreComercial() ?? `Iglesia ${this.iglesiaActuandoId()}`)));
    /** Nombre del enlace al inicio: en el móvil la topbar solo muestra el logo. */
    protected readonly etiquetaInicio = computed(() => `${this.identidad.nombreComercial() ?? 'IGLESIA-WEB'}: ir a la página de inicio`);

    protected readonly actuandoEnIglesia = computed(() => this.iglesiaActuandoId() !== null);
    protected readonly saliendo = signal(false);

    /** Opciones del menú de usuario; «Cambiar perfil» solo tiene sentido con más de un perfil. */
    protected readonly opcionesUsuario = computed<MenuItem[]>(() => [
        ...(this.sesion.tieneVariosPerfiles() ? [{ label: 'Cambiar perfil', icon: 'pi pi-users', routerLink: RUTAS_AUTH.seleccionarPerfil }] : []),
        { label: 'Cambiar contraseña', icon: 'pi pi-key', routerLink: RUTA_CAMBIAR_CONTRASENA },
        { separator: true },
        { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.cerrarSesion() }
    ]);

    /** El modo oscuro es de cada persona: se recuerda en su navegador. */
    protected alternarModoOscuro(): void {
        this.modoOscuro.alternar();
    }

    /** Vuelve a la vista global del administrador del sistema, en el catálogo de iglesias. */
    protected salirDeIglesia(): void {
        this.saliendo.set(true);
        this.sesion
            .salirDeIglesia()
            .pipe(finalize(() => this.saliendo.set(false)))
            .subscribe({
                next: () => void this.router.navigateByUrl(RUTA_IGLESIAS),
                // 5xx y sin conexión los avisa el toast global; un refresco inválido lo resuelve el interceptor.
                error: () => undefined
            });
    }

    /** `replaceUrl` saca la página protegida del historial; además los guards impiden volver a ella. */
    private cerrarSesion(): void {
        this.sesion.cerrarSesion().subscribe({ complete: () => void this.router.navigateByUrl(RUTAS_AUTH.login, { replaceUrl: true }) });
    }
}
