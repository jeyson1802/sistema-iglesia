# SPEC 11 — Páginas de inicio por perfil

> **Estado:** Implementado
> **Depende de:** SPEC 02 (permisos y perfiles), SPEC 03 (OpenAPI y trazabilidad), SPEC 05 (acceso global del administrador del sistema), SPEC 06 (miembros, grupos y jerarquía), SPEC 07 (aportes), SPEC 09 (asistencia y seguimiento), SPEC 10 (consolidación)
> **Fecha:** 2026-10-30
> **Objetivo:** Que cada persona, al entrar, vea en su página de inicio lo que le toca según su perfil, con estadísticas claras y atajos a lo que tiene que hacer hoy:
> - el **administrador del sistema** ve, solo en cifras, cómo crecen las iglesias, cuántos miembros hay en total y cuánto se usa el sistema;
> - el **administrador de iglesia** ve las estadísticas de todos los módulos de su iglesia;
> - el **líder** ve solo lo de su red;
> - el **miembro** ve solo sus propios datos.
>
> **Ningún** inicio muestra datos de aportes, salvo a quien tiene el permiso `aportes.ver`.

## Por qué existe esta spec

- Hoy los cuatro inicios (`/sistema`, `/administracion`, `/mi-red`, `/mi-perfil`) solo muestran una bienvenida y el aviso «Estamos preparando esta sección».
- Los módulos ya guardan datos reales: miembros y jerarquía, grupos, aportes, asistencia con seguimiento, y consolidación. Pero para ver cómo va la iglesia hay que entrar módulo por módulo.
- El administrador del sistema no tiene ninguna vista entre iglesias. El SPEC 05 dejó fuera los reportes globales «hasta que haya datos de negocio», y ya los hay.
- El sistema no sabe quién lo usa: `iglesia-seguridad` no guarda ningún historial de accesos.
- El miembro con rol `MIEMBRO` no tiene nada que consultar. Es el punto 6 de la hoja de ruta (`CLAUDE.md`, «Portal del miembro»).

## Alcance

**Entra:**

- **Registro de actividad** en `iglesia-seguridad`: una fila por usuario y día en que inició sesión o renovó su sesión. Es la base de «uso del sistema».
- **Selector de periodo** común a los inicios con estadísticas: *Este mes*, *Últimos 3 meses*, *Últimos 12 meses* y *Este año*. Cada cifra se compara con el periodo anterior de la misma duración.
- **Inicio del administrador del sistema** (`/sistema`, vista global), solo estadístico. Sin nombres de personas ni datos personales:
  - crecimiento de iglesias: total, altas por mes, iglesias activas en el periodo e iglesias por tamaño;
  - crecimiento de miembros a nivel general: total, altas y bajas por mes, y distribución por condición;
  - uso del sistema: usuarios totales, usuarios activos (diarios, semanales, mensuales), actividad por día y por perfil, y actividad registrada por módulo (miembros registrados, asistencias marcadas, contactos de consolidación). Aportes nunca.
- **Inicio del administrador de iglesia** (`/administracion`): estadísticas de todos los módulos, cada bloque visible solo con su permiso:
  - miembros;
  - grupos;
  - asistencia;
  - consolidación;
  - aportes (solo con `aportes.ver`);
  - uso del sistema en su iglesia.

  Además, atajos a lo que hay que hacer hoy.
- **Inicio del líder** (`/mi-red`): lo mismo, limitado a su red (las personas que lidera directa e indirectamente) y a los grupos que lidera. Atajos: tomar la asistencia de su grupo hoy, las personas de su red con alerta y sus asignados de consolidación.
- **Inicio del miembro** (`/mi-perfil`): sus datos (nivel, grupo, líder, fecha de ingreso), su asistencia (indicadores, últimas semanas y últimas ocasiones) y su código QR de asistencia.
- **Endpoints de estadísticas por módulo** en `iglesia-api`, con el ámbito del token (iglesia o red), y endpoints «mi perfil» limitados al miembro del token.
- **Estadísticas entre iglesias** mediante funciones de PostgreSQL que solo devuelven agregados. Ver «Estadísticas entre iglesias».
- **Documentación y cierre:** OpenAPI, Postman, pruebas y diagrama, como en las specs anteriores.

**Fuera de alcance (para specs futuras):**

- Exportar estadísticas (Excel, PDF) y reportes programados por correo.
- Tableros configurables: elegir qué tarjetas ver, moverlas o fijarlas.
- Rangos de fechas libres; solo se usan los cuatro periodos del selector.
- Ver los propios aportes desde el inicio del miembro: hoy ni el miembro ve los suyos (regla de aportes).
- La consolidación desde el portal del miembro (el miembro no ve sus casos ni sus contactos).
- Auditoría detallada de acciones (quién cambió qué) e intentos de acceso fallidos. Solo se registra la actividad diaria.
- Notificaciones o alertas push desde el inicio.

## Conceptos y reglas

### Qué inicio ve cada uno

- El inicio sigue saliendo del **perfil elegido** (`RUTA_INICIO_POR_ROL[perfil.rolBase]`). Un rol personalizado usa el inicio de su rol base. Por ejemplo, «Secretaría», que hereda de administrador de iglesia, usa `/administracion`.
- Dentro de cada inicio, **cada bloque se muestra solo si el token tiene el permiso de su módulo**. El backend exige el mismo permiso en el endpoint, así que esconderlo en la pantalla no es la única barrera:

| Bloque | Permiso para verlo | Ámbito |
|---|---|---|
| Miembros | `miembros.ver` | Iglesia (administrador) o red (líder) |
| Grupos | `grupos.ver` (o `asistencia.grupo` para los grupos que lidera) | Iglesia o grupos del líder |
| Asistencia | `asistencia.ver` (iglesia) o `asistencia.grupo` (red y sus grupos) | Iglesia o red |
| Consolidación | `consolidacion.ver` o `consolidacion.administrar` (iglesia); `consolidacion.contactar` (solo «mis asignados») | Iglesia o propios |
| Aportes | `aportes.ver` | Iglesia |
| Uso del sistema en la iglesia | `usuarios.ver` | Iglesia |

- **Regla de aportes (obligatoria):** ningún endpoint de estadísticas fuera de `/api/estadisticas/aportes` devuelve importes, cantidades ni conteos de aportes.
  - `/api/estadisticas/aportes` exige `aportes.ver`.
  - Las estadísticas del sistema y del líder nunca los incluyen, y el inicio del miembro tampoco.
  - Si un rol personalizado de administración no tiene `aportes.ver`, no ve el bloque.
- El **administrador del sistema dentro de una iglesia** (SPEC 05, «Entrar») opera como su administrador. En ese caso su inicio `/sistema` muestra el inicio de administración de esa iglesia, con el aviso «Estás viendo el inicio de {iglesia}». En la vista global muestra las estadísticas del sistema.

### Periodo

- `PeriodoEstadistico`:
  - `MES`: del 1 del mes actual a hoy.
  - `TRIMESTRE`: los últimos 3 meses, contando hoy.
  - `ANIO_MOVIL`: los últimos 12 meses.
  - `ANIO`: del 1 de enero a hoy.
- El servidor calcula `desde` y `hasta` con el reloj de la aplicación. El **periodo anterior** tiene la misma duración y termina el día antes de `desde`.
- Las series van **por mes** en `ANIO_MOVIL` y `ANIO`, y **por semana** en `MES` y `TRIMESTRE`.
- El periodo elegido se recuerda por usuario en el navegador (`localStorage` con `try/catch`; es solo una comodidad).

### Indicadores por módulo (iglesia o red)

Todos se calculan al consultar, con consultas agregadas en la base, nunca en memoria ni en bucle. Las de la red usan la CTE recursiva de la red, filtrando `iglesia_id` en cada paso.

- **Miembros:**
  - total de activos y su variación frente al periodo anterior;
  - altas del periodo (por fecha de ingreso, o de registro si no tiene) y bajas (desactivados en el periodo);
  - serie de crecimiento neto;
  - por condición (Nuevo, Antiguo, Recuperado), por nivel, por género y por rango de edad (0–12, 13–17, 18–29, 30–44, 45–59, 60+ y «sin fecha»);
  - sin grupo y sin líder (este último, solo en la iglesia).
- **Grupos:**
  - grupos activos y su promedio de integrantes;
  - porcentaje de miembros activos que están en un grupo;
  - grupos sin ninguna reunión registrada en el periodo;
  - asistencia promedio de integrantes por grupo, con los 5 mejores y los 5 más bajos.
- **Asistencia:**
  - asistentes promedio por día de culto;
  - serie de asistentes por semana o mes;
  - porcentaje de asistencia de los integrantes a sus grupos;
  - personas con alerta, por tipo (reutiliza `SeguimientoAsistenciaService`, con el ámbito del token);
  - nuevos del periodo que volvieron al menos una vez.
- **Consolidación:**
  - rondas en curso;
  - casos abiertos y cerrados en el periodo;
  - contactos registrados (en total y por medio);
  - personas contactadas que volvieron a asistir;
  - cierres por resultado de la iteración.
- **Aportes** (solo `aportes.ver`): reutiliza `ResumenAportesService` con el rango del periodo, es decir, el total, el desglose por categoría y por medio, y la comparación con el periodo anterior. Agrega la serie por mes o semana en la moneda de la iglesia.
- **Uso del sistema en la iglesia** (`usuarios.ver`):
  - usuarios de la iglesia;
  - usuarios activos en el periodo y su serie;
  - activos por perfil (rol base).

### Uso del sistema (registro de actividad)

- `iglesia-seguridad` guarda en `actividad_usuario` **una fila por usuario y día** (`usuario_id`, `fecha`), con la `iglesia_id` y el rol base del perfil con el que entró.
  - Se registra al iniciar sesión con éxito y al renovar el token de acceso (`INSERT … ON CONFLICT DO NOTHING`). Así cuenta a quien se queda con la sesión abierta varios días.
  - Mide **actividad**, no clics ni páginas: no guarda direcciones IP, navegador ni ninguna acción.
  - Se cuenta desde que se despliega esta spec; el periodo anterior sale vacío al principio.
- Indicadores:
  - **activos diarios** (promedio de usuarios distintos por día);
  - **activos semanales** y **mensuales** (usuarios distintos en los últimos 7 y 30 días hasta `hasta`);
  - **activos del periodo**;
  - serie por día;
  - activos por rol base;
  - iglesias con al menos un usuario activo.
- Las filas de más de 25 meses se borran con una tarea programada diaria; la retención se define por propiedad.

### Estadísticas entre iglesias (administrador del sistema)

- En la vista global no hay `app.iglesia_id`, así que la RLS no deja leer ninguna fila de negocio (SPEC 05). Esta spec **no** desactiva la RLS ni crea un rol con `BYPASSRLS`.
- La migración crea **funciones de PostgreSQL** `estadisticas_sistema_*` con `SECURITY DEFINER`, cuyo dueño es el rol propietario de las tablas, con `SET search_path` fijo:
  - solo devuelven **agregados**: conteos por mes, por iglesia o por condición;
  - nunca devuelven nombres, correos, teléfonos ni identificadores de miembros;
  - el rol de la aplicación solo recibe `EXECUTE` sobre ellas.
- `iglesia-api` las llama desde un servicio exclusivo (`estadisticas/service/EstadisticasSistemaService`), con el permiso nuevo `estadisticas.sistema` y solo en la vista global (`esGlobal()`). Dentro de una iglesia responde 403.
- Las iglesias **sí** se nombran (catálogo de tenants, sin RLS). Las personas, nunca.
- No incluye aportes de ningún tipo.

### Portal del miembro

- Los endpoints `/api/mi-perfil/**` toman el miembro **solo** del claim `miembro_id` del token, nunca de un parámetro. Si el usuario no está vinculado a un miembro, responden 404 con «Tu usuario no está vinculado a un miembro de la iglesia».
- No exigen permisos de módulo: cualquier usuario autenticado vinculado ve **sus propios** datos. Es la regla de `CLAUDE.md` para el rol `MIEMBRO` y no abre nada sobre otras personas.
- Devuelven lo justo para el inicio:
  - datos: nombre, nivel, grupo con su día y hora, líder, fecha de ingreso y edad calculada;
  - asistencia: los mismos indicadores y semanas de la ficha (SPEC 09) y sus últimas ocasiones;
  - código QR de asistencia: el mismo contenido del fotocheck.

  No devuelven aportes, ni consolidación, ni datos de otras personas.

## Modelo de datos

### Migración `iglesia-seguridad/src/main/resources/db/migration/V10__actividad_y_estadisticas.sql`

```sql
-- Una fila por usuario y día con actividad (inicio de sesión o renovación del token).
CREATE TABLE actividad_usuario (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    fecha      DATE        NOT NULL,
    iglesia_id BIGINT,                 -- NULL: administrador del sistema en la vista global
    rol_base   VARCHAR(20) NOT NULL,   -- rol base del perfil con el que entró
    PRIMARY KEY (usuario_id, fecha)
);
CREATE INDEX ix_actividad_fecha_iglesia ON actividad_usuario (fecha, iglesia_id);

-- Permiso de las estadísticas entre iglesias, solo para el administrador del sistema.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('estadisticas.sistema', 'Sistema', 'Ver las estadísticas de todas las iglesias y el uso del sistema', …);
-- + rol_permiso para ADMIN_SISTEMA
```

### Migración `iglesia-api/src/main/resources/db/migration/V12__estadisticas_del_sistema.sql`

- Funciones `SECURITY DEFINER`, todas con `(desde date, hasta date)` y `SET search_path = public`:
  - `estadisticas_sistema_iglesias`: total, altas por mes e iglesias activas (con al menos un registro de asistencia, miembro o contacto en el periodo);
  - `estadisticas_sistema_miembros`: activos, altas y bajas por mes, y conteo por condición;
  - `estadisticas_sistema_tamanos`: iglesias por rango de miembros activos (1–50, 51–200, 201–500, 501–1000, más de 1000) y las 10 iglesias con más miembros (nombre de la iglesia y conteo);
  - `estadisticas_sistema_actividad`: miembros registrados, asistencias marcadas y contactos de consolidación por mes.
- `REVOKE ALL … FROM PUBLIC` y `GRANT EXECUTE … TO` el rol de la aplicación.
- Índices de apoyo si las consultas lo piden, por ejemplo `miembro (iglesia_id, fecha_ingreso)`, verificados con `EXPLAIN` en el MCP.

## Backend `iglesia-api`

- `common/periodo/model/PeriodoEstadistico` (enum) y `common/periodo/model/RangoPeriodo` (`record`: `desde`, `hasta`, `anteriorDesde`, `anteriorHasta`, `granularidad`), con `common/periodo/component/CalculadoraPeriodo` (usa `Clock`).
- Cada contexto expone sus estadísticas, sin depender de las clases internas de otro contexto:
  - `miembros/controller/EstadisticasMiembrosController` y `miembros/service/EstadisticasMiembrosService`, con el ámbito de `AmbitoMiembros` (iglesia o red);
  - `grupos/.../EstadisticasGruposController` y su `Service`;
  - `asistencia/.../EstadisticasAsistenciaController`, que reutiliza `SeguimientoAsistenciaService` para las alertas;
  - `consolidacion/.../EstadisticasConsolidacionController`;
  - `aportes/.../EstadisticasAportesController`, que reutiliza `ResumenAportesService` y agrega la serie.
- `estadisticas/` (contexto nuevo): `EstadisticasSistemaController` y `EstadisticasSistemaService`, que llama a las funciones de PostgreSQL por medio de un repositorio de consultas nativas.
- `perfil/` (contexto nuevo): `MiPerfilController` y `MiPerfilService`, que delegan en los servicios públicos de miembros y asistencia con el `miembroId` del token.
- DTOs `record` con `@Schema`. Cada respuesta incluye el `RangoPeriodo` usado, para que la pantalla diga «1 oct – 30 oct 2026».
- Logs: `DEBUG` con el periodo y los tiempos; sin datos personales.

### Endpoints de `iglesia-api`

| Método y ruta | Permiso | Devuelve |
|---|---|---|
| `GET /api/estadisticas/miembros?periodo=` | `miembros.ver` | Indicadores de miembros (iglesia o red) |
| `GET /api/estadisticas/grupos?periodo=` | `grupos.ver` o `asistencia.grupo` | Indicadores de grupos (todos o los que lidera) |
| `GET /api/estadisticas/asistencia?periodo=` | `asistencia.ver` o `asistencia.grupo` | Indicadores de asistencia (iglesia o red) |
| `GET /api/estadisticas/consolidacion?periodo=` | `consolidacion.ver` o `consolidacion.administrar` | Indicadores de consolidación de la iglesia |
| `GET /api/estadisticas/aportes?periodo=` | `aportes.ver` | Totales y serie de aportes |
| `GET /api/estadisticas/sistema?periodo=` | `estadisticas.sistema`, solo en la vista global | Iglesias, miembros y actividad entre iglesias |
| `GET /api/mi-perfil` | autenticado y vinculado | Datos propios |
| `GET /api/mi-perfil/asistencia` | autenticado y vinculado | Indicadores, semanas y últimas ocasiones propias |
| `GET /api/mi-perfil/codigo-asistencia` | autenticado y vinculado | Contenido del QR propio |

Las respuestas de error son las de siempre: 400 si el periodo no es válido, 401, 403 y 404 (miembro no vinculado), como `ProblemDetail` con `traceId`.

## Backend `iglesia-seguridad`

- `actividad/` (contexto nuevo):
  - `entity/ActividadUsuario`, `repository/ActividadUsuarioRepository` (inserción nativa con `ON CONFLICT DO NOTHING`);
  - `service/RegistroActividadService`, que llama el inicio de sesión y la renovación del token; una falla al registrar se informa con `WARN` y nunca impide entrar;
  - `component/LimpiezaActividad` (`@Scheduled`, retención por propiedad);
  - `service/EstadisticasUsoService` y `controller/EstadisticasUsoController`.
- Endpoints:
  - `GET /api/estadisticas/uso?periodo=` con `estadisticas.sistema` en la vista global: todas las iglesias.
  - `GET /api/estadisticas/uso?periodo=` con `usuarios.ver` dentro de una iglesia: solo esa iglesia, la del token.
  - Usa el mismo `PeriodoEstadistico`, copiado en su `common/periodo`, porque los backends no comparten código.

## Frontend (`iglesia-web/src/app/`)

- `features/inicio/` (ya existe):
  - `services/estadisticas.service.ts`: fachada HTTP de `/api/estadisticas/*`, del uso en seguridad y de `/api/mi-perfil/*`;
  - `models/` con los contratos y los adaptadores;
  - `pages/`: los cuatro inicios dejan de usar el aviso «Estamos preparando esta sección» y pasan a ser contenedores;
  - `components/`, presentacionales y cada uno con su prueba:
    - `selector-periodo`;
    - `tarjeta-indicador` (cifra, etiqueta, variación con flecha y ayuda);
    - `bloque-miembros`, `bloque-grupos`, `bloque-asistencia`, `bloque-consolidacion`, `bloque-aportes` y `bloque-uso`;
    - `atajos-del-dia`;
    - `mis-datos`, `mi-asistencia` y `mi-codigo-qr`.
- `shared/components/grafico-distribucion/`: un componente nuevo para distribuciones (barras horizontales o anillo), con su descripción accesible. Las series usan `grafico-evolucion`, que ya existe.
- `bienvenida-perfil` se mantiene como encabezado compacto: saludo, perfil, iglesia y el selector de periodo a la derecha.
- Cada bloque pide sus datos por separado: se carga, falla y se reintenta solo, sin bloquear a los demás. Si no hay permiso, el bloque no se crea y no se hace la petición.
- Las grillas (los 5 mejores y los 5 más bajos, las últimas ocasiones del miembro) siguen la regla de paginación de `CLAUDE.md`, con el paginador siempre visible.

## Diseño de la experiencia

### Principios

- **Lo importante primero:** una fila de 3 o 4 tarjetas con las cifras clave del periodo y su variación (↑ verde si mejora, ↓ rojo si empeora y neutral si no cambia). Debajo, los gráficos por módulo.
- **Una sola acción principal por pantalla:** el atajo más urgente destacado; los demás, discretos.
- **Siempre explicar la cifra:** cada tarjeta tiene una ayuda corta, por ejemplo «Usuarios distintos que entraron al menos una vez en los últimos 30 días».
- **Sin datos no es cero:** si el periodo anterior no tiene datos, se muestra «Sin datos para comparar» y no un «+100 %».
- Responsivo y móvil primero. Tarjetas en una columna en el móvil, 2 en tableta y 4 en escritorio.
- Modo claro y oscuro con los tokens de Sakai.
- Estados de carga con *skeletons* con la forma de cada bloque, vacíos que explican y errores con «Reintentar».

### Administrador del sistema (`/sistema`)

1. **Encabezado:** «Panorama del sistema» y el selector de periodo.
2. **Tarjetas:** iglesias activas, miembros activos (en total), usuarios activos en el mes y nuevas iglesias del periodo.
3. **Crecimiento:** iglesias por mes, miembros por mes (altas, bajas y neto), iglesias por tamaño y las 10 iglesias con más miembros. Esta última muestra solo el nombre de la iglesia y el conteo, sin datos personales.
4. **Uso del sistema:** activos diarios, semanales y mensuales; actividad por día; activos por perfil; actividad por módulo (miembros registrados, asistencias y contactos).
5. Atajo: «Ir a Iglesias».

### Administrador de iglesia (`/administracion`)

1. **Encabezado** con el nombre y el logo de la iglesia y el selector de periodo.
2. **Atajos del día**, según permisos:
   - «Registrar miembro»;
   - «Tomar asistencia de hoy» (si hay cultos o grupos programados hoy);
   - «Personas con alerta (N)»;
   - «Rondas en curso (N)».
3. **Tarjetas:** miembros activos, nuevos del periodo, asistencia promedio por culto y personas con alerta. «Total de aportes del periodo» solo aparece con `aportes.ver`.
4. **Bloques por módulo**, en este orden: miembros, asistencia, grupos, consolidación, aportes y uso del sistema. Cada bloque tiene un enlace «Ver más» a su módulo.

### Líder (`/mi-red`)

1. **Encabezado:** «Mi red» y el selector de periodo.
2. **Atajos:**
   - «Tomar asistencia de {grupo}» si hoy es su día;
   - «Personas de tu red con alerta (N)»;
   - «Mis asignados (N)» si es consolidador.
3. **Tarjetas:** personas en su red (directas y en total), nuevos en su red en el periodo, asistencia de su red y asistencia de su grupo.
4. **Bloques:**
   - su red: crecimiento y distribución por condición y nivel;
   - asistencia de su red: serie y alertas;
   - sus grupos: integrantes y asistencia por reunión.

   Nada de otras redes ni de toda la iglesia, y nunca aportes.

### Miembro (`/mi-perfil`)

1. **Encabezado:** «Hola, {nombre}» con su nivel y su grupo.
2. **Mi código de asistencia:** el QR grande, para mostrarlo en la puerta del culto, con «Descargar» o «Ver en pantalla completa».
3. **Mi asistencia:** porcentaje de las últimas 8 semanas, faltas seguidas, la última vez que asistió, las últimas semanas (`semanas-asistencia`) y sus últimas ocasiones paginadas.
4. **Mis datos:** líder, grupo con día, hora y lugar, fecha de ingreso y edad. Son de solo lectura; para corregirlos, «Pídeselo a tu líder».

### Accesibilidad y rendimiento

- Cada gráfico tiene una descripción textual (`descripcion`) y su tabla equivalente para lectores de pantalla.
- Las tarjetas son una lista de definiciones (`dl`).
- Cada endpoint de estadísticas responde en menos de 1 s con 5.000 miembros y 2 años de asistencia en la base local. Se verifica con datos de prueba y `EXPLAIN`.
- Los bloques se cargan en paralelo y con `@defer (on viewport)` los que quedan debajo del primer pantallazo.

## Plan de implementación

1. **Seguridad, actividad:** migración `V10`, contexto `actividad`, registro en el inicio de sesión y en la renovación, y tarea de limpieza. Pruebas: una sola fila por usuario y día; si el registro falla, igual se entra; la limpieza respeta la retención.
2. **Seguridad, estadísticas de uso:** endpoint `/api/estadisticas/uso` con los dos ámbitos y el permiso `estadisticas.sistema`. Pruebas de integración de ámbito y de aislamiento. Postman y OpenAPI.
3. **API, periodo común:** `PeriodoEstadistico`, `RangoPeriodo` y `CalculadoraPeriodo`, con pruebas unitarias de los 4 periodos, el periodo anterior y la granularidad.
4. **API, estadísticas por módulo:** miembros, grupos, asistencia, consolidación y aportes, con el ámbito iglesia o red. Pruebas de integración:
   - el líder solo ve su red;
   - otra iglesia no ve nada;
   - sin `aportes.ver`, 403 en aportes;
   - ningún otro endpoint devuelve campos de aportes.
5. **API, estadísticas del sistema:** migración `V12` con las funciones `SECURITY DEFINER`, servicio y endpoint. Pruebas:
   - solo en la vista global;
   - dentro de una iglesia, 403;
   - ninguna respuesta lleva datos personales;
   - el rol de la aplicación sigue sin poder leer tablas de otra iglesia fuera de las funciones.
6. **API, mi perfil:** los tres endpoints con el miembro del token. Pruebas: sin vínculo da 404; no hay forma de pedir datos de otro miembro.
7. **Frontend, base:** servicio, modelos, `selector-periodo`, `tarjeta-indicador` y `grafico-distribucion`, cada uno con su prueba.
8. **Frontend, inicio del miembro.**
9. **Frontend, inicio del líder.**
10. **Frontend, inicio del administrador de iglesia**, con bloques por permiso y atajos. El administrador del sistema dentro de una iglesia ve este mismo inicio.
11. **Frontend, inicio del sistema.**
12. **Datos de prueba** en la base local (varias iglesias, meses de actividad) y revisión en el navegador de los 4 inicios, en escritorio y celular, en modo claro y oscuro.
13. **Cierre:** Postman de ambos backends, OpenAPI, y el diagrama `diagramas/spec-11-paginas-de-inicio.drawio` con 4 páginas: arquitectura, un inicio por perfil y su ámbito, registro de actividad, y funciones entre iglesias y la RLS.

## Criterios de aceptación

- [ ] Cada perfil entra a su inicio y ya no ve «Estamos preparando esta sección».
- [ ] El selector de periodo cambia todas las cifras del inicio, cada una se compara con el periodo anterior de la misma duración, y el periodo elegido se recuerda al volver.
- [ ] **Administrador del sistema, en la vista global:**
  - [ ] ve crecimiento de iglesias, crecimiento de miembros en total y uso del sistema;
  - [ ] no ve ningún nombre de persona, correo, teléfono ni dato de aportes.
- [ ] **Administrador del sistema, dentro de una iglesia:** ve el inicio de administración de esa iglesia, y `GET /api/estadisticas/sistema` responde 403.
- [ ] Las funciones `estadisticas_sistema_*` solo devuelven agregados, y el rol de la aplicación sigue sin poder leer filas de negocio sin `app.iglesia_id`.
- [ ] Iniciar sesión o renovar el token deja una sola fila por usuario y día en `actividad_usuario`. Si ese registro falla, el usuario igual entra.
- [ ] **Administrador de iglesia:** ve estadísticas de miembros, grupos, asistencia, consolidación, aportes y uso, y atajos del día.
- [ ] Un rol personalizado de administración sin `aportes.ver`:
  - [ ] no ve el bloque ni la tarjeta de aportes, y no se hace la petición;
  - [ ] si llama al endpoint, recibe 403.
- [ ] Un rol sin `consolidacion.ver` (u otro permiso de módulo) no ve ese bloque.
- [ ] **Líder:**
  - [ ] solo ve cifras de su red y de los grupos que lidera;
  - [ ] no puede obtener cifras de otra red ni de toda la iglesia;
  - [ ] nunca ve aportes.
- [ ] **Miembro:**
  - [ ] ve sus datos, su asistencia y su QR;
  - [ ] no ve datos de otras personas ni aportes, ni siquiera los suyos;
  - [ ] sin miembro vinculado, ve un mensaje claro.
- [ ] Ningún endpoint de estadísticas, salvo `/api/estadisticas/aportes`, incluye campos de aportes (prueba automática sobre las respuestas).
- [ ] Otra iglesia nunca ve cifras de esta (pruebas de aislamiento en cada endpoint).
- [ ] Cada bloque tiene estados de carga, vacío y error con «Reintentar», sin afectar a los demás.
- [ ] Los 4 inicios se ven bien en el móvil y en modo oscuro, con gráficos accesibles (descripción y tabla equivalente).
- [ ] Toda grilla nueva muestra el paginador siempre y tiene su prueba.
- [ ] Toda pieza nueva tiene prueba unitaria; los endpoints, pruebas de integración, OpenAPI y Postman.
- [ ] Existe `diagramas/spec-11-paginas-de-inicio.drawio` con las páginas del paso 13.

## Decisiones

- **Sí:** registrar la actividad como una fila por usuario y día, al iniciar sesión y al renovar el token. **No:** registrar cada inicio de sesión ni cada acción. Una fila diaria basta para medir usuarios activos, ocupa poco y no guarda datos de navegación. *(Decidido con el usuario: «Registrar accesos».)*
- **Sí:** el miembro no ve ningún dato de aportes, ni los suyos. Se aplica la regla de aportes al pie de la letra; se podrá abrir en una spec futura con un permiso propio. *(Decidido con el usuario.)*
- **Sí:** un selector con 4 periodos y comparación con el periodo anterior. **No:** rangos libres. *(Decidido con el usuario.)*
- **Sí:** estadísticas y atajos a lo que hay que hacer hoy en los inicios de administración y del líder. *(Decidido con el usuario.)*
- **Sí:** que cada bloque dependa del permiso de su módulo, y no del rol base. Así los roles personalizados ven exactamente lo que se les dio, y la regla de aportes queda como «tener `aportes.ver`».
- **Sí:** para las cifras entre iglesias, funciones `SECURITY DEFINER` que solo devuelven agregados. **No:** un rol con `BYPASSRLS` ni desactivar la RLS. Así la excepción queda acotada a consultas revisables en una migración, y el resto del sistema mantiene sus dos barreras.
- **Sí:** un endpoint de estadísticas por módulo, dentro de su contexto. **No:** un endpoint único con todo. Así cada uno aplica su permiso y su ámbito, se puede cachear o reintentar por separado, y no se pide lo que no se va a mostrar.
- **Sí:** los endpoints `/api/mi-perfil/**` sin permiso de módulo, con el miembro tomado solo del token. Es la regla de `CLAUDE.md` para el rol `MIEMBRO` y no expone a nadie más.
- **Sí:** el administrador del sistema dentro de una iglesia ve el inicio de administración de esa iglesia. Opera como su administrador (SPEC 05), y mezclar cifras globales con las de una iglesia confundiría.

## Riesgos

- **Rendimiento de las consultas agregadas** en iglesias grandes o en las funciones globales. Se mitiga con índices compuestos que empiezan por `iglesia_id`, `EXPLAIN` en el MCP y el límite de 1 s del criterio de aceptación. Si no alcanza, se agrega caché por periodo en una spec posterior.
- **Funciones `SECURITY DEFINER`:** un error en una función podría filtrar datos entre iglesias. Se mitiga así:
  - devuelven solo agregados;
  - tienen `search_path` fijo;
  - se revisan en la migración;
  - hay pruebas que verifican que no devuelven columnas personales.
- **La actividad empieza vacía:** el uso del sistema y su comparación no tienen historia hasta que pase el tiempo. La pantalla muestra «Sin datos para comparar».
- **Regla de aportes:** un endpoint nuevo podría filtrar un conteo de aportes por descuido. Se mitiga con la prueba automática sobre las respuestas de estadísticas.

## Qué **no** entra en esta spec

Ver «Fuera de alcance». En resumen: exportar o programar reportes, tableros configurables, rangos libres, aportes propios para el miembro, consolidación en el portal del miembro, auditoría detallada de acciones y notificaciones.
