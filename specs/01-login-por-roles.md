# SPEC 01 — Login por roles con perfil activo

> **Estado:** Aprobado
> **Depende de:** Ninguna
> **Fecha:** 2026-10-25
> **Objetivo:** Que cualquier usuario (administrador o miembro) inicie sesión en `iglesia-web`, entre a la página de inicio de su perfil y navegue solo por lo que su rol permite, con sesión renovada de forma segura.

## Por qué existe esta spec

`iglesia-seguridad` ya emite JWT RS256 (`roles`, `iglesia_id`, `miembro_id`), tiene `login`/`refresh`/`logout`, CRUD de usuarios y cambio de contraseña. `iglesia-api` ya valida el JWT y mapea `roles` a `ROLE_*`. Lo que falta es que el frontend use todo eso: hoy el login de Sakai es una demo, no hay guard, ni interceptor, ni almacenamiento del token, y cualquiera navega por todas las rutas.

Además, el token de refresco hoy viaja en el cuerpo JSON y el frontend tendría que guardarlo en `localStorage`, expuesto a XSS durante 7 días. Esta spec lo mueve a una cookie `HttpOnly`.

## Alcance

**Entra:**

- `iglesia-seguridad`: el token de refresco pasa del cuerpo JSON a una cookie `HttpOnly; Secure; SameSite=Strict` en `login`, `refresh` y `logout`.
- `iglesia-seguridad`: crear momentaneamente un usuario por cada perfil.
- `iglesia-seguridad`: CORS con credenciales (`allowCredentials=true`) para los orígenes del frontend.
- `iglesia-web`: pantalla de login real (correo + contraseña) en `core/auth/`.
- `iglesia-web`: componente reutilizable `shared/components/logo-iglesia` con la identidad neutra del sistema, usado en el login y en la topbar en lugar del logo de Sakai.
- `iglesia-web`: sesión en memoria (signals) con token de acceso, usuario actual (`/api/usuarios/me`) y perfil activo.
- `iglesia-web`: restauración de sesión al recargar la página (refresco silencioso al arrancar).
- `iglesia-web`: interceptor que agrega `Authorization: Bearer` y, ante un 401, refresca una sola vez (compartido entre peticiones simultáneas) y reintenta.
- `iglesia-web`: selector de perfil cuando el usuario tiene más de un rol; «Cambiar perfil» desde la topbar; perfil activo recordado en `sessionStorage`.
- `iglesia-web`: ruta de inicio por perfil, configurada en una constante (`RUTA_INICIO_POR_ROL`), con una página plantilla por perfil.
- `iglesia-web`: guards de autenticación, de invitado (login) y de perfil; página `/auth/acceso-denegado`.
- `iglesia-web`: menú lateral filtrado por el perfil activo.
- `iglesia-web`: menú de usuario en `app.topbar.ts` (correo, «Cambiar perfil», «Cambiar contraseña», «Cerrar sesión»). **Autorizado explícitamente** como excepción a la regla de no tocar `layout/`.
- `iglesia-web`: pantalla de cambio de contraseña usando `PUT /api/usuarios/me/contrasena` (ya existe).
- `iglesia-web`: demos de Sakai solo en desarrollo (`environment.production === false`) y detrás del login.
- `iglesia-web`: `seguridadUrl` en ambos `environment`.

**Fuera de alcance (para specs futuras):**

- Pantallas de administración de usuarios (crear, asignar roles, activar/desactivar). Los usuarios se crean con el administrador inicial y la API existente.
- Catálogos configurables (niveles y tipos de grupo). Decisiones ya tomadas para esa spec: tipos de grupo como lista por iglesia; desactivar en lugar de borrar; lectura para todos los autenticados y escritura solo `ADMIN_IGLESIA`.
- Bloqueo de cuenta por intentos fallidos.
- Recuperación de contraseña por correo («olvidé mi contraseña») y «Recordarme».
- Token con un solo rol según el perfil activo (mínimo privilegio a nivel de JWT).
- Volver a la URL original (`returnUrl`) después de iniciar sesión.
- Contenido real de las páginas de inicio de cada perfil.
- Operación de `ADMIN_SISTEMA` contra `iglesia-api` (no tiene `iglesia_id`).
- Logo y nombre reales de cada iglesia (spec 02 «Identidad de la iglesia»: almacenamiento en `iglesia-api` y su muestra en el layout después del login). Resolver la iglesia antes del login (p. ej. por subdominio).

## Modelo de datos

**Base de datos:** esta spec no agrega migraciones. Reutiliza `usuario`, `usuario_rol` y `token_refresco` de `V1__crear_tablas_seguridad.sql`. Además para hacer la ejecución de scripts y pruebas, usar el mcp disponible para la conexion al servidor de base de datos PostgreSQL tal como lo indica el CLAUDE.md del proyecto.

**Contrato HTTP de `iglesia-seguridad` (cambia):**

```text
POST /api/auth/login     body: { correo, contrasena }
  200 body:  { tokenAcceso, tipo: "Bearer", expiraEn }     ← ya no incluye tokenRefresco
      Set-Cookie: iglesia_refresco=<opaco>; Path=/api/auth; Max-Age=<duración refresco>;
                  HttpOnly; Secure; SameSite=Strict

POST /api/auth/refresh   sin body; lee la cookie iglesia_refresco
  200 body:  { tokenAcceso, tipo, expiraEn } + Set-Cookie con el token rotado
  401 sin cookie, o cookie inválida/expirada/revocada (y la cookie se borra con Max-Age=0)

POST /api/auth/logout    sin body; lee la cookie iglesia_refresco
  204 + Set-Cookie: iglesia_refresco=; Path=/api/auth; Max-Age=0; ...
```

```java
// auth/dto/TokenAccesoResponse.java (reemplaza a TokenResponse en la respuesta HTTP)
public record TokenAccesoResponse(String tokenAcceso, String tipo, long expiraEn) {}

// auth/CookieRefrescoProperties.java  → seguridad.cookie-refresco.*
public record CookieRefrescoProperties(String nombre, String ruta) {}
```

```properties
seguridad.cookie-refresco.nombre=iglesia_refresco
seguridad.cookie-refresco.ruta=${COOKIE_REFRESCO_RUTA:/api/auth}
```

- `TokenRefrescoRequest` se elimina (ya no hay cuerpo en `refresh`/`logout`).
- `Max-Age` de la cookie = `seguridad.jwt.duracion-token-refresco`.
- `Secure` siempre activo, también en `http://localhost`.

**Frontend (`iglesia-web/src/app/core/auth/`):**

```ts
// models/rol.ts
export const ROLES = ['ADMIN_SISTEMA', 'ADMIN_IGLESIA', 'LIDER', 'MIEMBRO'] as const; // orden = jerarquía
export type Rol = (typeof ROLES)[number];

export const ETIQUETA_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: 'Administrador del sistema',
    ADMIN_IGLESIA: 'Administrador de iglesia',
    LIDER: 'Líder',
    MIEMBRO: 'Miembro'
};

export const RUTA_INICIO_POR_ROL: Record<Rol, string> = {
    ADMIN_SISTEMA: '/sistema',
    ADMIN_IGLESIA: '/administracion',
    LIDER: '/mi-red',
    MIEMBRO: '/mi-perfil'
};

// models/usuario-actual.ts  (adaptado de UsuarioResponse de /api/usuarios/me)
export interface UsuarioActual {
    readonly id: number;
    readonly correo: string;
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    readonly roles: readonly Rol[];
}

// models/token-acceso.ts
export interface TokenAcceso {
    readonly tokenAcceso: string;
    readonly tipo: 'Bearer';
    readonly expiraEn: number; // segundos
}
```

Estado de `SesionService` (fachada, `providedIn: 'root'`):

```ts
private readonly token = signal<string | null>(null);
private readonly usuario = signal<UsuarioActual | null>(null);
private readonly perfil = signal<Rol | null>(null);

readonly usuarioActual = this.usuario.asReadonly();
readonly perfilActivo = this.perfil.asReadonly();
readonly autenticado = computed(() => this.token() !== null && this.usuario() !== null);
readonly tieneVariosPerfiles = computed(() => (this.usuario()?.roles.length ?? 0) > 1);
```

- Clave de `sessionStorage`: `iglesia.perfilActivo` (solo el nombre del rol; nunca tokens ni datos personales).
- Al restaurar, el perfil guardado se descarta si no está en `usuario.roles`.

**Entornos:**

```ts
// environment.development.ts
seguridadUrl: 'http://localhost:8081/api'
// environment.ts
seguridadUrl: '/seguridad/api'
```

## Plan de implementación

Cada paso deja el sistema compilando y con sus pruebas en verde.

**Backend (`iglesia-seguridad`)**

1. Crear `CookieRefrescoProperties` y las propiedades en `application.properties`. Crear el componente `CookieRefresco` en `auth/` que construye la cookie de emisión y la de borrado (`ResponseCookie` con `HttpOnly`, `Secure`, `SameSite=Strict`, `Path`, `Max-Age`). Prueba unitaria de sus atributos.
2. Cambiar `AuthService` para devolver un resultado interno con el token de acceso y el de refresco. Agregar `TokenAccesoResponse`. Cambiar `AuthController.iniciarSesion` para responder `TokenAccesoResponse` con la cabecera `Set-Cookie`. Actualizar `AuthIntegrationTest` para el login.
3. Cambiar `refresh` y `logout` para leer la cookie con `@CookieValue(required = false)`. Si falta en `refresh`, responder 401. Si el token es inválido, responder 401 y borrar la cookie. `logout` siempre responde 204 y borra la cookie. Eliminar `TokenRefrescoRequest`. Actualizar las pruebas de integración (rotación, reutilización, logout, sin cookie).
4. Activar `allowCredentials(true)` en `SeguridadHttpConfig.corsConfigurationSource`. Prueba de integración: un preflight desde `http://localhost:4200` devuelve `Access-Control-Allow-Credentials: true`.

**Frontend (`iglesia-web`)**

5. Agregar `seguridadUrl` a ambos `environment`. Crear `core/auth/models/` (`rol.ts`, `usuario-actual.ts`, `token-acceso.ts`).
6. Crear `core/auth/services/auth-api.service.ts`: `login`, `refrescar`, `cerrarSesion`, `obtenerUsuarioActual` y `cambiarContrasena` (con `withCredentials: true` en `login`, `refresh` y `logout`). Incluye un mapper de `UsuarioResponse` a `UsuarioActual`. Spec con `HttpTestingController`.
7. Crear `core/auth/services/sesion.service.ts` (fachada con signals): `iniciarSesion`, `restaurar`, `refrescar` (con una sola petición en curso compartida), `seleccionarPerfil`, `cerrarSesion` y `rutaInicio()`. Persiste el perfil en `sessionStorage` con lectura y escritura dentro de `try/catch`. Spec.
8. Crear `core/interceptors/auth.interceptor.ts`. Agrega el Bearer solo a URLs que empiecen por `apiUrl` o `seguridadUrl`. Ante un 401 que no venga de `/auth/*`, refresca una vez y reintenta. Si el refresco falla, cierra la sesión y navega a `/auth/login?motivo=sesion-expirada`. Registrarlo en `app.config.ts` con `withInterceptors`. Agregar `provideAppInitializer(() => inject(SesionService).restaurar())`. Spec.
9. Crear `core/auth/guards/auth.guards.ts`:
   - `autenticadoGuard`: sin sesión, redirige a `/auth/login`.
   - `invitadoGuard`: con sesión, redirige a la ruta de inicio.
   - `perfilGuard(...roles)`: si el perfil activo no está en `roles`, redirige a `/auth/acceso-denegado`.
   - `perfilSeleccionadoGuard`: sin perfil activo, redirige a `/auth/seleccionar-perfil`.
   - `inicioRedirect`: redirige `''` a `RUTA_INICIO_POR_ROL[perfilActivo]`.

   Spec.
10. Crear `core/auth/pages/login.ts`. Formulario reactivo tipado con `correo` (`required`, `email`, `maxLength(254)`) y `contrasena` (`required`, `maxLength(72)`). Botón con estado de carga. Mensajes:
    - 401: «Correo o contraseña incorrectos».
    - Cualquier otro error: «No se pudo iniciar sesión. Intenta de nuevo.».
    - `motivo=sesion-expirada`: «Tu sesión expiró. Inicia sesión de nuevo.».

    Sin «Recordarme» ni «olvidé mi contraseña». Con un rol, navega al inicio. Con varios, navega a `/auth/seleccionar-perfil`. Estructura visual del login de Sakai con `app-logo-iglesia` (orientación vertical) en lugar de su logo. Crear `shared/components/logo-iglesia/` con la identidad neutra del sistema. Specs.
11. Crear `core/auth/pages/seleccionar-perfil.ts`: una tarjeta por rol del usuario con `ETIQUETA_ROL`. Al elegir, guarda el perfil y navega a su inicio. Crear `core/auth/pages/acceso-denegado.ts`. Crear `core/auth/auth.routes.ts` (`login`, `seleccionar-perfil`, `acceso-denegado`) y reemplazar con él la ruta `auth` de `app.routes.ts`. Specs.
12. Crear `features/inicio/` con `pages/inicio-sistema.ts`, `inicio-administracion.ts`, `inicio-lider.ts` e `inicio-miembro.ts`. Cada una es una plantilla con título, saludo con el correo y la etiqueta del perfil. Crear `inicio.routes.ts` con cada ruta protegida por `perfilGuard`. Specs.
13. Reestructurar `app.routes.ts`:
    - `AppLayout` protegido por `autenticadoGuard` y `perfilSeleccionadoGuard`.
    - `''` usa `inicioRedirect`.
    - Rutas de inicio cargadas desde `inicio.routes.ts`.
    - `miembros` con `perfilGuard('ADMIN_IGLESIA')`.
    - `cuenta/contrasena`.
    - Rutas demo (`dashboard`, `uikit`, `pages`, `documentation`, `landing`) solo si `!environment.production`.
    - `/auth/login` con `invitadoGuard`.
14. Crear `core/auth/pages/cambiar-contrasena.ts` con la contraseña actual, la nueva y su confirmación, validando la coincidencia en el `FormGroup`. Si el cambio sale bien, el backend revoca todas las sesiones: se cierra la sesión local y se navega a `/auth/login?motivo=contrasena-cambiada` con el aviso «Contraseña actualizada. Inicia sesión de nuevo.». Si responde 400, se muestra el `detail` del `ProblemDetail`. Spec.
15. Reescribir `app.menu.ts`: el modelo se calcula con `computed` a partir del perfil activo. Cada entrada declara sus `roles`. Hay una entrada «Inicio» que apunta a la ruta del perfil y «Miembros» para `ADMIN_IGLESIA`. Los grupos demo de Sakai solo aparecen si `!environment.production`. Spec.
16. Modificar `app.topbar.ts`: reemplazar el logo de Sakai por `app-logo-iglesia` y los botones demo (calendario, mensajes, perfil) por un menú de usuario de PrimeNG. Muestra el correo y la etiqueta del perfil activo, y tiene las opciones «Cambiar perfil» (solo si `tieneVariosPerfiles()`), «Cambiar contraseña» y «Cerrar sesión». No se toca nada más de `layout/`. Spec.

## Criterios de aceptación

**Backend**

- [ ] `POST /api/auth/login` con credenciales válidas responde 200, sin `tokenRefresco` en el cuerpo, y con `Set-Cookie: iglesia_refresco=...` que incluye `HttpOnly`, `Secure`, `SameSite=Strict` y `Path=/api/auth`.
- [ ] `POST /api/auth/refresh` con la cookie válida responde 200 con un nuevo `tokenAcceso` y una cookie con un valor distinto (rotación).
- [ ] Reutilizar una cookie de refresco ya rotada responde 401.
- [ ] `POST /api/auth/refresh` sin cookie responde 401.
- [ ] `POST /api/auth/logout` responde 204 con la cookie borrada (`Max-Age=0`), y un `refresh` posterior con el valor anterior responde 401.
- [ ] Un preflight CORS desde `http://localhost:4200` devuelve `Access-Control-Allow-Credentials: true`.
- [ ] `./gradlew test` en `iglesia-seguridad` pasa.

**Frontend**

- [ ] Entrar a cualquier ruta sin sesión redirige a `/auth/login`.
- [ ] Con credenciales incorrectas se ve «Correo o contraseña incorrectos» y no se navega.
- [ ] Un usuario con solo `MIEMBRO` llega a `/mi-perfil` justo después del login.
- [ ] Un usuario con solo `ADMIN_SISTEMA` llega a `/sistema`, uno con solo `ADMIN_IGLESIA` a `/administracion` y uno con solo `LIDER` a `/mi-red`.
- [ ] Un usuario con `LIDER` y `MIEMBRO` ve `/auth/seleccionar-perfil` con dos opciones. Al elegir «Líder» llega a `/mi-red`.
- [ ] Con el perfil «Miembro», navegar a mano a `/administracion` redirige a `/auth/acceso-denegado`.
- [ ] El menú lateral con el perfil «Miembro» no muestra «Miembros».
- [ ] «Cambiar perfil» en la topbar solo aparece para usuarios con más de un rol y lleva al selector.
- [ ] Recargar la página (F5) con sesión iniciada mantiene la sesión y el perfil activo, sin pasar por el login.
- [ ] Ni el token de acceso ni el de refresco aparecen en `localStorage` ni en `sessionStorage`. `sessionStorage` solo contiene `iglesia.perfilActivo`.
- [ ] La cookie `iglesia_refresco` no es accesible desde `document.cookie`.
- [ ] Con un token de acceso vencido, la siguiente petición a `iglesia-api` se completa con éxito después de un único `POST /refresh`, aunque se hayan lanzado varias peticiones a la vez.
- [ ] Si el refresco falla, se llega a `/auth/login` con el aviso «Tu sesión expiró. Inicia sesión de nuevo.».
- [ ] «Cerrar sesión» llama a `/api/auth/logout`, limpia el estado y lleva a `/auth/login`. Luego, el botón «atrás» no muestra páginas protegidas.
- [ ] Cambiar la contraseña con la actual incorrecta muestra el mensaje del backend. Con la correcta, lleva al login con «Contraseña actualizada. Inicia sesión de nuevo.».
- [ ] Con un build de producción (`ng build`), las rutas `/uikit`, `/pages`, `/documentation`, `/landing` y `/dashboard` no existen (redirigen a `/notfound`) y el menú no muestra los grupos demo.
- [ ] El interceptor no agrega `Authorization` a URLs fuera de `apiUrl` y `seguridadUrl`.
- [ ] Cada pieza nueva (servicios, interceptor, guards, páginas) tiene su `.spec.ts` y `ng test` pasa.
- [ ] El login y la topbar muestran `app-logo-iglesia` y ya no aparece el logo de Sakai.
- [ ] No hay cambios en `src/app/layout/` fuera de `app.menu.ts` y `app.topbar.ts`.

## Decisiones

- **Sí:** token de acceso solo en memoria (signal) y token de refresco en cookie `HttpOnly; Secure; SameSite=Strict`. Un XSS no puede leer el token de 7 días.
- **No:** guardar los tokens en `localStorage` o `sessionStorage`. Es lo más simple, pero expone el token de refresco a XSS.
- **Sí:** `Path=/api/auth` en la cookie. Solo viaja a `login`, `refresh` y `logout`, no a cada petición. La ruta se puede configurar con `COOKIE_REFRESCO_RUTA` por si un proxy inverso cambia el prefijo.
- **Sí:** `Secure` siempre, incluso en local. Los navegadores aceptan `Secure` en `http://localhost` y así no hay una bandera que se olvide en producción.
- **No:** propiedad para desactivar `Secure`. Existe el riesgo de que quede desactivada en un entorno real.
- **Sí:** mantener CSRF desactivado. La cookie es `SameSite=Strict` y solo se acepta en `refresh`/`logout`, que no cambian datos de negocio. El resto de la API se autentica con Bearer, que el navegador no envía solo.
- **Sí:** los roles y el correo del usuario se obtienen de `GET /api/usuarios/me` y no decodificando el JWT en el frontend. El backend es la fuente de verdad y el frontend no depende del formato del token.
- **Sí:** el perfil activo limita solo la navegación (ruta de inicio, menú y guards). El JWT conserva todos los roles y el backend autoriza con ellos.
- **No:** emitir un token con un solo rol según el perfil. Es más seguro, pero cambia el login, el refresco y `token_refresco`. Queda para una spec futura si hace falta.
- **Sí:** el usuario con varios roles elige su perfil al entrar y puede cambiarlo desde la topbar. El perfil se recuerda en `sessionStorage` porque no es un dato sensible, y se valida contra los roles al restaurarlo.
- **Sí:** las rutas de inicio se definen en la constante `RUTA_INICIO_POR_ROL` del frontend. Es navegación de la UI y no debe vivir en el backend de seguridad.
- **Sí:** el orden de `ROLES` define la jerarquía (el primero es el mayor), para ordenar las opciones del selector de perfil.
- **Sí:** las páginas de inicio van en `features/inicio/` y la autenticación en `core/auth/`. Un feature por perfil sería prematuro con páginas plantilla.
- **Sí:** refresco silencioso ante un 401, con una única petición de refresco compartida, más un refresco al arrancar la app para sobrevivir a F5.
- **No:** refresco programado con temporizador antes de que venza el token. Agrega estado y complejidad sin una ganancia clara frente al reintento ante 401.
- **Sí:** al cambiar la contraseña se cierra la sesión local y se vuelve al login. El backend ya revoca todas las sesiones (`revocarTodos`).
- **Sí:** modificar `app.topbar.ts` para el menú de usuario. El usuario lo autorizó explícitamente como excepción a la regla de no tocar `layout/`.
- **Sí:** demos de Sakai solo en desarrollo y detrás del login. Siguen sirviendo de referencia sin llegar a producción.
- **Sí:** «Miembros» (feature ya existente) visible y accesible solo con el perfil `ADMIN_IGLESIA` hasta que su propia spec defina los permisos de `LIDER`.
- **No:** pantallas de administración de usuarios, bloqueo por intentos fallidos ni recuperación de contraseña. Cada uno tendrá su propia spec.
- **Sí:** logo y nombre centralizados en `app-logo-iglesia`. En esta spec muestra la identidad neutra del sistema; la spec 02 lo conecta con la iglesia del usuario sin tocar el login ni la topbar.
- **Sí:** el login muestra la identidad neutra: antes de autenticarse no se conoce la iglesia. Resolverla por subdominio queda para una spec futura.
- **No:** `returnUrl` después del login. Por YAGNI: con las páginas de inicio plantilla todavía no aporta.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| En producción el frontend y `iglesia-seguridad` quedan en sitios distintos (otro dominio registrable) y `SameSite=Strict` bloquea la cookie. | Servir ambos bajo el mismo sitio, por ejemplo con un proxy inverso en `/seguridad/api`. `COOKIE_REFRESCO_RUTA` permite ajustar el `Path` al prefijo del proxy. |
| Varias peticiones reciben 401 a la vez y disparan varios `refresh`. Con la rotación, el segundo uso del token se detecta como reutilización y revoca la sesión. | `SesionService.refrescar()` comparte una sola petición en curso (`shareReplay`/promesa única) y las demás esperan su resultado. Hay una prueba unitaria específica. |
| Dos pestañas abiertas refrescan a la vez con la misma cookie y una de ellas cae en reutilización. | Se acepta en esta spec: la pestaña afectada vuelve al login. Si molesta, una spec futura puede coordinar pestañas con `BroadcastChannel`. |
| `sessionStorage` bloqueado (modo privado o sitios bloqueados) | Lectura y escritura en `try/catch`. Sin almacenamiento, el usuario con varios roles vuelve a elegir perfil al recargar. |
| El `ADMIN_SISTEMA` no tiene `iglesia_id` y cualquier llamada a `iglesia-api` fallará. | Su página de inicio es una plantilla sin llamadas a `iglesia-api`. Su operación entre iglesias queda fuera de alcance. |

## Qué **no** entra en esta spec

- Administración de usuarios desde la web.
- Catálogos configurables (niveles y tipos de grupo).
- Bloqueo por intentos fallidos, «olvidé mi contraseña» y «Recordarme».
- JWT con un solo rol según el perfil activo.
- `returnUrl` después del login.
- Contenido real de las páginas de inicio.
- Operaciones de `ADMIN_SISTEMA` sobre `iglesia-api`.

Cada uno, si llega, va en su propia spec.
