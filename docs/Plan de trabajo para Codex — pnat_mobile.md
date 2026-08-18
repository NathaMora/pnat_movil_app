# Plan de trabajo: `pnat_mobile`

## 1. Objetivo

Construir una aplicación Android nativa independiente para RaySharkApp/PNAT.

El repositorio debe llamarse:

`pnat_mobile`

La aplicación móvil será un cliente de la API REST existente de `pnat_backend`.

No duplicar lógica del backend y no copiar la aplicación Angular.

La primera versión tendrá únicamente estas funciones principales:

1. Presentación del proyecto.
2. Envío de reportes de avistamiento.
3. My Reports.
4. Autenticación únicamente cuando sea necesaria para funciones asociadas al usuario.

La aplicación debe funcionar en condiciones de conectividad limitada.

El requisito principal es:

> Todo reporte terminado debe almacenarse primero de forma segura en el dispositivo. Si no existe conexión, debe permanecer pendiente. Cuando vuelva la conexión, la aplicación debe intentar enviarlo automáticamente al backend sin que el usuario tenga que volver a introducir la información.

---

# 2. Restricciones de ejecución en el sandbox

Trabajar únicamente dentro del repositorio `pnat_mobile`.

No modificar `pnat_backend`, `pnat_front`, PostgreSQL ni archivos externos al repositorio.

No realizar operaciones contra producción.

No introducir:

- contraseñas;
- JWT;
- secretos;
- credenciales;
- direcciones privadas;
- datos personales reales.

No hacer commits automáticamente salvo que se solicite.

No borrar archivos existentes sin justificarlo.

Antes de modificar código:

1. inspeccionar la estructura completa del repositorio;
2. revisar `git status`;
3. identificar versión de Gradle, Kotlin y Android Gradle Plugin;
4. comprobar si existe Android SDK en el sandbox;
5. comprobar si el proyecto compila antes de modificarlo, si el entorno lo permite.

Si el sandbox no dispone del Android SDK o no puede descargar dependencias, continuar con la implementación estructural pero indicar claramente qué validaciones no pudieron ejecutarse.

No afirmar que una compilación o prueba pasó si no fue ejecutada realmente.

---

# 3. Stack técnico

Usar:

- Kotlin;
- Jetpack Compose;
- Material 3;
- Navigation Compose;
- ViewModel;
- Kotlin Coroutines;
- StateFlow;
- Retrofit;
- OkHttp;
- Kotlin Serialization o el mecanismo de serialización que resulte más coherente con el proyecto generado;
- Room;
- WorkManager;
- Coil para visualización de imágenes;
- Android Photo Picker / APIs modernas de selección de multimedia.

Evitar XML para las nuevas pantallas salvo que el proyecto inicial ya dependa justificadamente de XML.

No crear una arquitectura innecesariamente compleja.

---

# 4. Arquitectura inicial

Organizar la aplicación aproximadamente así:

`app/src/main/java/.../pnat/`

- `data/`
  - `local/`
  - `remote/`
  - `repository/`
- `domain/`
  - `model/`
- `sync/`
- `ui/`
  - `home/`
  - `report/`
  - `myreports/`
  - `auth/`
  - `components/`
  - `theme/`
- `navigation/`
- `util/`

Separar claramente:

- DTO de API;
- entidades Room;
- modelos usados por UI;
- mappers entre ellos.

No usar entidades Room directamente como DTO de Retrofit.

---

# 5. Navegación principal

Implementar una navegación pequeña.

Pantallas principales:

### Home

Presentación del proyecto PNAT/RaySharkApp.

Debe ofrecer acceso a:

- Enviar reporte.
- My Reports.

### Send Report

Formulario móvil de avistamientos.

### My Reports

Lista de reportes del usuario y reportes locales pendientes.

### Login

Pantalla secundaria.

Debe aparecer cuando una funcionalidad requiera autenticación.

No convertir Login en el centro de la aplicación.

---

# 6. Capa de red

Crear una configuración única de Retrofit/OkHttp.

La URL base nunca debe escribirse directamente dentro de composables, ViewModels o repositories.

Preparar configuración diferenciable para desarrollo y producción mediante `BuildConfig`, Gradle properties o mecanismo equivalente seguro.

Nunca almacenar secretos en el repositorio.

Crear interfaces Retrofit para los contratos existentes del backend.

## Catálogos públicos

Consumir los endpoints existentes para:

- océanos;
- destinos;
- puntos de avistamiento;
- taxonomía;
- comportamientos.

Los catálogos deben poder almacenarse localmente para permitir completar formularios sin conexión después de haber sido descargados al menos una vez.

## Reportes invitados

Modelar:

- creación del reporte;
- subida multimedia;
- creación de observaciones.

## Reportes autenticados

Modelar:

- creación del reporte;
- subida multimedia;
- creación batch de observaciones.

## My Reports

Modelar:

- obtención paginada de contribuciones del usuario.

## Multimedia

Modelar:

- comprobación del hash;
- upload multipart;
- descarga/visualización de media cuando sea necesaria.

No inventar contratos API. Deben corresponder al backend actual.

---

# 7. Autenticación

El backend actual utiliza JWT almacenado en una cookie HTTP.

La aplicación Android no debe extraer el JWT para utilizarlo manualmente.

Implementar persistencia de cookies mediante OkHttp de forma compatible con:

- `POST /auth/login`;
- `GET /auth/session`;
- `POST /auth/logout`.

La cookie debe ser administrada por la capa HTTP.

No almacenar JWT en:

- SharedPreferences;
- DataStore;
- Room;
- archivos;
- logs.

Crear un `AuthRepository` que exponga solamente el estado funcional necesario:

- autenticado;
- no autenticado;
- rol cuando el servidor lo proporcione.

My Reports requiere sesión autenticada.

Si el usuario intenta abrir My Reports sin sesión:

- mostrar Login;
- al iniciar sesión correctamente volver a My Reports.

---

# 8. Diseño offline-first

Esta es una condición obligatoria.

No implementar:

`si hay Internet → enviar directamente`

`si no hay Internet → guardar`

Implementar siempre:

`formulario → persistencia local → cola de sincronización → backend`

Room debe ser la fuente de verdad mientras un reporte esté pendiente de sincronización.

Crear estados como mínimo:

- `DRAFT`
- `PENDING`
- `SYNCING`
- `SYNCED`
- `ERROR`

Un reporte nunca debe desaparecer de Room simplemente porque ocurrió un error de red.

---

# 9. Esquema local de Room

Crear entidades suficientes para representar un reporte completo.

Como mínimo:

### LocalReport

Debe poder conservar:

- UUID local generado por la app;
- tipo de reporte: registrado o invitado;
- datos del participante cuando corresponda;
- día;
- mes;
- año;
- oceanId;
- destinationId;
- sightingSpotId;
- participantMessage;
- estado de sincronización;
- `serverReportId` nullable;
- fecha de creación local;
- fecha de última modificación;
- último error de sincronización si existe.

### LocalMedia

Conservar:

- UUID local;
- reportLocalId;
- URI/ruta controlada por la aplicación;
- nombre original;
- MIME type;
- photo/video;
- hash SHA-512;
- estado de upload;
- indicador de que el backend confirmó la carga.

### LocalObservation

Conservar:

- UUID local;
- reportLocalId;
- genusId;
- speciesId;
- behaviorId;
- count.

### Relación observación-media

Conservar qué archivos pertenecen a cada observación.

No almacenar archivos binarios grandes dentro de Room.

Room almacena metadatos y rutas/URIs controladas.

---

# 10. Manejo seguro de fotos y videos

Cuando el usuario seleccione una fotografía o video:

1. obtener acceso mediante APIs Android apropiadas;
2. conservar una copia local controlada por la aplicación cuando sea necesario para garantizar que el archivo siga disponible durante una sincronización posterior;
3. calcular SHA-512;
4. guardar el hash y metadatos en Room;
5. generar preview;
6. relacionar el archivo con el reporte local.

El archivo pendiente no debe depender exclusivamente de un URI temporal que pueda dejar de ser accesible después de cerrar la aplicación.

No borrar el archivo local mientras su upload no esté confirmado.

Después de una sincronización completa se podrá aplicar una política posterior de limpieza de caché, pero no eliminar automáticamente evidencias necesarias sin una decisión explícita.

---

# 11. Formulario Send Report

Reproducir únicamente el flujo funcional necesario de la aplicación web.

Debe permitir:

- fecha del avistamiento;
- océano;
- destino;
- punto de avistamiento;
- fotografías;
- videos;
- género opcional;
- especie opcional;
- comportamiento opcional;
- número de individuos opcional;
- mensaje/comentario.

Para invitado incluir los campos exigidos por el backend:

- nombre;
- apellido;
- email;
- celular;
- consentimiento de datos;
- permiso de contacto.

No inventar campos obligatorios distintos a los contratos actuales del backend.

Permitir que género y especie permanezcan sin identificar cuando el backend lo permita.

Mantener las relaciones entre media y observaciones.

Antes de considerar el reporte "terminado":

- validar los campos requeridos;
- persistir todos los datos en una transacción Room;
- marcar `PENDING`;
- programar sincronización.

Mostrar al usuario:

`Reporte guardado. Se enviará automáticamente cuando haya conexión.`

Esto debe ocurrir incluso si en ese instante sí existe conexión.

---

# 12. SHA-512

Implementar SHA-512 de los archivos antes de subirlos.

El hash debe calcularse mediante streaming.

No cargar fotografías o videos completos en memoria para calcular el hash.

El resultado debe ser hexadecimal de 128 caracteres y compatible con el backend actual.

Usar ese mismo hash posteriormente en:

- comprobación de duplicado;
- upload;
- asociación de observaciones mediante `mediaHashes`.

---

# 13. WorkManager

Crear un Worker dedicado, por ejemplo:

`ReportSyncWorker`

Configurar:

`NetworkType.CONNECTED`

Cuando existe al menos un reporte `PENDING` o `ERROR` reintentable, programar un trabajo de sincronización.

Además, al iniciar la aplicación comprobar si existen elementos pendientes y asegurarse de que exista trabajo programado.

No implementar polling manual constante.

Dejar que WorkManager gestione la ejecución según conectividad y restricciones del sistema.

Usar nombres únicos de trabajo para evitar múltiples Workers sincronizando el mismo reporte simultáneamente.

---

# 14. Máquina de estados de sincronización

La sincronización debe ser reanudable.

No considerar el envío como una única llamada.

Para un reporte:

### Paso 1

Crear reporte en backend.

Si responde correctamente:

- guardar inmediatamente `serverReportId`;
- no volver a crear el reporte en ejecuciones posteriores.

### Paso 2

Procesar multimedia.

Para cada archivo:

- comprobar su estado local;
- si ya fue confirmado, saltarlo;
- si no, enviar;
- después de respuesta correcta marcarlo como confirmado.

### Paso 3

Enviar observaciones.

Construir `mediaHashes` usando los hashes ya persistidos.

### Paso 4

Cuando todo haya sido confirmado:

- marcar el reporte como `SYNCED`;
- limpiar el mensaje de error;
- conservar `serverReportId`.

Si se pierde Internet:

- abandonar ordenadamente;
- mantener el progreso confirmado;
- devolver `Result.retry()` cuando corresponda.

Cuando WorkManager vuelva a ejecutarse, continuar desde el último paso confirmado.

---

# 15. Prevención de duplicados

No confiar exclusivamente en la disponibilidad de Internet.

Usar:

- UUID local por reporte;
- SHA-512 por archivo;
- estados persistentes;
- `serverReportId`;
- confirmación por archivo.

No volver a subir multimedia confirmada.

No volver a crear observaciones si ya fueron confirmadas.

## Riesgo que debe documentarse

El backend actual crea el identificador del reporte en el servidor.

Existe un caso extremo:

1. Android envía `POST create report`.
2. El backend crea correctamente el reporte.
3. La respuesta se pierde antes de llegar al dispositivo.
4. Android desconoce el `serverReportId`.
5. Un retry podría crear un segundo reporte.

La app móvil por sí sola no puede garantizar idempotencia absoluta en este escenario con el contrato API actual.

No modificar el backend desde este repositorio para intentar resolverlo.

Crear en `docs/backend-idempotency.md` una explicación del problema y una propuesta futura:

- `Idempotency-Key`, o
- `clientSubmissionId` único enviado por Android y almacenado con restricción UNIQUE en backend.

La implementación móvil debe minimizar duplicados con los mecanismos disponibles, pero no afirmar que este caso está solucionado hasta que exista soporte de idempotencia del servidor.

---

# 16. My Reports

Si existe sesión:

consultar la API de contribuciones del usuario.

Guardar una caché local razonable de los resultados necesarios para visualización offline.

La pantalla debe mostrar dos tipos de información de forma comprensible:

### Reportes locales

- borrador;
- pendiente;
- sincronizando;
- error;
- enviado.

### Reportes del servidor

- fecha;
- ubicación;
- género/especie;
- estado de verificación;
- evidencia disponible.

Evitar mostrar dos veces un reporte que ya tiene `serverReportId` y también aparece en la respuesta remota.

Cuando no haya Internet:

- mostrar la última información cacheada;
- mostrar siempre los reportes locales;
- indicar que los datos remotos pueden no estar actualizados.

Un reporte pendiente debe ser visible en My Reports inmediatamente después de guardarlo, aunque nunca haya existido conexión.

---

# 17. Home / presentación

Crear una pantalla de presentación limpia en Material 3.

No copiar el HTML/SCSS de Angular.

Recrear conceptualmente la identidad de la aplicación usando componentes nativos Compose.

Debe contener como mínimo:

- nombre del proyecto;
- breve explicación;
- objetivo de participación;
- CTA "Enviar avistamiento";
- CTA "My Reports".

Mantener esta pantalla desacoplada de la red para que pueda abrir completamente sin Internet.

---

# 18. Estados de conectividad

La aplicación puede mostrar si el dispositivo está offline, pero la lógica funcional no debe depender de que el usuario interprete correctamente ese indicador.

Ejemplo:

`Sin conexión · tus reportes se guardarán en este dispositivo`

No bloquear el formulario cuando no exista Internet.

No mostrar errores de tipo "No puede enviar porque no tiene conexión".

Guardar y encolar.

---

# 19. Manejo de errores

Diferenciar:

### Error temporal

- sin Internet;
- timeout;
- HTTP 5xx razonablemente reintentable.

Resultado:

- conservar información;
- `ERROR`/`PENDING`;
- reintentar.

### Error de autenticación

HTTP 401/403.

Resultado:

- no borrar el reporte;
- detener el intento autenticado;
- requerir nueva sesión.

### Error de validación

HTTP 400.

Resultado:

- no reintentar infinitamente;
- guardar mensaje de error;
- marcar el reporte como `ERROR`;
- permitir al usuario corregirlo.

No registrar datos personales sensibles en Logcat.

---

# 20. Catálogos offline

Guardar localmente:

- océanos;
- destinos;
- sitios de avistamiento;
- géneros;
- especies;
- comportamientos.

Actualizar cuando exista conexión.

Mantener la última versión válida si una actualización falla.

El usuario debe poder crear un reporte offline usando los catálogos que ya fueron descargados anteriormente.

Si es la primera ejecución de la app y nunca se descargaron catálogos, explicar que se necesita una primera conexión para obtenerlos.

---

# 21. Tests

Crear tests para la lógica crítica.

Como mínimo:

### Unit tests

- SHA-512;
- mappers;
- máquina de estados;
- decisiones de retry;
- construcción de payloads.

### Room tests

- guardar reporte completo;
- relaciones reporte/media/observaciones;
- cambio de estados;
- recuperación después de reiniciar.

### Worker tests

Simular:

1. sin conexión;
2. conexión restaurada;
3. fallo después de crear reporte;
4. fallo después del primer media;
5. reanudación;
6. finalización correcta.

### API tests

Usar `MockWebServer`.

No depender de un backend de producción para ejecutar la suite automática.

---

# 22. Escenario obligatorio de aceptación offline

Implementar y documentar esta prueba:

1. Abrir app.
2. Desactivar Internet.
3. Crear reporte.
4. Adjuntar al menos una foto.
5. Completar una observación.
6. Pulsar finalizar/enviar.
7. Cerrar completamente la aplicación.
8. Volver a abrirla.
9. Confirmar que el reporte continúa visible como `PENDING`.
10. Reactivar Internet.
11. Confirmar que WorkManager se ejecuta.
12. Confirmar creación del reporte.
13. Confirmar upload de media.
14. Confirmar creación de observaciones.
15. Confirmar estado final `SYNCED`.
16. Confirmar que no se generó otro envío al volver a ejecutar el Worker.

Esta funcionalidad se considera incompleta si solamente funciona mientras la aplicación permanece abierta.

---

# 23. Escenario obligatorio de interrupción parcial

Testear:

1. comenzar sincronización;
2. crear el reporte remoto;
3. subir el primer archivo;
4. simular pérdida de Internet;
5. reiniciar Worker;
6. comprobar que NO vuelve a crear el reporte;
7. comprobar que NO vuelve a subir el archivo ya confirmado;
8. continuar con los elementos restantes;
9. terminar en `SYNCED`.

---

# 24. Build y calidad

Cuando el sandbox lo permita ejecutar:

`./gradlew test`

`./gradlew lint`

`./gradlew assembleDebug`

Corregir errores propios de la implementación.

No realizar upgrades masivos de dependencias sin necesidad.

No ignorar warnings importantes relacionados con:

- almacenamiento;
- permisos;
- seguridad;
- Room;
- WorkManager;
- networking.

---

# 25. Documentación

Crear:

### `README.md`

Incluir:

- propósito;
- stack;
- arquitectura;
- cómo abrir en Android Studio;
- configuración de base URL;
- ejecución;
- pruebas;
- funcionamiento offline.

### `docs/architecture.md`

Explicar:

`Compose → ViewModel → Repository → Room/Retrofit`

y:

`Room → WorkManager → Retrofit → Spring Boot`

### `docs/offline-sync.md`

Documentar completamente:

- estados;
- máquina de sincronización;
- retry;
- multimedia;
- reinicios;
- recuperación ante errores.

### `docs/backend-idempotency.md`

Documentar la limitación de idempotencia del endpoint de creación.

---

# 26. Orden de implementación

No intentar construir todo al mismo tiempo.

Ejecutar en este orden:

- [ ] Fase 0: inspección del repositorio y del entorno.
- [ ] Fase 1: proyecto Android mínimo compilable con Compose + Material 3.
- [ ] Fase 2: navegación Home / Send Report / My Reports / Login.
- [ ] Fase 3: capa Retrofit/OkHttp y DTO.
- [ ] Fase 4: autenticación basada en cookies.
- [ ] Fase 5: Room y modelo offline.
- [ ] Fase 6: caché de catálogos.
- [ ] Fase 7: formulario del reporte.
- [ ] Fase 8: selección y copia segura de multimedia.
- [ ] Fase 9: SHA-512.
- [ ] Fase 10: guardar siempre primero en Room.
- [ ] Fase 11: WorkManager.
- [ ] Fase 12: sincronización reanudable.
- [ ] Fase 13: My Reports local + remoto.
- [ ] Fase 14: pruebas automáticas.
- [ ] Fase 15: documentación.
- [ ] Fase 16: `test`, `lint`, `assembleDebug` y revisión final.

---

# 27. Forma de trabajar

Al terminar cada fase:

1. indicar los archivos creados;
2. indicar los archivos modificados;
3. explicar brevemente qué quedó funcionando;
4. ejecutar las pruebas relevantes;
5. mostrar resultados reales de las pruebas;
6. continuar a la siguiente fase únicamente si no existe un bloqueo estructural.

Si aparece un problema que requeriría modificar el backend:

- no improvisar;
- no cambiar el contrato desde Android;
- documentarlo;
- indicar exactamente qué necesita el backend y por qué.

---

# 28. Criterio final de terminado

La primera versión se considera terminada cuando:

- abre en Android;
- Home funciona offline;
- los catálogos pueden cachearse;
- puede completarse un reporte;
- fotos/videos quedan preservados localmente;
- se calcula SHA-512;
- el reporte se guarda primero en Room;
- puede cerrarse la app sin perderlo;
- WorkManager espera conectividad;
- al recuperar conexión intenta sincronizar automáticamente;
- una sincronización parcial puede continuar;
- el usuario ve el estado del reporte;
- Login permite acceder a My Reports;
- My Reports combina correctamente información local y remota;
- los tests críticos pasan;
- `assembleDebug` pasa cuando el entorno Android del sandbox lo permite;
- ningún secreto forma parte del repositorio.

No implementar todavía:

- Scientists Desk;
- administración;
- auditoría;
- Photo-ID;
- dashboard científico;
- trivia;
- edición científica de observaciones;
- gestión administrativa de usuarios;
- funcionalidades que no pertenezcan al alcance móvil definido.