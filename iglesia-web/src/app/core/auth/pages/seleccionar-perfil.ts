import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { ETIQUETA_ROL, ICONO_ROL, Rol } from '../models/rol';
import { RUTAS_AUTH } from '../models/rutas-auth';
import { SesionService } from '../services/sesion.service';

const DESCRIPCION_POR_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: 'Administra las iglesias registradas en el sistema',
    ADMIN_IGLESIA: 'Administra los miembros y la configuración de tu iglesia',
    LIDER: 'Da seguimiento a las personas que lideras',
    MIEMBRO: 'Consulta tu información personal'
};

interface OpcionPerfil {
    readonly rol: Rol;
    readonly etiqueta: string;
    readonly descripcion: string;
    readonly icono: string;
    readonly activo: boolean;
}

/** Permite al usuario con varios roles elegir con qué perfil navegar. */
@Component({
    selector: 'app-seleccionar-perfil',
    imports: [ButtonModule, TagModule, LogoIglesiaComponent, TarjetaAccesoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './seleccionar-perfil.html'
})
export class SeleccionarPerfilComponent {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);

    /** Los perfiles ya llegan ordenados por jerarquía. Puente temporal (SPEC 02, paso 11): una opción por rol base. */
    protected readonly opciones = computed<readonly OpcionPerfil[]>(() =>
        [...new Set((this.sesion.usuarioActual()?.perfiles ?? []).map((perfil) => perfil.rolBase))].map((rol) => ({
            rol,
            etiqueta: ETIQUETA_ROL[rol],
            descripcion: DESCRIPCION_POR_ROL[rol],
            icono: ICONO_ROL[rol],
            activo: rol === this.sesion.perfilActivo()
        }))
    );

    protected readonly saliendo = signal(false);

    protected seleccionar(rol: Rol): void {
        this.sesion.seleccionarPerfil(rol);
        void this.router.navigateByUrl(this.sesion.rutaInicio());
    }

    protected cerrarSesion(): void {
        this.saliendo.set(true);
        this.sesion
            .cerrarSesion()
            .pipe(finalize(() => this.saliendo.set(false)))
            .subscribe({ complete: () => void this.router.navigateByUrl(RUTAS_AUTH.login) });
    }
}
