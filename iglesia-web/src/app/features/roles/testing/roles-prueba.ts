import { PERMISOS } from '@/app/core/auth/models/permiso';
import { PermisoCatalogo, RolAcceso } from '../models/rol-acceso';

/** Roles base como los devuelve `GET /api/roles` (ids y permisos de la semilla de Flyway). */
export const ROLES_BASE: RolAcceso[] = [
    { id: 1, nombre: 'Administrador del sistema', rolBase: 'ADMIN_SISTEMA', base: true, activo: true, permisos: ['usuarios.ver', 'usuarios.administrar', 'roles.ver'], cantidadUsuarios: 1 },
    { id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA', base: true, activo: true, permisos: [...PERMISOS], cantidadUsuarios: 2 },
    { id: 3, nombre: 'Líder', rolBase: 'LIDER', base: true, activo: true, permisos: [], cantidadUsuarios: 5 },
    { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', base: true, activo: true, permisos: [], cantidadUsuarios: 40 }
];

export const SECRETARIA: RolAcceso = {
    id: 10,
    nombre: 'Secretaría',
    rolBase: 'ADMIN_IGLESIA',
    base: false,
    activo: true,
    permisos: ['miembros.ver', 'miembros.crear'],
    cantidadUsuarios: 3
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
    { codigo: 'usuarios.administrar', modulo: 'Usuarios', descripcion: 'Crear y editar usuarios y asignarles roles' }
];
