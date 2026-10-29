import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OpcionesMiembroService } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { FiltroAportes } from '../../models/aporte';
import { filtroInicial } from '../../models/filtro-url';
import { rangoDe } from '@/app/shared/utils/periodo';
import { DIEZMO, OFRENDA } from '../../testing/aportes-prueba';
import { FiltrosAportesComponent } from './filtros-aportes';

describe('FiltrosAportesComponent', () => {
    let fixture: ComponentFixture<FiltrosAportesComponent>;
    let emitidos: FiltroAportes[];

    const crear = async (filtro: FiltroAportes = filtroInicial(), mostrarMiembro = true): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [FiltrosAportesComponent], providers: [{ provide: OpcionesMiembroService, useValue: { buscar: () => of([]) } }] }).compileComponents();
        fixture = TestBed.createComponent(FiltrosAportesComponent);
        fixture.componentRef.setInput('filtro', filtro);
        fixture.componentRef.setInput('categorias', [DIEZMO, OFRENDA]);
        fixture.componentRef.setInput('mostrarMiembro', mostrarMiembro);
        emitidos = [];
        fixture.componentInstance.cambiar.subscribe((filtro) => emitidos.push(filtro));
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;

    it('un atajo de período emite su rango conservando los demás filtros', async () => {
        await crear({ ...filtroInicial(), medio: 'YAPE' });

        componente()['elegirPeriodo']('ANIO');

        expect(emitidos[0]).toEqual(jasmine.objectContaining({ ...rangoDe('ANIO'), medio: 'YAPE' }));
    });

    it('«Personalizado» muestra el rango y rechaza más de 366 días', async () => {
        await crear();
        componente()['elegirPeriodo']('PERSONALIZADO');
        await fixture.whenStable();
        expect(html().querySelector('#filtro-rango')).not.toBeNull();

        componente()['elegirRango']([new Date(2024, 0, 1), new Date(2026, 0, 1)]);
        await fixture.whenStable();

        expect(emitidos).toEqual([]);
        expect(html().textContent).toContain('Elige un rango de hasta 366 días');

        componente()['elegirRango']([new Date(2026, 8, 1), new Date(2026, 8, 30)]);
        expect(emitidos[0]).toEqual(jasmine.objectContaining({ desde: '2026-09-01', hasta: '2026-09-30' }));
    });

    it('muestra los filtros activos como chips que se pueden quitar uno a uno o limpiar', async () => {
        await crear({ ...filtroInicial(), categoriaId: 1, medio: 'YAPE', incluirAnulados: true });

        expect(html().textContent).toContain('Categoría: Diezmo');
        expect(html().textContent).toContain('Medio: Yape');
        expect(html().textContent).toContain('Con anulados');

        componente()['quitar']({ clave: 'medio', etiqueta: '' });
        expect(emitidos[0].medio).toBeNull();
        expect(emitidos[0].categoriaId).toBe(1);

        componente()['limpiar']();
        expect(emitidos[1]).toEqual(jasmine.objectContaining({ ...rangoDe('MES'), categoriaId: null, medio: null, incluirAnulados: false }));
    });

    it('para el líder oculta el filtro de aportante', async () => {
        await crear(filtroInicial(), false);

        expect(html().querySelector('#filtro-miembro')).toBeNull();
    });
});
