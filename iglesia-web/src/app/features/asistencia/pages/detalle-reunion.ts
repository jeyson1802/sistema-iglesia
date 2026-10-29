import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, Subscription } from 'rxjs';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { TablaAsistentesComponent } from '../components/tabla-asistentes/tabla-asistentes';
import { Asistente, ETIQUETA_TIPO_REUNION, horaCorta, Reunion, SEVERIDAD_TIPO_REUNION } from '../models/asistencia';
import { RUTA_ASISTENCIA } from '../models/rutas-asistencia';
import { AsistenciasService } from '../services/asistencias.service';
import { ReunionesService } from '../services/reuniones.service';

/** Espera tras la última tecla antes de buscar por nombre. */
const ESPERA_BUSQUEDA_MS = 300;
const MENSAJE_ERROR_REUNION = 'No se encontró la reunión o no tienes acceso a ella.';
const MENSAJE_ERROR_ASISTENTES = 'No se pudieron cargar los asistentes.';
const MENSAJE_ERROR_ELIMINAR = 'No se pudo eliminar el evento. Intenta de nuevo.';

/** Detalle de una reunión (contenedor): total, asistentes paginados con búsqueda y, en eventos vacíos, eliminarlos. */
@Component({
    selector: 'app-detalle-reunion',
    imports: [ReactiveFormsModule, RouterLink, ButtonModule, ConfirmDialogModule, IconFieldModule, InputIconModule, InputTextModule, MessageModule, SkeletonModule, TagModule, TablaAsistentesComponent],
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './detalle-reunion.html'
})
export class DetalleReunionComponent implements OnInit {
    private readonly reunionesServicio = inject(ReunionesService);
    private readonly asistenciasServicio = inject(AsistenciasService);
    private readonly sesion = inject(SesionService);
    private readonly confirmacion = inject(ConfirmationService);
    private readonly router = inject(Router);
    private readonly destroyRef = inject(DestroyRef);

    /** Parámetro de ruta `:id`. */
    private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

    protected readonly rutaInicio = RUTA_ASISTENCIA;
    protected readonly reunion = signal<Reunion | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal<string | null>(null);
    protected readonly errorAccion = signal<string | null>(null);
    protected readonly eliminando = signal(false);

    protected readonly busqueda = new FormControl('', { nonNullable: true });
    protected readonly asistentes = signal<readonly Asistente[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargandoAsistentes = signal(true);
    protected readonly errorAsistentes = signal<string | null>(null);
    private readonly texto = signal('');

    protected readonly tipo = computed(() => {
        const reunion = this.reunion();
        return reunion ? { etiqueta: ETIQUETA_TIPO_REUNION[reunion.tipo], severidad: SEVERIDAD_TIPO_REUNION[reunion.tipo] } : null;
    });
    protected readonly cuando = computed(() => {
        const reunion = this.reunion();
        if (!reunion) {
            return '';
        }
        const hora = horaCorta(reunion.hora);
        return hora ? `${fechaConDia(reunion.fecha)} · ${hora}` : fechaConDia(reunion.fecha);
    });
    protected readonly puedeTomar = computed(() => this.sesion.tienePermiso('asistencia.registrar') || this.sesion.tienePermiso('asistencia.grupo'));
    protected readonly puedeEliminar = computed(() => this.reunion()?.tipo === 'EVENTO' && this.reunion()?.totalAsistentes === 0 && this.sesion.tienePermiso('asistencia.configurar'));
    protected readonly filtrando = computed(() => this.texto().length > 0);

    private carga?: Subscription;

    ngOnInit(): void {
        this.destroyRef.onDestroy(() => this.carga?.unsubscribe());
        this.reunionesServicio
            .obtener(this.id)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (reunion) => {
                    this.reunion.set(reunion);
                    this.cargarAsistentes(0);
                },
                error: () => this.error.set(MENSAJE_ERROR_REUNION)
            });
        this.busqueda.valueChanges.pipe(debounceTime(ESPERA_BUSQUEDA_MS), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe((texto) => {
            this.texto.set(texto.trim());
            this.cargarAsistentes(0);
        });
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        const pagina = paginaACargar(pedida, this.tamanoPagina());
        this.tamanoPagina.set(pedida.tamano);
        this.cargarAsistentes(pagina);
    }

    protected recargarAsistentes(): void {
        this.cargarAsistentes(this.pagina());
    }

    protected eliminar(): void {
        const reunion = this.reunion();
        if (!reunion) {
            return;
        }
        this.confirmacion.confirm({
            header: 'Eliminar evento',
            message: `¿Eliminar «${reunion.nombre}» del ${fechaConDia(reunion.fecha)}? No tiene asistentes; los demás días del evento se conservan.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', outlined: true },
            accept: () => this.enviarEliminacion(reunion.id)
        });
    }

    private enviarEliminacion(id: number): void {
        this.eliminando.set(true);
        this.errorAccion.set(null);
        this.reunionesServicio
            .eliminarEvento(id)
            .pipe(finalize(() => this.eliminando.set(false)))
            .subscribe({
                next: () => this.router.navigate([RUTA_ASISTENCIA]),
                error: (error: unknown) => this.errorAccion.set(detalleDeError(error, MENSAJE_ERROR_ELIMINAR))
            });
    }

    /** Una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarAsistentes(pagina: number): void {
        this.carga?.unsubscribe();
        this.pagina.set(pagina);
        this.cargandoAsistentes.set(true);
        this.errorAsistentes.set(null);
        this.carga = this.asistenciasServicio
            .asistentes(this.id, this.texto(), pagina, this.tamanoPagina())
            .pipe(finalize(() => this.cargandoAsistentes.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.asistentes.set(content);
                    this.total.set(page.totalElements);
                },
                error: () => this.errorAsistentes.set(MENSAJE_ERROR_ASISTENTES)
            });
    }
}
