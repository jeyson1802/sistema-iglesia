import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormRecord, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { RolAcceso } from '@/app/features/roles/models/rol-acceso';
import { CONTRASENA_TEMPORAL_MAX, CONTRASENA_TEMPORAL_MIN, CORREO_MAX, CrearUsuario, EditarUsuario, UsuarioAdmin } from '../../models/usuario-admin';

export type ModoFormularioUsuario = 'crear' | 'editar';

const ERROR_SIN_ROLES = 'sinRoles';

/** Exige al menos un rol marcado; cuentan también los bloqueados (deshabilitados), que se reenvían. */
function alMenosUnRol(control: AbstractControl): ValidationErrors | null {
    const marcados = Object.values((control as FormRecord<FormControl<boolean>>).getRawValue()).some(Boolean);
    return marcados ? null : { [ERROR_SIN_ROLES]: true };
}

/** El backend guarda el correo sin espacios y en minúsculas: así se compara para saber si cambió. */
const normalizarCorreo = (correo: string): string => correo.trim().toLowerCase();

/**
 * Diálogo para crear o editar un usuario (presentacional). Solo ofrece los roles activos que no exigen miembro (ni el
 * de administrador del sistema); los que el usuario ya tiene y exigen miembro se muestran bloqueados y se reenvían.
 * Cambiar el correo o desactivar al usuario cierra sus sesiones, así que se confirma antes de guardar.
 */
@Component({
    selector: 'app-formulario-usuario',
    imports: [ReactiveFormsModule, ButtonModule, CheckboxModule, ConfirmDialogModule, DialogModule, InputTextModule, MessageModule, PasswordModule, ToggleSwitchModule, MensajeErrorCampoComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-usuario.html'
})
export class FormularioUsuarioComponent {
    private readonly confirmacion = inject(ConfirmationService);
    private readonly fb = inject(NonNullableFormBuilder);

    readonly visible = model(false);
    readonly modo = input<ModoFormularioUsuario>('crear');
    /** Usuario que se edita; `null` al crear. */
    readonly usuario = input<UsuarioAdmin | null>(null);
    /** Roles de `GET /api/roles` (base y personalizados de la iglesia). */
    readonly roles = input.required<readonly RolAcceso[]>();
    readonly guardando = input(false);
    /** Mensaje del backend (400, 404 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly crear = output<CrearUsuario>();
    readonly actualizar = output<EditarUsuario>();

    protected readonly correoMax = CORREO_MAX;
    protected readonly contrasenaMax = CONTRASENA_TEMPORAL_MAX;

    /** Un control por rol mostrado. Se guarda aparte porque el `FormBuilder` lo tipa como `FormGroup`. */
    private readonly rolesMarcados = new FormRecord<FormControl<boolean>>({}, { validators: alMenosUnRol });

    protected readonly formulario = this.fb.group({
        correo: ['', [Validators.required, Validators.email, Validators.maxLength(CORREO_MAX)]],
        contrasenaTemporal: ['', [Validators.required, Validators.minLength(CONTRASENA_TEMPORAL_MIN), Validators.maxLength(CONTRASENA_TEMPORAL_MAX)]],
        activo: true,
        roles: this.rolesMarcados
    });

    protected readonly mensajesCorreo: MensajesError = {
        required: 'Escribe el correo del usuario',
        email: 'Escribe un correo válido, por ejemplo nombre@iglesia.org',
        maxlength: `El correo no puede superar ${CORREO_MAX} caracteres`
    };
    protected readonly mensajesContrasena: MensajesError = {
        required: 'Escribe una contraseña temporal',
        minlength: `Usa al menos ${CONTRASENA_TEMPORAL_MIN} caracteres`,
        maxlength: `Usa como máximo ${CONTRASENA_TEMPORAL_MAX} caracteres`
    };
    protected readonly ayudaContrasena = `Entre ${CONTRASENA_TEMPORAL_MIN} y ${CONTRASENA_TEMPORAL_MAX} caracteres. El usuario deberá cambiarla al iniciar sesión.`;
    protected readonly mensajesRoles: MensajesError = { [ERROR_SIN_ROLES]: 'Marca al menos un rol' };

    protected readonly titulo = computed(() => (this.modo() === 'crear' ? 'Nuevo usuario' : `Editar «${this.usuario()?.correo ?? ''}»`));

    /** Roles que el usuario editado ya tiene y exigen miembro: no se pueden quitar desde aquí. */
    private readonly idsBloqueados = computed<ReadonlySet<number>>(() => {
        const usuario = this.modo() === 'editar' ? this.usuario() : null;
        const asignados = new Set(usuario?.roles.map(({ id }) => id) ?? []);
        return new Set(
            this.roles()
                .filter((rol) => rol.requiereMiembro && asignados.has(rol.id))
                .map(({ id }) => id)
        );
    });

    /** Roles ofrecidos más los bloqueados, en el orden en que llegan del backend. */
    protected readonly rolesMostrados = computed(() => this.roles().filter((rol) => this.idsBloqueados().has(rol.id) || esOfrecido(rol)));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Al abrir el diálogo (o cambiar de usuario) se reinicia el formulario con sus datos.
        effect(() => {
            if (this.visible()) {
                const [usuario, modo, mostrados, bloqueados] = [this.usuario(), this.modo(), this.rolesMostrados(), this.idsBloqueados()];
                untracked(() => this.reiniciar(usuario, modo, mostrados, bloqueados));
            }
        });
    }

    protected esBloqueado(rol: RolAcceso): boolean {
        return this.idsBloqueados().has(rol.id);
    }

    /**
     * El contenido de `p-dialog` se proyecta y Angular lo crea aunque el diálogo esté cerrado, así que la plantilla
     * puede pedir un control antes de abrirlo: se crea la primera vez y después siempre es la misma instancia.
     */
    protected controlRol(id: number): FormControl<boolean> {
        const clave = String(id);
        if (!this.rolesMarcados.contains(clave)) {
            this.rolesMarcados.addControl(clave, new FormControl(false, { nonNullable: true }), { emitEvent: false });
        }
        return this.rolesMarcados.controls[clave];
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { correo, contrasenaTemporal, activo } = this.formulario.getRawValue();
        const rolIds = this.rolesMostrados()
            .map(({ id }) => id)
            .filter((id) => this.controlRol(id).value);
        if (this.modo() === 'crear') {
            this.crear.emit({ correo: correo.trim(), contrasenaTemporal, rolIds });
            return;
        }
        const cambios: EditarUsuario = { correo: correo.trim(), rolIds, activo };
        const avisos = this.avisosDeCierreDeSesiones(cambios);
        if (avisos.length === 0) {
            this.actualizar.emit(cambios);
            return;
        }
        this.confirmacion.confirm({
            header: 'Confirmar cambios',
            message: `${avisos.join(' ')} ¿Quieres continuar?`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Guardar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.actualizar.emit(cambios)
        });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    /** Cambiar el correo o desactivar al usuario revoca sus sesiones en el backend. */
    private avisosDeCierreDeSesiones(cambios: EditarUsuario): string[] {
        const usuario = this.usuario();
        if (!usuario) {
            return [];
        }
        const avisos: string[] = [];
        if (normalizarCorreo(cambios.correo) !== normalizarCorreo(usuario.correo)) {
            avisos.push(`Se cerrarán las sesiones del usuario y deberá entrar con el correo nuevo (${normalizarCorreo(cambios.correo)}).`);
        }
        if (usuario.activo && !cambios.activo) {
            avisos.push('Al desactivarlo se cerrarán sus sesiones y no podrá entrar hasta que lo reactives.');
        }
        return avisos;
    }

    private reiniciar(usuario: UsuarioAdmin | null, modo: ModoFormularioUsuario, mostrados: readonly RolAcceso[], bloqueados: ReadonlySet<number>): void {
        // `reset` deja en su valor inicial los controles que no recibe: los roles se asignan después.
        this.formulario.reset({ correo: usuario?.correo ?? '', contrasenaTemporal: '', activo: usuario?.activo ?? true }, { emitEvent: false });
        this.formulario.enable({ emitEvent: false });
        if (modo === 'editar') {
            this.formulario.controls.contrasenaTemporal.disable({ emitEvent: false });
        }
        const asignados = new Set(modo === 'editar' ? (usuario?.roles.map(({ id }) => id) ?? []) : []);
        for (const clave of Object.keys(this.rolesMarcados.controls)) {
            this.rolesMarcados.removeControl(clave, { emitEvent: false });
        }
        for (const { id } of mostrados) {
            const control = this.controlRol(id);
            control.setValue(asignados.has(id), { emitEvent: false });
            if (bloqueados.has(id)) {
                control.disable({ emitEvent: false });
            }
        }
        this.formulario.updateValueAndValidity();
    }
}

/** Roles que se pueden asignar desde esta pantalla. */
function esOfrecido(rol: RolAcceso): boolean {
    return rol.activo && !rol.requiereMiembro && rol.rolBase !== 'ADMIN_SISTEMA';
}
