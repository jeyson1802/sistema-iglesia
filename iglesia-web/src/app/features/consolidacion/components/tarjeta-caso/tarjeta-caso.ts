import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ETIQUETA_CONDICION } from '@/app/features/miembros/models/miembro';
import { BotonesContactoComponent, MedioBoton } from '@/app/shared/components/botones-contacto/botones-contacto';
import { CasoResumen, diasHasta, ETIQUETA_MEDIO, iniciales, lineaContexto, textoIteracion, textoProximo } from '../../models/consolidacion';
import { ContactoEnCurso } from '../../services/contacto-en-curso';

/**
 * Tarjeta de una persona en «Mis asignados» (presentacional): iniciales, nombre, etiquetas, línea de contexto y los
 * botones grandes «Llamar», «WhatsApp» y «Registrar». Si se abrió una llamada que no se registró, lo recuerda.
 */
@Component({
    selector: 'app-tarjeta-caso',
    imports: [ButtonModule, TagModule, BotonesContactoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjeta-caso.html'
})
export class TarjetaCasoComponent {
    readonly caso = input.required<CasoResumen>();
    /** Contacto abierto desde esta tarjeta que todavía no se registró. */
    readonly sinRegistrar = input<ContactoEnCurso | null>(null);

    readonly contactar = output<MedioBoton>();
    readonly registrar = output<void>();
    readonly ver = output<void>();
    readonly descartar = output<void>();

    protected readonly iniciales = computed(() => iniciales(this.caso().persona.nombreCompleto));
    protected readonly contexto = computed(() => lineaContexto(this.caso()));
    protected readonly esNuevo = computed(() => this.caso().persona.condicion === 'NUEVO');
    protected readonly etiquetaNuevo = ETIQUETA_CONDICION.NUEVO;
    protected readonly iteracion = computed(() => (this.caso().iteracion > 1 ? textoIteracion(this.caso().iteracion) : null));
    protected readonly proximo = computed(() => {
        const fecha = this.caso().proximoContacto;
        return fecha ? { texto: textoProximo(fecha), vencido: diasHasta(fecha) <= 0 } : null;
    });
    protected readonly pendiente = computed(() => {
        const contacto = this.sinRegistrar();
        return contacto ? `${ETIQUETA_MEDIO[contacto.medio]} sin registrar` : null;
    });
}
