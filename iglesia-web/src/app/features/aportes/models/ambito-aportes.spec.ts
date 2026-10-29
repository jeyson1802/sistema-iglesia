import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';
import { veAportesDeTodaLaIglesia } from './ambito-aportes';

describe('veAportesDeTodaLaIglesia', () => {
    it('los administradores ven toda la iglesia y los demás solo lo suyo', () => {
        expect(veAportesDeTodaLaIglesia(perfilBase('ADMIN_IGLESIA'))).toBeTrue();
        expect(veAportesDeTodaLaIglesia(perfilBase('ADMIN_SISTEMA'))).toBeTrue();
        expect(veAportesDeTodaLaIglesia(perfilBase('LIDER'))).toBeFalse();
        expect(veAportesDeTodaLaIglesia(null)).toBeFalse();
    });

    it('un rol personalizado sigue a su rol base', () => {
        expect(veAportesDeTodaLaIglesia(perfilPersonalizado(9, 'Tesorería', 'ADMIN_IGLESIA', ['aportes.ver']))).toBeTrue();
    });
});
