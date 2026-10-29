import { ESPERA_MISMO_CODIGO_MS, ProcesadorCodigos } from './procesador-codigos';

describe('ProcesadorCodigos', () => {
    let procesador: ProcesadorCodigos;

    beforeEach(() => (procesador = new ProcesadorCodigos()));

    it('envía el código normalizado', () => {
        expect(procesador.procesar('asis:7k2qm9xdr4tb', 0)).toEqual({ tipo: 'enviar', codigo: '7K2QM9XDR4TB' });
    });

    it('ignora el mismo código leído varias veces seguidas y lo acepta pasada la espera', () => {
        procesador.procesar('ASIS:7K2QM9XDR4TB', 0);

        expect(procesador.procesar('ASIS:7K2QM9XDR4TB', 500).tipo).toBe('repetida');
        expect(procesador.procesar('ASIS:7K2QM9XDR4TB', ESPERA_MISMO_CODIGO_MS).tipo).toBe('enviar');
    });

    it('otro código se envía de inmediato', () => {
        procesador.procesar('ASIS:7K2QM9XDR4TB', 0);

        expect(procesador.procesar('ASIS:Z9Y8X7W6V5T4', 100).tipo).toBe('enviar');
    });

    it('avisa lo que no es un código y, tras olvidar, acepta la relectura', () => {
        expect(procesador.procesar('https://pago.example', 0).tipo).toBe('invalida');
        procesador.procesar('ASIS:7K2QM9XDR4TB', 0);
        procesador.olvidar();
        expect(procesador.procesar('ASIS:7K2QM9XDR4TB', 10).tipo).toBe('enviar');
    });
});
