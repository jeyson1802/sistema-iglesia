import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';
import { StyleClassModule } from 'primeng/styleclass';
import { ETIQUETA_ROL } from '@/app/core/auth/models/rol';
import { RUTAS_AUTH } from '@/app/core/auth/models/rutas-auth';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { LayoutService } from '@/app/layout/service/layout.service';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { AppConfigurator } from './app.configurator';

const RUTA_CAMBIAR_CONTRASENA = '/cuenta/contrasena';

@Component({
    selector: 'app-topbar',
    imports: [RouterModule, StyleClassModule, AvatarModule, MenuModule, AppConfigurator, LogoIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.topbar.html'
})
export class AppTopbar {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);

    readonly layoutService = inject(LayoutService);

    protected readonly correo = computed(() => this.sesion.usuarioActual()?.correo ?? '');
    protected readonly etiquetaPerfil = computed(() => {
        const perfil = this.sesion.perfilActivo();
        // Puente temporal (SPEC 02, paso 11): etiqueta del rol base en lugar del nombre del perfil.
        return perfil ? ETIQUETA_ROL[perfil.rolBase] : '';
    });

    /** Opciones del menú de usuario; «Cambiar perfil» solo tiene sentido con más de un rol. */
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

    /** `replaceUrl` saca la página protegida del historial; además los guards impiden volver a ella. */
    private cerrarSesion(): void {
        this.sesion.cerrarSesion().subscribe({ complete: () => void this.router.navigateByUrl(RUTAS_AUTH.login, { replaceUrl: true }) });
    }
}
