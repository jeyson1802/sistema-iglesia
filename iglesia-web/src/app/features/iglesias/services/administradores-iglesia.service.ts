import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap } from 'rxjs';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { UsuarioAdmin } from '@/app/features/usuarios/models/usuario-admin';
import { UsuariosService } from '@/app/features/usuarios/services/usuarios.service';
import { NuevoAdministrador } from '../models/administrador-iglesia';

/**
 * Crea administradores de iglesia desde el catálogo: busca el rol base «Administrador de iglesia» en iglesia-seguridad
 * (su id no se codifica en duro) y crea el usuario en la iglesia indicada con ese rol.
 */
@Injectable({ providedIn: 'root' })
export class AdministradoresIglesiaService {
    private readonly roles = inject(RolesService);
    private readonly usuarios = inject(UsuariosService);

    crear(iglesiaId: number, { correo, contrasenaTemporal }: NuevoAdministrador): Observable<UsuarioAdmin> {
        return this.idRolAdministrador().pipe(switchMap((rolId) => this.usuarios.crearEnIglesia(iglesiaId, { correo, contrasenaTemporal, rolIds: [rolId] })));
    }

    private idRolAdministrador(): Observable<number> {
        return this.roles.listar().pipe(
            map((roles) => {
                const rol = roles.find(({ base, rolBase }) => base && rolBase === 'ADMIN_IGLESIA');
                if (!rol) {
                    throw new Error('No se encontró el rol base ADMIN_IGLESIA');
                }
                return rol.id;
            })
        );
    }
}
