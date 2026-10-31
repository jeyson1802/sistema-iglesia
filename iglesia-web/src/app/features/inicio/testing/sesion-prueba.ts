import { signal } from '@angular/core';
import { Permiso } from '@/app/core/auth/models/permiso';
import { Perfil } from '@/app/core/auth/models/perfil';
import { usuarioCon } from '@/app/core/auth/testing/perfiles-prueba';

/** Sesión de prueba para las páginas de inicio: con el perfil dado y la posibilidad de estar dentro de una iglesia. */
export function sesionPrueba(perfil: Perfil, actuandoEnIglesia = false) {
    const permisos = new Set<Permiso>(perfil.permisos);
    return {
        usuarioActual: signal({ ...usuarioCon(perfil), actuandoEnIglesia }),
        perfilActivo: signal(perfil),
        actuandoEnIglesia: signal(actuandoEnIglesia),
        tienePermiso: (permiso: Permiso) => permisos.has(permiso)
    };
}
