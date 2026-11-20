# VozFlow Pro — Android 1.3.0

Teclado IME nativo para Android, desarrollado en Kotlin con vistas y recursos XML, Gradle Kotlin DSL y Gradle Wrapper. Paquete: `com.wlqboard.nativeapp`.

## Compilar

Requiere JDK 17 o posterior y Android SDK Platform 35 / Build Tools 35.0.0.

```bash
./gradlew clean assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk` y queda firmado con la clave de depuración predeterminada de Android. El proyecto no usa keystore externo ni GitHub Actions.

## Funciones

- Barra de altura fija con tres sugerencias contextuales, acceso al menú, micrófono y modo de herramientas. El panel de edición ofrece cortar, copiar, pegar, borrar, deshacer y volver a letras.
- Predicción y autocorrección local en español, vocabulario técnico/ampliado, diccionario personal y expansión de atajos. El historial de deshacer valida el contexto antes de restaurar y expone un botón temporal después de las operaciones relevantes.
- Gestos de cursor en la barra espaciadora mediante `InputConnection.setSelection`, eliminación progresiva de palabras al deslizar sobre Retroceso, repetición rápida al mantener y selección táctil por tecla más cercana con la opción de dedos grandes.
- Ajustes de sensibilidad, altura del teclado (70–145%), tamaño de fuente, tema, fila numérica, háptica, vista previa de tecla y una mano. El teclado flotante es arrastrable y redimensionable dentro de la ventana que Android asigna al IME; usa una región táctil limitada a la tarjeta para dejar pasar toques fuera de ella.
- Historial local SQLite del portapapeles: 20 elementos recientes por defecto (límite configurable de 1–200), fijados exentos del vencimiento, limpieza al superar una hora y poda pasiva mientras el IME está visible. Las miniaturas se decodifican reducidas para limitar el consumo de memoria.
- Capturas recientes opcionales: consulta puntual de MediaStore al abrir el teclado, sin observador ni sondeo en segundo plano. La inserción de imágenes usa `commitContent` solo si el campo declara un MIME compatible.
- Dictado con Vosk y modelo español incluido, todo local. El asistente de calibración propone 15 frases; el usuario puede añadir palabras para repetir, ver progreso, guardar alias aprendidos y administrarlos. Un control de 0 a 10 segundos (pasos de 0,5 s; 0 = inmediato) regula mayúscula tras pausas. La calibración crea reglas heurísticas y no reentrena el modelo acústico.
- Auditor global: `CrashHandler` guarda los últimos tres informes en SharedPreferences; la Activity permite leer, copiar y borrar el registro. El IME registra fallos de componentes secundarios e intenta conservar un teclado QWERTY básico.

## Privacidad y recursos

Vosk procesa audio en el dispositivo y no conserva grabaciones. El micrófono se usa durante dictado o entrenamiento, y el controlador se libera al ocultarse el teclado. Las conexiones SQLite y el listener del portapapeles se cierran/desregistran cuando el IME deja de estar visible. No se declara permiso de Internet ni se instala servicio permanente, overlay, wake lock u observador continuo de galería. El historial local puede incluir texto y URI de imágenes.

Los permisos de imágenes son opcionales y se solicitan solo al activar capturas. En Android 14 o posterior se admite el acceso seleccionado por el usuario. El manifiesto registra el servicio protegido por `BIND_INPUT_METHOD` y `@xml/method`.

## Diagnóstico y límites

Las raíces del IME usan `FrameLayout.LayoutParams`; filas y teclas usan `LinearLayout.LayoutParams`, evitando parámetros genéricos en las asignaciones del teclado. El modo flotante respeta los límites de la ventana de entrada que ofrece Android; no puede convertirse en overlay arbitrario. El evento Enter estándar puede ser interpretado por algunos editores de una sola línea como una acción del campo. El reconocimiento y las funciones del portapapeles pueden variar según el editor anfitrión.

La compilación en Sandbox confirma empaquetado y firma, pero no sustituye una prueba en Samsung u otro dispositivo físico. La selección de texto, permisos de fotos, inserción de imágenes y arrastre deben validarse en las aplicaciones objetivo.
