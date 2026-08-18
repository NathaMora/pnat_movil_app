# PNAT Mobile — Sincronización offline

## 1. Propósito

Este documento define las reglas obligatorias para guardar y sincronizar reportes en `pnat_mobile`.

La aplicación se utilizará en lugares donde la conectividad puede ser inexistente o inestable.

Por tanto, un reporte nunca debe depender de que exista Internet en el momento en que el usuario lo crea.

---

## 2. Regla principal

Todo reporte terminado se guarda primero localmente.

Flujo obligatorio:

```text
Usuario completa reporte
        ↓
Validar
        ↓
Guardar en Room
        ↓
Estado = PENDING
        ↓
Programar WorkManager
        ↓
Esperar conectividad
        ↓
Sincronizar
        ↓
Estado = SYNCED
```

La existencia de Internet no cambia el paso inicial.

Incluso si hay conexión, primero se guarda en Room.

---

## 3. Razón

Este diseño evita perder información cuando:

- la conexión desaparece durante el envío;
- Android mata el proceso;
- la app se cierra;
- un video tarda demasiado en subir;
- el backend responde tarde;
- el dispositivo cambia de Wi-Fi a datos móviles;
- una parte del reporte ya llegó al servidor y otra no.

Room conserva el estado durable del proceso.

---

## 4. Estados del reporte

### DRAFT

El usuario todavía está construyendo el reporte.

Puede modificarse.

No debe sincronizarse automáticamente.

### PENDING

El usuario terminó el reporte y los datos quedaron persistidos correctamente.

Es elegible para sincronización.

### SYNCING

Un Worker está procesando el reporte.

Este estado no significa que el envío esté completo.

### SYNCED

El backend confirmó todos los pasos necesarios.

Solo entonces puede considerarse completamente enviado.

### ERROR

Existe un problema que requiere:

- retry posterior;
- autenticación;
- o corrección por el usuario.

El reporte y sus archivos deben conservarse.

---

## 5. Estados por paso

No basta con un único estado para todo el reporte.

Debe persistirse progreso suficiente para continuar una sincronización parcial.

Como mínimo, poder conocer:

- si el reporte remoto ya fue creado;
- el `serverReportId`;
- qué archivos multimedia ya fueron confirmados;
- si las observaciones ya fueron confirmadas;
- el último error.

Ejemplo conceptual:

```text
LocalReport
  localId
  syncStatus
  serverReportId?
  observationsUploaded
  lastError?

LocalMedia
  localId
  reportLocalId
  sha512
  uploaded
```

La implementación concreta puede utilizar campos más explícitos si mejora la seguridad.

---

## 6. Inicio de sincronización

Un reporte es elegible cuando:

```text
status = PENDING
```

o cuando:

```text
status = ERROR
```

y el error sea reintentable.

Antes de iniciar:

1. comprobar que no exista otro Worker procesando el mismo reporte;
2. establecer `SYNCING`;
3. leer desde Room el estado persistido;
4. continuar desde el primer paso pendiente.

---

## 7. Restricción WorkManager

Usar:

```text
NetworkType.CONNECTED
```

WorkManager decide cuándo ejecutar el trabajo según las restricciones del sistema.

No mantener un servicio permanente únicamente para vigilar conectividad.

No hacer polling continuo.

---

## 8. Secuencia para usuario autenticado

El backend actual separa la creación del reporte, multimedia y observaciones.

Flujo:

```text
1. POST /api/reports
2. Guardar reportId devuelto
3. Subir multimedia pendiente
4. POST /api/observations/batch
5. Confirmar estado final
```

Multimedia autenticada:

```text
POST /api/media/upload
```

Parámetros:

- file;
- hash;
- reportId;
- mediaType.

Las observaciones usan `mediaHashes` para asociarse con la evidencia.

---

## 9. Secuencia para invitado

Flujo:

```text
1. POST /api/public-reports
2. Guardar reportId devuelto
3. POST /api/public-reports/{reportId}/media
4. POST /api/public-reports/{reportId}/observations
5. Confirmar estado final
```

El `reportId` devuelto debe persistirse inmediatamente.

---

## 10. Paso 1 — creación remota

Antes de crear:

```text
serverReportId == null
```

Si ya existe un `serverReportId`, no volver a ejecutar el POST de creación.

Después de una respuesta correcta:

1. persistir `serverReportId` inmediatamente en Room;
2. hacerlo antes de continuar con multimedia;
3. no depender de mantener ese ID solamente en memoria.

---

## 11. Paso 2 — multimedia

Procesar archivo por archivo.

Para cada `LocalMedia`:

```text
uploaded == true
```

→ saltar.

```text
uploaded == false
```

→ intentar enviar.

Después de confirmación del backend:

```text
uploaded = true
```

Persistir el estado inmediatamente.

No esperar hasta terminar todos los archivos para guardar progreso.

---

## 12. SHA-512

Calcular SHA-512 antes de la sincronización y persistirlo.

Reglas:

- usar streaming;
- salida hexadecimal;
- exactamente 128 caracteres;
- reutilizar el valor durante retries;
- no volver a calcular sin necesidad.

El backend actual usa el hash para detección de multimedia duplicada y para asociar observaciones mediante `mediaHashes`.

---

## 13. Comprobación de duplicados multimedia

Cuando corresponda utilizar:

```text
GET /api/media/check-hash
```

La existencia de este endpoint no reemplaza la máquina de estados local.

El estado local debe seguir registrando si la aplicación considera ese archivo confirmado para el reporte actual.

Un hash existente en el backend requiere interpretar el contrato real antes de decidir si el archivo puede omitirse o si existe un conflicto de asociación.

No inventar comportamiento.

---

## 14. Paso 3 — observaciones

Solo intentar crear observaciones cuando:

- exista `serverReportId`;
- los archivos requeridos estén en un estado compatible con el flujo;
- los hashes estén disponibles.

Construir el payload a partir de los datos persistidos en Room.

No construir observaciones a partir de estado temporal de una pantalla.

Una vez confirmadas:

```text
observationsUploaded = true
```

o mecanismo equivalente.

Persistir inmediatamente.

---

## 15. Paso final

El reporte solo cambia a:

```text
SYNCED
```

si todos los pasos requeridos fueron confirmados.

Ejemplo:

```text
serverReportId != null
AND required media confirmed
AND observations confirmed
```

Después:

- limpiar errores técnicos anteriores;
- conservar `serverReportId`;
- no eliminar automáticamente el registro local si My Reports lo necesita para reconciliación.

---

## 16. Pérdida de conexión

Ejemplo:

```text
Crear reporte     ✓
Foto A            ✓
Foto B            ✗ conexión perdida
Observaciones     pendiente
```

Persistencia:

```text
serverReportId = existente
Foto A.uploaded = true
Foto B.uploaded = false
observationsUploaded = false
```

El siguiente Worker debe:

```text
NO crear reporte otra vez
NO subir Foto A otra vez
SÍ continuar con Foto B
SÍ continuar luego con observaciones
```

---

## 17. Reinicio de aplicación

Cerrar la app no debe cambiar el progreso.

Al iniciar:

1. consultar Room;
2. identificar `PENDING`, `SYNCING` abandonados o `ERROR` reintentables;
3. normalizar estados si es necesario;
4. asegurar que exista trabajo programado.

Un estado `SYNCING` persistido no debe quedar bloqueado indefinidamente después de que Android mate el proceso.

La implementación debe definir una estrategia segura de recuperación, por ejemplo mediante el Worker como autoridad de ejecución y timestamps de intento.

---

## 18. Reinicio del dispositivo

No diseñar la sincronización suponiendo que la app permanecerá abierta.

WorkManager proporciona persistencia de trabajo de fondo compatible con el ciclo de vida Android.

Los datos esenciales siempre deben estar en Room/almacenamiento durable.

---

## 19. Clasificación de errores

### 19.1 Error temporal

Ejemplos:

- IOException;
- timeout;
- pérdida de red;
- error temporal de servidor;
- HTTP 5xx apropiado.

Acción:

```text
conservar estado
registrar error técnico sanitizado
volver a PENDING/ERROR reintentable
Result.retry()
```

Aplicar backoff razonable de WorkManager.

---

### 19.2 Error de autenticación

Ejemplos:

```text
401
403 por sesión
```

Acción:

- conservar reporte;
- no borrar media;
- marcar que requiere autenticación;
- no entrar en retry infinito;
- solicitar login al usuario;
- reprogramar después de sesión válida.

---

### 19.3 Error de validación

Ejemplo:

```text
400
```

cuando el payload es inválido.

Acción:

- conservar reporte;
- `ERROR`;
- guardar un mensaje interpretable/sanitizado;
- permitir corrección;
- no hacer retry infinito sin cambios.

---

## 20. Reintentos

No todos los errores deben usar retry automático.

### Reintentar automáticamente

- conectividad;
- timeout;
- errores transitorios.

### Esperar acción del usuario

- credenciales requeridas;
- validación de formulario;
- datos rechazados por regla de negocio.

---

## 21. Backoff

Usar la política de backoff de WorkManager.

Evitar ciclos rápidos que:

- consuman batería;
- saturen red;
- golpeen repetidamente el backend.

La frecuencia exacta puede ajustarse durante implementación.

---

## 22. Trabajo único

Evitar:

```text
Worker A -> Reporte X
Worker B -> Reporte X
```

simultáneamente.

Usar:

- unique work;
- clave derivada del local report ID;
- o mecanismo equivalente.

Las operaciones locales deben ser transaccionales cuando corresponda.

---

## 23. Transacciones Room

Cuando el usuario finaliza un reporte, guardar de forma coherente:

- report;
- media;
- observations;
- relaciones.

Usar una transacción para evitar estados como:

```text
reporte guardado
pero observaciones perdidas
```

Solo después de una persistencia local correcta marcarlo `PENDING`.

---

## 24. Multimedia durable

Un archivo pendiente debe sobrevivir al cierre de la app.

No confiar únicamente en memoria o preview URI temporal.

Cuando sea necesario:

```text
Photo Picker
   ↓
copiar a almacenamiento controlado
   ↓
persistir ruta local
   ↓
calcular hash
```

No eliminar hasta confirmación.

---

## 25. Limpieza de archivos

No implementar limpieza agresiva en la primera versión.

Un archivo puede eliminarse solo cuando exista una política definida y segura.

Como mínimo nunca borrar si:

```text
report.status != SYNCED
```

Si posteriormente se limpia media sincronizada, documentar:

- cuándo;
- qué se conserva;
- cómo My Reports obtiene previews remotos.

---

## 26. My Reports y estados locales

My Reports debe mostrar inmediatamente reportes locales.

Ejemplos:

```text
Borrador
Pendiente de envío
Enviando
No se pudo enviar
```

El estado científico es separado.

Ejemplo válido:

```text
Sincronizado
Pendiente de verificación científica
```

No usar “Verificado” para indicar que el upload terminó.

---

## 27. Reconciliación local-remota

Después de sincronizar:

```text
local.serverReportId = remote.reportId
```

Al consultar My Reports:

- si el servidor contiene el mismo reportId;
- combinar o preferir el objeto remoto según la capa de presentación;
- no mostrar duplicado.

Mantener el identificador local para trazabilidad interna mientras sea útil.

---

## 28. Caso límite: respuesta perdida al crear reporte

Escenario:

```text
Android -> POST create
Backend -> crea reporte
Backend -> envía 200 con reportId
Red -> respuesta se pierde
Android -> no recibe reportId
```

El cliente no puede saber con certeza si el reporte fue creado.

Reintentar el mismo POST puede crear un segundo reporte.

Este problema no puede resolverse completamente solo con Room.

Está documentado en:

`docs/backend-idempotency.md`

Hasta que el backend tenga soporte idempotente, la app debe minimizar el riesgo pero no afirmar garantía absoluta.

---

## 29. Logging

Los logs técnicos de sincronización pueden incluir:

- local report ID;
- paso;
- código HTTP;
- tipo de error.

No incluir:

- password;
- JWT;
- cookie;
- email completo si no es necesario;
- nombres personales;
- contenido sensible del reporte;
- rutas externas privadas innecesarias.

---

## 30. Métricas locales útiles

Sin necesidad de analítica externa, para debugging puede ser útil conservar:

- createdAt;
- updatedAt;
- lastSyncAttemptAt;
- retryCount;
- lastErrorType.

No usar estos campos para introducir comportamiento complejo sin necesidad.

---

## 31. Pruebas obligatorias

### Caso A — totalmente offline

1. desactivar Internet;
2. crear reporte;
3. agregar foto/video;
4. agregar observación;
5. finalizar;
6. verificar `PENDING`;
7. cerrar app;
8. abrir app;
9. verificar que existe;
10. activar Internet;
11. ejecutar sincronización;
12. verificar `SYNCED`.

---

### Caso B — interrupción después de crear reporte

1. iniciar sincronización;
2. confirmar creación remota;
3. persistir `serverReportId`;
4. perder Internet;
5. retry;
6. verificar que no se ejecuta de nuevo creación remota;
7. continuar media.

---

### Caso C — interrupción multimedia

1. crear reporte remoto;
2. subir media A;
3. persistir A como confirmada;
4. fallar media B;
5. retry;
6. A no se vuelve a subir;
7. B continúa.

---

### Caso D — sesión expirada

1. reporte autenticado pendiente;
2. sesión expira;
3. Worker obtiene 401;
4. reporte permanece;
5. usuario inicia sesión;
6. sincronización continúa.

---

### Caso E — validación

1. servidor devuelve 400;
2. Worker no entra en loop;
3. reporte permanece `ERROR`;
4. UI permite identificar/corregir problema.

---

## 32. Herramientas de test

Usar cuando sea posible:

- Room in-memory database;
- WorkManager testing;
- MockWebServer;
- unit tests de state machine;
- fakes de repository.

No depender de producción.

---

## 33. Criterio de aceptación

La sincronización se considera correcta cuando:

- no se pierde información offline;
- cerrar la app no pierde el reporte;
- WorkManager puede continuar después;
- progreso parcial se conserva;
- media confirmada no se vuelve a enviar deliberadamente;
- reportes con `serverReportId` no vuelven a crearse;
- errores se clasifican;
- autenticación no provoca pérdida de datos;
- My Reports refleja estados locales;
- solo se marca `SYNCED` tras confirmación completa.

---

## 34. Documentos relacionados

Leer junto con:

- `AGENTS.md`
- `docs/architecture.md`
- `docs/backend-idempotency.md`
- `docs/design-guide.md`
