# SPEC 10 — Consolidación de nuevos y seguimiento pastoral

> **Estado:** Implementado
> **Depende de:** SPEC 02 (permisos y perfiles personalizados), SPEC 03 (OpenAPI y trazabilidad), SPEC 06 (miembros, condición «Nuevo» y jerarquía), SPEC 07 (regla de grillas paginadas), SPEC 09 (asistencia y alertas de seguimiento)
> **Fecha:** 2026-10-30
> **Objetivo:** Que la iglesia organice la consolidación por **rondas**: arma un lote de personas (nuevos o miembros que se están alejando), lo reparte entre un equipo de consolidadores, y cada consolidador registra desde su celular cada llamada o visita y el resultado. Así se sabe, por persona, cuántas veces se la contactó, por qué medio, qué pasó en cada iteración y si volvió a asistir.

## Por qué existe esta spec

- Cuando llega un nuevo se le asigna un **líder** (de quién depende en la jerarquía), pero quien lo **consolida** suele ser otra persona: un equipo de miembros encargado de llamar o visitar a los nuevos.
- Hoy la consolidación se hace **por tandas en papel**: cada cierto tiempo se saca una lista de nuevos, se reparte y se llama. Lo que pasó en cada llamada se pierde o queda en hojas sueltas. No se sabe cuántas veces se contactó a alguien, quién lo hizo ni con qué resultado.
- Lo mismo pasa con miembros activos que dejan de venir: el SPEC 09 ya los **detecta** (alertas «Faltas seguidas», «Asistencia en caída», «Nuevo sin volver»), pero no hay dónde **registrar el contacto** que se les hace.
- Es el punto 4 de la hoja de ruta (`CLAUDE.md`, «Seguimiento y consolidación»).

## Alcance

**Entra:**

- **Equipo de consolidación:** la lista de miembros que consolidan en la iglesia. Sirve para repartir los lotes. Un consolidador registra sus contactos con su propio usuario. Si alguno no tiene usuario, un coordinador puede registrar por él.
- **Rondas de consolidación:** cada «tanda» con nombre, fechas de inicio y fin, descripción opcional y estado (planificada → en curso → cerrada).
- **Armar el lote** de una ronda con filtros (condición «Nuevo», fecha de ingreso, alerta de asistencia del SPEC 09, grupo, sin grupo, nunca consolidado) y vista previa de cuántas personas entran. También se puede agregar o quitar a alguien a mano. Las personas que están en papel y no existen en el sistema se registran primero con el formulario de miembros (SPEC 06).
- **Repartir:** asignar cada persona a un consolidador, a mano o «en partes iguales» entre los consolidadores elegidos. Mientras el caso esté abierto se puede reasignar.
- **Caso:** la persona dentro de una ronda (o en un seguimiento individual), con su consolidador, su estado y, al cerrarse, el **resultado de la iteración**.
- **Seguimiento individual:** un caso fuera de una ronda, para una persona suelta. Por ejemplo, un líder que quiere dar seguimiento a un miembro de su red que dejó de venir.
- **Contactos:** cada llamada, mensaje o visita, con fecha y hora, medio, resultado del intento, observaciones y fecha del próximo contacto (opcional).
- **Resultados de la iteración configurables por iglesia** (catálogo como los niveles), con valores iniciales sugeridos.
- **«Mis asignados»** (celular primero): la lista del consolidador, con botones para llamar y escribir por WhatsApp y el registro del contacto en dos toques.
- **Historial de consolidación de una persona:** todas sus rondas y casos, cada contacto y el resultado de cada iteración, junto a su evolución de asistencia (SPEC 09). Además, una tarjeta resumen en la ficha del miembro.
- **Avance de una ronda:** contactados, pendientes, cerrados, resultados, avance por consolidador y cuántos **volvieron a asistir** después de ser contactados.
- **Permisos nuevos** en `iglesia-seguridad` (ver «Permisos y ámbito»).
- **Documentación y cierre:** OpenAPI, Postman, pruebas y diagrama, como en las specs anteriores.

**Fuera de alcance (para specs futuras):**

- Acciones sobre la ficha al cerrar un caso (asignar grupo o líder, cambiar la condición): el caso solo registra; la ficha se edita aparte, como hasta ahora.
- Recordatorios o notificaciones (correo, WhatsApp, push) al consolidador, y plantillas de mensajes.
- Enviar mensajes desde el sistema: el botón de WhatsApp solo abre la conversación en el celular.
- Importar listas desde Excel o fotos de las hojas de papel.
- Reparto automático por cercanía (distrito) o por género, agenda o calendario de visitas y ubicación GPS de las visitas.
- Que el propio miembro vea su consolidación (portal del miembro).
- Reportes globales entre iglesias.

## Conceptos y reglas

### Equipo de consolidación

- Un consolidador es un **miembro activo** de la iglesia agregado al equipo. Se agrega o se quita (se desactiva) desde «Consolidación › Equipo».
- El equipo es la lista de candidatos al repartir una ronda. No da permisos: para registrar sus contactos, el consolidador necesita un usuario con `consolidacion.contactar` vinculado a su miembro (`miembroId` del token).
- Quitar a alguien del equipo no cambia sus casos abiertos. Se avisa cuántos tiene, para reasignarlos.

### Ronda

- **Datos:**
  - Nombre obligatorio, de 3 a 80 caracteres y único por iglesia sin distinguir mayúsculas (si no, 409).
  - Fecha de inicio obligatoria.
  - Fecha de fin opcional, que debe ser igual o posterior a la de inicio (si no, 400).
  - Descripción opcional, de hasta 500 caracteres.
- **Estados:**
  - `PLANIFICADA`: se arma el lote y se reparte. Los consolidadores aún no ven esos casos.
  - `EN_CURSO`: se pasa con «Iniciar ronda». Sus casos aparecen en «Mis asignados». Se pueden agregar personas y reasignar.
  - `CERRADA`: se pasa con «Cerrar ronda», con confirmación. Los casos aún abiertos se cierran sin resultado, como «No concluido». Una ronda cerrada es de solo lectura.
- **Borrar** una ronda solo se permite mientras está planificada (si no, 409).

### Caso

- Une a una **persona**, una **ronda** (o ninguna, si es seguimiento individual) y un **consolidador** (miembro). El consolidador puede quedar vacío mientras la ronda está planificada.
- **Estados:**
  - `PENDIENTE`: aún sin contactos.
  - `EN_CONTACTO`: ya tiene al menos un contacto.
  - `CERRADO`: tiene resultado y fecha de cierre, y guarda quién lo cerró.
- **Una persona no puede tener dos casos abiertos a la vez** (índice único parcial; si no, 409 «Ya tiene un seguimiento abierto en la ronda X»). Al armar un lote, quien ya tiene un caso abierto aparece marcado y no se agrega.
- **Cerrar un caso** exige un resultado activo del catálogo y admite una observación final. Un caso cerrado de una ronda en curso se puede **reabrir** (por error de registro). Esto vuelve a `EN_CONTACTO` o a `PENDIENTE`.
- **Seguimiento individual:**
  - Se abre desde la ficha o el historial de una persona («Iniciar seguimiento»).
  - El consolidador por defecto es quien lo abre (si tiene `miembroId`); un coordinador puede elegir otro.
  - Se cierra igual que uno de ronda.
- Una persona inactiva no se puede agregar a un lote ni a un seguimiento (409). Si se desactiva con un caso abierto, el caso sigue visible hasta que se cierre.

### Contacto

| Campo | Regla |
|---|---|
| `fechaHora` | Obligatoria, no futura (400). Por defecto, ahora. |
| `medio` | `LLAMADA`, `WHATSAPP`, `VISITA` o `EN_LA_IGLESIA`. |
| `resultado` | `SE_HABLO` (se habló con la persona), `NO_RESPONDIO` (no contestó o no respondió el mensaje), `NO_ESTABA` (visita sin encontrarla) o `DATO_INCORRECTO` (teléfono o dirección equivocados). En la pantalla se ofrecen solo los que tienen sentido para el medio elegido. |
| `observaciones` | Opcional, hasta 1000 caracteres. |
| `proximoContacto` | Fecha opcional, igual o posterior a la del contacto (400). Ordena «Mis asignados». |

- Solo se registra en casos abiertos de rondas en curso o en seguimientos individuales (si no, 409).
- `registrado_por` guarda el `usuarioId` del token; `consolidador_id` guarda el miembro que hizo el contacto. Por defecto es el consolidador del caso, y un coordinador puede registrar en nombre de otro.
- El primer contacto pasa el caso de `PENDIENTE` a `EN_CONTACTO`.
- Un consolidador puede **corregir o borrar** sus propios contactos durante 24 horas (si no, 403 o 409); después quedan como historial. El coordinador puede hacerlo siempre.

### Resultados de la iteración (catálogo por iglesia)

- Nombre (de 2 a 60 caracteres, único por iglesia), orden y activo, igual que los niveles. Se desactivan, no se borran, para conservar el historial.
- **Valores iniciales** que recibe toda iglesia (las que ya existen, por la migración; las nuevas, con el evento `IglesiaCreada`):
  - Se integró a un grupo
  - Volverá a asistir
  - Necesita otra visita
  - No le interesa por ahora
  - Se congrega en otra iglesia
  - No se pudo ubicar

### Indicadores

- **Por persona:** número de casos (iteraciones), contactos por medio, último contacto, último resultado y si tiene un caso abierto.
- **Por ronda:**
  - personas, contactados (con al menos un `SE_HABLO`), pendientes, en contacto, cerrados y no concluidos;
  - reparto por resultado y avance por consolidador (asignados, contactados, cerrados).
- **«Volvió a asistir»:** de las personas contactadas, cuántas asistieron (SPEC 09) al menos una vez en los **28 días** siguientes a su primer `SE_HABLO` de esa ronda. Se calcula en la base, en una sola consulta por ronda.

### Permisos y ámbito

| Permiso | Para qué | Roles base |
|---|---|---|
| `consolidacion.administrar` | Coordinador de consolidación. Gestiona el equipo, las rondas (armar, repartir, iniciar, cerrar) y el catálogo de resultados. Además puede hacer **todo lo que hace un consolidador, sobre cualquier caso y persona de la iglesia y sin restricciones de ámbito** (ver «El coordinador»). | `ADMIN_IGLESIA` |
| `consolidacion.ver` | Ver todas las rondas, su avance y el historial de consolidación de cualquier persona. | `ADMIN_IGLESIA` |
| `consolidacion.contactar` | Ver «Mis asignados», registrar contactos y cerrar sus casos. Abrir un seguimiento individual a personas de **su red**. | `ADMIN_IGLESIA`, `LIDER` |

- **El coordinador** (`consolidacion.administrar`) incluye, sin necesidad de tener los otros dos permisos, todo lo de `consolidacion.ver` y `consolidacion.contactar`, sin límite de ámbito:
  - ve todas las rondas, todos los casos y el historial de cualquier persona;
  - registra contactos en cualquier caso abierto, a su nombre o en nombre del consolidador asignado;
  - cierra y reabre cualquier caso;
  - corrige o borra cualquier contacto, sin la ventana de 24 horas (queda registrado en `INFO` quién lo cambió);
  - abre seguimientos individuales para cualquier persona activa;
  - si se asigna casos a sí mismo (forma parte del equipo), también tiene su «Mis asignados».

  Las únicas reglas que lo siguen alcanzando son las de integridad de los datos: un caso abierto por persona, no registrar en rondas planificadas o cerradas, una ronda cerrada es de solo lectura y no se registran contactos con fecha futura.
- En `iglesia-api` se resuelve con una sola regla: las expresiones de autorización de ver y contactar aceptan también `consolidacion.administrar` (constantes de `Permisos`), y `AmbitoConsolidacion` devuelve «toda la iglesia» para el coordinador.
- **Perfil «Consolidador»:** la iglesia crea un perfil personalizado (SPEC 02) con solo `consolidacion.contactar` y se lo da al usuario de cada miembro del equipo.
- **Ámbito de `consolidacion.contactar`** (sin `consolidacion.ver`):
  - los casos asignados a él;
  - el historial completo de las personas que tiene asignadas en un caso abierto, para saber qué pasó en las rondas anteriores;
  - el historial de las personas de su red (las que lidera directa o indirectamente).

  Cualquier otra persona o caso responde 404.
- **Datos que ve un consolidador** de cada asignado: lo necesario para contactarlo. Son nombre, teléfono, dirección y distrito, condición, fecha de ingreso, quién lo invitó, su líder y grupo, la petición con la que llegó y su asistencia reciente. No ve sus aportes ni el resto de la ficha.
- **Tarjeta «Consolidación» en la ficha del miembro:** se ve con `consolidacion.ver` o `consolidacion.administrar`, o con `consolidacion.contactar` dentro de su ámbito.

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V11__consolidacion.sql`

```sql
CREATE TABLE consolidador (
    iglesia_id  BIGINT      NOT NULL REFERENCES iglesia (id),
    miembro_id  BIGINT      NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (iglesia_id, miembro_id),
    FOREIGN KEY (iglesia_id, miembro_id) REFERENCES miembro (iglesia_id, id)
);

CREATE TABLE resultado_consolidacion (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id  BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre      VARCHAR(60) NOT NULL,
    orden       SMALLINT    NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_resultado_consolidacion_nombre ON resultado_consolidacion (iglesia_id, lower(nombre));

CREATE TABLE ronda_consolidacion (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id   BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre       VARCHAR(80)  NOT NULL,
    descripcion  VARCHAR(500),
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE,
    estado       VARCHAR(12)  NOT NULL DEFAULT 'PLANIFICADA' CHECK (estado IN ('PLANIFICADA', 'EN_CURSO', 'CERRADA')),
    creada_por   BIGINT       NOT NULL,           -- usuarioId (otra base, sin FK)
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cerrada_en   TIMESTAMPTZ,
    UNIQUE (iglesia_id, id),
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);
CREATE UNIQUE INDEX ux_ronda_consolidacion_nombre ON ronda_consolidacion (iglesia_id, lower(nombre));

CREATE TABLE caso_consolidacion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id      BIGINT        NOT NULL REFERENCES iglesia (id),
    ronda_id        BIGINT,                       -- NULL = seguimiento individual
    miembro_id      BIGINT        NOT NULL,
    consolidador_id BIGINT,
    estado          VARCHAR(12)   NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_CONTACTO', 'CERRADO')),
    resultado_id    BIGINT,
    observacion_cierre VARCHAR(1000),
    cerrado_en      TIMESTAMPTZ,
    cerrado_por     BIGINT,                       -- usuarioId
    creado_por      BIGINT        NOT NULL,       -- usuarioId
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, ronda_id)        REFERENCES ronda_consolidacion (iglesia_id, id),
    FOREIGN KEY (iglesia_id, miembro_id)      REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, consolidador_id) REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, resultado_id)    REFERENCES resultado_consolidacion (iglesia_id, id),
    CHECK ((estado = 'CERRADO') = (cerrado_en IS NOT NULL))
);
CREATE UNIQUE INDEX ux_caso_abierto_por_miembro ON caso_consolidacion (iglesia_id, miembro_id) WHERE estado <> 'CERRADO';
CREATE UNIQUE INDEX ux_caso_ronda_miembro      ON caso_consolidacion (iglesia_id, ronda_id, miembro_id) WHERE ronda_id IS NOT NULL;
CREATE INDEX ix_caso_consolidador ON caso_consolidacion (iglesia_id, consolidador_id, estado);
CREATE INDEX ix_caso_miembro      ON caso_consolidacion (iglesia_id, miembro_id);

CREATE TABLE contacto_consolidacion (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id       BIGINT        NOT NULL REFERENCES iglesia (id),
    caso_id          BIGINT        NOT NULL,
    consolidador_id  BIGINT,                      -- miembro que hizo el contacto
    fecha_hora       TIMESTAMPTZ   NOT NULL,
    medio            VARCHAR(14)   NOT NULL CHECK (medio IN ('LLAMADA', 'WHATSAPP', 'VISITA', 'EN_LA_IGLESIA')),
    resultado        VARCHAR(16)   NOT NULL CHECK (resultado IN ('SE_HABLO', 'NO_RESPONDIO', 'NO_ESTABA', 'DATO_INCORRECTO')),
    observaciones    VARCHAR(1000),
    proximo_contacto DATE,
    registrado_por   BIGINT        NOT NULL,      -- usuarioId
    registrado_en    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    FOREIGN KEY (iglesia_id, caso_id)         REFERENCES caso_consolidacion (iglesia_id, id),
    FOREIGN KEY (iglesia_id, consolidador_id) REFERENCES miembro (iglesia_id, id)
);
CREATE INDEX ix_contacto_caso ON contacto_consolidacion (iglesia_id, caso_id, fecha_hora DESC);

SELECT habilitar_aislamiento_por_iglesia('consolidador');
SELECT habilitar_aislamiento_por_iglesia('resultado_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('ronda_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('caso_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('contacto_consolidacion');

-- Resultados iniciales para las iglesias que ya existen. Las nuevas los reciben al darse de alta (IglesiaCreada).
INSERT INTO resultado_consolidacion (iglesia_id, nombre, orden)
SELECT i.id, r.nombre, r.orden
FROM iglesia i
CROSS JOIN (VALUES ('Se integró a un grupo', 1), ('Volverá a asistir', 2), ('Necesita otra visita', 3),
                   ('No le interesa por ahora', 4), ('Se congrega en otra iglesia', 5), ('No se pudo ubicar', 6)) AS r (nombre, orden);
```

- El índice único parcial `ux_caso_abierto_por_miembro` garantiza en la base la regla «un solo caso abierto por persona», también con dos coordinadores armando lotes a la vez.
- Los usuarios (`creada_por`, `registrado_por`…) viven en `iglesia_seguridad`: se guarda el `usuarioId` del token, sin FK, igual que en aportes.

### Migración `iglesia-seguridad/src/main/resources/db/migration/V9__permisos_de_consolidacion.sql`

Agrega los tres permisos de «Permisos y ámbito» en el módulo «Consolidación», con su descripción y orden. `ADMIN_IGLESIA` recibe los tres y `LIDER`, `consolidacion.contactar`.

## Backend `iglesia-api`

Nuevo contexto **`consolidacion/`** (`com.iglesia.api.consolidacion.<tipo>`):

- **`entity`:** `Consolidador`, `ResultadoConsolidacion`, `RondaConsolidacion`, `CasoConsolidacion`, `ContactoConsolidacion`.
- **`model`:**
  - `EstadoRonda` y `EstadoCaso`, con las transiciones permitidas en el propio enum (`puedePasarA`);
  - `MedioContacto`;
  - `ResultadoContacto`, que sabe para qué medios aplica;
  - `FiltroLote` (record);
  - `AlcanceConsolidacion` (toda la iglesia / casos propios + red).
- **`component`:** `ResultadosPorDefecto`, que escucha `IglesiaCreada` (patrón de `aportes/component/CategoriasPorDefecto`).
- **`service`:**
  - `EquipoConsolidacionService`: listar, agregar, desactivar, con la carga de casos abiertos de cada uno.
  - `RondaService`:
    - crear, editar, borrar si está planificada;
    - `vistaPreviaLote(filtro)` y `agregarLote(filtro)` en una sola sentencia `INSERT … SELECT … ON CONFLICT DO NOTHING`, sin bucles;
    - agregar o quitar personas;
    - `repartir(consolidadores, soloSinAsignar)`, balanceado por la carga actual;
    - reasignar, iniciar y cerrar.
  - `CasoService`: abrir un seguimiento individual, cerrar, reabrir, «Mis asignados» y el detalle del caso.
  - `ContactoService`: registrar, corregir y borrar (ventana de 24 h para el consolidador, sin límite para el coordinador).
  - `HistorialConsolidacionService`: todos los casos y contactos de una persona y su resumen para la ficha.
  - `AvanceRondaService`: indicadores de la ronda y «volvió a asistir», en una consulta con agregados (`COUNT … FILTER`). La asistencia se obtiene por el `service` de `asistencia`, sin usar sus entidades.
  - `AmbitoConsolidacion`: con los permisos y el `miembroId` del token decide qué casos e historiales puede ver (patrón de `AmbitoAsistencia`). La red usa la CTE recursiva de miembros, filtrando `iglesia_id` en cada paso.
  - `ResultadoConsolidacionService`: CRUD del catálogo, orden y activar o desactivar.
- **Filtro del lote:**
  - Condición; fecha de ingreso desde y hasta; alerta de asistencia del SPEC 09 (la toma de `SeguimientoAsistenciaService`); grupo; sin grupo; nunca consolidado (sin casos cerrados); solo activos.
  - Siempre excluye a quien tiene un caso abierto y avisa cuántos quedaron fuera por eso.
  - Se valida en el servicio que todos los ids (miembros, consolidadores, resultados) sean de la iglesia del token.
- **Logs (`INFO`, solo ids):** crear, iniciar y cerrar una ronda (`rondaId`, cantidad de casos), repartir, abrir y cerrar casos (`casoId`, `resultadoId`), y registrar, corregir o borrar contactos (`contactoId`, `casoId`, `medio`). Nunca observaciones, nombres ni teléfonos.

### Endpoints de `iglesia-api`

| Método y ruta | Permiso | Descripción |
|---|---|---|
| `GET /api/consolidacion/equipo` | `consolidacion.administrar` | Consolidadores con su carga (casos abiertos). |
| `POST /api/consolidacion/equipo` · `PATCH /{miembroId}/estado` | `consolidacion.administrar` | Agregar (miembro activo, 409 si ya está) y activar o desactivar. |
| `GET · POST · PUT · DELETE /api/consolidacion/resultados[/{id}]` · `PATCH /{id}/estado` · `PUT /orden` | `consolidacion.administrar` (leer también con `consolidacion.contactar`) | Catálogo de resultados de la iteración. |
| `GET /api/consolidacion/rondas?estado=&q=&page=&size=` | `consolidacion.ver` o `consolidacion.administrar` | Rondas paginadas con un resumen del avance. |
| `POST · PUT /api/consolidacion/rondas[/{id}]` · `DELETE /{id}` | `consolidacion.administrar` | Alta, edición y borrado (solo planificada). |
| `POST /api/consolidacion/rondas/{id}/iniciar` · `/cerrar` | `consolidacion.administrar` | Cambia el estado. 409 si la transición no es válida o la ronda está vacía al iniciar. |
| `POST /api/consolidacion/rondas/{id}/lote/vista-previa` | `consolidacion.administrar` | Body `FiltroLote` → cantidad que entraría, cuántas quedan fuera por tener un caso abierto y una muestra de nombres. |
| `POST /api/consolidacion/rondas/{id}/lote` | `consolidacion.administrar` | Agrega a la ronda las personas del filtro, o una lista de `miembroIds`. Devuelve cuántas se agregaron. |
| `GET /api/consolidacion/rondas/{id}/casos?estado=&consolidadorId=&sinAsignar=&q=&page=&size=` | `consolidacion.ver` o `consolidacion.administrar` | Casos de la ronda paginados: persona, consolidador, estado, número de contactos, último contacto y resultado. |
| `DELETE /api/consolidacion/rondas/{id}/casos/{casoId}` | `consolidacion.administrar` | Quita a una persona del lote (solo sin contactos). |
| `POST /api/consolidacion/rondas/{id}/repartir/vista-previa` | `consolidacion.administrar` | Body `{ consolidadorIds, soloSinAsignar }` → propuesta de reparto (quién recibe a quién) sin guardarla, para el paso 3 del asistente. |
| `POST /api/consolidacion/rondas/{id}/repartir` | `consolidacion.administrar` | Body `{ consolidadorIds, soloSinAsignar }` o `{ asignaciones: [{ casoId, consolidadorId }] }` (la propuesta ajustada) → guarda el reparto. |
| `PATCH /api/consolidacion/casos/{id}/consolidador` | `consolidacion.administrar` | Reasigna un caso abierto (también uno a varios casos: `PATCH /api/consolidacion/casos/consolidador` con `{ casoIds, consolidadorId }`). |
| `GET /api/consolidacion/rondas/{id}/avance` | `consolidacion.ver` o `consolidacion.administrar` | Indicadores de la ronda, reparto por resultado, avance por consolidador y «volvió a asistir». |
| `GET /api/consolidacion/mis-casos?estado=&q=&page=&size=` | `consolidacion.contactar` o `consolidacion.administrar` | Casos abiertos asignados al `miembroId` del token, de rondas en curso o individuales, con el número de iteración y el último resultado anterior. Primero los de próximo contacto vencido, luego los pendientes y luego el resto. |
| `GET /api/consolidacion/casos/{id}` | según ámbito (el coordinador, cualquiera) | Detalle del caso: datos de contacto de la persona, sus contactos, su asistencia reciente, **las asistencias del período del caso** (para la línea de tiempo combinada), el resumen «Antes de contactar» en datos estructurados y sus **iteraciones anteriores** (ronda, fechas, consolidador, contactos por medio, resultado y observación de cierre). |
| `POST /api/consolidacion/casos` | `consolidacion.contactar` (su red) o `consolidacion.administrar` | Abre un seguimiento individual. 409 si ya tiene un caso abierto. |
| `POST /api/consolidacion/casos/{id}/cerrar` · `/reabrir` | ámbito del caso o `consolidacion.administrar` | Cerrar con resultado; reabrir. |
| `POST /api/consolidacion/casos/{id}/contactos` | ámbito del caso o `consolidacion.administrar` | Registra un contacto. 201. |
| `PUT · DELETE /api/consolidacion/contactos/{id}` | quien lo registró (24 h) o `consolidacion.administrar` (sin límite) | Corrige o borra un contacto. |
| `GET /api/miembros/{id}/consolidacion` | `consolidacion.ver`, `consolidacion.administrar` o `consolidacion.contactar` con ámbito | Resumen para la ficha: iteraciones, contactos por medio, último resultado y caso abierto. |
| `GET /api/miembros/{id}/consolidacion/historial?page=&size=` | ídem | Casos de la persona, del más reciente al más antiguo, cada uno con sus contactos, y las semanas con contacto (para marcarlas en la evolución de asistencia). |

- Errores como `ProblemDetail` con `traceId`. Se responde 400 por validación, 401, 403 por falta de permiso, 404 fuera del ámbito y 409 por reglas de estado y unicidad.

## Frontend (`iglesia-web/src/app/`)

Nuevo feature **`features/consolidacion/`** con `consolidacion.routes.ts`:

| Ruta | Permiso | Pantalla |
|---|---|---|
| `/consolidacion` | `consolidacion.ver` o `consolidacion.administrar` | Rondas (lista paginada con su avance) y botón «Nueva ronda». Pestaña «Equipo» si tiene `consolidacion.administrar`. |
| `/consolidacion/rondas/:id` | ídem | Detalle de la ronda: pestañas «Personas» (lote y reparto) y «Avance». |
| `/consolidacion/mis-asignados` | `consolidacion.contactar` | Lista del consolidador. |
| `/consolidacion/casos/:id` | según ámbito | Caso: datos de contacto, registrar contacto, línea de tiempo y cerrar. |
| `/miembros/:id/consolidacion` | según ámbito | Historial de consolidación de la persona. |
| `/consolidacion/resultados` | `consolidacion.administrar` | Catálogo de resultados (patrón de la pantalla de niveles). |

- **Menú:**
  - «Iglesia › Consolidación» (`pi-heart`), con `consolidacion.ver` o `consolidacion.administrar`;
  - «Iglesia › Mis asignados» (`pi-phone`), con `consolidacion.contactar` o `consolidacion.administrar`;
  - «Administración › Resultados de consolidación», con `consolidacion.administrar`.
- **Servicios (fachada HTTP):** `RondasService`, `CasosService`, `EquipoConsolidacionService` y `ResultadosConsolidacionService`, con modelos que siguen el contrato de la API.
- **Reutilizables en `shared/components/`** (si aún no existen): selector de miembro (para agregar al lote, al equipo o elegir consolidador) y botones de contacto (`tel:` y `https://wa.me/…`, con el teléfono normalizado).

## Diseño de la experiencia

La consolidación la hacen voluntarios, muchas veces desde el celular, entre otras ocupaciones. Cada pantalla tiene que poder usarse sin capacitación. Se aplica todo lo de UX/UI de `CLAUDE.md` (Sakai, claro y oscuro, accesibilidad, retroalimentación) y, además, estos principios propios del módulo.

### Principios

1. **«¿Qué sigue?» siempre visible:** cada pantalla tiene **una** acción principal según el estado. Por ejemplo:
   - ronda sin personas → «Agregar personas»;
   - con personas sin asignar → «Repartir»;
   - repartida → «Iniciar ronda»;
   - en curso → avance.

   Los avisos accionables («8 personas sin asignar · Repartir») reemplazan a los mensajes pasivos.
2. **Contexto antes de actuar:** antes de llamar, el consolidador ve quién es la persona, qué pasó en las iteraciones anteriores y si está viniendo a la iglesia.
3. **Escribir lo mínimo:** botones grandes en lugar de listas desplegables y valores por defecto inteligentes:
   - fecha y hora: ahora;
   - medio: el del botón que se tocó;
   - próximo contacto: atajos.

   Las observaciones son siempre opcionales.
4. **Perdonar errores:** «Deshacer» durante 8 segundos tras registrar un contacto. Confirmación solo en lo destructivo o masivo (cerrar ronda, quitar del lote, repartir de nuevo). Nunca se pierde lo escrito: si falla el guardado, el formulario sigue abierto con los datos y el error al lado.
5. **Respuesta inmediata:** actualización optimista de tarjetas y contadores (se revierte con aviso si falla), skeletons con la forma real del contenido y nada de pantallas en blanco.
6. **Celular primero para el consolidador; escritorio cómodo para el coordinador:** las mismas pantallas se adaptan. En el móvil, las acciones principales quedan en una barra fija al pie, al alcance del pulgar, con objetivos táctiles de al menos 44 px.
7. **Lenguaje humano y cálido:** textos en segunda persona («Tienes 4 personas para hoy»), sin jerga técnica. Los estados vacíos explican y ofrecen la siguiente acción.

### Rondas (`/consolidacion`)

- **Arriba, «Rondas en curso»:** una tarjeta por ronda activa con nombre, días que faltan para su fin y un `p-metergroup` con cerrados / en contacto / pendientes y su leyenda. Cada tarjeta lleva un aviso accionable si hace falta: «5 con contacto vencido», «3 sin asignar». Un toque abre el detalle.
- **Debajo, «Todas las rondas»:** grilla paginada en el servidor con nombre, fechas, estado (`p-tag` con ícono), personas y avance. Tiene búsqueda y filtro por estado. En el móvil, tarjetas con el mismo paginador.
- **Vacío:** ilustración con ícono y «Aún no hay rondas. Crea la primera para organizar las llamadas y visitas de tu equipo», con el botón «Nueva ronda».
- **Pestaña «Equipo»:**
  - Tarjetas de consolidadores con iniciales, nombre, casos abiertos, contactados en los últimos 30 días y una etiqueta «Sin acceso al sistema» si su miembro no tiene usuario.
  - «Agregar al equipo» usa el selector de miembro.
  - Desactivar muestra antes «Tiene 6 casos abiertos: reasígnalos desde cada ronda».

### Nueva ronda: asistente en 4 pasos (`p-stepper`)

Crear una ronda es la tarea más larga del coordinador. Por eso se guía paso a paso, se puede salir en cualquier momento y queda guardada como planificada.

1. **Datos:** nombre (con sugerencia «Consolidación noviembre 2026»), fechas con atajos («2 semanas», «1 mes») y descripción.
2. **Personas:**
   - Filtros en chips («Nuevos», «Ingresaron en los últimos 90 días», «Nuevo sin volver», «Faltas seguidas», «Sin grupo», «Nunca consolidados»…), más grupo y rango de fechas.
   - La **vista previa** se actualiza al instante: «Entrarían **37** personas», con las primeras caras (iniciales) y «4 ya tienen un seguimiento abierto (no se agregan)». Un enlace lista a esas 4.
   - Se puede quitar o agregar a alguien antes de confirmar.
3. **Reparto:**
   - Se eligen consolidadores del equipo (todos los activos marcados por defecto), cada uno con su carga actual.
   - La propuesta se ve antes de aplicarla: «Ana 7 · Luis 6 · Rosa 6…». Se puede ajustar arrastrando personas entre consolidadores (escritorio) o con «Mover a…» (móvil).
   - Se puede «Dejar sin asignar por ahora».
4. **Revisar:** resumen (personas, consolidadores, fechas) y dos botones: **«Iniciar ronda»** (principal: los consolidadores ya la ven) o «Guardar como planificada».

### Detalle de una ronda (`/consolidacion/rondas/:id`)

- **Encabezado:**
  - nombre, estado y fechas con «faltan 5 días» o «venció hace 2 días»;
  - `p-metergroup` del avance;
  - la acción principal según el estado;
  - secundarias en un menú «Más» (editar datos, cerrar ronda, borrar si está planificada).
- **Avisos accionables** bajo el encabezado («3 personas sin asignar · Repartir», «5 con contacto vencido · Ver»).
- **Pestaña «Personas»:**
  - Grilla paginada en el servidor con persona (iniciales, nombre y etiquetas «Nuevo» o alerta de asistencia), consolidador, estado, contactos («2 llamadas · 1 visita»), último contacto relativo («hace 3 días») y resultado.
  - Filtros en chips con su cantidad («Todos 40 · Pendientes 12 · En contacto 18 · Cerrados 10 · Sin asignar 3»), consolidador y búsqueda.
  - Al seleccionar filas aparece una **barra de acciones fija** con «Asignar a…», «Repartir en partes iguales» y «Quitar del lote», junto a la cantidad seleccionada.
  - Un clic en la fila abre el caso.
  - «Agregar personas» reutiliza el paso 2 del asistente en un panel lateral (`p-drawer`).
- **Pestaña «Avance»:**
  - **Embudo** de Personas → Contactadas → Cerradas → **Volvieron a asistir**, con porcentajes entre etapas;
  - dona de resultados de la iteración;
  - tabla por consolidador con un medidor de avance;
  - tiempo medio hasta el primer contacto.

  Cada gráfico lleva su tabla accesible.

### Mis asignados (`/consolidacion/mis-asignados`)

La pantalla más importante del módulo. Debe sentirse como una lista de tareas del día.

- **Encabezado personal:** «Hola, Luis. Tienes **4 personas para hoy**» y un medidor de su avance en la ronda («9 de 15 contactadas»).
- **Secciones en orden de urgencia** (no una lista plana):
  1. **Para hoy:** próximo contacto vencido u hoy, con la etiqueta «vencido hace 2 días» en `warn`.
  2. **Sin contactar:** pendientes, primero los que llevan más días asignados.
  3. **En contacto:** el resto.

  Cada sección muestra su cantidad y se puede plegar. La búsqueda por nombre filtra todas.
- **Tarjeta de persona:**
  - iniciales, nombre, etiquetas («Nuevo», «2.ª iteración», alerta de asistencia);
  - una línea de contexto: «Último: no contestó (hace 3 días)» o, si es su primer contacto en esta ronda, «Antes: No le interesa por ahora (ago 2026)»;
  - botones grandes **«Llamar»** y **«WhatsApp»** (ocultos si no tiene teléfono, con el aviso «Sin teléfono: coordina una visita»), y **«Registrar»**.

  Tocar el nombre abre el caso.
- **Registro en un toque al volver de la llamada:**
  - Al tocar «Llamar» o «WhatsApp» se abre la app del teléfono y se recuerda a quién se contactó y por qué medio.
  - **Al volver al sistema** (evento `visibilitychange`), se abre solo la hoja «¿Cómo te fue con la llamada a Ana?» con el medio ya elegido.
  - Si no se completa, la tarjeta queda marcada «Llamada sin registrar» hasta registrarla o descartarla.
- **Hoja «Registrar contacto»:** `p-drawer` desde abajo en el móvil, `p-dialog` en escritorio.
  1. **Medio:** cuatro botones con ícono (Llamada, WhatsApp, Visita, En la iglesia), ya marcado si vino de un botón.
  2. **¿Qué pasó?:** botones grandes con ícono y color de severidad, solo los que aplican al medio. Por ejemplo, para una llamada: «Hablamos», «No contestó», «Número equivocado».
  3. **Opcional:** observaciones (con contador de caracteres) y **próximo contacto** con atajos «Mañana · En 3 días · En 1 semana · Elegir fecha».
  4. «Guardar» → toast «Contacto registrado» con **«Deshacer»** (8 s). La tarjeta se mueve sola a la sección que le corresponde.

  Si el resultado fue «Hablamos», la hoja ofrece al final «¿Terminó el seguimiento? **Cerrar caso**», que pasa al diálogo de cierre sin salir.
- **Cerrar caso:** los resultados del catálogo como botones (en orden), observación final opcional y confirmación con el resumen («Cerrarás el seguimiento de Ana con «Volverá a asistir»»).
- **Vacíos:**
  - sin asignados: «No tienes personas asignadas por ahora. Cuando tu coordinador inicie una ronda, aparecerán aquí»;
  - todo al día: «¡Todo al día! No tienes contactos pendientes para hoy».

### Caso (`/consolidacion/casos/:id`)

- **Cabecera de la persona:** iniciales grandes, nombre, edad, etiquetas (condición, alerta de asistencia, iteración), ronda y consolidador. En el móvil, **barra fija al pie** con «Llamar», «WhatsApp» y «Registrar».
- **«Antes de contactar»:** un recuadro destacado con un resumen en lenguaje natural, armado con los datos y sin IA. Por ejemplo: «Es su **3.ª iteración**. En agosto dijo que no le interesaba por ahora; en junio no contestó a 3 llamadas. **Volvió a asistir** 2 veces después del último contacto. Llegó pidiendo oración por su familia».
- **Columna izquierda:**
  - datos para contactar (teléfono con copiar y botones, dirección con «Abrir en mapas», distrito);
  - invitado por, líder y grupo;
  - la petición con la que llegó;
  - asistencia de 12 semanas (`app-indicadores-asistencia` y `app-semanas-asistencia`).
- **Columna derecha, «Línea de tiempo» (`p-timeline`):**
  - **Junta contactos y asistencias** del período del caso: «Luis llamó · Hablamos», «Asistió al Culto dominical», «Rosa visitó · No estaba». Así se ve el efecto de cada contacto.
  - Los contactos se distinguen por ícono y severidad; las asistencias, con un estilo atenuado.
- **«Iteraciones anteriores»:** acordeón (`p-accordion`), una por caso cerrado, de la más reciente a la más antigua. Cada una muestra ronda, fechas, consolidador, contactos por medio, resultado y observación de cierre, y al abrirla, sus contactos.
- **Acciones:**
  - principal: «Registrar contacto»;
  - secundaria: «Cerrar caso»;
  - en «Más»: reasignar (coordinador) y reabrir (si está cerrado y la ronda sigue en curso).

  Editar o borrar un contacto se hace desde su entrada en la línea de tiempo, con «Editable por 3 h más» visible para el consolidador.

### Historial de la persona (`/miembros/:id/consolidacion`) y ficha

- **Tarjeta «Consolidación» en la ficha:**
  - resumen en una línea («3 iteraciones · 5 llamadas · 1 visita»);
  - el último resultado con su fecha;
  - si hay un caso abierto: consolidador, estado y «Ver caso»;
  - acciones «Ver historial» e «Iniciar seguimiento» (si no tiene caso abierto y está en el ámbito).
  - Vacía: «Aún no ha tenido seguimiento de consolidación» con «Iniciar seguimiento».
- **Historial:**
  - arriba, la evolución de asistencia de 12 semanas (SPEC 09) con **marcas en las semanas en que hubo contacto**, para ver de un vistazo si volvió después de cada iteración;
  - debajo, una tarjeta por iteración (más reciente primero) con su resultado destacado y la línea de tiempo de sus contactos, paginada en el servidor.

### Resultados de consolidación (`/consolidacion/resultados`)

- Mismo patrón que niveles: lista ordenable (subir y bajar, con teclado), edición en un diálogo, activar o desactivar con explicación de que se conserva el historial, y vista previa de cómo se ven como botones al cerrar un caso.

### Accesibilidad, rendimiento y estado

- Toda grilla o lista con el paginador siempre visible y la configuración común (`shared/utils/paginacion.ts`), y su prueba de que el paginador se ve. Las secciones de «Mis asignados» se paginan cada una con el mismo paginador.
- Estados de carga (skeleton con la forma del contenido), vacío (con la siguiente acción) y error (con «Reintentar») en cada pantalla y en cada bloque que carga por separado.
- Foco gestionado:
  - al abrir una hoja, el foco va a su primer control;
  - al cerrarla, vuelve a la tarjeta que la abrió;
  - los toasts y el cambio de sección de una tarjeta se anuncian con `aria-live`.
- Botones de medio y resultado como grupos (`role="radiogroup"`) navegables con flechas, con texto e ícono, nunca solo color. Contraste suficiente en claro y oscuro.
- Fechas relativas («hace 3 días») con la fecha completa en `title` y para lectores de pantalla.
- Las listas de rondas, casos y «Mis asignados» se calculan con consultas agregadas (sin N+1), con los índices de la migración. La vista previa del lote se pide con espera de 300 ms tras el último cambio de filtro y cancela la anterior.

## Plan de implementación

1. **`iglesia-seguridad`:** migración `V9__permisos_de_consolidacion.sql`; pruebas de que los roles base reciben sus permisos.
2. **`iglesia-api`, base:** migración `V11__consolidacion.sql`, entidades, repositorios, enums con sus transiciones y `ResultadosPorDefecto` (evento `IglesiaCreada`); pruebas unitarias de las transiciones y de las reglas de contacto.
3. **`iglesia-api`, equipo y catálogo:** servicios y endpoints del equipo y de los resultados; OpenAPI; pruebas.
4. **`iglesia-api`, rondas:** CRUD, vista previa y alta del lote con `INSERT … SELECT`, repartir, reasignar, iniciar y cerrar; OpenAPI; pruebas unitarias e integración (un caso abierto por persona, aislamiento entre iglesias).
5. **`iglesia-api`, casos y contactos:** «Mis casos», detalle, seguimiento individual, contactos con ventana de corrección, cerrar y reabrir, `AmbitoConsolidacion`; OpenAPI; pruebas (ámbito del consolidador y de la red del líder, 404 fuera del ámbito).
6. **`iglesia-api`, historial y avance:** resumen e historial por persona y avance de la ronda con «volvió a asistir»; OpenAPI; pruebas.
7. **Postman:** carpeta «11. Consolidación» en `iglesia-api` (equipo, resultados, rondas, lote, reparto, mis casos, contactos, historial) y newman sin fallos.
8. **Frontend, base:** modelos, servicios, rutas, menú y componentes compartidos (selector de miembro si falta, botones de contacto).
9. **Frontend, rondas:** lista, equipo, detalle con lote, reparto y avance.
10. **Frontend, consolidador:** «Mis asignados», registrar contacto y la pantalla del caso.
11. **Frontend, persona y configuración:** tarjeta en la ficha, historial, «Iniciar seguimiento» y catálogo de resultados.
12. **Revisión de UX en el navegador:** recorrer los flujos del coordinador y del consolidador en un celular (375 px) y en escritorio, en modo claro y oscuro y solo con teclado; ajustar lo que no cumpla los principios de «Diseño de la experiencia».
13. **Datos de demostración y verificación:** en «Iglesia Demo Asistencia», un equipo de 6 consolidadores y dos rondas (una cerrada y una en curso) con contactos variados; `ng test`, `ng build`, `./gradlew test`.
14. Diagrama `diagramas/spec-10-consolidacion.drawio`: arquitectura, ciclo de una ronda, registro de un contacto y modelo de datos.

## Criterios de aceptación

- [ ] Un coordinador crea una ronda, ve en la vista previa cuántas personas entran con los filtros (incluida una alerta del SPEC 09) y las agrega en un paso; quien ya tiene un caso abierto queda fuera y se informa.
- [ ] «Repartir en partes iguales» asigna los casos sin consolidador de forma balanceada entre los elegidos, y se puede reasignar uno o varios.
- [ ] Mientras la ronda está planificada, los consolidadores no ven sus casos; al iniciarla aparecen en «Mis asignados».
- [ ] Un consolidador, con su usuario y solo `consolidacion.contactar`, ve únicamente sus asignados (y las personas de su red si es líder); otro caso o persona responde 404.
- [ ] En plena ronda, el consolidador ve en la tarjeta y en el caso las iteraciones anteriores de la persona (qué respondió, quién la contactó y con qué resultado), aunque las haya hecho otro consolidador.
- [ ] Desde «Mis asignados» se llama o se abre WhatsApp, y se registra un contacto con medio y resultado en dos toques; el caso pasa a «En contacto».
- [ ] Un coordinador puede registrar un contacto en nombre de un consolidador sin usuario; queda quién lo hizo y quién lo registró.
- [ ] Un consolidador corrige o borra sus contactos dentro de 24 h; después, no. El coordinador puede hacerlo siempre.
- [ ] Un usuario con solo `consolidacion.administrar` puede hacer todo lo del consolidador sobre cualquier caso y persona (ver, registrar contactos, cerrar, reabrir, abrir seguimientos), sin restricción de ámbito.
- [ ] Cerrar un caso exige un resultado activo del catálogo de la iglesia; cerrar la ronda deja los abiertos como «No concluido» y la ronda queda de solo lectura.
- [ ] Un líder abre un seguimiento individual a alguien de su red; no se puede abrir un segundo caso a quien ya tiene uno abierto (409).
- [ ] La ficha muestra cuántas iteraciones, llamadas y visitas tuvo la persona y su último resultado; el historial lista cada caso con sus contactos junto a su evolución de asistencia.
- [ ] El avance de la ronda muestra contactados, pendientes, cerrados, resultados, avance por consolidador y cuántos volvieron a asistir en los 28 días siguientes al contacto.
- [ ] Crear una ronda es un asistente de 4 pasos (datos, personas, reparto, revisar) con vista previa en vivo del lote y del reparto, que se puede dejar a medias como planificada.
- [ ] «Mis asignados» agrupa en «Para hoy», «Sin contactar» y «En contacto»; al volver de una llamada o un WhatsApp se abre sola la hoja de registro con el medio elegido, y registrar se hace con dos toques (qué pasó y guardar), con «Deshacer» durante 8 segundos.
- [ ] La pantalla del caso muestra «Antes de contactar», la línea de tiempo que junta contactos y asistencias, y las iteraciones anteriores.
- [ ] Todas las pantallas funcionan en un celular de 375 px y en escritorio, en modo claro y oscuro, solo con teclado, y tienen estados de carga, vacío (con siguiente acción) y error.
- [ ] Cada iglesia administra sus resultados (las nuevas reciben los seis iniciales) y los datos de una iglesia nunca se ven desde otra.
- [ ] Los logs registran los eventos con ids y nunca observaciones, nombres ni teléfonos.
- [ ] OpenAPI, Postman, pruebas (`.spec.ts` y paginador en cada grilla nueva) y diagrama actualizados.

## Decisiones

- **Consolidación separada del líder:** el líder es una relación permanente de la jerarquía; consolidar es temporal y por tandas. Guardarla en el miembro mezclaría las dos.
- **Ronda + caso + contacto:** la ronda refleja cómo trabaja hoy la iglesia (tandas); el caso permite medir cada iteración por persona; el contacto guarda cada intento. Con eso se responde «cuántas veces y qué pasó en cada iteración».
- **Seguimiento individual como caso sin ronda:** reutiliza todo (contactos, cierre, historial) sin crear rondas de una persona.
- **Un caso abierto por persona, garantizado en la base:** evita que dos consolidadores llamen a la misma persona sin saberlo.
- **El coordinador incluye al consolidador:** quien coordina debe poder cubrir a cualquier consolidador (ausente, sin usuario o que se equivocó) sin pedir permisos extra; por eso `consolidacion.administrar` implica ver y contactar en toda la iglesia.
- **Equipo en `iglesia-api` y permisos en `iglesia-seguridad`:** iglesia-api no conoce los permisos de otros usuarios, así que el equipo (a quién se puede repartir) es un dato de negocio, y el permiso `consolidacion.contactar` controla quién puede registrar.
- **Resultado de la iteración configurable, resultado del intento fijo:** el primero es vocabulario de cada iglesia; el segundo es técnico (¿se habló o no?) y permite indicadores comparables.
- **«Volvió a asistir» calculado:** se mide la efectividad con la asistencia real (SPEC 09), sin pedirle al consolidador que lo anote.
- **Al cerrar no se toca la ficha:** decidido así para mantener el alcance acotado; asignar grupo o cambiar la condición sigue haciéndose en la ficha.

### Ajustes durante la implementación

Datos y API:
- `consolidador` tiene su propio `id` (identity) con `UNIQUE (iglesia_id, miembro_id)`; los endpoints del equipo siguen identificando por `miembroId`.
- `caso_consolidacion.proximo_contacto` se guarda en el caso (el último contacto que lo fija manda), para ordenar «Para hoy» sin recorrer contactos.
- «Mis asignados» usa `GET /mis-casos?seccion=PARA_HOY|SIN_CONTACTAR|EN_CONTACTO` (cada sección pagina por su cuenta) y `GET /mis-casos/resumen` para el saludo y el medidor.
- Se agregaron `GET /rondas/{id}` y `PATCH /casos/consolidador` (reasignar varios). Los resultados no se borran: se desactivan, por eso no hay `DELETE`.
- Se quitó el paquete vacío `seguimiento` de `iglesia-api`.

Reglas:
- La fecha de un contacto admite 5 minutos de adelanto por diferencias de reloj.
- «Volvió a asistir» cuenta desde el día siguiente al primer «Se habló» y hasta 28 días después.
- Fuera de ámbito el backend responde 404; la tarjeta de la ficha se oculta con 404 o 403.

Frontend:
- La etiqueta «Sin acceso al sistema» del equipo se omitió: iglesia-api no sabe qué miembros tienen usuario (bases separadas).
- El gráfico de anillo de resultados se reemplazó por barras horizontales (se leen mejor con nombres largos y en el celular).
- El reparto se ajusta con «Mover a…» por persona en lugar de arrastrar y soltar (accesible con teclado y en el celular).
- El lote no permite excluir personas antes de confirmar (el backend solo acepta una lista de inclusión): para eso está el modo «Elegir a mano» y «Quitar del lote» después.
- El aviso de «contacto vencido» del detalle de la ronda no tiene botón «Ver».
- El saludo de «Mis asignados» no incluye el nombre: el usuario autenticado no lo trae.
- Desde la pantalla del caso, «Llamar» o «WhatsApp» abre la hoja de registro de inmediato (no espera a volver de la app), porque el caso ya está en pantalla.
- La franja de 12 semanas de asistencia (`app-semanas-asistencia`) acepta `marcas` para señalar las semanas con contacto en el historial.

## Riesgos

- **Datos sensibles en las observaciones** (situaciones personales, peticiones): se limitan por ámbito, nunca van a los logs y se muestran solo a quien tiene el caso, su red o `consolidacion.ver`. Conviene recordarlo en la ayuda de la pantalla.
- **Teléfonos con formatos variados:** el botón de WhatsApp necesita el número con código de país. Se normaliza en el frontend con el prefijo del país de la iglesia; si no se puede, se muestra solo «Llamar».
- **Consolidadores sin usuario:** si la iglesia no crea los usuarios, todo lo registra el coordinador y se pierde el valor de «Mis asignados». La pantalla de equipo marca quién no tiene acceso todavía.
- **Rondas grandes:** el alta del lote y el avance se resuelven con sentencias de conjunto y agregados, nunca persona por persona.

## Qué **no** entra en esta spec

Ver «Fuera de alcance» en «Alcance».
