# SPEC 04 — Administración de usuarios

> **Estado:** Implementado
> **Depende de:** SPEC 01, SPEC 02 (API de usuarios con `rolIds` y permisos `usuarios.*`), SPEC 03 (OpenAPI y trazabilidad)
> **Fecha:** 2026-10-26
> **Objetivo:** Que el administrador de una iglesia liste, cree y edite los usuarios de su iglesia (correo, roles y estado), les restablezca la contraseña desde la interfaz y nunca pueda dejar la iglesia sin administrador, y que todo usuario con una contraseña temporal esté obligado a cambiarla antes de usar el sistema.

## Por qué existe esta spec

La SPEC 02 dejó la API de usuarios lista (`rolIds`, permisos `usuarios.ver` y `usuarios.administrar`) pero sin pantalla. Hoy, para dar acceso a una persona hay que usar Swagger o Postman.

Un usuario del sistema puede llegar a ser miembro de la iglesia, pero `iglesia-api` todavía no tiene el módulo de miembros. Por eso esta spec administra usuarios **sin** vincularlos a un miembro. Los roles que exigen miembro (`LIDER`, `MIEMBRO` y los personalizados que heredan de ellos) no se asignan desde aquí: se asignarán cuando exista el vínculo con el miembro.

Hoy el administrador elige la contraseña del usuario nuevo y nada obliga a cambiarla, así que el administrador la conoce para siempre. Esta spec la convierte en **temporal**: el usuario debe cambiarla en su primer inicio de sesión, y lo mismo ocurre después de cada restablecimiento. La regla se aplica en los dos backends, no solo en la navegación del frontend.

Hoy una iglesia puede quedarse sin nadie que administre sus usuarios: basta con desactivar al último administrador o desactivar el rol personalizado que le daba el permiso. Esta spec lo impide en todos los caminos.

Esta spec es la primera de tres:

- **SPEC 04 (esta):** usuarios de una iglesia.
- **SPEC 05:** acceso del administrador del sistema a todas las iglesias, eligiendo una a la vez.
- **SPEC 06:** menú administrable en base de datos y su asignación a los roles de cada iglesia.

## Alcance

**Entra:**

- `iglesia-seguridad`: columna `usuario.debe_cambiar_contrasena`. Se marca al crear un usuario y al restablecer su contraseña, y se desmarca cuando el propio usuario la cambia.
- `iglesia-seguridad`: claim `cambio_contrasena` en el JWT. Mientras sea `true`, solo se permiten `GET /api/usuarios/me` y `PUT /api/usuarios/me/contrasena`; todo lo demás responde 403.
- `iglesia-api`: la misma regla. Con `cambio_contrasena: true`, todos sus endpoints protegidos responden 403.
- `iglesia-seguridad`: endpoint `PUT /api/usuarios/{id}/contrasena` para restablecer la contraseña de otro usuario con una contraseña temporal. Revoca sus tokens de refresco.
- `iglesia-seguridad`: filtros `correo`, `activo` y `rolId` en `GET /api/usuarios`.
- `iglesia-seguridad`: el correo se puede cambiar con `PUT /api/usuarios/{id}`. Responde 409 si ya está en uso, y revoca los tokens de refresco del usuario.
- `iglesia-seguridad`: una iglesia nunca se queda sin un usuario activo con un rol activo que tenga `usuarios.administrar`. Responde 409 en estos casos:
  - desactivar un usuario o cambiarle los roles (`PUT /api/usuarios/{id}`);
  - desactivar un rol personalizado (`PATCH /api/roles/{id}/estado`);
  - quitarle `usuarios.administrar` a un rol personalizado (`PUT /api/roles/{id}`).
- `iglesia-seguridad`: `UsuarioResponse` y `MiUsuarioResponse` agregan `debeCambiarContrasena`. `RolResponse` agrega `requiereMiembro`.
- `iglesia-seguridad`: `GET /api/roles` se permite con `roles.ver` **o** `usuarios.administrar`, para que quien asigna roles pueda ver cuáles existen.
- `iglesia-web`: pantalla `/usuarios` (feature `features/usuarios/`) con:
  - tabla paginada desde el servidor y filtros por correo, estado y rol;
  - diálogo para crear y editar (roles y estado);
  - diálogo para restablecer la contraseña.
- `iglesia-web`: opción «Administración › Usuarios» en el menú, con el permiso `usuarios.ver`.
- `iglesia-web`: guard que lleva a `/cuenta/contrasena` a quien tiene el cambio pendiente, y un aviso en esa pantalla.
- Postman: carpeta «3. Administración de usuarios» actualizada.
- OpenAPI de todo endpoint nuevo o modificado.
- Diagrama `diagramas/spec-04-administracion-de-usuarios.drawio`.

**Se conserva sin cambios (ya existe desde la SPEC 01 o la 02):**

- Un administrador no puede cambiar sus propios roles ni su estado (400).
- Desactivar a un usuario o cambiarle los roles revoca sus tokens de refresco.
- Un `ADMIN_IGLESIA` solo ve y administra los usuarios de su iglesia, y no puede otorgar `ADMIN_SISTEMA`.

**Fuera de alcance (para specs futuras):**

- Vincular un usuario con un miembro (selector de miembro) y asignar desde la interfaz los roles que exigen miembro. Va con el módulo de miembros de `iglesia-api`.
- **SPEC 05 — Acceso global del administrador del sistema:**
  - catálogo de iglesias;
  - el administrador del sistema elige una iglesia y opera dentro de ella, con Hibernate y la RLS activos para esa iglesia;
  - administrar usuarios, roles y datos de cualquier iglesia, incluido crear su primer administrador;
  - endpoints explícitos para reportes entre iglesias.
- **SPEC 06 — Menú administrable:**
  - opciones de menú guardadas en base de datos, sobre un catálogo de las pantallas que existen en el código;
  - cada iglesia asigna las opciones a sus roles.

  Reabre la decisión «No: menú en base de datos» de la SPEC 02.
- Recuperación de contraseña por correo («olvidé mi contraseña»).
- Invalidar los tokens de acceso ya emitidos (siguen válidos hasta que caducan).
- Borrar usuarios.

## Modelo de datos

### Migración `iglesia-seguridad/src/main/resources/db/migration/V3__cambio_contrasena_obligatorio.sql`

```sql
ALTER TABLE usuario
    ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE;
```

Los usuarios existentes quedan con `FALSE`: su contraseña no se considera temporal.

### Ciclo de vida de `debe_cambiar_contrasena`

| Evento | Valor |
| --- | --- |
| `POST /api/usuarios` (un administrador crea el usuario) | `TRUE` |
| `PUT /api/usuarios/{id}/contrasena` (un administrador la restablece) | `TRUE` |
| `PUT /api/usuarios/me/contrasena` (el propio usuario la cambia) | `FALSE` |
| `AdministradorInicial` (variables `SEGURIDAD_ADMIN_*`) | `FALSE` |

### Backend `iglesia-seguridad` (Java)

- `usuario/entity/Usuario`:
  - campo `debeCambiarContrasena`;
  - nuevo `cambiarCorreo(correoNormalizado)`, que devuelve si cambió;
  - `Usuario.nuevo(...)` recibe si la contraseña es temporal;
  - `cambiarContrasena(hash)` lo pone en `false`;
  - nuevo `restablecerContrasena(hash)` lo pone en `true`.
- `jwt/constant/ClaimsJwt.CAMBIO_CONTRASENA = "cambio_contrasena"`. `GeneradorTokenAcceso` lo emite siempre, como booleano.
- `common/seguridad/filter/FiltroCambioContrasenaPendiente` (`OncePerRequestFilter`, no `@Component`):
  - se registra en la cadena de seguridad después de `BearerTokenAuthenticationFilter`;
  - si el JWT trae `cambio_contrasena: true` y la ruta no está en la lista permitida, responde 403 con el `ProblemDetail` de `AccessDeniedProblemDetail`;
  - el detalle es «Debes cambiar tu contraseña temporal antes de continuar».
- `common/seguridad/constant/RutasCambioContrasena.PERMITIDAS`: `GET /api/usuarios/me` y `PUT /api/usuarios/me/contrasena`. Las rutas públicas (`/api/auth/**`, JWKS, documentación) no pasan por la regla porque no llevan token.
- `usuario/dto/RestablecerContrasenaRequest(String contrasenaTemporal)`: `@NotBlank` y `@Size(min = LimitesUsuario.CONTRASENA_MIN, max = LimitesUsuario.CONTRASENA_MAX)`.
- `usuario/dto/FiltroUsuarios(String correo, Boolean activo, Long rolId)`: `record` de los parámetros de consulta. Todos son opcionales.
- `usuario/repository/EspecificacionesUsuario`: `Specification<Usuario>` por iglesia, por correo (contiene, sin distinguir mayúsculas), por estado y por rol asignado. `UsuarioRepository` extiende `JpaSpecificationExecutor<Usuario>`. Todos los valores van como parámetros enlazados.
- `usuario/dto/ActualizarUsuarioRequest` agrega `String correo` (`@NotBlank`, `@Email` y `@Size(max = LimitesUsuario.CORREO_MAX)`). Se normaliza con `Correo.normalizar`.
- `rol/service/GarantiaAdministradorIglesia.verificarQueQuedaAdministrador(iglesiaId)`:
  - se llama al final de la transacción que cambia usuarios o roles, después de `flush`;
  - si el conteo es 0, lanza `ConflictoException` con el detalle «La iglesia debe conservar al menos un usuario activo que pueda administrar usuarios» y la transacción se revierte;
  - antes de contar toma `pg_advisory_xact_lock(iglesia_id)`, para que dos administradores no se desactiven entre sí a la vez.
- `rol/repository/RolRepository.contarAdministradoresActivos(iglesiaId, permiso)`: consulta nativa con parámetros enlazados sobre `usuario`, `usuario_rol`, `rol` y `rol_permiso`. Cuenta los usuarios activos de la iglesia que tienen un rol activo con el permiso.
- `UsuarioResponse` y `MiUsuarioResponse` agregan `boolean debeCambiarContrasena`.
- `rol/dto/RolResponse` agrega `boolean requiereMiembro`, calculado con `RolBase.requiereMiembro()` del rol base efectivo.
- `common/seguridad/constant/Permisos.PUEDE_VER_ROLES_O_ASIGNARLOS = "hasAnyAuthority('roles.ver', 'usuarios.administrar')"`, que se usa solo en `GET /api/roles`.

**Contrato HTTP nuevo (`iglesia-seguridad`):**

| Método y ruta | Permiso | Cuerpo | Respuestas |
| --- | --- | --- | --- |
| `PUT /api/usuarios/{id}/contrasena` | `usuarios.administrar` | `{ "contrasenaTemporal": "Temporal123" }` | 204; 400 (validación o el propio usuario); 403 (otra iglesia o `ADMIN_SISTEMA`); 404 |

**Contrato HTTP que cambia:**

| Método y ruta | Cambio |
| --- | --- |
| `GET /api/usuarios` | Nuevos parámetros opcionales `correo`, `activo` y `rolId`. Se combinan con `iglesiaId` y la paginación existentes. |
| `PUT /api/usuarios/{id}` | El cuerpo agrega `correo`, que es obligatorio. Responde 409 si el correo es de otro usuario. Responde 409 si deja la iglesia sin administrador. |
| `PUT /api/roles/{id}`, `PATCH /api/roles/{id}/estado` | Responden 409 si dejan la iglesia sin administrador. |
| `GET /api/usuarios`, `GET /api/usuarios/{id}`, `POST`, `PUT` | `UsuarioResponse` agrega `debeCambiarContrasena`. |
| `GET /api/usuarios/me` | Agrega `debeCambiarContrasena`. |
| `GET /api/roles`, `GET /api/roles/{id}` | `RolResponse` agrega `requiereMiembro`. `GET /api/roles` acepta `roles.ver` o `usuarios.administrar`. |
| Cualquier endpoint protegido | Con `cambio_contrasena: true` en el JWT, responde 403, salvo las dos rutas permitidas. |

Logs (`INFO`, solo con identificadores):

- `Contraseña restablecida usuarioId={} por usuarioId={}; sesiones revocadas`.
- `Usuario creado id={} por usuarioId={}`, que ya existe.
- `Correo cambiado usuarioId={} por usuarioId={}; sesiones revocadas`. No incluye ningún correo.

El rechazo por cambio pendiente se registra como `WARN` en el manejador común, igual que cualquier 403.

### Backend `iglesia-api`

- `common/seguridad/constant/ClaimsJwt.CAMBIO_CONTRASENA`.
- `common/seguridad/filter/FiltroCambioContrasenaPendiente`, con la misma regla que en seguridad pero **sin** rutas permitidas. Se registra en `SeguridadHttpConfig` después de `BearerTokenAuthenticationFilter`.

### Frontend (`iglesia-web/src/app/core/auth/`)

- `models/usuario-response.ts` y `models/usuario-actual.ts` agregan `debeCambiarContrasena: boolean`, y `usuario-actual.mapper.ts` lo copia.
- `SesionService.debeCambiarContrasena`: `computed` a partir de `usuarioActual`.
- `guards/auth.guards.ts`, nuevo `contrasenaVigenteGuard` (`CanActivateChildFn`):
  - con el cambio pendiente, cualquier hija del layout distinta de `/cuenta/contrasena` redirige ahí;
  - se registra como `canActivateChild` en la ruta del layout de `app.routes.ts`.
- `pages/cambiar-contrasena`: con el cambio pendiente, muestra un `p-message` de aviso: «Tu contraseña es temporal. Cámbiala para continuar.». Al guardar sigue igual que ahora: cierra la sesión y vuelve al login.

### Frontend: feature `features/usuarios/`

```ts
// models/usuario-admin.ts
interface UsuarioAdmin {
    readonly id: number;
    readonly correo: string;
    readonly miembroId: number | null;
    readonly activo: boolean;
    readonly debeCambiarContrasena: boolean;
    readonly roles: readonly RolAsignado[]; // { id, nombre, rolBase }
    readonly creadoEn: Date;
}

interface FiltroUsuarios {
    readonly correo: string;
    readonly activo: boolean | null; // null = todos
    readonly rolId: number | null;
}

interface CrearUsuario { readonly correo: string; readonly contrasenaTemporal: string; readonly rolIds: readonly number[] }
interface EditarUsuario { readonly correo: string; readonly rolIds: readonly number[]; readonly activo: boolean }

const PAGINA_USUARIOS = 10;
```

| Pieza | Responsabilidad |
| --- | --- |
| `services/usuarios.service.ts` | Fachada HTTP:<br>- `listar(filtro, pagina)` → `Page<UsuarioAdmin>`, ordenado por `correo,asc`;<br>- `crear` envía la `iglesiaId` del usuario autenticado y `miembroId` nulo;<br>- `actualizar(id, cambios, miembroIdActual)` reenvía el `miembroId` sin cambios;<br>- `restablecerContrasena(id, contrasenaTemporal)`. |
| `services/usuario-admin.mapper.ts` | Respuesta de la API → `UsuarioAdmin`. |
| `components/filtros-usuarios` | Presentacional:<br>- búsqueda por correo con espera de 300 ms;<br>- `p-select` de estado (Todos, Activos, Inactivos) y de rol;<br>- emite `FiltroUsuarios`. |
| `components/tabla-usuarios` | Presentacional:<br>- `p-table` con carga perezosa (`lazy`) y paginación;<br>- columnas Correo, Roles (etiquetas), Estado (Activo o Inactivo, más «Debe cambiar contraseña»), Creado y Acciones;<br>- esqueleto mientras carga y mensaje vacío;<br>- la fila propia muestra «Tú» y no tiene acciones. |
| `components/formulario-usuario` | Diálogo `p-dialog` para crear y editar (detalle abajo). |
| `components/restablecer-contrasena` | Diálogo con la contraseña temporal (`p-password`, `autocomplete="new-password"`) y un texto que explica que el usuario deberá cambiarla y que se cerrarán sus sesiones. |
| `pages/usuarios-lista` | Contenedor:<br>- carga roles y la primera página;<br>- abre los diálogos y guarda;<br>- muestra los 400, 404 y 409 (incluido «sin administrador») en el diálogo, y un toast de éxito;<br>- estado de error con «Reintentar». |
| `usuarios.routes.ts` | `permisoGuard('usuarios.ver')`. |

**Formulario de usuario:**

- **Al crear:**
  - Correo: `required`, `email` y máximo 254 caracteres.
  - Contraseña temporal: `required`, de 8 a 72 caracteres.
  - Roles: al menos uno.
- **Al editar:**
  - El correo es editable, con las mismas validaciones que al crear. No hay campo de contraseña.
  - Si el correo cambia, al guardar se pide confirmación porque se cerrarán las sesiones del usuario y deberá entrar con el correo nuevo.
  - Roles: al menos uno.
  - Activo: `p-toggleswitch`. Si se pasa de activo a inactivo, al guardar se pide confirmación porque se cerrarán sus sesiones.
- **Roles que se ofrecen:** los activos con `requiereMiembro = false` y rol base distinto de `ADMIN_SISTEMA`.
- **Roles bloqueados al editar:** los que el usuario ya tiene con `requiereMiembro = true` se muestran marcados y deshabilitados, con el aviso «Se gestiona al vincular el usuario con un miembro». Se reenvían sin cambios y cuentan para «al menos uno».
- **Acciones visibles:** «Nuevo usuario», editar y restablecer solo aparecen con `usuarios.administrar`. Con solo `usuarios.ver`, la tabla es de lectura.

La pantalla `/roles` no cambia: su página ya muestra dentro del diálogo, o en un toast al desactivar, el `detail` de los 409 del backend, que ahora incluye «sin administrador».

## Plan de implementación

Cada paso deja el sistema compilando y con sus pruebas en verde.

**Backend (`iglesia-seguridad`)**

1. Crear `V3__cambio_contrasena_obligatorio.sql` y aplicarla con `./gradlew bootRun`. Comprobar con el MCP que los usuarios existentes quedaron en `FALSE`. Agregar el campo a `Usuario` (`nuevo`, `cambiarContrasena` y `restablecerContrasena`). `UsuarioService.crear` lo marca y `AdministradorInicial` no. Pruebas unitarias de `Usuario`.
2. Emitir `cambio_contrasena` en `GeneradorTokenAcceso`. Crear `RutasCambioContrasena` y `FiltroCambioContrasenaPendiente`, y registrarlo en `SeguridadHttpConfig`. Pruebas de integración: con el cambio pendiente, `/me` y `/me/contrasena` funcionan, `GET /api/usuarios` responde 403 con `traceId`, y después de cambiar la contraseña el nuevo login emite `false`.
3. Agregar `debeCambiarContrasena` a `UsuarioResponse` y `MiUsuarioResponse`, y `requiereMiembro` a `RolResponse`. Aplicar `PUEDE_VER_ROLES_O_ASIGNARLOS` en `GET /api/roles`. Actualizar las pruebas de integración y el OpenAPI de esos DTOs.
4. Filtros de `GET /api/usuarios`: `FiltroUsuarios`, `EspecificacionesUsuario` y `JpaSpecificationExecutor`. Documentar los parámetros con `@Parameter`. Pruebas de integración de cada filtro, de su combinación y del aislamiento: un `ADMIN_IGLESIA` no ve usuarios de otra iglesia aunque envíe `iglesiaId`.
5. Endpoint `PUT /api/usuarios/{id}/contrasena`:
   - `RestablecerContrasenaRequest`, `UsuarioService.restablecerContrasena` (política existente, 400 sobre uno mismo, marca el cambio pendiente, revoca los tokens de refresco y registra el log `INFO`);
   - OpenAPI con 204, 400, 401, 403 y 404;
   - pruebas de integración, incluida una que verifica con `OutputCaptureExtension` que el log no incluye la contraseña.
6. Correo editable:
   - `correo` en `ActualizarUsuarioRequest` y `Usuario.cambiarCorreo`;
   - 409 si el correo es de otro usuario;
   - si cambió, revocar los tokens de refresco y registrar el log `INFO` sin correos;
   - OpenAPI y pruebas de integración, incluida una que verifica con `OutputCaptureExtension` que el log no incluye correos.
7. `GarantiaAdministradorIglesia` y `RolRepository.contarAdministradoresActivos`. Llamarlos en `UsuarioService.actualizar`, `RolService.actualizar` y `RolService.cambiarEstado`, y documentar el 409 en el OpenAPI. Pruebas:
   - unitarias del conteo;
   - de integración de los tres caminos: el 409 deja la base sin cambios, y el cambio se acepta si queda otro administrador.
8. Postman:
   - en «3. Administración de usuarios», agregar «Listar usuarios con filtros» y «Restablecer contraseña», agregar `correo` al cuerpo de «Actualizar usuario», probar `debeCambiarContrasena: true` en «Crear usuario» y probar el campo nuevo en «Obtener mi usuario»;
   - en «4. Roles y permisos», documentar el 409 «sin administrador» en las descripciones de «Actualizar rol» y «Desactivar rol».

   Ejecutar la colección completa con newman.

**Backend (`iglesia-api`)**

9. Agregar `ClaimsJwt.CAMBIO_CONTRASENA` y `FiltroCambioContrasenaPendiente`, y registrarlo en `SeguridadHttpConfig`. Prueba: con `cambio_contrasena: true`, un endpoint protegido responde 403 con `ProblemDetail` y `traceId`; con `false`, responde normalmente.

**Frontend (`iglesia-web`)**

10. `core/auth`:
   - `debeCambiarContrasena` en los modelos, en el mapper y en `SesionService`;
   - `contrasenaVigenteGuard` registrado como `canActivateChild` en el layout;
   - aviso en `cambiar-contrasena`;
   - specs del guard, de la página y casos nuevos en `app.routes.spec.ts`.
11. `features/usuarios/`: modelos, `usuario-admin.mapper.ts` y `UsuariosService`, con sus specs.
12. `FiltrosUsuariosComponent` y `TablaUsuariosComponent`, con sus specs (carga, vacío, paginación perezosa, fila propia sin acciones, acciones solo con `usuarios.administrar`).
13. `FormularioUsuarioComponent`, con su spec: validaciones de crear y editar, correo editable, roles ofrecidos, roles bloqueados que se reenvían, y confirmación al desactivar o al cambiar el correo.
14. `RestablecerContrasenaComponent`, con su spec.
15. `UsuariosListaComponent` y `usuarios.routes.ts`. Registrar `/usuarios` en `app.routes.ts` y agregar «Administración › Usuarios» (`pi-users`, `usuarios.ver`) en `app.menu.ts`, antes de «Roles». Specs de la página, de las rutas y del menú.

**Cierre**

16. Crear `diagramas/spec-04-administracion-de-usuarios.drawio` con varias páginas, colores con leyenda y notas:
    - arquitectura de la pantalla `/usuarios` (componentes, servicio, endpoints, clases y tablas);
    - flujo de crear un usuario y su primer inicio de sesión con el cambio obligatorio;
    - flujo de restablecer la contraseña;
    - dónde se aplica `cambio_contrasena` en cada backend y en el frontend;
    - la garantía de administrador y los tres caminos que protege.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Después de aplicar `V3`, todos los usuarios existentes tienen `debe_cambiar_contrasena = FALSE`.
- [ ] `POST /api/usuarios` crea el usuario con `debeCambiarContrasena: true`, y su login emite `cambio_contrasena: true`.
- [ ] Con ese token, `GET /api/usuarios/me` responde 200 y `PUT /api/usuarios/me/contrasena` responde 204.
- [ ] Con ese token, `GET /api/usuarios` y `GET /api/roles` responden 403, con `ProblemDetail`, `traceId` y el detalle «Debes cambiar tu contraseña temporal antes de continuar».
- [ ] Después de cambiar la contraseña, el siguiente login emite `cambio_contrasena: false` y `/me` devuelve `debeCambiarContrasena: false`.
- [ ] El administrador inicial (`SEGURIDAD_ADMIN_*`) se crea con `debeCambiarContrasena: false`.
- [ ] `PUT /api/usuarios/{id}/contrasena` responde 204, marca `debeCambiarContrasena: true` y deja revocados todos los tokens de refresco del usuario: `POST /api/auth/refrescar` con su cookie anterior responde 401.
- [ ] `PUT /api/usuarios/{id}/contrasena` sobre el propio usuario responde 400.
- [ ] `PUT /api/usuarios/{id}/contrasena` sobre un usuario de otra iglesia responde 403 o 404 (según la política existente) y no cambia nada.
- [ ] `PUT /api/usuarios/{id}/contrasena` con una contraseña de menos de 8 caracteres responde 400.
- [ ] `GET /api/usuarios?correo=PRUEBA` devuelve los usuarios cuyo correo contiene «prueba», sin distinguir mayúsculas.
- [ ] `GET /api/usuarios?activo=false` devuelve solo los inactivos, y `?rolId=N` solo los que tienen ese rol.
- [ ] Un `ADMIN_IGLESIA` que envía `iglesiaId` de otra iglesia sigue viendo solo la suya.
- [ ] Un usuario con un rol personalizado que tiene `usuarios.administrar` pero no `roles.ver` recibe 200 en `GET /api/roles` y 403 en `POST /api/roles`.
- [ ] `RolResponse.requiereMiembro` es `true` para `LIDER`, `MIEMBRO` y sus derivados, y `false` para los demás.
- [ ] `PUT /api/usuarios/{id}` con un correo nuevo responde 200, devuelve el correo normalizado en minúsculas y revoca los tokens de refresco del usuario. El usuario entra con el correo nuevo, y con el anterior recibe 401.
- [ ] `PUT /api/usuarios/{id}` con el correo de otro usuario (sin distinguir mayúsculas) responde 409 y no cambia nada.
- [ ] En una iglesia con un solo administrador activo, desactivarlo o quitarle su único rol con `usuarios.administrar` responde 409 con el detalle «La iglesia debe conservar al menos un usuario activo que pueda administrar usuarios».
- [ ] Si el único administrador lo es por un rol personalizado, desactivar ese rol o quitarle `usuarios.administrar` responde 409 y el rol queda como estaba.
- [ ] Con dos administradores activos, desactivar a uno responde 200.
- [ ] Ninguna línea de log contiene una contraseña ni un correo.

**Backend `iglesia-api`**

- [ ] Con `cambio_contrasena: true`, un endpoint protegido responde 403 con `ProblemDetail` y `traceId`.

**Frontend**

- [ ] «Administración › Usuarios» aparece con `usuarios.ver` y no aparece sin él. `/usuarios` sin ese permiso lleva a `/auth/acceso-denegado`.
- [ ] La tabla carga 10 usuarios por página desde el servidor, y cambiar de página pide la página siguiente al backend.
- [ ] Escribir en el buscador de correo, o cambiar el estado o el rol, vuelve a la primera página con el filtro aplicado.
- [ ] Mientras carga se ve el esqueleto. Sin resultados se ve «No hay usuarios que coincidan con los filtros». Si falla, se ve el estado de error con «Reintentar».
- [ ] La fila del usuario autenticado muestra «Tú» y no tiene acciones.
- [ ] Con solo `usuarios.ver` no se ven «Nuevo usuario» ni las acciones de las filas.
- [ ] Al crear, «Guardar» está deshabilitado hasta que el correo es válido, la contraseña tiene entre 8 y 72 caracteres y hay al menos un rol marcado. Cada campo muestra su mensaje en español al perder el foco.
- [ ] El selector de roles no ofrece `Administrador del sistema`, roles inactivos ni roles con `requiereMiembro`.
- [ ] Al editar `lider.miembro@prueba.test` (o cualquier usuario con roles que exigen miembro), esos roles aparecen marcados y deshabilitados con su aviso, y se conservan al guardar.
- [ ] Un correo repetido, al crear o al editar, muestra dentro del diálogo el mensaje 409 del backend, y el diálogo sigue abierto.
- [ ] Cambiar el correo al editar pide confirmación antes de guardar.
- [ ] Desactivar al último administrador muestra en el diálogo el mensaje «sin administrador» del backend, y la fila sigue «Activo».
- [ ] En `/roles`, desactivar el rol personalizado del último administrador muestra el mensaje 409 del backend y el rol sigue activo.
- [ ] Desactivar un usuario pide confirmación. Al confirmar, la tabla lo muestra «Inactivo».
- [ ] Restablecer la contraseña muestra un toast de éxito, y la fila pasa a mostrar «Debe cambiar contraseña».
- [ ] Un usuario recién creado, después del login, solo puede estar en `/cuenta/contrasena`: cualquier otra ruta del layout lo devuelve ahí y se ve el aviso de contraseña temporal.
- [ ] Después de cambiarla vuelve al login y, al entrar con la contraseña nueva, llega a su inicio normal.
- [ ] La pantalla `/usuarios` se ve correctamente en ancho de móvil y en modo oscuro, sin CSS propio.
- [ ] `ng test` y `ng build` terminan sin errores.

**Documentación**

- [ ] Swagger muestra `PUT /api/usuarios/{id}/contrasena`, los parámetros nuevos de `GET /api/usuarios` y los campos nuevos de los DTOs, con descripción y ejemplos.
- [ ] La colección de Postman pasa completa con newman.
- [ ] Existe `diagramas/spec-04-administracion-de-usuarios.drawio` con las páginas del paso 16.

## Decisiones

- **Sí:** diferir el vínculo usuario–miembro. `iglesia-api` todavía no tiene miembros. Un campo numérico `miembroId` sin validar dejaría datos que apuntan a miembros inexistentes.
- **No:** incluir aquí un módulo mínimo de miembros. Haría que la spec tocara tres áreas a la vez.
- **Sí:** ocultar los roles que exigen miembro al asignar, y mostrar en solo lectura los que el usuario ya tiene. El backend ya rechaza `LIDER` o `MIEMBRO` sin `miembroId`, y en solo lectura no se pierden asignaciones hechas por la API.
- **Sí:** `requiereMiembro` en `RolResponse`. La regla vive en `RolBase.requiereMiembro()` y el frontend no la duplica.
- **Sí:** contraseña temporal con cambio obligatorio, al crear y al restablecer. Así el administrador no conoce la contraseña definitiva de nadie.
- **No:** contraseña generada por el sistema. El administrador igual tendría que comunicarla, y con el cambio obligatorio deja de importar quién la eligió.
- **Sí:** aplicar el cambio obligatorio en los dos backends con un claim en el JWT. Solo en el frontend sería una regla de navegación que un cliente puede saltarse.
- **Sí:** un filtro en la cadena de seguridad en lugar de una condición en cada `@PreAuthorize`. Es una regla transversal, y así un endpoint nuevo queda protegido sin que nadie lo recuerde.
- **No:** obligar al administrador inicial a cambiar la contraseña. La controla quien despliega, y complicaría los arranques en desarrollo.
- **Sí:** al cambiar la propia contraseña se cierra la sesión y se vuelve al login, como ya ocurre. Así se obtiene un token nuevo sin el claim sin agregar un flujo de refresco especial.
- **Sí:** restablecer revoca los tokens de refresco. Es el mismo criterio que el cambio de contraseña propio.
- **No:** restablecer la propia contraseña desde la administración. Para eso está «Cambiar contraseña», que pide la actual.
- **Sí:** ampliar `GET /api/roles` a `usuarios.administrar`. Quien asigna roles necesita verlos, aunque no pueda editarlos.
- **No:** endpoint aparte `/api/usuarios/roles-asignables`. Duplicaría el listado de roles.
- **No:** exigir `roles.ver` junto con `usuarios.administrar`. Obligaría a combinar permisos al crear roles personalizados.
- **Sí:** filtros en el servidor con `Specification`. Combina filtros opcionales sin armar JPQL por concatenación ni crear un método derivado por combinación.
- **Sí:** el correo es editable, y al cambiarlo se revocan las sesiones. Corrige errores de escritura sin crear otro usuario. El log `INFO` deja rastro del cambio (quién y a quién) sin datos personales.
- **No:** correo inmutable. Era la primera propuesta, pero obligaba a desactivar el usuario y crear otro solo por un error de escritura.
- **Sí:** diálogos sobre la tabla, como en `/roles`. Mantiene la coherencia entre las dos pantallas de administración.
- **Sí:** impedir que la iglesia quede sin administrador en los tres caminos (usuario, estado del rol y permisos del rol). Validar solo en usuarios dejaba un hueco por la pantalla de roles.
- **Sí:** verificar después del cambio, en la misma transacción, y revertir si el conteo da 0. Una sola consulta cubre los tres caminos, sin simular cada cambio por separado.
- **Sí:** «administrador» significa «usuario activo con un rol activo que tiene `usuarios.administrar`», no «tiene el rol `ADMIN_IGLESIA`». Es coherente con la autorización por permisos de la SPEC 02.
- **Sí:** dividir el pedido en tres specs (04 usuarios, 05 acceso global del administrador del sistema, 06 menú). Cada una toca decisiones de seguridad distintas, y juntas pasarían de 30 pasos.
- **Sí (para la SPEC 05):** el administrador del sistema elige una iglesia a la vez y opera dentro de ella, con Hibernate y la RLS activos. Ver todas a la vez exigiría saltarse la RLS.
- **Sí (para la SPEC 06):** reabrir la decisión de la SPEC 02 y guardar el menú en base de datos, por iglesia. La recomendación era no hacerlo, porque los permisos ya deciden quién ve cada opción. El usuario prefiere asignar las opciones de menú a los roles de forma explícita.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Hay usuarios de prueba con `LIDER` o `MIEMBRO` y sin `miembroId` (p. ej. `lider@prueba.test`), y al editarlos el backend responde 400. | El diálogo muestra el mensaje del backend y no se pierde nada. Esos datos se corrigen con el MCP. Con datos creados por la API no ocurre, porque el backend ya exige `miembroId`. |
| Tras restablecer o desactivar, el token de acceso del usuario sigue válido hasta 15 minutos. | Los tokens de refresco se revocan, así que no puede renovar la sesión. Invalidar tokens de acceso queda fuera de alcance. |
| Un endpoint nuevo de `iglesia-seguridad` que deba funcionar con el cambio pendiente queda bloqueado por el filtro. | La lista `RutasCambioContrasena.PERMITIDAS` está en un solo lugar y tiene pruebas. Bloquear por defecto es el comportamiento seguro. |
| El filtro de `iglesia-api` lee un claim que un token antiguo no trae. | Si el claim falta, el filtro lo trata como `false`. Los tokens duran 15 minutos, así que la convivencia es breve. |
| Un usuario elimina su único rol editable y deja solo roles bloqueados. | Los bloqueados cuentan para «al menos uno», y el backend valida igual que hoy. |
| Dos administradores se desactivan entre sí al mismo tiempo y la iglesia queda sin ninguno. | `pg_advisory_xact_lock(iglesia_id)` serializa la verificación por iglesia. Prueba de integración con dos transacciones concurrentes. |
| Al cambiar su correo, el usuario no sabe con qué correo entrar. | El administrador confirma el cambio en un diálogo que lo advierte, y es quien se lo comunica. |
| Un rol se desactiva o pierde `usuarios.administrar` por un camino nuevo que no llama a la garantía. | La garantía vive en el contexto `rol` y se documenta en el diagrama. Cualquier operación nueva que cambie roles o usuarios debe llamarla, y su prueba lo verifica. |

## Qué **no** entra en esta spec

- Vincular usuarios con miembros y asignar desde la interfaz los roles que exigen miembro.
- Acceso del administrador del sistema a todas las iglesias (SPEC 05).
- Menú administrable en base de datos y su asignación a roles (SPEC 06).
- «Olvidé mi contraseña» por correo.
- Invalidar tokens de acceso ya emitidos.
- Borrar usuarios.

Cada uno, si llega, va en su propia spec.
