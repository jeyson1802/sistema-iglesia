import { PERMISOS } from '@/app/core/auth/models/permiso';
import { PermisoCatalogo, RolAcceso } from '../models/rol-acceso';

/** Roles base como los devuelve `GET /api/roles` (ids y permisos de la semilla de Flyway). */
export const ROLES_BASE: RolAcceso[] = [
    { id: 1, nombre: 'Administrador del sistema', rolBase: 'ADMIN_SISTEMA', base: true, activo: true, permisos: ['usuarios.ver', 'usuarios.administrar', 'roles.ver'], cantidadUsuarios: 1, requiereMiembro: false },
    { id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA', base: true, activo: true, permisos: PERMISOS.filter((permiso) => !permiso.startsWith('iglesias.')), cantidadUsuarios: 2, requiereMiembro: false },
    { id: 3, nombre: 'Líder', rolBase: 'LIDER', base: true, activo: true, permisos: [], cantidadUsuarios: 5, requiereMiembro: true },
    { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', base: true, activo: true, permisos: [], cantidadUsuarios: 40, requiereMiembro: true }
];

export const SECRETARIA: RolAcceso = {
    id: 10,
    nombre: 'Secretaría',
    rolBase: 'ADMIN_IGLESIA',
    base: false,
    activo: true,
    permisos: ['miembros.ver', 'miembros.crear'],
    cantidadUsuarios: 3,
    requiereMiembro: false
};

/** Rol personalizado que hereda de Líder: exige un miembro vinculado, como su rol base. */
export const CONSOLIDADOR: RolAcceso = {
    id: 11,
    nombre: 'Consolidador',
    rolBase: 'LIDER',
    base: false,
    activo: true,
    permisos: [],
    cantidadUsuarios: 0,
    requiereMiembro: true
};

/** Catálogo en el orden de `GET /api/permisos` (módulo y orden). */
export const CATALOGO: PermisoCatalogo[] = [
    { codigo: 'miembros.ver', modulo: 'Miembros', descripcion: 'Ver el listado y el detalle de miembros' },
    { codigo: 'miembros.crear', modulo: 'Miembros', descripcion: 'Registrar miembros' },
    { codigo: 'miembros.editar', modulo: 'Miembros', descripcion: 'Modificar datos de miembros' },
    { codigo: 'miembros.desactivar', modulo: 'Miembros', descripcion: 'Desactivar y reactivar miembros' },
    { codigo: 'roles.ver', modulo: 'Roles', descripcion: 'Ver roles y sus permisos' },
    { codigo: 'roles.administrar', modulo: 'Roles', descripcion: 'Crear, editar, desactivar y reactivar roles personalizados' },
    { codigo: 'usuarios.ver', modulo: 'Usuarios', descripcion: 'Ver usuarios del sistema' },
    { codigo: 'usuarios.administrar', modulo: 'Usuarios', descripcion: 'Crear y editar usuarios y asignarles roles' },
    { codigo: 'iglesias.ver', modulo: 'Iglesias', descripcion: 'Ver el catálogo de iglesias' }
];
