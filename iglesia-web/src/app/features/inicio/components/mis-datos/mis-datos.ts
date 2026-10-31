import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ETIQUETA_DIA } from '@/app/core/models/dia-semana';
import { ETIQUETA_CONDICION } from '@/app/features/miembros/models/miembro';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { MiPerfil } from '../../models/mi-perfil';

/** «Mis datos» (presentacional): líder, grupo con su día y hora, nivel, condición, ingreso y edad; solo lectura. */
@Component({
    selector: 'app-mis-datos',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mis-datos.html'
})
export class MisDatosComponent {
    readonly perfil = input.required<MiPerfil>();

    protected readonly filas = computed(() => {
        const { lider, grupo, nivel, condicion, fechaIngreso, edad, edadEsAproximada } = this.perfil();
        const reunion = grupo?.diaReunion ? [ETIQUETA_DIA[grupo.diaReunion], grupo.horaReunion?.slice(0, 5)].filter(Boolean).join(' · ') : null;
        return [
            { etiqueta: 'Mi líder', valor: lider ?? 'Sin asignar' },
            { etiqueta: 'Mi grupo', valor: grupo ? (reunion ? `${grupo.nombre} (${reunion})` : grupo.nombre) : 'Aún no estás en un grupo' },
            { etiqueta: 'Nivel', valor: nivel ?? 'Sin nivel' },
            { etiqueta: 'Condición', valor: condicion ? ETIQUETA_CONDICION[condicion] : '—' },
            { etiqueta: 'Fecha de ingreso', valor: fechaIngreso ? fechaConDia(fechaIngreso) : '—' },
            { etiqueta: 'Edad', valor: edad === null ? '—' : `${edadEsAproximada ? 'Unos ' : ''}${edad} años` }
        ];
    });
}
