import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { iglesiaPrueba } from '@/app/core/iglesias/testing/iglesia-prueba';
import { MiniaturaIglesiaComponent } from './miniatura-iglesia';

describe('MiniaturaIglesiaComponent', () => {
    let fixture: ComponentFixture<MiniaturaIglesiaComponent>;
    let iglesias: jasmine.SpyObj<IglesiasService>;

    beforeEach(async () => {
        iglesias = jasmine.createSpyObj<IglesiasService>('IglesiasService', ['logo']);
        iglesias.logo.and.returnValue(of(new Blob(['x'], { type: 'image/png' })));
        await TestBed.configureTestingModule({ imports: [MiniaturaIglesiaComponent], providers: [{ provide: IglesiasService, useValue: iglesias }] }).compileComponents();
        fixture = TestBed.createComponent(MiniaturaIglesiaComponent);
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('sin logo muestra el monograma sin pedir la imagen', async () => {
        fixture.componentRef.setInput('iglesia', iglesiaPrueba({ nombreComercial: 'Betel Norte' }));
        await fixture.whenStable();

        expect(html().textContent?.trim()).toBe('BN');
        expect(iglesias.logo).not.toHaveBeenCalled();
    });

    it('con logo pide la imagen de esa iglesia y la muestra', async () => {
        fixture.componentRef.setInput('iglesia', iglesiaPrueba({ id: 8, tieneLogo: true }));
        await fixture.whenStable();

        expect(iglesias.logo).toHaveBeenCalledOnceWith(8);
        expect(html().querySelector('img')?.getAttribute('src')).toMatch(/^blob:/);
    });
});
