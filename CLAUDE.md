# Sistema de Gestión – Iglesia

## Stack
- Angular 21 (standalone, signals, control flow @if/@for)
- PrimeNG 21 + Tailwind 4 (base: plantilla Sakai)
- Backend: Spring Boot REST + PostgreSQL (repo aparte)

## Reglas
- Todo siempre en español: respuestas, comentarios de código, commits, PRs y documentación.
- NO modificar src/app/layout/ salvo que se pida explícitamente.
- Cada módulo va en src/app/features/<modulo>/ (pages, components, services, models).
- Usar componentes PrimeNG y utilidades Tailwind. Evitar CSS propio.
- Antes de crear una pantalla, revisar los ejemplos en src/app/pages/uikit/.
- Formularios reactivos tipados. Servicios con HttpClient y URL base desde environment.
- Textos de la interfaz en español.
- Documentación PrimeNG: https://primeng.org/llms/llms.txt

## Estructura (`iglesia-web/src/`)
- `environments/`: `environment.ts` (producción, `apiUrl: '/api'`) y `environment.development.ts` (`http://localhost:8080/api`), reemplazado vía `fileReplacements`.
- `app/layout/`: shell de Sakai (no tocar; solo `app.menu.ts` para agregar módulos al menú).
- `app/pages/`: demos de Sakai, solo como referencia.
- `app/core/`: piezas únicas de la app (`auth/`, `interceptors/`, `models/` globales como `Page<T>`).
- `app/shared/`: reutilizable entre módulos (`components/`, `pipes/`, `utils/`).
- `app/features/<modulo>/`: `pages/`, `components/`, `services/`, `models/` y `<modulo>.routes.ts` con `export default` de `Routes`, registrado con `loadChildren` como hijo de `AppLayout` en `src/app.routes.ts`. Ejemplo: `features/miembros/`.
- Archivos sin sufijo `.component` y plantillas inline. Servicios de features con `providedIn: 'root'`.

## Notas importantes
- **App zoneless** (`provideZonelessChangeDetection`): el estado que controla la UI debe usar signals. La detección de cambios no detecta mutaciones simples de campos hechas desde callbacks asíncronos.
- **Estilos globales:** `src/assets/` (styles.scss, tailwind.css, layout/, demo/) viene copiado del repo `cetincakiroglu/sakai-assets` y está versionado directamente en este repo (ya no es submódulo). Los estilos del layout viven en `src/assets/layout/`.

## Comandos
- Ejecutar desde `iglesia-web/`: ng serve / ng build / ng test
