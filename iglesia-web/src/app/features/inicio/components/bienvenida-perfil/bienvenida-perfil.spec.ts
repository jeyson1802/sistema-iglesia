import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BienvenidaPerfilComponent } from './bienvenida-perfil';

describe('BienvenidaPerfilComponent', () => {
    let fixture: ComponentFixture<BienvenidaPerfilComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [BienvenidaPerfilComponent] }).compileComponents();
        fixture = TestBed.createComponent(BienvenidaPerfilComponent);
        fixture.componentRef.setInput('titulo', 'Mi red');
        fixture.componentRef.setInput('descripcion', 'Desde aquí darás seguimiento a tu red.');
        fixture.componentRef.setInput('icono', 'pi pi-sitemap');
        fixture.componentRef.setInput('etiquetaPerfil', 'Líder');
        fixture.componentRef.setInput('correo', 'lider@iglesia.test');
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('muestra el título como encabezado principal', () => {
        expect(html().querySelector('h1')?.textContent).toContain('Mi red');
    });

    it('saluda con el correo del usuario y la descripción del perfil', () => {
        expect(html().textContent).toContain('Hola, lider@iglesia.test.');
        expect(html().textContent).toContain('Desde aquí darás seguimiento a tu red.');
    });

    it('muestra la etiqueta y el ícono del perfil', () => {
        expect(html().querySelector('p-tag')?.textContent).toContain('Líder');
        expect(html().querySelector('i.pi-sitemap')).not.toBeNull();
    });

    it('indica que el contenido está en preparación', () => {
        expect(html().textContent).toContain('Estamos preparando esta sección');
    });
});
