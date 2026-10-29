import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { finalize, forkJoin, Observable, of, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { esErrorGlobal } from '@/app/core/errores/clasificar-error';
import { Page } from '@/app/core/models/page';
import { RolAcceso } from '@/app/features/roles/models/rol-acceso';
import { RolesService } from '@/app/features/roles/services/roles.service';
import { FiltrosUsuariosComponent } from '../components/filtros-usuarios/filtros-usuarios';
import { FormularioUsuarioComponent, ModoFormularioUsuario } from '../components/formulario-usuario/formulario-usuario';
import { RestablecerContrasenaComponent } from '../components/restablecer-contrasena/restablecer-contrasena';
import { TablaUsuariosComponent } from '../components/tabla-usuarios/tabla-usuarios';
import { PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { CrearUsuario, EditarUsuario, FiltroUsuarios, UsuarioAdmin, PAGINA_USUARIOS } from '../models/usuario-admin';
import { UsuariosService } from '../services/usuarios.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los usuarios.';
const MENSAJE_ERROR_GUARDAR = 'No se pudo guardar el usuario. Intenta de nuevo.';
const MENSAJE_ERROR_RESTABLECER = 'No se pudo restablecer la contraseña. Intenta de nuevo.';

/** Errores de negocio cuyo `detail` (ProblemDetail, en español) se muestra tal cual en el diálogo. */
const ESTADOS_CON_DETALLE: readonly number[] = [HttpStatusCode.BadRequest, HttpStatusCode.NotFound, HttpStatusCode.Conflict];

const FILTRO_INICIAL: FiltroUsuarios = { correo: '', activo: null, rolId: null };

interface DialogoUsuario {
    readonly modo: ModoFormularioUsuario;
    readonly usuario: UsuarioAdmin | null;
}

/**
 * Administración de los usuarios de la iglesia (contenedor): carga paginada y filtrada en el servidor, diálogos de
 * crear, editar y restablecer contraseña, y llamadas a la API.
 */
@Component({
    selector: 'app-usuarios-lista',
    imports: [ButtonModule, ToastModule, FiltrosUsuariosComponent, FormularioUsuarioComponent, RestablecerContrasenaComponent, TablaUsuariosComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './usuarios-lista.html'
})
export class UsuariosListaComponent {
    private readonly servicio = inject(UsuariosService);
    private readonly rolesServicio = inject(RolesService);
    private readonly sesion = inject(SesionService);
    private readonly mensajes = inject(MessageService);

    protected readonly roles = signal<readonly RolAcceso[]>([]);
    protected readonly usuarios = signal<readonly UsuarioAdmin[]>([]);
    protected readonly totalRegistros = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(PAGINA_USUARIOS);
    private readonly filtro = signal<FiltroUsuarios>(FILTRO_INICIAL);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    private rolesCargados = false;
    private carga?: Subscription;

    protected readonly dialogo = signal<DialogoUsuario | null>(null);
    protected readonly dialogoVisible = signal(false);
    protected readonly guardando = signal(false);
    protected readonly errorDialogo = signal<string | null>(null);

    protected readonly usuarioRestablecer = signal<UsuarioAdmin | null>(null);
    protected readonly restablecerVisible = signal(false);
    protected readonly restableciendo = signal(false);
    protected readonly errorRestablecer = signal<string | null>(null);

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('usuarios.administrar'));
    protected readonly usuarioActualId = computed(() => this.sesion.usuarioActual()?.id ?? null);

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        this.cargar();
    }

    /** Carga (o reintenta) los roles, si aún no están, y la página actual con el filtro actual. */
    protected cargar(): void {
        this.cargarPagina(this.pagina());
    }

    protected filtrar(filtro: FiltroUsuarios): void {
        this.filtro.set(filtro);
        this.cargarPagina(0);
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        const pagina = paginaACargar(pedida, this.tamanoPagina());
        this.tamanoPagina.set(pedida.tamano);
        this.cargarPagina(pagina);
    }

    protected nuevo(): void {
        this.abrirFormulario({ modo: 'crear', usuario: null });
    }

    protected editar(usuario: UsuarioAdmin): void {
        this.abrirFormulario({ modo: 'editar', usuario });
    }

    protected abrirRestablecer(usuario: UsuarioAdmin): void {
        this.errorRestablecer.set(null);
        this.usuarioRestablecer.set(usuario);
        this.restablecerVisible.set(true);
    }

    protected crear(datos: CrearUsuario): void {
        this.guardar(this.servicio.crear(datos), () => {
            this.avisarExito('Usuario creado', `${datos.correo.toLowerCase()} deberá cambiar su contraseña temporal al iniciar sesión.`);
            // El nuevo puede caer en cualquier página según el orden por correo: se recarga la actual.
            this.cargarPagina(this.pagina());
        });
    }

    protected actualizar(cambios: EditarUsuario): void {
        const usuario = this.dialogo()?.usuario;
        if (!usuario) {
            return;
        }
        this.guardar(this.servicio.actualizar(usuario.id, cambios, usuario.miembroId), (actualizado) => {
            this.reemplazar(actualizado);
            this.avisarExito('Usuario actualizado', `Se guardaron los cambios de ${actualizado.correo}.`);
        });
    }

    protected restablecer(contrasenaTemporal: string): void {
        const usuario = this.usuarioRestablecer();
        if (!usuario) {
            return;
        }
        this.restableciendo.set(true);
        this.errorRestablecer.set(null);
        this.servicio
            .restablecerContrasena(usuario.id, contrasenaTemporal)
            .pipe(finalize(() => this.restableciendo.set(false)))
            .subscribe({
                next: () => {
                    this.reemplazar({ ...usuario, debeCambiarContrasena: true });
                    this.restablecerVisible.set(false);
                    this.avisarExito('Contraseña restablecida', `${usuario.correo} deberá cambiarla al iniciar sesión.`);
                },
                error: (error: unknown) => this.errorRestablecer.set(mensajeDeError(error, MENSAJE_ERROR_RESTABLECER))
            });
    }

    /** Pide la página al backend; una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarPagina(pagina: number): void {
        this.carga?.unsubscribe();
        this.pagina.set(pagina);
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.carga = forkJoin({ roles: this.rolesPendientes(), usuarios: this.servicio.listar(this.filtro(), pagina, this.tamanoPagina()) })
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ roles, usuarios }) => {
                    if (roles) {
                        this.roles.set(roles);
                        this.rolesCargados = true;
                    }
                    this.mostrar(usuarios);
                },
                // La pantalla no sirve sin datos: el estado de error con «Reintentar» se muestra siempre.
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }

    /**
     * `GET /api/roles` exige `roles.ver` o `usuarios.administrar`. Con solo `usuarios.ver` no se piden: la tabla es
     * de lectura y el filtro de rol queda solo con «Todos».
     */
    private rolesPendientes(): Observable<RolAcceso[] | null> {
        const puedeVerRoles = this.sesion.tienePermiso('roles.ver') || this.sesion.tienePermiso('usuarios.administrar');
        return this.rolesCargados || !puedeVerRoles ? of(null) : this.rolesServicio.listar();
    }

    private mostrar({ content, page }: Page<UsuarioAdmin>): void {
        this.usuarios.set(content);
        this.totalRegistros.set(page.totalElements);
    }

    private guardar(peticion: Observable<UsuarioAdmin>, alGuardar: (usuario: UsuarioAdmin) => void): void {
        this.guardando.set(true);
        this.errorDialogo.set(null);
        peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
            next: (usuario) => {
                this.dialogoVisible.set(false);
                alGuardar(usuario);
            },
            error: (error: unknown) => this.errorDialogo.set(mensajeDeError(error, MENSAJE_ERROR_GUARDAR))
        });
    }

    private abrirFormulario(dialogo: DialogoUsuario): void {
        this.errorDialogo.set(null);
        this.dialogo.set(dialogo);
        this.dialogoVisible.set(true);
    }

    /** Actualiza la fila sin recargar la página. */
    private reemplazar(usuario: UsuarioAdmin): void {
        this.usuarios.update((usuarios) => usuarios.map((actual) => (actual.id === usuario.id ? usuario : actual)));
    }

    private avisarExito(summary: string, detail: string): void {
        this.mensajes.add({ severity: 'success', summary, detail });
    }
}

/**
 * Los 400, 404 y 409 traen en `detail` el motivo en español (p. ej. el correo repetido o «sin administrador»). Los
 * 5xx y la falta de conexión ya los avisa el toast global: la página no repite el mensaje.
 */
function mensajeDeError(error: unknown, generico: string): string | null {
    if (esErrorGlobal(error)) {
        return null;
    }
    if (error instanceof HttpErrorResponse && ESTADOS_CON_DETALLE.includes(error.status) && typeof error.error?.detail === 'string') {
        return error.error.detail;
    }
    return generico;
}
