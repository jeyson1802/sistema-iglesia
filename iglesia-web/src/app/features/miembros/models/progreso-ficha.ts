import { Miembro } from './miembro';

/** Dato de la ficha que cuenta para el progreso, con la etiqueta que se muestra si falta. */
interface DatoFicha {
    readonly etiqueta: string;
    readonly tiene: (miembro: Miembro) => boolean;
}

export interface ProgresoFicha {
    /** Porcentaje entero de datos completos (0 a 100). */
    readonly porcentaje: number;
    /** Etiquetas de los datos que faltan, en el orden de la ficha. */
    readonly faltan: readonly string[];
}

const conValor = (valor: unknown): boolean => valor !== null && valor !== undefined && valor !== '';

/**
 * Datos que se esperan en una ficha completa. Nombres y apellidos siempre están; la petición es opcional y no cuenta.
 * El pastor principal no tiene líder, así que para él el líder cuenta como completo.
 */
const DATOS_FICHA: readonly DatoFicha[] = [
    { etiqueta: 'Nombres', tiene: (m) => conValor(m.nombres) },
    { etiqueta: 'Apellidos', tiene: (m) => conValor(m.apellidos) },
    { etiqueta: 'Género', tiene: (m) => conValor(m.genero) },
    { etiqueta: 'Fecha de nacimiento o edad', tiene: (m) => conValor(m.edad) },
    { etiqueta: 'Estado civil', tiene: (m) => conValor(m.estadoCivil) },
    { etiqueta: 'Teléfono', tiene: (m) => conValor(m.telefono) },
    { etiqueta: 'Correo', tiene: (m) => conValor(m.correo) },
    { etiqueta: 'Dirección', tiene: (m) => conValor(m.direccion) },
    { etiqueta: 'Distrito', tiene: (m) => conValor(m.distritoCodigo) },
    { etiqueta: 'Fecha de ingreso', tiene: (m) => conValor(m.fechaIngreso) },
    { etiqueta: 'Condición', tiene: (m) => conValor(m.condicion) },
    { etiqueta: 'Nivel', tiene: (m) => conValor(m.nivel) },
    { etiqueta: 'Líder', tiene: (m) => m.esPastorPrincipal || conValor(m.lider) },
    { etiqueta: 'Grupo', tiene: (m) => conValor(m.grupo) },
    { etiqueta: 'Invitado por', tiene: (m) => conValor(m.invitadoPor) },
    { etiqueta: 'Nivel de estudios', tiene: (m) => conValor(m.nivelEstudios) },
    { etiqueta: 'Centro de estudios', tiene: (m) => conValor(m.centroEstudios) },
    { etiqueta: 'Ocupación', tiene: (m) => conValor(m.ocupacion) },
    { etiqueta: 'Profesión', tiene: (m) => conValor(m.profesion) }
];

/** Etiqueta del dato «Grupo», que se reemplaza por el nombre que la iglesia da a sus grupos. */
const ETIQUETA_GRUPO = 'Grupo';

export function calcularProgreso(miembro: Miembro, nombreGrupo = ETIQUETA_GRUPO): ProgresoFicha {
    const faltan = DATOS_FICHA.filter(({ tiene }) => !tiene(miembro)).map(({ etiqueta }) => (etiqueta === ETIQUETA_GRUPO ? nombreGrupo : etiqueta));
    const completos = DATOS_FICHA.length - faltan.length;
    return { porcentaje: Math.round((completos * 100) / DATOS_FICHA.length), faltan };
}
