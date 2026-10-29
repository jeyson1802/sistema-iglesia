import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '@/environments/environment';
import { Ubigeo } from '@/app/core/models/ubicacion';

/**
 * Fachada HTTP del ubigeo del Perú (catálogo INEI común a todas las iglesias). El catálogo no cambia mientras se usa
 * la app: cada lista se pide una sola vez y se reutiliza.
 */
@Injectable({ providedIn: 'root' })
export class UbigeoService {
    private readonly http = inject(HttpClient);
    private readonly ubigeoUrl = `${environment.apiUrl}/ubigeo`;
    private readonly cache = new Map<string, Observable<Ubigeo[]>>();

    departamentos(): Observable<Ubigeo[]> {
        return this.enCache(`${this.ubigeoUrl}/departamentos`);
    }

    provincias(departamento: string): Observable<Ubigeo[]> {
        return this.enCache(`${this.ubigeoUrl}/departamentos/${departamento}/provincias`);
    }

    distritos(provincia: string): Observable<Ubigeo[]> {
        return this.enCache(`${this.ubigeoUrl}/provincias/${provincia}/distritos`);
    }

    /** Si la petición falla no queda en caché, para que se pueda reintentar. */
    private enCache(url: string): Observable<Ubigeo[]> {
        let lista = this.cache.get(url);
        if (!lista) {
            lista = this.http.get<Ubigeo[]>(url).pipe(shareReplay({ bufferSize: 1, refCount: false }));
            this.cache.set(url, lista);
            lista.subscribe({ error: () => this.cache.delete(url) });
        }
        return lista;
    }
}
