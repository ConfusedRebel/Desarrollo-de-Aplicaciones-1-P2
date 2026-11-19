# Jornadas del Sur

App Android escrita en Kotlin. Usa Retrofit para obtener el programa de charlas y Room para guardarlo en el dispositivo.

## Cambios implementados

### 41 · Programa sin señal

**Problema:** Si fallaba una descarga, el catálogo ocultaba las charlas guardadas. Además, una respuesta exitosa sin charlas se confundía con no haber descargado nunca el programa.

**Solución:** La app conserva y muestra las charlas, fichas y favoritas guardadas, junto con un aviso de que usa datos guardados. Si no hay una edición previa, ofrece **Reintentar**. Room registra una descarga exitosa junto con la nueva lista, incluso cuando está vacía; por eso la edición v4 muestra **«sin charlas programadas»** también al volver a abrir la app sin señal.

### 42 · Mi agenda

**Problema:** Al actualizar, las charlas podían perder su marca de favorita y las charlas retiradas seguían apareciendo.

**Solución:** Cada programa nuevo reemplaza al anterior en una transacción. Las charlas que conservan su identificador mantienen la marca actual y reciben el título y la sala nuevos. Las retiradas se eliminan; si vuelven en otra edición, regresan sin marcar. La marca se consulta al aplicar la respuesta, por lo que se respetan los cambios hechos durante la espera. La migración de Room conserva los datos de las instalaciones existentes.

### 43 · Volver al punto de lectura

**Problema:** Al abrir una ficha, rotar o recuperar la tarea después de que Android cerrara el proceso, se perdían los filtros.

**Solución:** El día y el tema se guardan como estado restaurable, igual que la ficha abierta. Al volver, se recupera la selección; una tarea nueva comienza sin filtros.

### 44 · Control de descargas

**Problema:** La app descargaba de más al rotar, volver de una ficha o cambiar de pestaña. Varios toques durante una descarga podían iniciar varias peticiones a la vez.

**Solución:** Se inicia una sola carga automática por proceso. La navegación no inicia descargas. Los toques recibidos mientras hay una petición activa se agrupan en **una petición adicional** al terminar. Después de un éxito o una falla se puede actualizar de nuevo, y los filtros siguen disponibles durante la espera.

## Comprobar el proyecto

Con JDK 17 y Android SDK 34, ejecutá desde esta carpeta:

```bash
./gradlew testDebugUnitTest assembleDebug
```

El APK de prueba queda en `app/build/outputs/apk/debug/`. La pestaña **Mesa** permite elegir la edición, apagar el servicio, activar la demora larga y ver cuántas peticiones se hicieron en el proceso.
