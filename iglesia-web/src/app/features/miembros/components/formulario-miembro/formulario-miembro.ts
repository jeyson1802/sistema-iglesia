import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { Referencia } from '@/app/core/models/referencia';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { MensajeErrorServidorComponent } from '@/app/shared/components/mensaje-error-servidor/mensaje-error-servidor';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { SelectorUbigeoComponent } from '@/app/shared/components/selector-ubigeo/selector-ubigeo';
import { hoyIso } from '@/app/shared/utils/fecha';
import { textoOpcional } from '@/app/shared/utils/texto';
import { ERROR_FECHA_FUTURA, fechaNoFutura } from '@/app/shared/validators/fecha-no-futura';
import {
    CONDICIONES,
    Condicion,
    ESTADOS_CIVILES,
    EstadoCivil,
    ETIQUETA_CONDICION,
    ETIQUETA_ESTADO_CIVIL,
    ETIQUETA_GENERO,
    ETIQUETA_NIVEL_ESTUDIOS,
    Genero,
    GENEROS,
    GuardarMiembro,
    LIMITES_MIEMBRO,
    Miembro,
    NIVELES_ESTUDIOS,
    NivelEstudios,
    PATRON_TELEFONO
} from '../../models/miembro';

/** Exige al menos un carácter que no sea espacio. */
const CON_TEXTO = /\S/;

/** Opción de un selector de nivel o grupo. */
interface OpcionCatalogo {
    readonly etiqueta: string;
    readonly valor: number;
}

const opciones = <T extends string>(valores: readonly T[], etiquetas: Record<T, string>): { etiqueta: string; valor: T }[] => valores.map((valor) => ({ etiqueta: etiquetas[valor], valor }));

/**
 * Los activos del catálogo, en su orden, más el asignado si se desactivó después: se conserva y se marca, para no
 * quitárselo al miembro al guardar.
 */
function opcionesDeCatalogo(catalogo: readonly { id: number; nombre: string; activo: boolean }[], asignado: Referencia | null): OpcionCatalogo[] {
    const activos = catalogo.filter(({ activo }) => activo).map(({ id, nombre }) => ({ etiqueta: nombre, valor: id }));
    return asignado && !asignado.activo ? [...activos, { etiqueta: `${asignado.nombre} (inactivo)`, valor: asignado.id }] : activos;
}

const aOpcion = (referencia: Referencia | null): OpcionMiembro | null => (referencia ? { id: referencia.id, nombre: referencia.nombre } : null);

/**
 * Ficha del miembro como formulario reactivo tipado (presentacional). Solo nombres y apellidos son obligatorios; la
 * fecha de nacimiento y la edad aproximada son independientes (se pueden llenar las dos). Un grupo no tiene nombre:
 * se ofrece como «<singular> de <líder>». En solo lectura todos los campos quedan deshabilitados. Los errores del
 * backend por campo se muestran junto a cada uno.
 */
@Component({
    selector: 'app-formulario-miembro',
    imports: [ReactiveFormsModule, ButtonModule, InputTextModule, SelectModule, TextareaModule, MensajeErrorCampoComponent, MensajeErrorServidorComponent, SelectorMiembroComponent, SelectorUbigeoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-miembro.html'
})
export class FormularioMiembroComponent {
    /** Miembro que se edita o se ve; `null` al registrar uno nuevo. */
    readonly miembro = input<Miembro | null>(null);
    /** Catálogo completo de niveles (en su orden). */
    readonly niveles = input.required<readonly { id: number; nombre: string; activo: boolean }[]>();
    /** Catálogo completo de grupos; el `nombre` de cada uno es el de su líder. */
    readonly grupos = input.required<readonly { id: number; nombre: string; activo: boolean }[]>();
    /** Nombre singular de los grupos en la iglesia. */
    readonly nombreGrupo = input('Grupo');
    readonly soloLectura = input(false);
    readonly guardando = input(false);
    /** Mensajes del backend por campo (`errores` del ProblemDetail). */
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});

    readonly guardar = output<GuardarMiembro>();
    readonly cancelar = output<void>();

    protected readonly limites = LIMITES_MIEMBRO;
    protected readonly hoy = hoyIso();
    protected readonly opcionesGenero = opciones(GENEROS, ETIQUETA_GENERO);
    protected readonly opcionesCondicion = opciones(CONDICIONES, ETIQUETA_CONDICION);
    protected readonly opcionesEstadoCivil = opciones(ESTADOS_CIVILES, ETIQUETA_ESTADO_CIVIL);
    protected readonly opcionesNivelEstudios = opciones(NIVELES_ESTUDIOS, ETIQUETA_NIVEL_ESTUDIOS);
    protected readonly opcionesNivel = computed(() => opcionesDeCatalogo(this.niveles(), this.miembro()?.nivel ?? null));
    protected readonly opcionesGrupo = computed(() => opcionesDeCatalogo(this.grupos(), this.miembro()?.grupo ?? null).map((opcion) => ({ ...opcion, etiqueta: `${this.nombreGrupo()} de ${opcion.etiqueta}` })));
    protected readonly esPastorPrincipal = computed(() => this.miembro()?.esPastorPrincipal ?? false);

    private readonly fb = inject(NonNullableFormBuilder);
    protected readonly formulario = this.fb.group({
        nombres: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.maxLength(LIMITES_MIEMBRO.nombres)]],
        apellidos: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.maxLength(LIMITES_MIEMBRO.apellidos)]],
        genero: new FormControl<Genero | null>(null),
        fechaNacimiento: ['', fechaNoFutura()],
        edadAproximada: new FormControl<number | null>(null, [Validators.min(0), Validators.max(LIMITES_MIEMBRO.edad)]),
        estadoCivil: new FormControl<EstadoCivil | null>(null),
        telefono: ['', [Validators.pattern(PATRON_TELEFONO), Validators.maxLength(LIMITES_MIEMBRO.telefono)]],
        correo: ['', [Validators.email, Validators.maxLength(LIMITES_MIEMBRO.correo)]],
        direccion: ['', Validators.maxLength(LIMITES_MIEMBRO.direccion)],
        distritoCodigo: new FormControl<string | null>(null),
        fechaIngreso: ['', fechaNoFutura()],
        condicion: new FormControl<Condicion | null>(null),
        nivelId: new FormControl<number | null>(null),
        lider: new FormControl<OpcionMiembro | null>(null),
        grupoId: new FormControl<number | null>(null),
        invitadoPor: new FormControl<OpcionMiembro | null>(null),
        registradoPor: new FormControl<OpcionMiembro | null>(null),
        nivelEstudios: new FormControl<NivelEstudios | null>(null),
        centroEstudios: ['', Validators.maxLength(LIMITES_MIEMBRO.centroEstudios)],
        ocupacion: ['', Validators.maxLength(LIMITES_MIEMBRO.ocupacion)],
        profesion: ['', Validators.maxLength(LIMITES_MIEMBRO.profesion)],
        peticion: ['', Validators.maxLength(LIMITES_MIEMBRO.peticion)]
    });

    protected readonly mensajes: Readonly<Record<string, MensajesError>> = {
        nombres: { required: 'Escribe los nombres', pattern: 'Escribe los nombres', maxlength: `Usa como máximo ${LIMITES_MIEMBRO.nombres} caracteres` },
        apellidos: { required: 'Escribe los apellidos', pattern: 'Escribe los apellidos', maxlength: `Usa como máximo ${LIMITES_MIEMBRO.apellidos} caracteres` },
        fechaNacimiento: { [ERROR_FECHA_FUTURA]: 'La fecha de nacimiento no puede ser futura' },
        edadAproximada: { min: 'La edad no puede ser negativa', max: `La edad no puede superar ${LIMITES_MIEMBRO.edad} años` },
        telefono: { pattern: 'Usa solo dígitos, espacios, + y -', maxlength: `Usa como máximo ${LIMITES_MIEMBRO.telefono} caracteres` },
        correo: { email: 'Escribe un correo válido, p. ej. nombre@correo.com', maxlength: `Usa como máximo ${LIMITES_MIEMBRO.correo} caracteres` },
        direccion: { maxlength: `Usa como máximo ${LIMITES_MIEMBRO.direccion} caracteres` },
        fechaIngreso: { [ERROR_FECHA_FUTURA]: 'La fecha de ingreso no puede ser futura' },
        centroEstudios: { maxlength: `Usa como máximo ${LIMITES_MIEMBRO.centroEstudios} caracteres` },
        ocupacion: { maxlength: `Usa como máximo ${LIMITES_MIEMBRO.ocupacion} caracteres` },
        profesion: { maxlength: `Usa como máximo ${LIMITES_MIEMBRO.profesion} caracteres` },
        peticion: { maxlength: `Usa como máximo ${LIMITES_MIEMBRO.peticion} caracteres` }
    };

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            const [miembro, soloLectura] = [this.miembro(), this.soloLectura()];
            untracked(() => this.reiniciar(miembro, soloLectura));
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid || this.soloLectura()) {
            this.formulario.markAllAsTouched();
            return;
        }
        const valores = this.formulario.getRawValue();
        this.guardar.emit({
            nombres: valores.nombres.trim(),
            apellidos: valores.apellidos.trim(),
            genero: valores.genero,
            fechaNacimiento: valores.fechaNacimiento || null,
            edadAproximada: valores.edadAproximada,
            fechaIngreso: valores.fechaIngreso || null,
            condicion: valores.condicion,
            estadoCivil: valores.estadoCivil,
            telefono: textoOpcional(valores.telefono),
            correo: textoOpcional(valores.correo),
            direccion: textoOpcional(valores.direccion),
            distritoCodigo: valores.distritoCodigo,
            ocupacion: textoOpcional(valores.ocupacion),
            profesion: textoOpcional(valores.profesion),
            nivelEstudios: valores.nivelEstudios,
            centroEstudios: textoOpcional(valores.centroEstudios),
            peticion: textoOpcional(valores.peticion),
            nivelId: valores.nivelId,
            liderId: this.esPastorPrincipal() ? null : (valores.lider?.id ?? null),
            grupoId: valores.grupoId,
            invitadoPorId: valores.invitadoPor?.id ?? null,
            registradoPorId: valores.registradoPor?.id ?? null
        });
    }

    private reiniciar(miembro: Miembro | null, soloLectura: boolean): void {
        this.formulario.reset(
            {
                nombres: miembro?.nombres ?? '',
                apellidos: miembro?.apellidos ?? '',
                genero: miembro?.genero ?? null,
                fechaNacimiento: miembro?.fechaNacimiento ?? '',
                edadAproximada: miembro?.edadAproximada ?? null,
                estadoCivil: miembro?.estadoCivil ?? null,
                telefono: miembro?.telefono ?? '',
                correo: miembro?.correo ?? '',
                direccion: miembro?.direccion ?? '',
                distritoCodigo: miembro?.distritoCodigo ?? null,
                fechaIngreso: miembro?.fechaIngreso ?? '',
                condicion: miembro?.condicion ?? null,
                nivelId: miembro?.nivel?.id ?? null,
                lider: aOpcion(miembro?.lider ?? null),
                grupoId: miembro?.grupo?.id ?? null,
                invitadoPor: aOpcion(miembro?.invitadoPor ?? null),
                registradoPor: aOpcion(miembro?.registradoPor ?? null),
                nivelEstudios: miembro?.nivelEstudios ?? null,
                centroEstudios: miembro?.centroEstudios ?? '',
                ocupacion: miembro?.ocupacion ?? '',
                profesion: miembro?.profesion ?? '',
                peticion: miembro?.peticion ?? ''
            },
            { emitEvent: false }
        );
        if (soloLectura) {
            this.formulario.disable({ emitEvent: false });
        } else {
            this.formulario.enable({ emitEvent: false });
            if (miembro?.esPastorPrincipal) {
                this.formulario.controls.lider.disable({ emitEvent: false });
            }
        }
        this.formulario.updateValueAndValidity();
    }
}
