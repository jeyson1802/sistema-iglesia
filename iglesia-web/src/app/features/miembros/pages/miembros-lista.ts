import { Component, inject, OnInit, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { Miembro } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';

@Component({
    selector: 'app-miembros-lista',
    standalone: true,
    imports: [TableModule, TagModule, MessageModule],
    template: `
        <div class="card">
            <div class="font-semibold text-xl mb-4">Miembros</div>

            @if (error()) {
                <p-message severity="error" styleClass="mb-4">{{ error() }}</p-message>
            }

            <p-table [value]="miembros()" [loading]="cargando()" [paginator]="true" [rows]="10" [rowHover]="true" dataKey="id">
                <ng-template #header>
                    <tr>
                        <th>Nombres</th>
                        <th>Apellidos</th>
                        <th>Teléfono</th>
                        <th>Email</th>
                        <th>Estado</th>
                    </tr>
                </ng-template>
                <ng-template #body let-miembro>
                    <tr>
                        <td>{{ miembro.nombres }}</td>
                        <td>{{ miembro.apellidos }}</td>
                        <td>{{ miembro.telefono }}</td>
                        <td>{{ miembro.email }}</td>
                        <td>
                            <p-tag [value]="miembro.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'" [severity]="miembro.estado === 'ACTIVO' ? 'success' : 'secondary'" />
                        </td>
                    </tr>
                </ng-template>
                <ng-template #emptymessage>
                    <tr>
                        <td colspan="5">No hay miembros registrados.</td>
                    </tr>
                </ng-template>
            </p-table>
        </div>
    `
})
export class MiembrosLista implements OnInit {
    private miembrosService = inject(MiembrosService);

    miembros = signal<Miembro[]>([]);
    cargando = signal(false);
    error = signal<string | null>(null);

    ngOnInit() {
        this.cargando.set(true);
        this.miembrosService.listar().subscribe({
            next: (miembros) => {
                this.miembros.set(miembros);
                this.cargando.set(false);
            },
            error: () => {
                this.error.set('No se pudo cargar la lista de miembros.');
                this.cargando.set(false);
            }
        });
    }
}
