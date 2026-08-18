# PNAT Mobile — Idempotencia del backend

## 1. Propósito

Este documento registra una limitación conocida del contrato actual entre `pnat_mobile` y `pnat_backend`.

La limitación afecta principalmente la creación inicial de reportes cuando existe conectividad inestable.

No es un error de Room ni de WorkManager.

Es una propiedad del contrato HTTP actual.

---

## 2. Qué significa idempotencia

Una operación idempotente puede repetirse después de una duda de red sin crear efectos duplicados.

Ejemplo deseado:

```text
Cliente envía solicitud X
        ↓
Servidor crea Reporte A
        ↓
respuesta se pierde
        ↓
Cliente repite solicitud X
        ↓
Servidor reconoce que X ya fue procesada
        ↓
devuelve Reporte A
```

No crea Reporte B.

---

## 3. Contrato actual

La creación de reportes genera el identificador en el backend.

Para usuario autenticado:

```text
POST /api/reports
```

El backend crea el reporte y devuelve:

```text
reportId
```

Para invitado:

```text
POST /api/public-reports
```

El backend también crea el reporte y devuelve:

```text
reportId
```

En el contrato actual no existe, según el código revisado, un identificador único generado por el cliente móvil que el backend use para reconocer un retry de la misma creación.

Tampoco se observa una cabecera `Idempotency-Key` implementada para estos endpoints.

---

## 4. El escenario problemático

```text
1. Android guarda reporte local UUID = L1
2. Worker hace POST /api/reports
3. Backend crea report_id = R1
4. Backend confirma transacción
5. Respuesta HTTP con R1 viaja hacia Android
6. La conexión se pierde
7. Android nunca recibe R1
```

Estado real:

```text
Backend:
R1 existe

Android:
serverReportId = null
```

El cliente no tiene forma fiable de saber si el paso 3 ocurrió.

---

## 5. Qué pasa al reintentar

El Worker observa:

```text
serverReportId == null
```

y razonablemente vuelve a ejecutar:

```text
POST /api/reports
```

El backend puede crear:

```text
R2
```

Resultado:

```text
R1 = primer reporte
R2 = duplicado
```

Este es el hueco de idempotencia.

---

## 6. Por qué Room no puede solucionarlo

Room solo conoce lo que Android consiguió confirmar.

Si la respuesta con `R1` nunca llegó:

```text
Room no conoce R1
```

Guardar el estado local antes y después de cada paso reduce muchísimo los duplicados, pero no puede reconstruir un identificador remoto que nunca recibió.

---

## 7. Por qué SHA-512 tampoco lo resuelve

El backend utiliza SHA-512 para multimedia.

Esto ayuda a:

- detectar archivos duplicados;
- identificar contenido multimedia;
- relacionar observaciones con `mediaHashes`.

Pero un hash multimedia no representa inequívocamente una solicitud de creación de reporte.

Un mismo archivo tampoco debería utilizarse como sustituto técnico de una clave idempotente del reporte.

Por tanto:

```text
SHA-512 multimedia != idempotencia de create report
```

---

## 8. Qué sí puede hacer actualmente la app móvil

Mientras no cambie el backend:

1. generar un UUID local para cada reporte;
2. guardar todo primero en Room;
3. ejecutar solo un Worker por reporte;
4. persistir `serverReportId` inmediatamente cuando llegue;
5. nunca volver a crear si `serverReportId != null`;
6. conservar progreso por media;
7. evitar retries innecesarios;
8. usar backoff;
9. documentar el riesgo residual.

Esto minimiza duplicados, pero no los elimina al 100 %.

---

## 9. Solución recomendada

La opción recomendada para PNAT es introducir un identificador idempotente generado por el cliente.

Nombre sugerido:

```text
clientSubmissionId
```

Tipo:

```text
UUID
```

El móvil lo genera una sola vez al crear el objeto local.

Ejemplo:

```text
LocalReport.localId = 8fd...
clientSubmissionId = 8fd...
```

Ese identificador se mantiene durante todos los retries.

---

## 10. Cambio conceptual en base de datos

Agregar a `reports` una columna similar a:

```text
client_submission_id UUID
```

y una restricción UNIQUE apropiada.

La definición final debe decidir si la unicidad es:

```text
UNIQUE(client_submission_id)
```

o si necesita considerar namespace/origen.

Para una app controlada por PNAT, un UUID global único normalmente permite una implementación sencilla.

No ejecutar este cambio desde `pnat_mobile`.

Debe hacerse deliberadamente en `pnat_backend` y en el script PostgreSQL correspondiente.

---

## 11. Cambio conceptual en los DTO

Los DTO de creación deberían aceptar:

```text
clientSubmissionId
```

tanto para el flujo autenticado como para el flujo invitado.

Ejemplo conceptual:

```json
{
  "clientSubmissionId": "8fd0...",
  "...": "otros campos actuales"
}
```

La app debe enviar siempre el mismo UUID durante retries.

---

## 12. Comportamiento esperado del backend

Pseudoflujo:

```text
POST create report
clientSubmissionId = X
        ↓
¿existe report con X?
   ├── sí -> devolver reportId existente
   └── no -> crear report
             guardar X
             devolver nuevo reportId
```

La comprobación debe estar protegida también por una restricción UNIQUE de base de datos.

No confiar únicamente en:

```text
SELECT y luego INSERT
```

sin protección de concurrencia, porque dos solicitudes simultáneas podrían pasar la comprobación.

---

## 13. Alternativa: Idempotency-Key HTTP

Otra posibilidad es:

```text
Idempotency-Key: <UUID>
```

El backend mantiene una tabla/registro de claves procesadas y sus respuestas.

Ventajas:

- patrón HTTP general;
- no obliga a incluir el campo en el cuerpo.

Desventajas:

- requiere infraestructura de almacenamiento de claves;
- definición de expiración;
- definición de scope;
- almacenamiento/reconstrucción de respuesta;
- más complejidad para este caso.

Para PNAT, `clientSubmissionId` dentro del dominio del reporte probablemente sea más sencillo, siempre que se implemente correctamente.

---

## 14. Recomendación para primera implementación

Preferir:

```text
clientSubmissionId UUID
```

porque:

- existe una entidad clara: report;
- la app ya necesita UUID local;
- se puede conservar permanentemente;
- simplifica la reconciliación;
- puede auditarse;
- funciona para retries mucho después del primer intento.

---

## 15. Flujo futuro con idempotencia

```text
Android crea LocalReport
localId = L1
clientSubmissionId = L1
        ↓
Room PENDING
        ↓
POST create(clientSubmissionId=L1)
        ↓
Backend no encuentra L1
        ↓
crea R1 + guarda L1
        ↓
respuesta se pierde
        ↓
Worker retry
        ↓
POST create(clientSubmissionId=L1)
        ↓
Backend encuentra R1
        ↓
devuelve R1
        ↓
Android guarda serverReportId=R1
```

No aparece R2.

---

## 16. Aplicación al flujo invitado

La misma protección debe existir para:

```text
POST /api/public-reports
```

No debe limitarse a usuarios autenticados.

Los invitados son especialmente importantes porque también pueden enviar desde zonas con conectividad limitada.

---

## 17. Multimedia

La tabla `media` ya tiene una restricción de unicidad sobre el hash original según el esquema actual:

```text
UNIQUE(original_hash)
```

Esto aporta una protección de duplicidad diferente.

Sin embargo, la lógica móvil debe seguir tratando upload y asociación cuidadosamente.

Idempotencia de creación de reporte y unicidad de multimedia son problemas distintos.

---

## 18. Observaciones

Las observaciones tampoco deben asumirse automáticamente idempotentes.

El endpoint batch actual crea observaciones para un reporte.

Si la respuesta de creación de observaciones se pierde después de confirmar en el backend, un retry podría potencialmente repetir la operación dependiendo de la implementación del servicio y sus restricciones.

Antes de afirmar idempotencia completa del flujo, revisar específicamente el comportamiento real de:

```text
POST /api/observations/batch
POST /api/public-reports/{reportId}/observations
```

y decidir si necesitan también una clave cliente por observación o una operación de reemplazo/upsert controlada.

Este documento no afirma que esa protección exista actualmente.

---

## 19. Posible solución futura para observaciones

Si se requiere idempotencia fuerte de extremo a extremo, considerar:

```text
clientObservationId UUID
```

por observación.

El mismo principio:

```text
mismo clientObservationId
        ↓
misma observación remota
```

No implementar hasta revisar el backend y definir el contrato final.

---

## 20. Seguridad

Una clave idempotente:

- no es un secreto;
- no autentica al usuario;
- no sustituye JWT/cookie;
- no concede permisos.

El backend debe seguir aplicando autenticación/autorización normal.

Para invitado debe seguir aplicando las reglas existentes de validación del reporte público.

---

## 21. Compatibilidad

Al introducir `clientSubmissionId`, decidir explícitamente si:

- será obligatorio inmediatamente;
- será opcional para compatibilidad con Angular;
- Angular también comenzará a enviarlo;
- solo los clientes nuevos lo usarán inicialmente.

Una migración segura podría permitir inicialmente:

```text
client_submission_id NULL
```

para flujos legacy/web, con unicidad sobre valores no nulos, y exigirlo posteriormente donde corresponda.

La decisión final pertenece al backend y a la estrategia de despliegue.

---

## 22. Cambios que NO debe hacer Codex desde pnat_mobile

Codex no debe:

- editar entidades Java del backend;
- agregar columnas a PostgreSQL;
- cambiar controllers;
- cambiar DTO Java;
- modificar SecurityConfig;
- asumir que el endpoint ya es idempotente.

Desde `pnat_mobile` solo debe:

- generar/persistir UUID local;
- preparar la arquitectura para poder enviar una clave futura;
- documentar el bloqueo;
- usar el contrato actual hasta recibir instrucciones.

---

## 23. Criterio para considerar resuelto este riesgo

La creación de reportes podrá considerarse idempotente cuando exista una prueba que demuestre:

1. cliente envía clave X;
2. backend crea un único reporte;
3. se simula pérdida de respuesta;
4. cliente repite X;
5. backend devuelve el mismo `reportId`;
6. el conteo de reportes creados sigue siendo 1;
7. solicitudes concurrentes con X no crean dos filas.

Hasta entonces, describir el sistema como:

```text
sincronización reanudable con mitigación de duplicados
```

y no como:

```text
exactly-once garantizado
```

---

## 24. Prioridad

Este cambio no bloquea la construcción inicial de `pnat_mobile`.

Sí debe resolverse antes de declarar robustez total del flujo offline en producción.

Prioridad recomendada:

```text
Alta antes de producción
No bloqueante para prototipo/desarrollo
```

---

## 25. Documentos relacionados

Leer junto con:

- `AGENTS.md`
- `docs/architecture.md`
- `docs/offline-sync.md`
- `docs/design-guide.md`
