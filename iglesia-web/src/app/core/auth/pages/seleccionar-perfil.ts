import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { Perfil } from '../models/perfil';
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
    readonly id: number;
    readonly titulo: string;
    readonly subtitulo: string;
    readonly icono: string;
    readonly activo: boolean;
}

/**
 * Un rol base se llama igual que su etiqueta (así lo siembra Flyway). En un perfil personalizado, el subtítulo es el
 * rol base del que hereda («Secretaría» → «Administrador de iglesia»); en uno base, la descripción de lo que permite.
 */
function aOpcion(perfil: Perfil, activo: Perfil | null): OpcionPerfil {
    const etiquetaRolBase = ETIQUETA_ROL[perfil.rolBase];
    return {
        id: perfil.id,
        titulo: perfil.nombre,
        subtitulo: perfil.nombre === etiquetaRolBase ? DESCRIPCION_POR_ROL[perfil.rolBase] : etiquetaRolBase,
        icono: ICONO_ROL[perfil.rolBase],
        activo: perfil.id === activo?.id
    };
}

/** Permite al usuario con varios perfiles elegir con cuál navegar. */
@Component({
    selector: 'app-seleccionar-perfil',
    imports: [ButtonModule, TagModule, LogoIglesiaComponent, TarjetaAccesoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './seleccionar-perfil.html'
})
export class SeleccionarPerfilComponent {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);

    /** Los perfiles ya llegan ordenados por la jerarquía de su rol base y luego por nombre. */
    protected readonly opciones = computed<readonly OpcionPerfil[]>(() => {
        const activo = this.sesion.perfilActivo();
        return (this.sesion.usuarioActual()?.perfiles ?? []).map((perfil) => aOpcion(perfil, activo));
    });

    protected readonly saliendo = signal(false);

    protected seleccionar(id: number): void {
        this.sesion.seleccionarPerfil(id);
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
