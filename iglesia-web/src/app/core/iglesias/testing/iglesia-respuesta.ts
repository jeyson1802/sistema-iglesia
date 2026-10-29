import { APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { IglesiaResponse } from '../models/iglesia';

/** `IglesiaResponse` tal como la envía iglesia-api, para pruebas de servicios HTTP. */
export function iglesiaRespuesta(cambios: Partial<IglesiaResponse> = {}): IglesiaResponse {
    return {
        id: 3,
        nombreComercial: 'Iglesia Central',
        denominacion: 'Iglesia Evangélica Central',
        direccion: 'Av. Lima 1',
        distritoCodigo: '150132',
        ubicacion: null,
        telefono: null,
        correo: null,
        sitioWeb: null,
        activa: true,
        tieneLogo: false,
        logoActualizadoEn: null,
        apariencia: APARIENCIA_SISTEMA,
        creadaEn: '2026-10-28T15:30:00Z',
        ...cambios
    };
}
