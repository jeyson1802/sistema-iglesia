# SPEC 06 — Registro de miembros

> **Estado:** Aprobado
> **Depende de:** SPEC 02 (permisos y roles base), SPEC 03 (OpenAPI y trazabilidad), SPEC 04 (administración de usuarios y contraseña temporal), SPEC 05 (acceso global del administrador del sistema)
> **Fecha:** 2026-10-28
> **Objetivo:** Que cada iglesia registre y vaya completando la ficha de todos sus congregantes (antiguos, nuevos y recuperados) con su nivel, su líder, su grupo y su ubicación, administre sus niveles y sus grupos, y que cada líder vea su red y a sus superiores.

## Por qué existe esta spec

Hoy `iglesia-api` no tiene miembros: los paquetes `miembros` y `nuevosmiembros` están vacíos y no hay tablas. En el frontend, `features/miembros` es un resto de la plantilla que no cumple las reglas del proyecto (plantilla inline, sin `OnPush`, sin pruebas). Sin miembros no se puede construir ningún otro módulo de la hoja de ruta: el miembro es la entidad central.

Las iglesias registran a la gente por partes: primero solo el nombre y después el resto, cuando lo consiguen. Por eso el único dato obligatorio son los **nombres y apellidos**. Todo lo demás se completa con el tiempo.

Esta spec junta en un solo registro lo que la hoja de ruta separaba en «miembros actuales» y «nuevos miembros»: es la misma persona en dos momentos, y la **condición** (antiguo, nuevo o recuperado) basta para distinguirlas.

## Alcance

**Entra:**

- **Registro de miembros (`/miembros`):** listado paginado con búsqueda y filtros, alta, edición, desactivación y reactivación.
  - Solo nombres y apellidos son obligatorios. La ficha indica qué datos faltan.
  - Al dar de alta avisa si ya existe alguien con los mismos nombres y apellidos, y deja confirmar.
- **Datos de la ficha:**
  - nombres, apellidos, género, fecha de nacimiento y edad;
  - fecha de ingreso a la iglesia y condición (antiguo, nuevo o recuperado);
  - dirección, con departamento, provincia y distrito del ubigeo;
  - teléfono, correo, estado civil, ocupación y profesión;
  - nivel de estudios y centro de estudios;
  - nivel, líder y grupo;
  - quién lo invitó, quién lo registró y petición.
- **Edad:** con fecha de nacimiento se calcula y no se guarda. Sin fecha se puede indicar una edad aproximada, que se guarda como año de nacimiento estimado.
- **Jerarquía de liderazgo:**
  - cada miembro tiene a lo sumo un líder (opcional);
  - no se permiten ciclos;
  - la acción «Designar pastor principal» marca al único pastor principal de la iglesia, que no tiene líder;
  - la ficha muestra la cadena de liderazgo hasta el pastor y a cuántos lidera directamente.
- **Visibilidad del líder:** un usuario con rol base `LIDER` ve, en solo lectura, a las personas de su red (directas e indirectas), a sí mismo y a sus superiores hasta el pastor.
- **Acceso al sistema desde la ficha:** la acción «Dar acceso al sistema» crea un usuario en `iglesia-seguridad` vinculado a ese miembro, con rol `Líder` o `Miembro` y una contraseña temporal. La ficha muestra si el miembro ya tiene acceso.
- **Niveles (`/niveles`):** catálogo por iglesia para crear, renombrar, ordenar, desactivar y reactivar niveles.
- **Grupos (`/grupos`):**
  - mantenimiento por iglesia con nombre, líder, anfitrión, día y hora de reunión, dirección con ubigeo y descripción;
  - cada iglesia configura cómo se llaman sus grupos (p. ej. «Célula» o «Hogar de paz», en singular y plural).
  - El líder del miembro y el grupo del miembro son independientes: alguien puede tener un líder y estar en el grupo de otro.
- **Ubigeo del Perú:** catálogo oficial INEI (departamentos, provincias y distritos), común a todas las iglesias, con selectores en cascada.
- **Permisos nuevos** en `iglesia-seguridad`: `niveles.administrar`, `grupos.ver` y `grupos.administrar`, asignados a `ADMIN_IGLESIA`. Además, `LIDER` recibe `miembros.ver`.
- **Piezas reutilizables:** selector de miembro, selector de ubigeo, OpenAPI, Postman, pruebas y diagrama.

**Fuera de alcance (para specs futuras):**

- Vistas de árbol de la iglesia o de una rama, y totales de la red completa.
- Que un líder registre o edite personas. En esta spec solo ve.
- Portal del miembro: que un usuario con rol `Miembro` vea su propia ficha (módulo 6).
- Vincular un miembro con un usuario que ya existe, o desvincularlos, desde `/usuarios`.
- Seguimiento y consolidación de nuevos (llamadas, contactos), asistencia y ofrendas.
- Niveles académicos y discipulado (módulo 7). El «nivel de estudios» de esta spec es la educación formal, no el académico de la iglesia.
- Historial de cambios de líder, nivel o grupo.
- Importar miembros desde Excel.
- Foto del miembro y documento de identidad.
- Ubigeo de otros países.

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V4__ubigeo_peru.sql`

Catálogo geográfico común: no lleva `iglesia_id` ni RLS, y la aplicación solo lo lee (`GRANT SELECT`). Los datos son los del ubigeo oficial del INEI.

```sql
CREATE TABLE ubigeo_departamento (
    codigo CHAR(2)     PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL
);

CREATE TABLE ubigeo_provincia (
    codigo              CHAR(4)     PRIMARY KEY,
    departamento_codigo CHAR(2)     NOT NULL REFERENCES ubigeo_departamento (codigo),
    nombre              VARCHAR(60) NOT NULL
);

CREATE TABLE ubigeo_distrito (
    codigo           CHAR(6)     PRIMARY KEY,
    provincia_codigo CHAR(4)     NOT NULL REFERENCES ubigeo_provincia (codigo),
    nombre           VARCHAR(80) NOT NULL
);

-- INSERT de los 25 departamentos, sus provincias y sus distritos (INEI).
REVOKE INSERT, UPDATE, DELETE ON ubigeo_departamento, ubigeo_provincia, ubigeo_distrito FROM ${rol_aplicacion};
```

### Migración `iglesia-api/src/main/resources/db/migration/V5__registro_de_miembros.sql`

Todas las tablas siguen la convención de V1: `iglesia_id NOT NULL`, `UNIQUE (iglesia_id, id)` como destino de FK compuestas, índices que empiezan por `iglesia_id` y `habilitar_aislamiento_por_iglesia(...)`.

```sql
-- Cómo llama cada iglesia a sus grupos. Si no hay fila, la API devuelve «Grupo» / «Grupos».
CREATE TABLE configuracion_iglesia (
    iglesia_id            BIGINT PRIMARY KEY REFERENCES iglesia (id),
    nombre_grupo_singular VARCHAR(40) NOT NULL,
    nombre_grupo_plural   VARCHAR(40) NOT NULL
);

CREATE TABLE nivel (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre     VARCHAR(60)  NOT NULL,
    orden      INTEGER      NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_nivel_nombre ON nivel (iglesia_id, lower(nombre));

CREATE TABLE miembro (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id               BIGINT       NOT NULL REFERENCES iglesia (id),
    nombres                  VARCHAR(100) NOT NULL,
    apellidos                VARCHAR(100) NOT NULL,
    genero                   VARCHAR(10)  CHECK (genero IN ('MASCULINO', 'FEMENINO')),
    fecha_nacimiento         DATE,
    anio_nacimiento_estimado SMALLINT,
    fecha_ingreso            DATE,
    condicion                VARCHAR(12)  CHECK (condicion IN ('ANTIGUO', 'NUEVO', 'RECUPERADO')),
    estado_civil             VARCHAR(12)  CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO')),
    telefono                 VARCHAR(20),
    correo                   VARCHAR(254),
    direccion                VARCHAR(200),
    distrito_codigo          CHAR(6)      REFERENCES ubigeo_distrito (codigo),
    ocupacion                VARCHAR(100),
    profesion                VARCHAR(100),
    nivel_estudios           VARCHAR(15)  CHECK (nivel_estudios IN ('SIN_ESTUDIOS', 'PRIMARIA', 'SECUNDARIA', 'TECNICO', 'UNIVERSITARIO', 'POSGRADO')),
    centro_estudios          VARCHAR(150),
    peticion                 VARCHAR(1000),
    nivel_id                 BIGINT,
    lider_id                 BIGINT,
    grupo_id                 BIGINT,
    invitado_por_id          BIGINT,
    registrado_por_id        BIGINT,
    es_pastor_principal      BOOLEAN      NOT NULL DEFAULT FALSE,
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, nivel_id)          REFERENCES nivel   (iglesia_id, id),
    FOREIGN KEY (iglesia_id, lider_id)          REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, invitado_por_id)   REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, registrado_por_id) REFERENCES miembro (iglesia_id, id),
    CHECK (lider_id IS NULL OR lider_id <> id),
    CHECK (NOT (es_pastor_principal AND lider_id IS NOT NULL)),
    CHECK (fecha_nacimiento IS NULL OR anio_nacimiento_estimado IS NULL)
);
CREATE UNIQUE INDEX ux_miembro_pastor_principal ON miembro (iglesia_id) WHERE es_pastor_principal;
CREATE INDEX ix_miembro_lider  ON miembro (iglesia_id, lider_id);
CREATE INDEX ix_miembro_grupo  ON miembro (iglesia_id, grupo_id);
CREATE INDEX ix_miembro_nombre ON miembro (iglesia_id, lower(apellidos), lower(nombres));

CREATE TABLE grupo (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id      BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre          VARCHAR(100) NOT NULL,
    lider_id        BIGINT,
    anfitrion_id    BIGINT,
    dia_reunion     VARCHAR(10)  CHECK (dia_reunion IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    hora_reunion    TIME,
    direccion       VARCHAR(200),
    distrito_codigo CHAR(6)      REFERENCES ubigeo_distrito (codigo),
    descripcion     VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, lider_id)     REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, anfitrion_id) REFERENCES miembro (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_grupo_nombre ON grupo (iglesia_id, lower(nombre));

-- miembro y grupo se referencian mutuamente: la FK del grupo del miembro se agrega al final.
ALTER TABLE miembro ADD FOREIGN KEY (iglesia_id, grupo_id) REFERENCES grupo (iglesia_id, id);

SELECT habilitar_aislamiento_por_iglesia('configuracion_iglesia');
SELECT habilitar_aislamiento_por_iglesia('nivel');
SELECT habilitar_aislamiento_por_iglesia('miembro');
SELECT habilitar_aislamiento_por_iglesia('grupo');
```

### Migración `iglesia-seguridad/src/main/resources/db/migration/V5__permisos_de_miembros_niveles_y_grupos.sql`

```sql
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('niveles.administrar', 'Niveles', 'Crear, renombrar, ordenar y desactivar niveles', 1),
    ('grupos.ver',          'Grupos',  'Ver los grupos de la iglesia',                    1),
    ('grupos.administrar',  'Grupos',  'Crear, editar y desactivar grupos',               2);

-- ADMIN_IGLESIA recibe los tres permisos nuevos; LIDER recibe miembros.ver (ve su red, ver «Ámbito»).
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r JOIN permiso p ON p.codigo IN ('niveles.administrar', 'grupos.ver', 'grupos.administrar')
WHERE r.codigo = 'ADMIN_IGLESIA';
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'miembros.ver' FROM rol r WHERE r.codigo = 'LIDER';
```

### Tipos de dominio (`iglesia-api`, `com.iglesia.api.miembros.model`)

```java
public enum Genero { MASCULINO, FEMENINO }
public enum CondicionMiembro { ANTIGUO, NUEVO, RECUPERADO }
public enum EstadoCivil { SOLTERO, CASADO, DIVORCIADO, VIUDO }
public enum NivelEstudios { SIN_ESTUDIOS, PRIMARIA, SECUNDARIA, TECNICO, UNIVERSITARIO, POSGRADO }
// com.iglesia.api.grupos.model
public enum DiaSemana { LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO }
```

### Reglas de la ficha

| Dato | Regla |
| --- | --- |
| `nombres`, `apellidos` | Obligatorios, sin espacios al inicio ni al final, máx. 100. |
| `fechaNacimiento` | Opcional, no futura. Si se envía, `edadAproximada` debe ir vacía. |
| `edadAproximada` | Opcional, entre 0 y 120, solo sin `fechaNacimiento`. Se guarda como `anio_nacimiento_estimado = año actual − edad`. |
| `edad` (respuesta) | Con fecha: exacta. Con año estimado: año actual − año, con `edadEsAproximada: true`. Sin ninguno: `null`. |
| `fechaIngreso` | Opcional, no futura. |
| `telefono` | Opcional: dígitos, espacios, `+` y `-`, máx. 20. |
| `correo` | Opcional, formato de correo, máx. 254. **No** es único (una familia puede compartirlo). |
| `distritoCodigo` | Opcional, debe existir en `ubigeo_distrito`. Departamento y provincia se deducen del distrito. |
| `nivelId`, `grupoId` | Opcionales, de la misma iglesia y activos al asignarlos. |
| `liderId`, `invitadoPorId`, `registradoPorId` | Opcionales, de la misma iglesia, activos al asignarlos y distintos del propio miembro. |
| `liderId` (además) | Prohibido para el pastor principal. No puede ser alguien de la red del propio miembro (ciclo). |

### Ámbito de lectura (`iglesia-api`)

`AmbitoMiembros` se calcula desde el token, nunca desde parámetros:

| Token | Ve |
| --- | --- |
| `roles` incluye `ADMIN_IGLESIA`, o el administrador del sistema dentro de una iglesia (SPEC 05) | Toda la iglesia. |
| `roles` incluye `LIDER` y el token trae `miembro_id` | Ese miembro, su red (directos e indirectos) y su cadena de superiores hasta el pastor. |
| Cualquier otro caso con `miembros.ver` | Nada: listado vacío y 404 en el detalle. |

La red y la cadena se obtienen con CTE recursivas (`WITH RECURSIVE`) que filtran `iglesia_id` también en el paso recursivo, como exige CLAUDE.md. Escribir sigue exigiendo `miembros.crear`, `miembros.editar` o `miembros.desactivar`, que `LIDER` no tiene.

### Backend `iglesia-api`

| Contexto | Piezas |
| --- | --- |
| `ubigeo` | Entidades de solo lectura, `UbigeoController` y `UbigeoService`. |
| `niveles` | `Nivel`, `NivelRepository`, `NivelService`, `NivelController`, DTO y mapper. |
| `grupos` | `Grupo` y `ConfiguracionIglesia` (o contexto `configuracion` aparte si crece), con sus repository, service, controller, DTO y mapper. |
| `miembros` | `Miembro`, `MiembroRepository` (con la Specification del listado y las CTE de red y cadena), `MiembroService`, `PoliticaJerarquia` (ciclos, pastor principal, desactivación), `AmbitoMiembros`, `MiembroController`, DTO y mapper. |
| `common/seguridad` | `UsuarioActual` agrega `miembroId()` y `rolesBase()` desde el JWT. Las constantes de `Permisos` agregan las nuevas. |
| `common/openapi` | Sin cambios: se reutilizan `ProblemDetailEsquema` y `RespuestasOpenApi`. |

El paquete vacío `nuevosmiembros` se elimina, porque nuevos y antiguos son el mismo registro.

### Endpoints de `iglesia-api`

| Endpoint | Permiso | Respuestas |
| --- | --- | --- |
| `GET /api/miembros?q&condicion&nivelId&grupoId&liderId&activo&page&size&sort` | `miembros.ver` (filtrado por ámbito) | 200 `Page<MiembroResumenResponse>` |
| `GET /api/miembros/{id}` | `miembros.ver` (ámbito) | 200 `MiembroResponse` · 404 |
| `GET /api/miembros/{id}/cadena` | `miembros.ver` (ámbito) | 200: lista del líder directo al pastor · 404 |
| `GET /api/miembros/opciones?q` | `miembros.ver` | 200: hasta 20 miembros activos `{ id, nombreCompleto }` del ámbito |
| `GET /api/miembros/posibles-duplicados?nombres&apellidos` | `miembros.crear` | 200: coincidencias sin distinguir mayúsculas ni espacios |
| `POST /api/miembros` | `miembros.crear` | 201 + `Location` · 400 · 409 (ciclo o referencia inactiva) |
| `PUT /api/miembros/{id}` | `miembros.editar` | 200 · 400 · 404 · 409 |
| `PATCH /api/miembros/{id}/estado` `{ activo }` | `miembros.desactivar` | 200 · 404 · 409 (lidera miembros o grupos activos, o es el pastor principal) |
| `PUT /api/miembros/{id}/pastor-principal` | `miembros.editar` | 200: quita la marca al anterior y el líder al nuevo · 404 · 409 (inactivo) |
| `GET /api/niveles` | `miembros.ver` o `niveles.administrar` | 200: ordenados por `orden`, incluye inactivos con `activo: false` |
| `POST /api/niveles` · `PUT /api/niveles/{id}` | `niveles.administrar` | 201/200 · 400 · 404 · 409 (nombre repetido) |
| `PUT /api/niveles/orden` `{ ids: [...] }` | `niveles.administrar` | 200: deben venir todos los niveles de la iglesia · 400 |
| `PATCH /api/niveles/{id}/estado` | `niveles.administrar` | 200 · 404 |
| `GET /api/grupos` · `GET /api/grupos/{id}` | `grupos.ver` o `miembros.ver` | 200 (con `cantidadMiembros`) · 404 |
| `POST /api/grupos` · `PUT /api/grupos/{id}` | `grupos.administrar` | 201/200 · 400 · 404 · 409 |
| `PATCH /api/grupos/{id}/estado` | `grupos.administrar` | 200 · 404 · 409 (tiene miembros activos) |
| `GET /api/configuracion-iglesia` | autenticado con iglesia | 200 `{ nombreGrupoSingular, nombreGrupoPlural }` |
| `PUT /api/configuracion-iglesia` | `grupos.administrar` | 200 · 400 |
| `GET /api/ubigeo/departamentos` · `/departamentos/{codigo}/provincias` · `/provincias/{codigo}/distritos` | autenticado | 200 ordenados por nombre · 404 |

Todo con OpenAPI completo (`@Tag`, `@Operation`, `@ApiResponse` con `ProblemDetail`, `@Schema` y `@Parameter`) y logs `INFO` con identificadores, nunca nombres, teléfonos ni correos.

### Backend `iglesia-seguridad`

- `V5__permisos_de_miembros_niveles_y_grupos.sql` (arriba) y las constantes nuevas en `Permisos`.
- `GET /api/usuarios` acepta el filtro `miembroId`, para que la ficha sepa si el miembro ya tiene acceso.
- `POST /api/usuarios` ya acepta `miembroId` y lo hace único por iglesia (409). Se confirma con pruebas que permite los roles que exigen miembro cuando `miembroId` viene informado.

### Frontend (`iglesia-web/src/app/`)

- **`features/miembros/`** (reemplaza el código heredado):

| Pieza | Responsabilidad |
| --- | --- |
| `pages/miembros-lista` | Contenedor:<br>- tabla paginada en el servidor;<br>- búsqueda por nombre y filtros de condición, nivel, grupo, líder y estado;<br>- «Nuevo miembro» solo con `miembros.crear`. |
| `pages/miembro-ficha` | Contenedor de `/miembros/nuevo` y `/miembros/:id`:<br>- secciones Datos personales, Contacto y ubicación, En la iglesia, Estudios y trabajo, y Petición;<br>- solo lectura sin `miembros.editar`;<br>- acciones «Desactivar/Reactivar», «Designar pastor principal» y «Dar acceso al sistema». |
| `components/formulario-miembro` | Formulario reactivo tipado. Solo nombres y apellidos son obligatorios. Fecha de nacimiento o edad aproximada (excluyentes). |
| `components/tabla-miembros` | Nombre, condición, nivel, líder, grupo, teléfono y estado. Etiqueta «Pastor principal». Esqueleto y vacío. |
| `components/cadena-liderazgo` | Cadena del miembro hasta el pastor y «lidera a N». |
| `components/progreso-ficha` | Porcentaje de la ficha completado y lista de datos que faltan. |
| `components/dar-acceso` | `p-dialog`: correo (precargado con el del miembro), contraseña temporal y rol `Líder` o `Miembro`. |
| `services/miembros.service.ts` y mapper | Fachada HTTP de `/api/miembros`. |
| `miembros.routes.ts` | `''` con `permisoGuard('miembros.ver')`, `nuevo` con `miembros.crear` y `:id` con `miembros.ver`. |

- **`features/niveles/`:** `/niveles` con tabla ordenada, botones subir y bajar, y diálogo para crear o renombrar. Exige `niveles.administrar`.
- **`features/grupos/`:**
  - `/grupos` con tabla y diálogo de grupo (nombre, líder, anfitrión, día, hora, dirección, ubigeo y descripción);
  - diálogo «Cómo se llaman los grupos» (singular y plural);
  - ver exige `grupos.ver` y editar, `grupos.administrar`.
- **`core/configuracion/configuracion-iglesia.service.ts`:** carga el nombre de los grupos al entrar al layout. Lo usan el menú, los títulos y las etiquetas.
- **`shared/components/`:**
  - `selector-miembro`: `p-autoComplete` contra `/api/miembros/opciones`, con opción de excluir un id;
  - `selector-ubigeo`: tres `p-select` en cascada que emiten el código de distrito.
- **`features/usuarios/services/usuarios.service.ts`:** `crearParaMiembro(miembroId, datos)` envía `miembroId`.
- **`layout/component/app.menu.ts`:**
  - en «Iglesia», «Miembros» y «<nombre plural de grupos>» (`grupos.ver`, `pi-sitemap`);
  - en «Administración», «Niveles» (`niveles.administrar`, `pi-sort-amount-up`);
  - todo con `requiereIglesia`.
- **Rutas en `app.routes.ts`:** `/niveles` y `/grupos` con `iglesiaFijadaGuard`.

## Plan de implementación

**Backend `iglesia-seguridad`**

1. `V5__permisos_de_miembros_niveles_y_grupos.sql` y las constantes nuevas en `Permisos`. Pruebas: `ADMIN_IGLESIA` tiene los permisos nuevos y `LIDER` tiene `miembros.ver`. Actualizar los conteos del catálogo en `RolIntegrationTest`.
2. Filtro `miembroId` en `GET /api/usuarios` y prueba de integración de `POST /api/usuarios` con `miembroId` y rol `LIDER`, incluido el 409 por miembro repetido. OpenAPI y Postman de seguridad.

**Backend `iglesia-api`**

3. `V4__ubigeo_peru.sql` con los datos del INEI y el contexto `ubigeo` (entidades, servicio y controlador). Pruebas de integración de los tres endpoints.
4. `V5__registro_de_miembros.sql`. Prueba de integración: la RLS aísla `miembro`, `nivel`, `grupo` y `configuracion_iglesia`, y las FK compuestas impiden referenciar filas de otra iglesia.
5. Contexto `niveles` completo (CRUD, orden y estado). Pruebas unitarias del servicio y de integración de los endpoints.
6. `UsuarioActual.miembroId()` y `rolesBase()`. Contexto `miembros`, primera parte:
   - alta, edición, detalle, listado con filtros y `posibles-duplicados`;
   - reglas de la ficha, incluida la edad.
   - Pruebas unitarias y de integración.
7. Jerarquía:
   - `PoliticaJerarquia` (ciclos con CTE recursiva filtrada por iglesia, pastor principal y bloqueos al desactivar);
   - `PATCH /estado`, `PUT /pastor-principal` y `GET /cadena`;
   - pruebas, incluida la de ciclo indirecto (A lidera a B, B a C, y C no puede liderar a A).
8. `AmbitoMiembros`: el líder ve su red, a sí mismo y su cadena; otros roles sin administración no ven nada. Aplicarlo al listado, al detalle, a la cadena y a las opciones. Pruebas de integración con tokens de `ADMIN_IGLESIA`, `LIDER` y del administrador del sistema dentro de una iglesia.
9. Contexto `grupos` y `configuracion-iglesia`: CRUD, estado (409 con miembros activos) y nombres de grupo con su valor por defecto. Pruebas.
10. Colección de Postman de `iglesia-api`:
    - carpetas «2. Ubigeo», «3. Niveles», «4. Grupos y configuración» y «5. Miembros»;
    - renumerar las existentes;
    - variables de entorno nuevas sin datos sensibles;
    - ejecutar con newman.

**Frontend `iglesia-web`**

11. Componentes compartidos `selector-ubigeo` y `selector-miembro`, con sus specs.
12. `core/configuracion` y `features/niveles` (servicio, tabla, diálogo, página y rutas), con specs.
13. `features/grupos` (servicio, tabla, diálogo de grupo, diálogo de nombres, página y rutas), con specs.
14. `features/miembros`, primera parte: eliminar el código heredado, modelos, mapper, servicio, `tabla-miembros`, `miembros-lista` y rutas, con specs.
15. `features/miembros`, segunda parte:
    - `formulario-miembro`, `progreso-ficha`, `cadena-liderazgo` y `miembro-ficha` (alta con aviso de duplicados, edición, solo lectura, desactivar y pastor principal);
    - specs.
16. `dar-acceso` y `UsuariosService.crearParaMiembro`: la ficha muestra «Acceso: <correo>» o el botón. Specs.
17. Menú (`app.menu.ts`) y `app.routes.ts` con las entradas y guards nuevos. Specs de menú y rutas.

**Cierre**

18. Crear `diagramas/spec-06-registro-de-miembros.drawio` con varias páginas, colores con leyenda y notas:
    - arquitectura (componentes, servicios, endpoints, clases y tablas);
    - alta de un miembro con el aviso de duplicados;
    - jerarquía: asignar líder, validar ciclos y designar pastor principal;
    - ámbito del líder (red + cadena) con las CTE;
    - dar acceso al sistema entre los dos backends.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Después de `V5`, `ADMIN_IGLESIA` tiene `niveles.administrar`, `grupos.ver` y `grupos.administrar`, y `LIDER` tiene `miembros.ver`.
- [ ] `GET /api/usuarios?miembroId=5` devuelve solo el usuario vinculado a ese miembro, o una página vacía.
- [ ] `POST /api/usuarios` con `miembroId` y el rol `LIDER` crea el usuario (201), y su token trae `miembro_id`. Repetir el mismo `miembroId` responde 409.

**Backend `iglesia-api`**

- [ ] `GET /api/ubigeo/departamentos` devuelve los 25 departamentos. `GET /api/ubigeo/provincias/1501/distritos` devuelve los distritos de Lima.
- [ ] `POST /api/miembros` con solo `nombres` y `apellidos` responde 201. Sin uno de los dos responde 400.
- [ ] Con `fechaNacimiento` la respuesta trae la edad exacta. Con `edadAproximada: 30` trae `edad: 30` y `edadEsAproximada: true`. Enviar las dos responde 400.
- [ ] Una fecha de nacimiento o de ingreso futura responde 400.
- [ ] `posibles-duplicados` encuentra a «Juan Pérez» al buscar `juan` / `PÉREZ ` (sin distinguir mayúsculas ni espacios sobrantes).
- [ ] Asignar como líder a alguien de la propia red (ciclo directo o indirecto) responde 409 y no cambia nada.
- [ ] Asignarse a sí mismo como líder, invitador o registrador responde 400.
- [ ] `PUT /pastor-principal` deja un solo pastor en la iglesia, le quita el líder al nuevo y la marca al anterior. Asignarle un líder al pastor responde 409.
- [ ] Desactivar a quien lidera miembros activos o un grupo activo, o al pastor principal, responde 409. Reactivar funciona.
- [ ] Un nivel, grupo o miembro inactivo no se puede asignar (409). Los ya asignados se conservan y se muestran como inactivos.
- [ ] `GET /api/miembros/{id}/cadena` devuelve los líderes en orden, desde el directo hasta el pastor.
- [ ] Con un token `LIDER` vinculado al miembro L:
  - el listado devuelve a L, a su red completa y a sus superiores, y nada más;
  - el detalle de otro miembro responde 404;
  - `POST` y `PUT` responden 403.
- [ ] Con un token de la iglesia 1, ningún endpoint devuelve ni acepta niveles, grupos o miembros de la iglesia 2 (404 o 409), aunque se envíen sus ids.
- [ ] Niveles: el nombre repetido sin distinguir mayúsculas responde 409, y `PUT /orden` con una lista incompleta responde 400.
- [ ] Desactivar un grupo con miembros activos responde 409.
- [ ] `GET /api/configuracion-iglesia` devuelve «Grupo»/«Grupos» si la iglesia no lo configuró, y el valor guardado después de `PUT`.
- [ ] Todos los errores devuelven `ProblemDetail` con `traceId` y la cabecera `X-Trace-Id`. Los logs `INFO` de alta y cambio no incluyen nombres, teléfonos ni correos.
- [ ] Swagger documenta todos los endpoints nuevos, y las colecciones de Postman pasan con newman.

**Frontend**

- [ ] «Guardar» de un miembro nuevo se habilita con solo nombres y apellidos. La ficha guardada muestra el progreso y los datos que faltan.
- [ ] Si existe alguien con los mismos nombres y apellidos, se pide confirmar antes de crear.
- [ ] Departamento, provincia y distrito se eligen en cascada, y cambiar el departamento limpia los otros dos.
- [ ] Al escribir una fecha de nacimiento se deshabilita la edad aproximada, y al revés.
- [ ] El selector de líder no ofrece al propio miembro. Si el backend rechaza un ciclo, el mensaje sale junto al campo.
- [ ] «Designar pastor principal» pide confirmar y, si ya había pastor, nombra en el aviso a quién reemplaza.
- [ ] «Dar acceso al sistema» crea el usuario vinculado. La ficha pasa a mostrar «Acceso: <correo>» y el botón desaparece.
- [ ] Un usuario `LIDER` ve en `/miembros` solo su red y sus superiores, sin «Nuevo miembro» y con las fichas en solo lectura.
- [ ] `/niveles` permite crear, renombrar, subir, bajar y desactivar. El orden se refleja en los selectores de nivel.
- [ ] `/grupos` y el menú usan el nombre configurado (p. ej. «Hogares de paz»).
- [ ] Todas las pantallas nuevas tienen estados de carga, vacío y error, funcionan en modo claro y oscuro y en móvil, y tienen sus pruebas `.spec.ts`. `ng test` pasa.
- [ ] Existe `diagramas/spec-06-registro-de-miembros.drawio` con las páginas del paso 18.

## Decisiones

- **Sí:** un único registro de miembros con la condición `ANTIGUO`, `NUEVO` o `RECUPERADO`. **No:** módulos separados de miembros actuales y nuevos: es la misma persona en dos momentos, y separarlos duplicaría tablas y pantallas.
- **Sí:** solo nombres y apellidos son obligatorios, y la ficha muestra qué falta. Las iglesias reciben los datos por partes.
- **Sí:** una sola spec con miembros, niveles, grupos, jerarquía, visibilidad del líder y acceso al sistema, por decisión del usuario, aunque es grande. Se mitiga con un plan de 18 pasos que dejan el sistema funcionando en cada uno.
- **Sí:** «Quién lo registró» es un miembro opcional, elegido a mano. **No** se guarda el usuario del token, por decisión del usuario. Esto cambia lo que dice CLAUDE.md («se toma del token»), que conviene actualizar.
- **Sí:** edad aproximada guardada como año de nacimiento estimado, para que siga siendo correcta con el tiempo. **No:** guardar la edad.
- **Sí:** el nivel de estudios y el centro de estudios son visibles y opcionales desde ya. El nivel de estudios es una lista fija (educación formal), distinta de los niveles académicos configurables del módulo 7.
- **Sí:** el ubigeo oficial del INEI es un catálogo común sin `iglesia_id`, la excepción a «catálogos por iglesia», porque la geografía no depende de la iglesia. **No:** texto libre, que genera datos inconsistentes.
- **Sí:** el líder es opcional y único, el pastor principal es único y sin líder, y los ciclos se validan con CTE recursivas filtradas por iglesia. El índice único parcial y los `CHECK` respaldan la regla en la base. Cambiar el líder mueve la red completa de forma natural, porque cada uno apunta solo a su líder directo.
- **Sí:** designar al pastor principal transfiere la marca y le quita el líder al nuevo pastor.
- **Sí:** el grupo del miembro es independiente de su líder.
- **Sí:** no se puede desactivar a quien lidera miembros activos o un grupo activo, ni al pastor principal. Así nadie queda colgado de un líder inactivo.
- **Sí:** el ámbito del líder es su red, él mismo y su cadena superior, en solo lectura. Se decide por `roles` y `miembro_id` del token. `LIDER` recibe `miembros.ver` para entrar a `/miembros`.
- **Sí:** el vínculo usuario–miembro se crea desde la ficha («Dar acceso al sistema»), y el frontend orquesta la llamada a `iglesia-seguridad`. Los backends siguen sin comunicarse entre sí. `iglesia-seguridad` no valida que el miembro exista (bases separadas) y se acepta, igual que la iglesia en la SPEC 05.
- **Sí:** permisos nuevos `niveles.administrar`, `grupos.ver` y `grupos.administrar`. **No:** reutilizar `miembros.editar`, porque no permitiría repartirlos en roles personalizados.
- **Sí:** los avisos de duplicados no bloquean, porque dos personas pueden llamarse igual.
- **Sí:** el correo del miembro no es único (familias) y no es el de inicio de sesión: «Dar acceso» lo propone, pero se puede cambiar.
- **No:** borrar miembros, niveles o grupos. Solo se desactivan, para no romper referencias ni el historial de módulos futuros.
- **Sí:** se elimina el paquete vacío `nuevosmiembros` y el código heredado de `features/miembros`.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| La spec es grande y puede tardar en cerrarse. | Pasos que dejan el sistema funcionando cada uno, con revisión después de cada paso. El backend se termina antes que el frontend. |
| Los datos de ubigeo del INEI tienen cambios (distritos nuevos) o son difíciles de conseguir en un formato limpio. | Se versionan en una migración propia (`V4`). Una actualización futura será otra migración. La fuente se cita en el comentario del SQL. |
| Las CTE recursivas del ámbito pesan en iglesias grandes. | Índice `(iglesia_id, lider_id)`, red calculada una sola vez por petición y paginación en la base. Se mide con `EXPLAIN` en el paso 8. |
| Un líder con varios perfiles (líder y administrador) ve más de lo esperado al elegir el perfil Líder. | El ámbito lo decide el token (unión de roles), no el perfil activo del frontend: con `ADMIN_IGLESIA` ve toda la iglesia. Se documenta en la ficha de decisiones del diagrama. |
| «Dar acceso» crea un usuario vinculado a un miembro que después se desactiva. | La ficha lo muestra. Desactivar el usuario sigue siendo manual en `/usuarios` (sincronizar ambos backends queda fuera de alcance). |
| La FK circular entre `miembro` y `grupo` complica la migración y las pruebas. | La FK del grupo del miembro se agrega al final de `V5`. Las pruebas crean primero los miembros y después los grupos. |
| El nombre configurable de los grupos llega tarde y el menú parpadea. | Valor por defecto «Grupos» hasta que llegue la configuración. |

## Qué **no** entra en esta spec

- Vistas de árbol y totales de la red completa.
- Que un líder registre o edite personas.
- Portal del miembro.
- Vincular o desvincular un usuario existente con un miembro desde `/usuarios`.
- Seguimiento y consolidación, asistencia, ofrendas y académico.
- Historial de cambios de líder, nivel o grupo.
- Importación desde Excel, foto y documento de identidad.
- Ubigeo de otros países.

Cada uno, si llega, va en su propia spec.
