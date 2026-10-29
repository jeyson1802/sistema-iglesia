import { formatearCodigoAsistencia } from './codigo-asistencia';
import { generarQr } from './qr';

/** Píxeles por milímetro a 300 ppp: la imagen se puede imprimir a tamaño real (85,6 × 54 mm). */
const PX_POR_MM = 300 / 25.4;
const ANCHO_MM = 85.6;
const ALTO_MM = 54;
const QR_MM = 29;
const FRANJA_MM = 2.5;
const MARGEN_MM = 4;
const NEGRO = '#000000';
const BLANCO = '#ffffff';
const FUENTE_RESPALDO = 'serif';

/** Lo que va en la imagen: el miembro, su código y la identidad de la iglesia. */
export interface ContenidoImagenFotocheck {
    readonly nombres: string;
    readonly apellidos: string;
    readonly nivel: string | null;
    readonly codigo: string;
    readonly contenidoQr: string;
    readonly nombreIglesia: string;
    readonly urlLogo: string | null;
}

const mm = (valor: number): number => Math.round(valor * PX_POR_MM);

/**
 * Dibuja el fotocheck en un PNG del tamaño de una credencial a 300 ppp, con el mismo diseño que el componente
 * `app-fotocheck`: franja con el color primario, logo y nombre de la iglesia con su tipografía, el miembro y el QR en
 * negro sobre blanco con el código impreso debajo.
 */
export async function crearImagenFotocheck(contenido: ContenidoImagenFotocheck, documento: Document = document): Promise<Blob> {
    const lienzo = documento.createElement('canvas');
    lienzo.width = mm(ANCHO_MM);
    lienzo.height = mm(ALTO_MM);
    const ctx = lienzo.getContext('2d');
    if (!ctx) {
        throw new Error('El navegador no permite dibujar la imagen');
    }
    const estilos = getComputedStyle(documento.documentElement);
    const primario = estilos.getPropertyValue('--p-primary-color').trim() || NEGRO;
    const fuenteMarca = estilos.getPropertyValue('--fuente-marca').trim() || FUENTE_RESPALDO;
    const fuenteTexto = estilos.fontFamily || FUENTE_RESPALDO;

    ctx.fillStyle = BLANCO;
    ctx.fillRect(0, 0, lienzo.width, lienzo.height);
    ctx.fillStyle = primario;
    ctx.fillRect(0, 0, lienzo.width, mm(FRANJA_MM));

    const qr = await cargarImagen(await generarQr(contenido.contenidoQr), documento);
    const xQr = mm(ANCHO_MM - MARGEN_MM - QR_MM);
    ctx.drawImage(qr, xQr, mm(5), mm(QR_MM), mm(QR_MM));
    ctx.fillStyle = NEGRO;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'alphabetic';
    ctx.font = `${mm(2.8)}px monospace`;
    ctx.fillText(formatearCodigoAsistencia(contenido.codigo), xQr + mm(QR_MM) / 2, mm(38));

    ctx.textAlign = 'left';
    const anchoTexto = xQr - mm(MARGEN_MM + 2);
    let xNombre = mm(MARGEN_MM);
    if (contenido.urlLogo) {
        const logo = await cargarImagen(contenido.urlLogo, documento).catch(() => null);
        if (logo) {
            const alto = mm(9);
            const ancho = Math.min((logo.width / logo.height) * alto, mm(18));
            ctx.drawImage(logo, xNombre, mm(5), ancho, alto);
            xNombre += ancho + mm(2);
        }
    }
    ctx.font = `${mm(3.6)}px ${fuenteMarca}`;
    escribir(ctx, contenido.nombreIglesia, xNombre, mm(8.5), anchoTexto - (xNombre - mm(MARGEN_MM)), mm(4.2), 2);

    ctx.font = `bold ${mm(4.2)}px ${fuenteTexto}`;
    let y = escribir(ctx, contenido.nombres, mm(MARGEN_MM), mm(30), anchoTexto, mm(4.8), 2);
    ctx.font = `600 ${mm(3.6)}px ${fuenteTexto}`;
    y = escribir(ctx, contenido.apellidos, mm(MARGEN_MM), y, anchoTexto, mm(4.2), 2);
    if (contenido.nivel) {
        ctx.font = `${mm(3)}px ${fuenteTexto}`;
        escribir(ctx, contenido.nivel.toUpperCase(), mm(MARGEN_MM), y + mm(1), anchoTexto, mm(3.6), 1);
    }

    return new Promise((resolver, rechazar) => lienzo.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error('No se pudo crear la imagen'))), 'image/png'));
}

/** Nombre del archivo: `fotocheck-<nombres>-<apellidos>.png`, sin tildes ni espacios. */
export function nombreArchivoFotocheck(nombres: string, apellidos: string): string {
    const limpio = `${nombres}-${apellidos}`
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
    return `fotocheck-${limpio}.png`;
}

function cargarImagen(url: string, documento: Document): Promise<HTMLImageElement> {
    return new Promise((resolver, rechazar) => {
        const imagen = documento.createElement('img');
        imagen.onload = () => resolver(imagen);
        imagen.onerror = () => rechazar(new Error('No se pudo cargar la imagen'));
        imagen.src = url;
    });
}

/** Escribe el texto en hasta `lineasMax` líneas del ancho dado (la última se recorta con «…») y devuelve la `y` siguiente. */
function escribir(ctx: CanvasRenderingContext2D, texto: string, x: number, y: number, ancho: number, interlineado: number, lineasMax: number): number {
    const lineas: string[] = [];
    let actual = '';
    for (const palabra of texto.split(/\s+/)) {
        const candidata = actual ? `${actual} ${palabra}` : palabra;
        if (ctx.measureText(candidata).width <= ancho || !actual) {
            actual = candidata;
        } else {
            lineas.push(actual);
            actual = palabra;
        }
    }
    lineas.push(actual);
    const visibles = lineas.slice(0, lineasMax);
    if (lineas.length > lineasMax) {
        let ultima = visibles[lineasMax - 1];
        while (ultima.length > 1 && ctx.measureText(`${ultima}…`).width > ancho) {
            ultima = ultima.slice(0, -1);
        }
        visibles[lineasMax - 1] = `${ultima}…`;
    }
    visibles.forEach((linea, indice) => ctx.fillText(linea, x, y + indice * interlineado));
    return y + visibles.length * interlineado;
}
