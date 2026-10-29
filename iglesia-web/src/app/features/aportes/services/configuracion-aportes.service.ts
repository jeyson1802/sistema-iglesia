import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '@/environments/environment';
import { ConfiguracionAportes, Moneda, MONEDA_POR_DEFECTO } from '../models/configuracion-aportes';

/**
 * Moneda de los aportes de la iglesia (`/api/aportes/configuracion`). La guarda como signal para que todas las
 * pantallas formateen los montos igual; se carga una vez y se actualiza al guardar.
 */
@Injectable({ providedIn: 'root' })
export class ConfiguracionAportesService {
    private readonly http = inject(HttpClient);
    private readonly configuracionUrl = `${environment.apiUrl}/aportes/configuracion`;
    private readonly monedasUrl = `${environment.apiUrl}/aportes/monedas`;

    private readonly estado = signal<ConfiguracionAportes>({ moneda: MONEDA_POR_DEFECTO, monedaEditable: true });
    private cargada = false;

    readonly configuracion = this.estado.asReadonly();

    /** Carga la configuración la primera vez; si falla, se queda con la moneda por defecto y se reintenta luego. */
    asegurarCargada(): void {
        if (!this.cargada) {
            this.recargar().subscribe({ error: () => undefined });
        }
    }

    recargar(): Observable<ConfiguracionAportes> {
        return this.http.get<ConfiguracionAportes>(this.configuracionUrl).pipe(tap((configuracion) => this.fijar(configuracion)));
    }

    guardar(moneda: string): Observable<ConfiguracionAportes> {
        return this.http.put<ConfiguracionAportes>(this.configuracionUrl, { moneda }).pipe(tap((configuracion) => this.fijar(configuracion)));
    }

    monedas(): Observable<Moneda[]> {
        return this.http.get<Moneda[]>(this.monedasUrl);
    }

    /** Tras registrar o anular aportes cambia si la moneda se puede editar. */
    marcarConAportes(): void {
        this.estado.update((actual) => ({ ...actual, monedaEditable: false }));
    }

    private fijar(configuracion: ConfiguracionAportes): void {
        this.cargada = true;
        this.estado.set(configuracion);
    }
}
