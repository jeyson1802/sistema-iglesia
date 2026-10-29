# SPEC 09 — Asistencia a cultos, eventos y grupos

> **Estado:** Aprobado
> **Depende de:** SPEC 02 (permisos y perfiles personalizados), SPEC 03 (OpenAPI y trazabilidad), SPEC 06 (miembros, grupos y su jerarquía), SPEC 07 (regla de grillas paginadas), SPEC 08 (identidad de la iglesia para el fotocheck)
> **Fecha:** 2026-10-29
> **Objetivo:** Que cada iglesia tome la asistencia de sus cultos, eventos y reuniones de grupo de la forma más rápida posible: en las reuniones grandes, escaneando con un celular (o con un lector de códigos) el QR personal de cada miembro, que lleva en su celular o en un fotocheck impreso; en los grupos, marcando a mano a sus integrantes. Siempre se puede marcar a mano, y la asistencia queda consultable por reunión, por grupo y por miembro.

## Por qué existe esta spec

La asistencia es el módulo prioritario de la hoja de ruta (`CLAUDE.md`, punto 5) y hoy no existe:

- Cada iglesia tiene cultos con día y hora fijos (p. ej. «Culto dominical», domingos 10:00) y, de vez en cuando, eventos (convenciones, vigilias, campañas). El sistema no conoce ninguno de los dos.
- Los grupos ya tienen día y hora de reunión (SPEC 06), pero no se registra quién asistió.
- En un culto con cientos de personas, pasar lista a mano no es viable: hace falta marcar a cada persona en uno o dos segundos.
- Sin asistencia no se puede dar seguimiento a quien deja de venir (base de la futura spec de seguimiento y consolidación).

## Alcance

**Entra:**

- **Horarios de culto:** cada iglesia define sus cultos fijos con nombre, día de la semana y hora (p. ej. «Culto dominical · domingo 10:00», «Culto de oración · miércoles 19:30»). Nada viene codificado: una iglesia nueva empieza sin cultos y el administrador los crea.
- **Eventos:** reuniones que no se repiten (convención, vigilia, bautismo…), con nombre, fecha y hora. Un evento de varios días se crea de una vez eligiendo fecha de inicio y de fin (hasta 7 días) y genera una reunión por día.
- **Reuniones de grupo:** cada grupo activo con día de reunión tiene su reunión semanal, con el día y la hora que ya tiene registrados.
- **Reunión:** cada ocurrencia concreta donde se toma asistencia (el culto dominical del 1 de noviembre, el grupo «Betel» del jueves 5). Las de cultos y grupos se crean solas la primera vez que alguien toma asistencia ese día; no hay tareas programadas.
- **Código QR personal:**
  - Cada miembro tiene un código de asistencia aleatorio, que no revela su id, su nombre ni su documento. Es fijo: sirve igual en la pantalla del celular y en el fotocheck impreso.
  - Se puede **regenerar** (p. ej. si se pierde el fotocheck): el código anterior deja de valer al instante.
  - Desde la ficha del miembro se **descarga** el fotocheck como imagen, se **comparte** (WhatsApp u otra app, con el menú de compartir del celular) o se **imprime**. El miembro lo guarda en la galería de su celular.
  - **Impresión en lote:** fotochecks de varios miembros (filtrados por grupo, nivel o condición) en hojas A4 listas para recortar.
- **Tomar asistencia** con tres modos en la misma pantalla:
  - **Cámara:** escaneo continuo con la cámara del celular o la tableta, sin tocar la pantalla entre una persona y la siguiente.
  - **Lector:** lectores de códigos USB, Bluetooth o de gatillo, que se comportan como un teclado: escriben el código y un Enter. Deben ser lectores **2D** (ver «Equipos recomendados»).
  - **Manual:** buscar por nombre y marcar, o, en los grupos, la lista de integrantes con un interruptor por persona.
- **Consulta:** historial de reuniones con su total de asistentes; detalle de una reunión con la lista de asistentes; asistencia de un grupo en sus últimas semanas; y asistencia de un miembro en su ficha.
- **Permisos nuevos** en `iglesia-seguridad` (ver «Permisos y ámbito»), pensados para que la iglesia cree un perfil «Ujier» o «Servidor de asistencia» que solo marque asistencia.
- **Documentación y cierre:** OpenAPI, Postman, pruebas y diagrama, como en las specs anteriores.

**Fuera de alcance (para specs futuras):**

- Visitantes no registrados: no se cuentan ni se registran desde la asistencia. Quien llega por primera vez se registra en Miembros como hasta ahora.
- Funcionar sin conexión: si no hay internet, la pantalla lo avisa y no marca. Se puede marcar a mano después.
- QR dentro del portal del miembro o en billeteras del celular (Apple Wallet, Google Wallet): llega con la spec del portal.
- QR dinámico (que cambia cada pocos segundos).
- Autoregistro del miembro (que él mismo escanee un QR de la reunión).
- Alertas por inasistencia y seguimiento de quien deja de venir (spec de seguimiento y consolidación).
- Exportar a Excel o PDF y reportes globales entre iglesias.
- Hora de llegada como dato de puntualidad (se guarda cuándo se marcó, pero no se reporta tardanza).

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V9__asistencia.sql`

```sql
-- Cultos fijos de cada iglesia.
CREATE TABLE culto_programado (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre     VARCHAR(60) NOT NULL,
    dia_semana VARCHAR(10) NOT NULL CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    hora       TIME        NOT NULL,
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_culto_programado_nombre ON culto_programado (iglesia_id, lower(nombre));

-- Cada ocurrencia concreta donde se toma asistencia.
CREATE TABLE reunion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id          BIGINT       NOT NULL REFERENCES iglesia (id),
    tipo                VARCHAR(10)  NOT NULL CHECK (tipo IN ('CULTO', 'EVENTO', 'GRUPO')),
    -- Se copia del culto, del grupo o del evento para que el historial no cambie si luego se renombran.
    nombre              VARCHAR(100) NOT NULL,
    fecha               DATE         NOT NULL,
    hora                TIME,
    culto_programado_id BIGINT,
    grupo_id            BIGINT,
    creada_por          BIGINT       NOT NULL,
    creada_en           TIMESTAMPTZ  NOT NULL,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, culto_programado_id) REFERENCES culto_programado (iglesia_id, id),
    FOREIGN KEY (iglesia_id, grupo_id)            REFERENCES grupo (iglesia_id, id),
    CONSTRAINT ck_reunion_origen CHECK (
        (tipo = 'CULTO'  AND culto_programado_id IS NOT NULL AND grupo_id IS NULL) OR
        (tipo = 'GRUPO'  AND grupo_id IS NOT NULL AND culto_programado_id IS NULL) OR
        (tipo = 'EVENTO' AND culto_programado_id IS NULL AND grupo_id IS NULL))
);
-- Una sola reunión por culto o por grupo y día: tomar asistencia dos veces el mismo día abre la misma.
CREATE UNIQUE INDEX ux_reunion_culto_fecha ON reunion (iglesia_id, culto_programado_id, fecha) WHERE culto_programado_id IS NOT NULL;
CREATE UNIQUE INDEX ux_reunion_grupo_fecha ON reunion (iglesia_id, grupo_id, fecha) WHERE grupo_id IS NOT NULL;
CREATE INDEX ix_reunion_fecha ON reunion (iglesia_id, fecha DESC);

CREATE TABLE asistencia (
    iglesia_id     BIGINT      NOT NULL REFERENCES iglesia (id),
    reunion_id     BIGINT      NOT NULL,
    miembro_id     BIGINT      NOT NULL,
    medio          VARCHAR(10) NOT NULL CHECK (medio IN ('QR', 'MANUAL')),
    registrada_por BIGINT      NOT NULL,
    registrada_en  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (iglesia_id, reunion_id, miembro_id),
    FOREIGN KEY (iglesia_id, reunion_id) REFERENCES reunion (iglesia_id, id) ON DELETE CASCADE,
    FOREIGN KEY (iglesia_id, miembro_id) REFERENCES miembro (iglesia_id, id)
);
CREATE INDEX ix_asistencia_miembro ON asistencia (iglesia_id, miembro_id);

-- Código del QR personal. Nulo hasta que se genera (al crear el miembro o al pedir su QR por primera vez).
ALTER TABLE miembro ADD COLUMN codigo_asistencia VARCHAR(16);
CREATE UNIQUE INDEX ux_miembro_codigo_asistencia ON miembro (iglesia_id, codigo_asistencia) WHERE codigo_asistencia IS NOT NULL;

SELECT habilitar_aislamiento_por_iglesia('culto_programado');
SELECT habilitar_aislamiento_por_iglesia('reunion');
SELECT habilitar_aislamiento_por_iglesia('asistencia');
```

- `creada_por` y `registrada_por` guardan el `usuarioId` del token, sin FK porque vive en otra base (igual que en aportes).
- La clave primaria de `asistencia` impide marcar dos veces a la misma persona en la misma reunión.
- Quitar una asistencia borra su fila. Una reunión no se borra: se queda en el historial aunque tenga cero asistentes, salvo un evento sin asistentes, que se puede eliminar con `asistencia.configurar` si se creó por error.

### Migración `iglesia-seguridad/src/main/resources/db/migration/V8__permisos_de_asistencia.sql`

Agrega los permisos de «Permisos y ámbito» con su descripción y orden en el módulo «Asistencia», y los asigna a los roles base.

### Código QR

- **Código:** 12 caracteres en base32 de Crockford (sin `I`, `L`, `O` ni `U`, para que no se confundan al leerlos o escribirlos), generados con `SecureRandom` (60 bits). Único por iglesia. Ejemplo: `7K2QM9XDR4TB`.
- **Contenido del QR:** `ASIS:7K2QM9XDR4TB`. El prefijo permite rechazar al instante cualquier otro QR (un enlace, el QR de un pago…) con el mensaje «Este QR no es un código de asistencia». El servidor busca el código **solo dentro de la iglesia del token**: el fotocheck de otra iglesia responde «Código no encontrado en esta iglesia».
- **Se genera** al registrar un miembro y, para los que ya existen, la primera vez que se pide su QR o su fotocheck.
- **Regenerar** (permiso `miembros.editar`) reemplaza el código; el anterior responde «Código no encontrado». Se registra en `INFO` con `miembroId` y `usuarioId`, nunca con el código.
- El código se muestra impreso bajo el QR, para escribirlo a mano en el modo «Lector» si el QR está dañado.

### Reglas

- **Horario de culto:** nombre obligatorio (2 a 60 caracteres, único por iglesia sin distinguir mayúsculas, 409), día y hora obligatorios. Se desactiva, no se borra, para conservar su historial.
- **Evento:** nombre obligatorio (2 a 100 caracteres), fecha de inicio, fecha de fin opcional (de 0 a 6 días después de la de inicio; si no, 400) y hora opcional.
- **Fechas:** no se toma asistencia en una fecha futura (400). Sí en fechas pasadas, para corregir o completar.
- **Reunión de culto o grupo:** se pide con el culto o el grupo y la fecha (`POST /api/reuniones`); si ya existe, se devuelve la misma (200). El día de la fecha no tiene que coincidir con el día programado: un culto se puede mover a otro día excepcionalmente.
- **Marcar por QR:**
  - Código válido de un miembro activo: se marca (201) y se devuelve el miembro (nombre, nivel y grupo) para mostrarlo en pantalla.
  - Ya estaba marcado: 200 con `yaRegistrado = true` y la hora en que se marcó. No es un error: el ujier solo necesita saber que la persona está.
  - Código desconocido: 404 «Código no encontrado en esta iglesia». Miembro inactivo: 409 «El miembro está inactivo».
  - Texto que no empieza por `ASIS:` y no es un código válido de 12 caracteres: 400 «Este QR no es un código de asistencia».
- **Marcar a mano:** por `miembroId`, con las mismas respuestas.
- **Quitar una asistencia:** borra la marca (204). Es el «Deshacer» de la pantalla.
- **Reuniones de grupo:** se puede marcar a cualquier miembro activo, no solo a los integrantes del grupo (p. ej. alguien que visitó otro grupo).
- **Logs:** creación de reuniones y eventos, marcas por QR o a mano, quitar asistencia y regenerar código se registran en `INFO` con ids (`reunionId`, `miembroId`, `usuarioId`), sin nombres ni códigos. Los códigos inválidos se registran en `WARN` sin el texto leído.

### Permisos y ámbito

| Permiso | Para qué | Roles base |
|---|---|---|
| `asistencia.registrar` | Tomar asistencia (escaneo y manual) en cultos y eventos, y en las reuniones de cualquier grupo. | `ADMIN_IGLESIA` |
| `asistencia.grupo` | Tomar asistencia y verla en las reuniones de los grupos **que lidera**. | `LIDER` |
| `asistencia.ver` | Ver el historial y el detalle de todas las reuniones de la iglesia y la asistencia de cualquier miembro. | `ADMIN_IGLESIA` |
| `asistencia.configurar` | Administrar horarios de culto y crear o eliminar eventos. | `ADMIN_IGLESIA` |

- **Ujier o servidor de asistencia:** la iglesia crea un perfil personalizado (SPEC 02) con solo `asistencia.registrar`. Ese perfil ve la pantalla «Asistencia» con las reuniones de hoy y marca, sin acceder a los demás módulos ni a los reportes.
- **Líder:** con `asistencia.grupo` ve y marca solo las reuniones de los grupos cuyo `lider_id` es su `miembroId` del token. Una reunión de otro grupo responde 404 (no se revela que existe).
- **Asistencia de un miembro** (su ficha): con `asistencia.ver`, cualquiera; con `miembros.ver` y ámbito de red (líder), los miembros de su red, como el resto de la ficha.
- **QR y fotocheck** de un miembro: se ven con `miembros.ver` dentro del ámbito de miembros; regenerar el código exige `miembros.editar`.

## Backend `iglesia-api`

Nuevo contexto **`asistencia/`** (`com.iglesia.api.asistencia.<tipo>`):

- **`entity`:** `CultoProgramado`, `Reunion` y `Asistencia` (clave compuesta).
- **`model`:** `TipoReunion` (`CULTO`, `EVENTO`, `GRUPO`), `MedioAsistencia` (`QR`, `MANUAL`), `DiaSemana` (se reutiliza el de grupos si ya existe en su `model`), `CodigoAsistencia` (value object: valida el formato, quita el prefijo y normaliza a mayúsculas).
- **`component`:** `GeneradorCodigoAsistencia` (`SecureRandom`, base32 de Crockford; reintenta si choca con uno existente).
- **`service`:**
  - `CultoProgramadoService`: CRUD y orden por día y hora.
  - `ReunionService`: `abrir(culto o grupo, fecha)` idempotente, `crearEvento(...)` (una reunión por día), `reunionesDeHoy()` (cultos activos del día de la semana, eventos de hoy y grupos del día, según el ámbito), historial paginado con filtros y eliminar evento vacío.
  - `AsistenciaService`: `marcarPorCodigo`, `marcarMiembro`, `quitar`, asistentes de una reunión y asistencia de un miembro.
  - `AmbitoAsistencia`: decide con los permisos del token y el `miembroId` qué reuniones puede ver o marcar (patrón de `AmbitoAportes`).
  - `CodigoAsistenciaService` (o dentro de `miembros/service`, que es dueño de la columna): obtener o generar el código de un miembro y regenerarlo. `asistencia` lo usa por su `service`, sin depender de las entidades de miembros.
- **Consultas:**
  - Totales por reunión con `COUNT` agrupado en JPQL (sin N+1).
  - Asistencia de un miembro: últimas reuniones y porcentaje de las últimas 8 semanas, calculado en la base sobre las reuniones de los cultos activos y de su grupo.
  - Todas las consultas filtran por `iglesia_id` (Hibernate `@TenantId` y RLS).

### Endpoints de `iglesia-api`

| Método y ruta | Permiso | Descripción |
|---|---|---|
| `GET /api/cultos-programados` | `asistencia.configurar` o `asistencia.registrar` | Horarios de culto ordenados por día y hora. |
| `POST · PUT /api/cultos-programados[/{id}]` · `PATCH /{id}/estado` | `asistencia.configurar` | Alta, edición y activar o desactivar. 409 nombre repetido. |
| `GET /api/reuniones/hoy` | `asistencia.registrar` o `asistencia.grupo` | Reuniones de hoy (abiertas o por abrir) con su total de asistentes, según el ámbito. Las que aún no existen vienen sin `id`, con su culto o grupo. |
| `POST /api/reuniones` | `asistencia.registrar` o `asistencia.grupo` (su grupo) | Abre (o devuelve) la reunión de un culto o un grupo en una fecha. 201 o 200. |
| `POST /api/reuniones/eventos` | `asistencia.configurar` | Crea un evento de uno a siete días; devuelve las reuniones creadas. |
| `DELETE /api/reuniones/{id}` | `asistencia.configurar` | Elimina un evento sin asistentes. 409 si tiene asistentes o no es un evento. |
| `GET /api/reuniones?desde=&hasta=&tipo=&grupoId=&page=&size=` | `asistencia.ver` o `asistencia.grupo` (sus grupos) | Historial paginado con el total de asistentes por reunión. |
| `GET /api/reuniones/{id}` | ídem | Datos de la reunión y su total. |
| `GET /api/reuniones/{id}/asistentes?q=&page=&size=` | ídem, o `asistencia.registrar` | Asistentes paginados (nombre, nivel, grupo, medio y hora), los últimos primero. |
| `POST /api/reuniones/{id}/asistencias/codigo` | `asistencia.registrar` o `asistencia.grupo` (su grupo) | Body `{ codigo }` (con o sin `ASIS:`). 201 marcada · 200 ya registrada · 400 · 404 · 409. |
| `POST /api/reuniones/{id}/asistencias` | ídem | Body `{ miembroId }`: marca a mano. Mismas respuestas. |
| `DELETE /api/reuniones/{id}/asistencias/{miembroId}` | ídem | Quita la asistencia. 204. |
| `GET /api/reuniones/{id}/integrantes` | ídem (solo reuniones de grupo) | Integrantes del grupo con `presente` por cada uno, para la lista manual. |
| `GET /api/miembros/{id}/asistencia` | `asistencia.ver` o `miembros.ver` con ámbito | Porcentaje de las últimas 8 semanas y últimas reuniones. |
| `GET /api/miembros/{id}/codigo-asistencia` | `miembros.ver` con ámbito | Código del QR (lo genera si no tiene). |
| `POST /api/miembros/{id}/codigo-asistencia` | `miembros.editar` | Regenera el código. |

- Errores como `ProblemDetail` con `traceId`: 400, 401, 403, 404 y 409 según cada regla.
- La respuesta de una marca es pequeña (`miembroId`, nombre completo, nivel, grupo, `yaRegistrado`, `registradaEn`, `totalAsistentes`) para que el escaneo sea rápido y el contador se actualice sin otra petición.

## Frontend (`iglesia-web/src/app/`)

- **`features/asistencia/`** (rutas con `loadChildren`, `iglesiaFijadaGuard`):
  - `/asistencia` → `AsistenciaInicioComponent`: «Hoy» y el historial.
  - `/asistencia/reuniones/:id` → `TomarAsistenciaComponent` (pantalla de escaneo y marcado).
  - `/asistencia/reuniones/:id/detalle` → `DetalleReunionComponent` (lista de asistentes).
  - `/asistencia/configuracion` → `ConfiguracionAsistenciaComponent` (horarios de culto).
  - Servicios con `HttpClient`: `ReunionesService`, `AsistenciasService`, `CultosProgramadosService`.
  - Componentes presentacionales: `TarjetaReunionHoy`, `EscanerCamara`, `EntradaLector`, `MarcadoManual`, `ListaIntegrantes`, `ResultadoMarca`, `UltimasMarcas`, `TablaReuniones`, `FormularioEvento`, `TablaCultos`, `FormularioCulto`.
  - `ProcesadorCodigos` (servicio): recibe lo leído por la cámara o el lector, descarta lo que no tiene formato, ignora el mismo código durante 3 segundos (la cámara lo lee varias veces por segundo) y lo envía.
- **`shared/components/`:**
  - `fotocheck` (presentacional): tarjeta tamaño credencial con la identidad de la iglesia, el nombre del miembro y su QR. Se reutiliza en la ficha, en la impresión en lote y para generar la imagen.
  - `codigo-asistencia-miembro`: tarjeta de la ficha del miembro con el QR y las acciones.
- **`features/miembros/`:** tarjeta «Código de asistencia» y tarjeta «Asistencia» en la ficha; acción «Imprimir fotochecks» en el listado con los filtros actuales.
- **`features/grupos/`:** en el detalle del grupo, la asistencia de sus últimas 8 reuniones y el acceso «Tomar asistencia» del día.
- **Menú:** «Iglesia › Asistencia» (`pi-check-square`, visible con `asistencia.registrar`, `asistencia.grupo` o `asistencia.ver`) y «Administración › Horarios de culto» (`pi-clock`, `asistencia.configurar`).
- **Librerías:**
  - Lectura de QR con la cámara: `qr-scanner` (Nimiq), que decodifica en un web worker y usa el `BarcodeDetector` nativo del navegador cuando existe. Se confirma la versión al implementar.
  - Generación del QR: `qrcode`, en el navegador. El backend solo entrega el código, nunca imágenes.
  - Sin librería de PDF: la impresión usa la del navegador («Guardar como PDF» incluido) con las variantes `print:` de Tailwind.
- **APIs del navegador**, todas opcionales y con alternativa si no existen:
  - `navigator.vibrate` y un sonido corto al marcar.
  - Screen Wake Lock para que la pantalla no se apague mientras se escanea.
  - `navigator.share` con archivo para compartir el fotocheck; si no existe, se descarga.

## Diseño de la experiencia

El principio: **la persona que toma asistencia no debería tener que mirar la pantalla salvo cuando algo sale mal.** Todo lo demás se resuelve con colores grandes, sonido y vibración.

### Inicio de Asistencia (`/asistencia`)

- **«Hoy»** arriba, con una tarjeta grande por reunión del día, ordenadas por hora: nombre, hora, tipo (etiqueta «Culto», «Evento» o «Grupo») y el total de presentes si ya se abrió. Toda la tarjeta es un botón «Tomar asistencia».
- Si hoy no hay reuniones: estado vacío «Hoy no hay cultos ni reuniones programadas» con las acciones «Crear evento» y, si tiene permiso, «Configurar horarios de culto».
- Un ujier o un líder que solo marca ve solo «Hoy»: es su pantalla de trabajo.
- **Historial** debajo (con `asistencia.ver` o `asistencia.grupo`): filtros de rango de fechas (atajos «Esta semana», «Este mes», «Mes anterior», como en aportes) y tipo; grilla paginada en el servidor con fecha, reunión, tipo y asistentes; clic en la fila abre el detalle. En el móvil, tarjetas con el mismo paginador.
- Acción secundaria «Tomar asistencia de otra fecha» para corregir una reunión pasada (elige culto o grupo y fecha).

### Tomar asistencia (`/asistencia/reuniones/:id`)

Pantalla pensada primero para el celular, usable con una mano:

- **Cabecera fija:** nombre de la reunión y fecha, y un **contador grande** «128 presentes» que se actualiza con cada marca.
- **Selector de modo** (`p-selectbutton`) con tres opciones: «Cámara», «Lector» y «Manual».
  - Modo inicial: «Manual» en las reuniones de grupo; «Cámara» en cultos y eventos desde el celular; «Lector» desde una computadora.
  - Se recuerda el último modo elegido por tipo de reunión en ese navegador.
- **Cámara:**
  - Vista de la cámara a todo el ancho, con un marco que indica dónde poner el QR.
  - Usa la cámara trasera por defecto, con un botón para cambiar de cámara y otro para la linterna si el equipo la tiene.
  - Escanea sin parar: no hay que tocar nada entre una persona y otra.
  - Si el navegador no da permiso de cámara, explica cómo activarlo y ofrece pasar a «Lector» o «Manual».
- **Lector:**
  - Un campo grande y siempre enfocado, con el texto «Pase el fotocheck por el lector». Si pierde el foco (p. ej. por un clic), lo recupera solo.
  - El lector escribe el código y un Enter. También se puede escribir a mano el código impreso bajo el QR.
- **Resultado de cada marca:** una franja grande sobre la vista, que dura 1,5 segundos y no bloquea el siguiente escaneo:
  - **Verde** con ✓, el nombre completo y el nivel: «Marcado». Vibración corta y sonido agudo.
  - **Ámbar**: «Ya estaba registrado a las 10:02». Sonido distinto.
  - **Rojo** con el motivo: «Código no encontrado en esta iglesia», «El miembro está inactivo» o «Este QR no es un código de asistencia». Vibración doble y sonido grave. Se queda hasta el siguiente escaneo.
  - Además del color, siempre un ícono y un texto, y la franja se anuncia a los lectores de pantalla (`aria-live`).
- **Últimas marcas:** debajo, las últimas cinco personas marcadas con su hora y un botón «Deshacer» en cada una, por si se escaneó a la persona equivocada o dos veces.
- **Manual:**
  - Cultos y eventos: buscador por nombre (con espera de 300 ms). Cada resultado muestra nombre, nivel y grupo con un botón «Marcar», o «Presente ✓» si ya está (al pulsarlo se quita, con confirmación).
  - Grupos: la lista de integrantes del grupo con un `p-toggleswitch` «Presente» por persona, ordenados por nombre. Arriba, «Marcar a todos» y el buscador para agregar a alguien que no es del grupo. Cada cambio se guarda al instante (sin botón «Guardar») y se revierte con un aviso si falla.
- **Sin conexión:** una franja roja fija «Sin conexión: no se están guardando las asistencias» y el escaneo se pausa hasta que vuelve la conexión.
- La pantalla se mantiene encendida mientras está abierta (Wake Lock).
- Botón «Terminar» que vuelve a «Hoy». No hay que «cerrar» la reunión: siempre se puede volver a ella.

### Detalle de una reunión

- Cabecera con nombre, fecha, hora y total; acción «Tomar asistencia» para seguir marcando.
- Grilla paginada de asistentes con buscador: nombre, nivel, grupo, medio (ícono de QR o de mano) y hora.
- En un evento sin asistentes, «Eliminar evento» con confirmación.

### Horarios de culto (`/asistencia/configuracion`)

- Grilla paginada en el navegador con nombre, día, hora y estado.
- Acciones «Nuevo culto», «Editar» y «Activar/Desactivar».
- El formulario usa el día como `p-selectbutton` con las iniciales («L M M J V S D», con el nombre completo como `aria-label`) y un selector de hora.
- «Crear evento» está en el inicio de Asistencia: nombre, fecha de inicio, fecha de fin opcional («Una sola fecha» por defecto) y hora opcional.

### Fotocheck y código QR

- **Ficha del miembro, tarjeta «Código de asistencia»:** vista previa del fotocheck y cuatro acciones:
  - «Descargar» (PNG en alta resolución, nombre de archivo `fotocheck-<nombres>-<apellidos>.png`);
  - «Compartir» (menú de compartir del celular, con el texto «Tu código de asistencia de &lt;Nombre comercial&gt;. Guárdalo y muéstralo al llegar»);
  - «Imprimir»;
  - «Regenerar código», con confirmación: «El fotocheck y la imagen actuales dejarán de funcionar».
- **Diseño del fotocheck:** tamaño credencial CR80 (85,6 × 54 mm) en horizontal.
  - A la izquierda, el logo y el nombre comercial de la iglesia con su tipografía de marca, y debajo los nombres y apellidos del miembro y su nivel.
  - A la derecha, el QR (mínimo 25 mm, con margen blanco para que lo lea cualquier lector) y debajo el código impreso en grupos de cuatro (`7K2Q M9XD R4TB`).
  - Siempre en negro sobre blanco para el QR, aunque la iglesia use modo oscuro, y con el color primario de la iglesia solo en una franja decorativa.
- **Impresión en lote:**
  - Desde el listado de miembros con sus filtros, «Imprimir fotochecks (45)» abre una vista previa en hojas A4 con 10 fotochecks por hoja (2 × 5) y líneas de corte.
  - Al confirmar se abre el diálogo de impresión del navegador.
  - Tope de 500 por impresión; si hay más, pide filtrar.

### Ficha del miembro y grupos

- **Tarjeta «Asistencia» del miembro:** porcentaje de las últimas 8 semanas con una barra, y sus últimas 5 reuniones con fecha y nombre. Estado vacío «Aún no tiene asistencias registradas».
- **Detalle del grupo:** asistentes de sus últimas 8 reuniones en un gráfico de barras pequeño y el botón «Tomar asistencia de hoy» si hoy es su día de reunión (o «de otra fecha»).

### Equipos recomendados

Esta sección se documenta también como ayuda dentro de la pantalla de escaneo («¿Qué equipo uso?»), para que la iglesia sepa qué comprar.

| | Cámara del celular | Lector de códigos 2D |
|---|---|---|
| Costo | Ninguno: cada servidor usa su celular | Aprox. entre 25 y 80 USD por lector (referencial) |
| Velocidad | Alrededor de 1 segundo por persona | Menos de medio segundo |
| Poca luz | Le cuesta más; ayuda la linterna | Lee bien, tiene luz propia |
| QR en la pantalla de un celular | Sí | Sí |
| Varias entradas a la vez | Un celular por puerta, sin costo | Un lector por puerta |
| Batería | La cámara encendida consume bastante | El lector USB no usa la del celular |

- **El lector debe ser 2D («imager»):** los lectores láser o «infrarrojos» de luz roja, como los de supermercado, suelen ser **1D**: leen solo códigos de barras lineales y **no leen QR**. Al comprar, se verifica que diga «2D» o «QR». Un lector 2D lee el fotocheck impreso y el QR en la pantalla de un celular.
- **Integración:**
  - **Cámara:** la página pide permiso al navegador (`getUserMedia`), muestra la imagen en vivo y `qr-scanner` busca el QR varias veces por segundo. No requiere instalar ninguna app, pero el navegador exige HTTPS.
  - **Lector:** se conecta por USB o se empareja por Bluetooth como un teclado; al leer, «escribe» `ASIS:…` y Enter en el campo enfocado. No requiere drivers, permisos ni integración con una marca concreta (ni WebUSB ni WebHID).
- **Organización sugerida para un culto grande:**
  - en la entrada principal, una laptop o tableta con un lector 2D;
  - en las demás puertas, servidores con su celular en modo «Cámara»;
  - para quien olvidó su fotocheck, el modo «Manual».

  Todos marcan la misma reunión a la vez y el contador suma lo de todos.
- **Para empezar no se necesita comprar nada:** la cámara del celular basta. El lector se agrega después si la fila de la entrada principal es muy larga.

### Accesibilidad y rendimiento

- Objetivos táctiles de al menos 48 px en la pantalla de escaneo; contraste AA en las franjas de resultado; todo usable con teclado.
- Una marca debe verse en pantalla en menos de 1 segundo con buena conexión: la petición es pequeña, el contador viene en la respuesta y no se recarga la lista completa.
- La librería de cámara se carga solo en la pantalla de escaneo (`@defer` o importación dinámica), sin engordar el resto de la aplicación.

## Plan de implementación

1. **`iglesia-seguridad`:** migración `V8` con los cuatro permisos y su asignación a `ADMIN_IGLESIA` y `LIDER`; constantes y pruebas de roles.
2. **`iglesia-api`, base de datos:** migración `V9`; entidades, repositorios y `CodigoAsistencia`.
3. **`iglesia-api`, código QR:** generador, generación al crear un miembro, obtener y regenerar; pruebas.
4. **`iglesia-api`, horarios y reuniones:** `CultoProgramadoService`, `ReunionService` (hoy, abrir idempotente, eventos, historial) y `AmbitoAsistencia`; pruebas unitarias.
5. **`iglesia-api`, marcar asistencia:** por código, a mano, quitar, asistentes, integrantes y asistencia del miembro; pruebas unitarias y de integración (incluido el aislamiento entre iglesias y el ámbito del líder); OpenAPI.
6. **Postman:** carpeta «Asistencia» (horarios, eventos, reuniones de hoy, marcar por QR, repetido, inválido, de otra iglesia, manual, quitar, historial) y QR en «Miembros».
7. **Frontend, fotocheck:** componente `fotocheck`, tarjeta de código en la ficha (descargar, compartir, imprimir, regenerar) e impresión en lote.
8. **Frontend, inicio y configuración:** `/asistencia` (hoy e historial), horarios de culto, eventos y menú.
9. **Frontend, tomar asistencia:** pantalla con los modos Cámara, Lector y Manual, resultado con sonido y vibración, últimas marcas con deshacer, lista de integrantes del grupo y aviso sin conexión.
10. **Frontend, consultas:** detalle de reunión, tarjeta de asistencia del miembro y asistencia en el detalle del grupo.
11. **Pruebas, build y verificación en vivo:** `ng test`, `ng build`, `./gradlew test`, newman, prueba de humo y prueba real de escaneo con un celular (cámara) y con el modo Lector.
12. Crear `diagramas/spec-09-asistencia.drawio` con varias páginas, colores con leyenda y notas: arquitectura; flujo de un escaneo (cámara o lector → validación → resultado); apertura de reuniones del día; fotocheck y código QR (generar, compartir, regenerar).

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Existen `asistencia.registrar`, `asistencia.grupo`, `asistencia.ver` y `asistencia.configurar`; `ADMIN_IGLESIA` tiene los cuatro salvo `asistencia.grupo`, y `LIDER` tiene `asistencia.grupo`.
- [ ] Un perfil personalizado con solo `asistencia.registrar` puede tomar asistencia y no puede ver el historial ni otros módulos.

**Backend `iglesia-api`**

- [ ] Los horarios de culto se crean, editan y desactivan; un nombre repetido responde 409.
- [ ] `GET /api/reuniones/hoy` devuelve los cultos activos del día, los eventos de hoy y los grupos que se reúnen hoy, según el ámbito del usuario.
- [ ] Abrir dos veces la reunión de un culto o un grupo en la misma fecha devuelve la misma reunión; una fecha futura responde 400.
- [ ] Un evento de 3 días crea 3 reuniones; uno de más de 7 días responde 400; un evento sin asistentes se puede eliminar y uno con asistentes, no (409).
- [ ] Marcar con un código válido responde 201 con el nombre del miembro y el total; repetirlo responde 200 con `yaRegistrado`; un código de otra iglesia responde 404; un miembro inactivo, 409; un texto que no es un código, 400.
- [ ] El código acepta el prefijo `ASIS:` y minúsculas; regenerarlo invalida el anterior.
- [ ] Un líder solo ve y marca las reuniones de los grupos que lidera; otra reunión responde 404.
- [ ] Una iglesia no puede leer ni marcar reuniones de otra (Hibernate y RLS).
- [ ] Los logs no contienen nombres, códigos de asistencia ni lo leído por el escáner.
- [ ] OpenAPI y Postman completos; newman sin fallos.

**Frontend**

- [ ] «Asistencia» muestra las reuniones de hoy como tarjetas que abren la toma de asistencia, y el historial paginado con filtros para quien puede verlo.
- [ ] En la cámara del celular, escanear varios fotochecks seguidos marca a cada persona sin tocar la pantalla, con resultado verde, ámbar o rojo, sonido y vibración.
- [ ] Con un lector USB o de gatillo (o escribiendo el código y Enter) se marca igual; el campo recupera el foco solo.
- [ ] El mismo QR leído varias veces seguidas por la cámara genera una sola petición.
- [ ] Dos equipos marcando la misma reunión a la vez (p. ej. un celular con cámara y una laptop con lector) no duplican asistencias y ambos contadores reflejan el total.
- [ ] La pantalla de escaneo ofrece la ayuda «¿Qué equipo uso?» con la recomendación de celular o lector 2D.
- [ ] «Deshacer» en las últimas marcas quita la asistencia y actualiza el contador.
- [ ] En una reunión de grupo, el modo inicial es «Manual» con la lista de integrantes; cada interruptor guarda al instante y «Marcar a todos» funciona.
- [ ] Sin permiso de cámara o sin conexión, la pantalla lo explica y ofrece una alternativa.
- [ ] La ficha del miembro permite descargar, compartir, imprimir y regenerar el fotocheck; la impresión en lote genera hojas A4 con 10 fotochecks y líneas de corte.
- [ ] El QR del fotocheck descargado o impreso se lee correctamente con la pantalla de escaneo.
- [ ] La ficha del miembro muestra su porcentaje de asistencia y sus últimas reuniones; el detalle del grupo, la asistencia de sus últimas reuniones.
- [ ] Toda grilla nueva usa el paginador común; cada componente, servicio y pipe nuevo tiene su `.spec.ts`; `ng test` y `ng build` pasan.

## Decisiones

- **Código fijo y regenerable:** permite que el mismo código sirva en el celular y en el fotocheck impreso. Si se pierde un fotocheck, regenerarlo invalida el anterior.
- **Código aleatorio y no el id ni el documento:** un QR fotografiado o reenviado no revela datos personales ni permite adivinar los de otros miembros.
- **El QR llega al celular como imagen** (descargar o compartir desde la ficha), porque los miembros todavía no tienen usuario. El QR dentro del portal llegará con su spec.
- **Una sola pantalla con tres modos (Cámara, Lector, Manual):** quien toma asistencia cambia de modo sin salir, y el modo inicial se elige según la reunión y el dispositivo.
- **Lectores de códigos sin integración especial:** los lectores USB, Bluetooth o de gatillo actúan como un teclado, así que basta un campo enfocado que reciba el código y el Enter. Funcionan en cualquier computadora sin instalar nada.
- **Celular primero, lector 2D opcional:** la cámara del celular no cuesta nada y permite varias entradas a la vez; el lector 2D se recomienda solo para la entrada con más fila. Los lectores 1D (láser o «infrarrojos» de códigos de barras) no sirven porque no leen QR.
- **Varios equipos a la vez en la misma reunión:** cada marca es independiente y la clave primaria de `asistencia` evita duplicados aunque dos equipos lean a la misma persona. El contador de cada pantalla se actualiza con el total que devuelve cada marca.
- **Un QR repetido no es un error:** responde «Ya estaba registrado» para que la fila avance sin alarma.
- **Reuniones creadas al tomar asistencia:** evita tareas programadas y reuniones vacías de días en que no hubo culto. Los días sin reunión simplemente no aparecen en el historial.
- **El nombre se copia en la reunión:** el historial muestra el nombre que tenía el culto, el grupo o el evento ese día aunque luego se renombre.
- **Visitantes fuera:** en esta spec solo se toma asistencia de miembros registrados.
- **Exige conexión:** más simple y sin riesgo de duplicados ni conflictos. Se avisa claramente cuando se cae y se puede marcar a mano después.
- **QR generado en el navegador y no en el servidor:** el backend solo guarda y valida el código; la imagen se arma donde se necesita (vista previa, descarga e impresión).
- **Permiso propio para el líder (`asistencia.grupo`):** el líder marca su grupo sin poder marcar cultos ni ver la asistencia de toda la iglesia.

## Riesgos

- **La cámara del navegador exige HTTPS** (salvo en `localhost`). Para probar con un celular en desarrollo hay que servir el frontend por HTTPS en la red local (`ng serve --ssl`) o usar un túnel. Se documenta en el paso de verificación.
- **Celulares lentos o con poca luz:** se mitiga con la decodificación en un web worker, el `BarcodeDetector` nativo cuando existe, la linterna y el modo Lector como alternativa.
- **Fotochecks impresos de mala calidad:** QR de al menos 25 mm con margen blanco y el código impreso debajo para escribirlo a mano.
- **Conexión saturada en un culto grande:** cada marca es una petición pequeña y no se recarga la lista. Si se cae, se avisa y se sigue a mano.
- **Ruido en las consultas de porcentaje:** el porcentaje de un miembro depende de qué reuniones cuentan (cultos activos y su grupo); se documenta en la ficha con un texto de ayuda.
- **Peso de las librerías nuevas:** `qr-scanner` y `qrcode` se cargan solo en las pantallas que las usan.

## Qué **no** entra en esta spec

- Visitantes no registrados y registro rápido desde la asistencia.
- Funcionamiento sin conexión.
- QR en el portal del miembro, billeteras del celular o QR dinámico.
- Autoregistro del miembro escaneando un QR de la reunión.
- Alertas por inasistencia, seguimiento y reportes de tardanza.
- Exportación a Excel o PDF y reportes entre iglesias.
