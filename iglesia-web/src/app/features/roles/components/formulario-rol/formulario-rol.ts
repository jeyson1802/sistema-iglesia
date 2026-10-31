import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormRecord, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { Permiso } from '@/app/core/auth/models/permiso';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { ETIQUETA_ALCANCE, GuardarRol, INICIO_DEL_ALCANCE, NOMBRE_ROL_MAX, PermisoCatalogo, RolAcceso, ROLES_BASE_HEREDABLES } from '../../models/rol-acceso';
import { agruparPorModulo } from '../../services/rol-acceso.mapper';

/** Qué se hace en el diálogo: los roles base y quien no administra roles solo pueden verlos. */
export type ModoFormularioRol = 'crear' | 'editar' | 'ver';

const ERROR_SIN_PERMISOS = 'sinPermisos';

/** Exige al menos un permiso marcado. */
function alMenosUnPermiso(control: AbstractControl): ValidationErrors | null {
    const marcados = Object.values(control.value as Record<string, boolean>).some(Boolean);
    return marcados ? null : { [ERROR_SIN_PERMISOS]: true };
}

/**
 * Diálogo para crear, editar o ver un rol (presentacional). Al crear o editar se puede marcar cualquier permiso de
 * iglesia, sea cual sea el rol base (p. ej. «Tesorería» con todo lo de aportes). El rol base se muestra como «alcance»:
 * solo decide qué datos alcanzan sus permisos y la página de inicio. Los permisos del sistema (catálogo de iglesias…) no se ofrecen porque no se pueden asignar.
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
    /** Todos los roles base (de ellos salen las opciones de herencia y los permisos asignables). */
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
    protected readonly mensajesRolBase: MensajesError = { required: 'Elige el alcance del rol' };
    protected readonly mensajesPermisos: MensajesError = { [ERROR_SIN_PERMISOS]: 'Marca al menos un permiso' };

    protected readonly soloLectura = computed(() => this.modo() === 'ver');

    protected readonly titulo = computed(() => {
        const nombre = this.rol()?.nombre ?? '';
        return { crear: 'Nuevo rol', editar: `Editar «${nombre}»`, ver: `Permisos de «${nombre}»` }[this.modo()];
    });

    protected readonly opcionesRolBase = computed(() =>
        this.rolesBase()
            .filter((rol) => ROLES_BASE_HEREDABLES.includes(rol.rolBase))
            .map((rol) => ({ id: rol.id, etiqueta: ETIQUETA_ALCANCE[rol.rolBase], inicio: INICIO_DEL_ALCANCE[rol.rolBase] }))
    );

    /** Permisos que se pueden dar a un rol de iglesia: los de los roles base heredables (mismo criterio que el backend). */
    private readonly asignables = computed(
        () => new Set<Permiso>(this.rolesBase().filter((rol) => ROLES_BASE_HEREDABLES.includes(rol.rolBase)).flatMap((rol) => rol.permisos))
    );

    /** Al ver se muestran todos los permisos del rol; al crear o editar, solo los asignables. */
    private readonly permisosVisibles = computed(() => (this.soloLectura() ? this.catalogo() : this.catalogo().filter(({ codigo }) => this.asignables().has(codigo))));

    protected readonly grupos = computed(() => agruparPorModulo(this.permisosVisibles()));

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
            permisos: this.permisosVisibles()
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
        }
        this.formulario.updateValueAndValidity();
    }
}
