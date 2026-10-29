import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { map, startWith, switchMap } from 'rxjs';
import { CORREO_IGLESIA_MAX, DatosIglesia, DENOMINACION_MAX, DIRECCION_MAX, Iglesia, NOMBRE_COMERCIAL_MAX, NOMBRE_COMERCIAL_MIN, SITIO_WEB_MAX, SITIO_WEB_PATRON, TELEFONO_MAX, TELEFONO_PATRON } from '@/app/core/iglesias/models/iglesia';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { MensajeErrorServidorComponent } from '@/app/shared/components/mensaje-error-servidor/mensaje-error-servidor';
import { SelectorUbigeoComponent } from '@/app/shared/components/selector-ubigeo/selector-ubigeo';
import { textoOpcional } from '@/app/shared/utils/texto';

/** Un texto hecho solo de espacios no cuenta: el backend lo guarda sin espacios sobrantes. */
const CON_TEXTO = /\S/;

export type GrupoIglesia = FormGroup<{
    nombreComercial: FormControl<string>;
    denominacion: FormControl<string>;
    direccion: FormControl<string>;
    distritoCodigo: FormControl<string | null>;
    telefono: FormControl<string>;
    correo: FormControl<string>;
    sitioWeb: FormControl<string>;
}>;

/** Crea los controles de la identidad de una iglesia con las mismas reglas que iglesia-api. */
export function crearGrupoIglesia(fb: NonNullableFormBuilder): GrupoIglesia {
    return fb.group({
        nombreComercial: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.minLength(NOMBRE_COMERCIAL_MIN), Validators.maxLength(NOMBRE_COMERCIAL_MAX)]],
        denominacion: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.maxLength(DENOMINACION_MAX)]],
        direccion: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.maxLength(DIRECCION_MAX)]],
        distritoCodigo: fb.control<string | null>(null, Validators.required),
        telefono: ['', [Validators.pattern(TELEFONO_PATRON), Validators.maxLength(TELEFONO_MAX)]],
        correo: ['', [Validators.email, Validators.maxLength(CORREO_IGLESIA_MAX)]],
        sitioWeb: ['', [Validators.pattern(SITIO_WEB_PATRON), Validators.maxLength(SITIO_WEB_MAX)]]
    });
}

/** Valores del formulario a partir de una iglesia (lo que falta queda vacío). */
export function valoresDeIglesia(iglesia: Iglesia): GrupoIglesia['value'] {
    return {
        nombreComercial: iglesia.nombreComercial,
        denominacion: iglesia.denominacion,
        direccion: iglesia.direccion ?? '',
        distritoCodigo: iglesia.distritoCodigo,
        telefono: iglesia.telefono ?? '',
        correo: iglesia.correo ?? '',
        sitioWeb: iglesia.sitioWeb ?? ''
    };
}

/** Datos para la API: textos sin espacios sobrantes y lo opcional vacío como `null`. */
export function datosDelGrupo(grupo: GrupoIglesia): DatosIglesia {
    const valor = grupo.getRawValue();
    return {
        nombreComercial: valor.nombreComercial.trim(),
        denominacion: valor.denominacion.trim(),
        direccion: valor.direccion.trim(),
        distritoCodigo: valor.distritoCodigo ?? '',
        telefono: textoOpcional(valor.telefono),
        correo: textoOpcional(valor.correo),
        sitioWeb: textoOpcional(valor.sitioWeb)
    };
}

/**
 * Campos de la identidad de una iglesia (presentacional): nombres, dirección con su ubicación y contacto. Los usan el
 * alta del catálogo y el editor de la identidad («Mi iglesia» y la edición desde el catálogo).
 */
@Component({
    selector: 'app-campos-iglesia',
    imports: [ReactiveFormsModule, InputTextModule, MensajeErrorCampoComponent, MensajeErrorServidorComponent, SelectorUbigeoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './campos-iglesia.html',
    host: { class: 'flex flex-col gap-4' }
})
export class CamposIglesiaComponent {
    readonly grupo = input.required<GrupoIglesia>();
    /** Prefijo de los `id` de los campos, para que no se repitan si hay dos formularios en la página. */
    readonly idPrefijo = input.required<string>();
    /** Mensajes del backend por campo (`errores` del ProblemDetail). */
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});

    protected readonly limites = { nombreComercial: NOMBRE_COMERCIAL_MAX, denominacion: DENOMINACION_MAX, direccion: DIRECCION_MAX, telefono: TELEFONO_MAX, correo: CORREO_IGLESIA_MAX, sitioWeb: SITIO_WEB_MAX };

    /** Caracteres del nombre comercial para el contador (los controles no son signals). */
    protected readonly largoNombre = toSignal(
        toObservable(this.grupo).pipe(
            switchMap((grupo) =>
                grupo.controls.nombreComercial.valueChanges.pipe(
                    startWith(grupo.controls.nombreComercial.value),
                    map((nombre) => nombre.trim().length)
                )
            )
        ),
        { initialValue: 0 }
    );

    protected readonly mensajes: Readonly<Record<string, MensajesError>> = {
        nombreComercial: {
            required: 'Escribe el nombre comercial de la iglesia',
            pattern: 'Escribe el nombre comercial de la iglesia',
            minlength: `Usa al menos ${NOMBRE_COMERCIAL_MIN} caracteres`,
            maxlength: `Usa como máximo ${NOMBRE_COMERCIAL_MAX} caracteres; es el nombre corto de la barra superior`
        },
        denominacion: {
            required: 'Escribe la denominación completa',
            pattern: 'Escribe la denominación completa',
            maxlength: `Usa como máximo ${DENOMINACION_MAX} caracteres`
        },
        direccion: {
            required: 'Escribe la dirección de la iglesia',
            pattern: 'Escribe la dirección de la iglesia',
            maxlength: `Usa como máximo ${DIRECCION_MAX} caracteres`
        },
        distritoCodigo: { required: 'Elige el departamento, la provincia y el distrito' },
        telefono: { pattern: 'Usa solo números, espacios, paréntesis, + y -, por ejemplo +51 1 234 5678', maxlength: `Usa como máximo ${TELEFONO_MAX} caracteres` },
        correo: { email: 'Escribe un correo válido, por ejemplo contacto@iglesia.org', maxlength: `Usa como máximo ${CORREO_IGLESIA_MAX} caracteres` },
        sitioWeb: { pattern: 'Escribe la dirección completa, por ejemplo https://iglesia.org', maxlength: `Usa como máximo ${SITIO_WEB_MAX} caracteres` }
    };
}
