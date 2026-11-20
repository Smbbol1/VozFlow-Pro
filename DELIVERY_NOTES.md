# VozFlow Pro — hotfix de predicción

## Corrección

Se corrigió únicamente `SpanishPredictionEngine.kt`. El patrón inválido `(?<= )+` se eliminó de `phonetic()`: era redundante porque la normalización siguiente ya elimina espacios con `whitespacePattern`. Las expresiones regulares restantes se auditaron y ahora se declaran como propiedades `private val` precompiladas a nivel del objeto, incluidas las variantes para alias de frases; así no se compilan en cada predicción.

No se cambió el `versionCode`/`versionName` (continúa `4` / `1.3.0`), ni Gradle, permisos, interfaz, configuración de ahorro/gestión de batería o las demás funciones.

## Verificación

La compilación ejecutó `./gradlew assembleDebug` y terminó con `BUILD SUCCESSFUL`. El APK debug verifica con certificado `C=US, O=Android, CN=Android Debug`; SHA-256 del certificado: `f8b4ef3cdc0291a1bd0acc7061a51db24f198a3cc19a03ca6b83ece24eb1ea7a`. SHA-256 del APK actualizado: `2b3b1fe839dc65893eeb502c3000b01b04eecf1704be3ccaffa492ad41f6965e`.

La búsqueda confirmó que `(?<= )+` ya no aparece y que las construcciones `Regex(...)` restantes están en las propiedades del objeto, no dentro de las funciones de predicción. Se verificó la firma del APK. No se conectó un dispositivo/emulador para ejecutar una prueba de dictado en Android físico.
