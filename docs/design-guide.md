# PNAT Mobile — Guía de diseño

## 1. Propósito

La aplicación móvil debe percibirse como una extensión nativa de RaySharkApp/PNAT, no como un producto visual diferente.

No se pretende copiar literalmente el frontend Angular en una pantalla Android. La aplicación debe adaptar la identidad visual existente a las convenciones de Android y Jetpack Compose Material 3.

Principio:

**Misma identidad → interacción nativa móvil.**

Codex no debe crear una nueva identidad visual, nueva paleta de marca, gradientes decorativos, estilos experimentales ni componentes que contradigan la aplicación web.

---

# 2. Fuente visual de referencia

La fuente de verdad es el diseño existente de `pnat_front`.

Los elementos principales que deben trasladarse a Android son:

- paleta institucional;
- jerarquía tipográfica;
- fondos claros;
- tarjetas limpias;
- bordes discretos;
- elevación moderada;
- uso de azul como color de acción y acento;
- uso contenido de colores semánticos;
- iconografía Material;
- fotografías de fauna marina como contenido, no como decoración excesiva.

La aplicación móvil debe utilizar Material 3 mediante Jetpack Compose.

---

# 3. Paleta institucional

Utilizar como referencia exacta la paleta existente:

| Token | Color |
|---|---|
| Primary 10 | `#001B2D` |
| Primary 20 | `#00324F` |
| Primary 30 | `#004A73` |
| Primary 40 | `#2FA4DC` |
| Primary 50 | `#4FB4E3` |
| Primary 60 | `#6FC2EA` |
| Primary 70 | `#8FD0F1` |
| Primary 80 | `#AFDFF7` |
| Primary 90 | `#D7F0FC` |
| Primary 95 | `#EEF9FE` |
| Primary 100 | `#FFFFFF` |

Color institucional principal:

`#2FA4DC`

No sustituirlo por el azul Material predeterminado.

## Colores estructurales

Usar como referencia:

- fondo principal: `#FFFFFF`;
- fondo secundario: `#F8FAFC`;
- fondo general aproximado: `#F8F9FA`;
- borde claro: `#E2E8F0`;
- texto principal institucional: `#2F2F2F`;
- texto de cuerpo: `#555555`;
- texto secundario: `#64748B`;
- acento: `#2FA4DC`.

## Colores semánticos existentes

- éxito/verificado: `#2E7D32`;
- pendiente científico: `#F57C00`;
- error/eliminado: aproximadamente `#D32F2F`.

No utilizar colores semánticos como decoración.

---

# 4. Theme de Compose

Crear un único theme de aplicación.

Ubicación esperada:

`app/src/main/java/.../pnat/ui/theme/`

Debe contener como mínimo:

- `Color.kt`;
- `Type.kt`;
- `Theme.kt`;
- `Shape.kt` si resulta necesario.

No declarar colores repetidamente dentro de cada pantalla.

Las pantallas deben consumir colores desde `MaterialTheme.colorScheme` o desde tokens centralizados de PNAT.

Evitar hexadecimales desperdigados por composables.

---

# 5. Tipografía

La aplicación web utiliza:

### Montserrat

Para:

- títulos;
- encabezados;
- información con fuerte jerarquía visual.

### Inter

Para:

- cuerpo;
- campos;
- descripciones;
- etiquetas;
- textos secundarios.

La app móvil debe mantener esta combinación siempre que las fuentes puedan incorporarse correctamente al proyecto.

No descargar fuentes dinámicamente durante la ejecución.

Si los archivos de las fuentes no están disponibles en el sandbox:

1. no inventar sustitutos externos;
2. implementar temporalmente la jerarquía utilizando las fuentes del sistema;
3. dejar documentado dónde incorporar Montserrat e Inter posteriormente.

No incluir archivos de fuente obtenidos de fuentes no autorizadas.

## Jerarquía

Referencia aproximada:

### Título principal de pantalla

- Montserrat;
- semibold/bold;
- equivalente móvil aproximado de 28–32 sp.

### Títulos de sección

- Montserrat;
- semibold;
- 20–24 sp aproximadamente.

### Títulos de tarjetas

- Montserrat o Inter semibold según densidad;
- 17–20 sp.

### Texto normal

- Inter;
- 14–16 sp.

### Texto secundario

- Inter;
- 12–14 sp.

No reducir texto hasta comprometer legibilidad para introducir más información.

---

# 6. Encabezados

La web utiliza encabezados oscuros acompañados por una pequeña línea azul inferior.

Cuando sea apropiado en móvil, conservar esta señal visual:

`Título`

seguido por una línea corta en `#2FA4DC`.

No es necesario utilizarla dentro de cada tarjeta.

Utilizarla principalmente en títulos importantes de pantalla o secciones de presentación.

---

# 7. Formas y bordes

La web tiene dos niveles visuales.

## Componentes institucionales y formularios

Tienen formas relativamente contenidas y poco redondeadas.

Usar aproximadamente:

- radio 4–8 dp.

Esto aplica a:

- botones;
- text fields;
- selects;
- contenedores sencillos.

## Tarjetas visuales

`ReportCard` y tarjetas KPI utilizan un aspecto más suave, aproximadamente:

- radio 12 dp.

Usar 12 dp para:

- tarjetas de reportes;
- tarjetas destacadas de Home;
- tarjetas estadísticas si llegan a utilizarse.

No convertir toda la aplicación en tarjetas extremadamente redondeadas.

Evitar radios de 24–32 dp salvo componentes Android que lo requieran funcionalmente.

---

# 8. Elevación

La aplicación web utiliza sombras discretas.

La app móvil debe conservar este principio.

Tarjeta normal:

- elevación baja.

Tarjeta seleccionada/presionada:

- cambio moderado de elevación.

No usar sombras profundas de forma permanente.

En Android no reproducir el `hover` de escritorio.

Sustituirlo por:

- estado pressed;
- ripple;
- cambio ligero de elevación;
- feedback Material 3.

---

# 9. Espaciado

Tomar como base una escala coherente:

- 4 dp;
- 8 dp;
- 12 dp;
- 16 dp;
- 20 dp;
- 24 dp;
- 32 dp.

Padding horizontal normal de pantalla:

`16–20 dp`

Separación entre bloques principales:

`24 dp`

Padding habitual de tarjeta:

`16–20 dp`

Mantener suficiente espacio para uso con los dedos.

No intentar reproducir en teléfono la densidad de información del escritorio.

---

# 10. Navegación móvil

La aplicación tendrá tres destinos funcionales principales:

1. Inicio.
2. Enviar reporte.
3. My Reports.

Usar navegación Android sencilla.

Para teléfonos, preferir `NavigationBar` inferior cuando las tres secciones estén disponibles.

Ejemplo conceptual:

`Home | Report | My Reports`

Usar Material Icons.

Iconos sugeridos:

- Home → `home`;
- Report → `add_a_photo`, `post_add` o equivalente apropiado;
- My Reports → `assignment`, `list_alt` o equivalente.

No crear iconografía personalizada si Material Icons cubre la necesidad.

Login no necesita ser un destino permanente del menú.

Debe aparecer cuando una acción requiera autenticación.

---

# 11. Home

Home debe comunicar inmediatamente que se trata de PNAT/RaySharkApp.

Debe incluir:

1. identidad/nombre del proyecto;
2. breve presentación;
3. imagen institucional o de fauna marina cuando esté disponible;
4. CTA primario:
   `Enviar avistamiento`;
5. CTA secundario:
   `My Reports`.

No crear un dashboard complejo.

La función de Home en la app móvil es:

**presentar + orientar + permitir empezar un reporte rápidamente.**

## Imagen

Si existe un recurso gráfico aprobado proveniente de la aplicación web, puede reutilizarse dentro del nuevo repositorio.

Si no está disponible en el sandbox:

- no descargar una imagen aleatoria de Internet;
- dejar un placeholder claramente identificado;
- documentar el asset necesario.

---

# 12. Send Report

Esta pantalla debe conservar conceptualmente el formulario por etapas de la web, pero adaptado a móvil.

No colocar todos los campos en una única pantalla interminable.

Organizar el flujo aproximadamente en:

### Paso 1 — Avistamiento

- fecha;
- océano;
- destino;
- sitio.

### Paso 2 — Evidencia

- fotografías;
- videos.

### Paso 3 — Identificación

- género;
- especie.

### Paso 4 — Detalles

- comportamiento;
- número de individuos;
- comentario.

### Paso adicional para invitados

- nombre;
- apellido;
- email;
- teléfono;
- consentimiento;
- permiso de contacto.

La interfaz puede utilizar un stepper propio de Compose o una progresión visual simple.

No es obligatorio reproducir literalmente `mat-stepper`.

Debe quedar claro:

- dónde está el usuario;
- qué falta;
- cómo volver atrás;
- cómo continuar.

---

# 13. Campos de formulario

Utilizar Material 3:

- `OutlinedTextField`;
- dropdowns Material;
- date picker;
- switches/checkbox según semántica;
- botones Material.

Mantener etiquetas visibles.

No depender únicamente del placeholder.

Los campos opcionales deben distinguirse claramente.

Los errores deben aparecer cerca del campo correspondiente.

No utilizar rojo para información que no sea error.

---

# 14. Botones

## Acción primaria

Utilizar el azul institucional.

Ejemplos:

- Continuar;
- Guardar reporte;
- Enviar avistamiento;
- Iniciar sesión.

## Acción secundaria

Usar botón outlined/text según importancia.

Ejemplos:

- Atrás;
- Cancelar;
- Editar.

Los botones no deben ser excesivamente redondeados.

Mantener aproximadamente la apariencia institucional de 4–8 dp.

No utilizar botones flotantes si no proporcionan una ventaja clara.

---

# 15. Evidencia multimedia

Las fotografías y videos son parte central del sistema.

Mostrar previews claramente.

Cuando existan múltiples archivos:

- grid o lista compacta;
- miniaturas;
- indicador de video;
- opción clara para retirar antes de terminar el reporte.

Utilizar radio aproximado de 8 dp para las miniaturas.

Mantener proporciones consistentes.

No deformar las fotografías.

Usar `ContentScale.Crop` cuando sea adecuado.

---

# 16. My Reports

Debe inspirarse directamente en `ReportCard` de la web.

Cada tarjeta debe mostrar prioritariamente:

1. fecha;
2. estado;
3. ubicación;
4. nombre común;
5. nombre científico;
6. fotografía/video disponible.

La imagen debe tener un protagonismo visual importante.

La web utiliza multimedia cuadrada; mantener preferentemente una preview cuadrada o cercana a cuadrada en móvil.

## Nombre científico

Mostrar en cursiva cuando exista identificación taxonómica.

Ejemplo:

*Triaenodon obesus*

Si solo existe género:

*Triaenodon* sp.

Si todavía no existe identificación:

`Aún no verificado`

---

# 17. Estados científicos del reporte

Conservar las convenciones existentes:

### Verificado

- verde;
- icono de verificación.

### Pendiente de verificación

- naranja;
- icono pendiente.

### Eliminado/rechazado cuando corresponda

- rojo;
- icono apropiado.

El color nunca debe ser la única forma de comunicar el estado.

Siempre utilizar:

- icono;
- texto o descripción accesible.

---

# 18. Estados de sincronización móvil

Los estados técnicos de sincronización son distintos del estado científico.

**No mezclarlos.**

Un reporte puede ser:

`Sincronizado + pendiente de verificación científica`

Son dos cosas diferentes.

Mostrar sincronización mediante un elemento secundario.

Ejemplos:

### DRAFT

`Borrador`

### PENDING

`Pendiente de envío`

### SYNCING

`Enviando…`

### SYNCED

No necesita ocupar espacio permanente si el reporte ya está integrado normalmente.

### ERROR

`No se pudo enviar`

con acción:

`Reintentar`

No utilizar el icono verde de “verified” para indicar que la sincronización terminó, porque ese color/icono ya representa validación científica en la aplicación web.

---

# 19. Estado offline

Cuando no exista conexión, mostrar un mensaje discreto pero visible:

`Sin conexión · tus reportes se guardarán en este dispositivo`

Preferir:

- banner;
- Surface;
- snackbar persistente razonable.

No usar un diálogo bloqueante.

No cambiar toda la interfaz a rojo.

Offline es una condición esperable, no un error excepcional.

El usuario debe poder continuar creando el reporte.

---

# 20. Confirmación del guardado

Al finalizar un reporte local mostrar:

`Reporte guardado`

y debajo:

`Se enviará automáticamente cuando haya conexión.`

Si existe conexión, la aplicación igualmente guarda primero localmente y la sincronización comienza posteriormente.

No mostrar “enviado” hasta que el backend haya confirmado la sincronización completa.

---

# 21. Cards KPI

Si My Reports incluye en el futuro:

- Total Reports;
- Total Observations;
- User Rank;

seguir la identidad actual.

Las KPI de la web utilizan:

- fondo azul institucional;
- valor grande blanco;
- etiqueta blanca secundaria;
- border radius aproximado de 12 px.

En móvil pueden mostrarse horizontalmente o mediante un pequeño grid.

No deben desplazar los reportes fuera de la primera vista innecesariamente.

---

# 22. Fondos

Fondo general preferido:

`#F8FAFC` o equivalente coherente con Material Theme.

Superficies principales:

`#FFFFFF`

Evitar grandes zonas de azul intenso como fondo.

El azul debe funcionar principalmente como:

- identidad;
- CTA;
- acento;
- iconografía;
- indicador.

---

# 23. Material Icons

Utilizar iconografía Material coherente con la web.

No mezclar múltiples familias visuales de iconos.

No utilizar emojis como iconos funcionales.

Los iconos deben acompañar acciones y estados, no sustituir etiquetas importantes.

---

# 24. Idiomas

La app móvil debe quedar preparada para inglés y español.

No escribir textos directamente dentro de composables.

Usar:

`res/values/strings.xml`

y:

`res/values-es/strings.xml`

o la organización Android equivalente.

Toda cadena visible debe provenir de recursos.

Esto incluye:

- títulos;
- botones;
- mensajes offline;
- errores;
- estados;
- accesibilidad;
- content descriptions.

---

# 25. Accesibilidad

Mantener como mínimo:

- áreas táctiles de 48 dp;
- contraste suficiente;
- `contentDescription` donde corresponda;
- soporte para tamaño de fuente del sistema;
- no comunicar estados solamente mediante color;
- labels persistentes en formularios.

No fijar alturas de texto que corten contenido cuando Android aumenta el tamaño de fuente.

---

# 26. Qué NO hacer

Codex no debe:

- diseñar una marca nueva;
- cambiar el azul institucional;
- utilizar colores neón;
- introducir gradientes sin referencia;
- usar glassmorphism;
- usar neumorphism;
- llenar la app de tarjetas redondeadas;
- crear animaciones decorativas largas;
- copiar literalmente layouts desktop de dos columnas;
- utilizar WebView para mostrar el frontend Angular;
- convertir la app en una versión comprimida del sitio web;
- incorporar SCSS/CSS dentro del proyecto móvil;
- reproducir comportamientos hover de escritorio;
- descargar assets aleatorios de Internet;
- crear un dark theme distinto de la identidad actual durante esta primera versión.

La primera versión debe utilizar tema claro.

---

# 27. Adaptación, no copia

Cuando una pantalla de escritorio no funcione bien en móvil, adaptar la composición preservando:

1. contenido;
2. jerarquía;
3. identidad;
4. colores;
5. significado.

Ejemplo:

Web:

`Formulario | Imagen`

en dos columnas.

Móvil:

`Formulario`

con una imagen de cabecera o sección visual cuando aporte valor.

No intentar mantener las dos columnas en un teléfono.

---

# 28. Componentes reutilizables

Crear componentes Compose reutilizables cuando tengan uso real.

Ejemplos:

- `PnatScreenTitle`
- `PnatPrimaryButton`
- `PnatOfflineBanner`
- `ReportCard`
- `SyncStatusIndicator`
- `MediaThumbnail`
- `FormSection`
- `EmptyState`

No crear un design system abstracto gigantesco.

El objetivo es consistencia, no sobrearquitectura.

---

# 29. Revisión visual obligatoria

Antes de considerar una pantalla terminada, Codex debe revisar:

### Identidad

- [ ] Usa el azul PNAT correcto.
- [ ] Usa fondos claros.
- [ ] Respeta jerarquía Montserrat/Inter cuando estén disponibles.
- [ ] No introduce una nueva identidad.

### Layout

- [ ] Funciona en teléfono.
- [ ] No reproduce layouts desktop literalmente.
- [ ] Tiene padding consistente.
- [ ] Tiene targets táctiles adecuados.

### Estados

- [ ] Loading.
- [ ] Empty.
- [ ] Error.
- [ ] Offline cuando corresponda.
- [ ] Estado de sincronización.
- [ ] Estado científico cuando corresponda.

### Textos

- [ ] No existen strings visibles hardcoded.
- [ ] Español e inglés están preparados.
- [ ] Los textos largos no se cortan.

### Multimedia

- [ ] Las imágenes conservan proporción.
- [ ] Videos se distinguen visualmente.
- [ ] Existe fallback cuando no hay imagen.

---

# 30. Evidencia visual durante desarrollo

Para cada pantalla principal:

- Home;
- Send Report;
- My Reports;
- Login;

generar o capturar al menos una evidencia visual mediante Preview de Compose, screenshot de emulator o mecanismo disponible en el sandbox.

Si el entorno no permite renderizar previews o ejecutar emulador, indicarlo explícitamente.

No afirmar que el diseño fue validado visualmente si solo se compiló.

---

# 31. Criterio final

El objetivo no es que alguien diga:

`Esta app Android utiliza Material 3.`

El objetivo es que diga:

`Esta es claramente la aplicación móvil del mismo PNAT/RaySharkApp que la aplicación web.`

La identidad debe mantenerse mediante:

**paleta + tipografía + jerarquía + tarjetas + iconografía + fotografía + lenguaje visual.**

La interacción debe mantenerse nativa mediante:

**Compose + Material 3 + navegación Android + estados offline + controles táctiles.**