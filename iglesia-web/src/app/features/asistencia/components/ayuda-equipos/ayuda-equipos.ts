import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { DialogModule } from 'primeng/dialog';

interface FilaComparacion {
    readonly aspecto: string;
    readonly celular: string;
    readonly lector: string;
}

/** Comparación de la sección «Equipos recomendados» de la spec. */
const COMPARACION: readonly FilaComparacion[] = [
    { aspecto: 'Costo', celular: 'Ninguno: cada servidor usa su celular', lector: 'Aprox. entre 25 y 80 USD por lector (referencial)' },
    { aspecto: 'Velocidad', celular: 'Alrededor de 1 segundo por persona', lector: 'Menos de medio segundo' },
    { aspecto: 'Poca luz', celular: 'Le cuesta más; ayuda la linterna', lector: 'Lee bien, tiene luz propia' },
    { aspecto: 'QR en la pantalla de un celular', celular: 'Sí', lector: 'Sí' },
    { aspecto: 'Varias entradas a la vez', celular: 'Un celular por puerta, sin costo', lector: 'Un lector por puerta' },
    { aspecto: 'Batería', celular: 'La cámara encendida consume bastante', lector: 'El lector USB no usa la del celular' }
];

/** Ayuda «¿Qué equipo uso?»: celular o lector 2D, y cómo organizarse en un culto grande. */
@Component({
    selector: 'app-ayuda-equipos',
    imports: [DialogModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './ayuda-equipos.html'
})
export class AyudaEquiposComponent {
    readonly visible = model(false);

    protected readonly comparacion = COMPARACION;
}
