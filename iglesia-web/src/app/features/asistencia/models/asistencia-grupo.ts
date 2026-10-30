/** Contrato de `ReunionGrupoResponse`: resumen de una reunión de grupo. */
export interface ReunionGrupo {
    readonly reunionId: number;
    readonly fecha: string;
    readonly hora: string | null;
    readonly esperados: number;
    readonly asistieron: number;
    readonly faltaron: number;
    readonly visitantes: number;
    readonly porcentaje: number | null;
}

/** Contrato de `PersonaReunionGrupoResponse`. */
export interface PersonaReunionGrupo {
    readonly miembroId: number;
    readonly nombreCompleto: string;
    readonly nivel: string | null;
}

/** Contrato de `DetalleReunionGrupoResponse`: quiénes asistieron, faltaron y vinieron de visita. */
export interface DetalleReunionGrupo {
    readonly reunionId: number;
    readonly fecha: string;
    readonly hora: string | null;
    readonly asistieron: readonly PersonaReunionGrupo[];
    readonly faltaron: readonly PersonaReunionGrupo[];
    readonly visitantes: readonly PersonaReunionGrupo[];
}

/** Listas del detalle de una reunión, en el orden en que se muestran. */
export const LISTAS_REUNION_GRUPO = ['asistieron', 'faltaron', 'visitantes'] as const;
export type ListaReunionGrupo = (typeof LISTAS_REUNION_GRUPO)[number];

/** Cómo se nombra cada lista; en la reunión de hoy, quien falta aún puede llegar: «sin marcar». */
export function etiquetaLista(lista: ListaReunionGrupo, esHoy: boolean): string {
    switch (lista) {
        case 'asistieron':
            return 'Asistieron';
        case 'faltaron':
            return esHoy ? 'Sin marcar' : 'No asistieron';
        case 'visitantes':
            return 'Visitantes';
    }
}
