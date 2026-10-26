# SPEC 02 — Roles configurables con permisos granulares y menú por permiso

> **Estado:** Aprobado
> **Depende de:** SPEC 01, SPEC 03 (los endpoints nuevos se documentan con OpenAPI y siguen las reglas de trazabilidad de `CLAUDE.md`)
> **Fecha:** 2026-10-26
> **Objetivo:** Que cada iglesia cree sus propios roles de acceso a partir de un catálogo fijo de permisos por acción, y que el menú, los guards del frontend y la autorización de ambos backends se decidan por permisos en lugar de por los cuatro roles fijos.

## Por qué existe esta spec

En la SPEC 01 todo se decide con el `enum` fijo `ADMIN_SISTEMA`, `ADMIN_IGLESIA`, `LIDER` y `MIEMBRO`: el menú (`EntradaMenu.roles`), los guards (`perfilGuard`), los endpoints (`hasAnyRole(...)`) y la columna `usuario_rol.rol` (con un `CHECK`). Si una iglesia quiere una «Secretaría» que registre miembros pero no administre usuarios, hoy no hay forma de hacerlo.

Esta spec separa tres conceptos que hasta ahora estaban mezclados:

| Concepto | Qué responde | Dónde vive | ¿Configurable? |
|---|---|---|---|
| **Nivel o rango** (pastor, diácono…) | Qué rango tiene el miembro en la iglesia. No da acceso a nada. | `iglesia-api` (spec futura) | Sí, por iglesia |
| **Rol de acceso** (perfil) | Qué permisos tiene el usuario y con qué ámbito (sistema, iglesia, red o propio). | `iglesia-seguridad` | Los 4 roles base son fijos; los roles personalizados se configuran por iglesia |
| **Permiso** (`miembros.crear`) | Qué acción concreta puede hacer. | Código y Flyway | No, porque cada permiso corresponde a código real |

El menú no es un concepto aparte: una opción se muestra si el perfil activo tiene el permiso que esa opción exige.

## Alcance

**Entra:**

- `iglesia-seguridad`: catálogo fijo de permisos con la forma `<modulo>.<accion>`, sembrado por Flyway.
- `iglesia-seguridad`: tabla `rol` con los 4 **roles base**, que son globales, de solo lectura y con permisos fijados por Flyway, y con los **roles personalizados** de cada iglesia.
- `iglesia-seguridad`: un rol personalizado **hereda de un rol base** (`ADMIN_IGLESIA`, `LIDER` o `MIEMBRO`). De él toma la ruta de inicio y el ámbito, y sus permisos deben ser un subconjunto de los del rol base.
- `iglesia-seguridad`: migración de `usuario_rol.rol` (texto) a `usuario_rol.rol_id` (FK a `rol`), sin perder asignaciones.
- `iglesia-seguridad`: API de roles con listar, obtener, crear, editar (nombre y permisos), desactivar y reactivar, más el catálogo de permisos.
- `iglesia-seguridad`: la API de usuarios (`POST`/`PUT /api/usuarios`) recibe `rolIds` en lugar del `enum`, y sus respuestas devuelven roles con id, nombre y rol base. No se crea pantalla en esta spec.
- `iglesia-seguridad`: `GET /api/usuarios/me` devuelve `perfiles` (un perfil por rol activo, cada uno con sus permisos).
- `iglesia-seguridad`: el JWT agrega el claim `permisos` (unión de los permisos de los roles activos). El claim `roles` pasa a contener los **roles base** distintos de los roles activos.
- `iglesia-seguridad`: los endpoints autorizan por permiso (`hasAuthority('usuarios.administrar')`) en lugar de por rol.
- `iglesia-api`: conversor del JWT que mapea `permisos` a autoridades (además de `roles` a `ROLE_*`) y la constante `ClaimsJwt.PERMISOS`.
- `iglesia-web`: modelos `Permiso` y `Perfil`. El perfil activo pasa a ser un rol concreto (base o personalizado), identificado por su id.
- `iglesia-web`: menú y guards por permiso (`permisoGuard`). Las páginas de inicio se protegen por el rol base del perfil (`rolBaseGuard`).
- `iglesia-web`: selector de perfil y topbar con el nombre del rol (personalizado o base).
- `iglesia-web`: nuevo feature `features/roles/` con la pantalla de administración de roles.
- Postman: carpeta «4. Roles y permisos» y actualización de los requests de usuarios.
- Diagrama `diagramas/spec-02-roles-y-permisos-granulares.drawio`.

**Fuera de alcance (para specs futuras):**

- **Pantallas de administración de usuarios** (listar, crear, editar, activar o desactivar usuarios y asignarles roles). Es la spec siguiente y usa la API adaptada aquí.
- Menú almacenado en base de datos (etiquetas, íconos y rutas configurables).
- Editar los permisos de los roles base por iglesia (sobrescrituras).
- Administración de roles por `ADMIN_SISTEMA` en nombre de una iglesia, y cualquier operación entre iglesias.
- Aplicar el ámbito `RED` o `PROPIO` en consultas de negocio de `iglesia-api`, que todavía no tiene endpoints de negocio. Lo hará cada módulo en su spec.
- Revocar sesiones al cambiar los permisos de un rol. El cambio se aplica en el siguiente refresco del token.
- Niveles y rangos configurables de la iglesia, que van en `iglesia-api` y en otra spec.
- Auditoría de quién cambió qué permisos.

## Modelo de datos

### Catálogo de permisos (fijo)

| Código | Módulo | Descripción |
|---|---|---|
| `miembros.ver` | Miembros | Ver el listado y el detalle de miembros |
| `miembros.crear` | Miembros | Registrar miembros |
| `miembros.editar` | Miembros | Modificar datos de miembros |
| `miembros.desactivar` | Miembros | Desactivar y reactivar miembros |
| `usuarios.ver` | Usuarios | Ver usuarios del sistema |
| `usuarios.administrar` | Usuarios | Crear y editar usuarios y asignarles roles |
| `roles.ver` | Roles | Ver roles y sus permisos |
| `roles.administrar` | Roles | Crear, editar, desactivar y reactivar roles personalizados |

### Permisos de los roles base (fijos, por Flyway)

| Rol base | Ámbito | Permisos |
|---|---|---|
| `ADMIN_SISTEMA` | Sistema | `usuarios.ver`, `usuarios.administrar`, `roles.ver` |
| `ADMIN_IGLESIA` | Iglesia | Todos los del catálogo salvo los reservados al sistema (hoy, todos) |
| `LIDER` | Red | Ninguno por ahora. «Miembros» sigue oculto para él hasta su spec, como en la SPEC 01. |
| `MIEMBRO` | Propio | Ninguno. Solo su inicio y su cuenta, que no exigen permiso. |

El ámbito **no se guarda**: se deriva del rol base (`RolBase.ambito()`).

### Migración `iglesia-seguridad/src/main/resources/db/migration/V2__roles_y_permisos.sql`

```sql
CREATE TABLE permiso (
    codigo      VARCHAR(60)  PRIMARY KEY,
    modulo      VARCHAR(40)  NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    orden       INT          NOT NULL
);

-- Roles base (iglesia_id NULL, con código) y roles personalizados por iglesia (con rol base).
CREATE TABLE rol (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id     BIGINT,
    codigo         VARCHAR(30),
    nombre         VARCHAR(60)  NOT NULL,
    rol_base_id    BIGINT REFERENCES rol (id),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL,
    actualizado_en TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_rol_codigo UNIQUE (codigo),
    CONSTRAINT ck_rol_base_o_personalizado CHECK (
        (iglesia_id IS NULL AND codigo IS NOT NULL AND rol_base_id IS NULL)
        OR (iglesia_id IS NOT NULL AND codigo IS NULL AND rol_base_id IS NOT NULL))
);

CREATE UNIQUE INDEX uk_rol_iglesia_nombre ON rol (iglesia_id, lower(nombre)) WHERE iglesia_id IS NOT NULL;
CREATE INDEX ix_rol_iglesia ON rol (iglesia_id);

CREATE TABLE rol_permiso (
    rol_id         BIGINT      NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    permiso_codigo VARCHAR(60) NOT NULL REFERENCES permiso (codigo),
    PRIMARY KEY (rol_id, permiso_codigo)
);

-- Semilla: permisos, los 4 roles base y sus permisos (tablas de arriba).

-- usuario_rol: de texto a FK, conservando las asignaciones existentes.
ALTER TABLE usuario_rol ADD COLUMN rol_id BIGINT REFERENCES rol (id);
UPDATE usuario_rol ur SET rol_id = r.id FROM rol r WHERE r.codigo = ur.rol;
ALTER TABLE usuario_rol ALTER COLUMN rol_id SET NOT NULL;
ALTER TABLE usuario_rol DROP CONSTRAINT usuario_rol_pkey;
ALTER TABLE usuario_rol DROP COLUMN rol;   -- elimina también ck_usuario_rol_valido
ALTER TABLE usuario_rol ADD PRIMARY KEY (usuario_id, rol_id);
CREATE INDEX ix_usuario_rol_rol ON usuario_rol (rol_id);
```

**Reglas que valida el servicio** (no se pueden expresar con restricciones de la base):

- El rol base de un rol personalizado es `ADMIN_IGLESIA`, `LIDER` o `MIEMBRO`, nunca `ADMIN_SISTEMA` ni otro rol personalizado.
- Los permisos de un rol personalizado son un subconjunto de los permisos de su rol base.
- Un rol personalizado solo se asigna a usuarios de su misma iglesia y solo si está activo.
- `LIDER` y `MIEMBRO`, y los roles personalizados que heredan de ellos, exigen un miembro vinculado (regla actual de `requiereMiembro`).
- Un rol no se puede desactivar si deja a algún usuario sin roles activos (409).

### Backend `iglesia-seguridad` (Java)

```java
// usuario/Rol.java → se renombra a rol/RolBase.java (el enum de los 4 roles base)
public enum RolBase {
    ADMIN_SISTEMA(Ambito.SISTEMA), ADMIN_IGLESIA(Ambito.IGLESIA), LIDER(Ambito.RED), MIEMBRO(Ambito.PROPIO);
    public Ambito ambito() { ... }
    public boolean requiereMiembro() { ... }   // LIDER, MIEMBRO
    public boolean heredable() { ... }         // todos menos ADMIN_SISTEMA
}

// rol/Ambito.java
public enum Ambito { SISTEMA, IGLESIA, RED, PROPIO }

// rol/Rol.java (@Entity, tabla rol): id, iglesiaId, codigo, nombre, rolBase (ManyToOne LAZY),
//   activo, permisos (ManyToMany LAZY a Permiso vía rol_permiso), version
//   rolBaseEfectivo(): RolBase   → el suyo si es base, el de rolBase si es personalizado
//   esBase(): boolean

// rol/Permiso.java (@Entity, tabla permiso, solo lectura): codigo, modulo, descripcion, orden

// rol/dto
public record PermisoResponse(String codigo, String modulo, String descripcion) {}
public record RolResponse(Long id, String nombre, RolBase rolBase, boolean base, boolean activo,
        List<String> permisos, long cantidadUsuarios) {}
public record CrearRolRequest(@NotBlank @Size(max = 60) String nombre, @NotNull @Positive Long rolBaseId,
        @NotNull Set<@NotBlank String> permisos) {}
public record ActualizarRolRequest(@NotBlank @Size(max = 60) String nombre, @NotNull Set<@NotBlank String> permisos) {}
public record CambiarEstadoRolRequest(@NotNull Boolean activo) {}

// usuario/dto (cambian)
public record RolAsignadoResponse(Long id, String nombre, RolBase rolBase) {}
public record UsuarioResponse(Long id, String correo, Long iglesiaId, Long miembroId, boolean activo,
        List<RolAsignadoResponse> roles, Instant creadoEn) {}
public record PerfilResponse(Long id, String nombre, RolBase rolBase, List<String> permisos) {}
public record MiUsuarioResponse(Long id, String correo, Long iglesiaId, Long miembroId, List<PerfilResponse> perfiles) {}
// CrearUsuarioRequest y ActualizarUsuarioRequest: `Set<Rol> roles` → `@NotEmpty Set<@NotNull @Positive Long> rolIds`

// common/seguridad/UsuarioAutenticado: roles pasa a Set<RolBase> y se agrega Set<String> permisos

// jwt/ClaimsJwt: + PERMISOS = "permisos"
```

**Contrato HTTP nuevo (`iglesia-seguridad`):**

```text
GET   /api/permisos               roles.ver          → 200 [PermisoResponse] ordenado por módulo y orden
GET   /api/roles                  roles.ver          → 200 [RolResponse] roles base + personalizados de mi iglesia (ADMIN_SISTEMA: solo base)
GET   /api/roles/{id}             roles.ver          → 200 RolResponse | 404 (también si es de otra iglesia)
POST  /api/roles                  roles.administrar  → 201 + Location | 400 (permiso fuera del rol base, rol base inválido) | 409 (nombre repetido)
PUT   /api/roles/{id}             roles.administrar  → 200 | 400 | 403 (rol base) | 404 | 409
PATCH /api/roles/{id}/estado      roles.administrar  → 200 | 403 (rol base) | 404 | 409 (dejaría usuarios sin roles activos)
```

**Contrato HTTP que cambia:**

```text
POST /api/usuarios, PUT /api/usuarios/{id}   body: { ..., rolIds: [1, 7] }   (antes: roles: ["LIDER"])
GET  /api/usuarios[/{id}]                    usuarios.ver
POST/PUT /api/usuarios                       usuarios.administrar
GET  /api/usuarios/me                        → MiUsuarioResponse { ..., perfiles: [{ id, nombre, rolBase, permisos }] }
JWT  roles:    ["ADMIN_IGLESIA"]                       roles base distintos de los roles activos
     permisos: ["miembros.crear", "miembros.ver", …]   unión, ordenada
```

`PoliticaAdministracionUsuarios` sigue decidiendo **sobre quién** se actúa: la misma iglesia, y nunca otorgar `ADMIN_SISTEMA` si no se es `ADMIN_SISTEMA`. Ahora usa el rol base efectivo de los roles asignados. **Qué acción** se puede hacer lo decide el permiso.

### Backend `iglesia-api`

```java
// common/seguridad/ClaimsJwt: + PERMISOS = "permisos"
// common/seguridad/ConvertidorAutoridadesJwt implements Converter<Jwt, AbstractAuthenticationToken>
//   roles → ROLE_<rol>, permisos → <permiso> (sin prefijo)
```

### Frontend (`iglesia-web/src/app/core/auth/models/`)

```ts
// permiso.ts
export const PERMISOS = [
    'miembros.ver', 'miembros.crear', 'miembros.editar', 'miembros.desactivar',
    'usuarios.ver', 'usuarios.administrar', 'roles.ver', 'roles.administrar'
] as const;
export type Permiso = (typeof PERMISOS)[number];
export const esPermiso = (valor: string): valor is Permiso => (PERMISOS as readonly string[]).includes(valor);

// perfil.ts
export interface Perfil {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: Rol;                 // rol.ts se conserva: ROLES, ETIQUETA_ROL, ICONO_ROL, RUTA_INICIO_POR_ROL
    readonly permisos: readonly Permiso[]; // el mapper descarta los códigos desconocidos
}

// usuario-actual.ts: `roles: readonly Rol[]` → `perfiles: readonly Perfil[]`
```

Cambios en `SesionService`:

```ts
private readonly perfil = signal<Perfil | null>(null);
readonly perfilActivo = this.perfil.asReadonly();
readonly permisosActivos = computed(() => new Set(this.perfil()?.permisos ?? []));
tienePermiso(permiso: Permiso): boolean;
seleccionarPerfil(id: number): void;
rutaInicio(): string; // RUTA_INICIO_POR_ROL[perfil.rolBase]
```

- `sessionStorage['iglesia.perfilActivo']` guarda el **id** del rol. Los valores antiguos (`'ADMIN_IGLESIA'`) no coinciden con ningún id y se descartan.
- Los perfiles se ordenan por la jerarquía de su rol base (`ROLES`) y luego por nombre.

Guards (`core/auth/guards/auth.guards.ts`):

- `perfilGuard(...roles)` se renombra a `rolBaseGuard(...roles)`. Compara con `perfilActivo().rolBase` y se usa en las páginas de inicio.
- `permisoGuard(permiso)` es nuevo. Si el perfil activo no tiene el permiso, redirige a `/auth/acceso-denegado`.

Menú (`app.menu.ts`): `EntradaMenu.roles` se reemplaza por `EntradaMenu.permiso?: Permiso`. Sin `permiso`, la entrada la ven todos. `construirMenu(perfil, produccion)` recibe el `Perfil`.

```ts
const ENTRADAS_APP: EntradaMenu[] = [
    { label: 'Iglesia', items: [{ label: 'Miembros', icon: 'pi pi-fw pi-users', routerLink: ['/miembros'], permiso: 'miembros.ver' }] },
    { label: 'Administración', items: [{ label: 'Roles', icon: 'pi pi-fw pi-shield', routerLink: ['/roles'], permiso: 'roles.ver' }] }
];
```

### Frontend: feature `features/roles/`

```text
features/roles/
├── roles.routes.ts                       '' → RolesListaComponent (permisoGuard('roles.ver'))
├── models/rol-acceso.ts                  RolAcceso, PermisoCatalogo, GuardarRol (+ responses de la API)
├── services/roles.service.ts             fachada: listar, obtener, crear, actualizar, cambiarEstado, permisos
├── services/rol-acceso.mapper.ts
├── pages/roles-lista.{ts,html,spec.ts}   contenedor (smart)
└── components/
    ├── tabla-roles/{ts,html,spec.ts}     presentacional: p-table
    └── formulario-rol/{ts,html,spec.ts}  presentacional: p-dialog con formulario reactivo tipado
```

- **Lista:** `p-table` con nombre, rol base (`ETIQUETA_ROL`), tipo (`p-tag` «Base» o «Personalizado»), estado (`p-tag` «Activo» o «Inactivo»), cantidad de usuarios y número de permisos. Incluye estados de carga (skeleton), vacío y error con reintento. La acción principal «Nuevo rol» solo aparece con `roles.administrar`.
- **Roles base:** se abren en modo solo lectura («Ver permisos»), sin editar ni desactivar.
- **Formulario:** `nombre` (`required`, `maxLength(60)`), `rolBaseId` (`p-select` con `ADMIN_IGLESIA`, `LIDER` y `MIEMBRO`, solo al crear) y permisos con un `p-checkbox` por permiso, agrupados por módulo. Solo se habilitan los permisos del rol base elegido; al cambiar el rol base se descartan los que ya no apliquen. Debe haber al menos un permiso marcado. Los errores 400 y 409 del backend (`detail`) se muestran en el diálogo.
- **Desactivar:** pide confirmación (`p-confirmdialog`) que indica cuántos usuarios tienen el rol. Si el backend responde 409, se muestra su mensaje.
- Ruta `/roles` registrada en `app.routes.ts` como hija de `AppLayout`.

## Plan de implementación

Cada paso deja el sistema compilando y con sus pruebas en verde.

**Backend (`iglesia-seguridad`)**

1. Crear `V2__roles_y_permisos.sql`: tablas, semilla de permisos y roles base, y migración de `usuario_rol`. Aplicar con `./gradlew bootRun` contra la base local y verificar con el MCP de PostgreSQL que las asignaciones previas siguen intactas (mismo número de filas y mismos roles).
2. Renombrar `usuario/Rol` a `rol/RolBase` con `ambito()`, `requiereMiembro()` y `heredable()`, y crear `Ambito`. Crear las entidades `Permiso` y `Rol` y sus repositorios (con `@EntityGraph` para los permisos, sin N+1). Cambiar `Usuario.roles` a `Set<Rol>` mediante `usuario_rol` y adaptar `Usuario.validar` al rol base efectivo. Pruebas unitarias de `RolBase` y de `Usuario`.
3. Adaptar `GeneradorTokenAcceso`: `roles` con los roles base distintos de los roles activos y `permisos` con la unión ordenada. Adaptar `UsuarioAutenticado` (`Set<RolBase>` y permisos) y el conversor de autoridades del resource server de seguridad (`permisos` sin prefijo). Prueba de integración: los claims del login de un usuario con un rol personalizado.
4. Cambiar `UsuarioController` a `hasAuthority(...)`. `CrearUsuarioRequest` y `ActualizarUsuarioRequest` pasan a `rolIds`. `UsuarioService` valida que los roles existan, estén activos, sean de la misma iglesia o sean base, y que no otorguen `ADMIN_SISTEMA` sin serlo. `UsuarioResponse` usa `RolAsignadoResponse` y `/me` devuelve `MiUsuarioResponse` con `perfiles`. Actualizar las pruebas de integración de usuarios y la política.
5. Crear el paquete `rol/`: `RolService`, `PoliticaRoles` (subconjunto del rol base, roles base inmutables, misma iglesia, desactivación que no deja usuarios sin roles), `RolMapper`, `RolController` y `PermisoController` con `ProblemDetail` para 400, 403, 404 y 409. Pruebas unitarias de `PoliticaRoles` y de integración de cada endpoint, incluido el aislamiento entre iglesias (404 al pedir un rol de otra iglesia).
6. Postman: crear la carpeta «4. Roles y permisos» (listar permisos, listar roles, obtener, crear, editar, desactivar, reactivar) con descripción, cuerpo de ejemplo y pruebas, más un script que guarde `{{rolId}}`. Actualizar los bodies de «Crear usuario» y «Actualizar usuario» a `rolIds` y la prueba de «Obtener mi usuario» a `perfiles`. «Infraestructura» pasa a ser la carpeta 5.

**Backend (`iglesia-api`)**

7. Agregar `ClaimsJwt.PERMISOS` y `ConvertidorAutoridadesJwt`, y registrarlo en `SeguridadHttpConfig`. Prueba unitaria del conversor (roles a `ROLE_*` y permisos sin prefijo).

**Frontend (`iglesia-web`)**

8. Crear `permiso.ts` y `perfil.ts`. Adaptar `usuario-response.ts`, `usuario-actual.ts` y `usuario-actual.mapper.ts` (`perfiles`, descartando permisos desconocidos). Specs.
9. Adaptar `SesionService`: perfil activo como `Perfil`, `permisosActivos`, `tienePermiso`, `seleccionarPerfil(id)`, persistencia por id y `rutaInicio` por `rolBase`. Spec, incluido el descarte del valor antiguo de `sessionStorage`.
10. En `auth.guards.ts`, renombrar `perfilGuard` a `rolBaseGuard` y crear `permisoGuard`. Usar `rolBaseGuard` en `inicio.routes.ts` y `permisoGuard('miembros.ver')` en la ruta `miembros` de `app.routes.ts`. Specs.
11. Adaptar `seleccionar-perfil` (una tarjeta por `Perfil`, con su nombre como título, `ICONO_ROL[rolBase]` y `ETIQUETA_ROL[rolBase]` como subtítulo en los personalizados) y la topbar (nombre del perfil activo). Specs.
12. Adaptar `app.menu.ts` (`permiso` en lugar de `roles` y nuevo grupo «Administración» con «Roles»). Spec.
13. Crear `features/roles/`: modelos, mapper, `RolesService`, `TablaRolesComponent`, `FormularioRolComponent` y `RolesListaComponent`, y registrar `/roles` en `app.routes.ts`. Specs de cada pieza.

**Cierre**

14. Crear `diagramas/spec-02-roles-y-permisos-granulares.drawio` con varias páginas:
    - Conceptos (nivel, rol y permiso; ámbito).
    - Modelo de datos (`permiso`, `rol`, `rol_permiso`, `usuario_rol`).
    - Flujo de login a JWT y de `/me` al menú.
    - Flujo de administración de roles (componentes, endpoints, clases y tablas).

    Con colores, leyenda y notas sobre las decisiones.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Después de aplicar `V2`, cada fila previa de `usuario_rol` apunta al rol base equivalente y no se pierde ninguna asignación.
- [ ] La tabla `rol` contiene exactamente 4 roles base con `iglesia_id` nulo y los permisos de la tabla «Permisos de los roles base».
- [ ] El login de un `ADMIN_IGLESIA` emite `roles: ["ADMIN_IGLESIA"]` y `permisos` con los 8 permisos.
- [ ] Un usuario con solo un rol personalizado «Secretaría» (base `ADMIN_IGLESIA`, permisos `miembros.ver` y `miembros.crear`) recibe `roles: ["ADMIN_IGLESIA"]` y `permisos: ["miembros.crear", "miembros.ver"]`.
- [ ] Ese usuario recibe 403 en `GET /api/usuarios` y en `POST /api/roles`.
- [ ] `POST /api/roles` con un permiso que no tiene el rol base responde 400.
- [ ] `POST /api/roles` con rol base `ADMIN_SISTEMA` o con un rol personalizado como base responde 400.
- [ ] `POST /api/roles` con un nombre ya usado en la misma iglesia (sin distinguir mayúsculas) responde 409. El mismo nombre en otra iglesia se acepta.
- [ ] `PUT` y `PATCH .../estado` sobre un rol base responden 403.
- [ ] `GET /api/roles/{id}` de un rol personalizado de otra iglesia responde 404 y `GET /api/roles` no lo lista.
- [ ] Desactivar un rol que es el único rol activo de algún usuario responde 409. Si no lo es, responde 200, y en el siguiente login o refresco ese rol ya no aporta permisos ni aparece en `perfiles`.
- [ ] `PUT /api/usuarios/{id}` con un rol de otra iglesia o con un rol inactivo responde 400. Con `ADMIN_SISTEMA`, si quien actúa es `ADMIN_IGLESIA`, responde 403.
- [ ] Asignar un rol personalizado con base `LIDER` a un usuario sin miembro vinculado responde 400.
- [ ] `GET /api/usuarios/me` devuelve `perfiles` con id, nombre, rol base y permisos de cada rol activo.
- [ ] La colección de Postman tiene la carpeta «4. Roles y permisos» y los requests de usuarios usan `rolIds`. Todas sus pruebas pasan contra el entorno local.
- [ ] Los endpoints de roles y permisos, y los de usuarios modificados, están documentados en OpenAPI (`@Tag`, `@Operation`, `@ApiResponse` por código, `@Schema` con ejemplo), y la prueba de cobertura de `/v3/api-docs` de la SPEC 03 pasa.
- [ ] Crear, editar, desactivar y reactivar un rol registra `INFO` con el id del rol, `iglesiaId` y el `usuarioId` de quien actúa.
- [ ] `./gradlew test` pasa en `iglesia-seguridad`.

**Backend `iglesia-api`**

- [ ] Un JWT con `roles: ["LIDER"]` y `permisos: ["miembros.ver"]` produce las autoridades `ROLE_LIDER` y `miembros.ver`.
- [ ] `./gradlew test` pasa en `iglesia-api`.

**Frontend**

- [ ] Con los 4 roles base, el comportamiento visible es el de la SPEC 01: mismas páginas de inicio, `ADMIN_IGLESIA` ve «Miembros» y `LIDER` y `MIEMBRO` no lo ven.
- [ ] `ADMIN_IGLESIA` ve «Administración › Roles». `LIDER` y `MIEMBRO` no lo ven y, si navegan a `/roles` a mano, llegan a `/auth/acceso-denegado`.
- [ ] Un usuario con «Secretaría» y `MIEMBRO` ve dos tarjetas en el selector de perfil («Secretaría» con el subtítulo «Administrador de iglesia» y «Miembro»). Con «Secretaría» llega a `/administracion` y ve «Miembros» pero no «Roles».
- [ ] La topbar muestra el nombre del perfil activo («Secretaría»).
- [ ] Al recargar (F5) se conserva el perfil activo por id. Un valor antiguo (`'ADMIN_IGLESIA'`) en `sessionStorage` se descarta sin errores.
- [ ] En `/roles`, los roles base aparecen con la etiqueta «Base», solo con la opción «Ver permisos» y sin acciones de edición.
- [ ] Al crear un rol, solo se pueden marcar los permisos del rol base elegido. Sin nombre o sin permisos, el botón «Guardar» queda deshabilitado y se muestra el mensaje junto al campo.
- [ ] Un nombre repetido muestra en el diálogo el mensaje 409 del backend.
- [ ] Desactivar pide confirmación con la cantidad de usuarios. Un 409 muestra el mensaje del backend y el rol sigue activo.
- [ ] La pantalla de roles muestra estados de carga, vacío y error, y funciona en móvil y en modo oscuro.
- [ ] Cada pieza nueva o modificada tiene su `.spec.ts` y `ng test` pasa.
- [ ] No hay cambios en `src/app/layout/` fuera de `app.menu.ts` y `app.topbar.ts` (más sus `.html` y `.spec.ts`).

**Documentación**

- [ ] Existe `diagramas/spec-02-roles-y-permisos-granulares.drawio` con las páginas descritas en el paso 14.

## Decisiones

- **Sí:** separar **nivel** (rango de negocio, en `iglesia-api`), **rol de acceso** (paquete de permisos con ámbito) y **permiso** (acción concreta). Mezclarlos era la causa de la confusión entre «roles de negocio» y «roles de menú».
- **Sí:** catálogo de permisos fijo, sembrado por Flyway. Cada permiso corresponde a código (endpoint y pantalla); un permiso creado desde la web no haría nada.
- **No:** menú en base de datos. Una opción nueva necesita una pantalla y un despliegue de todas formas; lo dinámico es **quién** la ve, y eso ya lo resuelven los permisos. Evita duplicar rutas entre el frontend y la base de datos.
- **Sí:** permisos por acción (`<modulo>.<accion>`). Permite «puede ver pero no editar». El menú exige el `.ver` de su módulo y los botones exigen su acción.
- **Sí:** los 4 roles actuales pasan a ser roles base globales y de solo lectura. Así no se rompen `RUTA_INICIO_POR_ROL`, el selector de perfil ni las reglas de `ADMIN_SISTEMA`, y una iglesia no puede quitarse a sí misma el acceso a la administración.
- **No:** permitir que cada iglesia edite los permisos de los roles base. Requiere tablas de sobrescritura y abre la puerta a dejar la iglesia sin administrador.
- **Sí:** los roles personalizados son por iglesia (`iglesia_id`), coherente con «todo catálogo configurable es por iglesia».
- **Sí:** un rol personalizado hereda de un rol base: toma de él la ruta de inicio y el ámbito, y sus permisos son un subconjunto de los del rol base. Evita la escalada de privilegios y no exige páginas de inicio nuevas.
- **Sí:** el ámbito (`SISTEMA`, `IGLESIA`, `RED` o `PROPIO`) se deriva del rol base y no se guarda. Separarlo del permiso evita multiplicar permisos como `miembros.ver_red` o `miembros.ver_todos`.
- **Sí:** desactivar en lugar de borrar los roles personalizados, igual que lo decidido para los catálogos en la SPEC 01. No se puede desactivar un rol que deje a algún usuario sin roles activos (409), para no crear usuarios que inician sesión sin ningún perfil.
- **Sí:** el claim `roles` conserva los códigos base (para el ámbito y por compatibilidad) y el nuevo claim `permisos` lleva la unión. **La autorización se hace siempre por permiso, nunca por rol**: un rol personalizado con base `ADMIN_IGLESIA` lleva `ROLE_ADMIN_IGLESIA` en el token sin tener todos sus permisos.
- **Sí:** permisos dentro del JWT. `iglesia-api` autoriza sin llamar a `iglesia-seguridad`. Un cambio de permisos se aplica en el siguiente refresco, que llega pronto porque el token de acceso dura poco.
- **No:** revocar sesiones al cambiar los permisos de un rol. El retraso máximo es la duración del token de acceso y se acepta.
- **Sí:** el perfil activo pasa a ser un rol concreto (base o personalizado) identificado por su id, y el menú y los guards usan **los permisos de ese perfil**, no la unión de todos. Se mantiene el criterio de la SPEC 01: el perfil limita la navegación y el backend autoriza con todos los permisos del token.
- **Sí:** adaptar la API de usuarios a `rolIds` en esta spec, sin pantalla, para poder asignar y probar roles personalizados con Postman. La pantalla va en la spec siguiente.
- **Sí:** administran los roles quienes tengan `roles.administrar` en su iglesia. `ADMIN_SISTEMA` solo ve los roles base (`roles.ver`) para poder asignarlos por la API.
- **No:** `ADMIN_SISTEMA` administrando roles de una iglesia. Requiere operar entre iglesias, que sigue fuera de alcance.
- **Sí:** `LIDER` y `MIEMBRO` sin permisos por ahora. Se conserva exactamente el comportamiento de la SPEC 01; cada módulo les dará permisos en su spec.
- **Sí:** el feature `features/roles/` está en la ruta `/roles` y no bajo `/administracion`, que ya es la página de inicio de `ADMIN_IGLESIA`.
- **Sí:** esta spec lleva el número 02. La «spec 02 Identidad de la iglesia» que menciona la SPEC 01 tomará el siguiente número libre cuando se escriba, y se corregirá esa referencia.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| La migración de `usuario_rol` pierde o duplica asignaciones. | La `UPDATE` cruza por `codigo` antes del `NOT NULL`: si quedara alguna fila sin rol, `SET NOT NULL` falla y Flyway revierte la migración. Se verifica con el MCP que el conteo de filas sea el mismo antes y después. |
| Algún endpoint sigue autorizando por rol (`hasRole`) y un rol personalizado con base `ADMIN_IGLESIA` obtiene más acceso del que tiene. | La decisión es autorizar siempre por permiso. Hay una prueba de integración con «Secretaría» que exige 403 en usuarios y roles. Al cerrar, se busca `hasRole`/`hasAnyRole` en ambos backends. |
| El JWT crece con muchos permisos. | Hoy son 8. Si el catálogo supera unas decenas, una spec futura puede emitir los permisos solo del perfil activo o compactarlos. |
| Un permiso del frontend (`PERMISOS`) no coincide con el catálogo del backend. | El mapper descarta los códigos desconocidos y las pruebas fijan la lista. Cualquier permiso nuevo se agrega en Flyway y en `permiso.ts` dentro de la misma spec. |
| Un rol se desactiva mientras su usuario tiene la sesión abierta. | Sus permisos se mantienen hasta el siguiente refresco (duración corta del token). Se acepta y queda documentado. |
| El cambio de tipo del perfil activo (de `Rol` a `Perfil`) rompe piezas de la SPEC 01. | Se hace en pasos separados (8 a 12), cada uno con sus specs en verde, y un criterio de aceptación exige el mismo comportamiento visible con los roles base. |

## Qué **no** entra en esta spec

- Pantallas de administración de usuarios (spec siguiente).
- Menú almacenado en base de datos.
- Permisos editables de los roles base.
- Operación de `ADMIN_SISTEMA` sobre los roles de una iglesia.
- Aplicación del ámbito `RED` o `PROPIO` en consultas de negocio.
- Revocación de sesiones al cambiar permisos.
- Niveles y rangos de la iglesia.
- Auditoría de cambios de permisos.

Cada uno, si llega, va en su propia spec.
