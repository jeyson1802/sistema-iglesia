import { Aporte, ResumenAportes } from '../models/aporte';
import { CategoriaAporte } from '../models/configuracion-aportes';

export const DIEZMO: CategoriaAporte = { id: 1, nombre: 'Diezmo', exigeAportante: true, orden: 1, activo: true };
export const OFRENDA: CategoriaAporte = { id: 2, nombre: 'Ofrenda', exigeAportante: false, orden: 2, activo: true };

export const aporte = (cambios: Partial<Aporte> = {}): Aporte => ({
    id: 45,
    fecha: '2026-10-25',
    miembro: { id: 12, nombre: 'Pérez Soto, Juan', activo: true },
    categoria: { id: 1, nombre: 'Diezmo', activo: true },
    monto: 150.5,
    medio: 'YAPE',
    numeroOperacion: '00123456',
    peticion: 'Por la salud de mi familia',
    observacion: null,
    anulado: false,
    motivoAnulacion: null,
    registradoEn: '2026-10-25T15:30:00Z',
    modificadoEn: null,
    ...cambios
});

export const RESUMEN: ResumenAportes = {
    moneda: 'PEN',
    total: 300,
    cantidad: 4,
    totalPeriodoAnterior: 200,
    porCategoria: [
        { categoria: { id: 1, nombre: 'Diezmo', activo: true }, total: 250, cantidad: 2 },
        { categoria: { id: 2, nombre: 'Ofrenda', activo: true }, total: 50, cantidad: 2 }
    ],
    porMedio: [
        { medio: 'EFECTIVO', total: 50, cantidad: 2 },
        { medio: 'YAPE', total: 250, cantidad: 2 }
    ]
};
