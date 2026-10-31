import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MiCodigoQrComponent } from './mi-codigo-qr';

describe('MiCodigoQrComponent', () => {
    let fixture: ComponentFixture<MiCodigoQrComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({});
        fixture = TestBed.createComponent(MiCodigoQrComponent);
        fixture.componentRef.setInput('codigo', { codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' });
        await fixture.whenStable();
        await new Promise((resolve) => setTimeout(resolve, 50));
        await fixture.whenStable();
    });

    it('genera el QR, muestra el código en grupos y permite descargarlo', () => {
        expect(html().querySelector('img')?.getAttribute('src')).toMatch(/^data:image\/png/);
        expect(html().textContent!.replace(/\s+/g, '')).toContain('7K2QM9XDR4TB');
        expect(html().querySelector('a[download]')).not.toBeNull();
    });

    it('lo muestra en pantalla completa', async () => {
        html().querySelector<HTMLButtonElement>('p-button button')!.click();
        await fixture.whenStable();
        expect(document.body.textContent!.replace(/\s+/g, '')).toContain('Micódigodeasistencia');
        expect(document.querySelector('img[alt="Código QR de asistencia en grande"]')).not.toBeNull();
    });
});
