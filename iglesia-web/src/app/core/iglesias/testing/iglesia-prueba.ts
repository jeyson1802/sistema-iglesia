import { APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { Iglesia } from '../models/iglesia';

/** Iglesia completa para pruebas («Sion» en San Juan de Lurigancho, sin contacto ni logo), con los cambios pedidos. */
export function iglesiaPrueba(cambios: Partial<Iglesia> = {}): Iglesia {
    return {
        id: 5,
        nombreComercial: 'Sion',
        denominacion: 'Iglesia Evangélica Sion',
        direccion: 'Av. Los Próceres 123',
        distritoCodigo: '150132',
        ubicacion: { departamento: { codigo: '15', nombre: 'Lima' }, provincia: { codigo: '1501', nombre: 'Lima' }, distrito: { codigo: '150132', nombre: 'San Juan de Lurigancho' } },
        telefono: null,
        correo: null,
        sitioWeb: null,
        activa: true,
        tieneLogo: false,
        logoActualizadoEn: null,
        apariencia: APARIENCIA_SISTEMA,
        creadaEn: new Date(2026, 0, 1),
        ...cambios
    };
}
