# SPEC 05 — Acceso global del administrador del sistema

> **Estado:** Implementado
> **Depende de:** SPEC 01 (login, JWT y refresco), SPEC 02 (permisos y roles base), SPEC 03 (OpenAPI y trazabilidad), SPEC 04 (administración de usuarios y contraseña temporal)
> **Fecha:** 2026-10-28
> **Objetivo:** Que el administrador del sistema gestione el catálogo de iglesias, dé de alta una iglesia con su primer administrador y entre a cualquier iglesia para operar dentro de ella con Hibernate y la RLS activos, como si fuera su administrador.

## Por qué existe esta spec

Hoy dar de alta una iglesia exige insertar a mano una fila en `iglesia` (base `iglesia_negocio`) y crear su primer administrador con Swagger o Postman. El sistema es multi-iglesia en la base de datos, pero no en la práctica.

Además, el token del administrador del sistema no trae `iglesia_id`:

- En `iglesia-api` no se fija ninguna iglesia en la transacción, así que la RLS no le deja ver ninguna fila.
- En `iglesia-seguridad`, `/usuarios` le muestra los usuarios de todas las iglesias mezclados, y no puede crear roles (403 «sin iglesia»).

Esta spec no se salta el aislamiento. El administrador del sistema **entra** a una iglesia: `iglesia-seguridad` le emite un token con el `iglesia_id` de esa iglesia, y a partir de ahí todo funciona con las mismas barreras (Hibernate y RLS) que para el administrador de esa iglesia.

La regla de CLAUDE.md se mantiene: «`iglesia-api` resuelve la iglesia a partir del claim, nunca a partir de un parámetro enviado por el cliente».

## Alcance

**Entra:**

- `iglesia-api`: primer contexto de negocio, `iglesia/`, con el catálogo de iglesias:
  - `GET /api/iglesias` y `GET /api/iglesias/{id}` con `iglesias.ver`;
  - `POST /api/iglesias` y `PUT /api/iglesias/{id}` con `iglesias.administrar`;
  - solo se edita el nombre, que es único sin distinguir mayúsculas (409 si se repite).
- `iglesia-seguridad`: permisos nuevos `iglesias.ver` e `iglesias.administrar`, asignados solo al rol base `ADMIN_SISTEMA`.
- `iglesia-seguridad`: entrar a una iglesia y salir de ella.
  - `POST /api/auth/iglesia-actuando` emite un token de acceso con `iglesia_id` de la iglesia elegida, el claim `actuando_en_iglesia: true` y los permisos de un administrador de iglesia sumados a los propios.
  - `DELETE /api/auth/iglesia-actuando` vuelve al token global.
  - Los dos rotan el token de refresco.
- `iglesia-seguridad`: el token de refresco recuerda la iglesia de actuación (`token_refresco.iglesia_actuando_id`), así que el refresco la conserva.
- `iglesia-seguridad`: dentro de una iglesia, el administrador del sistema tiene el ámbito de esa iglesia:
  - las políticas de usuarios y de roles lo tratan como administrador de esa iglesia;
  - `GET /api/usuarios/me` devuelve esa iglesia en `iglesiaId` y `actuandoEnIglesia: true`.
- `iglesia-web`: pantalla `/iglesias` (feature `features/iglesias/`) con:
  - tabla de iglesias;
  - diálogo de alta (nombre de la iglesia, correo y contraseña temporal de su primer administrador);
  - edición del nombre;
  - acción «Crear administrador» para una iglesia existente;
  - acción «Entrar».
- `iglesia-web`: opción de menú «Sistema › Iglesias» con `iglesias.ver`.
- `iglesia-web`: «Miembros», «Usuarios» y «Roles» solo aparecen, y solo se pueden abrir, cuando el usuario opera dentro de una iglesia. En la vista global el administrador del sistema solo ve «Iglesias».
- `iglesia-web`: indicador en la barra superior, «Operando en: MINPAES», con el botón «Salir de la iglesia». **El usuario autoriza tocar `src/app/layout/component/app.topbar.*` para esto.**
- Postman: carpeta «1. Iglesias» en `iglesia-api` y peticiones «Entrar a una iglesia» y «Salir de la iglesia» en `iglesia-seguridad`.
- OpenAPI de todo endpoint nuevo o modificado.
- Diagrama `diagramas/spec-05-acceso-global-del-administrador-del-sistema.drawio`.

**Se conserva sin cambios:**

- `POST /api/usuarios` con `iglesiaId` sigue disponible para el administrador del sistema en la vista global. Es lo que usa el alta del primer administrador.
- `GET /api/usuarios` en la vista global conserva su contrato actual: sin `iglesiaId` lista todas las iglesias. La interfaz deja de usarlo así.
- Un administrador de iglesia solo ve y opera su iglesia, nunca otorga `ADMIN_SISTEMA` y no puede usar `iglesias.*`.
- La garantía de administrador de la SPEC 04: el administrador del sistema no tiene `iglesia_id` propio, así que no cuenta como administrador de ninguna iglesia.

**Fuera de alcance (para specs futuras):**

- Reportes y endpoints entre iglesias. Llegarán cuando haya datos de negocio (miembros, asistencia).
- Desactivar o borrar una iglesia. Para que tuviera efecto, `iglesia-seguridad` tendría que conocer el estado de una tabla de `iglesia-api`.
- Identidad de la iglesia (logo, subdominio, otros datos): el catálogo solo guarda el nombre.
- Mostrar el nombre de la iglesia en `app-logo-iglesia` para los usuarios de esa iglesia.
- SPEC 06 — Menú administrable.

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V3__nombre_de_iglesia_unico.sql`

```sql
-- El administrador del sistema distingue las iglesias por su nombre: no puede haber dos iguales.
CREATE UNIQUE INDEX ux_iglesia_nombre ON iglesia (lower(nombre));
```

La tabla `iglesia` es el catálogo de tenants: **no** tiene `iglesia_id` ni RLS, y la lee y escribe el rol de aplicación.

### Migración `iglesia-seguridad/src/main/resources/db/migration/V4__acceso_global_del_sistema.sql`

```sql
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('iglesias.ver',         'Iglesias', 'Ver el catálogo de iglesias',                         1),
    ('iglesias.administrar', 'Iglesias', 'Dar de alta iglesias, editarlas y entrar a operarlas', 2);

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo
FROM rol r
JOIN permiso p ON p.codigo IN ('iglesias.ver', 'iglesias.administrar')
WHERE r.codigo = 'ADMIN_SISTEMA';

-- Iglesia en la que opera el administrador del sistema con esta sesión; NULL = vista global.
ALTER TABLE token_refresco ADD COLUMN iglesia_actuando_id BIGINT;
```

`ADMIN_IGLESIA` **no** recibe `iglesias.*`, así que un rol personalizado de iglesia nunca puede marcarlos (regla de la SPEC 02: solo se marcan permisos del rol base).

### Claims del JWT

| Claim | Vista global (sin cambios) | Dentro de una iglesia |
| --- | --- | --- |
| `iglesia_id` | ausente | id de la iglesia elegida |
| `actuando_en_iglesia` | ausente | `true` |
| `roles` | `["ADMIN_SISTEMA"]` | `["ADMIN_SISTEMA"]` |
| `permisos` | los de sus roles activos | los de sus roles activos ∪ los del rol base `ADMIN_IGLESIA` |

### Backend `iglesia-api` (contexto nuevo `com.iglesia.api.iglesia`)

- `entity/Iglesia` (`@Entity` sobre `iglesia`; no extiende `EntidadDeIglesia` porque es el catálogo).
- `repository/IglesiaRepository`: `findAllByOrderByNombreAsc`, `existsByNombreIgnoreCase`, `existsByNombreIgnoreCaseAndIdNot`.
- `dto/IglesiaResponse(Long id, String nombre, Instant creadaEn)` y `dto/GuardarIglesiaRequest(String nombre)`:
  - `@NotBlank` y `@Size(max = 150)`;
  - el nombre se guarda sin espacios al inicio ni al final.
- `mapper/IglesiaMapper`.
- `service/IglesiaService`:
  - `listar`, `obtener` (404 si no existe), `crear` y `renombrar`;
  - responde 409 «Ya existe una iglesia con ese nombre»;
  - registra en `INFO` `Iglesia creada id={} por usuarioId={}` y `Iglesia renombrada id={} por usuarioId={}`.
- `controller/IglesiaController` (`/api/iglesias`): `@PreAuthorize("hasAuthority('iglesias.ver')")` o `iglesias.administrar`, `@Valid` y OpenAPI completo.
- `common/openapi/model/ProblemDetailEsquema` y `common/openapi/constant/RespuestasOpenApi`, copiados de `iglesia-seguridad` con este primer controlador, como indica CLAUDE.md.

### Backend `iglesia-seguridad`

- `auth/entity/TokenRefresco`: campo `iglesiaActuandoId` (nullable).
- `auth/service/TokenRefrescoService`:
  - `emitir(usuario, iglesiaActuandoId)`;
  - al rotar en el refresco se conserva la iglesia del token usado.
- `jwt/service/GeneradorTokenAcceso.generar(usuario, iglesiaActuandoId)`. Con iglesia de actuación:
  - pone `iglesia_id` y `actuando_en_iglesia: true`;
  - suma los permisos del rol base `ADMIN_IGLESIA`, leídos de la base.
- `jwt/constant/ClaimsJwt.ACTUANDO_EN_IGLESIA = "actuando_en_iglesia"`.
- `auth/dto/EntrarAIglesiaRequest(Long iglesiaId)` (`@NotNull`, `@Positive`).
- `auth/controller/AuthController`:
  - `POST /api/auth/iglesia-actuando` y `DELETE /api/auth/iglesia-actuando`;
  - exigen `iglesias.administrar` y la cookie de refresco;
  - revocan el refresco actual y emiten un par nuevo;
  - responden 200 con `TokenAccesoResponse` y la cookie rotada; sin cookie o con la cookie inválida, 401.
- `auth/service/AuthService`:
  - `entrarAIglesia(tokenRefresco, iglesiaId)` y `salirDeIglesia(tokenRefresco)`;
  - el token de refresco debe ser del mismo usuario del token de acceso (si no, 401);
  - registran en `INFO` `Administrador del sistema usuarioId={} entra a iglesiaId={}` y `... sale de iglesiaId={}`.
- `common/seguridad/model/UsuarioAutenticado`:
  - `iglesiaId` se toma del claim, igual que hoy;
  - nuevo `actuandoEnIglesia()`;
  - nuevo `esGlobal()`, que es `tieneRol(ADMIN_SISTEMA) && iglesiaId == null`.
- `usuario/service/PoliticaAdministracionUsuarios` y `rol/service/PoliticaRoles`:
  - donde hoy preguntan «¿es `ADMIN_SISTEMA`?» para dar el ámbito global, preguntan `esGlobal()`;
  - dentro de una iglesia, el administrador del sistema solo ve y administra esa iglesia;
  - nunca ve ni edita a otros `ADMIN_SISTEMA`.
- `usuario/dto/MiUsuarioResponse`:
  - `iglesiaId` pasa a ser la iglesia del token;
  - nuevo campo `boolean actuandoEnIglesia`;
  - dentro de una iglesia, el perfil «Administrador del sistema» lleva los permisos del token.
- `RolController`: `GET /api/permisos` sigue devolviendo el catálogo completo; el formulario de roles ya deshabilita los permisos fuera del rol base.

### Frontend (`iglesia-web/src/app/`)

- `core/auth/models/permiso.ts`: `iglesias.ver` e `iglesias.administrar` en `PERMISOS`.
- `core/auth/models/usuario-actual.ts` y `usuario-response.ts`: `actuandoEnIglesia: boolean`. `iglesiaId` es la iglesia en la que opera.
- `core/auth/services/auth-api.service.ts`: `entrarAIglesia(iglesiaId)` y `salirDeIglesia()`, con `withCredentials`.
- `core/auth/services/sesion.service.ts`:
  - `entrarAIglesia(id)` y `salirDeIglesia()`: guardan el token nuevo y recargan `/me`;
  - `operaEnIglesia = computed(() => usuarioActual()?.iglesiaId != null)`;
  - `actuandoEnIglesia = computed(...)`.
- `core/auth/guards/auth.guards.ts`: `iglesiaFijadaGuard`. Sin iglesia lleva a `/iglesias` si el usuario tiene `iglesias.ver`, y a `/auth/acceso-denegado` si no.
- `core/iglesias/`, pieza única porque la usan la barra superior y la feature:
  - `models/iglesia.ts` (`IglesiaResponse`, `Iglesia`, `NOMBRE_IGLESIA_MAX = 150`);
  - `iglesia.mapper.ts`;
  - `iglesias.service.ts` (`listar`, `obtener`, `crear`, `renombrar`; URL base `environment.apiUrl`).
- `features/iglesias/`:

| Pieza | Responsabilidad |
| --- | --- |
| `components/tabla-iglesias` | Presentacional:<br>- `p-table` con Nombre, Creada y Acciones;<br>- acciones «Entrar», «Editar nombre» y «Crear administrador», solo con `iglesias.administrar`;<br>- la iglesia en la que se opera muestra la etiqueta «Actual»;<br>- esqueleto y estado vacío. |
| `components/formulario-iglesia` | `p-dialog`:<br>- al crear: nombre (`required`, máx. 150), correo del primer administrador (`required`, `email`, máx. 254) y contraseña temporal (8–72, `p-password` con `autocomplete="new-password"`);<br>- al editar: solo el nombre. |
| `components/formulario-administrador` | `p-dialog` para crear un administrador (rol base `ADMIN_IGLESIA`) de una iglesia existente: correo y contraseña temporal. |
| `pages/iglesias-lista` | Contenedor:<br>- carga el catálogo;<br>- alta en dos llamadas (`POST /api/iglesias` y luego `POST /api/usuarios` con ese `iglesiaId` y el rol base `ADMIN_IGLESIA`);<br>- si falla la segunda, cierra el diálogo, recarga la tabla y muestra un aviso: «La iglesia se creó, pero no su administrador: <detalle>. Usa "Crear administrador".»;<br>- 400 y 409 dentro del diálogo, toast de éxito, y estado de error con «Reintentar»;<br>- «Entrar» llama a `SesionService.entrarAIglesia` y navega a `/usuarios`. |
| `iglesias.routes.ts` | `permisoGuard('iglesias.ver')`. |

- `app.routes.ts`: `/iglesias` hija del layout; `iglesiaFijadaGuard` en `/miembros`, `/usuarios` y `/roles`.
- `layout/component/app.menu.ts`:
  - grupo «Sistema» › «Iglesias» (`pi-building`, `iglesias.ver`), antes de «Iglesia»;
  - `EntradaMenu` gana `requiereIglesia?: boolean`, marcado en «Miembros», «Usuarios» y «Roles»;
  - `construirMenu` recibe si se opera en una iglesia.
- `layout/component/app.topbar.*` (autorizado):
  - con `actuandoEnIglesia`, muestra un `p-tag` «Operando en: <nombre>» (nombre de `IglesiasService.obtener`) y un botón «Salir de la iglesia»;
  - salir llama a `SesionService.salirDeIglesia` y navega a `/iglesias`.

## Plan de implementación

**Backend `iglesia-seguridad`**

1. Crear `V4__acceso_global_del_sistema.sql` y aplicarla con `./gradlew bootRun`. Comprobar con el MCP los permisos de `ADMIN_SISTEMA` y la columna nueva. Agregar las constantes de los permisos en `Permisos`.
2. Iglesia de actuación en el token y en el refresco:
   - `TokenRefresco.iglesiaActuandoId`, `TokenRefrescoService` y `GeneradorTokenAcceso.generar(usuario, iglesiaActuandoId)`;
   - el refresco conserva la iglesia;
   - pruebas unitarias del generador (claims y permisos sumados) y de integración del refresco.
3. `POST` y `DELETE /api/auth/iglesia-actuando`:
   - `AuthService.entrarAIglesia` y `salirDeIglesia`, logs `INFO` y OpenAPI;
   - pruebas de integración: 200 con cookie rotada y claims correctos, 401 sin cookie, 403 sin `iglesias.administrar`, la cookie anterior queda revocada.
4. Ámbito dentro de una iglesia:
   - `UsuarioAutenticado.esGlobal()` y `actuandoEnIglesia()`;
   - las políticas de usuarios y de roles usan `esGlobal()`;
   - `MiUsuarioResponse.actuandoEnIglesia`, con perfil y permisos del token;
   - pruebas: dentro de una iglesia, `GET /api/usuarios` solo trae esa iglesia, crear un rol funciona en esa iglesia, `/me` refleja la iglesia, y otro `ADMIN_SISTEMA` no aparece.
5. Postman `iglesia-seguridad`: en «1. Autenticación», «Entrar a una iglesia» (guarda el token) y «Salir de la iglesia», con pruebas de los claims. Verificar con newman.

**Backend `iglesia-api`**

6. Crear `V3__nombre_de_iglesia_unico.sql`. Copiar `ProblemDetailEsquema` y `RespuestasOpenApi`.
7. Contexto `iglesia/`: entidad, repositorio, DTOs, mapper, servicio y controlador con OpenAPI.
   - Pruebas unitarias del servicio.
   - Pruebas de integración con Testcontainers:
     - listar, crear, renombrar;
     - 409 por nombre repetido con otra capitalización;
     - 404, y 403 sin el permiso;
     - un token con `iglesia_id` de otra iglesia sigue viendo el catálogo completo, porque `iglesia` no tiene RLS;
     - el log `INFO` sin el nombre.
8. Postman `iglesia-api`: carpeta «1. Iglesias» (las carpetas actuales pasan a «2.» y «3.»), con el token del administrador del sistema. Verificar con newman.

**Frontend `iglesia-web`**

9. `core/auth`:
   - permisos nuevos;
   - `actuandoEnIglesia` en modelos y mapper;
   - `AuthApiService.entrarAIglesia` y `salirDeIglesia`;
   - `SesionService.entrarAIglesia`, `salirDeIglesia`, `operaEnIglesia` y `actuandoEnIglesia`;
   - `iglesiaFijadaGuard`;
   - specs.
10. `core/iglesias/`: modelos, mapper e `IglesiasService`, con specs.
11. `TablaIglesiasComponent`, `FormularioIglesiaComponent` y `FormularioAdministradorComponent`, con specs (validaciones, acciones según permiso, «Actual», esqueleto y vacío).
12. `IglesiasListaComponent` y `iglesias.routes.ts`. Registrar `/iglesias` y aplicar `iglesiaFijadaGuard` en `app.routes.ts`.
   - Specs de la página: alta completa, alta con fallo del administrador, 409, «Entrar».
   - Specs de rutas: vista global → `/usuarios` lleva a `/iglesias`; dentro de una iglesia se abre.
13. `app.menu.ts`: grupo «Sistema › Iglesias» y `requiereIglesia`. Specs del menú en la vista global y dentro de una iglesia.
14. `app.topbar.*`: indicador «Operando en» y «Salir de la iglesia», con su spec.

**Cierre**

15. Crear `diagramas/spec-05-acceso-global-del-administrador-del-sistema.drawio` con varias páginas, colores con leyenda y notas:
    - arquitectura de `/iglesias` (componentes, servicios, endpoints de los dos backends, clases y tablas);
    - alta de una iglesia con su primer administrador, incluido el fallo del segundo paso;
    - entrar a una iglesia, refrescar y salir: el token, la cookie y `token_refresco.iglesia_actuando_id`;
    - cómo la iglesia del token activa Hibernate y la RLS en `iglesia-api` y el ámbito en las políticas de `iglesia-seguridad`.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Después de aplicar `V4`, `ADMIN_SISTEMA` tiene `iglesias.ver` e `iglesias.administrar`, y `ADMIN_IGLESIA` no.
- [ ] Un rol personalizado heredado de `ADMIN_IGLESIA` no puede marcar `iglesias.*` (400).
- [ ] `POST /api/auth/iglesia-actuando` con `iglesiaId: 1` responde 200.
  - El token nuevo trae `iglesia_id: 1`, `actuando_en_iglesia: true`, `roles: ["ADMIN_SISTEMA"]` y los permisos de `ADMIN_IGLESIA` más `iglesias.*`.
  - La cookie de refresco rota y la anterior responde 401 en `POST /api/auth/refresh`.
- [ ] `POST /api/auth/refresh` con la cookie nueva emite un token que conserva `iglesia_id: 1` y `actuando_en_iglesia: true`.
- [ ] `DELETE /api/auth/iglesia-actuando` responde 200 con un token sin `iglesia_id` ni `actuando_en_iglesia`, y el refresco siguiente sigue global.
- [ ] Los dos endpoints responden 401 sin cookie y 403 con un token sin `iglesias.administrar` (p. ej. un `ADMIN_IGLESIA`).
- [ ] Dentro de la iglesia 1:
  - `GET /api/usuarios` devuelve solo usuarios de la iglesia 1, aunque se envíe `iglesiaId=2`;
  - `POST /api/roles` crea el rol en la iglesia 1;
  - `GET /api/usuarios/me` devuelve `iglesiaId: 1` y `actuandoEnIglesia: true`.
- [ ] Dentro de una iglesia, el administrador del sistema no ve a otros usuarios `ADMIN_SISTEMA` ni puede editarlos (404).
- [ ] En la vista global, `POST /api/usuarios` con `iglesiaId` sigue creando usuarios en esa iglesia.
- [ ] La garantía de administrador de la SPEC 04 sigue igual: el administrador del sistema dentro de una iglesia no cuenta como administrador de esa iglesia.
- [ ] Los logs de entrar y salir llevan `usuarioId` e `iglesiaId`, sin correos.

**Backend `iglesia-api`**

- [ ] `GET /api/iglesias` con `iglesias.ver` devuelve las iglesias ordenadas por nombre. Sin el permiso, 403 con `ProblemDetail` y `traceId`.
- [ ] `POST /api/iglesias` crea la iglesia (201) y guarda el nombre sin espacios al inicio ni al final. Un nombre repetido con otra capitalización responde 409.
- [ ] `PUT /api/iglesias/{id}` cambia el nombre (200), responde 404 si no existe y 409 si el nombre es de otra iglesia.
- [ ] Un nombre vacío o de más de 150 caracteres responde 400.
- [ ] Con un token dentro de una iglesia, el catálogo sigue completo: `iglesia` no está filtrada por RLS.

**Frontend**

- [ ] El administrador del sistema, en la vista global, ve en el menú solo «Inicio» y «Sistema › Iglesias». `/usuarios`, `/roles` y `/miembros` lo llevan a `/iglesias`.
- [ ] Un administrador de iglesia no ve «Sistema › Iglesias», y `/iglesias` lo lleva a `/auth/acceso-denegado`.
- [ ] Alta de una iglesia:
  - «Guardar» está deshabilitado hasta que el nombre, el correo y la contraseña temporal son válidos;
  - al guardar se crean la iglesia y su administrador, con toast de éxito;
  - el administrador nuevo, al entrar, debe cambiar su contraseña temporal (SPEC 04).
- [ ] Un nombre de iglesia repetido muestra el 409 dentro del diálogo, que sigue abierto.
- [ ] Si la creación del administrador falla (p. ej. correo repetido), la iglesia aparece en la tabla y se muestra el aviso que indica usar «Crear administrador».
- [ ] «Crear administrador» en una iglesia existente crea un usuario con el rol «Administrador de iglesia» en esa iglesia.
- [ ] «Editar nombre» cambia el nombre en la tabla.
- [ ] Al pulsar «Entrar» en MINPAES:
  - lleva a `/usuarios` con solo los usuarios de MINPAES;
  - el menú muestra «Miembros», «Usuarios» y «Roles»;
  - la barra superior muestra «Operando en: MINPAES»;
  - en `/iglesias`, la fila de MINPAES tiene la etiqueta «Actual».
- [ ] Dentro de MINPAES se puede crear un rol personalizado y un usuario de MINPAES.
- [ ] Al recargar la página o tras renovar el token, se sigue operando en MINPAES.
- [ ] «Salir de la iglesia» vuelve a `/iglesias` con la vista global. El indicador desaparece y el menú vuelve a mostrar solo «Iglesias».
- [ ] La pantalla `/iglesias` y el indicador se ven bien en móvil y en modo oscuro, sin CSS propio.
- [ ] `ng test` y `ng build` terminan sin errores.

**Documentación**

- [ ] Swagger de `iglesia-api` muestra los 4 endpoints de `/api/iglesias`, y el de `iglesia-seguridad` los 2 de `/api/auth/iglesia-actuando` y el campo `actuandoEnIglesia`, con descripción, ejemplos y errores.
- [ ] Las dos colecciones de Postman pasan completas con newman.
- [ ] Existe `diagramas/spec-05-acceso-global-del-administrador-del-sistema.drawio` con las páginas del paso 15.

## Decisiones

- **Sí:** dejar fuera los reportes entre iglesias. `iglesia-api` todavía no tiene datos de negocio que reportar, y definir ese patrón sin datos reales sería especular.
- **Sí:** el catálogo de iglesias vive solo en `iglesia-api`, donde ya está la tabla `iglesia`. **No:** duplicarlo en `iglesia-seguridad` ni hacer que un backend llame al otro.
- **Sí:** el frontend orquesta el alta (iglesia y después administrador). No es atómica, y se acepta: si falla el segundo paso, la iglesia queda sin administrador y la acción «Crear administrador» lo resuelve. Los backends siguen sin comunicarse entre sí.
- **Sí:** entrar a una iglesia emite un token con `iglesia_id`. **No:** cabecera `X-Iglesia-Id` ni parámetro por endpoint. Así la iglesia sale siempre del claim, como exige CLAUDE.md, y Hibernate, la RLS y las políticas funcionan sin excepciones.
- **Sí:** la iglesia de actuación se guarda en `token_refresco`, para que el refresco la conserve. Perderla cada 15 minutos sacaría al administrador de la iglesia en mitad del trabajo.
- **Sí:** dentro de una iglesia, el token lleva los permisos de `ADMIN_IGLESIA` sumados a los propios. Los módulos futuros le funcionan sin cambios.
- **Sí:** `roles` sigue siendo `["ADMIN_SISTEMA"]` dentro de una iglesia, con el claim `actuando_en_iglesia` aparte. Los logs y las políticas siguen distinguiendo quién es en realidad.
- **Sí:** no se valida en `iglesia-seguridad` que la iglesia exista; es un riesgo aceptado. Solo el administrador del sistema elige la iglesia, desde el catálogo. Con un id inexistente, `iglesia-api` no muestra datos por la RLS.
- **Sí:** permisos nuevos `iglesias.ver` e `iglesias.administrar`, solo de `ADMIN_SISTEMA`. **No:** `hasRole('ADMIN_SISTEMA')`, que rompería la convención de la SPEC 02 de autorizar siempre por permiso.
- **Sí:** el catálogo solo tiene el nombre, único sin distinguir mayúsculas. Sin unicidad, el administrador del sistema no sabría en qué «Iglesia Central» entra.
- **Sí:** el alta pide en un mismo diálogo la iglesia y su primer administrador, para no olvidar el segundo paso.
- **Sí:** en la vista global, la interfaz solo ofrece «Iglesias». `/usuarios` y `/roles` se usan dentro de una iglesia. El contrato de la API de usuarios no cambia.
- **Sí:** el usuario autoriza tocar `app.topbar` para el indicador «Operando en», que es lo más visible y no se pierde al navegar.
- **Sí:** `IglesiasService` va en `core/iglesias/` y no en la feature, porque lo usan la barra superior y la pantalla `/iglesias`, y el layout no debe depender de una feature.
- **Sí:** después de «Entrar» se navega a `/usuarios`. Las páginas de inicio dependen del rol base, y el del administrador del sistema sigue siendo `ADMIN_SISTEMA`.
- **No:** desactivar iglesias en esta spec. Bloquear el login de sus usuarios exige sincronizar estado entre los dos backends.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| El alta deja una iglesia sin administrador si falla el segundo paso. | Aviso explícito en la pantalla y acción «Crear administrador» en cada fila. Se documenta en el diagrama. |
| Se entra a una iglesia que no existe (id inventado, llamando directo a la API). | Solo `iglesias.administrar` puede hacerlo. Con un id inexistente no hay datos que ver en `iglesia-api`. En `iglesia-seguridad` podría crear usuarios con ese `iglesia_id`, igual que hoy con `POST /api/usuarios`, y se acepta. |
| Un endpoint futuro de `iglesia-seguridad` decide el ámbito con «¿es `ADMIN_SISTEMA`?» y salta el aislamiento dentro de una iglesia. | La regla se centraliza en `UsuarioAutenticado.esGlobal()` y las pruebas de las políticas cubren el caso dentro de una iglesia. |
| Los permisos `iglesias.*` aparecen deshabilitados en el formulario de roles de las iglesias. | Ya pasa con cualquier permiso fuera del rol base. El grupo «Iglesias» sale deshabilitado y no se puede marcar. |
| El token de acceso anterior a «Salir» sigue valiendo hasta 15 minutos con la iglesia fijada. | El frontend lo descarta al recibir el nuevo y el refresco anterior queda revocado. Invalidar tokens de acceso sigue fuera de alcance (SPEC 04). |
| Tocar `app.topbar` choca con futuras actualizaciones de la plantilla Sakai. | El cambio se limita a un bloque condicional con su spec. Está autorizado de forma explícita en esta spec. |

## Qué **no** entra en esta spec

- Reportes y endpoints entre iglesias.
- Desactivar o borrar iglesias.
- Logo, subdominio u otros datos de identidad de la iglesia.
- Vincular usuarios con miembros.
- Menú administrable (SPEC 06).

Cada uno, si llega, va en su propia spec.
