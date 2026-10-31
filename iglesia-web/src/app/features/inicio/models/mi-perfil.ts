import { DiaSemana } from '@/app/core/models/dia-semana';
import { Condicion } from '@/app/features/miembros/models/miembro';

/** Contrato de `MiPerfilResponse`: los datos del propio miembro para su inicio. */
export interface MiPerfil {
    readonly miembroId: number;
    readonly nombres: string;
    readonly apellidos: string;
    readonly edad: number | null;
    readonly edadEsAproximada: boolean;
    readonly fechaIngreso: string | null;
    readonly condicion: Condicion | null;
    readonly nivel: string | null;
    readonly lider: string | null;
    readonly grupo: { readonly id: number; readonly nombre: string; readonly diaReunion: DiaSemana | null; readonly horaReunion: string | null } | null;
}
