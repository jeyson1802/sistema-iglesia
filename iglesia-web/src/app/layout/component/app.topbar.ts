import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterModule } from '@angular/router';
import { catchError, finalize, map, of, switchMap } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { StyleClassModule } from 'primeng/styleclass';
import { TagModule } from 'primeng/tag';
import { RUTAS_AUTH } from '@/app/core/auth/models/rutas-auth';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { LayoutService } from '@/app/layout/service/layout.service';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { AppConfigurator } from './app.configurator';

const RUTA_CAMBIAR_CONTRASENA = '/cuenta/contrasena';

@Component({
    selector: 'app-topbar',
    imports: [RouterModule, StyleClassModule, AvatarModule, ButtonModule, MenuModule, TagModule, AppConfigurator, LogoIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.topbar.html'
})
export class AppTopbar {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly iglesias = inject(IglesiasService);

    readonly layoutService = inject(LayoutService);

    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    /** Nombre del perfil activo: el del rol personalizado («Secretaría») o el del rol base. */
    protected readonly nombrePerfil = computed(() => this.sesion.perfilActivo()?.nombre ?? '');

    /** Iglesia en la que opera el administrador del sistema; `null` fuera de ella (y para cualquier otro usuario). */
    private readonly iglesiaActuandoId = computed(() => (this.sesion.actuandoEnIglesia() ? (this.sesion.usuarioActual()?.iglesiaId ?? null) : null));

    /**
     * Nombre de esa iglesia, pedido a iglesia-api cada vez que cambia. Si no se puede obtener se muestra su número: el
     * indicador importa más que el nombre exacto.
     */
    protected readonly nombreIglesia = toSignal(
        toObservable(this.iglesiaActuandoId).pipe(
            switchMap((id) =>
                id === null
                    ? of(null)
                    : this.iglesias.obtener(id).pipe(
                          map(({ nombre }) => nombre),
                          catchError(() => of(`Iglesia ${id}`))
                      )
            )
        ),
        { initialValue: null }
    );

    protected readonly actuandoEnIglesia = computed(() => this.iglesiaActuandoId() !== null);
    protected readonly saliendo = signal(false);

    /** Opciones del menú de usuario; «Cambiar perfil» solo tiene sentido con más de un perfil. */
    protected readonly opcionesUsuario = computed<MenuItem[]>(() => [
        ...(this.sesion.tieneVariosPerfiles() ? [{ label: 'Cambiar perfil', icon: 'pi pi-users', routerLink: RUTAS_AUTH.seleccionarPerfil }] : []),
        { label: 'Cambiar contraseña', icon: 'pi pi-key', routerLink: RUTA_CAMBIAR_CONTRASENA },
        { separator: true },
        { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.cerrarSesion() }
    ]);

    toggleDarkMode() {
        this.layoutService.layoutConfig.update((state) => ({
            ...state,
            darkTheme: !state.darkTheme
        }));
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
