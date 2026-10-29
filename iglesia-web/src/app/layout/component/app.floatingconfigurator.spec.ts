import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PreferenciaModoOscuroService } from '@/app/core/apariencia/services/preferencia-modo-oscuro.service';
import { AppFloatingConfigurator } from './app.floatingconfigurator';

describe('AppFloatingConfigurator', () => {
    let fixture: ComponentFixture<AppFloatingConfigurator>;
    let alternar: jasmine.Spy;

    beforeEach(async () => {
        alternar = jasmine.createSpy('alternar');
        await TestBed.configureTestingModule({
            imports: [AppFloatingConfigurator],
            providers: [{ provide: PreferenciaModoOscuroService, useValue: { oscuro: signal(false), alternar } }]
        }).compileComponents();
        fixture = TestBed.createComponent(AppFloatingConfigurator);
        await fixture.whenStable();
    });

    it('solo ofrece el modo oscuro, sin el configurador de colores', () => {
        const html = fixture.nativeElement as HTMLElement;

        expect(html.querySelector('.pi-palette')).toBeNull();
        html.querySelector<HTMLButtonElement>('button[aria-label="Usar modo oscuro"]')!.click();

        expect(alternar).toHaveBeenCalledTimes(1);
    });
});
