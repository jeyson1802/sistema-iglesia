import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormRecord, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { Permiso } from '@/app/core/auth/models/permiso';
import { ETIQUETA_ROL } from '@/app/core/auth/models/rol';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { GuardarRol, NOMBRE_ROL_MAX, PermisoCatalogo, RolAcceso, ROLES_BASE_HEREDABLES } from '../../models/rol-acceso';
import { agruparPorModulo } from '../../services/rol-acceso.mapper';

/** Qué se hace en el diálogo: los roles base y quien no administra roles solo pueden verlos. */
export type ModoFormularioRol = 'crear' | 'editar' | 'ver';

const ERROR_SIN_PERMISOS = 'sinPermisos';

/** Exige al menos un permiso marcado; los deshabilitados (fuera del rol base) no cuentan. */
function alMenosUnPermiso(control: AbstractControl): ValidationErrors | null {
    const marcados = Object.values(control.value as Record<string, boolean>).some(Boolean);
    return marcados ? null : { [ERROR_SIN_PERMISOS]: true };
}

/**
 * Diálogo para crear, editar o ver un rol (presentacional). Solo se pueden marcar los permisos del rol base: al
 * cambiarlo, los que ya no aplican se desmarcan y se deshabilitan.
 */
@Component({
    selector: 'app-formulario-rol',
    imports: [ReactiveFormsModule, ButtonModule, CheckboxModule, DialogModule, InputTextModule, MessageModule, SelectModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-rol.html'
})
export class FormularioRolComponent {
    readonly visible = model(false);
    readonly modo = input<ModoFormularioRol>('crear');
    /** Rol que se edita o se ve; `null` al crear. */
    readonly rol = input<RolAcceso | null>(null);
    /** Todos los roles base (de ellos salen las opciones de herencia y los permisos permitidos). */
    readonly rolesBase = input.required<readonly RolAcceso[]>();
    readonly catalogo = input.required<readonly PermisoCatalogo[]>();
    readonly guardando = input(false);
    /** Mensaje del backend (400 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly guardar = output<GuardarRol>();

    protected readonly nombreMax = NOMBRE_ROL_MAX;

    /** Un control por permiso del catálogo. Se guarda aparte porque el `FormBuilder` lo tipa como `FormGroup`. */
    private readonly permisos = new FormRecord<FormControl<boolean>>({}, { validators: alMenosUnPermiso });

    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombre: ['', [Validators.required, Validators.maxLength(NOMBRE_ROL_MAX)]],
        rolBaseId: new FormControl<number | null>(null, Validators.required),
        permisos: this.permisos
    });

    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre del rol',
        maxlength: `El nombre no puede superar ${NOMBRE_ROL_MAX} caracteres`
    };
    protected readonly mensajesRolBase: MensajesError = { required: 'Elige el rol base del que hereda' };
    protected readonly mensajesPermisos: MensajesError = { [ERROR_SIN_PERMISOS]: 'Marca al menos un permiso' };

    protected readonly soloLectura = computed(() => this.modo() === 'ver');

    protected readonly titulo = computed(() => {
        const nombre = this.rol()?.nombre ?? '';
        return { crear: 'Nuevo rol', editar: `Editar «${nombre}»`, ver: `Permisos de «${nombre}»` }[this.modo()];
    });

    protected readonly opcionesRolBase = computed(() =>
        this.rolesBase()
            .filter((rol) => ROLES_BASE_HEREDABLES.includes(rol.rolBase))
            .map((rol) => ({ id: rol.id, etiqueta: ETIQUETA_ROL[rol.rolBase] }))
    );

    protected readonly grupos = computed(() => agruparPorModulo(this.catalogo()));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Al abrir el diálogo (o cambiar de rol) se reinicia el formulario con los datos del rol.
        effect(() => {
            if (this.visible()) {
                const [rol, modo, catalogo] = [this.rol(), this.modo(), this.catalogo()];
                untracked(() => this.reiniciar(rol, modo, catalogo));
            }
        });
        // Al crear, el rol base elegido decide qué permisos se pueden marcar. Se escucha cada cambio (no un signal)
        // para que elegir de nuevo el mismo rol base tras reabrir el diálogo también aplique.
        this.formulario.controls.rolBaseId.valueChanges.pipe(takeUntilDestroyed()).subscribe((rolBaseId) => {
            if (this.modo() === 'crear') {
                this.limitarPermisos(this.permisosDelRolBase(this.rolesBase().find((rol) => rol.id === rolBaseId)));
            }
        });
    }

    /**
     * El contenido de `p-dialog` se proyecta y Angular lo crea aunque el diálogo esté cerrado, así que la plantilla
     * puede pedir un control antes de abrirlo: se crea la primera vez y después siempre es la misma instancia.
     */
    protected controlPermiso(codigo: Permiso): FormControl<boolean> {
        if (!this.permisos.contains(codigo)) {
            this.permisos.addControl(codigo, new FormControl(false, { nonNullable: true }), { emitEvent: false });
        }
        return this.permisos.controls[codigo];
    }

    protected enviar(): void {
        if (this.formulario.invalid || this.soloLectura()) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { nombre, rolBaseId, permisos } = this.formulario.getRawValue();
        this.guardar.emit({
            nombre: nombre.trim(),
            rolBaseId: this.modo() === 'crear' ? rolBaseId : null,
            permisos: this.catalogo()
                .map(({ codigo }) => codigo)
                .filter((codigo) => permisos[codigo])
        });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    private reiniciar(rol: RolAcceso | null, modo: ModoFormularioRol, catalogo: readonly PermisoCatalogo[]): void {
        // `reset` deja en su valor inicial los controles que no recibe: los permisos se asignan después.
        this.formulario.reset({ nombre: rol?.nombre ?? '', rolBaseId: null }, { emitEvent: false });
        for (const { codigo } of catalogo) {
            this.controlPermiso(codigo).setValue(rol?.permisos.includes(codigo) ?? false, { emitEvent: false });
        }
        if (modo === 'ver') {
            this.formulario.disable({ emitEvent: false });
        } else {
            this.formulario.enable({ emitEvent: false });
            if (modo === 'editar') {
                this.formulario.controls.rolBaseId.disable({ emitEvent: false });
            }
            // Al editar valen los permisos del rol base del que hereda; al crear, ninguno hasta elegirlo.
            this.limitarPermisos(modo === 'editar' ? this.permisosDelRolBase(this.rolesBase().find((base) => base.base && base.rolBase === rol?.rolBase)) : new Set());
        }
        this.formulario.updateValueAndValidity();
    }

    private permisosDelRolBase(rolBase: RolAcceso | undefined): ReadonlySet<Permiso> {
        return new Set(rolBase?.permisos ?? []);
    }

    /** Habilita solo los permisos permitidos; los demás se desmarcan para no enviar permisos fuera del rol base. */
    private limitarPermisos(permitidos: ReadonlySet<Permiso>): void {
        const permisos = this.permisos;
        for (const [codigo, control] of Object.entries(permisos.controls)) {
            if (permitidos.has(codigo as Permiso)) {
                control.enable({ emitEvent: false });
            } else {
                control.setValue(false, { emitEvent: false });
                control.disable({ emitEvent: false });
            }
        }
        permisos.updateValueAndValidity();
    }
}
