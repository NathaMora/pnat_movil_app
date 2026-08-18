# PNAT Mobile — Arquitectura técnica

## 1. Objetivo

`pnat_mobile` es una aplicación Android nativa en Kotlin para RaySharkApp / PNAT.

La aplicación móvil no reemplaza ni duplica el backend. Funciona como un cliente adicional de la API REST existente de `pnat_backend`, igual que el frontend web Angular, pero adaptado a Android y con funcionamiento offline-first.

El alcance inicial de la aplicación móvil es:

1. Presentación del proyecto.
2. Envío de reportes de avistamiento.
3. My Reports.
4. Login cuando una función requiera una sesión autenticada.

Quedan fuera de esta primera versión:

- Scientists Desk;
- administración;
- auditoría;
- Photo-ID;
- dashboard científico;
- trivia;
- edición científica avanzada;
- gestión administrativa de usuarios y afiliaciones.

---

## 2. Principios arquitectónicos

La arquitectura debe priorizar:

- funcionamiento sin conexión;
- conservación de la información del usuario;
- sincronización reanudable;
- separación de responsabilidades;
- compatibilidad con la API actual;
- código Android nativo mantenible;
- identidad visual coherente con la aplicación web.

La regla principal es:

`UI -> ViewModel -> Repository -> Room / API`

La UI nunca debe llamar directamente a Retrofit, Room o WorkManager.

---

## 3. Arquitectura general

```text
┌─────────────────────────────────────────────┐
│                PNAT Mobile                  │
│          Kotlin + Jetpack Compose           │
├─────────────────────────────────────────────┤
│ UI                                          │
│ Home | Send Report | My Reports | Login     │
├─────────────────────────────────────────────┤
│ ViewModels                                  │
│ StateFlow + Coroutines                      │
├─────────────────────────────────────────────┤
│ Repositories                                │
│ Coordinan datos locales y remotos           │
├──────────────────────┬──────────────────────┤
│ Local                │ Remote               │
│ Room                 │ Retrofit + OkHttp    │
│ archivos locales     │ REST API             │
├──────────────────────┴──────────────────────┤
│ WorkManager                                 │
│ sincronización en segundo plano             │
└──────────────────────┬──────────────────────┘
                       │ HTTPS
                       ▼
┌─────────────────────────────────────────────┐
│              pnat_backend                   │
│          Spring Boot REST API               │
├─────────────────────────────────────────────┤
│ PostgreSQL + almacenamiento multimedia      │
└─────────────────────────────────────────────┘
```

---

## 4. Stack técnico

Usar:

- Kotlin;
- Jetpack Compose;
- Material 3;
- Navigation Compose;
- Android ViewModel;
- Kotlin Coroutines;
- StateFlow;
- Retrofit;
- OkHttp;
- Room;
- WorkManager;
- Coil para imágenes;
- Android Photo Picker o API Android moderna equivalente.

No usar WebView para incrustar Angular.

No crear nuevos layouts XML salvo una necesidad técnica explícita y documentada.

---

## 5. Organización recomendada del código

Ubicación aproximada:

```text
app/src/main/java/.../pnat/
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── database/
│   │   └── mapper/
│   │
│   ├── remote/
│   │   ├── api/
│   │   ├── dto/
│   │   ├── auth/
│   │   └── mapper/
│   │
│   └── repository/
│
├── domain/
│   └── model/
│
├── sync/
│   ├── ReportSyncWorker.kt
│   ├── SyncScheduler.kt
│   └── SyncState.kt
│
├── ui/
│   ├── home/
│   ├── report/
│   ├── myreports/
│   ├── auth/
│   ├── components/
│   └── theme/
│
├── navigation/
│
├── util/
│
└── MainActivity.kt
```

La estructura exacta puede ajustarse si el proyecto generado por Android Studio lo requiere, pero debe mantenerse la separación conceptual.

---

## 6. Capas

### 6.1 UI

Responsabilidad:

- renderizar estado;
- recibir interacción;
- mostrar validaciones;
- navegar;
- mostrar estados offline/sincronización.

Tecnología:

- Jetpack Compose;
- Material 3.

La UI no debe:

- ejecutar SQL;
- llamar endpoints;
- calcular reglas de sincronización;
- gestionar cookies directamente;
- leer/escribir archivos de negocio directamente.

---

### 6.2 ViewModel

Responsabilidad:

- exponer `StateFlow`;
- recibir eventos de UI;
- coordinar repositories;
- transformar datos de dominio a estado de pantalla;
- mantener lógica de presentación.

Los ViewModels no deben conocer detalles SQL ni construir requests HTTP manualmente.

---

### 6.3 Repository

Los repositories son el punto de coordinación entre datos locales y remotos.

Ejemplos:

- `AuthRepository`
- `CatalogRepository`
- `ReportRepository`
- `MyReportsRepository`

Responsabilidades:

- decidir cuándo leer Room;
- decidir cuándo refrescar API;
- persistir datos;
- entregar modelos coherentes a ViewModels;
- ocultar detalles Retrofit/DAO.

Para reportes offline, Room es la fuente de verdad mientras el reporte no esté completamente sincronizado.

---

## 7. Capa local

### 7.1 Room

Room conserva información estructurada necesaria para:

- borradores;
- reportes pendientes;
- progreso de sincronización;
- multimedia;
- observaciones;
- asociaciones observación-media;
- catálogos;
- caché razonable de My Reports.

Entidades mínimas sugeridas:

```text
LocalReport
LocalMedia
LocalObservation
LocalObservationMedia
CachedOcean
CachedDestination
CachedSightingSpot
CachedGenus
CachedSpecies
CachedBehavior
CachedRemoteReport
```

No es obligatorio utilizar exactamente estos nombres, pero sí representar estas responsabilidades.

---

### 7.2 Archivos multimedia locales

No guardar fotografías o videos como BLOB dentro de Room.

Guardar:

- UUID local;
- ruta/URI durable;
- nombre original;
- MIME type;
- tipo photo/video;
- SHA-512;
- estado de upload.

Cuando sea necesario, copiar el archivo seleccionado a almacenamiento controlado por la aplicación para asegurar que continúe disponible después de cerrar la app.

---

## 8. Capa remota

### 8.1 Retrofit

Los endpoints deben estar centralizados por responsabilidad.

Ejemplo conceptual:

```text
AuthApi
CatalogApi
AuthenticatedReportApi
PublicReportApi
MediaApi
MyReportsApi
```

No construir URLs dentro de composables o ViewModels.

La base URL debe venir de configuración centralizada.

---

### 8.2 Contratos actuales relevantes

La aplicación móvil debe adaptarse a los contratos existentes del backend.

### Autenticación

```text
POST /auth/login
GET  /auth/session
POST /auth/logout
```

El backend administra el JWT mediante cookie HTTP.

### Reporte autenticado

```text
POST /api/reports
```

El backend devuelve un `reportId`.

### My Reports

```text
GET /api/reports/my-contributions
```

Requiere usuario autenticado.

### Multimedia autenticada

```text
GET  /api/media/check-hash
POST /api/media/upload
```

La subida utiliza:

- file;
- hash;
- reportId;
- mediaType.

### Observaciones autenticadas

```text
POST /api/observations/batch
```

Las observaciones se asocian con multimedia mediante `mediaHashes`.

### Reporte invitado

```text
POST /api/public-reports
POST /api/public-reports/{reportId}/media
POST /api/public-reports/{reportId}/observations
```

### Catálogos

La API existente expone catálogos públicos para:

- ubicaciones;
- taxonomía;
- comportamientos.

Codex debe inspeccionar los contratos reales antes de crear DTOs. No inventar campos.

---

## 9. Autenticación

El backend actual guarda el JWT en una cookie HTTP.

Android debe manejar la sesión mediante OkHttp y una estrategia de cookies compatible.

No almacenar manualmente el JWT en:

- Room;
- DataStore;
- SharedPreferences;
- archivos;
- logs.

La aplicación solo necesita conocer información funcional como:

- sesión autenticada;
- rol cuando el backend lo devuelva.

Si My Reports se abre sin sesión:

```text
My Reports
   ↓
Session check
   ↓
No autenticado
   ↓
Login
   ↓
Login correcto
   ↓
My Reports
```

---

## 10. Arquitectura offline-first

Todo reporte terminado debe pasar por Room antes de la red.

Flujo obligatorio:

```text
Formulario
   ↓
Validación
   ↓
Transacción Room
   ↓
PENDING
   ↓
WorkManager
   ↓
Retrofit
   ↓
Backend
   ↓
SYNCED
```

No usar:

```text
si hay Internet -> enviar directamente
si no hay Internet -> guardar
```

La aplicación debe usar un único flujo consistente.

---

## 11. Estados de sincronización

Estados mínimos:

```text
DRAFT
PENDING
SYNCING
SYNCED
ERROR
```

Significado:

### DRAFT

Reporte todavía editable.

### PENDING

Reporte finalizado y persistido, pendiente de sincronización.

### SYNCING

Existe una ejecución activa intentando enviar el reporte.

### SYNCED

Todos los pasos requeridos fueron confirmados por el backend.

### ERROR

Existe un problema que impide completar temporal o permanentemente la sincronización.

Los detalles de la máquina de estados se documentan en:

`docs/offline-sync.md`

---

## 12. WorkManager

La sincronización en background usa WorkManager.

Restricción:

```text
NetworkType.CONNECTED
```

No usar polling constante.

Debe existir un trabajo único o mecanismo equivalente que evite dos Workers procesando simultáneamente el mismo reporte.

La sincronización debe seguir funcionando aunque el usuario cierre la app.

---

## 13. Catálogos offline

Los catálogos necesarios para completar el formulario deben almacenarse localmente.

Regla:

```text
API responde correctamente
        ↓
actualizar Room
        ↓
UI lee Room
```

Si una actualización remota falla:

- conservar la última versión válida;
- no vaciar el catálogo.

Si es la primera ejecución y nunca se descargaron catálogos, la app puede requerir una primera conexión.

---

## 14. My Reports

My Reports combina:

1. reportes locales;
2. contribuciones remotas del usuario;
3. caché local de resultados remotos cuando exista.

Los reportes locales deben aparecer inmediatamente, incluso sin conexión.

Un reporte con `serverReportId` conocido no debe mostrarse duplicado si ya está presente en los resultados remotos.

---

## 15. Manejo de errores

La arquitectura debe diferenciar:

### Error temporal

Ejemplos:

- sin Internet;
- timeout;
- backend temporalmente indisponible.

Acción:

- conservar datos;
- reintentar.

### Autenticación

HTTP 401/403 relacionado con sesión.

Acción:

- conservar datos;
- solicitar login;
- continuar después.

### Validación

HTTP 400 de negocio o payload.

Acción:

- no reintentar indefinidamente;
- conservar reporte;
- mostrar error corregible.

---

## 16. Seguridad

Nunca almacenar:

- contraseñas;
- secretos;
- tokens JWT;
- cookies en logs;
- claves de producción.

No desactivar validación TLS.

No utilizar certificados “trust all”.

No hardcodear URLs productivas por toda la aplicación.

---

## 17. Configuración de entornos

Centralizar la base URL mediante un mecanismo de build, por ejemplo:

- `BuildConfig`;
- Gradle properties;
- flavors si posteriormente son necesarios.

Las capas UI y dominio no deben conocer URLs concretas.

---

## 18. Dependencias entre módulos funcionales

```text
Home
 ├── no requiere conexión para render básico
 ├── abre Send Report
 └── abre My Reports

Send Report
 ├── CatalogRepository
 ├── ReportRepository
 ├── Room
 ├── almacenamiento multimedia local
 └── SyncScheduler

My Reports
 ├── AuthRepository
 ├── MyReportsRepository
 ├── Room
 └── API remota

Login
 └── AuthRepository
```

---

## 19. Pruebas prioritarias

La arquitectura debe permitir probar componentes sin depender de producción.

Prioridades:

- mappers;
- Room;
- SHA-512;
- máquina de sincronización;
- Workers;
- Retrofit con MockWebServer;
- reanudación después de fallo parcial;
- pérdida de autenticación.

---

## 20. Regla de evolución

Si el backend cambia, actualizar primero:

1. DTO remoto;
2. mapper;
3. repository;
4. tests;
5. documentación.

Evitar propagar estructuras de API directamente hasta la UI.

---

## 21. Documentos relacionados

Este documento debe leerse junto con:

- `AGENTS.md`
- `docs/design-guide.md`
- `docs/offline-sync.md`
- `docs/backend-idempotency.md`

`AGENTS.md` contiene las reglas obligatorias de trabajo.

`architecture.md` describe cómo se organiza técnicamente la aplicación.

`offline-sync.md` define exactamente cómo debe comportarse la sincronización.

`backend-idempotency.md` documenta una limitación conocida del contrato actual del backend.
