import { crearImagenFotocheck, nombreArchivoFotocheck } from './imagen-fotocheck';

describe('imagen-fotocheck', () => {
    it('crea un PNG del tamaño de una credencial a 300 ppp', async () => {
        const blob = await crearImagenFotocheck({
            nombres: 'Ana María',
            apellidos: 'Torres Ruiz',
            nivel: 'Líder',
            codigo: '7K2QM9XDR4TB',
            contenidoQr: 'ASIS:7K2QM9XDR4TB',
            nombreIglesia: 'Iglesia Vida Nueva',
            urlLogo: null
        });
        const imagen = await createImageBitmap(blob);

        expect(blob.type).toBe('image/png');
        expect(imagen.width).toBe(1011);
        expect(imagen.height).toBe(638);
    });

    it('arma el nombre del archivo sin tildes ni espacios', () => {
        expect(nombreArchivoFotocheck('Ana María', 'Núñez Ruiz')).toBe('fotocheck-ana-maria-nunez-ruiz.png');
    });
});
