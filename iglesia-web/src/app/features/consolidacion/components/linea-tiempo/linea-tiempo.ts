import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { ETIQUETA_TIPO_REUNION, ReunionAsistida } from '@/app/features/asistencia/models/asistencia';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { Contacto, etiquetaResultado, fechaCompleta, fechaHoraCompleta, haceDias, ICONO_MEDIO, Severidad, SEVERIDAD_RESULTADO, textoProximo, tiempoRestante, VERBO_MEDIO } from '../../models/consolidacion';

/** Un punto de la línea de tiempo: un contacto o una asistencia. */
type Entrada = { readonly tipo: 'contacto'; readonly orden: number; readonly contacto: Contacto } | { readonly tipo: 'asistencia'; readonly orden: number; readonly reunion: ReunionAsistida };

const FONDO_MARCA: Readonly<Record<Severidad, string>> = {
    success: 'bg-green-500 text-white',
    info: 'bg-sky-500 text-white',
    warn: 'bg-orange-500 text-white',
    danger: 'bg-red-500 text-white',
    secondary: 'bg-surface-300 dark:bg-surface-600',
    contrast: 'bg-surface-900 text-surface-0 dark:bg-surface-0 dark:text-surface-900'
};

/**
 * Línea de tiempo de un caso (presentacional): junta los contactos y las asistencias del período, de lo más reciente a
 * lo más antiguo, para ver el efecto de cada contacto. Los contactos llevan ícono del medio y color del resultado; las
 * asistencias, un estilo atenuado. Los contactos corregibles ofrecen «Corregir» y «Borrar».
 */
@Component({
    selector: 'app-linea-tiempo',
    imports: [ButtonModule, TimelineModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './linea-tiempo.html'
})
export class LineaTiempoComponent {
    readonly contactos = input.required<readonly Contacto[]>();
    readonly asistencias = input<readonly ReunionAsistida[]>([]);
    readonly mensajeVacio = input('Aún no hay contactos registrados.');

    readonly corregir = output<Contacto>();
    readonly borrar = output<Contacto>();

    protected readonly entradas = computed<Entrada[]>(() =>
        [
            ...this.contactos().map((contacto): Entrada => ({ tipo: 'contacto', orden: new Date(contacto.fechaHora).getTime(), contacto })),
            ...this.asistencias().map((reunion): Entrada => ({ tipo: 'asistencia', orden: new Date(`${reunion.fecha}T12:00:00`).getTime(), reunion }))
        ].sort((a, b) => b.orden - a.orden)
    );
    protected readonly conDia = fechaConDia;
    protected readonly hace = haceDias;
    protected readonly completa = fechaHoraCompleta;
    protected readonly diaCompleto = fechaCompleta;
    protected readonly proximo = textoProximo;

    protected titulo({ consolidador, medio, resultado }: Contacto): string {
        return `${consolidador?.nombre.split(' ')[0] ?? 'Alguien'} ${VERBO_MEDIO[medio]} · ${etiquetaResultado(resultado, medio)}`;
    }

    protected icono(contacto: Contacto): string {
        return ICONO_MEDIO[contacto.medio];
    }

    protected marca(contacto: Contacto): string {
        return FONDO_MARCA[SEVERIDAD_RESULTADO[contacto.resultado]];
    }

    protected tipoReunion(reunion: ReunionAsistida): string {
        return ETIQUETA_TIPO_REUNION[reunion.tipo];
    }

    /** «Editable por 3 h más» para quien lo registró; el coordinador lo corrige siempre. */
    protected restante(contacto: Contacto): string | null {
        return contacto.editable && contacto.registradoPorMi ? tiempoRestante(contacto.editableHasta) : null;
    }
}
