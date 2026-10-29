import { describirUbicacion } from './ubicacion';

describe('describirUbicacion', () => {
    it('une distrito, provincia y departamento', () => {
        const ubicacion = { departamento: { codigo: '15', nombre: 'Lima' }, provincia: { codigo: '1501', nombre: 'Lima' }, distrito: { codigo: '150135', nombre: 'San Martín de Porres' } };

        expect(describirUbicacion(ubicacion)).toBe('San Martín de Porres, Lima, Lima');
    });

    it('sin ubicación devuelve vacío', () => {
        expect(describirUbicacion(null)).toBe('');
    });
});
