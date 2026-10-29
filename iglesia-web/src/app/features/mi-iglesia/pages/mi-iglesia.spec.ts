import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MiIglesiaService } from '@/app/core/iglesias/services/mi-iglesia.service';
import { EditorIglesiaComponent } from '@/app/shared/components/editor-iglesia/editor-iglesia';
import { MiIglesiaComponent } from './mi-iglesia';

describe('MiIglesiaComponent', () => {
    let fixture: ComponentFixture<MiIglesiaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [MiIglesiaComponent], providers: [{ provide: MiIglesiaService, useValue: {} }] })
            .overrideComponent(MiIglesiaComponent, { remove: { imports: [EditorIglesiaComponent] }, add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] } })
            .compileComponents();
        fixture = TestBed.createComponent(MiIglesiaComponent);
        await fixture.whenStable();
    });

    it('tiene un solo título y edita la iglesia del token con el editor compartido', () => {
        const html = fixture.nativeElement as HTMLElement;

        expect(html.querySelectorAll('h1').length).toBe(1);
        expect(html.querySelector('h1')?.textContent).toContain('Mi iglesia');
        expect(html.querySelector('app-editor-iglesia')).not.toBeNull();
    });
});
