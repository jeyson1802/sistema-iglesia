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
- Usar componentes PrimeNG y utilidades Tailwind. Evitar CSS propio.
- Antes de crear una pantalla, revisar los ejemplos en `src/app/pages/uikit/`.
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
- Archivos sin sufijo `.component` y plantillas inline. Servicios de features con `providedIn: 'root'`.

### Notas importantes
- **App zoneless** (`provideZonelessChangeDetection`): el estado que controla la UI debe usar signals. La detección de cambios no detecta mutaciones simples de campos hechas desde callbacks asíncronos.
- **Estilos globales:** `src/assets/` (styles.scss, tailwind.css, layout/, demo/) viene copiado del repo `cetincakiroglu/sakai-assets` y está versionado directamente en este repo (ya no es submódulo). Los estilos del layout viven en `src/assets/layout/`.

## Backends (`iglesia-api/` e `iglesia-seguridad/`)

### Stack
- Spring Boot: última versión estable al momento de crear el proyecto (verificarla en https://spring.io/projects/spring-boot).
- Java: la versión LTS compatible con esa versión de Spring Boot.
- **Gradle** (Kotlin DSL, con Gradle Wrapper). No usar Maven.
- PostgreSQL. Migraciones de esquema versionadas con Flyway.
- Pruebas unitarias con JUnit 5 + Mockito; pruebas de integración con Testcontainers (PostgreSQL).

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

## Comandos
- Frontend, desde `iglesia-web/`: `ng serve` / `ng build` / `ng test`
- Backends, desde `iglesia-api/` o `iglesia-seguridad/`: `./gradlew bootRun` / `./gradlew build` / `./gradlew test`
