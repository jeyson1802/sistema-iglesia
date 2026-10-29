import { Perfil } from '@/app/core/auth/models/perfil';
import { Rol } from '@/app/core/auth/models/rol';

/** Roles base que ven los aportes de toda la iglesia; el resto ve solo los suyos (mismo criterio que `AmbitoAportes`). */
const ROLES_CON_VISTA_COMPLETA: readonly Rol[] = ['ADMIN_IGLESIA', 'ADMIN_SISTEMA'];

export const veAportesDeTodaLaIglesia = (perfil: Perfil | null): boolean => perfil !== null && ROLES_CON_VISTA_COMPLETA.includes(perfil.rolBase);
