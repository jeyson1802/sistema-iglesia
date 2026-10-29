import { CamaraQr, ErrorCamara } from './camara-qr';

describe('CamaraQr', () => {
    it('sin cámara disponible falla con un motivo que la pantalla sabe explicar', async () => {
        const video = document.createElement('video');
        spyOn(navigator.mediaDevices, 'getUserMedia').and.rejectWith(new DOMException('denegado', 'NotAllowedError'));

        const error = await new CamaraQr().iniciar(video, () => undefined).catch((causa: unknown) => causa);

        expect(error).toEqual(jasmine.any(ErrorCamara));
        expect(['sin-permiso', 'sin-camara']).toContain((error as ErrorCamara).motivo);
    });
});
