import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { ROLES_BASE, SECRETARIA } from '@/app/features/roles/testing/roles-prueba';
import { UsuarioAdmin } from '@/app/features/usuarios/models/usuario-admin';
import { UsuariosService } from '@/app/features/usuarios/services/usuarios.service';
import { AdministradoresIglesiaService } from './administradores-iglesia.service';

describe('AdministradoresIglesiaService', () => {
    let roles: jasmine.SpyObj<RolesService>;
    let usuarios: jasmine.SpyObj<UsuariosService>;
    let servicio: AdministradoresIglesiaService;

    const CREADO: UsuarioAdmin = { id: 30, correo: 'pastor@norte.test', miembroId: null, activo: true, debeCambiarContrasena: true, roles: [], creadoEn: new Date(2026, 0, 1) };

    beforeEach(() => {
        roles = jasmine.createSpyObj<RolesService>('RolesService', ['listar']);
        usuarios = jasmine.createSpyObj<UsuariosService>('UsuariosService', ['crearEnIglesia']);
        usuarios.crearEnIglesia.and.returnValue(of(CREADO));
        TestBed.configureTestingModule({
            providers: [
                { provide: RolesService, useValue: roles },
                { provide: UsuariosService, useValue: usuarios }
            ]
        });
        servicio = TestBed.inject(AdministradoresIglesiaService);
    });

    it('crea el usuario en la iglesia con el rol base Administrador de iglesia, no con uno personalizado', async () => {
        roles.listar.and.returnValue(of([SECRETARIA, ...ROLES_BASE]));

        const creado = await firstValueFrom(servicio.crear(3, { correo: 'pastor@norte.test', contrasenaTemporal: 'Temporal123' }));

        expect(creado).toEqual(CREADO);
        expect(usuarios.crearEnIglesia).toHaveBeenCalledOnceWith(3, { correo: 'pastor@norte.test', contrasenaTemporal: 'Temporal123', rolIds: [2] });
    });

    it('falla sin crear el usuario si no encuentra el rol base', async () => {
        roles.listar.and.returnValue(of([SECRETARIA]));

        await expectAsync(firstValueFrom(servicio.crear(3, { correo: 'pastor@norte.test', contrasenaTemporal: 'Temporal123' }))).toBeRejected();
        expect(usuarios.crearEnIglesia).not.toHaveBeenCalled();
    });
});
