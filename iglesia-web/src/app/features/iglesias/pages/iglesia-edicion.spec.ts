import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { FuenteIglesia } from '@/app/core/iglesias/models/fuente-iglesia';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { EditorIglesiaComponent } from '@/app/shared/components/editor-iglesia/editor-iglesia';
import { IglesiaEdicionComponent } from './iglesia-edicion';

describe('IglesiaEdicionComponent', () => {
    let fixture: ComponentFixture<IglesiaEdicionComponent>;
    let iglesias: jasmine.SpyObj<IglesiasService>;

    beforeEach(async () => {
        iglesias = jasmine.createSpyObj<IglesiasService>('IglesiasService', ['fuente']);
        iglesias.fuente.and.returnValue({} as FuenteIglesia);
        await TestBed.configureTestingModule({
            imports: [IglesiaEdicionComponent],
            providers: [provideRouter([]), { provide: IglesiasService, useValue: iglesias }, { provide: ActivatedRoute, useValue: { paramMap: new BehaviorSubject(convertToParamMap({ id: '7' })) } }]
        })
            .overrideComponent(IglesiaEdicionComponent, { remove: { imports: [EditorIglesiaComponent] }, add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] } })
            .compileComponents();
        fixture = TestBed.createComponent(IglesiaEdicionComponent);
        await fixture.whenStable();
    });

    it('edita la iglesia del id de la ruta con el editor compartido', () => {
        expect(iglesias.fuente).toHaveBeenCalledWith(7);
        expect((fixture.nativeElement as HTMLElement).querySelector('app-editor-iglesia')).not.toBeNull();
    });

    it('ofrece volver al catálogo', () => {
        expect((fixture.nativeElement as HTMLElement).querySelector('[aria-label="Volver al catálogo de iglesias"]')).not.toBeNull();
    });
});
