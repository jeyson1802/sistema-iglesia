# SPEC 07 — Aportes de los miembros

> **Estado:** Implementado
> **Depende de:** SPEC 02 (permisos y roles), SPEC 03 (OpenAPI y trazabilidad), SPEC 05 (acceso global del administrador del sistema), SPEC 06 (registro de miembros)
> **Fecha:** 2026-10-29
> **Objetivo:** Que cada iglesia registre, consulte y corrija los aportes de sus miembros (diezmos, ofrendas y las categorías que ella misma defina) en la moneda que configure, con su monto, medio de pago, petición y observación; que vea los totales por categoría y por medio en cualquier rango de fechas; y que cada líder consulte sus propios aportes.

## Por qué existe esta spec

El módulo 3 de la hoja de ruta («Ofrendas y diezmos») no existe: en `iglesia-api` el paquete `ofrendas` está vacío y no hay tablas. Hoy las iglesias anotan los aportes en cuadernos o en Excel. Así no saben cuánto entró en un mes por cada concepto ni por cada medio (efectivo, Yape, transferencia…), y no pueden revisar el historial de aportes de una persona.

Las iglesias no llaman igual a sus conceptos: casi todas tienen **diezmo** y **ofrenda**, pero muchas suman «primicias», «pro templo», «misiones» o «ayuda social». Por eso las **categorías de aporte son configurables por iglesia**, como los niveles. Diezmo y ofrenda se crean por defecto y se pueden renombrar, ordenar, desactivar o completar con otras. Por la misma razón, cada iglesia elige su **moneda**: el sistema da servicio a iglesias de distintos países.

Se llama **aporte** y no «ofrenda» porque la ofrenda es solo una de las categorías. El paquete vacío `ofrendas` se reemplaza por `aportes`.

## Alcance

**Entra:**

- **Configuración de aportes (`/aportes/configuracion`):** una sola pantalla con dos secciones.
  - **Moneda de la iglesia:** se elige de una lista de monedas admitidas (por defecto, sol peruano `PEN`). Mientras la iglesia no tenga aportes vigentes se puede cambiar libremente; después, no (ver «Reglas de la configuración»).
  - **Categorías de aporte:** crear, renombrar, ordenar, desactivar y reactivar, con la misma experiencia que `/niveles`. Toda iglesia tiene **Diezmo** y **Ofrenda** desde el inicio: las existentes por la migración y las nuevas al darse de alta. Cada categoría indica si **exige identificar al aportante** (p. ej. el diezmo) o si admite aportes **anónimos** (p. ej. la ofrenda del culto). Una categoría con aportes no se borra: se desactiva.
- **Registro de aportes (`/aportes`):** listado paginado con filtros, alta, edición y anulación.
  - **Datos:** fecha del aporte, miembro aportante (opcional si la categoría admite anónimos), categoría, monto, medio, número de operación, petición y observación.
  - **Medios** (lista fija): efectivo, transferencia, Yape, Plin, cheque y otro.
  - **Filtros:** rango de fechas con atajos (hoy, esta semana, este mes, mes anterior, este año; por defecto, este mes), categoría, medio, miembro y si se incluyen los anulados.
  - **Totales** del rango filtrado: total general, cantidad de aportes, desglose por categoría y por medio, y comparación con el período anterior de la misma duración. Los anulados no cuentan.
- **Ámbito de lectura:** el administrador de la iglesia (y el administrador del sistema dentro de una iglesia) ve todos los aportes. Un **líder solo ve sus propios aportes** (los que tienen como aportante al miembro vinculado a su usuario), en solo lectura, con sus totales.
- **Aportes en la ficha del miembro:** sección «Aportes» con el total del año, los últimos aportes y un enlace al listado filtrado por esa persona. Se muestra a quien puede verlos según el ámbito (el administrador en todas las fichas, el líder solo en la suya).
- **Edición con auditoría y anulación en lugar de borrado:** un aporte se puede corregir, y queda registrado quién y cuándo lo modificó por última vez. Un aporte mal registrado se anula con un motivo obligatorio y queda en el historial, tachado y fuera de los totales. Los aportes nunca se borran.
- **Auditoría:** cada aporte guarda quién lo registró y cuándo (del token) y quién lo modificó o anuló por última vez y cuándo.
- **Permisos nuevos** en `iglesia-seguridad`: `aportes.ver`, `aportes.registrar`, `aportes.editar`, `aportes.anular` y `aportes.configurar`. `ADMIN_IGLESIA` recibe los cinco y `LIDER` recibe `aportes.ver` (limitado a los suyos por el ámbito). Se pueden repartir en roles personalizados (p. ej. un rol «Tesorería»).
- **Experiencia de uso cuidada** (ver «Diseño de la experiencia»): registro rápido de varios sobres seguidos, resumen visual con gráficos, vista en tarjetas en el móvil, filtros en la URL y estados de carga, vacío y error en cada pantalla.
- **Documentación y cierre:** OpenAPI, Postman, pruebas y diagrama, como en las specs anteriores.

**Fuera de alcance (para specs futuras):**

- Recibos o constancias imprimibles, y su numeración correlativa.
- Exportar a Excel o PDF y reportes avanzados (comparativos de varios años, proyecciones).
- Varias monedas a la vez en una misma iglesia, y conversión entre monedas.
- Cierre o arqueo de caja por culto, y vincular la ofrenda con una reunión del módulo de asistencia.
- Egresos o gastos de la iglesia (tesorería completa).
- Que un líder vea los aportes de su red. El portal del miembro (módulo 6) reutilizará el mismo ámbito para que cada miembro vea los suyos.
- Promesas o compromisos de aporte, y metas por campaña.
- Medios de pago configurables por iglesia.
- Historial completo de cambios de un aporte (se guarda solo la última modificación).
- Registro masivo de sobres en una tabla editable.

## Modelo de datos

### Migración `iglesia-api/src/main/resources/db/migration/V7__aportes.sql`

Sigue la convención de V1 y V5: `iglesia_id NOT NULL`, `UNIQUE (iglesia_id, id)` como destino de FK compuestas, índices que empiezan por `iglesia_id` y `habilitar_aislamiento_por_iglesia(...)`.

```sql
-- Moneda de la iglesia para los aportes. Si no hay fila, la API devuelve PEN.
CREATE TABLE configuracion_aportes (
    iglesia_id BIGINT  PRIMARY KEY REFERENCES iglesia (id),
    moneda     CHAR(3) NOT NULL CHECK (moneda ~ '^[A-Z]{3}$')
);

CREATE TABLE categoria_aporte (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id      BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre          VARCHAR(60) NOT NULL,
    exige_aportante BOOLEAN     NOT NULL DEFAULT FALSE,
    orden           INTEGER     NOT NULL,
    activo          BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_categoria_aporte_nombre ON categoria_aporte (iglesia_id, lower(nombre));

CREATE TABLE aporte (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id       BIGINT        NOT NULL REFERENCES iglesia (id),
    fecha            DATE          NOT NULL,
    miembro_id       BIGINT,                          -- NULL = aporte anónimo
    categoria_id     BIGINT        NOT NULL,
    monto            NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    medio            VARCHAR(15)   NOT NULL CHECK (medio IN ('EFECTIVO', 'TRANSFERENCIA', 'YAPE', 'PLIN', 'CHEQUE', 'OTRO')),
    numero_operacion VARCHAR(40),
    peticion         VARCHAR(1000),
    observacion      VARCHAR(500),
    anulado          BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(300),
    registrado_por   BIGINT        NOT NULL,           -- usuarioId del token (iglesia-seguridad)
    registrado_en    TIMESTAMPTZ   NOT NULL,
    modificado_por   BIGINT,
    modificado_en    TIMESTAMPTZ,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, miembro_id)   REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, categoria_id) REFERENCES categoria_aporte (iglesia_id, id),
    CHECK (anulado = (motivo_anulacion IS NOT NULL))
);
CREATE INDEX ix_aporte_fecha     ON aporte (iglesia_id, fecha DESC);
CREATE INDEX ix_aporte_miembro   ON aporte (iglesia_id, miembro_id, fecha DESC);
CREATE INDEX ix_aporte_categoria ON aporte (iglesia_id, categoria_id, fecha DESC);

SELECT habilitar_aislamiento_por_iglesia('configuracion_aportes');
SELECT habilitar_aislamiento_por_iglesia('categoria_aporte');
SELECT habilitar_aislamiento_por_iglesia('aporte');

-- Diezmo y Ofrenda para las iglesias que ya existen. Las nuevas los reciben al darse de alta (ver Backend).
INSERT INTO categoria_aporte (iglesia_id, nombre, exige_aportante, orden)
SELECT i.id, c.nombre, c.exige_aportante, c.orden
FROM iglesia i
CROSS JOIN (VALUES ('Diezmo', TRUE, 1), ('Ofrenda', FALSE, 2)) AS c (nombre, exige_aportante, orden);
```

- `registrado_por` y `modificado_por` guardan el `usuarioId` del token, no un miembro: quien registra es un usuario del sistema, y la tabla de usuarios vive en otra base (no hay FK).
- La moneda vive en su propia tabla del contexto `aportes`, y no en `configuracion_iglesia` (que pertenece a `grupos`), para que cada contexto sea dueño de su configuración.
- Los montos no guardan la moneda: la moneda es una sola por iglesia y no puede cambiar cuando ya hay aportes vigentes.

### Migración `iglesia-seguridad/src/main/resources/db/migration/V6__permisos_de_aportes.sql`

```sql
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('aportes.ver',       'Aportes', 'Ver aportes y totales (el líder, solo los suyos)', 1),
    ('aportes.registrar', 'Aportes', 'Registrar aportes',                                 2),
    ('aportes.editar',    'Aportes', 'Corregir aportes registrados',                      3),
    ('aportes.anular',    'Aportes', 'Anular aportes con un motivo',                      4),
    ('aportes.configurar','Aportes', 'Elegir la moneda y administrar las categorías',     5);

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r JOIN permiso p ON p.modulo = 'Aportes'
WHERE r.codigo = 'ADMIN_IGLESIA';
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'aportes.ver' FROM rol r WHERE r.codigo = 'LIDER';
```

### Tipos de dominio (`iglesia-api`, `com.iglesia.api.aportes`)

```java
// model
public enum MedioAporte { EFECTIVO, TRANSFERENCIA, YAPE, PLIN, CHEQUE, OTRO }
// constant: monedas admitidas (ISO 4217), validadas además con java.util.Currency
public final class MonedasAdmitidas {
    public static final String POR_DEFECTO = "PEN";
    public static final Set<String> CODIGOS = Set.of("PEN", "USD", "EUR", "MXN", "COP", "CLP", "ARS", "BOB",
            "BRL", "UYU", "PYG", "GTQ", "HNL", "NIO", "CRC", "PAB", "DOP", "VES");
}
```

### Reglas del aporte

| Dato | Regla |
| --- | --- |
| `fecha` | Obligatoria, no futura. El formulario propone la fecha de hoy. |
| `miembroId` | Miembro de la misma iglesia. Obligatorio si la categoría exige aportante (409 junto al campo). Si falta, el aporte es **anónimo**. Debe estar activo al registrar, pero un aporte ya registrado conserva a su miembro aunque después se desactive. |
| `categoriaId` | Obligatoria, de la misma iglesia y activa al registrar. Al editar se conserva la categoría asignada aunque esté inactiva. |
| `monto` | Obligatorio, mayor que 0, hasta 10 enteros y 2 decimales (`@Positive`, `@Digits(integer = 10, fraction = 2)`). `BigDecimal` en Java; nunca `double`. Se expresa en la moneda de la iglesia. |
| `medio` | Obligatorio, uno de `MedioAporte`. |
| `numeroOperacion` | Opcional, máx. 40. Sirve para cuadrar transferencias, Yape, Plin y cheques. Con `EFECTIVO` se guarda `null`. |
| `peticion` | Opcional, texto libre, máx. 1000 (motivo de oración que acompaña el aporte). |
| `observacion` | Opcional, máx. 500 (nota interna de quien registra; con `OTRO`, sirve para indicar el medio). |
| Edición | Con `aportes.editar`, se pueden cambiar todos los datos con las mismas reglas del alta. Guarda `modificado_por` y `modificado_en`. Un aporte anulado no se edita (409). |
| Anulación | Con `aportes.anular`, `motivo` obligatorio (máx. 300). Un aporte anulado no se vuelve a anular (409). No hay «desanular»: si fue un error, se registra de nuevo. |

### Reglas de la configuración

| Dato | Regla |
| --- | --- |
| `moneda` | Código ISO 4217 de `MonedasAdmitidas` (otro → 400). Por defecto `PEN`. Se puede cambiar mientras la iglesia no tenga aportes no anulados; si los tiene, 409 «Ya hay aportes registrados en <moneda>: no se puede cambiar la moneda». |
| `nombre` de categoría | Obligatorio, máx. 60, único en la iglesia sin distinguir mayúsculas (409). |
| `exigeAportante` | Indica si los aportes de esta categoría deben tener miembro. Cambiarlo no afecta a los aportes ya registrados. |
| `orden` | Orden de la lista en el formulario y en los totales; se cambia con «subir/bajar», como los niveles. |
| Desactivar categoría | Siempre permitido: deja de ofrecerse para aportes nuevos y sus aportes siguen contando en los totales. |

### Ámbito de lectura (`iglesia-api`)

`AmbitoAportes` se calcula desde el token, nunca desde parámetros, igual que `AmbitoMiembros`:

| Token | Ve |
| --- | --- |
| `roles` incluye `ADMIN_IGLESIA`, o el administrador del sistema dentro de una iglesia (SPEC 05) | Todos los aportes de la iglesia. |
| Cualquier otro rol con `aportes.ver` y `miembro_id` en el token (p. ej. `LIDER`) | Solo los aportes cuyo `miembro_id` es el suyo. El filtro `miembroId` se ignora, y pedir un aporte ajeno responde 404. |
| `aportes.ver` sin `miembro_id` | Nada: listado vacío, totales en cero y 404 en el detalle. |

El ámbito se aplica al listado, al detalle y al resumen. Registrar, editar, anular y configurar siguen exigiendo sus permisos, que `LIDER` no tiene. Si un administrador también es líder, ve todo (el ámbito es la unión de roles, como en la SPEC 06).

### Backend `iglesia-api`

| Contexto | Piezas |
| --- | --- |
| `aportes` (reemplaza el paquete vacío `ofrendas`) | Entidades `ConfiguracionAportes`, `CategoriaAporte` y `Aporte`, sus repository, `ConfiguracionAportesService`, `CategoriaAporteService`, `AporteService`, `ResumenAportesService` (totales con `GROUP BY` en JPQL), `AmbitoAportes`, controladores, DTO y mappers. `EspecificacionesAporte` para el listado (Specification). |
| `aportes.component` | `CategoriasPorDefecto`: crea Diezmo y Ofrenda al recibir el evento de dominio `IglesiaCreada`. |
| `iglesia` | `IglesiaService.crear` publica `IglesiaCreada` (Observer / eventos de dominio de Spring), así `iglesia` no depende de `aportes`. |
| `miembros` | Se usa `ReferenciasMiembro` para validar al aportante y obtener su nombre. |
| `common/seguridad` | Constantes nuevas en `Permisos`. `UsuarioActual.id()` da `registrado_por` y `modificado_por`; `miembroId()` y `rolesBase()`, el ámbito. |

Logs: `INFO «Aporte registrado id={} por usuarioId={}»`, y lo mismo al editar, anular o cambiar la moneda. **Nunca** se registran montos, nombres, peticiones ni observaciones: los aportes son datos sensibles.

### Endpoints de `iglesia-api`

| Endpoint | Permiso | Respuestas |
| --- | --- | --- |
| `GET /api/aportes/configuracion` | `aportes.ver`, `aportes.registrar` o `aportes.configurar` | 200 `{ moneda, monedaEditable }` (`PEN` si no se configuró; `monedaEditable` es `false` si ya hay aportes vigentes). |
| `PUT /api/aportes/configuracion` | `aportes.configurar` | 200 · 400 (moneda no admitida) · 409 (ya hay aportes). |
| `GET /api/aportes/monedas` | `aportes.configurar` | 200: monedas admitidas con código, nombre en español y símbolo (de `java.util.Currency`). |
| `GET /api/categorias-aporte` | `aportes.ver`, `aportes.registrar` o `aportes.configurar` | 200: todas, activas e inactivas, por `orden`. |
| `POST /api/categorias-aporte` | `aportes.configurar` | 201 · 400 · 409 (nombre repetido). Se agrega al final del orden. |
| `PUT /api/categorias-aporte/{id}` | `aportes.configurar` | 200 · 400 · 404 · 409. Cambia nombre y `exigeAportante`. |
| `PUT /api/categorias-aporte/orden` | `aportes.configurar` | 200: recibe la lista completa de ids en el nuevo orden · 400 si falta o sobra alguno. |
| `PATCH /api/categorias-aporte/{id}/estado` | `aportes.configurar` | 200 · 404. |
| `GET /api/aportes?desde&hasta&categoriaId&medio&miembroId&incluirAnulados&page&size&sort` | `aportes.ver` (filtrado por ámbito) | 200 `Page<AporteResponse>` ordenado por fecha y registro descendentes. `desde` y `hasta` son obligatorios, con `desde ≤ hasta` y un rango máximo de 366 días (400). |
| `GET /api/aportes/resumen?desde&hasta&categoriaId&medio&miembroId` | `aportes.ver` (filtrado por ámbito) | 200 `{ moneda, total, cantidad, totalPeriodoAnterior, porCategoria: [{ categoria, total, cantidad }], porMedio: [{ medio, total, cantidad }] }`, sin anulados. El período anterior es el de la misma duración inmediatamente antes de `desde`. |
| `GET /api/aportes/{id}` | `aportes.ver` (filtrado por ámbito) | 200 · 404. |
| `POST /api/aportes` | `aportes.registrar` | 201 · 400 · 409 (categoría o miembro inactivos, categoría que exige aportante sin miembro). |
| `PUT /api/aportes/{id}` | `aportes.editar` | 200 · 400 · 404 · 409 (anulado). |
| `PATCH /api/aportes/{id}/anulacion` | `aportes.anular` | 200 · 400 (sin motivo) · 404 · 409 (ya anulado). |

`AporteResponse`: `id`, `fecha`, `miembro` (`ReferenciaResponse` o `null` si es anónimo), `categoria` (`ReferenciaResponse`), `monto`, `medio`, `numeroOperacion`, `peticion`, `observacion`, `anulado`, `motivoAnulacion`, `registradoEn`, `modificadoEn`. Los montos viajan como número decimal en JSON (`BigDecimal`).

### Frontend (`iglesia-web/src/app/`)

| Pieza | Qué hace |
| --- | --- |
| `features/aportes/services/aportes.service.ts` | Fachada HTTP: listar, resumen, obtener, crear, actualizar, anular. |
| `features/aportes/services/configuracion-aportes.service.ts` | Moneda de la iglesia como signal (se carga una vez), monedas admitidas, guardar. |
| `features/aportes/services/categorias-aporte.service.ts` | Listar, crear, actualizar, ordenar y cambiar estado. |
| `features/aportes/pages/configuracion-aportes` | Contenedor de `/aportes/configuracion`: sección de moneda y sección de categorías. |
| `features/aportes/components/selector-moneda` | `p-select` con filtro, «S/ · Sol peruano (PEN)». Bloqueado con explicación si `monedaEditable` es `false`. |
| `features/aportes/components/tabla-categorias` y `formulario-categoria` | Tabla ordenable (subir/bajar), etiqueta «Exige aportante» o «Admite anónimos», y diálogo con nombre e interruptor (`p-toggleswitch`). Mismo diseño que `features/niveles`. |
| `features/aportes/components/filtros-aportes` | Atajos de período en `p-selectbutton` (Hoy · Semana · Mes · Mes anterior · Año · Personalizado), `p-datepicker` de rango para el personalizado, categoría, medio, miembro (`app-selector-miembro`, oculto para el líder) e «Incluir anulados». |
| `features/aportes/components/resumen-aportes` | Tarjetas al estilo de `statswidget` del dashboard de Sakai: total del período con la variación frente al anterior (flecha y %), cantidad de aportes y promedio. Debajo, un gráfico de anillo por categoría y uno de barras por medio (`p-chart`, colores de los tokens del tema). |
| `features/aportes/components/tabla-aportes` | Escritorio: tabla paginada en el servidor con fecha, aportante («Anónimo» en cursiva si no hay), categoría (`p-tag`), medio con icono, monto alineado a la derecha y acciones (editar, anular) en un menú por fila. Móvil: la misma información en tarjetas apiladas. Los anulados aparecen tachados con la etiqueta «Anulado» y el motivo en un tooltip. Un clic en la fila abre el detalle. |
| `features/aportes/components/detalle-aporte` | Panel lateral (`p-drawer`) con todos los datos, la petición destacada y la auditoría («Registrado el … · Modificado el …»). |
| `features/aportes/components/formulario-aporte` | Diálogo de alta y edición (ver «Diseño de la experiencia»). |
| `features/aportes/components/anular-aporte` | Diálogo con el resumen del aporte (fecha, aportante, monto) y el motivo obligatorio. |
| `features/aportes/pages/aportes-lista` | Contenedor: filtros, resumen, tabla y diálogos. Acciones según permisos; para el líder, título «Mis aportes» y solo lectura. |
| `features/miembros/components/aportes-miembro` | Sección de la ficha: total del año, últimos 5 aportes y «Ver todos» (enlace a `/aportes?miembroId=`). Se muestra solo si el ámbito lo permite. |
| `shared/pipes/moneda.pipe.ts` | `monto \| moneda: codigo` con `Intl.NumberFormat('es', { style: 'currency' })`: «S/ 1,250.00», «US$ 80.00». Se usa en todas las pantallas. |
| Menú y rutas | Iglesia › Aportes (`aportes.ver`, `pi-wallet`; «Mis aportes» para el líder); Administración › Configuración de aportes (`aportes.configurar`, `pi-cog`). Rutas con `iglesiaFijadaGuard` y `permisoGuard`. |

## Diseño de la experiencia

El registro de aportes se hace casi siempre al final de un culto, con una pila de sobres, a veces desde el celular. Por eso la prioridad es que **registrar sea rápido y sin errores**, y que **entender los números sea inmediato**.

**Registro rápido**

- La acción principal «Registrar aporte» está siempre visible arriba a la derecha (en el móvil, un botón flotante abajo).
- Al abrir el formulario, el foco va al **monto**, que es grande, con el símbolo de la moneda y teclado numérico en el móvil (`inputmode="decimal"`).
- La **categoría** y el **medio** se eligen con botones grandes de un toque (`p-selectbutton`, el medio con icono), no con listas desplegables.
- La fecha propone **hoy**; el aportante se busca escribiendo el nombre (`app-selector-miembro`).
- Si la categoría admite anónimos, el aportante muestra la ayuda «Déjalo vacío si es anónimo». Si lo exige, el campo pasa a obligatorio y lo dice junto a la etiqueta.
- El **número de operación** aparece solo si el medio no es efectivo, con el texto de ayuda adecuado (p. ej. «Código de operación de Yape»).
- Petición y observación quedan plegadas en «Más datos (opcional)», para no alargar el formulario.
- **«Guardar y registrar otro»** guarda, muestra un toast breve («S/ 50.00 de Juan Pérez registrado») y deja el formulario listo con la misma fecha, categoría y medio, el foco en el monto y un contador de sobres registrados en la sesión. `Ctrl+Enter` (`⌘+Enter` en Mac) guarda y sigue.
- La última categoría y el último medio usados se recuerdan en el navegador (comodidad por usuario; si no se pueden leer, se usan Diezmo y Efectivo).

**Entender los números**

- Arriba, el resumen del período: el total en grande, la variación frente al período anterior (verde si sube, rojo si baja, siempre con flecha y texto, no solo con color), la cantidad de aportes y el promedio.
- Gráficos de anillo por categoría y de barras por medio, con leyenda y los montos al pasar el mouse. En pantallas pequeñas se apilan.
- Al hacer clic en una categoría o en un medio del gráfico, se filtra el listado por ella.

**Navegación y filtros**

- Los filtros viven en la URL (`/aportes?desde=…&categoriaId=…`): se pueden compartir, el botón «Atrás» los respeta y el enlace de la ficha del miembro llega ya filtrado.
- Los filtros activos se muestran como chips con «×» para quitarlos uno por uno, y hay un «Limpiar filtros».

**Retroalimentación y prevención de errores**

- Esqueletos de carga en el resumen y en la tabla; estado vacío con ilustración e invitación («Aún no hay aportes en este período · Registrar aporte»); error con «Reintentar».
- Validación junto a cada campo, en español y diciendo cómo corregir («Escribe un monto mayor a 0»).
- «Guardar» se deshabilita mientras se procesa; anular pide confirmación mostrando qué se anula y exige el motivo.
- Montos siempre con el formato de la moneda de la iglesia (`moneda.pipe`), alineados a la derecha y con números tabulares (`tabular-nums`) para que se lean en columna.

**Accesibilidad y tema**

- Todo se puede hacer con teclado: `label` en cada campo, orden de foco lógico, menús por fila accesibles y `aria-live` para los toasts y los totales que cambian.
- Modo claro y oscuro con los tokens `surface` y `primary` de Sakai; los gráficos toman sus colores del tema. Sin colores fijos ni CSS propio.

## Plan de implementación

**Backend `iglesia-seguridad`**

1. `V6__permisos_de_aportes.sql` y las constantes en `Permisos`. Pruebas: `ADMIN_IGLESIA` tiene los cinco permisos y `LIDER` tiene `aportes.ver`. Actualizar los conteos del catálogo en las pruebas.

**Backend `iglesia-api`**

2. `V7__aportes.sql`. Pruebas de integración: la RLS aísla `configuracion_aportes`, `categoria_aporte` y `aporte`; las FK compuestas impiden referenciar miembros o categorías de otra iglesia; toda iglesia existente tiene Diezmo y Ofrenda.
3. Contexto `aportes`, configuración: moneda (con su bloqueo cuando hay aportes), monedas admitidas, categorías (CRUD, orden y estado) y `CategoriasPorDefecto` con el evento `IglesiaCreada`. Pruebas unitarias y de integración, incluida «una iglesia nueva nace con Diezmo y Ofrenda». Eliminar el paquete `ofrendas`.
4. Aportes: alta, edición, detalle, listado con filtros y anulación, con sus reglas y auditoría. Pruebas unitarias y de integración (anónimo permitido o rechazado según la categoría, anulado no editable, montos con decimales, aislamiento entre iglesias).
5. `AmbitoAportes` aplicado al listado, al detalle y al resumen, y `GET /api/aportes/resumen` con totales agrupados en la base y el período anterior. Pruebas con tokens de `ADMIN_IGLESIA`, `LIDER` (solo los suyos, 404 en ajenos) y del administrador del sistema dentro de una iglesia; anulados que no cuentan.
6. OpenAPI completo y Postman: carpetas «6. Configuración de aportes» y «7. Aportes». Ejecutar con newman.

**Frontend `iglesia-web`**

7. `shared/pipes/moneda.pipe.ts`, servicios de configuración y categorías, y la página `/aportes/configuracion` (moneda y categorías). Specs.
8. `features/aportes`, listado: modelos, servicio, filtros en la URL, resumen con gráficos, tabla con vista móvil en tarjetas y detalle lateral. Specs.
9. `features/aportes`, registro: formulario con registro rápido, edición y anulación. Specs.
10. `aportes-miembro` en la ficha del miembro, menú («Aportes» / «Mis aportes», «Configuración de aportes») y rutas. Specs de menú y rutas.

**Cierre**

11. Crear `diagramas/spec-07-aportes-de-miembros.drawio` con varias páginas, colores con leyenda y notas:
    - arquitectura (componentes, servicios, endpoints, clases y tablas);
    - registro de un aporte con sus validaciones;
    - configuración: moneda, categorías y categorías por defecto con el evento `IglesiaCreada`;
    - ámbito (administrador y líder), anulación y cálculo de totales.

## Criterios de aceptación

**Backend `iglesia-seguridad`**

- [ ] Existen los cinco permisos de aportes; `ADMIN_IGLESIA` los tiene todos y `LIDER` solo `aportes.ver`.

**Backend `iglesia-api`**

- [ ] Toda iglesia existente tiene Diezmo (exige aportante) y Ofrenda (admite anónimos), y una iglesia nueva los recibe al darse de alta.
- [ ] La moneda por defecto es `PEN`; se cambia a otra moneda admitida mientras no haya aportes vigentes, y con aportes responde 409. Una moneda no admitida responde 400.
- [ ] Las categorías se crean, renombran, ordenan, desactivan y reactivan; un nombre repetido (sin distinguir mayúsculas) responde 409.
- [ ] Un aporte se registra con fecha, categoría, monto y medio; miembro, número de operación, petición y observación son opcionales.
- [ ] Un aporte sin miembro en una categoría que exige aportante responde 409 con el error junto al campo `miembroId`; en una que admite anónimos, se registra como anónimo.
- [ ] Una fecha futura, un monto 0 o negativo, o con más de 2 decimales, responde 400.
- [ ] Una categoría o un miembro inactivos o de otra iglesia no se pueden asignar (409 / 404).
- [ ] Un aporte se edita con `aportes.editar` y guarda `modificado_por` / `modificado_en`; anulado, no se edita (409).
- [ ] Anular exige motivo; un aporte anulado no se vuelve a anular (409) y no cuenta en el resumen.
- [ ] El listado filtra por rango de fechas, categoría, medio, miembro y anulados; un rango invertido o mayor a 366 días responde 400.
- [ ] El resumen devuelve la moneda, el total, la cantidad, el total del período anterior y los desgloses por categoría y por medio, calculados en la base.
- [ ] Un `LIDER` ve solo sus propios aportes en el listado y el resumen, y un aporte ajeno le responde 404; no puede registrar, editar, anular ni configurar (403).
- [ ] Una iglesia no ve ni modifica aportes, categorías ni configuración de otra (Hibernate y RLS).
- [ ] Los logs no contienen montos, nombres, peticiones ni observaciones.
- [ ] OpenAPI y Postman completos; newman sin fallos.

**Frontend**

- [ ] `/aportes/configuracion` permite elegir la moneda (bloqueada con explicación si ya hay aportes) y administrar las categorías, marcando si exigen aportante.
- [ ] `/aportes` muestra por defecto los aportes del mes en curso con el resumen (total, variación frente al período anterior, cantidad, promedio y gráficos por categoría y por medio).
- [ ] Los atajos de período y los filtros actualizan la URL; abrir esa URL reproduce el mismo listado.
- [ ] El formulario pone el foco en el monto, formatea con la moneda de la iglesia, propone la fecha de hoy, oculta el número de operación para efectivo y exige el aportante cuando la categoría lo pide.
- [ ] «Guardar y registrar otro» (y `Ctrl/⌘+Enter`) deja el formulario listo con la misma fecha, categoría y medio y el foco en el monto.
- [ ] Anular pide confirmación con el motivo; el aporte queda tachado con «Anulado» y el resumen se actualiza.
- [ ] En el móvil la tabla se muestra como tarjetas y el botón de registrar es flotante.
- [ ] Un líder ve «Mis aportes» con sus aportes y totales, sin filtro de miembro ni acciones de escritura.
- [ ] La ficha del miembro muestra el total del año y los últimos aportes solo si el ámbito lo permite.
- [ ] Hay estados de carga, vacío y error en el resumen, la tabla y la configuración, y todo funciona con teclado y en modo oscuro.

## Decisiones

- **Sí:** «aporte» como nombre del módulo, porque diezmo y ofrenda son dos de sus categorías. **No:** mantener el paquete `ofrendas`.
- **Sí:** categorías configurables por iglesia, con Diezmo y Ofrenda creados por defecto como filas normales (se pueden renombrar o desactivar). **No:** categorías fijas en código, como exige CLAUDE.md.
- **Sí:** las categorías por defecto de las iglesias nuevas se crean escuchando el evento `IglesiaCreada`. **No:** que `IglesiaService` conozca el contexto `aportes`.
- **Sí:** aportes anónimos, permitidos o no según la categoría (`exigeAportante`), por decisión del usuario.
- **Sí:** una moneda por iglesia, configurable, por decisión del usuario; por defecto `PEN`. Solo se puede cambiar mientras no haya aportes vigentes, para que ningún monto antiguo cambie de significado. **No:** varias monedas a la vez ni conversión.
- **Sí:** medios de pago como lista fija (`MedioAporte`), por decisión del usuario. **No:** medios configurables por iglesia en esta spec.
- **Sí:** `NUMERIC(12,2)` en la base y `BigDecimal` en Java; el frontend no suma montos, los totales los da el backend.
- **Sí:** un aporte se puede editar, por decisión del usuario, guardando quién y cuándo lo modificó por última vez. Anular con motivo en lugar de borrar.
- **Sí:** quien registra y quien modifica se guardan como `usuarioId` del token (como dice CLAUDE.md para el registro), no como un miembro elegido a mano.
- **Sí:** el líder ve solo sus propios aportes, por decisión del usuario, con un ámbito calculado desde el token (`AmbitoAportes`). **No:** que vea los de su red. El mismo ámbito servirá al portal del miembro.
- **Sí:** los aportes son datos sensibles: permisos propios y ni montos ni nombres en los logs.
- **Sí:** el listado exige un rango de fechas (máx. 366 días), para que las consultas y los totales sigan siendo rápidos en iglesias con muchos años de datos.
- **Sí:** totales y período anterior calculados en la base con `GROUP BY`. **No:** sumar en memoria.
- **Sí:** experiencia de registro rápido (foco en el monto, botones de un toque, «Guardar y registrar otro», atajo de teclado) y resumen visual, por pedido del usuario de priorizar UX/UI. El tamaño del bundle no es una restricción para la UX/UI.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Al dar de alta una iglesia, el administrador del sistema no tiene esa iglesia en el contexto (tenant y RLS), y crear sus categorías por defecto choca con el aislamiento. | `CategoriasPorDefecto` corre en la misma transacción del alta con el tenant de la iglesia nueva (`SET LOCAL app.iglesia_id`) y una inserción explícita con `iglesia_id`. Lo cubre una prueba de integración. |
| Errores de redondeo con los montos. | `BigDecimal` de punta a punta, `NUMERIC(12,2)` y `@Digits`. En el frontend, `p-inputnumber` con 2 decimales y totales calculados por el backend. |
| Cambiar la moneda con aportes registrados cambiaría el significado de los montos antiguos. | Bloqueado (409) si hay aportes vigentes; la pantalla lo explica antes de intentarlo (`monedaEditable`). |
| Corregir un aporte cambia totales que ya se informaron. | Se guarda quién y cuándo lo modificó. El historial completo de cambios queda para una spec futura. |
| Que un miembro desactivado o una categoría desactivada rompan los aportes antiguos. | Solo se valida que estén activos al asignarlos; los aportes antiguos los conservan y se muestran atenuados. |
| Un líder sin miembro vinculado en el token no vería nada y no entendería por qué. | El ámbito devuelve vacío y la pantalla «Mis aportes» explica que su usuario no está vinculado a un miembro. |
| Los gráficos (Chart.js) aumentan el tamaño del bundle. | Por decisión del usuario, el tamaño del bundle no limita la UX/UI. Aun así, `p-chart` vive solo en la ruta diferida de aportes, sin costo para el resto de la app. |
| Los aportes son datos sensibles y podrían filtrarse en logs o a roles sin permiso. | Permisos propios, ámbito por token, logs con identificadores y pruebas con `OutputCaptureExtension`. |

## Qué **no** entra en esta spec

- Recibos imprimibles y numeración correlativa.
- Exportación a Excel o PDF, y reportes avanzados.
- Varias monedas en una misma iglesia y conversión.
- Cierre de caja por culto y vínculo con asistencia.
- Egresos y tesorería completa.
- Que el líder vea los aportes de su red; portal del miembro.
- Promesas, compromisos y metas de campaña.
- Medios de pago configurables.
- Historial completo de cambios de un aporte.
- Registro masivo en una tabla editable.

Cada uno, si llega, va en su propia spec.
