import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin, map, Subscription } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { describirUbicacion } from '@/app/core/models/ubicacion';
import { AsistenciaGrupoComponent, GrupoAsistencia } from '@/app/features/asistencia/components/asistencia-grupo/asistencia-grupo';
import { IntegrantesGrupoComponent } from '../components/integrantes-grupo/integrantes-grupo';
import { CandidatoGrupo, ETIQUETA_DIA, Grupo, IntegranteGrupo, nombreDeGrupo } from '../models/grupo';
import { GruposService } from '../services/grupos.service';

const MENSAJE_ERROR_CARGA = 'No se pudo cargar el grupo.';
const MENSAJE_ERROR_BUSQUEDA = 'No se pudieron buscar miembros disponibles.';
const MENSAJE_ERROR_AGREGAR = 'No se pudo agregar al miembro. Intenta de nuevo.';
const MENSAJE_ERROR_QUITAR = 'No se pudo quitar al miembro. Intenta de nuevo.';

const porNombre = (a: IntegranteGrupo, b: IntegranteGrupo): number => a.nombreCompleto.localeCompare(b.nombreCompleto, 'es');
const conId = (ids: ReadonlySet<number>, id: number): ReadonlySet<number> => new Set(ids).add(id);
const sinId = (ids: ReadonlySet<number>, id: number): ReadonlySet<number> => new Set([...ids].filter((otro) => otro !== id));

/**
 * Integrantes de un grupo (contenedor), en `/grupos/:id`: carga el grupo y sus miembros, busca candidatos y agrega,
 * mueve o quita miembros. Mover a alguien que está en otro grupo pide confirmación nombrando ese grupo. Solo edita
 * quien tiene `grupos.administrar`, y solo si el grupo está activo.
 */
@Component({
    selector: 'app-grupo-miembros',
    imports: [RouterLink, ButtonModule, ConfirmDialogModule, MessageModule, SkeletonModule, TagModule, AsistenciaGrupoComponent, IntegrantesGrupoComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './grupo-miembros.html'
})
export class GrupoMiembrosComponent {
    private readonly servicio = inject(GruposService);
    private readonly sesion = inject(SesionService);
    private readonly confirmacion = inject(ConfirmationService);

    protected readonly nombres = inject(ConfiguracionIglesiaService).nombresGrupos;
    protected readonly grupo = signal<Grupo | null>(null);
    protected readonly integrantes = signal<readonly IntegranteGrupo[]>([]);
    protected readonly candidatos = signal<readonly CandidatoGrupo[]>([]);
    protected readonly cargando = signal(true);
    protected readonly buscando = signal(false);
    protected readonly procesando = signal<ReadonlySet<number>>(new Set());
    protected readonly errorCarga = signal<string | null>(null);
    protected readonly errorAccion = signal<string | null>(null);
    protected readonly aviso = signal<string | null>(null);

    private grupoId = 0;
    private textoBuscado = '';
    private busqueda?: Subscription;

    protected readonly puedeAdministrar = computed(() => this.sesion.tienePermiso('grupos.administrar'));
    protected readonly editable = computed(() => this.puedeAdministrar() && (this.grupo()?.activo ?? false));
    protected readonly titulo = computed(() => {
        const grupo = this.grupo();
        return grupo ? nombreDeGrupo(this.nombres().nombreGrupoSingular, grupo.lider.nombre) : this.nombres().nombreGrupoSingular;
    });
    protected readonly grupoAsistencia = computed<GrupoAsistencia | null>(() => {
        const grupo = this.grupo();
        return grupo ? { id: grupo.id, nombre: this.titulo(), diaReunion: grupo.diaReunion, activo: grupo.activo } : null;
    });
    /** «Jueves 19:30 · Av. Perú 123 · San Martín de Porres, Lima, Lima», con lo que haya. */
    protected readonly reunion = computed(() => {
        const grupo = this.grupo();
        if (!grupo) {
            return '';
        }
        const cuando = [grupo.diaReunion ? ETIQUETA_DIA[grupo.diaReunion] : null, grupo.horaReunion].filter(Boolean).join(' ');
        return [cuando, grupo.direccion, describirUbicacion(grupo.ubicacion)].filter((parte) => !!parte).join(' · ');
    });

    constructor() {
        inject(DestroyRef).onDestroy(() => this.busqueda?.unsubscribe());
        inject(ActivatedRoute)
            .paramMap.pipe(
                map((parametros) => Number(parametros.get('id'))),
                takeUntilDestroyed()
            )
            .subscribe((id) => {
                this.grupoId = id;
                this.cargar();
            });
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.errorCarga.set(null);
        forkJoin({ grupo: this.servicio.obtener(this.grupoId), integrantes: this.servicio.integrantes(this.grupoId) })
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ grupo, integrantes }) => {
                    this.grupo.set(grupo);
                    this.integrantes.set(integrantes);
                    if (this.puedeAdministrar() && grupo.activo) {
                        this.buscar(this.textoBuscado);
                    }
                },
                error: (error: unknown) => this.errorCarga.set(detalleDeError(error, MENSAJE_ERROR_CARGA) ?? MENSAJE_ERROR_CARGA)
            });
    }

    /** Una búsqueda nueva cancela la anterior para que no llegue una respuesta vieja. */
    protected buscar(texto: string): void {
        this.textoBuscado = texto;
        this.busqueda?.unsubscribe();
        this.buscando.set(true);
        this.busqueda = this.servicio
            .candidatos(this.grupoId, texto)
            .pipe(finalize(() => this.buscando.set(false)))
            .subscribe({ next: (candidatos) => this.candidatos.set(candidatos), error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_BUSQUEDA)) });
    }

    /** Quien ya está en otro grupo se mueve después de confirmar; quien no tiene grupo se agrega directamente. */
    protected agregar(candidato: CandidatoGrupo): void {
        this.limpiarMensajes();
        if (!candidato.grupo) {
            this.enviarAgregar(candidato);
            return;
        }
        const singular = this.nombres().nombreGrupoSingular.toLowerCase();
        const destino = nombreDeGrupo(singular, this.grupo()?.lider.nombre ?? '');
        this.confirmacion.confirm({
            header: 'Ya está en otro grupo',
            message: `«${candidato.nombreCompleto}» ya está en ${nombreDeGrupo(singular, candidato.grupo.nombre)}. ¿Deseas moverlo a ${destino}? Dejará su grupo actual.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Sí, moverlo',
            rejectLabel: 'Cancelar',
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarAgregar(candidato)
        });
    }

    protected quitar(integrante: IntegranteGrupo): void {
        this.limpiarMensajes();
        this.procesando.update((ids) => conId(ids, integrante.id));
        this.servicio
            .quitarIntegrante(this.grupoId, integrante.id)
            .pipe(finalize(() => this.procesando.update((ids) => sinId(ids, integrante.id))))
            .subscribe({
                next: () => {
                    this.integrantes.update((lista) => lista.filter(({ id }) => id !== integrante.id));
                    this.aviso.set(`${integrante.nombreCompleto} salió del grupo.`);
                    this.buscar(this.textoBuscado);
                },
                error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_QUITAR))
            });
    }

    private enviarAgregar(candidato: CandidatoGrupo): void {
        this.procesando.update((ids) => conId(ids, candidato.id));
        this.servicio
            .agregarIntegrante(this.grupoId, candidato.id)
            .pipe(finalize(() => this.procesando.update((ids) => sinId(ids, candidato.id))))
            .subscribe({
                next: (integrante) => {
                    this.integrantes.update((lista) => [...lista.filter(({ id }) => id !== integrante.id), integrante].sort(porNombre));
                    this.candidatos.update((lista) => lista.filter(({ id }) => id !== candidato.id));
                    this.aviso.set(`${candidato.nombreCompleto} se agregó al grupo.`);
                },
                error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_AGREGAR))
            });
    }

    private limpiarMensajes(): void {
        this.errorAccion.set(null);
        this.aviso.set(null);
    }
}
