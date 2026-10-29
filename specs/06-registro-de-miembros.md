# SPEC 06 — Registro de miembros

> **Estado:** Implementado
> **Depende de:** SPEC 02 (permisos y roles base), SPEC 03 (OpenAPI y trazabilidad), SPEC 04 (administración de usuarios y contraseña temporal), SPEC 05 (acceso global del administrador del sistema)
> **Fecha:** 2026-10-28 · **Revisión 2:** 2026-10-29 (ver «Cambios de la revisión 2»)
> **Objetivo:** Que cada iglesia registre y vaya completando la ficha de todos sus congregantes (consolidados, nuevos y recuperados) con su nivel, su líder, su grupo y su ubicación, administre sus niveles y sus grupos, y que cada líder vea su red y a sus superiores.

## Por qué existe esta spec

Hoy `iglesia-api` no tiene miembros: los paquetes `miembros` y `nuevosmiembros` están vacíos y no hay tablas. En el frontend, `features/miembros` es un resto de la plantilla que no cumple las reglas del proyecto (plantilla inline, sin `OnPush`, sin pruebas). Sin miembros no se puede construir ningún otro módulo de la hoja de ruta: el miembro es la entidad central.

Las iglesias registran a la gente por partes: primero solo el nombre y después el resto, cuando lo consiguen. Por eso el único dato obligatorio son los **nombres y apellidos**. Todo lo demás se completa con el tiempo.

Esta spec junta en un solo registro lo que la hoja de ruta separaba en «miembros actuales» y «nuevos miembros»: es la misma persona en dos momentos, y la **condición** (consolidado, nuevo o recuperado) basta para distinguirlas.

## Cambios de la revisión 2 (2026-10-29)

Pedidos por el usuario al probar la primera implementación. Están incorporados en las secciones de abajo y se implementan en los pasos 19 a 27.

1. **Edad y fecha de nacimiento independientes:** se pueden llenar las dos y se guardan tal como se escriben.
2. **Grupos sin nombre:** un grupo es «el grupo de» su líder, que pasa a ser obligatorio y único (un grupo por líder).
3. **Miembros del grupo con drag and drop** en `/grupos/:id`, desde todos los miembros activos (moverlo desde otro grupo pide confirmación).
4. **Condición `ANTIGUO` → `CONSOLIDADO`.**
5. **Árbol de liderazgo en la ficha:** superiores arriba y liderados abajo, con su nivel. Reemplaza a `cadena-liderazgo`.
6. **Listado de miembros:** sin columna ni filtro de grupo; columna «Dirección» (solo el campo dirección) y columna «Región» (distrito, provincia y departamento en un solo texto, armada en la grilla del frontend), y filtros de departamento, provincia y distrito. La tabla de grupos usa las mismas dos columnas.

## Alcance

**Entra:**

- **Registro de miembros (`/miembros`):** listado paginado con búsqueda y filtros, alta, edición, desactivación y reactivación.
  - Solo nombres y apellidos son obligatorios. La ficha indica qué datos faltan.
  - Al dar de alta avisa si ya existe alguien con los mismos nombres y apellidos, y deja confirmar.
- **Datos de la ficha:**
  - nombres, apellidos, género, fecha de nacimiento y edad;
  - fecha de ingreso a la iglesia y condición (consolidado, nuevo o recuperado);
  - dirección, con departamento, provincia y distrito del ubigeo;
  - teléfono, correo, estado civil, ocupación y profesión;
  - nivel de estudios y centro de estudios;
  - nivel, líder y grupo;
  - quién lo invitó, quién lo registró y petición.
- **Edad:** fecha de nacimiento y edad son campos independientes: se pueden llenar uno, otro o los dos, y se guardan tal como se escriben, aunque no coincidan. La edad escrita se guarda como año de nacimiento estimado (para que siga siendo correcta con el tiempo); la edad exacta se calcula de la fecha y no se guarda.
- **Jerarquía de liderazgo:**
  - cada miembro tiene a lo sumo un líder (opcional);
  - no se permiten ciclos;
  - la acción «Designar pastor principal» marca al único pastor principal de la iglesia, que no tiene líder;
  - la ficha muestra un **árbol de liderazgo**: arriba sus superiores hasta el pastor y abajo las personas que lidera (directas, y al expandir, las de cada una), cada una con su nivel.
- **Visibilidad del líder:** un usuario con rol base `LIDER` ve, en solo lectura, a las personas de su red (directas e indirectas), a sí mismo y a sus superiores hasta el pastor.
- **Acceso al sistema desde la ficha:** la acción «Dar acceso al sistema» crea un usuario en `iglesia-seguridad` vinculado a ese miembro, con rol `Líder` o `Miembro` y una contraseña temporal. La ficha muestra si el miembro ya tiene acceso.
- **Niveles (`/niveles`):** catálogo por iglesia para crear, renombrar, ordenar, desactivar y reactivar niveles.
- **Grupos (`/grupos`):**
  - un grupo **no tiene nombre**: es «el grupo de» su líder. El líder es obligatorio y cada líder tiene a lo sumo un grupo. Se muestra como «<nombre singular> de <líder>» (p. ej. «Célula de Ana Pérez»);
  - mantenimiento por iglesia con líder, anfitrión, día y hora de reunión, dirección con ubigeo y descripción;
  - **miembros del grupo (`/grupos/:id`):** se agregan y quitan arrastrando (drag and drop) entre «Disponibles» y «Miembros del grupo», con botones equivalentes para teclado. En «Disponibles» aparecen todos los miembros activos que no están en el grupo, con su grupo actual; agregar a alguien que ya está en otro grupo lo mueve, después de confirmar;
  - cada iglesia configura cómo se llaman sus grupos (p. ej. «Célula» o «Hogar de paz», en singular y plural).
  - El líder del miembro y el grupo del miembro son independientes: alguien puede tener un líder y estar en el grupo de otro.
- **Ubigeo del Perú:** catálogo oficial INEI (departamentos, provincias y distritos), común a todas las iglesias, con selectores en cascada.
- **Permisos nuevos** en `iglesia-seguridad`: `niveles.administrar`, `grupos.ver` y `grupos.administrar`, asignados a `ADMIN_IGLESIA`. Además, `LIDER` recibe `miembros.ver`.
- **Piezas reutilizables:** selector de miembro, selector de ubigeo, OpenAPI, Postman, pruebas y diagrama.

**Fuera de alcance (para specs futuras):**

- Vista de árbol de la iglesia completa fuera de la ficha, y totales de la red completa.
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

### Migración `iglesia-api/src/main/resources/db/migration/V6__ajustes_de_miembros_y_grupos.sql` (revisión 2)

V5 ya se aplicó en las bases de desarrollo, así que los ajustes van en una migración nueva (CLAUDE.md: nunca DDL manual ni editar una migración aplicada). Los nombres de las restricciones automáticas se confirman en la base antes de escribirla.

```sql
-- 1. Fecha de nacimiento y edad aproximada son independientes.
ALTER TABLE miembro DROP CONSTRAINT miembro_check2;  -- CHECK (fecha_nacimiento IS NULL OR anio_nacimiento_estimado IS NULL)

-- 4. ANTIGUO pasa a llamarse CONSOLIDADO.
ALTER TABLE miembro DROP CONSTRAINT miembro_condicion_check;
UPDATE miembro SET condicion = 'CONSOLIDADO' WHERE condicion = 'ANTIGUO';
ALTER TABLE miembro ADD CONSTRAINT miembro_condicion_check CHECK (condicion IN ('CONSOLIDADO', 'NUEVO', 'RECUPERADO'));

-- 2. El grupo no tiene nombre: es el grupo de su líder (obligatorio y único).
--    Solo las bases de desarrollo tienen grupos sin líder o con líder repetido: se sacan sus miembros y se eliminan,
--    conservando el grupo más antiguo de cada líder.
UPDATE miembro SET grupo_id = NULL WHERE grupo_id IN (
    SELECT id FROM grupo g WHERE g.lider_id IS NULL
       OR EXISTS (SELECT 1 FROM grupo o WHERE o.iglesia_id = g.iglesia_id AND o.lider_id = g.lider_id AND o.id < g.id));
DELETE FROM grupo g WHERE g.lider_id IS NULL
   OR EXISTS (SELECT 1 FROM grupo o WHERE o.iglesia_id = g.iglesia_id AND o.lider_id = g.lider_id AND o.id < g.id);
DROP INDEX ux_grupo_nombre;
ALTER TABLE grupo DROP COLUMN nombre;
ALTER TABLE grupo ALTER COLUMN lider_id SET NOT NULL;
CREATE UNIQUE INDEX ux_grupo_lider ON grupo (iglesia_id, lider_id);

-- 6. Filtro del listado por departamento, provincia o distrito (prefijo del código de distrito).
CREATE INDEX ix_miembro_distrito ON miembro (iglesia_id, distrito_codigo);
```

El `UPDATE` y el `DELETE` corren con el rol dueño de Flyway, que no está sujeto a la RLS, así que alcanzan a todas las iglesias.

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
public enum CondicionMiembro { CONSOLIDADO, NUEVO, RECUPERADO }
public enum EstadoCivil { SOLTERO, CASADO, DIVORCIADO, VIUDO }
public enum NivelEstudios { SIN_ESTUDIOS, PRIMARIA, SECUNDARIA, TECNICO, UNIVERSITARIO, POSGRADO }
// com.iglesia.api.grupos.model
public enum DiaSemana { LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO }
```

### Reglas de la ficha

| Dato | Regla |
| --- | --- |
| `nombres`, `apellidos` | Obligatorios, sin espacios al inicio ni al final, máx. 100. |
| `fechaNacimiento` | Opcional, no futura. Independiente de `edadAproximada`. |
| `edadAproximada` | Opcional, entre 0 y 120. Independiente de `fechaNacimiento` (se pueden enviar las dos aunque no coincidan). Se guarda como `anio_nacimiento_estimado = año actual − edad`. |
| `edad` (respuesta) | Con fecha: exacta, calculada de la fecha. Sin fecha y con año estimado: año actual − año, con `edadEsAproximada: true`. Sin ninguno: `null`. |
| `edadAproximada` (respuesta) | Año actual − año estimado, o `null`. Se devuelve aparte para que el formulario muestre lo que se escribió. |
| `fechaIngreso` | Opcional, no futura. |
| `telefono` | Opcional: dígitos, espacios, `+` y `-`, máx. 20. |
| `correo` | Opcional, formato de correo, máx. 254. **No** es único (una familia puede compartirlo). |
| `distritoCodigo` | Opcional, debe existir en `ubigeo_distrito`. Departamento y provincia se deducen del distrito. |
| `nivelId`, `grupoId` | Opcionales, de la misma iglesia y activos al asignarlos. |
| `liderId` del grupo | Obligatorio, miembro activo de la misma iglesia, sin otro grupo (409 «Ese líder ya tiene un grupo»). |
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
| `GET /api/miembros?q&condicion&nivelId&liderId&ubigeo&activo&page&size&sort` | `miembros.ver` (filtrado por ámbito) | 200 `Page<MiembroResumenResponse>`. `ubigeo` es un código de 2, 4 o 6 dígitos (departamento, provincia o distrito; otro formato → 400). El resumen trae `direccion` y `ubicacion` (departamento, provincia y distrito), y ya no trae el grupo. |
| `GET /api/miembros/{id}` | `miembros.ver` (ámbito) | 200 `MiembroResponse` · 404 |
| `GET /api/miembros/{id}/cadena` | `miembros.ver` (ámbito) | 200: lista del líder directo al pastor, cada uno con su nivel · 404 |
| `GET /api/miembros/{id}/liderados` | `miembros.ver` (ámbito) | 200: liderados directos activos `{ id, nombreCompleto, nivel, cantidadLiderados }`, ordenados por apellidos · 404. Alimenta el árbol de la ficha, que expande un nodo cada vez. |
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
| `GET /api/grupos` · `GET /api/grupos/{id}` | `grupos.ver` o `miembros.ver` | 200 (con `lider` y `cantidadMiembros`, sin `nombre`) · 404 |
| `GET /api/grupos/{id}/miembros` | `grupos.ver` | 200: miembros activos del grupo `{ id, nombreCompleto, nivel }` · 404 |
| `GET /api/grupos/{id}/candidatos?q` | `grupos.administrar` | 200: hasta 20 miembros activos que no están en el grupo, con su grupo actual `{ id, nombreCompleto, grupo: { id, lider } \| null }` · 404 |
| `PUT /api/grupos/{id}/miembros/{miembroId}` | `grupos.administrar` | 200: lo agrega o lo mueve desde otro grupo · 404 · 409 (grupo o miembro inactivo) |
| `DELETE /api/grupos/{id}/miembros/{miembroId}` | `grupos.administrar` | 204: lo quita del grupo · 404 (no existe o no está en ese grupo) |
| `POST /api/grupos` · `PUT /api/grupos/{id}` | `grupos.administrar` | 201/200 · 400 (sin líder) · 404 · 409 (el líder ya tiene grupo o está inactivo) |
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
| `pages/miembros-lista` | Contenedor:<br>- tabla paginada en el servidor;<br>- búsqueda por nombre y filtros de condición, nivel, líder, departamento, provincia, distrito y estado;<br>- «Nuevo miembro» solo con `miembros.crear`. |
| `pages/miembro-ficha` | Contenedor de `/miembros/nuevo` y `/miembros/:id`:<br>- secciones Datos personales, Contacto y ubicación, En la iglesia, Estudios y trabajo, y Petición;<br>- solo lectura sin `miembros.editar`;<br>- acciones «Desactivar/Reactivar», «Designar pastor principal» y «Dar acceso al sistema». |
| `components/formulario-miembro` | Formulario reactivo tipado. Solo nombres y apellidos son obligatorios. Fecha de nacimiento y edad aproximada independientes (se pueden llenar las dos). El grupo se elige como «<singular> de <líder>». |
| `components/tabla-miembros` | Nombre, condición, nivel, líder, dirección (dirección, distrito, provincia y departamento en una sola columna; «—» si no hay), teléfono y estado. Etiqueta «Pastor principal». Esqueleto y vacío. |
| `components/arbol-liderazgo` | `p-tree` de la plantilla (estilo Sakai) con los superiores (del pastor hacia abajo) hasta el miembro, resaltado, y debajo sus liderados. Cada nodo muestra avatar con iniciales, nombre, relación (líder directo, superior, liderado) con su nivel y un contador «lidera a N». Los liderados se cargan al expandir (`/liderados`). Al elegir un nodo (clic o Enter) se abre su ficha. Reemplaza a `cadena-liderazgo`. |
| `components/progreso-ficha` | Porcentaje de la ficha completado y lista de datos que faltan. |
| `components/dar-acceso` | `p-dialog`: correo (precargado con el del miembro), contraseña temporal y rol `Líder` o `Miembro`. |
| `services/miembros.service.ts` y mapper | Fachada HTTP de `/api/miembros`. |
| `miembros.routes.ts` | `''` con `permisoGuard('miembros.ver')`, `nuevo` con `miembros.crear` y `:id` con `miembros.ver`. |

- **`features/niveles/`:** `/niveles` con tabla ordenada, botones subir y bajar, y diálogo para crear o renombrar. Exige `niveles.administrar`.
- **`features/grupos/`:**
  - `/grupos` con tabla (grupo «<singular> de <líder>», anfitrión, reunión, ubicación y cantidad de miembros) y diálogo de grupo (líder obligatorio, anfitrión, día, hora, dirección, ubigeo y descripción);
  - `pages/grupo-miembros` en `/grupos/:id` (`grupos.ver`; editar con `grupos.administrar`): dos listas, «Disponibles» con búsqueda en el servidor y «Miembros del grupo», con arrastrar y soltar (`pDraggable`/`pDroppable` de PrimeNG) y botones «Agregar»/«Quitar» para teclado y móvil. Cada disponible muestra una etiqueta con su grupo actual («En célula de Beto Soto») o «Sin grupo». Mover a alguien de otro grupo (arrastrando o con el botón) avisa «ya está en <grupo>, ¿deseas moverlo a <este grupo>?» y solo lo mueve si se confirma;
  - diálogo «Cómo se llaman los grupos» (singular y plural);
  - ver exige `grupos.ver` y editar, `grupos.administrar`.
- **`core/configuracion/configuracion-iglesia.service.ts`:** carga el nombre de los grupos al entrar al layout. Lo usan el menú, los títulos y las etiquetas.
- **`shared/components/`:**
  - `selector-miembro`: `p-autoComplete` contra `/api/miembros/opciones`, con opción de excluir un id;
  - `selector-ubigeo`: tres `p-select` en cascada que emiten el código de distrito. Con la opción `parcial` emite el código del nivel más profundo elegido (2, 4 o 6 dígitos), para los filtros.
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

**Revisión 2**

19. `V6__ajustes_de_miembros_y_grupos.sql`. Confirmar antes los nombres de las restricciones automáticas en la base. Prueba de integración: la condición `CONSOLIDADO`, el índice único `(iglesia_id, lider_id)` del grupo y que se aceptan fecha y año estimado juntos.
20. Miembros en el backend:
    - edad y fecha independientes (`edadAproximada` aparte en la respuesta);
    - `CONSOLIDADO`;
    - filtro `ubigeo` (sin `grupoId`) y resumen con dirección y ubicación;
    - nivel en la cadena y `GET /liderados`, con el ámbito del líder.
    - Pruebas unitarias y de integración.
21. Grupos en el backend: sin nombre, líder obligatorio y único, y `miembros`, `candidatos`, `PUT`/`DELETE` de miembros del grupo. Pruebas.
22. Postman (carpetas de miembros y grupos), OpenAPI y newman.
23. Frontend de miembros: `CONSOLIDADO`, edad y fecha independientes, columnas «Dirección» y «Región» y filtros de ubigeo (selector con `parcial`), sin grupo en la tabla ni en los filtros. Specs.
24. `arbol-liderazgo` en la ficha, en lugar de `cadena-liderazgo`. Specs.
25. Grupos en el frontend: tabla y diálogo sin nombre y con líder obligatorio, y el selector de grupo del formulario de miembro como «<singular> de <líder>». Specs.
26. `grupo-miembros` con arrastrar y soltar, botones equivalentes y confirmación al mover. Ruta `/grupos/:id`. Specs.
27. Actualizar `diagramas/spec-06-registro-de-miembros.drawio`: grupos sin nombre, miembros del grupo y árbol de la ficha.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Después de `V5`, `ADMIN_IGLESIA` tiene `niveles.administrar`, `grupos.ver` y `grupos.administrar`, y `LIDER` tiene `miembros.ver`.
- [ ] `GET /api/usuarios?miembroId=5` devuelve solo el usuario vinculado a ese miembro, o una página vacía.
- [ ] `POST /api/usuarios` con `miembroId` y el rol `LIDER` crea el usuario (201), y su token trae `miembro_id`. Repetir el mismo `miembroId` responde 409.

**Backend `iglesia-api`**

- [ ] `GET /api/ubigeo/departamentos` devuelve los 25 departamentos. `GET /api/ubigeo/provincias/1501/distritos` devuelve los distritos de Lima.
- [ ] `POST /api/miembros` con solo `nombres` y `apellidos` responde 201. Sin uno de los dos responde 400.
- [ ] Con `fechaNacimiento` la respuesta trae la edad exacta. Con `edadAproximada: 30` trae `edad: 30` y `edadEsAproximada: true`. Enviar las dos responde 201 y guarda ambas: la respuesta trae la edad exacta de la fecha y `edadAproximada: 30`.
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
- [ ] Crear un grupo sin líder responde 400, y con un líder que ya tiene grupo responde 409. La respuesta del grupo no trae `nombre`.
- [ ] `PUT /api/grupos/{id}/miembros/{miembroId}` agrega al miembro o lo mueve desde otro grupo; `DELETE` lo quita. `candidatos` no incluye a los que ya están en el grupo y trae el grupo actual de los demás.
- [ ] Después de `V6`, los miembros que tenían `ANTIGUO` tienen `CONSOLIDADO`, y `condicion=ANTIGUO` responde 400.
- [ ] `GET /api/miembros?ubigeo=15` devuelve solo los de Lima (departamento); `ubigeo=1501` y `ubigeo=150122` filtran por provincia y distrito; `ubigeo=1` responde 400.
- [ ] `GET /api/miembros/{id}/liderados` devuelve los liderados directos con su nivel y cuántos lidera cada uno. Con token `LIDER` responde 404 para alguien fuera de su ámbito.
- [ ] `GET /api/configuracion-iglesia` devuelve «Grupo»/«Grupos» si la iglesia no lo configuró, y el valor guardado después de `PUT`.
- [ ] Todos los errores devuelven `ProblemDetail` con `traceId` y la cabecera `X-Trace-Id`. Los logs `INFO` de alta y cambio no incluyen nombres, teléfonos ni correos.
- [ ] Swagger documenta todos los endpoints nuevos, y las colecciones de Postman pasan con newman.

**Frontend**

- [ ] «Guardar» de un miembro nuevo se habilita con solo nombres y apellidos. La ficha guardada muestra el progreso y los datos que faltan.
- [ ] Si existe alguien con los mismos nombres y apellidos, se pide confirmar antes de crear.
- [ ] Departamento, provincia y distrito se eligen en cascada, y cambiar el departamento limpia los otros dos.
- [ ] Fecha de nacimiento y edad aproximada se pueden llenar a la vez, y la ficha muestra las dos.
- [ ] El selector de líder no ofrece al propio miembro. Si el backend rechaza un ciclo, el mensaje sale junto al campo.
- [ ] «Designar pastor principal» pide confirmar y, si ya había pastor, nombra en el aviso a quién reemplaza.
- [ ] «Dar acceso al sistema» crea el usuario vinculado. La ficha pasa a mostrar «Acceso: <correo>» y el botón desaparece.
- [ ] Un usuario `LIDER` ve en `/miembros` solo su red y sus superiores, sin «Nuevo miembro» y con las fichas en solo lectura.
- [ ] `/niveles` permite crear, renombrar, subir, bajar y desactivar. El orden se refleja en los selectores de nivel.
- [ ] `/grupos` y el menú usan el nombre configurado (p. ej. «Hogares de paz»). Cada grupo se muestra como «<singular> de <líder>».
- [ ] En `/grupos/:id` se agregan y quitan miembros arrastrando y también con los botones. Cada disponible indica su grupo actual; mover a alguien de otro grupo pide confirmación nombrando ese grupo.
- [ ] La ficha muestra el árbol: superiores arriba con su nivel, el miembro resaltado y sus liderados debajo, que se expanden por niveles.
- [ ] El listado de miembros no tiene columna ni filtro de grupo; tiene las columnas «Dirección» (solo la dirección) y «Región» (distrito, provincia y departamento), igual que la tabla de grupos, y filtros de departamento, provincia y distrito en cascada. La condición se muestra como «Consolidado».
- [ ] Todas las pantallas nuevas tienen estados de carga, vacío y error, funcionan en modo claro y oscuro y en móvil, y tienen sus pruebas `.spec.ts`. `ng test` pasa.
- [ ] Existe `diagramas/spec-06-registro-de-miembros.drawio` con las páginas del paso 18, actualizado en el paso 27.

## Decisiones

- **Sí:** un único registro de miembros con la condición `CONSOLIDADO` (antes `ANTIGUO`, revisión 2), `NUEVO` o `RECUPERADO`. **No:** módulos separados de miembros actuales y nuevos: es la misma persona en dos momentos, y separarlos duplicaría tablas y pantallas.
- **Sí:** solo nombres y apellidos son obligatorios, y la ficha muestra qué falta. Las iglesias reciben los datos por partes.
- **Sí:** una sola spec con miembros, niveles, grupos, jerarquía, visibilidad del líder y acceso al sistema, por decisión del usuario, aunque es grande. Se mitiga con un plan de 18 pasos que dejan el sistema funcionando en cada uno.
- **Sí:** «Quién lo registró» es un miembro opcional, elegido a mano. **No** se guarda el usuario del token, por decisión del usuario. Esto cambia lo que dice CLAUDE.md («se toma del token»), que conviene actualizar.
- **Sí:** edad aproximada guardada como año de nacimiento estimado, para que siga siendo correcta con el tiempo. **No:** guardar la edad.
- **Sí (revisión 2):** fecha de nacimiento y edad son independientes y se guardan aunque no coincidan, por decisión del usuario. La edad calculada de la fecha manda en la respuesta (`edad`); la escrita se devuelve aparte (`edadAproximada`). **No:** derivar una de la otra ni rechazar las dos juntas.
- **Sí:** el nivel de estudios y el centro de estudios son visibles y opcionales desde ya. El nivel de estudios es una lista fija (educación formal), distinta de los niveles académicos configurables del módulo 7.
- **Sí:** el ubigeo oficial del INEI es un catálogo común sin `iglesia_id`, la excepción a «catálogos por iglesia», porque la geografía no depende de la iglesia. **No:** texto libre, que genera datos inconsistentes.
- **Sí:** el líder es opcional y único, el pastor principal es único y sin líder, y los ciclos se validan con CTE recursivas filtradas por iglesia. El índice único parcial y los `CHECK` respaldan la regla en la base. Cambiar el líder mueve la red completa de forma natural, porque cada uno apunta solo a su líder directo.
- **Sí:** designar al pastor principal transfiere la marca y le quita el líder al nuevo pastor.
- **Sí:** el grupo del miembro es independiente de su líder.
- **Sí (revisión 2):** el grupo no tiene nombre; se identifica por su líder, obligatorio y con un solo grupo, respaldado por el índice único `(iglesia_id, lider_id)`. **No:** varios grupos por líder.
- **Sí (revisión 2):** los miembros del grupo se administran desde el grupo, arrastrando, y la lista de disponibles incluye a los de otros grupos, por decisión del usuario; mover pide confirmación. Los botones equivalentes cumplen la accesibilidad por teclado. Los candidatos se buscan en el servidor (hasta 20), para no cargar toda la iglesia.
- **Sí (revisión 2):** el árbol de la ficha carga los liderados por niveles (`/liderados`), no la red completa de golpe, para que una red grande no pese. Respeta el ámbito del líder.
- **Sí (revisión 2):** el filtro geográfico del listado es un solo parámetro `ubigeo` (prefijo del código de distrito), porque los códigos INEI son jerárquicos.
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
| `V6` elimina grupos sin líder o con líder repetido. | Solo existen en bases de desarrollo (V5 no llegó a producción). Antes se sacan sus miembros del grupo, sin borrar personas. |
| La FK circular entre `miembro` y `grupo` complica la migración y las pruebas. | La FK del grupo del miembro se agrega al final de `V5`. Las pruebas crean primero los miembros y después los grupos. |
| El nombre configurable de los grupos llega tarde y el menú parpadea. | Valor por defecto «Grupos» hasta que llegue la configuración. |

## Qué **no** entra en esta spec

- Árbol de la iglesia completa fuera de la ficha, y totales de la red completa.
- Que un líder registre o edite personas.
- Portal del miembro.
- Vincular o desvincular un usuario existente con un miembro desde `/usuarios`.
- Seguimiento y consolidación, asistencia, ofrendas y académico.
- Historial de cambios de líder, nivel o grupo.
- Importación desde Excel, foto y documento de identidad.
- Ubigeo de otros países.

Cada uno, si llega, va en su propia spec.
