import { formatearCodigoAsistencia, leerCodigoAsistencia } from './codigo-asistencia';

describe('codigo-asistencia', () => {
    it('lee el código con o sin prefijo, en minúsculas y con separadores', () => {
        for (const leido of ['ASIS:7K2QM9XDR4TB', '7K2QM9XDR4TB', 'asis:7k2qm9xdr4tb', ' 7K2Q M9XD R4TB ', '7K2Q-M9XD-R4TB']) {
            expect(leerCodigoAsistencia(leido)).withContext(leido).toBe('7K2QM9XDR4TB');
        }
    });

    it('descarta lo que no es un código de asistencia', () => {
        for (const leido of ['https://pago.example/qr', '7K2QM9XDR4T', '7K2QM9XDR4TBX', '7K2QM9XDR4TI', '7K2QM9XDR4TU', '']) {
            expect(leerCodigoAsistencia(leido)).withContext(leido).toBeNull();
        }
    });

    it('formatea en grupos de cuatro', () => {
        expect(formatearCodigoAsistencia('7K2QM9XDR4TB')).toBe('7K2Q M9XD R4TB');
    });
});
