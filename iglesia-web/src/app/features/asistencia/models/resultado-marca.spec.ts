import { MARCA_ANA } from '../testing/asistencia-prueba';
import { horaDe, resultadoDeError, resultadoDeMarca } from './resultado-marca';

describe('resultado-marca', () => {
    it('una marca nueva es verde con el nombre y el nivel', () => {
        expect(resultadoDeMarca(MARCA_ANA)).toEqual({ tipo: 'marcado', titulo: 'Ana Torres', detalle: 'Marcado · Líder' });
        expect(resultadoDeMarca({ ...MARCA_ANA, nivel: null }).detalle).toBe('Marcado');
    });

    it('una marca repetida es ámbar con la hora en que se registró', () => {
        const resultado = resultadoDeMarca({ ...MARCA_ANA, yaRegistrado: true });

        expect(resultado.tipo).toBe('repetido');
        expect(resultado.detalle).toBe(`Ya estaba registrado a las ${horaDe(MARCA_ANA.registradaEn)}`);
    });

    it('un error es rojo con el motivo', () => {
        expect(resultadoDeError('El miembro está inactivo')).toEqual({ tipo: 'error', titulo: 'El miembro está inactivo', detalle: null });
    });
});
