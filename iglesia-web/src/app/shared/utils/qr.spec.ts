import { generarQr } from './qr';

describe('generarQr', () => {
    it('genera la imagen PNG del QR en el navegador', async () => {
        const url = await generarQr('ASIS:7K2QM9XDR4TB', 120);

        expect(url).toMatch(/^data:image\/png;base64,/);
    });
});
