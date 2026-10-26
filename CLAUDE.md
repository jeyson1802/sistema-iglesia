# Sistema de Gestión – Iglesia

## Contexto del negocio
Las iglesias registran a sus miembros a mano, en papel o, como mucho, en Excel. Cuando la iglesia crece se pierde el control: no se sabe quién es líder de quién, qué rango tiene cada miembro ni cuántas personas lidera cada líder. Este sistema resuelve el registro de nuevos miembros y la administración de los actuales.

- **Multi-iglesia:** el sistema da servicio a varias iglesias. Cada iglesia se da de alta en el sistema y sus datos son totalmente independientes de las demás (esquema compartido con columna `iglesia_id` en cada tabla, aislado por Hibernate y por Row-Level Security de PostgreSQL).
- **Jerarquía de liderazgo:** toda la iglesia forma un árbol. Cada miembro tiene un líder, y ese líder a su vez tiene otro líder encima, hasta llegar a la cabeza: el **pastor principal**, que es el único sin líder. Se debe poder consultar:
  - a quién lidera directamente cada uno y cuántas personas tiene a su cargo (directas y en total, contando toda su red);
  - la cadena de liderazgo de cualquier miembro hasta el pastor principal;
  - el árbol completo de la iglesia o de una rama.

  Reglas: un solo pastor principal por iglesia; no se permiten ciclos (nadie puede quedar como líder de su propio líder ni de sí mismo); al cambiar el líder de alguien, se mueve toda su red con él. La jerarquía de liderazgo es independiente del nivel configurable (el nivel indica el rango; el líder indica de quién depende).
- **Niveles configurables (lo más importante):** los rangos/niveles de los miembros los define cada iglesia (p. ej. "pastor", "diácono", "líder", etc.), con su orden jerárquico. Nunca se codifican en duro.
- **Grupos configurables:** el nombre del tipo de grupo pequeño lo define cada iglesia (p. ej. "célula" en una, "hogar de paz" en otra). Un grupo tiene un líder y sus miembros.

### Datos de registro de un nuevo miembro
- Nombre (nombres y apellidos por separado)
- Fecha de nacimiento
- Edad (se calcula a partir de la fecha de nacimiento; no se guarda)
- Sexo
- Estado civil: soltero, casado, divorciado, viudo
- Teléfono
- Correo electrónico
- Quién lo invitó a la iglesia (referencia a un miembro)
- Quién lo registró en el sistema (usuario autenticado, se toma del token)
- Quién lo va a liderar (referencia a un miembro)
- Petición o necesidad con la que llega (opcional, texto libre)

### Hoja de ruta (el sistema irá creciendo)
Diseñar desde el inicio pensando en estos módulos, aunque se implementen por etapas. Cada uno será su propio feature (`features/<modulo>/` en el frontend y su propio paquete en `iglesia-api`):

1. **Miembros actuales:** registro de los miembros que ya existen en la iglesia, con su líder, nivel y grupo.
2. **Nuevos miembros:** registro de las personas que van llegando, con los datos de ingreso de la sección anterior.
3. **Ofrendas y diezmos:** registro y consulta de ofrendas y diezmos por persona.
4. **Seguimiento y consolidación:** registro de llamadas y contactos a los nuevos miembros (fecha, quién llamó, resultado, observaciones) para dar seguimiento a su consolidación en la iglesia.
5. **Asistencia (prioritario):** control de asistencia de los miembros a reuniones y cultos. Se evaluará identificar a cada miembro con un código propio (p. ej. un código QR personal) para agilizar el registro.
6. **Portal del miembro:** el login, el JWT y la seguridad no son solo para administradores; los miembros también tendrán usuario para consultar su propia información, como su asistencia.
7. **Académico (futuro):** clases y discipulado, con niveles de formación por los que avanza cada discípulo para alcanzar ciertos niveles en la iglesia. Estos niveles académicos también deben ser configurables por iglesia.

Implicaciones de diseño:
- El miembro es la entidad central; los demás módulos se relacionan con él.
- Roles mínimos previstos: administrador del sistema, administrador de iglesia, líder y miembro. Un miembro con rol `MIEMBRO` solo puede ver su propia información; un líder, la de toda su red (las personas que lidera directa e indirectamente).
- El usuario de `iglesia-seguridad` se vincula con el miembro de `iglesia-api` mediante un identificador incluido en el JWT, sin compartir bases de datos.

## Arquitectura general
Tres proyectos independientes dentro de este repositorio:

| Proyecto | Descripción | Base de datos |
|---|---|---|
| `iglesia-web/` | Frontend Angular | — |
| `iglesia-api/` | Backend de negocio (Spring Boot) | PostgreSQL `iglesia_negocio` (esquema compartido, discriminado por `iglesia_id`) |
| `iglesia-seguridad/` | Backend de seguridad: autenticación, usuarios, roles, emisión de JWT (Spring Boot) | PostgreSQL `iglesia_seguridad` |

- La seguridad está **completamente separada** del negocio: cada backend tiene su propia base de datos y no comparten tablas ni conexiones.
- `iglesia-seguridad` emite el JWT; `iglesia-api` solo lo valida (resource server) y nunca gestiona credenciales.
- El JWT incluye, como mínimo: usuario, roles e identificador de la iglesia (tenant). `iglesia-api` resuelve la iglesia (`iglesia_id`) a partir de ese claim, nunca a partir de un parámetro enviado por el cliente.

## Buenas prácticas y patrones de diseño (obligatorio en back y front)
Todo el código, tanto de los backends como del frontend, debe aplicar **siempre** buenas prácticas y patrones de diseño:

- **Principios:** SOLID, DRY, KISS y YAGNI. Clases y funciones pequeñas con una sola responsabilidad; nombres claros y descriptivos.
- **Separación de responsabilidades:** cada capa hace solo lo suyo (presentación, lógica de negocio, acceso a datos). Nada de lógica de negocio en controladores, plantillas ni repositorios.
- **Bajo acoplamiento y alta cohesión:** depender de abstracciones (interfaces) y usar inyección de dependencias; evitar dependencias circulares entre módulos/features.
- **Patrones de diseño** cuando aporten claridad, sin sobreingeniería. Por ejemplo:
  - Backend: capas (controller/service/repository), DTO, Mapper, Strategy, Factory, Builder, Specification, Template Method, Observer/eventos de dominio.
  - Frontend: componentes contenedor/presentacionales (smart/dumb), servicios como fachada (Facade) para el estado y el acceso HTTP, interceptores, guards, adaptadores/mappers entre la API y los modelos de la UI.
- **Inmutabilidad** por defecto (`record` en Java, `readonly` y estado inmutable en signals en Angular).
- **Manejo de errores** explícito y centralizado; nunca silenciar excepciones.
- **Código limpio:** sin código muerto, sin valores mágicos (usar constantes o enums), sin duplicación; refactorizar cuando haga falta.
- **Pruebas** como parte del trabajo: código diseñado para ser testeable y cubierto por pruebas unitarias (y de integración en los backends).
- **Seguridad por diseño:** validar toda entrada, mínimo privilegio y nunca confiar en datos del cliente.

## Documentación técnica (obligatorio)
- **Postman:** todo servicio REST que se cree o modifique se agrega a la colección de Postman de su backend, en la carpeta `postman/` de la raíz del monorepo:
  - Una colección por backend (`postman/<proyecto>.postman_collection.json`, formato v2.1) y un entorno local (`postman/local.postman_environment.json`).
  - Carpetas ordenadas semánticamente por recurso o caso de uso (p. ej. «1. Autenticación», «2. Mi cuenta», «3. Administración de usuarios»), en el orden en que se usan.
  - Cada request con descripción, cuerpo de ejemplo válido y pruebas básicas; URLs y tokens como variables (`{{seguridadUrl}}`, `{{tokenAcceso}}`) y scripts que guarden el token tras el login.
  - Nunca contraseñas ni secretos reales en el repositorio: los valores sensibles del entorno van vacíos.
- **Swagger / OpenAPI:** toda API REST de los backends se documenta con OpenAPI 3 (`springdoc-openapi`), lo más descriptiva posible:
  - Cada controlador con `@Tag` (nombre y descripción del recurso) y cada endpoint con `@Operation` (`summary` corto y `description` que explique qué hace, reglas de negocio y permisos requeridos).
  - Todas las respuestas posibles declaradas con `@ApiResponse` (éxito y cada error: 400, 401, 403, 404, 409…), indicando cuándo ocurre cada una y que los errores se devuelven como `ProblemDetail`.
  - DTOs (`record`) con `@Schema` en la clase y en cada campo: descripción, ejemplo válido, formato y restricciones (las de Bean Validation se reflejan solas; no duplicarlas a mano). Parámetros de ruta y consulta con `@Parameter`.
  - Esquema de seguridad `bearerAuth` (JWT) declarado una vez en la configuración y aplicado a los endpoints protegidos; los públicos se marcan sin seguridad.
  - Información general en un bean `OpenAPI` (título, versión, descripción del backend). Swagger UI en `/swagger-ui.html` y especificación en `/v3/api-docs`, habilitados en desarrollo y deshabilitados en producción por propiedad (`springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled`).
  - La documentación OpenAPI es parte del endpoint: no se da por terminado un servicio sin ella (igual que con Postman).
- **Diagrama al cerrar un spec:** al terminar la implementación de cada spec se genera `diagramas/spec-NN-slug.drawio` con el flujo creado: componentes del frontend, servicios REST que consumen, clases del backend que los atienden y tablas que leen o escriben. Debe ser didáctico (técnico pero entendible): varias páginas (arquitectura y flujos principales), colores con leyenda y notas que expliquen las decisiones importantes.

## Frontend (`iglesia-web/`)

### Stack
- Angular 21 (standalone, signals, control flow `@if`/`@for`/`@switch`)
- PrimeNG 21 + Tailwind 4 (base: plantilla Sakai)
- Pruebas unitarias: Karma + Jasmine (`ng test`)

### Reglas
- Usar **signals** para el estado (`signal`, `computed`, `effect`, `input()`, `output()`, `model()`).
- **Formularios reactivos tipados** (`FormGroup`/`FormControl` tipados o `NonNullableFormBuilder`), con validaciones en el formulario, no en la plantilla.
- Solo control flow nuevo (`@if`, `@for` con `track`, `@switch`, `@defer`). No usar `*ngIf`, `*ngFor` ni `ngSwitch`.
- Inyección con `inject()`; `ChangeDetectionStrategy.OnPush` en los componentes.
- Lo que se repita entre pantallas se extrae a **componentes reutilizables** en `shared/components/` (p. ej. selector de miembro, selector de nivel, tabla paginada).
- NO modificar `src/app/layout/` salvo que se pida explícitamente (excepción: `app.menu.ts` para agregar módulos al menú). Si hace falta adaptar otros componentes de la plantilla, se puede hacer.
- Usar componentes PrimeNG y utilidades Tailwind. Evitar CSS propio (tampoco `style` inline).
- Antes de crear una pantalla, revisar los ejemplos en `src/app/pages/uikit/` y las páginas de Sakai (`src/app/pages/`), y partir de su estructura, clases y espaciados.
- **UX/UI en toda plantilla HTML:** aplicar siempre las mejores prácticas de diseño de interfaces con el estilo visual de Sakai:
  - Jerarquía visual clara (un título, una acción principal destacada, acciones secundarias discretas) y diseño consistente entre pantallas.
  - Responsivo (móvil primero) y compatible con modo claro y oscuro (tokens `surface`/`primary` de Sakai, nunca colores fijos).
  - Accesibilidad: `label` asociado a cada campo, `autocomplete` correcto, foco visible, navegación por teclado, `aria-*` donde haga falta y contraste suficiente.
  - Retroalimentación inmediata: estados de carga, vacíos y de error; mensajes de validación junto al campo, en español y que digan cómo corregir.
  - Prevenir errores: deshabilitar acciones mientras se procesan y pedir confirmación en acciones destructivas.
- **Identidad de la iglesia:** nunca se muestra el logo de la plantilla. Logo y nombre salen siempre del componente compartido `app-logo-iglesia` (`shared/components/logo-iglesia/`): en el layout principal, los de la iglesia del usuario autenticado; en el login, la identidad neutra del sistema (antes de autenticarse no se conoce la iglesia).
- Servicios con `HttpClient` y URL base desde `environment`.
- Textos de la interfaz en español.
- Toda pieza nueva (componente, servicio, pipe) lleva su prueba unitaria `.spec.ts`.
- Documentación PrimeNG: https://primeng.org/llms/llms.txt

### Estructura (`iglesia-web/src/`)
- `environments/`: `environment.ts` (producción, `apiUrl: '/api'`) y `environment.development.ts` (`http://localhost:8080/api`), reemplazado vía `fileReplacements`. Agregar también la URL del servicio de seguridad.
- `app/layout/`: shell de Sakai (no tocar; solo `app.menu.ts`).
- `app/pages/`: demos de Sakai, solo como referencia.
- `app/core/`: piezas únicas de la app (`auth/` con login, guards y almacenamiento del token; `interceptors/` con el interceptor que agrega el JWT; `models/` globales como `Page<T>`).
- `app/shared/`: reutilizable entre módulos (`components/`, `pipes/`, `utils/`).
- `app/features/<modulo>/`: `pages/`, `components/`, `services/`, `models/` y `<modulo>.routes.ts` con `export default` de `Routes`, registrado con `loadChildren` como hijo de `AppLayout` en `src/app.routes.ts`. Ejemplo: `features/miembros/`.
- Archivos sin sufijo `.component`. Cada componente se divide en **tres archivos** con el mismo nombre base: `<nombre>.ts` (clase, con `templateUrl`), `<nombre>.html` (plantilla) y `<nombre>.spec.ts` (prueba unitaria). No usar plantillas inline (`template:`).
- Los componentes son standalone (valor por defecto en Angular 21; no declarar `standalone: true` ni usar `NgModule`) y el nombre de su clase termina en `Component` (p. ej. `LoginComponent` en `login.ts`).
- Servicios de features con `providedIn: 'root'`.

### Notas importantes
- **App zoneless** (`provideZonelessChangeDetection`): el estado que controla la UI debe usar signals. La detección de cambios no detecta mutaciones simples de campos hechas desde callbacks asíncronos.
- **Estilos globales:** `src/assets/` (styles.scss, tailwind.css, layout/, demo/) viene copiado del repo `cetincakiroglu/sakai-assets` y está versionado directamente en este repo (ya no es submódulo). Los estilos del layout viven en `src/assets/layout/`.

## Backends (`iglesia-api/` e `iglesia-seguridad/`)

### Stack
- Spring Boot: última versión estable al momento de crear el proyecto (verificarla en https://spring.io/projects/spring-boot).
- Java: la versión LTS compatible con esa versión de Spring Boot.
- **Gradle** con **Groovy DSL** (`build.gradle` y `settings.gradle`) y Gradle Wrapper. No usar Kotlin DSL (`.gradle.kts`) ni Maven.
- PostgreSQL. Migraciones de esquema versionadas con Flyway.
- Pruebas unitarias con JUnit 5 + Mockito; pruebas de integración con Testcontainers (PostgreSQL).

### Organización de paquetes (obligatorio)
Primero por **contexto de negocio** y, dentro de cada contexto, por **capa o tipo técnico**: `com.iglesia.<backend>.<contexto>.<tipo>`. Un desarrollador debe poder ubicar cualquier clase solo por lo que es.

| Subpaquete | Contiene |
|---|---|
| `controller` | `@RestController` (solo HTTP: recibir, validar con `@Valid`, delegar y responder) |
| `service` | Lógica de negocio y casos de uso (`@Service`), incluidas las políticas o reglas de autorización de negocio |
| `repository` | Interfaces de Spring Data |
| `entity` | `@Entity` y `@MappedSuperclass` |
| `dto` | `record` de entrada y salida de la API (`*Request`, `*Response`) |
| `mapper` | Conversión entre entidades y DTOs |
| `model` | Tipos de dominio que no son entidades: enums, value objects y `record` internos |
| `exception` | Excepciones propias del contexto |
| `handler` | `@RestControllerAdvice` y manejadores de errores |
| `config` | `@Configuration` y `@ConfigurationProperties` |
| `component` | Otros `@Component` de infraestructura (proveedores, generadores, inicializadores) |
| `filter` | Filtros servlet (`OncePerRequestFilter`) |
| `constant` | Clases de constantes (`final` con constructor privado) |
| `util` | Funciones utilitarias estáticas y sin estado |

- Solo se crean los subpaquetes que el contexto necesita; nada de paquetes vacíos «por si acaso».
- Lo transversal va en `common/<asunto>/<tipo>` (p. ej. `common/error/exception`, `common/error/handler`, `common/traza/filter`); la configuración global de la aplicación, en `config/` de la raíz.
- Los contextos no dependen de las clases internas de otros contextos salvo a través de sus `service`, `dto` o `model`; las dependencias entre contextos nunca son circulares.
- Visibilidad: al separar por subpaquetes, las clases que se usan desde otro subpaquete son `public`; lo que solo se usa dentro de su subpaquete se deja con visibilidad de paquete.
- Las pruebas replican el paquete de la clase que prueban.

### Acceso a datos
- **Spring Data JPA (Hibernate)** con entidades `@Entity` que mapean las tablas y consultas en **JPQL** (`@Query`) cuando no basten los métodos derivados.
- Parámetros siempre enlazados (`:parametro`); nunca concatenar texto para armar consultas.
- Jerarquía de liderazgo como autorrelación (`lider_id` → miembro). Para recorrer redes completas o cadenas hasta el pastor usar CTE recursivas (`WITH RECURSIVE`, soportado en HQL de Hibernate 6 o en consulta nativa), nunca recorridos en memoria ni consultas en bucle.
- Relaciones con `FetchType.LAZY` por defecto; usar `JOIN FETCH` o `@EntityGraph` para evitar N+1.
- `spring.jpa.open-in-view=false` y `ddl-auto=validate`: el esquema lo crea Flyway, no Hibernate.
- Patrón por capas: `controller` → `service` → `repository` (Spring Data). Las entidades no salen de la capa de servicio: la API expone DTOs como `record` de Java.
- Validación de entrada con Bean Validation (`@Valid`) y manejo centralizado de errores con `@RestControllerAdvice` y `ProblemDetail`.

### Multi-iglesia (`iglesia-api`)
- **Esquema compartido con discriminador:** una sola base `iglesia_negocio` y un solo esquema para todas las iglesias. La tabla `iglesia` es el catálogo de iglesias y todas las tablas de negocio llevan la columna `iglesia_id` (FK a `iglesia`, `NOT NULL`).
- Dar de alta una iglesia es insertar una fila en `iglesia`; no se crean bases ni esquemas. Las migraciones de Flyway se aplican una sola vez para todas las iglesias.
- **Primera barrera, Hibernate:** multi-tenancy por discriminador con `@TenantId` en la columna `iglesia_id` de cada entidad. `CurrentTenantIdentifierResolver` obtiene la iglesia del claim del JWT; Hibernate filtra automáticamente todas las consultas y llena `iglesia_id` al insertar. El contexto se limpia al terminar la petición.
- **Segunda barrera, PostgreSQL Row-Level Security:** toda tabla con `iglesia_id` tiene RLS habilitado y una política `USING (iglesia_id = current_setting('app.iglesia_id')::bigint)`. La aplicación ejecuta `SET LOCAL app.iglesia_id = ...` al inicio de cada transacción y se conecta con un rol que **no** es dueño de las tablas (para que la RLS se aplique). Las tablas y políticas las crea Flyway con el rol dueño.
- Las consultas nativas y las CTE recursivas (`WITH RECURSIVE`) también filtran explícitamente por `iglesia_id`, incluido el paso recursivo.
- Índices y restricciones de unicidad compuestos que empiezan por `iglesia_id` (p. ej. `UNIQUE (iglesia_id, correo)`, `INDEX (iglesia_id, lider_id)`). Las FK entre tablas de negocio no deben permitir referenciar filas de otra iglesia (validarlo en el servicio y, cuando sea posible, con FK compuestas `(iglesia_id, id)`).
- Niveles, tipos de grupo y demás catálogos configurables se guardan por iglesia (con su `iglesia_id`), nunca como catálogos globales.
- Solo el rol administrador del sistema puede operar entre iglesias (p. ej. reportes globales), mediante endpoints y servicios explícitos para ello.
- **Desarrollo con MCP de PostgreSQL:** el MCP se conecta solo a la base local o de desarrollo, con el rol dueño (ve todas las iglesias). Los cambios de esquema siempre van como migración de Flyway (`V{n}__descripcion.sql`), nunca como DDL manual; el MCP se usa para datos de prueba, verificación y depuración.

### Seguridad (`iglesia-seguridad` y validación en `iglesia-api`)
- Usar la suite de **Spring Security**: autenticación, autorización por **roles** y emisión/validación de **JWT** (OAuth2 Resource Server en `iglesia-api`).
- Contraseñas con hash BCrypt o Argon2; nunca en texto plano.
- Tokens firmados (preferentemente con par de claves asimétricas, RS256) con expiración corta y refresh token.
- Toda la comunicación por HTTPS; CORS restringido a los orígenes del frontend.
- Secretos y credenciales por variables de entorno, nunca en el repositorio.
- Los datos personales de los miembros son sensibles: no exponerlos en logs.

### Trazabilidad y logs (obligatorio en toda API)
El objetivo es que, ante un error en producción, se pueda rastrear la petición completa (frontend → backend → base de datos) a partir de un solo identificador.
- **Identificador de traza por petición:** Micrometer Tracing (`micrometer-tracing-bridge-otel`) genera o propaga `traceId`/`spanId` (cabecera W3C `traceparent`). Toda respuesta devuelve la cabecera `X-Trace-Id` y todo `ProblemDetail` de error incluye la propiedad `traceId`, para que el usuario o soporte puedan reportarlo.
- **Contexto en MDC:** cada línea de log lleva `traceId`, `spanId` y, si hay usuario autenticado, `usuarioId` e `iglesiaId` (de los claims del JWT; nunca correo, nombre ni otros datos personales). El contexto se limpia al terminar la petición.
- **Formato:** logs estructurados en JSON en producción (logging estructurado de Spring Boot, `logging.structured.format.console=ecs`) y legibles en texto en desarrollo, por perfil. Nombre de la aplicación (`spring.application.name`) en cada línea.
- **Qué registrar y con qué nivel** (SLF4J con parámetros `{}`, nunca concatenación):
  - `INFO`: eventos de negocio relevantes (alta, cambio o baja de entidades, login exitoso o fallido, cambios de permisos), con identificadores, no datos personales.
  - `WARN`: errores del cliente (4xx) y reglas de negocio rechazadas, sin stack trace.
  - `ERROR`: errores inesperados (5xx) **siempre con la excepción completa** (stack trace), registrados una sola vez en el `@RestControllerAdvice` centralizado; las capas inferiores no registran y relanzan (evitar logs duplicados).
  - `DEBUG`: detalle técnico (consultas, decisiones internas), desactivado en producción.
- **Nunca** registrar contraseñas, tokens (JWT, refresco), cabeceras `Authorization`/`Cookie` ni cuerpos completos de peticiones con datos personales.
- **Respuesta al cliente:** los 5xx devuelven un mensaje genérico en español con el `traceId`, nunca el mensaje interno ni el stack trace.
- **Frontend:** el manejo centralizado de errores HTTP muestra el `traceId` en los errores inesperados («Código de seguimiento: …») para que el usuario pueda reportarlo.
  - `core/interceptors/errores.interceptor.ts` (registrado después de `auth.interceptor`) avisa los 5xx y la falta de conexión por `NotificadorErroresService` (`core/errores/`), que muestra el toast global `app-toast-errores-globales` con `shared/components/codigo-seguimiento`.
  - `core/errores/clasificar-error.ts` (`esErrorGlobal`) define qué errores avisa el toast. Las páginas manejan solo los 4xx (mensajes junto al formulario) y no repiten el aviso de un 5xx o de un error de conexión.
- **Pruebas:** se verifica que los errores devuelvan `traceId` en el `ProblemDetail` y la cabecera `X-Trace-Id`. Los logs se verifican con `OutputCaptureExtension` (p. ej. que un 5xx deja una sola línea `ERROR` con el stack trace, o que un evento no incluye correo ni contraseña).
- **Perfiles:** `application.properties` es el de producción y seguro por defecto: logs JSON (ECS) y Swagger deshabilitado. `application-local.properties` (perfil `local`, activado por `./gradlew bootRun`) pone logs en texto con `[traceId,spanId,u=usuarioId,i=iglesiaId]` y habilita Swagger UI. Las pruebas corren sin perfil, así que ven el formato JSON de producción.
- **Piezas comunes** (iguales en ambos backends, en `common/<asunto>/<tipo>`; se reutilizan, no se duplican):
  - `common/traza/filter/FiltroCabeceraTraza`: agrega `X-Trace-Id` a toda respuesta. El valor lo da `common/traza/component/ProveedorTraceId`, y los nombres de cabecera y propiedad están en `common/traza/constant/TrazaConstantes`.
  - `common/traza/filter/FiltroContextoUsuario`: pone `usuarioId` e `iglesiaId` en el MDC. No es un `@Component`: se registra en la cadena de seguridad, después de `BearerTokenAuthenticationFilter`.
  - `common/error/component/FabricaProblemDetail`: crea todo `ProblemDetail` (título y detalle en español y `traceId`). Ningún otro código arma `ProblemDetail` a mano.
  - `common/error/handler/ManejadorGlobalErrores`: registra el `WARN`/`ERROR` de las respuestas de error. Un manejador propio de un contexto, como `auth/handler/ManejadorErroresAuth`, sigue la misma regla. `EntryPointProblemDetail` (401) y `AccessDeniedProblemDetail` (403) delegan en él los errores de Spring Security.
  - `common/openapi/config/OpenApiConfig`: esquema `bearerAuth`, información del backend y cabecera `X-Trace-Id` documentada en todas las respuestas. Las rutas de la documentación son públicas (`RUTAS_DOCUMENTACION`).
  - `common/openapi/model/ProblemDetailEsquema` y `common/openapi/constant/RespuestasOpenApi`: esquema y descripciones comunes de los errores para `@ApiResponse` (`content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailEsquema.class))`). Se copian a `iglesia-api` con su primer controlador.

## Comandos
- Frontend, desde `iglesia-web/`: `ng serve` / `ng build` / `ng test`
- Backends, desde `iglesia-api/` o `iglesia-seguridad/`: `./gradlew bootRun` / `./gradlew build` / `./gradlew test`
- Documentación de la API con el backend en perfil `local`: Swagger UI en `http://localhost:<puerto>/swagger-ui.html` y especificación en `/v3/api-docs` (8081 seguridad, 8080 api).
