# SPEC 03 — Documentación OpenAPI y trazabilidad de logs

> **Estado:** Aprobado
> **Depende de:** SPEC 01
> **Fecha:** 2026-10-26
> **Objetivo:** Que ambos backends expongan su API documentada con OpenAPI y que cada petición deje logs estructurados identificados por un `traceId`, el cual llega al usuario en los errores para poder rastrear cualquier fallo en producción.

## Por qué existe esta spec

`CLAUDE.md` exige ahora dos reglas en toda API: documentación Swagger/OpenAPI lo más descriptiva posible, y trazabilidad de logs que permita rastrear un error de producción. Hoy ninguno de los dos backends cumple:

- No hay `springdoc`. La única referencia de la API es la colección de Postman.
- No hay tracing ni MDC. Los logs son texto plano sin identificador de petición. Hay un solo `LOG.error` en `ManejadorGlobalErrores` y casi ningún evento de negocio registrado.
- Los 401 y 403 que responde el filtro de Spring Security, antes de llegar al controlador, salen sin cuerpo `ProblemDetail`.
- Hay un solo `application.properties`, sin distinción entre desarrollo y producción.
- El frontend no tiene manejo global de errores HTTP. Ante un 500, cada página muestra un mensaje genérico sin nada que el usuario pueda reportar.

Se implementa **antes que la SPEC 02**, para que los endpoints de roles nazcan documentados y trazados.

## Alcance

**Entra:**

- **Perfiles (ambos backends):** `application.properties` queda **seguro por defecto**, con logs JSON y Swagger apagado. Un nuevo `application-local.properties` enciende Swagger y deja los logs en texto. `./gradlew bootRun` activa el perfil `local`.
- **OpenAPI (ambos backends):**
  - `springdoc-openapi`, en la versión 3.x compatible con Spring Boot 4.1 (se verifica al implementar).
  - Bean `OpenAPI` con título, versión y descripción, y esquema de seguridad `bearerAuth` (JWT).
  - Documentación completa de **todos los endpoints existentes**: `@Tag`, `@Operation`, `@ApiResponse` por cada código, `@Parameter` y `@Schema` en todos los DTO con ejemplo.
- **Trazabilidad (ambos backends):**
  - Micrometer Tracing (`micrometer-tracing-bridge-otel`) con muestreo de 100 %, sin exportador.
  - Cabecera `X-Trace-Id` en toda respuesta, también en los 401 y 403 del filtro de seguridad.
  - Propiedad `traceId` en todo `ProblemDetail`.
  - `usuarioId` e `iglesiaId` en el MDC cuando hay un JWT válido.
- **Errores (ambos backends):**
  - `AuthenticationEntryPoint` y `AccessDeniedHandler` propios que responden `ProblemDetail` en español con `traceId`.
  - `ManejadorGlobalErrores` registra `WARN` sin stack trace en 4xx y `ERROR` con la excepción completa en 5xx, una sola vez.
- **Logs de eventos de negocio en `iglesia-seguridad`** (nivel `INFO`, con identificadores y sin datos personales):
  - login exitoso;
  - login fallido (con `usuarioId` si el correo existe, o «usuario inexistente», más la IP);
  - refresco;
  - cierre de sesión;
  - cambio de contraseña;
  - usuario creado o actualizado.
- **IP real del cliente** detrás del proxy inverso: `server.forward-headers-strategy=framework`.
- **CORS:** `X-Trace-Id` se agrega a las cabeceras expuestas en ambos backends.
- **Frontend:**
  - Interceptor de errores que, ante un 5xx o un error de red, muestra un `p-toast` global con el código de seguimiento y un botón «Copiar».
  - Componente reutilizable `shared/components/codigo-seguimiento/`.
  - `<p-toast>` y `MessageService` en la raíz de la app.
- **Postman:** prueba a nivel de colección que exige la cabecera `X-Trace-Id` en toda respuesta, y una carpeta «5. Documentación» con `GET /v3/api-docs`.
- **Diagrama:** `diagramas/spec-03-documentacion-openapi-y-trazabilidad.drawio`.

**Fuera de alcance (para specs futuras):**

- Exportar trazas a un backend de observabilidad (Zipkin, Jaeger, OTLP) y centralizar logs (ELK, Loki).
- Propagar `traceparent` desde el frontend. La traza nace en el backend que recibe la petición.
- Métricas, paneles de control y alertas.
- Registro de auditoría persistente en base de datos.
- Logs de eventos de negocio de `iglesia-api` (todavía no tiene endpoints de negocio). Cada módulo los agrega en su spec, siguiendo `CLAUDE.md`.
- Clientes generados a partir de la especificación OpenAPI.

## Modelo de datos

**Base de datos:** esta spec no agrega migraciones.

### Configuración (ambos backends)

```properties
# application.properties (producción, seguro por defecto)
spring.application.name=iglesia-seguridad            # iglesia-api en el otro backend
logging.structured.format.console=ecs
management.tracing.sampling.probability=1.0
server.forward-headers-strategy=framework
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false

# application-local.properties
logging.structured.format.console=
logging.pattern.correlation=[%X{traceId:-},%X{spanId:-},u=%X{usuarioId:-},i=%X{iglesiaId:-}]
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

```groovy
// build.gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0'   // construida sobre Boot 4.1.2
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
tasks.named('bootRun') {
    systemProperty 'spring.profiles.active', 'local'
}
```

### Clases nuevas (en cada backend, bajo `common/<asunto>/<tipo>`, según la organización de paquetes de `CLAUDE.md`)

```java
// common/openapi/config/OpenApiConfig.java
//   @Bean OpenAPI: info (título, versión, descripción), componente bearerAuth (http, bearer, JWT)
//   y seguridad global; los endpoints públicos se marcan con @SecurityRequirements() vacío.
//   Rutas /v3/api-docs/** y /swagger-ui/** con permitAll en SeguridadHttpConfig.

// common/traza/constant/TrazaConstantes.java
public final class TrazaConstantes {
    public static final String CABECERA_TRACE_ID = "X-Trace-Id";
    public static final String PROPIEDAD_TRACE_ID = "traceId";
    public static final String MDC_USUARIO_ID = "usuarioId";
    public static final String MDC_IGLESIA_ID = "iglesiaId";
}

// common/traza/filter/FiltroCabeceraTraza.java   (OncePerRequestFilter, antes de la cadena de seguridad)
//   Agrega X-Trace-Id con el traceId actual (Tracer) antes de continuar, así sale incluso en 401/403.

// common/traza/filter/FiltroContextoUsuario.java (OncePerRequestFilter, después de la autenticación)
//   Si hay un JwtAuthenticationToken: MDC usuarioId (sub) e iglesiaId (claim); en finally, MDC.remove.

// common/traza/component/ProveedorTraceId.java      (componente con interfaz, inyectable y testeable)
//   Optional<String> traceIdActual()

// common/error/component/FabricaProblemDetail.java
//   Crea los ProblemDetail con title/detail en español y la propiedad traceId; la usan
//   ManejadorGlobalErrores, ManejadorErroresAuth, EntryPointProblemDetail y AccessDeniedProblemDetail.

// common/error/handler/EntryPointProblemDetail.java      implements AuthenticationEntryPoint  → 401 ProblemDetail
// common/error/handler/AccessDeniedProblemDetail.java    implements AccessDeniedHandler       → 403 ProblemDetail
```

Contrato de error (todas las respuestas 4xx y 5xx):

```json
{
  "type": "about:blank",
  "title": "Error interno",
  "status": 500,
  "detail": "Ocurrió un error inesperado. Comunica el código de seguimiento a soporte.",
  "instance": "/api/usuarios",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

Cabecera en toda respuesta: `X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736`.

### Logs de eventos (`iglesia-seguridad`)

| Evento | Nivel | Mensaje (parámetros `{}`) |
|---|---|---|
| Login exitoso | INFO | `Inicio de sesión exitoso usuarioId={}` |
| Login fallido | INFO | `Inicio de sesión fallido usuarioId={} ip={}` (`usuarioId=inexistente` si el correo no existe) |
| Login de usuario inactivo | INFO | `Inicio de sesión rechazado: usuario inactivo usuarioId={} ip={}` |
| Refresco | INFO | `Token renovado usuarioId={}` |
| Reutilización de refresco | WARN | (ya existe) |
| Cierre de sesión | INFO | `Sesión cerrada usuarioId={}` |
| Cambio de contraseña | INFO | `Contraseña cambiada usuarioId={}; sesiones revocadas` |
| Usuario creado o actualizado | INFO | `Usuario creado id={} iglesiaId={} por usuarioId={}` / `Usuario actualizado id={} por usuarioId={}` |
| 4xx | WARN | `{status} {método} {ruta}: {detail}` sin stack trace |
| 5xx | ERROR | `Error no controlado en {método} {ruta}` + excepción |

### Frontend

```text
src/app/core/interceptors/errores.interceptor.ts   5xx y status 0 → NotificadorErroresService
src/app/core/errores/notificador-errores.service.ts fachada sobre MessageService (toast 'errores-globales')
src/app/core/errores/extraer-trace-id.ts           ProblemDetail.traceId ?? cabecera X-Trace-Id ?? null
src/app/shared/components/codigo-seguimiento/      muestra el código y el botón «Copiar» (Clipboard API)
src/app.component.ts                               + <p-toast key="errores-globales">
src/app.config.ts                                  + MessageService y errores.interceptor (después de auth)
```

- **Mensaje del toast:**
  - 5xx: «Ocurrió un error inesperado. Si persiste, comunica este código a soporte», más el componente de código.
  - status 0: «No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo», sin código.
- Los 4xx **no** pasan por el toast global, porque los manejan las páginas (validaciones, 401 con refresco, etc.). El interceptor relanza siempre el error para que la página siga con su flujo (por ejemplo, quitar el estado de carga).

## Plan de implementación

Cada paso deja el sistema compilando y con sus pruebas en verde. Los pasos 1 a 7 se aplican primero en `iglesia-seguridad` y luego se replican en `iglesia-api`.

**Backends**

1. **Perfiles:** crear `application-local.properties`, mover a él lo que es solo de desarrollo, dejar `application.properties` seguro por defecto y configurar `bootRun` con el perfil `local`. Verificar que `./gradlew bootRun` arranca con logs en texto y que el JAR sin perfil arranca con logs JSON (ECS).
2. **Tracing:** agregar `micrometer-tracing-bridge-otel`, `ProveedorTraceId`, `TrazaConstantes` y `FiltroCabeceraTraza`, y exponer `X-Trace-Id` en CORS. Prueba de integración: cualquier respuesta, incluido `/actuator/health`, trae `X-Trace-Id` de 32 caracteres hexadecimales, y el log de esa petición tiene el mismo `traceId`.
3. **Errores:**
   - Crear `FabricaProblemDetail` y hacer que `ManejadorGlobalErrores` (y `ManejadorErroresAuth` en seguridad) la usen, con `WARN` para los 4xx y `ERROR` para los 5xx.
   - Crear `EntryPointProblemDetail` y `AccessDeniedProblemDetail` y registrarlos en `SeguridadHttpConfig`.
   - Pruebas de integración: un 401 sin token, un 403 sin permiso, un 404 y un 500 forzado devuelven `ProblemDetail` con `traceId` igual a la cabecera. El 500 no expone el mensaje interno.
   - Prueba con `OutputCaptureExtension`: el 500 registra el stack trace y el 404 no.
4. **MDC de usuario:** crear `FiltroContextoUsuario`. Prueba: un log emitido durante una petición autenticada contiene `usuarioId` e `iglesiaId`, y una petición posterior en el mismo hilo ya no los tiene.
5. **OpenAPI:** agregar `springdoc` y `OpenApiConfig`, y dar `permitAll` a sus rutas. Prueba: con el perfil `local`, `GET /v3/api-docs` responde 200 con el esquema `bearerAuth`; sin perfil, responde 404.
6. **Documentar los endpoints existentes** con `@Tag`, `@Operation` (resumen, descripción con reglas y rol requerido), `@ApiResponse` por código con `ProblemDetail` como esquema de error y `@Schema` con ejemplo en todos los DTO y campos:
   - `iglesia-seguridad`: `AuthController` (login, refresh, logout, documentando la cookie), `UsuarioController` (incluidos `/me` y `/me/contrasena`) y `JwksController` (público).
   - `iglesia-api`: no tiene controladores todavía; queda lista la configuración.

   Prueba que recorre `/v3/api-docs` y falla si alguna operación no tiene `summary` y `description`, o si algún esquema de DTO propio no tiene `description`.
7. **Eventos de negocio (solo `iglesia-seguridad`):** agregar los logs de la tabla en `AuthService`, `TokenRefrescoService` y `UsuarioService`. En el login fallido, la IP se obtiene de `HttpServletRequest#getRemoteAddr()` (ya resuelta por `forward-headers-strategy`) y se pasa desde el controlador. Prueba con `OutputCaptureExtension`: el login fallido registra `usuarioId` e IP, y **no** registra el correo ni la contraseña.
8. **Postman:**
   - Script de prueba a nivel de colección que exige la cabecera `X-Trace-Id` en ambas colecciones (la de `iglesia-api` se crea si no existe).
   - Carpeta «5. Documentación» con `GET {{seguridadUrl}}/../v3/api-docs` como variable `{{seguridadBaseUrl}}`, agregada al entorno local.

**Frontend (`iglesia-web`)**

9. Crear `extraer-trace-id.ts`, `NotificadorErroresService` y `shared/components/codigo-seguimiento/` (botón «Copiar» con `aria-label` y aviso «Copiado»). Specs.
10. Crear `errores.interceptor.ts`, registrarlo después de `auth.interceptor` y agregar `MessageService` y `<p-toast key="errores-globales">` en la raíz. Spec del interceptor:
    - un 500 con `ProblemDetail` notifica con su `traceId`;
    - un 503 sin cuerpo usa la cabecera;
    - un status 0 notifica sin código;
    - un 400 o un 404 no notifica;
    - el error siempre se relanza.

**Cierre**

11. Ampliar la sección «Trazabilidad y logs» de `CLAUDE.md` si la implementación dejó convenciones nuevas (nombres de clases comunes y perfil `local`).
12. Crear `diagramas/spec-03-documentacion-openapi-y-trazabilidad.drawio` con varias páginas:
    - Recorrido de una petición (filtros en orden: traza, seguridad, contexto de usuario, controlador y manejador de errores).
    - Anatomía de un error (de la excepción a `ProblemDetail`, al log y al toast con el código).
    - Perfiles `local` y por defecto.

    Con colores, leyenda y notas.

## Criterios de aceptación

**Backends (ambos)**

- [ ] `./gradlew bootRun` arranca con el perfil `local`: logs en texto con `[traceId,spanId,u=…,i=…]` y Swagger UI disponible en `/swagger-ui.html`.
- [ ] El JAR ejecutado sin perfil escribe logs JSON (ECS) con `service.name`, `trace.id`, `span.id` y, en peticiones autenticadas, `usuarioId` e `iglesiaId`. `/swagger-ui.html` y `/v3/api-docs` responden 404.
- [ ] Toda respuesta, incluidos 200, 401 sin token y `/actuator/health`, trae la cabecera `X-Trace-Id`.
- [ ] Los 401 (sin token o con token inválido) y 403 responden `ProblemDetail` en español con `traceId` igual a `X-Trace-Id`.
- [ ] Un 500 responde «Ocurrió un error inesperado…» con `traceId`, sin el mensaje ni la traza de la excepción. El log tiene una sola línea `ERROR` con el stack trace y el mismo `traceId`.
- [ ] Un 4xx deja una línea `WARN` sin stack trace.
- [ ] `X-Trace-Id` es legible desde el frontend (cabecera expuesta en CORS).
- [ ] `./gradlew test` pasa en ambos backends.

**`iglesia-seguridad`**

- [ ] En `/swagger-ui.html` aparecen las etiquetas «Autenticación», «Usuarios» e «Infraestructura». Cada operación tiene resumen, descripción y todas sus respuestas de error, y cada DTO muestra descripción y ejemplo por campo.
- [ ] Desde Swagger UI se puede hacer login, pegar el token en «Authorize» y llamar `GET /api/usuarios/me` con éxito.
- [ ] Un login fallido registra `INFO` con `usuarioId` (o `inexistente`) e IP, sin correo ni contraseña.
- [ ] Ningún log del proyecto contiene contraseñas, tokens, cabeceras `Authorization`/`Cookie` ni correos (revisión con `grep` de los logs de la suite de pruebas).
- [ ] La prueba que recorre `/v3/api-docs` pasa (no hay operación sin `summary`/`description` ni DTO sin `description`).

**Frontend**

- [ ] Ante un 500, aparece un toast «Ocurrió un error inesperado…» con el código de seguimiento, y «Copiar» lo deja en el portapapeles.
- [ ] Sin conexión al backend aparece «No se pudo conectar con el servidor…» sin código.
- [ ] Los 400, 401 y 404 no muestran el toast global: el login sigue mostrando «Correo o contraseña incorrectos» y el refresco ante 401 sigue funcionando.
- [ ] El toast se ve bien en móvil y en modo oscuro, y el botón «Copiar» se puede usar con teclado y tiene `aria-label`.
- [ ] Cada pieza nueva tiene su `.spec.ts` y `ng test` pasa.

**Documentación**

- [ ] Las colecciones de Postman exigen la cabecera `X-Trace-Id` en todas sus pruebas y tienen la carpeta «5. Documentación».
- [ ] Existe `diagramas/spec-03-documentacion-openapi-y-trazabilidad.drawio` con las páginas del paso 12.

## Decisiones

- **Sí:** spec separada de la SPEC 02 e implementada antes. Es un cambio transversal en los dos backends y en el frontend; así los endpoints de roles nacen ya documentados y con trazas.
- **Sí:** `springdoc-openapi` con anotaciones en el código. La documentación vive junto al endpoint y no se desfasa. Postman se mantiene para pruebas y flujos.
- **No:** escribir el `openapi.yaml` a mano (enfoque *contract-first*). Duplica la información de los DTO y se desincroniza.
- **Sí:** prueba automática que exige `summary`, `description` y descripción de los esquemas. Convierte «lo más descriptivo posible» en algo verificable.
- **Sí:** seguro por defecto. Sin perfil, los logs salen en JSON y Swagger está apagado; el perfil `local` lo activa `bootRun`. Si en producción se olvida configurar el perfil, no se expone Swagger.
- **No:** perfil `prod` explícito con desarrollo por defecto. Olvidarlo expondría Swagger.
- **Sí:** Micrometer Tracing con el puente OpenTelemetry y la cabecera W3C `traceparent`. Es la vía estándar de Spring Boot, llena el MDC sola y deja abierta la exportación a un backend de observabilidad sin cambiar código.
- **Sí:** muestreo al 100 % y sin exportador. Por ahora el `traceId` solo se usa para correlacionar logs y su costo es mínimo.
- **No:** propagar la traza desde el frontend. Por YAGNI: hoy no hay llamadas entre backends dentro de una petición y la traza que nace en el backend basta para rastrear.
- **Sí:** `X-Trace-Id` en la respuesta y `traceId` en el `ProblemDetail`. La cabecera cubre las respuestas sin cuerpo y la propiedad la ve el usuario sin herramientas.
- **Sí:** `AuthenticationEntryPoint` y `AccessDeniedHandler` propios. Sin ellos, los 401 y 403 del filtro saldrían sin `ProblemDetail` ni `traceId`.
- **Sí (implementación):** `EntryPointProblemDetail` y `AccessDeniedProblemDetail` no escriben el cuerpo ellos mismos: agregan la cabecera `WWW-Authenticate` (RFC 6750) y delegan en el `HandlerExceptionResolver` de Spring MVC, de modo que el `ManejadorGlobalErrores` arma el `ProblemDetail` con `FabricaProblemDetail` y registra el `WARN`. Así los 401 y 403 del filtro y los de `@PreAuthorize` salen por el mismo camino, sin duplicar la serialización ni el log.
- **Sí (implementación):** `FiltroContextoUsuario` se registra dentro de la cadena de Spring Security con `addFilterAfter(..., BearerTokenAuthenticationFilter.class)` y no como `@Component`. Como filtro de servlet independiente correría antes de la autenticación (o se saltaría la segunda vez por ser `OncePerRequestFilter`), y los 403 de la propia cadena quedarían sin `usuarioId`.
- **Sí:** el `ERROR` con stack trace se registra una sola vez, en el manejador global. Evita logs duplicados que confunden el rastreo.
- **Sí:** el login fallido registra `usuarioId` (o `inexistente`) e IP, nunca el correo. La IP permite detectar fuerza bruta. El correo es un dato personal y, si no existe, sería información que no aporta.
- **Sí:** interceptor global con toast solo para 5xx y errores de red. Los 4xx ya los manejan las páginas con mensajes junto al campo; un toast global duplicaría los mensajes.
- **Sí:** `ProveedorTraceId` como componente inyectable, para que las fábricas y los filtros se prueben sin depender del tracer real.
- **Sí:** cambio en `app.component.ts` y `app.config.ts`. No pertenecen a `layout/`, así que no rompen la regla de no tocar el shell.

- **Sí (cambio de alcance autorizado durante la implementación):** migrar `build.gradle.kts` y `settings.gradle.kts` de ambos backends a Groovy DSL (`build.gradle`, `settings.gradle`), por la nueva regla general de `CLAUDE.md`. Se hizo después del paso 1 y antes del paso 2 para no duplicar trabajo; es una conversión mecánica, sin cambiar dependencias ni versiones.

- **Sí (cambio de alcance autorizado durante la implementación):** reorganizar los paquetes de ambos backends en `<contexto>/<tipo>` (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `model`, `exception`, `handler`, `config`, `component`, `filter`, `constant`), por la nueva regla general de `CLAUDE.md`. Se hizo después del paso 2 y antes del paso 3: solo mueve clases y ajusta visibilidad (lo que se usa desde otro subpaquete pasa a `public`), sin cambiar comportamiento; las pruebas siguen el paquete de su clase.
- **Sí:** además de `micrometer-tracing-bridge-otel`, Boot 4 requiere `spring-boot-micrometer-tracing-opentelemetry` (la autoconfiguración del tracing vive en ese módulo) y, en pruebas, `spring-boot-micrometer-tracing-test` (`@AutoConfigureTracing`), porque el tracing está apagado por defecto en los tests.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| La versión de `springdoc` no es compatible con Spring Boot 4.1. | Se verifica la versión compatible al implementar (rama 3.x). Si no hay una estable, se decide antes de seguir y se registra en Decisiones. |
| `FiltroCabeceraTraza` corre antes de que exista la observación HTTP y no hay `traceId`. | Se ordena después del filtro de observación de Spring (`ServerHttpObservationFilter`) y antes de la cadena de seguridad. Lo cubre la prueba de 401 con cabecera. |
| Se cuelan datos personales en los logs (correo en un `detail` de 409, por ejemplo). | Los mensajes de `WARN` usan el `detail` del `ProblemDetail`. Se revisa que los `detail` existentes no incluyan correos (hoy «El correo ya está registrado» no incluye el valor). Hay un criterio con `grep` sobre los logs de las pruebas. |
| La IP registrada es la del proxy y no la del cliente. | `server.forward-headers-strategy=framework`. En producción, el proxy debe enviar `X-Forwarded-For` y solo él debe ser alcanzable. |
| El toast global se muestra junto con el mensaje de la página. | Solo actúa en 5xx y status 0. Las páginas que ya muestran un error genérico en esos casos se ajustan para no duplicar (revisión de `login.ts` y `cambiar-contrasena.ts`). |
| Los logs JSON son difíciles de leer en desarrollo. | En el perfil `local` los logs están en texto, con el patrón de correlación compacto. |

## Qué **no** entra en esta spec

- Exportar trazas o centralizar logs (Zipkin, OTLP, ELK, Loki).
- Propagar `traceparent` desde el frontend.
- Métricas, paneles de control y alertas.
- Auditoría persistente en base de datos.
- Eventos de negocio de `iglesia-api` (cada módulo en su spec).
- Clientes generados desde OpenAPI.

Cada uno, si llega, va en su propia spec.
