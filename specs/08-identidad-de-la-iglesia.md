# SPEC 08 — Identidad y apariencia de la iglesia

> **Estado:** Implementado
> **Depende de:** SPEC 02 (permisos y roles), SPEC 03 (OpenAPI y trazabilidad), SPEC 05 (acceso global del administrador del sistema), SPEC 06 (ubigeo y selector de ubigeo), SPEC 07 (regla de grillas paginadas)
> **Fecha:** 2026-10-29
> **Objetivo:** Que cada iglesia tenga su propia identidad en el sistema: denominación completa, nombre comercial con una tipografía cuidada, logo, dirección y datos de contacto, y una apariencia (preset, color primario y superficie) que define su administrador y que ven todos sus usuarios. El modo oscuro queda como preferencia de cada usuario, el catálogo de iglesias se puede filtrar y el menú muestra solo las opciones del sistema.

## Por qué existe esta spec

Hoy el sistema no tiene identidad propia por iglesia:

- La iglesia solo tiene un `nombre`. La topbar muestra siempre «IGLESIA-WEB» (`app-logo-iglesia`), aunque `CLAUDE.md` pide que muestre el logo y el nombre de la iglesia del usuario.
- El footer dice «IGLESIA-WEB by PrimeNG», que es un texto de la plantilla.
- El configurador de temas de Sakai (el botón de paleta de la topbar) lo puede usar **cualquier** usuario, y lo que elige no se guarda: al recargar la página se pierde. La apariencia debería ser una decisión de la iglesia, no de cada persona.
- El catálogo de iglesias del administrador del sistema no tiene filtros ni dirección. Cuando haya muchas iglesias no se podrán ubicar por región.
- El menú lateral sigue mostrando, en desarrollo, las opciones de la plantilla («UI Components», «Pages», «Hierarchy», «Get Started»).

## Alcance

**Entra:**

- **Datos de la iglesia:**
  - **Denominación completa** (p. ej. «Iglesia Evangélica Pentecostal Movimiento Misionero Mundial en el Perú»): se muestra en el footer y en los documentos formales.
  - **Nombre comercial** (p. ej. «MMM Lima Centro»): el nombre corto con el que se reconoce la iglesia. Va en la topbar, en el footer y en el título de la pestaña del navegador. Reemplaza al `nombre` actual y sigue siendo único sin distinguir mayúsculas.
  - **Dirección** y **ubicación** (distrito, con su provincia y departamento, con el `app-selector-ubigeo` de la SPEC 06).
  - **Teléfono**, **correo** y **sitio web** de contacto, opcionales.
- **Identidad visual:**
  - **Logo** de la iglesia: imagen PNG, JPEG o WebP de hasta 1 MB. Se muestra en la topbar junto al nombre comercial y como ícono de la pestaña (favicon). Sin logo se muestra un monograma con las iniciales del nombre comercial sobre el color primario.
  - **Tipografía del nombre comercial:** el administrador elige una entre cinco fuentes seleccionadas (Playfair Display, que es la de por defecto, Cinzel, Montserrat, Merriweather y Poppins). Se usa en la topbar y en el footer.
  - **Apariencia:** preset (Aura, Lara o Nora), color primario y superficie, los mismos que ofrece hoy el configurador de Sakai, más la tipografía del nombre comercial. Se guardan en la base y se aplican a **todos** los usuarios de la iglesia al iniciar sesión.
- **Quién configura:**
  - El **administrador del sistema** registra los datos de la iglesia en el alta y puede editar todo (datos, logo y apariencia) de cualquier iglesia desde el catálogo.
  - El **administrador de la iglesia** edita los datos, el logo y la apariencia de **su** iglesia en una nueva pantalla «Mi iglesia». La iglesia se toma del token, nunca de un parámetro del cliente.
  - Permiso nuevo en `iglesia-seguridad`: `iglesia.personalizar`, que recibe `ADMIN_IGLESIA`.
- **Modo oscuro por usuario:** el botón de modo claro u oscuro de la topbar sigue disponible para todos. La elección se recuerda en el navegador de cada persona y no se guarda en la base.
- **Configurador de temas fuera de la topbar:** desaparecen el botón de paleta de la topbar y el de la pantalla de acceso. La paleta solo existe dentro de «Mi iglesia» (y de la edición de la iglesia en el catálogo), con vista previa en vivo.
- **Footer con los datos de la iglesia:** nombre comercial (con su tipografía), denominación, dirección con distrito, provincia y departamento, teléfono, correo y sitio web (los que tenga), y el año.
- **Catálogo de iglesias con filtros:** búsqueda por nombre comercial o denominación, departamento, provincia o distrito, y estado (activa o inactiva). Paginado en el servidor según la regla de grillas de `CLAUDE.md`, con columnas de ubicación.
- **Menú solo con opciones del sistema:** se quitan del menú las entradas de la plantilla. Las páginas de demostración siguen en `src/app/pages/` como referencia de diseño, accesibles por URL solo en desarrollo.
- **Documentación y cierre:** OpenAPI, Postman, pruebas y diagrama, como en las specs anteriores.

**Fuera de alcance (para specs futuras):**

- Subir un logo distinto para el modo oscuro, o varias versiones del logo.
- Colores totalmente libres (selector hexadecimal) en lugar de las paletas de PrimeNG.
- Personalizar la pantalla de acceso con la identidad de una iglesia, que exigiría saber la iglesia antes de autenticarse (p. ej. por subdominio).
- Redes sociales, horarios de culto y página pública de la iglesia.
- Almacenamiento de archivos en un servicio externo (S3 o similar).

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V8__identidad_de_iglesia.sql`

`iglesia` es el catálogo de tenants: no tiene `iglesia_id` ni RLS. Los datos de identidad se agregan a esa tabla porque el administrador del sistema los lista y filtra para todas las iglesias.

```sql
-- El nombre actual pasa a ser el nombre comercial (corto, único y el que se muestra en la topbar).
ALTER TABLE iglesia RENAME COLUMN nombre TO nombre_comercial;
ALTER INDEX ux_iglesia_nombre RENAME TO ux_iglesia_nombre_comercial;

ALTER TABLE iglesia
    ADD COLUMN denominacion     VARCHAR(200),
    ADD COLUMN direccion        VARCHAR(200),
    ADD COLUMN distrito_codigo  CHAR(6) REFERENCES ubigeo_distrito (codigo),
    ADD COLUMN telefono         VARCHAR(20),
    ADD COLUMN correo           VARCHAR(120),
    ADD COLUMN sitio_web        VARCHAR(200),
    ADD COLUMN tema_preset      VARCHAR(10) NOT NULL DEFAULT 'Aura',
    ADD COLUMN color_primario   VARCHAR(20) NOT NULL DEFAULT 'emerald',
    ADD COLUMN color_superficie VARCHAR(20),
    ADD COLUMN tipografia       VARCHAR(30) NOT NULL DEFAULT 'Playfair Display';

-- Las iglesias existentes conservan su nombre también como denominación hasta que la completen.
UPDATE iglesia SET denominacion = nombre_comercial;
ALTER TABLE iglesia ALTER COLUMN denominacion SET NOT NULL;

CREATE INDEX ix_iglesia_distrito ON iglesia (distrito_codigo);

-- El logo va aparte para no cargar la imagen cada vez que se lee la iglesia.
CREATE TABLE logo_iglesia (
    iglesia_id     BIGINT      PRIMARY KEY REFERENCES iglesia (id),
    contenido      BYTEA       NOT NULL,
    tipo           VARCHAR(20) NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_logo_iglesia_tipo CHECK (tipo IN ('image/png', 'image/jpeg', 'image/webp'))
);
SELECT habilitar_aislamiento_por_iglesia('logo_iglesia');
```

- `direccion` y `distrito_codigo` admiten nulos solo por las iglesias que ya existen. La API los exige al crear y al editar, y la interfaz avisa «Completa la dirección de tu iglesia» mientras falten.
- `tipografia` guarda el nombre de una de las fuentes admitidas.
- `tema_preset`, `color_primario` y `color_superficie` guardan los mismos nombres que usa el configurador de Sakai (`emerald`, `slate`…). `color_superficie` nulo significa la superficie por defecto del preset.
- `logo_iglesia` sí tiene RLS: el administrador de la iglesia lo escribe dentro de su transacción con `app.iglesia_id`. El administrador del sistema lo escribe fijando la iglesia destino, igual que `CategoriasPorDefecto` en la SPEC 07.

### Migración `iglesia-seguridad/src/main/resources/db/migration/V7__permiso_personalizar_iglesia.sql`

Agrega el permiso `iglesia.personalizar` («Personalizar los datos, el logo y la apariencia de la iglesia») y se lo asigna a `ADMIN_IGLESIA`.

### Reglas

- **Nombre comercial:** obligatorio, de 2 a 60 caracteres (para que quepa en la topbar), normalizado (espacios) y único sin distinguir mayúsculas (409 en el campo `nombreComercial`).
- **Denominación:** obligatoria, hasta 200 caracteres.
- **Dirección y distrito:** obligatorios al crear y al editar. El distrito debe existir en el ubigeo (400).
- **Teléfono, correo y sitio web:** opcionales. El correo con `@Email`, el sitio web con `http://` o `https://` y el teléfono con dígitos, espacios, `+` y `-`.
- **Apariencia:** el preset debe ser `Aura`, `Lara` o `Nora`; el color primario, la superficie y la tipografía deben ser de las listas admitidas (constantes `PRESETS_ADMITIDOS`, `COLORES_PRIMARIOS`, `SUPERFICIES` y `TIPOGRAFIAS` en `iglesia/constant`). Un valor fuera de la lista responde 400.
- **Logo:**
  - El tipo se valida por los primeros bytes del archivo (firma de PNG, JPEG o WebP), no por la cabecera que envía el cliente. SVG no se admite porque puede contener código.
  - Tamaño máximo de 1 MB (`spring.servlet.multipart.max-file-size`); si se supera responde 413 con `ProblemDetail`.
  - Se recomienda una imagen cuadrada o apaisada de al menos 128 px de alto; la interfaz la muestra con altura fija sin deformarla.
  - Quitar el logo vuelve al monograma.
- **Logs:** alta, edición, cambio de logo y de apariencia se registran en `INFO` con `iglesiaId` y `usuarioId`, sin el contenido.

## Backend `iglesia-api`

- **`iglesia/`** (contexto existente):
  - `Iglesia` gana los campos nuevos. `GuardarIglesiaRequest` pasa a llevar `nombreComercial`, `denominacion`, `direccion`, `distritoCodigo`, `telefono`, `correo` y `sitioWeb`.
  - `IglesiaResponse` devuelve esos datos, la ubicación descrita (distrito, provincia y departamento, reutilizando lo de miembros), `tieneLogo` y la apariencia.
  - `AparienciaIglesia` (`model`, `record`) y `GuardarAparienciaRequest` (`dto`).
  - `IglesiaService` valida el distrito con el servicio de ubigeo y agrega `listar(filtro, pageable)` con `EspecificacionesIglesia` (texto en nombre comercial o denominación, prefijo de ubigeo de 2, 4 o 6 dígitos y estado).
  - `LogoIglesiaService`: guardar (con validación de la firma), obtener y quitar.
- **`MiIglesiaController`** (`/api/mi-iglesia`): siempre opera sobre la iglesia del token.
- La configuración de grupos (`/api/configuracion-iglesia`) no cambia.

### Endpoints de `iglesia-api`

| Método y ruta | Permiso | Descripción |
|---|---|---|
| `GET /api/iglesias?q=&ubigeo=&activa=&page=&size=&sort=` | `iglesias.ver` | Catálogo paginado y filtrado (antes devolvía la lista completa). |
| `GET /api/iglesias/{id}` | `iglesias.ver` | Datos completos de una iglesia. |
| `POST /api/iglesias` · `PUT /api/iglesias/{id}` | `iglesias.administrar` | Alta y edición con los datos nuevos. |
| `PUT /api/iglesias/{id}/apariencia` | `iglesias.administrar` | Preset, color primario, superficie y tipografía de una iglesia. |
| `PUT · DELETE /api/iglesias/{id}/logo` | `iglesias.administrar` | Subir (multipart) o quitar el logo de una iglesia. |
| `GET /api/mi-iglesia` | autenticado con iglesia | Identidad de la iglesia del token: datos, apariencia y `tieneLogo`. La usan la topbar, el footer y el tema de todos los usuarios. |
| `GET /api/mi-iglesia/logo` | autenticado con iglesia | Bytes del logo con `Content-Type`, `ETag` y `Cache-Control: private`. 404 si no tiene. |
| `PUT /api/mi-iglesia` | `iglesia.personalizar` | El administrador de la iglesia edita sus datos (no puede desactivarla). |
| `PUT /api/mi-iglesia/apariencia` | `iglesia.personalizar` | Guarda la apariencia de su iglesia. |
| `PUT · DELETE /api/mi-iglesia/logo` | `iglesia.personalizar` | Sube o quita el logo de su iglesia. |

- El administrador del sistema operando dentro de una iglesia (SPEC 05) usa `/api/mi-iglesia` como cualquier usuario de esa iglesia.
- Errores como `ProblemDetail` con `traceId`: 400 (validación, distrito o apariencia no admitidos, imagen no válida), 401, 403, 404, 409 (nombre comercial repetido) y 413 (logo demasiado grande).

## Frontend (`iglesia-web/src/app/`)

- **`core/iglesias/`:**
  - `IdentidadIglesiaService` (fachada con signals): carga `GET /api/mi-iglesia` y el logo (como `Blob` con `HttpClient`, para que viaje el token, convertido en URL de objeto) en cuanto hay una iglesia en la sesión, y los limpia al salir o cambiar de iglesia.
  - Expone `identidad`, `urlLogo` y `cargando`.
- **`core/apariencia/`:**
  - `AparienciaService`: aplica preset, color primario y superficie con `updatePreset` y `updateSurfacePalette` de `@primeuix/themes`. La lógica se extrae del configurador de Sakai, que hoy la tiene dentro del componente.
  - Al iniciar sesión aplica la apariencia de la iglesia. Sin iglesia (login, administrador del sistema sin iglesia fijada) aplica la del sistema (Aura, `emerald`).
  - `PreferenciaModoOscuro`: guarda la preferencia de modo oscuro en `localStorage` (con `try/catch`). Si no hay preferencia guardada, usa la del sistema operativo (`prefers-color-scheme`).
- **`shared/components/`:**
  - `logo-iglesia` pasa a mostrar el logo (o el monograma) y el nombre comercial con su tipografía. En el login sigue mostrando la identidad neutra del sistema.
  - Nuevos `selector-apariencia` (preset, colores primarios y superficies en muestras de color accesibles con teclado, como el configurador actual, y la tipografía con una tarjeta por fuente que muestra el nombre comercial escrito con ella) y `selector-logo` (`p-fileupload` en modo básico, vista previa, validación de tipo y tamaño antes de subir, botón «Quitar logo» con confirmación).
- **Layout** (cambios pedidos explícitamente en esta spec):
  - `app.topbar`: logo y nombre comercial de la iglesia; se quita el botón de paleta y `app-configurator`; queda el de modo oscuro.
  - `app.footer`: datos de la iglesia (ver «Diseño de la experiencia»); sin iglesia, «IGLESIA-WEB» y el año.
  - `app.floatingconfigurator` (pantalla de acceso): solo el botón de modo oscuro.
  - `app.menu.ts`: se quitan `ENTRADAS_DEMO`; se agrega «Administración › Mi iglesia» (`pi-building`, permiso `iglesia.personalizar`).
- **`features/mi-iglesia/`:** página `/mi-iglesia` (permiso `iglesia.personalizar`) con tres secciones: «Datos de la iglesia», «Logo» y «Apariencia». Cada una se guarda por separado. La apariencia se ve en vivo mientras se elige y, si se sale sin guardar, se vuelve a la guardada.
- **`features/iglesias/`:**
  - El catálogo pasa a grilla paginada en el servidor con filtros (texto con espera de 300 ms, `app-selector-ubigeo` parcial y estado) en la URL.
  - El alta pide nombre comercial, denominación, dirección, ubicación y contacto.
  - Nueva página `/iglesias/:id` para editar todo de una iglesia (datos, logo y apariencia), reutilizando los componentes de `mi-iglesia`.
- **Título de la pestaña:** «&lt;Página&gt; · &lt;Nombre comercial&gt;» con un `TitleStrategy` propio; sin iglesia, «&lt;Página&gt; · IGLESIA-WEB».
- **Tipografía:** las cinco fuentes se cargan desde Google Fonts (solo el peso que se usa, con `display=swap`). La elegida se aplica con una variable CSS (`--fuente-marca`) que el `AparienciaService` actualiza, expuesta como utilidad de Tailwind (`font-marca`), sin CSS propio en los componentes. El selector carga las cinco para la vista previa; el resto de la aplicación, solo la elegida.

## Diseño de la experiencia

- **Topbar:** logo de 40 px de alto (sin deformar) y el nombre comercial con la tipografía de marca, en una línea con puntos suspensivos si no cabe. En el móvil se ve solo el logo o el monograma, con el nombre como `aria-label`.
- **Footer:** en escritorio, dos columnas. A la izquierda, el nombre comercial con la tipografía de marca y debajo la denominación en texto pequeño. A la derecha, la dirección («Av. Los Próceres 123, San Juan de Lurigancho, Lima, Lima»), el teléfono y el correo como enlaces (`tel:` y `mailto:`) y el sitio web en una pestaña nueva. Al final, «© 2026 &lt;Nombre comercial&gt;». En el móvil, todo en una columna centrada. Lo que falta no se muestra (sin «—» ni huecos).
- **Mi iglesia:**
  - «Datos de la iglesia»: formulario en dos columnas con el contador de caracteres del nombre comercial y una vista previa de cómo se verá en la topbar.
  - «Logo»: zona para soltar o elegir la imagen, vista previa sobre fondo claro y oscuro, y el mensaje de error junto al campo («La imagen pesa 2.3 MB; elige una de hasta 1 MB»).
  - «Apariencia»: las mismas muestras de color que el configurador de Sakai, con el nombre de cada color como `aria-label`, y las tarjetas de tipografía con el nombre comercial de la iglesia escrito en cada fuente. Se aplica en vivo y hay botones «Guardar apariencia» y «Descartar cambios».
  - Aviso si falta la dirección.
- **Catálogo de iglesias:** filtros en una tarjeta, chips de filtros activos, columnas logo (miniatura o monograma), nombre comercial, denominación, ubicación y estado, y acción «Editar» que abre `/iglesias/:id`.
- **Carga sin parpadeo:** mientras llega la identidad, la topbar muestra un esqueleto del tamaño del logo y el nombre. El tema de la iglesia se guarda también en `localStorage` para aplicarlo al instante en la siguiente visita y luego confirmarlo con la respuesta del backend.

## Plan de implementación

1. **`iglesia-seguridad`:** migración `V7` con el permiso `iglesia.personalizar` para `ADMIN_IGLESIA`; constantes y pruebas de roles.
2. **`iglesia-api`, base de datos:** migración `V8`; entidad `Iglesia` actualizada, `LogoIglesia` y repositorios.
3. **`iglesia-api`, catálogo:** DTOs, validaciones, `EspecificacionesIglesia`, listado paginado con filtros, alta y edición con los datos nuevos; pruebas unitarias.
4. **`iglesia-api`, identidad:** `MiIglesiaController`, apariencia y logo (validación de firma, 413, `ETag`); pruebas unitarias y de integración; OpenAPI.
5. **Postman:** carpeta «Mi iglesia» y actualización de «Iglesias» (filtros, logo y apariencia).
6. **Frontend, núcleo:** `IdentidadIglesiaService`, `AparienciaService` y la preferencia de modo oscuro; `TitleStrategy`; tipografía.
7. **Frontend, layout:** topbar, footer, configurador flotante y menú (sin demos, con «Mi iglesia»); `logo-iglesia` actualizado.
8. **Frontend, componentes compartidos:** `selector-apariencia` y `selector-logo`.
9. **Frontend, `mi-iglesia`:** página con sus tres secciones.
10. **Frontend, catálogo de iglesias:** grilla paginada con filtros, alta ampliada y página `/iglesias/:id`.
11. **Pruebas, build y verificación en vivo:** `ng test`, `ng build`, `./gradlew test`, newman y prueba de humo.
12. Crear `diagramas/spec-08-identidad-de-la-iglesia.drawio` con varias páginas, colores con leyenda y notas: arquitectura; cómo se carga y aplica la identidad al iniciar sesión; edición y subida del logo (validaciones); catálogo con filtros.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Existe el permiso `iglesia.personalizar` y lo tiene `ADMIN_IGLESIA`.

**Backend `iglesia-api`**

- [ ] Las iglesias existentes conservan su nombre como nombre comercial y como denominación; una iglesia nueva exige nombre comercial, denominación, dirección y distrito.
- [ ] Un nombre comercial repetido (sin distinguir mayúsculas) responde 409; uno de más de 60 caracteres, 400.
- [ ] Un distrito inexistente, un correo o sitio web inválido, o un preset, color o tipografía no admitidos responden 400.
- [ ] `GET /api/iglesias` pagina y filtra por texto, ubigeo (2, 4 o 6 dígitos) y estado.
- [ ] `GET /api/mi-iglesia` devuelve la identidad de la iglesia del token a cualquier usuario autenticado de esa iglesia; sin iglesia en el token responde 403.
- [ ] El administrador de la iglesia edita datos, apariencia y logo de su iglesia; sin `iglesia.personalizar` responde 403. El administrador del sistema edita los de cualquier iglesia por su id.
- [ ] El logo acepta PNG, JPEG y WebP validados por su firma; un SVG o un archivo renombrado responde 400 y uno de más de 1 MB, 413. El logo se sirve con `ETag` y se puede quitar.
- [ ] Una iglesia no puede leer ni modificar el logo de otra (Hibernate y RLS).
- [ ] Los logs no contienen el contenido del logo ni datos de contacto.
- [ ] OpenAPI y Postman completos; newman sin fallos.

**Frontend**

- [ ] La topbar muestra el logo (o el monograma) y el nombre comercial de la iglesia con la tipografía de marca; el login muestra la identidad neutra del sistema.
- [ ] El footer muestra el nombre comercial, la denominación, la dirección con su ubicación y los datos de contacto que existan.
- [ ] El título de la pestaña incluye el nombre comercial y el favicon es el logo de la iglesia.
- [ ] Ningún usuario ve el configurador de temas en la topbar ni en el login; el modo oscuro lo cambia cualquiera y se recuerda en su navegador.
- [ ] La apariencia (preset, colores y tipografía) que guarda el administrador de la iglesia o el del sistema se aplica a todos sus usuarios al iniciar sesión, y se mantiene al recargar.
- [ ] Una iglesia existente sin dirección muestra el aviso «Completa la dirección de tu iglesia» en «Mi iglesia» y en el catálogo, sin bloquear el uso del sistema.
- [ ] `/mi-iglesia` permite editar los datos, el logo (con vista previa y validación) y la apariencia (con vista previa en vivo y descarte), solo con `iglesia.personalizar`.
- [ ] El catálogo de iglesias tiene filtros en la URL y grilla paginada en el servidor; el alta y `/iglesias/:id` piden los datos nuevos.
- [ ] El menú solo tiene opciones del sistema; las demos de la plantilla no aparecen en ningún entorno.
- [ ] Cada componente, servicio y pipe nuevo tiene su `.spec.ts`; `ng test` y `ng build` pasan.

## Decisiones

- **El `nombre` actual pasa a ser el nombre comercial** (se renombra la columna): es el que ya se usa para distinguir iglesias y ya tiene índice único. La denominación es nueva.
- **Logo en PostgreSQL (`BYTEA`), en una tabla aparte:** evita montar almacenamiento de archivos para imágenes pequeñas y queda aislado por RLS y en los respaldos de la base. Si más adelante se suben archivos grandes, se migra a un almacenamiento externo.
- **El logo se pide con `HttpClient` y no con una URL pública:** así viaja el token y una iglesia no puede ver el logo de otra adivinando ids.
- **Sin SVG:** puede contener scripts. PNG, JPEG y WebP cubren los logos habituales.
- **Apariencia en la tabla `iglesia`:** son tres valores de la propia iglesia que se leen siempre junto con su identidad.
- **Modo oscuro en el navegador y no en la base:** es una preferencia de cada persona, no de la iglesia, y no necesita sincronizarse entre dispositivos.
- **Tipografía elegida de una lista cerrada:** el administrador elige entre cinco fuentes probadas en la topbar y el footer, en lugar de cualquier fuente, para que siempre se lean bien y carguen rápido.
- **El administrador del sistema también personaliza:** puede editar datos, logo y apariencia de cualquier iglesia (p. ej. para dejarla lista al darla de alta).
- **Las iglesias existentes sin dirección solo reciben un aviso:** no se bloquea el uso del sistema hasta completarla.
- **Contacto en el footer:** teléfono, correo y sitio web, opcionales.
- **Paletas de PrimeNG y no colores libres:** garantizan contraste y coherencia en modo claro y oscuro sin diseñar una paleta completa por iglesia.
- **Las demos de Sakai siguen en el código** como referencia de diseño (`CLAUDE.md` pide revisarlas antes de crear pantallas), pero salen del menú.

## Riesgos

- **Cambio de contrato de `GET /api/iglesias`:** pasa de lista a página. El frontend del catálogo y la selección de iglesia del administrador del sistema se actualizan en la misma spec.
- **Renombrar `nombre` a `nombre_comercial`:** toca entidad, DTOs, pruebas y la colección de Postman; se hace en un solo paso de la migración.
- **Parpadeo del tema al iniciar sesión:** se mitiga aplicando la última apariencia conocida desde `localStorage` y confirmándola con el backend.
- **Peso de las fuentes:** cinco fuentes son más descargas; se mitiga cargando un solo peso por fuente y, fuera del selector, solo la elegida.
- **Imágenes muy grandes en memoria:** el límite de 1 MB se aplica en el servidor (multipart) y en el navegador antes de subir.

## Qué **no** entra en esta spec

- Recursos públicos sin autenticación (página pública de la iglesia, logo en el login).
- Varias sedes o anexos de una misma iglesia.
- Plantillas de documentos imprimibles con la identidad de la iglesia (llegarán con los recibos de aportes).
