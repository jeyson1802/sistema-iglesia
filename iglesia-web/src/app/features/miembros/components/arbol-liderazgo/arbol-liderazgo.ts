import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { PrimeTemplate, TreeNode } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { TreeModule } from 'primeng/tree';
import { EslabonCadena, Liderado, Miembro, nombreCompleto } from '../../models/miembro';
import { MiembrosService } from '../../services/miembros.service';

/** Filas de relleno mientras se carga la cadena, cada una más adentro, como el árbol. */
const FILAS_CARGA = ['pl-0', 'pl-6', 'pl-12'];

/** Lugar del nodo en el árbol respecto del miembro de la ficha. */
type Rol = 'superior' | 'actual' | 'liderado';

/** Datos que muestra cada nodo. `cantidadLiderados` es `null` en los superiores (no se consulta). */
export interface DatosNodo {
    readonly id: number;
    readonly nombre: string;
    /** Relación con el miembro de la ficha: «Líder directo», «Superior», «Esta ficha» o «Liderado». */
    readonly relacion: string;
    readonly nivel: string | null;
    readonly cantidadLiderados: number | null;
    readonly esPastorPrincipal: boolean;
    readonly activo: boolean;
    readonly rol: Rol;
}

const RELACION: Record<Rol, string> = { superior: 'Superior', actual: 'Esta ficha', liderado: 'Liderado' };
const RELACION_LIDER_DIRECTO = 'Líder directo';

const conElemento = (conjunto: ReadonlySet<number>, id: number): ReadonlySet<number> => new Set(conjunto).add(id);
const sinElemento = (conjunto: ReadonlySet<number>, id: number): ReadonlySet<number> => {
    const copia = new Set(conjunto);
    copia.delete(id);
    return copia;
};

/**
 * Árbol de liderazgo de la ficha: arriba los superiores (del pastor principal hacia abajo), el miembro resaltado y
 * debajo las personas que lidera, cada una con su nivel. Elegir un nodo (clic o Enter) abre su ficha. Los liderados
 * se piden al expandir cada nodo (un nivel cada vez), así que este componente usa `MiembrosService` directamente; la
 * cadena la recibe ya cargada. El estado (liderados cargados, nodos abiertos) vive en signals inmutables y el árbol se
 * reconstruye a partir de ellos.
 */
@Component({
    selector: 'app-arbol-liderazgo',
    imports: [PrimeTemplate, AvatarModule, MessageModule, SkeletonModule, TagModule, TooltipModule, TreeModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './arbol-liderazgo.html'
})
export class ArbolLiderazgoComponent {
    private readonly servicio = inject(MiembrosService);
    private readonly router = inject(Router);

    readonly miembro = input.required<Miembro>();
    /** Líderes del directo al pastor principal. */
    readonly cadena = input.required<readonly EslabonCadena[]>();
    readonly cargando = input(false);

    protected readonly filasCarga = FILAS_CARGA;
    /** Liderados directos ya cargados, por id del líder. */
    private readonly liderados = signal<ReadonlyMap<number, readonly Liderado[]>>(new Map());
    /** Liderados abiertos (empiezan cerrados). */
    private readonly abiertos = signal<ReadonlySet<number>>(new Set());
    /** Superiores y miembro cerrados a mano (empiezan abiertos). */
    private readonly cerrados = signal<ReadonlySet<number>>(new Set());
    private readonly pidiendo = signal<ReadonlySet<number>>(new Set());
    protected readonly errorLiderados = signal(false);

    protected readonly textoLiderados = computed(() => {
        const cantidad = this.miembro().cantidadLiderados;
        if (cantidad === 0) {
            return 'No lidera a nadie directamente.';
        }
        return cantidad === 1 ? 'Lidera a 1 persona directamente.' : `Lidera a ${cantidad} personas directamente.`;
    });

    /** Del pastor al miembro, una sola rama; debajo del miembro, sus liderados. */
    protected readonly nodos = computed<TreeNode<DatosNodo>[]>(() => {
        const miembro = this.miembro();
        let raiz: TreeNode<DatosNodo> = {
            key: `m-${miembro.id}`,
            label: nombreCompleto(miembro),
            data: {
                id: miembro.id,
                nombre: nombreCompleto(miembro),
                relacion: RELACION.actual,
                nivel: miembro.nivel?.nombre ?? null,
                cantidadLiderados: miembro.cantidadLiderados,
                esPastorPrincipal: miembro.esPastorPrincipal,
                activo: miembro.activo,
                rol: 'actual'
            },
            selectable: false,
            leaf: miembro.cantidadLiderados === 0,
            expanded: !this.cerrados().has(miembro.id),
            loading: this.pidiendo().has(miembro.id),
            children: this.hijos(miembro.id)
        };
        this.cadena().forEach((eslabon, indice) => {
            raiz = {
                key: `m-${eslabon.id}`,
                label: eslabon.nombreCompleto,
                data: {
                    id: eslabon.id,
                    nombre: eslabon.nombreCompleto,
                    relacion: indice === 0 ? RELACION_LIDER_DIRECTO : RELACION.superior,
                    nivel: eslabon.nivel,
                    cantidadLiderados: null,
                    esPastorPrincipal: eslabon.esPastorPrincipal,
                    activo: eslabon.activo,
                    rol: 'superior'
                },
                leaf: false,
                expanded: !this.cerrados().has(eslabon.id),
                children: [raiz]
            };
        });
        return [raiz];
    });

    constructor() {
        effect(() => {
            const miembro = this.miembro();
            untracked(() => this.reiniciar(miembro));
        });
    }

    /** El cuerpo del árbol recibe el nodo sin tipo: se tipa aquí. */
    protected datos(nodo: TreeNode<DatosNodo>): DatosNodo {
        return nodo.data as DatosNodo;
    }

    /** Hasta dos iniciales para el avatar: «Ana Ríos» → «AR». */
    protected iniciales(nombre: string): string {
        return nombre
            .split(/\s+/)
            .filter((parte) => !!parte)
            .slice(0, 2)
            .map((parte) => parte.charAt(0).toUpperCase())
            .join('');
    }

    protected textoLideraA(cantidad: number): string {
        return cantidad === 1 ? 'Lidera a 1 persona' : `Lidera a ${cantidad} personas`;
    }

    /** Elegir a otra persona del árbol abre su ficha; el miembro actual no es seleccionable. */
    protected abrir(nodo: TreeNode<DatosNodo>): void {
        const { id, rol } = this.datos(nodo);
        if (rol !== 'actual') {
            this.router.navigate(['/miembros', id]);
        }
    }

    protected expandir(nodo: TreeNode<DatosNodo>): void {
        const { id, rol } = this.datos(nodo);
        if (rol === 'liderado') {
            this.abiertos.update((abiertos) => conElemento(abiertos, id));
        } else {
            this.cerrados.update((cerrados) => sinElemento(cerrados, id));
        }
        if (rol !== 'superior' && !this.liderados().has(id)) {
            this.cargarLiderados(id);
        }
    }

    protected colapsar(nodo: TreeNode<DatosNodo>): void {
        const { id, rol } = this.datos(nodo);
        if (rol === 'liderado') {
            this.abiertos.update((abiertos) => sinElemento(abiertos, id));
        } else {
            this.cerrados.update((cerrados) => conElemento(cerrados, id));
        }
    }

    private hijos(id: number): TreeNode<DatosNodo>[] {
        return (this.liderados().get(id) ?? []).map((liderado) => ({
            key: `m-${liderado.id}`,
            label: liderado.nombreCompleto,
            data: {
                id: liderado.id,
                nombre: liderado.nombreCompleto,
                relacion: RELACION.liderado,
                nivel: liderado.nivel,
                cantidadLiderados: liderado.cantidadLiderados,
                esPastorPrincipal: false,
                activo: true,
                rol: 'liderado'
            },
            leaf: liderado.cantidadLiderados === 0,
            expanded: this.abiertos().has(liderado.id),
            loading: this.pidiendo().has(liderado.id),
            children: this.hijos(liderado.id)
        }));
    }

    /** Al cambiar de miembro se empieza de cero y, si lidera a alguien, se cargan sus liderados directos. */
    private reiniciar(miembro: Miembro): void {
        this.liderados.set(new Map());
        this.abiertos.set(new Set());
        this.cerrados.set(new Set());
        this.pidiendo.set(new Set());
        this.errorLiderados.set(false);
        if (miembro.cantidadLiderados > 0) {
            this.cargarLiderados(miembro.id);
        }
    }

    private cargarLiderados(id: number): void {
        this.pidiendo.update((pidiendo) => conElemento(pidiendo, id));
        this.errorLiderados.set(false);
        this.servicio.liderados(id).subscribe({
            next: (lista) => {
                this.liderados.update((cargados) => new Map(cargados).set(id, lista));
                this.pidiendo.update((pidiendo) => sinElemento(pidiendo, id));
            },
            error: () => {
                this.errorLiderados.set(true);
                this.pidiendo.update((pidiendo) => sinElemento(pidiendo, id));
            }
        });
    }
}
