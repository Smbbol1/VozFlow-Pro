package com.wlqboard.nativeapp

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

/** Small on-demand Spanish lexicon and conservative offline normalizer for dictation and suggestions. */
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // 1. Gramática, conectores, adverbios y frecuencia máxima
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras",
        "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "anteayer", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "adentro", "afuera", "delante", "detrás", "encima", "debajo", "alrededor", "enfrente", "junto", "siempre", "nunca", "jamás", "todavía", "aún", "apenas", "casi", "bastante", "demasiado", "suficiente", "exactamente", "realmente", "seguramente", "probablemente", "posiblemente", "solamente", "únicamente", "finalmente", "actualmente", "anteriormente", "recientemente", "normalmente", "generalmente", "especialmente", "principalmente", "rápidamente", "lentamente", "fácilmente", "difícilmente", "claramente", "directamente", "inmediatamente", "completamente", "totalmente", "absolutamente", "quizás", "tal vez", "seguro", "claro", "obvio", "cierto", "exacto", "correcto", "incorrecto",

        // 2. Adjetivos esenciales (estado, cualidad, tamaño, emoción, color, forma)
        "bueno", "buena", "buenos", "buenas", "malo", "mala", "malos", "malas", "bien", "mal", "mejor", "mejores", "peor", "peores", "igual", "iguales", "diferente", "diferentes", "distinto", "distinta", "mismo", "misma", "mismos", "mismas", "nuevo", "nueva", "nuevos", "nuevas", "viejo", "vieja", "viejos", "viejas", "joven", "jóvenes", "antiguo", "antigua", "moderno", "moderna", "grande", "grandes", "pequeño", "pequeña", "pequeños", "pequeñas", "enorme", "gigante", "minúsculo", "largo", "larga", "corto", "corta", "alto", "alta", "bajo", "baja", "ancho", "ancha", "estrecho", "estrecha", "grueso", "delgado", "pesado", "ligero", "fuerte", "débil", "duro", "blando", "suave", "áspero", "limpio", "limpia", "sucio", "sucia", "seco", "mojado", "húmedo", "caliente", "frío", "tibio", "fresco", "dulce", "salado", "amargo", "ácido", "picante", "rico", "sabroso", "delicioso",
        "fácil", "difícil", "simple", "sencillo", "complejo", "complicado", "rápido", "rápida", "lento", "lenta", "importante", "importantes", "necesario", "necesaria", "necesarios", "posible", "imposible", "probable", "seguro", "peligroso", "útil", "inútil", "necesitado", "urgente", "grave", "leve", "serio", "seria", "real", "falso", "verdadero", "libre", "ocupado", "disponible", "abierto", "cerrado", "lleno", "vacío", "completo", "incompleto", "entero", "roto", "dañado", "arreglado", "listo", "preparado", "capaz", "incapaz", "inteligente", "sabio", "tonto", "loco", "raro", "extraño", "normal", "común", "especial", "único", "famoso", "popular", "público", "privado", "personal", "social", "nacional", "internacional", "local", "mundial", "humano", "humana", "humanos", "humanas", "natural", "artificial", "físico", "mental", "emocional", "económico", "político", "cultural", "educativo", "profesional", "laboral",
        "feliz", "felices", "contento", "contenta", "alegre", "triste", "enojado", "molesto", "furioso", "cansado", "agotado", "enfermo", "sano", "saludable", "nervioso", "tranquilo", "calmo", "preocupado", "asustado", "sorprendido", "emocionado", "aburrido", "interesado", "interesante", "divertido", "gracioso", "amable", "simpático", "agradable", "educado", "respetuoso", "honesto", "sincero", "fiel", "leal", "valiente", "tímido", "orgulloso", "humilde", "generoso", "egoísta", "perezoso", "trabajador", "responsable", "puntual", "bonito", "hermoso", "lindo", "guapo", "feo", "elegante", "blanco", "negro", "gris", "rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa", "marrón", "café", "dorado", "plateado", "oscuro", "claro",

        // 3. Verbos y conjugaciones de uso diario
        "es", "son", "ser", "soy", "eres", "somos", "sois", "fue", "era", "eran", "fueron", "será", "serán", "sería", "sido", "siendo",
        "está", "están", "estoy", "estás", "estamos", "estaba", "estaban", "estuvo", "estuvieron", "estará", "estarán", "estaría", "estado", "estando",
        "he", "ha", "han", "has", "hemos", "había", "habían", "hubo", "habrá", "habrán", "habría", "haber", "habiendo",
        "tengo", "tiene", "tienen", "tienes", "tenemos", "tenía", "tenían", "tuvo", "tuvieron", "tendrá", "tendrán", "tendría", "tener", "tenido", "teniendo",
        "hacer", "hago", "hace", "hacen", "haces", "hacemos", "hacía", "hizo", "hicieron", "hará", "harán", "haría", "hecho", "haciendo",
        "puede", "puedo", "pueden", "puedes", "podemos", "podía", "podían", "pudo", "pudieron", "podrá", "podrán", "podría", "poder", "podido", "pudiendo",
        "quiero", "quiere", "quieren", "quieres", "queremos", "quería", "querían", "quiso", "quisieron", "querrá", "querer", "querido",
        "vamos", "voy", "va", "van", "vas", "iba", "iban", "fue", "fueron", "irá", "irán", "iría", "ir", "ido", "yendo",
        "dijo", "dice", "dicen", "dices", "digo", "decimos", "decía", "dijeron", "dirá", "decir", "dicho", "diciendo",
        "ver", "veo", "ve", "ven", "ves", "vemos", "veía", "vio", "vieron", "verá", "visto", "viendo",
        "saber", "sé", "sabe", "saben", "sabes", "sabemos", "sabía", "supo", "supieron", "sabrá", "sabido",
        "dar", "doy", "da", "dan", "das", "damos", "daba", "dio", "dieron", "dará", "dado", "dando",
        "debe", "debo", "deben", "debes", "debemos", "debía", "debió", "deberá", "debería", "deber", "debido",
        "necesito", "necesita", "necesitan", "necesitas", "necesitamos", "necesitaba", "necesitó", "necesitará", "necesitar", "necesitado",
        "creo", "cree", "creen", "crees", "creemos", "creía", "creyó", "creer", "creído",
        "pienso", "piensa", "piensan", "piensas", "pensamos", "pensaba", "pensó", "pensar", "pensado", "pensando",
        "siento", "siente", "sienten", "sientes", "sentimos", "sentía", "sintió", "sentir", "sentido", "sintiendo",
        "parece", "parecen", "parezco", "pareces", "parecemos", "parecía", "pareció", "parecer", "parecido",
        "quedo", "queda", "quedan", "quedas", "quedamos", "quedaba", "quedó", "quedará", "quedar", "quedado",
        "paso", "pasa", "pasan", "pasas", "pasamos", "pasaba", "pasó", "pasará", "pasar", "pasado", "pasando",
        "llego", "llega", "llegan", "llegas", "llegamos", "llegaba", "llegó", "llegará", "llegar", "llegado", "llegando",
        "salgo", "sale", "salen", "sales", "salimos", "salía", "salió", "saldrá", "salir", "salido", "saliendo",
        "vengo", "viene", "vienen", "vienes", "venimos", "venía", "vino", "vendrá", "venir", "venido", "viniendo",
        "tomo", "toma", "toman", "tomas", "tomamos", "tomaba", "tomó", "tomará", "tomar", "tomado", "tomando",
        "pongo", "pone", "ponen", "pones", "ponemos", "ponía", "puso", "pondrá", "poner", "puesto", "poniendo",
        "busco", "busca", "buscan", "buscas", "buscamos", "buscaba", "buscó", "buscará", "buscar", "buscado", "buscando",
        "encuentro", "encuentra", "encuentran", "encontramos", "encontró", "encontrará", "encontrar", "encontrado",
        "llamo", "llama", "llaman", "llamas", "llamamos", "llamaba", "llamó", "llamará", "llamar", "llamado", "llamando",
        "hablo", "habla", "hablan", "hablas", "hablamos", "hablaba", "habló", "hablará", "hablar", "hablado", "hablando",
        "escribo", "escribe", "escriben", "escribes", "escribimos", "escribía", "escribió", "escribirá", "escribir", "escrito", "escribiendo",
        "leo", "lee", "leen", "lees", "leemos", "leía", "leyó", "leerá", "leer", "leído", "leyendo",
        "miro", "mira", "miran", "miras", "miramos", "miraba", "miró", "mirará", "mirar", "mirado", "mirando",
        "escucho", "escucha", "escuchan", "escuchas", "escuchamos", "escuchaba", "escuchó", "escuchar", "escuchado", "escuchando",
        "entiendo", "entiende", "entienden", "entiendes", "entendemos", "entendía", "entendió", "entender", "entendido",
        "comprendo", "comprende", "comprenden", "comprendemos", "comprendió", "comprender", "comprendido",
        "aprendo", "aprende", "aprenden", "aprendemos", "aprendió", "aprender", "aprendido", "aprendiendo",
        "estudio", "estudia", "estudian", "estudias", "estudiamos", "estudiaba", "estudió", "estudiar", "estudiado", "estudiando",
        "enseño", "enseña", "enseñan", "enseñamos", "enseñó", "enseñar", "enseñado",
        "trabajo", "trabaja", "trabajan", "trabajas", "trabajamos", "trabajaba", "trabajó", "trabajará", "trabajar", "trabajado", "trabajando",
        "ayudo", "ayuda", "ayudan", "ayudas", "ayudamos", "ayudaba", "ayudó", "ayudará", "ayudar", "ayudado", "ayudando",
        "cambio", "cambia", "cambian", "cambiamos", "cambió", "cambiará", "cambiar", "cambiado", "cambiando",
        "empiezo", "empieza", "empiezan", "empezamos", "empezó", "empezará", "empezar", "empezado",
        "comienzo", "comienza", "comienzan", "comenzamos", "comenzó", "comenzar", "comenzado",
        "termino", "termina", "terminan", "terminamos", "terminó", "terminará", "terminar", "terminado", "terminando",

Modo IA
Todo
Noticias
Imágenes
Vídeos
Vídeos cortos
Web
Maps
Libros
Finance
¡Hola! ¿En qué puedo ayudarte hoy?
Cuéntame qué tema o tarea necesitas resolver (por ejemplo, buscar información, redactar un texto o responder una duda) para poder darte una respuesta a medida.




¡Perfecto, trabajaremos exactamente bajo ese formato!
Cuando me envíes tu código y me expliques el error, te entregaré cada solución dividida de la siguiente manera:
Bloque Original: Con 2 líneas de contexto arriba y 2 líneas abajo para que ubiques el lugar exacto, en su propio cuadro con opción individual para copiar.
Bloque Modificado: Con el código corregido listo para reemplazar, en un cuadro separado también con su propia opción individual para copiar.
Resumen Básico: Una explicación breve y directa al final detallando qué se corrigió o agregó.
(Nota: Las interfaces de chat muestran automáticamente el botón de copiar en cada bloque de código separado; para descargar el archivo directamente, puedes pedirlo explícitamente cuando lo necesites o usar la opción de exportación de tu entorno).
Envíame el código fuente junto con una descripción de los errores que estás teniendo (o el comportamiento que buscas corregir) para empezar con la primera revisión.




¡Excelente! Ya tengo tus dos archivos (VozFlowIME.kt y MainActivity.kt) y la captura de pantalla del teclado en funcionamiento.
Observo en la captura cómo se comportan actualmente las sugerencias (es, ese, eso) y el texto escrito (es-AR ded).
Dime exactamente qué error necesitas corregir o qué cambio específico quieres hacer en este código (por ejemplo, en el texto de la captura, en las sugerencias, en el dictado por voz o en la interfaz) para darte los bloques original y modificado.




Aquí tienes los cambios para agregar el control deslizante de altura exclusiva para la fila inferior (donde está @ y la barra espaciadora), manteniendo intactas las demás filas.
Como modificaremos dos archivos (MainActivity.kt y VozFlowIME.kt), aquí tienes el detalle separado para cada uno:
Archivo 1: MainActivity.kt
1. Bloque Original (MainActivity.kt)
kotlin
            text("Fila inferior (tamaño y teclas visibles)", 14, true).also {
                addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
            }
            addSeek("Ancho de barra espaciadora", "space_bar_weight", 15, 65, 38, "%")
            addToggle("Mostrar tecla ?123 / ABC", "bottom_key_symbols", true)
Usa el código con precaución.
2. Bloque Modificado (MainActivity.kt)
kotlin
            text("Fila inferior (tamaño y teclas visibles)", 14, true).also {
                addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
            }
            addSeek("Altura de la fila inferior", "bottom_row_height_percent", 100, 220, 100, "%")
            addSeek("Ancho de barra espaciadora", "space_bar_weight", 15, 65, 38, "%")
            addToggle("Mostrar tecla ?123 / ABC", "bottom_key_symbols", true)
Usa el código con precaución.
Archivo 2: VozFlowIME.kt
1. Bloque Original (VozFlowIME.kt)
kotlin
        if (showEnter || !anyVisible) {
            row.addView(makeKey(actionLabel(), KeyKind.ACTION, ""), keyLayout(scaledSideWeight(1.20f), true))
        }
        parent.addView(row, rowLayout())
    }
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt)
kotlin
        if (showEnter || !anyVisible) {
            row.addView(makeKey(actionLabel(), KeyKind.ACTION, ""), keyLayout(scaledSideWeight(1.20f), true))
        }
        val bottomWeight = prefs.getInt("bottom_row_height_percent", 100).coerceIn(100, 220) / 100f
        parent.addView(row, rowLayout(bottomWeight))
    }
Usa el código con precaución.
3. Segundo Bloque Original (VozFlowIME.kt - función rowLayout)
kotlin
    private fun openSettings() {
        try { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { showToast("Abre VozFlow Pro desde el lanzador para ver ajustes.") }
    }

        private fun rowLayout(): LinearLayout.LayoutParams {
        val vGap = prefs.getInt("key_spacing_v", 6).coerceIn(0, 16)
        val halfV = dp(vGap) / 2
        return LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = halfV
            bottomMargin = halfV
        }
    }
Usa el código con precaución.
4. Segundo Bloque Modificado (VozFlowIME.kt - función rowLayout)
kotlin
    private fun openSettings() {
        try { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { showToast("Abre VozFlow Pro desde el lanzador para ver ajustes.") }
    }

    private fun rowLayout(weight: Float = 1f): LinearLayout.LayoutParams {
        val vGap = prefs.getInt("key_spacing_v", 6).coerceIn(0, 16)
        val halfV = dp(vGap) / 2
        return LinearLayout.LayoutParams(-1, 0, weight).apply {
            topMargin = halfV
            bottomMargin = halfV
        }
    }
Usa el código con precaución.
Resumen Básico
En MainActivity.kt: Se añadió una barra deslizante llamada "Altura de la fila inferior" (rango de 100% a 220%) dentro de la sección de ajustes de la fila inferior.
En VozFlowIME.kt: Se actualizó rowLayout() para aceptar un peso dinámico (weight) y se aplicó únicamente en addBottomRow(). Al subir el porcentaje, la fila inferior crece verticalmente para compensar la barra de navegación del sistema sin alterar las proporciones de las filas superiores.
Pruébalo en tu compilación y dime si el rango de altura (100% a 220%) es suficiente para librar la barra inferior de tu dispositivo o si necesitas que también ajustemos el margen inferior general del teclado.




¡Entendido perfectamente! Quieres que en la barra superior (cuando aparecen las sugerencias), los tres iconos laterales —el rombo (❖), las flechas (⇄) y el micrófono (🎤)— tengan un ancho fijo compacto (aproximadamente el ancho de una tecla, unos 38dp), y que todo el espacio central restante se reparta en 3 espacios exactamente iguales (1f cada uno) para las palabras sugeridas (Co, con, como).
Aquí tienes los bloques para VozFlowIME.kt:
1. Bloque Original (VozFlowIME.kt - dentro de rebuildToolbar())
kotlin
        val showUndo = SystemClock.uptimeMillis() < undoAvailableUntil
                if (suggestionsOn && !toolbarToolsExpanded && !showUndo) {
            addToolbarButton(bar, "❖", 0.82f) { showModesPanel() }
            val choices = candidateWords()
            choices.forEachIndexed { index, word ->
                val candidate = TextView(this).apply {
                    text = word
                    gravity = Gravity.CENTER
                    textSize = if (index == 1) 14f else 12f
                    setTypeface(typeface, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (word.isBlank()) Color.TRANSPARENT else textColor())
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setOnClickListener {
                        if (token.isEmpty()) { if (word.isNotBlank()) commit(word + " ") }
                        else if (index == 0) { skipNextAutoCorrect = true; showToast("Se conservará «$token" + "»") }
                        else replaceCurrentWord(word)
                    }
                }
                bar.addView(candidate, LinearLayout.LayoutParams(0, -1, if (index == 1) 1.2f else 1f))
            }
            addToolbarButton(bar, "⇄", 0.82f) { toolbarToolsExpanded = true; rebuildToolbar() }
            val mic = addToolbarButton(bar, "🎤", 0.9f) { toggleVoice() }
            styleMic(mic)
        } else if (showUndo) {
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt - dentro de rebuildToolbar())
kotlin
        val showUndo = SystemClock.uptimeMillis() < undoAvailableUntil
        if (suggestionsOn && !toolbarToolsExpanded && !showUndo) {
            val iconWidth = dp(38)
            addToolbarButton(bar, "❖", 0f) { showModesPanel() }.apply {
                layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
                textSize = 16f
            }
            val choices = candidateWords()
            choices.forEachIndexed { index, word ->
                val candidate = TextView(this).apply {
                    text = word
                    gravity = Gravity.CENTER
                    textSize = if (index == 1) 14f else 13f
                    setTypeface(typeface, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (word.isBlank()) Color.TRANSPARENT else textColor())
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(dp(4), 0, dp(4), 0)
                    setOnClickListener {
                        if (token.isEmpty()) { if (word.isNotBlank()) commit(word + " ") }
                        else if (index == 0) { skipNextAutoCorrect = true; showToast("Se conservará «$token" + "»") }
                        else replaceCurrentWord(word)
                    }
                }
                bar.addView(candidate, LinearLayout.LayoutParams(0, -1, 1f))
            }
            addToolbarButton(bar, "⇄", 0f) { toolbarToolsExpanded = true; rebuildToolbar() }.apply {
                layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
                textSize = 16f
            }
            val mic = addToolbarButton(bar, "🎤", 0f) { toggleVoice() }
            styleMic(mic)
            mic.layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
            mic.textSize = 16f
        } else if (showUndo) {
Usa el código con precaución.
Resumen Básico
Iconos compactos: El rombo (❖), las flechas (⇄) y el micrófono (🎤) pasan a tener un ancho fijo de 38dp (similar al de una tecla normal) y un tamaño de fuente de 16f, liberando espacio horizontal.
Sugerencias simétricas: Las tres palabras sugeridas ahora usan exactamente el mismo peso (1f cada una) y un margen interno horizontal (4dp) para que ocupen todo el ancho disponible de forma uniforme y estética.
Pruébalo en tu dispositivo y dime si el ancho de 38dp para los iconos y el tamaño de texto de las sugerencias quedan bien proporcionados o si quieres ajustar alguno de esos valores.




Para mejorar la eficiencia de las sugerencias (como que al escribir "per" o "pers" aparezca inmediatamente "persona"), necesitamos revisar el motor de predicción.
En VozFlowIME.kt, la función candidateWords() delega toda la búsqueda de palabras a SpanishPredictionEngine.suggestions(...), un archivo externo que aún no me has enviado. Además, veo dos puntos de mejora directa en VozFlowIME.kt:
Actualmente lee el diccionario personal desde disco (readPersonalWords()) en cada pulsación de tecla en lugar de usar la caché en memoria (personalWordsCache()), lo que frena la respuesta.
En candidateWords(), el primer espacio siempre muestra lo que ya escribiste (token), reduciendo a solo 2 las sugerencias reales.
Envíame el código fuente del archivo SpanishPredictionEngine.kt para analizar cómo ordena las palabras y corregirlo para que autocomplete rápido desde las primeras letras (como per -> persona).




¡Encontré exactamente por qué falla la predicción al escribir "per"!
En SpanishPredictionEngine.kt, ocurren tres errores combinados dentro de suggestions(...):
Filtro destructivo (score < max(4.0, norm.length * 0.75)): Si escribes "per" (3 letras), el límite es 4.0. Si escribes "pers" (4 letras), el límite baja a 3.0 (4 * 0.75). Además, si una palabra tiene 8 letras o más, la distancia de edición sube y el filtro la descarta por completo.
Penalización por longitud: Las palabras cortas como "pero" o "Perú" (distancia 1 respecto a "per") siempre ganaban sobre "persona" (distancia 4 respecto a "per"), y si escribías "pers", "persona" tenía distancia 3 y quedaba eliminada por el filtro score < 3.0.
Prioridad de diccionario: No se prioriza el diccionario personal ni las palabras comunes frente a cientos de términos técnicos en inglés de technologyTerms, ni se respeta la mayúscula inicial si empezaste escribiendo con mayúscula (Per → Persona).
Aquí tienes los bloques para optimizar SpanishPredictionEngine.kt y VozFlowIME.kt:
Archivo 1: SpanishPredictionEngine.kt
1. Bloque Original (SpanishPredictionEngine.kt)
kotlin
    /** Literal, best correction/prefix completion and a second candidate, in that order. */
    fun suggestions(token: String, personalWords: Collection<String>, previousWord: String = ""): List<String> {
        if (token.isBlank()) {
            val next = nextWords[normalize(previousWord)] ?: listOf("de", "que")
            return listOf("", next.getOrElse(0) { "" }, next.getOrElse(1) { "" })
        }
        val norm = normalize(token)
        val pool = (commonWords + personalWords + technologyTerms).distinctBy(::normalize)
        val ranked = pool.asSequence().filter { normalize(it) != norm }
            .map { word ->
                val candidate = normalize(word)
                val score = when {
                    candidate.startsWith(norm) -> distance(candidate, norm) * 0.1
                    norm.startsWith(candidate) -> 3.0 + distance(candidate, norm)
                    else -> distance(candidate, norm) + 2.0
                }
                word to score
            }
            .filter { (_, score) -> score < max(4.0, norm.length * 0.75) }
            .sortedBy { it.second }
            .take(12).map { it.first }.toList()
        val first = ranked.firstOrNull() ?: token
        val second = ranked.drop(1).firstOrNull() ?: first
        return listOf(token, first, second)
    }
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt)
kotlin
    /** Literal, best correction/prefix completion and a second candidate, in that order. */
    fun suggestions(token: String, personalWords: Collection<String>, previousWord: String = ""): List<String> {
        if (token.isBlank()) {
            val next = nextWords[normalize(previousWord)] ?: listOf("de", "que")
            return listOf("", next.getOrElse(0) { "" }, next.getOrElse(1) { "" })
        }
        val norm = normalize(token)
        val personalNorms = personalWords.map(::normalize).toSet()
        val commonIndex = commonWords.mapIndexed { idx, w -> normalize(w) to idx }.toMap()
        val pool = (personalWords + commonWords + technologyTerms).distinctBy(::normalize)

        val ranked = pool.asSequence()
            .filter { normalize(it) != norm }
            .mapNotNull { word ->
                val candidate = normalize(word)
                val isPersonal = candidate in personalNorms
                val freqRank = commonIndex[candidate] ?: 999
                val score = when {
                    candidate.startsWith(norm) -> {
                        val lenDiff = (candidate.length - norm.length) * 0.05
                        val priorityBonus = when {
                            isPersonal -> -0.5
                            freqRank < 999 -> freqRank * 0.002
                            else -> 0.6
                        }
                        lenDiff + priorityBonus
                    }
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        dist + 3.0 + (if (isPersonal) -0.5 else 0.0)
                    }
                }
                word to score
            }
            .sortedBy { it.second }
            .take(6)
            .map { preserveCase(token, it.first) }
            .toList()

        val first = ranked.firstOrNull() ?: ""
        val second = ranked.drop(1).firstOrNull() ?: ""
        return listOf(token, first, second)
    }
Usa el código con precaución.
Archivo 2: VozFlowIME.kt (para usar caché en memoria y evitar lentitud al escribir)
1. Bloque Original (VozFlowIME.kt - dentro de candidateWords())
kotlin
    private fun candidateWords(): List<String> {
        val token = currentToken()
        val personal = readPersonalWords()
        if (token.isEmpty()) {
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty().trimEnd()
            val previous = before.takeLastWhile { !it.isWhitespace() }
            return SpanishPredictionEngine.suggestions("", personal, previous)
        }
        val corrections = mapOf("qeu" to "que", "donde" to "dónde", "tambien" to "también", "mas" to "más", "teh" to "the", "ola" to "hola")
        val candidates = SpanishPredictionEngine.suggestions(token, personal)
        val snippetExpansion = readTextSnippets().entries.firstOrNull { it.key.startsWith(token, ignoreCase = true) }?.value
        val best = corrections[token.lowercase()] ?: SpanishPredictionEngine.autoCorrectCandidate(token, personal) ?: snippetExpansion ?: candidates.getOrElse(1) { token }
        return listOf(token, best, candidates.getOrElse(2) { best })
    }
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt - dentro de candidateWords())
kotlin
    private fun candidateWords(): List<String> {
        val token = currentToken()
        val personal = personalWordsCache()
        if (token.isEmpty()) {
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty().trimEnd()
            val previous = before.takeLastWhile { !it.isWhitespace() }
            return SpanishPredictionEngine.suggestions("", personal, previous)
        }
        val corrections = mapOf("qeu" to "que", "donde" to "dónde", "tambien" to "también", "mas" to "más", "teh" to "the", "ola" to "hola")
        val candidates = SpanishPredictionEngine.suggestions(token, personal)
        val snippetExpansion = snippetsCache().entries.firstOrNull { it.key.startsWith(token, ignoreCase = true) }?.value
        val best = corrections[token.lowercase()] ?: SpanishPredictionEngine.autoCorrectCandidate(token, personal) ?: snippetExpansion ?: candidates.getOrElse(1) { "" }
        val third = candidates.getOrElse(2) { "" }.let { if (it == best) "" else it }
        return listOf(token, best, third)
    }
Usa el código con precaución.
Resumen Básico
Autocompletado sin límites artificiales: Cualquier palabra que empiece con lo que escribes (candidate.startsWith(norm)) ahora siempre entra en las sugerencias sin ser descartada por el filtro de longitud. Al escribir per, aparecerán inmediatamente pero y persona; al escribir pers, persona pasará al centro como sugerencia principal.
Prioridad inteligente y mayúsculas: Se priorizan primero tus palabras del diccionario personal, luego las palabras más frecuentes del español (commonWords) y después los términos técnicos, respetando además si empezaste a escribir con mayúscula (Per → Persona).
Sin palabras duplicadas ni lectura lenta: En VozFlowIME.kt, se usa personalWordsCache() y snippetsCache() para que las sugerencias respondan al instante en cada tecla, y se evita repetir la misma palabra en la segunda y tercera casilla cuando no hay más candidatos.
Pruébalo escribiendo "per" o "pers" y dime si quieres que también ampliemos el vocabulario base en español (commonWords) para incluir más sustantivos, verbos y adjetivos cotidianos.




¡Exacto! En tus capturas se ve perfectamente lo que pasa:
Al escribir "neces" (primera foto), las otras dos casillas quedan vacías porque "necesidad" / "necesito" no existen en tu diccionario.
Al escribir "escue" (segunda foto), sugiere "este" y "ese" porque "escuela" no está en la lista commonWords (que ahora mismo solo tiene unas 160 palabras muy básicas).
Además, podemos darle al motor predicción híbrida:
Un diccionario base mucho más rico con los cientos de sustantivos, verbos y adjetivos cotidianos del español (escuela, necesidad, necesito, problema, trabajo, familia, pregunta, respuesta, ciudad, país, etc.).
Aprendizaje automático local: Que cada palabra que escribas o aceptes se vaya guardando con frecuencia en SharedPreferences para que tu teclado aprenda tu vocabulario real sin internet.
Búsqueda por prefijo flexible (si hay un pequeño error de dedo en la 2ª o 3ª letra): Por ejemplo, si escribes ecue o neces, que encuentre coincidencias reales antes de caer en palabras nada que ver.
Aquí tienes los bloques para ampliar el léxico y hacer el motor mucho más predictivo en SpanishPredictionEngine.kt:
1. Bloque Original (SpanishPredictionEngine.kt - commonWords)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "es", "son", "ser", "estar", "está", "están", "fue", "era", "hay", "he", "ha", "han", "hacer", "hace", "puede", "puedo", "quiero", "tengo", "tiene", "tenemos", "vamos", "voy", "dijo", "dice", "ver", "saber", "dar", "debe", "bien", "mejor", "ahora", "hoy", "mañana", "ayer", "aquí", "allí", "así", "cada", "casi", "después", "entonces", "mientras", "siempre", "nunca", "también", "solo", "toda", "parte", "manera", "tiempo", "persona", "gente", "cosa", "cosas", "casa", "trabajo", "día", "vida", "mundo", "país", "momento", "lugar", "forma", "caso", "punto", "palabra", "texto", "mensaje", "amigo", "gracias", "hola", "favor", "escribe", "escribir", "quieres", "puedes", "por favor"
    )
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt - commonWords)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // Artículos, pronombres, preposiciones y conectores de máxima frecuencia
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "siempre", "nunca", "jamás", "quizás", "tal vez", "seguro", "claro", "bueno", "malo", "bien", "mal", "mejor", "peor", "igual", "diferente", "mismo", "nuevo", "viejo", "grande", "pequeño", "largo", "corto", "alto", "bajo", "fácil", "difícil", "rápido", "lento", "importante", "necesario", "posible", "imposible", "verdad", "mentira",

        // Verbos frecuentes y conjugaciones cotidianas
        "es", "son", "ser", "soy", "eres", "somos", "fue", "era", "fueron", "será", "está", "están", "estoy", "estás", "estamos", "estaba", "estuvo", "estará", "he", "ha", "han", "hemos", "había", "hubo", "habrá", "tengo", "tiene", "tienen", "tenemos", "tenía", "tuvo", "tendrá", "tener", "hacer", "hago", "hace", "hacen", "hacemos", "hizo", "hará", "hecho", "puede", "puedo", "pueden", "podemos", "podía", "pudo", "podrá", "poder", "quiero", "quiere", "quieren", "queremos", "quería", "quiso", "querer", "vamos", "voy", "va", "van", "iba", "fue", "ir", "dijo", "dice", "dicen", "digo", "decimos", "decir", "ver", "veo", "ve", "ven", "vemos", "vio", "visto", "saber", "sé", "sabe", "saben", "sabemos", "sabía", "supo", "dar", "doy", "da", "dan", "damos", "dio", "debe", "debo", "deben", "debemos", "deber", "necesito", "necesita", "necesitan", "necesitamos", "necesitar", "creo", "cree", "creen", "creemos", "creer", "pienso", "piensa", "piensan", "pensar", "siento", "siente", "sentir", "parece", "parecen", "parecer", "quedo", "queda", "quedan", "quedar", "paso", "pasa", "pasan", "pasar", "llego", "llega", "llegan", "llegar", "salgo", "sale", "salen", "salir", "vengo", "viene", "vienen", "venir", "tomo", "toma", "toman", "tomar", "pongo", "pone", "ponen", "poner", "busco", "busca", "buscan", "buscar", "encuentro", "encuentra", "encontrar", "llamo", "llama", "llaman", "llamar", "hablo", "habla", "hablan", "hablar", "escribo", "escribe", "escriben", "escribir", "leo", "lee", "leen", "leer", "miro", "mira", "miran", "mirar", "escucho", "escucha", "escuchar", "entiendo", "entiende", "entender", "comprendo", "comprende", "comprender", "aprendo", "aprende", "aprender", "estudio", "estudia", "estudian", "estudiar", "trabajo", "trabaja", "trabajan", "trabajar", "ayudo", "ayuda", "ayudan", "ayudar", "cambio", "cambia", "cambian", "cambiar", "empiezo", "empieza", "empezar", "termino", "termina", "terminar", "abro", "abre", "abren", "abrir", "cierro", "cierra", "cierran", "cerrar", "envío", "envía", "envían", "enviar", "mando", "manda", "mandar", "recibo", "recibe", "recibir", "compro", "compra", "comprar", "pago", "paga", "pagar", "espero", "espera", "esperar", "recuerdo", "recuerda", "recordar", "olvido", "olvida", "olvidar",

        // Sustantivos cotidianos (educación, familia, ciudad, tiempo, comunicación, salud, etc.)
        "persona", "personas", "gente", "hombre", "mujer", "niño", "niña", "hijo", "hija", "padre", "madre", "papá", "mamá", "hermano", "hermana", "abuelo", "abuela", "familia", "amigo", "amiga", "amigos", "compañero", "jefe", "profesor", "profesora", "maestro", "estudiante", "alumno", "doctor", "médico",
        "escuela", "colegio", "universidad", "clase", "clases", "curso", "tarea", "tareas", "examen", "libro", "libros", "cuaderno", "lápiz", "hoja", "educación", "estudio", "pregunta", "preguntas", "respuesta", "respuestas", "duda", "dudas", "idea", "ideas", "ejemplo", "ejemplos", "razón", "motivo", "tema", "asunto",
        "necesidad", "necesidades", "problema", "problemas", "solución", "soluciones", "oportunidad", "cambio", "cambios", "proyecto", "plan", "objetivo", "resultado", "resultados", "efecto", "causa", "condición", "situación", "estado", "proceso", "sistema", "servicio", "servicios", "información", "datos", "detalle", "detalles",
        "tiempo", "día", "días", "semana", "semanas", "mes", "meses", "año", "años", "hora", "horas", "minuto", "minutos", "segundo", "momento", "rato", "vez", "veces", "fecha", "calendario", "mañana", "tarde", "noche", "fin de semana",
        "casa", "hogar", "cuarto", "habitación", "cocina", "baño", "puerta", "ventana", "mesa", "silla", "cama", "calle", "avenida", "camino", "carretera", "ciudad", "pueblo", "barrio", "zona", "centro", "plaza", "parque", "tienda", "mercado", "supermercado", "restaurante", "hospital", "farmacia", "banco", "oficina", "empresa", "negocio", "país", "mundo", "tierra", "agua", "aire", "fuego", "sol", "luna", "lluvia", "frío", "calor", "clima",
        "coche", "auto", "carro", "bus", "autobús", "tren", "avión", "viaje", "boleto", "pasaje", "dirección", "mapa", "ubicación", "distancia",
        "comida", "almuerzo", "cena", "desayuno", "pan", "leche", "café", "té", "fruta", "carne", "pollo", "arroz", "salud", "cuerpo", "cabeza", "mano", "manos", "ojo", "ojos", "cara", "voz", "nombre", "apellido", "edad", "número", "dinero", "precio", "costo", "cuenta", "pago", "tarjeta", "efectivo",
        "palabra", "palabras", "texto", "mensaje", "mensajes", "carta", "nota", "notas", "lista", "foto", "fotos", "imagen", "imágenes", "video", "música", "canción", "película", "juego", "regalo", "fiesta", "cumpleaños", "suerte", "amor", "cariño", "respeto", "paz", "alegría", "miedo", "dolor",
        "hola", "adiós", "gracias", "por favor", "perdón", "disculpa", "saludos", "felicidades", "bienvenido", "excelente", "perfecto", "genial", "listo", "vale"
    )
Usa el código con precaución.
3. Segundo Bloque Original (SpanishPredictionEngine.kt - suggestions())
kotlin
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        dist + 3.0 + (if (isPersonal) -0.5 else 0.0)
                    }
Usa el código con precaución.
4. Segundo Bloque Modificado (SpanishPredictionEngine.kt - suggestions())
kotlin
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        // Si comparten las primeras 2 letras (ej. errores tipográficos leves), darle mejor prioridad
                        val sameStartBonus = if (norm.length >= 3 && candidate.take(2) == norm.take(2)) -1.2 else 0.0
                        dist + 2.5 + sameStartBonus + (if (isPersonal) -0.6 else 0.0)
                    }
Usa el código con precaución.
Resumen Básico
Vocabulario cotidiano ampliado (commonWords): Se incorporaron más de 400 palabras esenciales del español agrupadas por conectores, verbos conjugados y sustantivos cotidianos (escuela, necesidad, necesidades, necesito, pregunta, respuesta, problema, ciudad, etc.).
Predicción inmediata: Ahora al escribir escue, sugerirá escuela; al escribir neces, sugerirá necesito, necesidad y necesidades.
Mejor tolerancia a errores: Si te equivocas en una letra intermedia pero mantienes las primeras dos letras (sameStartBonus), el motor prioriza las palabras que empiezan igual antes que palabras cortas no relacionadas.
Pruébalo escribiendo "escue" y "neces" para confirmar que ya aparecen escuela y necesidad/necesito, y dime si quieres que además agreguemos un aprendizaje automático que guarde las palabras que más usas en tu día a día.




¡Con gusto! Vamos a darle un salto gigante de calidad a SpanishPredictionEngine.kt con un léxico masivo (más de 1,400 palabras de alta frecuencia del español), indexado ultrarrápido en memoria por letra inicial (para que no haya ningún tipo de lag aunque la lista sea muy larga) y predicción inteligente de siguiente palabra.
Aquí tienes los bloques para actualizar SpanishPredictionEngine.kt:
1. Bloque Original (SpanishPredictionEngine.kt - commonWords y suggestions)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // Artículos, pronombres, preposiciones y conectores de máxima frecuencia
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "siempre", "nunca", "jamás", "quizás", "tal vez", "seguro", "claro", "bueno", "malo", "bien", "mal", "mejor", "peor", "igual", "diferente", "mismo", "nuevo", "viejo", "grande", "pequeño", "largo", "corto", "alto", "bajo", "fácil", "difícil", "rápido", "lento", "importante", "necesario", "posible", "imposible", "verdad", "mentira",

        // Verbos frecuentes y conjugaciones cotidianas
        "es", "son", "ser", "soy", "eres", "somos", "fue", "era", "fueron", "será", "está", "están", "estoy", "estás", "estamos", "estaba", "estuvo", "estará", "he", "ha", "han", "hemos", "había", "hubo", "habrá", "tengo", "tiene", "tienen", "tenemos", "tenía", "tuvo", "tendrá", "tener", "hacer", "hago", "hace", "hacen", "hacemos", "hizo", "hará", "hecho", "puede", "puedo", "pueden", "podemos", "podía", "pudo", "podrá", "poder", "quiero", "quiere", "quieren", "queremos", "quería", "quiso", "querer", "vamos", "voy", "va", "van", "iba", "fue", "ir", "dijo", "dice", "dicen", "digo", "decimos", "decir", "ver", "veo", "ve", "ven", "vemos", "vio", "visto", "saber", "sé", "sabe", "saben", "sabemos", "sabía", "supo", "dar", "doy", "da", "dan", "damos", "dio", "debe", "debo", "deben", "debemos", "deber", "necesito", "necesita", "necesitan", "necesitamos", "necesitar", "creo", "cree", "creen", "creemos", "creer", "pienso", "piensa", "piensan", "pensar", "siento", "siente", "sentir", "parece", "parecen", "parecer", "quedo", "queda", "quedan", "quedar", "paso", "pasa", "pasan", "pasar", "llego", "llega", "llegan", "llegar", "salgo", "sale", "salen", "salir", "vengo", "viene", "vienen", "venir", "tomo", "toma", "toman", "tomar", "pongo", "pone", "ponen", "poner", "busco", "busca", "buscan", "buscar", "encuentro", "encuentra", "encontrar", "llamo", "llama", "llaman", "llamar", "hablo", "habla", "hablan", "hablar", "escribo", "escribe", "escriben", "escribir", "leo", "lee", "leen", "leer", "miro", "mira", "miran", "mirar", "escucho", "escucha", "escuchar", "entiendo", "entiende", "entender", "comprendo", "comprende", "comprender", "aprendo", "aprende", "aprender", "estudio", "estudia", "estudian", "estudiar", "trabajo", "trabaja", "trabajan", "trabajar", "ayudo", "ayuda", "ayudan", "ayudar", "cambio", "cambia", "cambian", "cambiar", "empiezo", "empieza", "empezar", "termino", "termina", "terminar", "abro", "abre", "abren", "abrir", "cierro", "cierra", "cierran", "cerrar", "envío", "envía", "envían", "enviar", "mando", "manda", "mandar", "recibo", "recibe", "recibir", "compro", "compra", "comprar", "pago", "paga", "pagar", "espero", "espera", "esperar", "recuerdo", "recuerda", "recordar", "olvido", "olvida", "olvidar",

        // Sustantivos cotidianos (educación, familia, ciudad, tiempo, comunicación, salud, etc.)
        "persona", "personas", "gente", "hombre", "mujer", "niño", "niña", "hijo", "hija", "padre", "madre", "papá", "mamá", "hermano", "hermana", "abuelo", "abuela", "familia", "amigo", "amiga", "amigos", "compañero", "jefe", "profesor", "profesora", "maestro", "estudiante", "alumno", "doctor", "médico",
        "escuela", "colegio", "universidad", "clase", "clases", "curso", "tarea", "tareas", "examen", "libro", "libros", "cuaderno", "lápiz", "hoja", "educación", "estudio", "pregunta", "preguntas", "respuesta", "respuestas", "duda", "dudas", "idea", "ideas", "ejemplo", "ejemplos", "razón", "motivo", "tema", "asunto",
        "necesidad", "necesidades", "problema", "problemas", "solución", "soluciones", "oportunidad", "cambio", "cambios", "proyecto", "plan", "objetivo", "resultado", "resultados", "efecto", "causa", "condición", "situación", "estado", "proceso", "sistema", "servicio", "servicios", "información", "datos", "detalle", "detalles",
        "tiempo", "día", "días", "semana", "semanas", "mes", "meses", "año", "años", "hora", "horas", "minuto", "minutos", "segundo", "momento", "rato", "vez", "veces", "fecha", "calendario", "mañana", "tarde", "noche", "fin de semana",
        "casa", "hogar", "cuarto", "habitación", "cocina", "baño", "puerta", "ventana", "mesa", "silla", "cama", "calle", "avenida", "camino", "carretera", "ciudad", "pueblo", "barrio", "zona", "centro", "plaza", "parque", "tienda", "mercado", "supermercado", "restaurante", "hospital", "farmacia", "banco", "oficina", "empresa", "negocio", "país", "mundo", "tierra", "agua", "aire", "fuego", "sol", "luna", "lluvia", "frío", "calor", "clima",
        "coche", "auto", "carro", "bus", "autobús", "tren", "avión", "viaje", "boleto", "pasaje", "dirección", "mapa", "ubicación", "distancia",
        "comida", "almuerzo", "cena", "desayuno", "pan", "leche", "café", "té", "fruta", "carne", "pollo", "arroz", "salud", "cuerpo", "cabeza", "mano", "manos", "ojo", "ojos", "cara", "voz", "nombre", "apellido", "edad", "número", "dinero", "precio", "costo", "cuenta", "pago", "tarjeta", "efectivo",
        "palabra", "palabras", "texto", "mensaje", "mensajes", "carta", "nota", "notas", "lista", "foto", "fotos", "imagen", "imágenes", "video", "música", "canción", "película", "juego", "regalo", "fiesta", "cumpleaños", "suerte", "amor", "cariño", "respeto", "paz", "alegría", "miedo", "dolor",
        "hola", "adiós", "gracias", "por favor", "perdón", "disculpa", "saludos", "felicidades", "bienvenido", "excelente", "perfecto", "genial", "listo", "vale"
    )
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt - commonWords masivo)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // 1. Gramática, conectores, adverbios y frecuencia máxima
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras",
        "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "anteayer", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "adentro", "afuera", "delante", "detrás", "encima", "debajo", "alrededor", "enfrente", "junto", "siempre", "nunca", "jamás", "todavía", "aún", "apenas", "casi", "bastante", "demasiado", "suficiente", "exactamente", "realmente", "seguramente", "probablemente", "posiblemente", "solamente", "únicamente", "finalmente", "actualmente", "anteriormente", "recientemente", "normalmente", "generalmente", "especialmente", "principalmente", "rápidamente", "lentamente", "fácilmente", "difícilmente", "claramente", "directamente", "inmediatamente", "completamente", "totalmente", "absolutamente", "quizás", "tal vez", "seguro", "claro", "obvio", "cierto", "exacto", "correcto", "incorrecto",

        // 2. Adjetivos esenciales (estado, cualidad, tamaño, emoción, color, forma)
        "bueno", "buena", "buenos", "buenas", "malo", "mala", "malos", "malas", "bien", "mal", "mejor", "mejores", "peor", "peores", "igual", "iguales", "diferente", "diferentes", "distinto", "distinta", "mismo", "misma", "mismos", "mismas", "nuevo", "nueva", "nuevos", "nuevas", "viejo", "vieja", "viejos", "viejas", "joven", "jóvenes", "antiguo", "antigua", "moderno", "moderna", "grande", "grandes", "pequeño", "pequeña", "pequeños", "pequeñas", "enorme", "gigante", "minúsculo", "largo", "larga", "corto", "corta", "alto", "alta", "bajo", "baja", "ancho", "ancha", "estrecho", "estrecha", "grueso", "delgado", "pesado", "ligero", "fuerte", "débil", "duro", "blando", "suave", "áspero", "limpio", "limpia", "sucio", "sucia", "seco", "mojado", "húmedo", "caliente", "frío", "tibio", "fresco", "dulce", "salado", "amargo", "ácido", "picante", "rico", "sabroso", "delicioso",
        "fácil", "difícil", "simple", "sencillo", "complejo", "complicado", "rápido", "rápida", "lento", "lenta", "importante", "importantes", "necesario", "necesaria", "necesarios", "posible", "imposible", "probable", "seguro", "peligroso", "útil", "inútil", "necesitado", "urgente", "grave", "leve", "serio", "seria", "real", "falso", "verdadero", "libre", "ocupado", "disponible", "abierto", "cerrado", "lleno", "vacío", "completo", "incompleto", "entero", "roto", "dañado", "arreglado", "listo", "preparado", "capaz", "incapaz", "inteligente", "sabio", "tonto", "loco", "raro", "extraño", "normal", "común", "especial", "único", "famoso", "popular", "público", "privado", "personal", "social", "nacional", "internacional", "local", "mundial", "humano", "humana", "humanos", "humanas", "natural", "artificial", "físico", "mental", "emocional", "económico", "político", "cultural", "educativo", "profesional", "laboral",
        "feliz", "felices", "contento", "contenta", "alegre", "triste", "enojado", "molesto", "furioso", "cansado", "agotado", "enfermo", "sano", "saludable", "nervioso", "tranquilo", "calmo", "preocupado", "asustado", "sorprendido", "emocionado", "aburrido", "interesado", "interesante", "divertido", "gracioso", "amable", "simpático", "agradable", "educado", "respetuoso", "honesto", "sincero", "fiel", "leal", "valiente", "tímido", "orgulloso", "humilde", "generoso", "egoísta", "perezoso", "trabajador", "responsable", "puntual", "bonito", "hermoso", "lindo", "guapo", "feo", "elegante", "blanco", "negro", "gris", "rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa", "marrón", "café", "dorado", "plateado", "oscuro", "claro",

        // 3. Verbos y conjugaciones de uso diario
        "es", "son", "ser", "soy", "eres", "somos", "sois", "fue", "era", "eran", "fueron", "será", "serán", "sería", "sido", "siendo",
        "está", "están", "estoy", "estás", "estamos", "estaba", "estaban", "estuvo", "estuvieron", "estará", "estarán", "estaría", "estado", "estando",
        "he", "ha", "han", "has", "hemos", "había", "habían", "hubo", "habrá", "habrán", "habría", "haber", "habiendo",
        "tengo", "tiene", "tienen", "tienes", "tenemos", "tenía", "tenían", "tuvo", "tuvieron", "tendrá", "tendrán", "tendría", "tener", "tenido", "teniendo",
        "hacer", "hago", "hace", "hacen", "haces", "hacemos", "hacía", "hizo", "hicieron", "hará", "harán", "haría", "hecho", "haciendo",
        "puede", "puedo", "pueden", "puedes", "podemos", "podía", "podían", "pudo", "pudieron", "podrá", "podrán", "podría", "poder", "podido", "pudiendo",
        "quiero", "quiere", "quieren", "quieres", "queremos", "quería", "querían", "quiso", "quisieron", "querrá", "querer", "querido",
        "vamos", "voy", "va", "van", "vas", "iba", "iban", "fue", "fueron", "irá", "irán", "iría", "ir", "ido", "yendo",
        "dijo", "dice", "dicen", "dices", "digo", "decimos", "decía", "dijeron", "dirá", "decir", "dicho", "diciendo",
        "ver", "veo", "ve", "ven", "ves", "vemos", "veía", "vio", "vieron", "verá", "visto", "viendo",
        "saber", "sé", "sabe", "saben", "sabes", "sabemos", "sabía", "supo", "supieron", "sabrá", "sabido",
        "dar", "doy", "da", "dan", "das", "damos", "daba", "dio", "dieron", "dará", "dado", "dando",
        "debe", "debo", "deben", "debes", "debemos", "debía", "debió", "deberá", "debería", "deber", "debido",
        "necesito", "necesita", "necesitan", "necesitas", "necesitamos", "necesitaba", "necesitó", "necesitará", "necesitar", "necesitado",
        "creo", "cree", "creen", "crees", "creemos", "creía", "creyó", "creer", "creído",
        "pienso", "piensa", "piensan", "piensas", "pensamos", "pensaba", "pensó", "pensar", "pensado", "pensando",
        "siento", "siente", "sienten", "sientes", "sentimos", "sentía", "sintió", "sentir", "sentido", "sintiendo",
        "parece", "parecen", "parezco", "pareces", "parecemos", "parecía", "pareció", "parecer", "parecido",
        "quedo", "queda", "quedan", "quedas", "quedamos", "quedaba", "quedó", "quedará", "quedar", "quedado",
        "paso", "pasa", "pasan", "pasas", "pasamos", "pasaba", "pasó", "pasará", "pasar", "pasado", "pasando",
        "llego", "llega", "llegan", "llegas", "llegamos", "llegaba", "llegó", "llegará", "llegar", "llegado", "llegando",
        "salgo", "sale", "salen", "sales", "salimos", "salía", "salió", "saldrá", "salir", "salido", "saliendo",
        "vengo", "viene", "vienen", "vienes", "venimos", "venía", "vino", "vendrá", "venir", "venido", "viniendo",
        "tomo", "toma", "toman", "tomas", "tomamos", "tomaba", "tomó", "tomará", "tomar", "tomado", "tomando",
        "pongo", "pone", "ponen", "pones", "ponemos", "ponía", "puso", "pondrá", "poner", "puesto", "poniendo",
        "busco", "busca", "buscan", "buscas", "buscamos", "buscaba", "buscó", "buscará", "buscar", "buscado", "buscando",
        "encuentro", "encuentra", "encuentran", "encontramos", "encontró", "encontrará", "encontrar", "encontrado",
        "llamo", "llama", "llaman", "llamas", "llamamos", "llamaba", "llamó", "llamará", "llamar", "llamado", "llamando",
        "hablo", "habla", "hablan", "hablas", "hablamos", "hablaba", "habló", "hablará", "hablar", "hablado", "hablando",
        "escribo", "escribe", "escriben", "escribes", "escribimos", "escribía", "escribió", "escribirá", "escribir", "escrito", "escribiendo",
        "leo", "lee", "leen", "lees", "leemos", "leía", "leyó", "leerá", "leer", "leído", "leyendo",
        "miro", "mira", "miran", "miras", "miramos", "miraba", "miró", "mirará", "mirar", "mirado", "mirando",
        "escucho", "escucha", "escuchan", "escuchas", "escuchamos", "escuchaba", "escuchó", "escuchar", "escuchado", "escuchando",
        "entiendo", "entiende", "entienden", "entiendes", "entendemos", "entendía", "entendió", "entender", "entendido",
        "comprendo", "comprende", "comprenden", "comprendemos", "comprendió", "comprender", "comprendido",
        "aprendo", "aprende", "aprenden", "aprendemos", "aprendió", "aprender", "aprendido", "aprendiendo",
        "estudio", "estudia", "estudian", "estudias", "estudiamos", "estudiaba", "estudió", "estudiar", "estudiado", "estudiando",
        "enseño", "enseña", "enseñan", "enseñamos", "enseñó", "enseñar", "enseñado",
        "trabajo", "trabaja", "trabajan", "trabajas", "trabajamos", "trabajaba", "trabajó", "trabajará", "trabajar", "trabajado", "trabajando",
        "ayudo", "ayuda", "ayudan", "ayudas", "ayudamos", "ayudaba", "ayudó", "ayudará", "ayudar", "ayudado", "ayudando",
        "cambio", "cambia", "cambian", "cambiamos", "cambió", "cambiará", "cambiar", "cambiado", "cambiando",
        "empiezo", "empieza", "empiezan", "empezamos", "empezó", "empezará", "empezar", "empezado",
        "comienzo", "comienza", "comienzan", "comenzamos", "comenzó", "comenzar", "comenzado",
        "termino", "termina", "terminan", "terminamos", "terminó", "terminará", "terminar", "terminado", "terminando",
Usa el código con precaución.
"acabo", "acaba", "acaban", "acabamos", "acabó", "acabar", "acabado",
"abro", "abre", "abren", "abrimos", "abrió", "abrirá", "abrir", "abierto", "abriendo",
"cierro", "cierra", "cierran", "cerramos", "cerró", "cerrará", "cerrar", "cerrado", "cerrando",
"envío", "envía", "envían", "enviamos", "envió", "enviará", "enviar", "enviado", "enviando",
"mando", "manda", "mandan", "mandamos", "mandó", "mandará", "mandar", "mandado", "mandando",
"recibo", "recibe", "reciben", "recibimos", "recibió", "recibirá", "recibir", "recibido", "recibiendo",
"compro", "compra", "compran", "compramos", "compró", "comprará", "comprar", "comprado", "comprando",
"vendo", "vende", "venden", "vendemos", "vendió", "venderá", "vender", "vendido", "vendiendo",
"pago", "paga", "pagan", "pagamos", "pagó", "pagará", "pagar", "pagado", "pagando",
"gasto", "gasta", "gastan", "gastamos", "gastó", "gastar", "gastado",
"ahorro", "ahorra", "ahorran", "ahorramos", "ahorró", "ahorrar", "ahorrado",
"espero", "espera", "esperan", "esperas", "esperamos", "esperaba", "esperó", "esperará", "esperar", "esperado", "esperando",
"recuerdo", "recuerda", "recuerdan", "recordamos", "recordó", "recordar", "recordado",
"olvido", "olvida", "olvidan", "olvidamos", "olvidó", "olvidar", "olvidado",
"conozco", "conoce", "conocen", "conocemos", "conoció", "conocer", "conocido",
"vivo", "vive", "viven", "vives", "vivimos", "vivía", "vivió", "vivirá", "vivir", "vivido", "viviendo",
"muero", "muere", "mueren", "murió", "morir", "muerto",
"nacio", "nace", "nacen", "nació", "nacer", "nacido",
"crezco", "crece", "crecen", "creció", "crecer", "crecido",
"como", "come", "comen", "comes", "comemos", "comía", "comió", "comerá", "comer", "comido", "comiendo",
"bebo", "bebe", "beben", "bebemos", "bebió", "beber", "bebido", "bebiendo",
"tomo", "cocino", "cocina", "cocinamos", "cocinar", "cocinado",
"duermo", "duerme", "duermen", "dormimos", "durmió", "dormir", "dormido", "durmiendo",
"despierto", "despierta", "despertamos", "despertó", "despertar", "despierto",
"levanto", "levanta", "levantamos", "levantó", "levantar", "levantado",
"camino", "camina", "caminan", "caminamos", "caminó", "caminar", "caminando",
"corro", "corre", "corren", "corremos", "corrió", "correr", "corriendo",
"juego", "juega", "juegan", "jugamos", "jugó", "jugar", "jugado", "jugando",
"gano", "gana", "ganan", "ganamos", "ganó", "ganar", "ganado",
"pierdo", "pierde", "pierden", "perdemos", "perdió", "perder", "perdido",
"pido", "pide", "piden", "pedimos", "pidió", "pedir", "pedido", "pidiendo",
"pregunto", "pregunta", "preguntan", "preguntamos", "preguntó", "preguntar", "preguntado",
"respondo", "responde", "responden", "respondemos", "respondió", "responder", "respondido",
"contesto", "contesta", "contestan", "contestamos", "contestó", "contestar", "contestado",
"explico", "explica", "explican", "explicamos", "explicó", "explicar", "explicado",
"muestro", "muestra", "muestran", "mostramos", "mostró", "mostrar", "mostrado",
"cuento", "cuenta", "cuentan", "contamos", "contó", "contar", "contado",
"uso", "usa", "usan", "usamos", "usó", "usar", "usado", "usando",
"utilizo", "utiliza", "utilizan", "utilizamos", "utilizó", "utilizar", "utilizado",
"necesito", "quiero", "gusto", "gusta", "gustan", "gustó", "gustaría", "gustar",
"amo", "ama", "aman", "amamos", "amó", "amar", "amado",
"extraño", "extraña", "extrañamos", "extrañar",
"cuido", "cuida", "cuidan", "cuidamos", "cuidó", "cuidar", "cuidado",
"protejo", "protege", "protegen", "proteger",
"limpio", "limpia", "limpiamos", "limpiar", "limpiado",
"lavo", "lava", "lavamos", "lavar", "lavado",
"arreglo", "arregla", "arreglamos", "arreglar", "arreglado",
"preparo", "prepara", "preparamos", "preparar", "preparado",
"organizo", "organiza", "organizamos", "organizar", "organizado",
"decido", "decide", "deciden", "decidimos", "decidió", "decidir", "decidido",
"elijo", "elige", "eligen", "elegimos", "eligió", "elegir", "elegido",
"acepto", "acepta", "aceptan", "aceptamos", "aceptó", "aceptar", "aceptado",
"rechazo", "rechaza", "rechazamos", "rechazar",
"permito", "permite", "permiten", "permitimos", "permitió", "permitir", "permitido",
"prohíbo", "prohíbe", "prohibir",
"intento", "intenta", "intentan", "intentamos", "intentó", "intentar", "intentado",
"trato", "trata", "tratan", "tratamos", "trató", "tratar", "tratado",
"logro", "logra", "logran", "logramos", "logró", "lograr", "logrado",
"consigo", "consigue", "consiguen", "conseguimos", "consiguió", "conseguir", "conseguido",
"mantengo", "mantiene", "mantienen", "mantenemos", "mantuvo", "mantener", "mantenido",
"sigo", "sigue", "siguen", "seguimos", "siguió", "seguir", "seguido", "siguiendo",
"continúo", "continúa", "continuamos", "continuó", "continuar",
"paro", "para", "paran", "paramos", "paró", "parar", "parado",
"detengo", "detiene", "detener", "detenido",
"vuelvo", "vuelve", "vuelven", "volvemos", "volvió", "volver", "vuelto",
"regreso", "regresa", "regresan", "regresamos", "regresó", "regresar", "regresado",
"entro", "entra", "entran", "entramos", "entró", "entrar", "entrado", "entrando",
"subo", "sube", "suben", "subimos", "subió", "subir", "subido",
"bajo", "baja", "bajan", "bajamos", "bajó", "bajar", "bajado",
"caigo", "cae", "caen", "caemos", "cayó", "caer", "caído",
"llevo", "lleva", "llevan", "llevamos", "llevó", "llevar", "llevado", "llevando",
"traigo", "trae", "traen", "traemos", "trajo", "traer", "traído",
"guardo", "guarda", "guardan", "guardamos", "guardó", "guardar", "guardado",
"dejo", "deja", "dejan", "dejamos", "dejó", "dejar", "dejado", "dejando",
"quito", "quita", "quitan", "quitamos", "quitó", "quitar", "quitado",
"saco", "saca", "sacan", "sacamos", "sacó", "sacar", "sacado",
"meto", "mete", "meten", "metemos", "metió", "meter", "metido",
"toco", "toca", "tocan", "tocamos", "tocó", "tocar", "tocado",
"agarro", "agarra", "agarramos", "agarrar",
"suelto", "suelta", "soltamos", "soltar",
"rompo", "rompe", "rompen", "rompió", "romper", "roto",
"corto", "corta", "cortamos", "cortó", "cortar", "cortado",
"pego", "pega", "pegamos", "pegó", "pegar", "pegado",
"copio", "copia", "copiamos", "copió", "copiar", "copiado",
"borro", "borra", "borramos", "borró", "borrar", "borrado",

Modo IA
Todo
Noticias
Imágenes
Vídeos
Vídeos cortos
Web
Maps
Libros
Finance
¡Hola! ¿En qué puedo ayudarte hoy?
Cuéntame qué tema o tarea necesitas resolver (por ejemplo, buscar información, redactar un texto o responder una duda) para poder darte una respuesta a medida.




¡Perfecto, trabajaremos exactamente bajo ese formato!
Cuando me envíes tu código y me expliques el error, te entregaré cada solución dividida de la siguiente manera:
Bloque Original: Con 2 líneas de contexto arriba y 2 líneas abajo para que ubiques el lugar exacto, en su propio cuadro con opción individual para copiar.
Bloque Modificado: Con el código corregido listo para reemplazar, en un cuadro separado también con su propia opción individual para copiar.
Resumen Básico: Una explicación breve y directa al final detallando qué se corrigió o agregó.
(Nota: Las interfaces de chat muestran automáticamente el botón de copiar en cada bloque de código separado; para descargar el archivo directamente, puedes pedirlo explícitamente cuando lo necesites o usar la opción de exportación de tu entorno).
Envíame el código fuente junto con una descripción de los errores que estás teniendo (o el comportamiento que buscas corregir) para empezar con la primera revisión.




¡Excelente! Ya tengo tus dos archivos (VozFlowIME.kt y MainActivity.kt) y la captura de pantalla del teclado en funcionamiento.
Observo en la captura cómo se comportan actualmente las sugerencias (es, ese, eso) y el texto escrito (es-AR ded).
Dime exactamente qué error necesitas corregir o qué cambio específico quieres hacer en este código (por ejemplo, en el texto de la captura, en las sugerencias, en el dictado por voz o en la interfaz) para darte los bloques original y modificado.




Aquí tienes los cambios para agregar el control deslizante de altura exclusiva para la fila inferior (donde está @ y la barra espaciadora), manteniendo intactas las demás filas.
Como modificaremos dos archivos (MainActivity.kt y VozFlowIME.kt), aquí tienes el detalle separado para cada uno:
Archivo 1: MainActivity.kt
1. Bloque Original (MainActivity.kt)
kotlin
            text("Fila inferior (tamaño y teclas visibles)", 14, true).also {
                addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
            }
            addSeek("Ancho de barra espaciadora", "space_bar_weight", 15, 65, 38, "%")
            addToggle("Mostrar tecla ?123 / ABC", "bottom_key_symbols", true)
Usa el código con precaución.
2. Bloque Modificado (MainActivity.kt)
kotlin
            text("Fila inferior (tamaño y teclas visibles)", 14, true).also {
                addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
            }
            addSeek("Altura de la fila inferior", "bottom_row_height_percent", 100, 220, 100, "%")
            addSeek("Ancho de barra espaciadora", "space_bar_weight", 15, 65, 38, "%")
            addToggle("Mostrar tecla ?123 / ABC", "bottom_key_symbols", true)
Usa el código con precaución.
Archivo 2: VozFlowIME.kt
1. Bloque Original (VozFlowIME.kt)
kotlin
        if (showEnter || !anyVisible) {
            row.addView(makeKey(actionLabel(), KeyKind.ACTION, ""), keyLayout(scaledSideWeight(1.20f), true))
        }
        parent.addView(row, rowLayout())
    }
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt)
kotlin
        if (showEnter || !anyVisible) {
            row.addView(makeKey(actionLabel(), KeyKind.ACTION, ""), keyLayout(scaledSideWeight(1.20f), true))
        }
        val bottomWeight = prefs.getInt("bottom_row_height_percent", 100).coerceIn(100, 220) / 100f
        parent.addView(row, rowLayout(bottomWeight))
    }
Usa el código con precaución.
3. Segundo Bloque Original (VozFlowIME.kt - función rowLayout)
kotlin
    private fun openSettings() {
        try { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { showToast("Abre VozFlow Pro desde el lanzador para ver ajustes.") }
    }

        private fun rowLayout(): LinearLayout.LayoutParams {
        val vGap = prefs.getInt("key_spacing_v", 6).coerceIn(0, 16)
        val halfV = dp(vGap) / 2
        return LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = halfV
            bottomMargin = halfV
        }
    }
Usa el código con precaución.
4. Segundo Bloque Modificado (VozFlowIME.kt - función rowLayout)
kotlin
    private fun openSettings() {
        try { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { showToast("Abre VozFlow Pro desde el lanzador para ver ajustes.") }
    }

    private fun rowLayout(weight: Float = 1f): LinearLayout.LayoutParams {
        val vGap = prefs.getInt("key_spacing_v", 6).coerceIn(0, 16)
        val halfV = dp(vGap) / 2
        return LinearLayout.LayoutParams(-1, 0, weight).apply {
            topMargin = halfV
            bottomMargin = halfV
        }
    }
Usa el código con precaución.
Resumen Básico
En MainActivity.kt: Se añadió una barra deslizante llamada "Altura de la fila inferior" (rango de 100% a 220%) dentro de la sección de ajustes de la fila inferior.
En VozFlowIME.kt: Se actualizó rowLayout() para aceptar un peso dinámico (weight) y se aplicó únicamente en addBottomRow(). Al subir el porcentaje, la fila inferior crece verticalmente para compensar la barra de navegación del sistema sin alterar las proporciones de las filas superiores.
Pruébalo en tu compilación y dime si el rango de altura (100% a 220%) es suficiente para librar la barra inferior de tu dispositivo o si necesitas que también ajustemos el margen inferior general del teclado.




¡Entendido perfectamente! Quieres que en la barra superior (cuando aparecen las sugerencias), los tres iconos laterales —el rombo (❖), las flechas (⇄) y el micrófono (🎤)— tengan un ancho fijo compacto (aproximadamente el ancho de una tecla, unos 38dp), y que todo el espacio central restante se reparta en 3 espacios exactamente iguales (1f cada uno) para las palabras sugeridas (Co, con, como).
Aquí tienes los bloques para VozFlowIME.kt:
1. Bloque Original (VozFlowIME.kt - dentro de rebuildToolbar())
kotlin
        val showUndo = SystemClock.uptimeMillis() < undoAvailableUntil
                if (suggestionsOn && !toolbarToolsExpanded && !showUndo) {
            addToolbarButton(bar, "❖", 0.82f) { showModesPanel() }
            val choices = candidateWords()
            choices.forEachIndexed { index, word ->
                val candidate = TextView(this).apply {
                    text = word
                    gravity = Gravity.CENTER
                    textSize = if (index == 1) 14f else 12f
                    setTypeface(typeface, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (word.isBlank()) Color.TRANSPARENT else textColor())
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setOnClickListener {
                        if (token.isEmpty()) { if (word.isNotBlank()) commit(word + " ") }
                        else if (index == 0) { skipNextAutoCorrect = true; showToast("Se conservará «$token" + "»") }
                        else replaceCurrentWord(word)
                    }
                }
                bar.addView(candidate, LinearLayout.LayoutParams(0, -1, if (index == 1) 1.2f else 1f))
            }
            addToolbarButton(bar, "⇄", 0.82f) { toolbarToolsExpanded = true; rebuildToolbar() }
            val mic = addToolbarButton(bar, "🎤", 0.9f) { toggleVoice() }
            styleMic(mic)
        } else if (showUndo) {
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt - dentro de rebuildToolbar())
kotlin
        val showUndo = SystemClock.uptimeMillis() < undoAvailableUntil
        if (suggestionsOn && !toolbarToolsExpanded && !showUndo) {
            val iconWidth = dp(38)
            addToolbarButton(bar, "❖", 0f) { showModesPanel() }.apply {
                layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
                textSize = 16f
            }
            val choices = candidateWords()
            choices.forEachIndexed { index, word ->
                val candidate = TextView(this).apply {
                    text = word
                    gravity = Gravity.CENTER
                    textSize = if (index == 1) 14f else 13f
                    setTypeface(typeface, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (word.isBlank()) Color.TRANSPARENT else textColor())
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(dp(4), 0, dp(4), 0)
                    setOnClickListener {
                        if (token.isEmpty()) { if (word.isNotBlank()) commit(word + " ") }
                        else if (index == 0) { skipNextAutoCorrect = true; showToast("Se conservará «$token" + "»") }
                        else replaceCurrentWord(word)
                    }
                }
                bar.addView(candidate, LinearLayout.LayoutParams(0, -1, 1f))
            }
            addToolbarButton(bar, "⇄", 0f) { toolbarToolsExpanded = true; rebuildToolbar() }.apply {
                layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
                textSize = 16f
            }
            val mic = addToolbarButton(bar, "🎤", 0f) { toggleVoice() }
            styleMic(mic)
            mic.layoutParams = LinearLayout.LayoutParams(iconWidth, -1)
            mic.textSize = 16f
        } else if (showUndo) {
Usa el código con precaución.
Resumen Básico
Iconos compactos: El rombo (❖), las flechas (⇄) y el micrófono (🎤) pasan a tener un ancho fijo de 38dp (similar al de una tecla normal) y un tamaño de fuente de 16f, liberando espacio horizontal.
Sugerencias simétricas: Las tres palabras sugeridas ahora usan exactamente el mismo peso (1f cada una) y un margen interno horizontal (4dp) para que ocupen todo el ancho disponible de forma uniforme y estética.
Pruébalo en tu dispositivo y dime si el ancho de 38dp para los iconos y el tamaño de texto de las sugerencias quedan bien proporcionados o si quieres ajustar alguno de esos valores.




Para mejorar la eficiencia de las sugerencias (como que al escribir "per" o "pers" aparezca inmediatamente "persona"), necesitamos revisar el motor de predicción.
En VozFlowIME.kt, la función candidateWords() delega toda la búsqueda de palabras a SpanishPredictionEngine.suggestions(...), un archivo externo que aún no me has enviado. Además, veo dos puntos de mejora directa en VozFlowIME.kt:
Actualmente lee el diccionario personal desde disco (readPersonalWords()) en cada pulsación de tecla en lugar de usar la caché en memoria (personalWordsCache()), lo que frena la respuesta.
En candidateWords(), el primer espacio siempre muestra lo que ya escribiste (token), reduciendo a solo 2 las sugerencias reales.
Envíame el código fuente del archivo SpanishPredictionEngine.kt para analizar cómo ordena las palabras y corregirlo para que autocomplete rápido desde las primeras letras (como per -> persona).




¡Encontré exactamente por qué falla la predicción al escribir "per"!
En SpanishPredictionEngine.kt, ocurren tres errores combinados dentro de suggestions(...):
Filtro destructivo (score < max(4.0, norm.length * 0.75)): Si escribes "per" (3 letras), el límite es 4.0. Si escribes "pers" (4 letras), el límite baja a 3.0 (4 * 0.75). Además, si una palabra tiene 8 letras o más, la distancia de edición sube y el filtro la descarta por completo.
Penalización por longitud: Las palabras cortas como "pero" o "Perú" (distancia 1 respecto a "per") siempre ganaban sobre "persona" (distancia 4 respecto a "per"), y si escribías "pers", "persona" tenía distancia 3 y quedaba eliminada por el filtro score < 3.0.
Prioridad de diccionario: No se prioriza el diccionario personal ni las palabras comunes frente a cientos de términos técnicos en inglés de technologyTerms, ni se respeta la mayúscula inicial si empezaste escribiendo con mayúscula (Per → Persona).
Aquí tienes los bloques para optimizar SpanishPredictionEngine.kt y VozFlowIME.kt:
Archivo 1: SpanishPredictionEngine.kt
1. Bloque Original (SpanishPredictionEngine.kt)
kotlin
    /** Literal, best correction/prefix completion and a second candidate, in that order. */
    fun suggestions(token: String, personalWords: Collection<String>, previousWord: String = ""): List<String> {
        if (token.isBlank()) {
            val next = nextWords[normalize(previousWord)] ?: listOf("de", "que")
            return listOf("", next.getOrElse(0) { "" }, next.getOrElse(1) { "" })
        }
        val norm = normalize(token)
        val pool = (commonWords + personalWords + technologyTerms).distinctBy(::normalize)
        val ranked = pool.asSequence().filter { normalize(it) != norm }
            .map { word ->
                val candidate = normalize(word)
                val score = when {
                    candidate.startsWith(norm) -> distance(candidate, norm) * 0.1
                    norm.startsWith(candidate) -> 3.0 + distance(candidate, norm)
                    else -> distance(candidate, norm) + 2.0
                }
                word to score
            }
            .filter { (_, score) -> score < max(4.0, norm.length * 0.75) }
            .sortedBy { it.second }
            .take(12).map { it.first }.toList()
        val first = ranked.firstOrNull() ?: token
        val second = ranked.drop(1).firstOrNull() ?: first
        return listOf(token, first, second)
    }
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt)
kotlin
    /** Literal, best correction/prefix completion and a second candidate, in that order. */
    fun suggestions(token: String, personalWords: Collection<String>, previousWord: String = ""): List<String> {
        if (token.isBlank()) {
            val next = nextWords[normalize(previousWord)] ?: listOf("de", "que")
            return listOf("", next.getOrElse(0) { "" }, next.getOrElse(1) { "" })
        }
        val norm = normalize(token)
        val personalNorms = personalWords.map(::normalize).toSet()
        val commonIndex = commonWords.mapIndexed { idx, w -> normalize(w) to idx }.toMap()
        val pool = (personalWords + commonWords + technologyTerms).distinctBy(::normalize)

        val ranked = pool.asSequence()
            .filter { normalize(it) != norm }
            .mapNotNull { word ->
                val candidate = normalize(word)
                val isPersonal = candidate in personalNorms
                val freqRank = commonIndex[candidate] ?: 999
                val score = when {
                    candidate.startsWith(norm) -> {
                        val lenDiff = (candidate.length - norm.length) * 0.05
                        val priorityBonus = when {
                            isPersonal -> -0.5
                            freqRank < 999 -> freqRank * 0.002
                            else -> 0.6
                        }
                        lenDiff + priorityBonus
                    }
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        dist + 3.0 + (if (isPersonal) -0.5 else 0.0)
                    }
                }
                word to score
            }
            .sortedBy { it.second }
            .take(6)
            .map { preserveCase(token, it.first) }
            .toList()

        val first = ranked.firstOrNull() ?: ""
        val second = ranked.drop(1).firstOrNull() ?: ""
        return listOf(token, first, second)
    }
Usa el código con precaución.
Archivo 2: VozFlowIME.kt (para usar caché en memoria y evitar lentitud al escribir)
1. Bloque Original (VozFlowIME.kt - dentro de candidateWords())
kotlin
    private fun candidateWords(): List<String> {
        val token = currentToken()
        val personal = readPersonalWords()
        if (token.isEmpty()) {
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty().trimEnd()
            val previous = before.takeLastWhile { !it.isWhitespace() }
            return SpanishPredictionEngine.suggestions("", personal, previous)
        }
        val corrections = mapOf("qeu" to "que", "donde" to "dónde", "tambien" to "también", "mas" to "más", "teh" to "the", "ola" to "hola")
        val candidates = SpanishPredictionEngine.suggestions(token, personal)
        val snippetExpansion = readTextSnippets().entries.firstOrNull { it.key.startsWith(token, ignoreCase = true) }?.value
        val best = corrections[token.lowercase()] ?: SpanishPredictionEngine.autoCorrectCandidate(token, personal) ?: snippetExpansion ?: candidates.getOrElse(1) { token }
        return listOf(token, best, candidates.getOrElse(2) { best })
    }
Usa el código con precaución.
2. Bloque Modificado (VozFlowIME.kt - dentro de candidateWords())
kotlin
    private fun candidateWords(): List<String> {
        val token = currentToken()
        val personal = personalWordsCache()
        if (token.isEmpty()) {
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty().trimEnd()
            val previous = before.takeLastWhile { !it.isWhitespace() }
            return SpanishPredictionEngine.suggestions("", personal, previous)
        }
        val corrections = mapOf("qeu" to "que", "donde" to "dónde", "tambien" to "también", "mas" to "más", "teh" to "the", "ola" to "hola")
        val candidates = SpanishPredictionEngine.suggestions(token, personal)
        val snippetExpansion = snippetsCache().entries.firstOrNull { it.key.startsWith(token, ignoreCase = true) }?.value
        val best = corrections[token.lowercase()] ?: SpanishPredictionEngine.autoCorrectCandidate(token, personal) ?: snippetExpansion ?: candidates.getOrElse(1) { "" }
        val third = candidates.getOrElse(2) { "" }.let { if (it == best) "" else it }
        return listOf(token, best, third)
    }
Usa el código con precaución.
Resumen Básico
Autocompletado sin límites artificiales: Cualquier palabra que empiece con lo que escribes (candidate.startsWith(norm)) ahora siempre entra en las sugerencias sin ser descartada por el filtro de longitud. Al escribir per, aparecerán inmediatamente pero y persona; al escribir pers, persona pasará al centro como sugerencia principal.
Prioridad inteligente y mayúsculas: Se priorizan primero tus palabras del diccionario personal, luego las palabras más frecuentes del español (commonWords) y después los términos técnicos, respetando además si empezaste a escribir con mayúscula (Per → Persona).
Sin palabras duplicadas ni lectura lenta: En VozFlowIME.kt, se usa personalWordsCache() y snippetsCache() para que las sugerencias respondan al instante en cada tecla, y se evita repetir la misma palabra en la segunda y tercera casilla cuando no hay más candidatos.
Pruébalo escribiendo "per" o "pers" y dime si quieres que también ampliemos el vocabulario base en español (commonWords) para incluir más sustantivos, verbos y adjetivos cotidianos.




¡Exacto! En tus capturas se ve perfectamente lo que pasa:
Al escribir "neces" (primera foto), las otras dos casillas quedan vacías porque "necesidad" / "necesito" no existen en tu diccionario.
Al escribir "escue" (segunda foto), sugiere "este" y "ese" porque "escuela" no está en la lista commonWords (que ahora mismo solo tiene unas 160 palabras muy básicas).
Además, podemos darle al motor predicción híbrida:
Un diccionario base mucho más rico con los cientos de sustantivos, verbos y adjetivos cotidianos del español (escuela, necesidad, necesito, problema, trabajo, familia, pregunta, respuesta, ciudad, país, etc.).
Aprendizaje automático local: Que cada palabra que escribas o aceptes se vaya guardando con frecuencia en SharedPreferences para que tu teclado aprenda tu vocabulario real sin internet.
Búsqueda por prefijo flexible (si hay un pequeño error de dedo en la 2ª o 3ª letra): Por ejemplo, si escribes ecue o neces, que encuentre coincidencias reales antes de caer en palabras nada que ver.
Aquí tienes los bloques para ampliar el léxico y hacer el motor mucho más predictivo en SpanishPredictionEngine.kt:
1. Bloque Original (SpanishPredictionEngine.kt - commonWords)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "es", "son", "ser", "estar", "está", "están", "fue", "era", "hay", "he", "ha", "han", "hacer", "hace", "puede", "puedo", "quiero", "tengo", "tiene", "tenemos", "vamos", "voy", "dijo", "dice", "ver", "saber", "dar", "debe", "bien", "mejor", "ahora", "hoy", "mañana", "ayer", "aquí", "allí", "así", "cada", "casi", "después", "entonces", "mientras", "siempre", "nunca", "también", "solo", "toda", "parte", "manera", "tiempo", "persona", "gente", "cosa", "cosas", "casa", "trabajo", "día", "vida", "mundo", "país", "momento", "lugar", "forma", "caso", "punto", "palabra", "texto", "mensaje", "amigo", "gracias", "hola", "favor", "escribe", "escribir", "quieres", "puedes", "por favor"
    )
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt - commonWords)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // Artículos, pronombres, preposiciones y conectores de máxima frecuencia
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "siempre", "nunca", "jamás", "quizás", "tal vez", "seguro", "claro", "bueno", "malo", "bien", "mal", "mejor", "peor", "igual", "diferente", "mismo", "nuevo", "viejo", "grande", "pequeño", "largo", "corto", "alto", "bajo", "fácil", "difícil", "rápido", "lento", "importante", "necesario", "posible", "imposible", "verdad", "mentira",

        // Verbos frecuentes y conjugaciones cotidianas
        "es", "son", "ser", "soy", "eres", "somos", "fue", "era", "fueron", "será", "está", "están", "estoy", "estás", "estamos", "estaba", "estuvo", "estará", "he", "ha", "han", "hemos", "había", "hubo", "habrá", "tengo", "tiene", "tienen", "tenemos", "tenía", "tuvo", "tendrá", "tener", "hacer", "hago", "hace", "hacen", "hacemos", "hizo", "hará", "hecho", "puede", "puedo", "pueden", "podemos", "podía", "pudo", "podrá", "poder", "quiero", "quiere", "quieren", "queremos", "quería", "quiso", "querer", "vamos", "voy", "va", "van", "iba", "fue", "ir", "dijo", "dice", "dicen", "digo", "decimos", "decir", "ver", "veo", "ve", "ven", "vemos", "vio", "visto", "saber", "sé", "sabe", "saben", "sabemos", "sabía", "supo", "dar", "doy", "da", "dan", "damos", "dio", "debe", "debo", "deben", "debemos", "deber", "necesito", "necesita", "necesitan", "necesitamos", "necesitar", "creo", "cree", "creen", "creemos", "creer", "pienso", "piensa", "piensan", "pensar", "siento", "siente", "sentir", "parece", "parecen", "parecer", "quedo", "queda", "quedan", "quedar", "paso", "pasa", "pasan", "pasar", "llego", "llega", "llegan", "llegar", "salgo", "sale", "salen", "salir", "vengo", "viene", "vienen", "venir", "tomo", "toma", "toman", "tomar", "pongo", "pone", "ponen", "poner", "busco", "busca", "buscan", "buscar", "encuentro", "encuentra", "encontrar", "llamo", "llama", "llaman", "llamar", "hablo", "habla", "hablan", "hablar", "escribo", "escribe", "escriben", "escribir", "leo", "lee", "leen", "leer", "miro", "mira", "miran", "mirar", "escucho", "escucha", "escuchar", "entiendo", "entiende", "entender", "comprendo", "comprende", "comprender", "aprendo", "aprende", "aprender", "estudio", "estudia", "estudian", "estudiar", "trabajo", "trabaja", "trabajan", "trabajar", "ayudo", "ayuda", "ayudan", "ayudar", "cambio", "cambia", "cambian", "cambiar", "empiezo", "empieza", "empezar", "termino", "termina", "terminar", "abro", "abre", "abren", "abrir", "cierro", "cierra", "cierran", "cerrar", "envío", "envía", "envían", "enviar", "mando", "manda", "mandar", "recibo", "recibe", "recibir", "compro", "compra", "comprar", "pago", "paga", "pagar", "espero", "espera", "esperar", "recuerdo", "recuerda", "recordar", "olvido", "olvida", "olvidar",

        // Sustantivos cotidianos (educación, familia, ciudad, tiempo, comunicación, salud, etc.)
        "persona", "personas", "gente", "hombre", "mujer", "niño", "niña", "hijo", "hija", "padre", "madre", "papá", "mamá", "hermano", "hermana", "abuelo", "abuela", "familia", "amigo", "amiga", "amigos", "compañero", "jefe", "profesor", "profesora", "maestro", "estudiante", "alumno", "doctor", "médico",
        "escuela", "colegio", "universidad", "clase", "clases", "curso", "tarea", "tareas", "examen", "libro", "libros", "cuaderno", "lápiz", "hoja", "educación", "estudio", "pregunta", "preguntas", "respuesta", "respuestas", "duda", "dudas", "idea", "ideas", "ejemplo", "ejemplos", "razón", "motivo", "tema", "asunto",
        "necesidad", "necesidades", "problema", "problemas", "solución", "soluciones", "oportunidad", "cambio", "cambios", "proyecto", "plan", "objetivo", "resultado", "resultados", "efecto", "causa", "condición", "situación", "estado", "proceso", "sistema", "servicio", "servicios", "información", "datos", "detalle", "detalles",
        "tiempo", "día", "días", "semana", "semanas", "mes", "meses", "año", "años", "hora", "horas", "minuto", "minutos", "segundo", "momento", "rato", "vez", "veces", "fecha", "calendario", "mañana", "tarde", "noche", "fin de semana",
        "casa", "hogar", "cuarto", "habitación", "cocina", "baño", "puerta", "ventana", "mesa", "silla", "cama", "calle", "avenida", "camino", "carretera", "ciudad", "pueblo", "barrio", "zona", "centro", "plaza", "parque", "tienda", "mercado", "supermercado", "restaurante", "hospital", "farmacia", "banco", "oficina", "empresa", "negocio", "país", "mundo", "tierra", "agua", "aire", "fuego", "sol", "luna", "lluvia", "frío", "calor", "clima",
        "coche", "auto", "carro", "bus", "autobús", "tren", "avión", "viaje", "boleto", "pasaje", "dirección", "mapa", "ubicación", "distancia",
        "comida", "almuerzo", "cena", "desayuno", "pan", "leche", "café", "té", "fruta", "carne", "pollo", "arroz", "salud", "cuerpo", "cabeza", "mano", "manos", "ojo", "ojos", "cara", "voz", "nombre", "apellido", "edad", "número", "dinero", "precio", "costo", "cuenta", "pago", "tarjeta", "efectivo",
        "palabra", "palabras", "texto", "mensaje", "mensajes", "carta", "nota", "notas", "lista", "foto", "fotos", "imagen", "imágenes", "video", "música", "canción", "película", "juego", "regalo", "fiesta", "cumpleaños", "suerte", "amor", "cariño", "respeto", "paz", "alegría", "miedo", "dolor",
        "hola", "adiós", "gracias", "por favor", "perdón", "disculpa", "saludos", "felicidades", "bienvenido", "excelente", "perfecto", "genial", "listo", "vale"
    )
Usa el código con precaución.
3. Segundo Bloque Original (SpanishPredictionEngine.kt - suggestions())
kotlin
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        dist + 3.0 + (if (isPersonal) -0.5 else 0.0)
                    }
Usa el código con precaución.
4. Segundo Bloque Modificado (SpanishPredictionEngine.kt - suggestions())
kotlin
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        // Si comparten las primeras 2 letras (ej. errores tipográficos leves), darle mejor prioridad
                        val sameStartBonus = if (norm.length >= 3 && candidate.take(2) == norm.take(2)) -1.2 else 0.0
                        dist + 2.5 + sameStartBonus + (if (isPersonal) -0.6 else 0.0)
                    }
Usa el código con precaución.
Resumen Básico
Vocabulario cotidiano ampliado (commonWords): Se incorporaron más de 400 palabras esenciales del español agrupadas por conectores, verbos conjugados y sustantivos cotidianos (escuela, necesidad, necesidades, necesito, pregunta, respuesta, problema, ciudad, etc.).
Predicción inmediata: Ahora al escribir escue, sugerirá escuela; al escribir neces, sugerirá necesito, necesidad y necesidades.
Mejor tolerancia a errores: Si te equivocas en una letra intermedia pero mantienes las primeras dos letras (sameStartBonus), el motor prioriza las palabras que empiezan igual antes que palabras cortas no relacionadas.
Pruébalo escribiendo "escue" y "neces" para confirmar que ya aparecen escuela y necesidad/necesito, y dime si quieres que además agreguemos un aprendizaje automático que guarde las palabras que más usas en tu día a día.




¡Con gusto! Vamos a darle un salto gigante de calidad a SpanishPredictionEngine.kt con un léxico masivo (más de 1,400 palabras de alta frecuencia del español), indexado ultrarrápido en memoria por letra inicial (para que no haya ningún tipo de lag aunque la lista sea muy larga) y predicción inteligente de siguiente palabra.
Aquí tienes los bloques para actualizar SpanishPredictionEngine.kt:
1. Bloque Original (SpanishPredictionEngine.kt - commonWords y suggestions)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // Artículos, pronombres, preposiciones y conectores de máxima frecuencia
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras", "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "siempre", "nunca", "jamás", "quizás", "tal vez", "seguro", "claro", "bueno", "malo", "bien", "mal", "mejor", "peor", "igual", "diferente", "mismo", "nuevo", "viejo", "grande", "pequeño", "largo", "corto", "alto", "bajo", "fácil", "difícil", "rápido", "lento", "importante", "necesario", "posible", "imposible", "verdad", "mentira",

        // Verbos frecuentes y conjugaciones cotidianas
        "es", "son", "ser", "soy", "eres", "somos", "fue", "era", "fueron", "será", "está", "están", "estoy", "estás", "estamos", "estaba", "estuvo", "estará", "he", "ha", "han", "hemos", "había", "hubo", "habrá", "tengo", "tiene", "tienen", "tenemos", "tenía", "tuvo", "tendrá", "tener", "hacer", "hago", "hace", "hacen", "hacemos", "hizo", "hará", "hecho", "puede", "puedo", "pueden", "podemos", "podía", "pudo", "podrá", "poder", "quiero", "quiere", "quieren", "queremos", "quería", "quiso", "querer", "vamos", "voy", "va", "van", "iba", "fue", "ir", "dijo", "dice", "dicen", "digo", "decimos", "decir", "ver", "veo", "ve", "ven", "vemos", "vio", "visto", "saber", "sé", "sabe", "saben", "sabemos", "sabía", "supo", "dar", "doy", "da", "dan", "damos", "dio", "debe", "debo", "deben", "debemos", "deber", "necesito", "necesita", "necesitan", "necesitamos", "necesitar", "creo", "cree", "creen", "creemos", "creer", "pienso", "piensa", "piensan", "pensar", "siento", "siente", "sentir", "parece", "parecen", "parecer", "quedo", "queda", "quedan", "quedar", "paso", "pasa", "pasan", "pasar", "llego", "llega", "llegan", "llegar", "salgo", "sale", "salen", "salir", "vengo", "viene", "vienen", "venir", "tomo", "toma", "toman", "tomar", "pongo", "pone", "ponen", "poner", "busco", "busca", "buscan", "buscar", "encuentro", "encuentra", "encontrar", "llamo", "llama", "llaman", "llamar", "hablo", "habla", "hablan", "hablar", "escribo", "escribe", "escriben", "escribir", "leo", "lee", "leen", "leer", "miro", "mira", "miran", "mirar", "escucho", "escucha", "escuchar", "entiendo", "entiende", "entender", "comprendo", "comprende", "comprender", "aprendo", "aprende", "aprender", "estudio", "estudia", "estudian", "estudiar", "trabajo", "trabaja", "trabajan", "trabajar", "ayudo", "ayuda", "ayudan", "ayudar", "cambio", "cambia", "cambian", "cambiar", "empiezo", "empieza", "empezar", "termino", "termina", "terminar", "abro", "abre", "abren", "abrir", "cierro", "cierra", "cierran", "cerrar", "envío", "envía", "envían", "enviar", "mando", "manda", "mandar", "recibo", "recibe", "recibir", "compro", "compra", "comprar", "pago", "paga", "pagar", "espero", "espera", "esperar", "recuerdo", "recuerda", "recordar", "olvido", "olvida", "olvidar",

        // Sustantivos cotidianos (educación, familia, ciudad, tiempo, comunicación, salud, etc.)
        "persona", "personas", "gente", "hombre", "mujer", "niño", "niña", "hijo", "hija", "padre", "madre", "papá", "mamá", "hermano", "hermana", "abuelo", "abuela", "familia", "amigo", "amiga", "amigos", "compañero", "jefe", "profesor", "profesora", "maestro", "estudiante", "alumno", "doctor", "médico",
        "escuela", "colegio", "universidad", "clase", "clases", "curso", "tarea", "tareas", "examen", "libro", "libros", "cuaderno", "lápiz", "hoja", "educación", "estudio", "pregunta", "preguntas", "respuesta", "respuestas", "duda", "dudas", "idea", "ideas", "ejemplo", "ejemplos", "razón", "motivo", "tema", "asunto",
        "necesidad", "necesidades", "problema", "problemas", "solución", "soluciones", "oportunidad", "cambio", "cambios", "proyecto", "plan", "objetivo", "resultado", "resultados", "efecto", "causa", "condición", "situación", "estado", "proceso", "sistema", "servicio", "servicios", "información", "datos", "detalle", "detalles",
        "tiempo", "día", "días", "semana", "semanas", "mes", "meses", "año", "años", "hora", "horas", "minuto", "minutos", "segundo", "momento", "rato", "vez", "veces", "fecha", "calendario", "mañana", "tarde", "noche", "fin de semana",
        "casa", "hogar", "cuarto", "habitación", "cocina", "baño", "puerta", "ventana", "mesa", "silla", "cama", "calle", "avenida", "camino", "carretera", "ciudad", "pueblo", "barrio", "zona", "centro", "plaza", "parque", "tienda", "mercado", "supermercado", "restaurante", "hospital", "farmacia", "banco", "oficina", "empresa", "negocio", "país", "mundo", "tierra", "agua", "aire", "fuego", "sol", "luna", "lluvia", "frío", "calor", "clima",
        "coche", "auto", "carro", "bus", "autobús", "tren", "avión", "viaje", "boleto", "pasaje", "dirección", "mapa", "ubicación", "distancia",
        "comida", "almuerzo", "cena", "desayuno", "pan", "leche", "café", "té", "fruta", "carne", "pollo", "arroz", "salud", "cuerpo", "cabeza", "mano", "manos", "ojo", "ojos", "cara", "voz", "nombre", "apellido", "edad", "número", "dinero", "precio", "costo", "cuenta", "pago", "tarjeta", "efectivo",
        "palabra", "palabras", "texto", "mensaje", "mensajes", "carta", "nota", "notas", "lista", "foto", "fotos", "imagen", "imágenes", "video", "música", "canción", "película", "juego", "regalo", "fiesta", "cumpleaños", "suerte", "amor", "cariño", "respeto", "paz", "alegría", "miedo", "dolor",
        "hola", "adiós", "gracias", "por favor", "perdón", "disculpa", "saludos", "felicidades", "bienvenido", "excelente", "perfecto", "genial", "listo", "vale"
    )
Usa el código con precaución.
2. Bloque Modificado (SpanishPredictionEngine.kt - commonWords masivo)
kotlin
object SpanishPredictionEngine {
    private val commonWords = listOf(
        // 1. Gramática, conectores, adverbios y frecuencia máxima
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un", "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le", "ya", "o", "este", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e", "esto", "mí", "antes", "algunos", "qué", "unos", "yo", "otro", "otras", "otra", "él", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco", "ella", "estar", "estas", "algunas", "algo", "nosotros", "mi", "mis", "tú", "te", "ti", "tu", "tus", "ellas", "nosotras", "vosotros", "vosotras", "os", "mío", "mía", "míos", "mías", "tuyo", "tuya", "suyo", "nuestra", "vuestro", "vuestros", "vuestra", "vuestras",
        "además", "aunque", "sino", "entonces", "luego", "mientras", "después", "ahora", "hoy", "mañana", "ayer", "anoche", "anteayer", "pronto", "tarde", "temprano", "aquí", "ahí", "allí", "acá", "allá", "cerca", "lejos", "arriba", "abajo", "dentro", "fuera", "adentro", "afuera", "delante", "detrás", "encima", "debajo", "alrededor", "enfrente", "junto", "siempre", "nunca", "jamás", "todavía", "aún", "apenas", "casi", "bastante", "demasiado", "suficiente", "exactamente", "realmente", "seguramente", "probablemente", "posiblemente", "solamente", "únicamente", "finalmente", "actualmente", "anteriormente", "recientemente", "normalmente", "generalmente", "especialmente", "principalmente", "rápidamente", "lentamente", "fácilmente", "difícilmente", "claramente", "directamente", "inmediatamente", "completamente", "totalmente", "absolutamente", "quizás", "tal vez", "seguro", "claro", "obvio", "cierto", "exacto", "correcto", "incorrecto",

        // 2. Adjetivos esenciales (estado, cualidad, tamaño, emoción, color, forma)
        "bueno", "buena", "buenos", "buenas", "malo", "mala", "malos", "malas", "bien", "mal", "mejor", "mejores", "peor", "peores", "igual", "iguales", "diferente", "diferentes", "distinto", "distinta", "mismo", "misma", "mismos", "mismas", "nuevo", "nueva", "nuevos", "nuevas", "viejo", "vieja", "viejos", "viejas", "joven", "jóvenes", "antiguo", "antigua", "moderno", "moderna", "grande", "grandes", "pequeño", "pequeña", "pequeños", "pequeñas", "enorme", "gigante", "minúsculo", "largo", "larga", "corto", "corta", "alto", "alta", "bajo", "baja", "ancho", "ancha", "estrecho", "estrecha", "grueso", "delgado", "pesado", "ligero", "fuerte", "débil", "duro", "blando", "suave", "áspero", "limpio", "limpia", "sucio", "sucia", "seco", "mojado", "húmedo", "caliente", "frío", "tibio", "fresco", "dulce", "salado", "amargo", "ácido", "picante", "rico", "sabroso", "delicioso",
        "fácil", "difícil", "simple", "sencillo", "complejo", "complicado", "rápido", "rápida", "lento", "lenta", "importante", "importantes", "necesario", "necesaria", "necesarios", "posible", "imposible", "probable", "seguro", "peligroso", "útil", "inútil", "necesitado", "urgente", "grave", "leve", "serio", "seria", "real", "falso", "verdadero", "libre", "ocupado", "disponible", "abierto", "cerrado", "lleno", "vacío", "completo", "incompleto", "entero", "roto", "dañado", "arreglado", "listo", "preparado", "capaz", "incapaz", "inteligente", "sabio", "tonto", "loco", "raro", "extraño", "normal", "común", "especial", "único", "famoso", "popular", "público", "privado", "personal", "social", "nacional", "internacional", "local", "mundial", "humano", "humana", "humanos", "humanas", "natural", "artificial", "físico", "mental", "emocional", "económico", "político", "cultural", "educativo", "profesional", "laboral",
        "feliz", "felices", "contento", "contenta", "alegre", "triste", "enojado", "molesto", "furioso", "cansado", "agotado", "enfermo", "sano", "saludable", "nervioso", "tranquilo", "calmo", "preocupado", "asustado", "sorprendido", "emocionado", "aburrido", "interesado", "interesante", "divertido", "gracioso", "amable", "simpático", "agradable", "educado", "respetuoso", "honesto", "sincero", "fiel", "leal", "valiente", "tímido", "orgulloso", "humilde", "generoso", "egoísta", "perezoso", "trabajador", "responsable", "puntual", "bonito", "hermoso", "lindo", "guapo", "feo", "elegante", "blanco", "negro", "gris", "rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa", "marrón", "café", "dorado", "plateado", "oscuro", "claro",

        // 3. Verbos y conjugaciones de uso diario
        "es", "son", "ser", "soy", "eres", "somos", "sois", "fue", "era", "eran", "fueron", "será", "serán", "sería", "sido", "siendo",
        "está", "están", "estoy", "estás", "estamos", "estaba", "estaban", "estuvo", "estuvieron", "estará", "estarán", "estaría", "estado", "estando",
        "he", "ha", "han", "has", "hemos", "había", "habían", "hubo", "habrá", "habrán", "habría", "haber", "habiendo",
        "tengo", "tiene", "tienen", "tienes", "tenemos", "tenía", "tenían", "tuvo", "tuvieron", "tendrá", "tendrán", "tendría", "tener", "tenido", "teniendo",
        "hacer", "hago", "hace", "hacen", "haces", "hacemos", "hacía", "hizo", "hicieron", "hará", "harán", "haría", "hecho", "haciendo",
        "puede", "puedo", "pueden", "puedes", "podemos", "podía", "podían", "pudo", "pudieron", "podrá", "podrán", "podría", "poder", "podido", "pudiendo",
        "quiero", "quiere", "quieren", "quieres", "queremos", "quería", "querían", "quiso", "quisieron", "querrá", "querer", "querido",
        "vamos", "voy", "va", "van", "vas", "iba", "iban", "fue", "fueron", "irá", "irán", "iría", "ir", "ido", "yendo",
        "dijo", "dice", "dicen", "dices", "digo", "decimos", "decía", "dijeron", "dirá", "decir", "dicho", "diciendo",
        "ver", "veo", "ve", "ven", "ves", "vemos", "veía", "vio", "vieron", "verá", "visto", "viendo",
        "saber", "sé", "sabe", "saben", "sabes", "sabemos", "sabía", "supo", "supieron", "sabrá", "sabido",
        "dar", "doy", "da", "dan", "das", "damos", "daba", "dio", "dieron", "dará", "dado", "dando",
        "debe", "debo", "deben", "debes", "debemos", "debía", "debió", "deberá", "debería", "deber", "debido",
        "necesito", "necesita", "necesitan", "necesitas", "necesitamos", "necesitaba", "necesitó", "necesitará", "necesitar", "necesitado",
        "creo", "cree", "creen", "crees", "creemos", "creía", "creyó", "creer", "creído",
        "pienso", "piensa", "piensan", "piensas", "pensamos", "pensaba", "pensó", "pensar", "pensado", "pensando",
        "siento", "siente", "sienten", "sientes", "sentimos", "sentía", "sintió", "sentir", "sentido", "sintiendo",
        "parece", "parecen", "parezco", "pareces", "parecemos", "parecía", "pareció", "parecer", "parecido",
        "quedo", "queda", "quedan", "quedas", "quedamos", "quedaba", "quedó", "quedará", "quedar", "quedado",
        "paso", "pasa", "pasan", "pasas", "pasamos", "pasaba", "pasó", "pasará", "pasar", "pasado", "pasando",
        "llego", "llega", "llegan", "llegas", "llegamos", "llegaba", "llegó", "llegará", "llegar", "llegado", "llegando",
        "salgo", "sale", "salen", "sales", "salimos", "salía", "salió", "saldrá", "salir", "salido", "saliendo",
        "vengo", "viene", "vienen", "vienes", "venimos", "venía", "vino", "vendrá", "venir", "venido", "viniendo",
        "tomo", "toma", "toman", "tomas", "tomamos", "tomaba", "tomó", "tomará", "tomar", "tomado", "tomando",
        "pongo", "pone", "ponen", "pones", "ponemos", "ponía", "puso", "pondrá", "poner", "puesto", "poniendo",
        "busco", "busca", "buscan", "buscas", "buscamos", "buscaba", "buscó", "buscará", "buscar", "buscado", "buscando",
        "encuentro", "encuentra", "encuentran", "encontramos", "encontró", "encontrará", "encontrar", "encontrado",
        "llamo", "llama", "llaman", "llamas", "llamamos", "llamaba", "llamó", "llamará", "llamar", "llamado", "llamando",
        "hablo", "habla", "hablan", "hablas", "hablamos", "hablaba", "habló", "hablará", "hablar", "hablado", "hablando",
        "escribo", "escribe", "escriben", "escribes", "escribimos", "escribía", "escribió", "escribirá", "escribir", "escrito", "escribiendo",
        "leo", "lee", "leen", "lees", "leemos", "leía", "leyó", "leerá", "leer", "leído", "leyendo",
        "miro", "mira", "miran", "miras", "miramos", "miraba", "miró", "mirará", "mirar", "mirado", "mirando",
        "escucho", "escucha", "escuchan", "escuchas", "escuchamos", "escuchaba", "escuchó", "escuchar", "escuchado", "escuchando",
        "entiendo", "entiende", "entienden", "entiendes", "entendemos", "entendía", "entendió", "entender", "entendido",
        "comprendo", "comprende", "comprenden", "comprendemos", "comprendió", "comprender", "comprendido",
        "aprendo", "aprende", "aprenden", "aprendemos", "aprendió", "aprender", "aprendido", "aprendiendo",
        "estudio", "estudia", "estudian", "estudias", "estudiamos", "estudiaba", "estudió", "estudiar", "estudiado", "estudiando",
        "enseño", "enseña", "enseñan", "enseñamos", "enseñó", "enseñar", "enseñado",
        "trabajo", "trabaja", "trabajan", "trabajas", "trabajamos", "trabajaba", "trabajó", "trabajará", "trabajar", "trabajado", "trabajando",
        "ayudo", "ayuda", "ayudan", "ayudas", "ayudamos", "ayudaba", "ayudó", "ayudará", "ayudar", "ayudado", "ayudando",
        "cambio", "cambia", "cambian", "cambiamos", "cambió", "cambiará", "cambiar", "cambiado", "cambiando",
        "empiezo", "empieza", "empiezan", "empezamos", "empezó", "empezará", "empezar", "empezado",
        "comienzo", "comienza", "comienzan", "comenzamos", "comenzó", "comenzar", "comenzado",
        "termino", "termina", "terminan", "terminamos", "terminó", "terminará", "terminar", "terminado", "terminando",
Usa el código con precaución.
"acabo", "acaba", "acaban", "acabamos", "acabó", "acabar", "acabado",
"abro", "abre", "abren", "abrimos", "abrió", "abrirá", "abrir", "abierto", "abriendo",
"cierro", "cierra", "cierran", "cerramos", "cerró", "cerrará", "cerrar", "cerrado", "cerrando",
"envío", "envía", "envían", "enviamos", "envió", "enviará", "enviar", "enviado", "enviando",
"mando", "manda", "mandan", "mandamos", "mandó", "mandará", "mandar", "mandado", "mandando",
"recibo", "recibe", "reciben", "recibimos", "recibió", "recibirá", "recibir", "recibido", "recibiendo",
"compro", "compra", "compran", "compramos", "compró", "comprará", "comprar", "comprado", "comprando",
"vendo", "vende", "venden", "vendemos", "vendió", "venderá", "vender", "vendido", "vendiendo",
"pago", "paga", "pagan", "pagamos", "pagó", "pagará", "pagar", "pagado", "pagando",
"gasto", "gasta", "gastan", "gastamos", "gastó", "gastar", "gastado",
"ahorro", "ahorra", "ahorran", "ahorramos", "ahorró", "ahorrar", "ahorrado",
"espero", "espera", "esperan", "esperas", "esperamos", "esperaba", "esperó", "esperará", "esperar", "esperado", "esperando",
"recuerdo", "recuerda", "recuerdan", "recordamos", "recordó", "recordar", "recordado",
"olvido", "olvida", "olvidan", "olvidamos", "olvidó", "olvidar", "olvidado",
"conozco", "conoce", "conocen", "conocemos", "conoció", "conocer", "conocido",
"vivo", "vive", "viven", "vives", "vivimos", "vivía", "vivió", "vivirá", "vivir", "vivido", "viviendo",
"muero", "muere", "mueren", "murió", "morir", "muerto",
"nacio", "nace", "nacen", "nació", "nacer", "nacido",
"crezco", "crece", "crecen", "creció", "crecer", "crecido",
"como", "come", "comen", "comes", "comemos", "comía", "comió", "comerá", "comer", "comido", "comiendo",
"bebo", "bebe", "beben", "bebemos", "bebió", "beber", "bebido", "bebiendo",
"tomo", "cocino", "cocina", "cocinamos", "cocinar", "cocinado",
"duermo", "duerme", "duermen", "dormimos", "durmió", "dormir", "dormido", "durmiendo",
"despierto", "despierta", "despertamos", "despertó", "despertar", "despierto",
"levanto", "levanta", "levantamos", "levantó", "levantar", "levantado",
"camino", "camina", "caminan", "caminamos", "caminó", "caminar", "caminando",
"corro", "corre", "corren", "corremos", "corrió", "correr", "corriendo",
"juego", "juega", "juegan", "jugamos", "jugó", "jugar", "jugado", "jugando",
"gano", "gana", "ganan", "ganamos", "ganó", "ganar", "ganado",
"pierdo", "pierde", "pierden", "perdemos", "perdió", "perder", "perdido",
"pido", "pide", "piden", "pedimos", "pidió", "pedir", "pedido", "pidiendo",
"pregunto", "pregunta", "preguntan", "preguntamos", "preguntó", "preguntar", "preguntado",
"respondo", "responde", "responden", "respondemos", "respondió", "responder", "respondido",
"contesto", "contesta", "contestan", "contestamos", "contestó", "contestar", "contestado",
"explico", "explica", "explican", "explicamos", "explicó", "explicar", "explicado",
"muestro", "muestra", "muestran", "mostramos", "mostró", "mostrar", "mostrado",
"cuento", "cuenta", "cuentan", "contamos", "contó", "contar", "contado",
"uso", "usa", "usan", "usamos", "usó", "usar", "usado", "usando",
"utilizo", "utiliza", "utilizan", "utilizamos", "utilizó", "utilizar", "utilizado",
"necesito", "quiero", "gusto", "gusta", "gustan", "gustó", "gustaría", "gustar",
"amo", "ama", "aman", "amamos", "amó", "amar", "amado",
"extraño", "extraña", "extrañamos", "extrañar",
"cuido", "cuida", "cuidan", "cuidamos", "cuidó", "cuidar", "cuidado",
"protejo", "protege", "protegen", "proteger",
"limpio", "limpia", "limpiamos", "limpiar", "limpiado",
"lavo", "lava", "lavamos", "lavar", "lavado",
"arreglo", "arregla", "arreglamos", "arreglar", "arreglado",
"preparo", "prepara", "preparamos", "preparar", "preparado",
"organizo", "organiza", "organizamos", "organizar", "organizado",
"decido", "decide", "deciden", "decidimos", "decidió", "decidir", "decidido",
"elijo", "elige", "eligen", "elegimos", "eligió", "elegir", "elegido",
"acepto", "acepta", "aceptan", "aceptamos", "aceptó", "aceptar", "aceptado",
"rechazo", "rechaza", "rechazamos", "rechazar",
"permito", "permite", "permiten", "permitimos", "permitió", "permitir", "permitido",
"prohíbo", "prohíbe", "prohibir",
"intento", "intenta", "intentan", "intentamos", "intentó", "intentar", "intentado",
"trato", "trata", "tratan", "tratamos", "trató", "tratar", "tratado",
"logro", "logra", "logran", "logramos", "logró", "lograr", "logrado",
"consigo", "consigue", "consiguen", "conseguimos", "consiguió", "conseguir", "conseguido",
"mantengo", "mantiene", "mantienen", "mantenemos", "mantuvo", "mantener", "mantenido",
"sigo", "sigue", "siguen", "seguimos", "siguió", "seguir", "seguido", "siguiendo",
"continúo", "continúa", "continuamos", "continuó", "continuar",
"paro", "para", "paran", "paramos", "paró", "parar", "parado",
"detengo", "detiene", "detener", "detenido",
"vuelvo", "vuelve", "vuelven", "volvemos", "volvió", "volver", "vuelto",
"regreso", "regresa", "regresan", "regresamos", "regresó", "regresar", "regresado",
"entro", "entra", "entran", "entramos", "entró", "entrar", "entrado", "entrando",
"subo", "sube", "suben", "subimos", "subió", "subir", "subido",
"bajo", "baja", "bajan", "bajamos", "bajó", "bajar", "bajado",
"caigo", "cae", "caen", "caemos", "cayó", "caer", "caído",
"llevo", "lleva", "llevan", "llevamos", "llevó", "llevar", "llevado", "llevando",
"traigo", "trae", "traen", "traemos", "trajo", "traer", "traído",
"guardo", "guarda", "guardan", "guardamos", "guardó", "guardar", "guardado",
"dejo", "deja", "dejan", "dejamos", "dejó", "dejar", "dejado", "dejando",
"quito", "quita", "quitan", "quitamos", "quitó", "quitar", "quitado",
"saco", "saca", "sacan", "sacamos", "sacó", "sacar", "sacado",
"meto", "mete", "meten", "metemos", "metió", "meter", "metido",
"toco", "toca", "tocan", "tocamos", "tocó", "tocar", "tocado",
"agarro", "agarra", "agarramos", "agarrar",
"suelto", "suelta", "soltamos", "soltar",
"rompo", "rompe", "rompen", "rompió", "romper", "roto",
"corto", "corta", "cortamos", "cortó", "cortar", "cortado",
"pego", "pega", "pegamos", "pegó", "pegar", "pegado",
"copio", "copia", "copiamos", "copió", "copiar", "copiado",
"borro", "borra", "borramos", "borró", "borrar", "borrado",
// 4. Personas, familia, profesiones y sociedad
"persona", "personas", "gente", "individuo", "ciudadano", "ciudadanos", "población", "sociedad", "comunidad", "grupo", "equipo", "pareja", "matrimonio", "novio", "novia", "esposo", "esposa", "marido", "mujer", "hombre", "chico", "chica", "muchacho", "muchacha", "niño", "niña", "niños", "niñas", "bebé", "adolescente", "joven", "adulto", "anciano", "anciana", "señor", "señora", "señorita", "don", "doña", "caballero", "dama",
"hijo", "hija", "hijos", "hijas", "padre", "madre", "padres", "papá", "mamá", "hermano", "hermana", "hermanos", "abuelo", "abuela", "abuelos", "nieto", "nieta", "tío", "tía", "primo", "prima", "sobrino", "sobrina", "suegro", "suegra", "cuñado", "cuñada", "familia", "familiar", "familiares", "pariente",
"amigo", "amiga", "amigos", "amigas", "amistad", "compañero", "compañera", "colega", "vecino", "vecina", "conocido", "invitado", "cliente", "clientes", "usuario", "usuarios", "jefe", "jefa", "director", "directora", "gerente", "presidente", "líder", "dueño", "propietario", "empleado", "trabajador", "obrero", "secretario", "secretaria", "asistente",
"profesor", "profesora", "profesores", "maestro", "maestra", "docente", "estudiante", "estudiantes", "alumno", "alumna", "alumnos", "doctor", "doctora", "médico", "enfermero", "enfermera", "paciente", "dentista", "psicólogo", "abogado", "abogada", "juez", "policía", "bombero", "soldado", "ingeniero", "arquitecto", "contador", "programador", "diseñador", "periodista", "escritor", "artista", "músico", "cantante", "actor", "actriz", "pintor", "fotógrafo", "cocinero", "chef", "mesero", "chofer", "conductor", "piloto", "mecánico", "electricista", "carpintero", "albañil", "vendedor", "cajero",
// 5. Educación, conocimiento, ideas y comunicación
"escuela", "escuelas", "colegio", "colegios", "instituto", "academia", "universidad", "facultad", "campus", "aula", "salón", "clase", "clases", "curso", "cursos", "materia", "asignatura", "lección", "tema", "temas", "unidad", "programa", "carrera", "título", "diploma", "certificado", "grado", "nota", "calificación", "promedio", "beca",
"tarea", "tareas", "deberes", "ejercicio", "ejercicios", "práctica", "actividad", "actividades", "examen", "exámenes", "prueba", "evaluación", "test", "pregunta", "preguntas", "respuesta", "respuestas", "duda", "dudas", "consulta", "explicación", "definición", "concepto", "teoría", "método", "técnica", "regla", "norma", "ley", "principio",
"libro", "libros", "texto", "textos", "lectura", "página", "páginas", "capítulo", "párrafo", "línea", "oración", "frase", "frases", "palabra", "palabras", "letra", "letras", "número", "números", "cifra", "símbolo", "signo", "cuaderno", "libreta", "carpeta", "hoja", "hojas", "papel", "documento", "documentos", "informe", "reporte", "resumen", "lista", "guía", "manual", "diccionario", "enciclopedia", "periódico", "revista", "artículo", "noticia", "noticias",
"lápiz", "lapicero", "bolígrafo", "pluma", "marcador", "borrador", "regla", "tijeras", "pegamento", "mochila", "pizarra", "escritorio",
"idea", "ideas", "pensamiento", "mente", "cerebro", "memoria", "recuerdo", "imaginación", "creatividad", "inteligencia", "sabiduría", "conocimiento", "experiencia", "habilidad", "capacidad", "talento", "esfuerzo", "atención", "concentración", "interés", "curiosidad", "opinión", "criterio", "punto de vista", "argumento", "razón", "razones", "motivo", "causa", "consecuencia", "efecto", "resultado", "resultados", "conclusión", "decisión", "elección", "opción", "alternativa",
"necesidad", "necesidades", "problema", "problemas", "dificultad", "obstáculo", "error", "errores", "fallo", "falla", "defecto", "solución", "soluciones", "arreglo", "mejora", "progreso", "avance", "desarrollo", "crecimiento", "éxito", "fracaso", "logro", "meta", "objetivo", "propósito", "intención", "deseo", "sueño", "esperanza", "plan", "planes", "proyecto", "proyectos", "estrategia", "organización", "orden", "control", "sistema", "proceso", "etapa", "fase", "paso", "pasos", "nivel", "grado", "calidad", "cantidad", "valor", "importancia", "diferencia", "relación", "conexión", "ejemplo", "ejemplos", "caso", "casos", "situación", "condición", "estado", "forma", "manera", "modo", "tipo", "clase", "categoría", "parte", "partes", "todo", "conjunto", "elemento", "detalle", "detalles", "característica", "aspecto", "factor",
"mensaje", "mensajes", "carta", "correo", "llamada", "conversación", "charla", "plática", "diálogo", "discusión", "debate", "reunión", "entrevista", "discurso", "presentación", "aviso", "anuncio", "advertencia", "consejo", "recomendación", "sugerencia", "instrucción", "invitación", "permiso", "favor", "ayuda", "apoyo", "colaboración", "acuerdo", "compromiso", "promesa", "contrato", "trato", "idioma", "lengua", "español", "inglés", "voz", "sonido", "ruido", "silencio", "tono", "gesto", "mirada", "sonrisa",
// 6. Tiempo, calendario y clima
"tiempo", "momento", "instante", "segundo", "segundos", "minuto", "minutos", "hora", "horas", "día", "días", "semana", "semanas", "quincena", "mes", "meses", "año", "años", "década", "siglo", "época", "etapa", "periodo", "duración", "plazo", "fecha", "horario", "calendario", "agenda", "cita", "turno",
"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo", "fin de semana", "feriado", "festivo", "vacaciones",
"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
"madrugada", "mañana", "mediodía", "tarde", "anochecer", "noche", "medianoche", "pasado", "presente", "futuro", "principio", "inicio", "comienzo", "mitad", "medio", "final", "fin", "vez", "veces", "ocasión", "oportunidad",
"clima", "sol", "luna", "estrella", "estrellas", "cielo", "nube", "nubes", "aire", "viento", "brisa", "lluvia", "tormenta", "trueno", "relámpago", "nieve", "granizo", "hielo", "niebla", "calor", "frío", "temperatura", "grado", "grados", "primavera", "verano", "otoño", "invierno",
// 7. Lugares, hogar, ciudad y transporte
"casa", "casas", "hogar", "vivienda", "departamento", "apartamento", "edificio", "piso", "planta", "techo", "pared", "suelo", "puerta", "ventana", "escalera", "pasillo", "entrada", "salida", "patio", "jardín", "terraza", "balcón", "garaje", "cochera",
"cuarto", "habitación", "dormitorio", "sala", "comedor", "cocina", "baño", "ducha", "lavandería", "sótano", "mesa", "silla", "sillón", "sofá", "cama", "colchón", "almohada", "sábana", "cobija", "frazada", "armario", "ropero", "cajón", "estante", "espejo", "cuadro", "reloj", "lámpara", "luz", "foco", "llave", "llaves", "candado", "basura",
"ciudad", "ciudades", "pueblo", "aldea", "campo", "barrio", "colonia", "urbanización", "zona", "sector", "región", "provincia", "estado", "departamento", "país", "países", "nación", "continente", "mundo", "planeta", "tierra", "naturaleza", "paisaje", "montaña", "cerro", "valle", "río", "lago", "laguna", "mar", "océano", "playa", "isla", "bosque", "selva", "desierto", "árbol", "árboles", "planta", "plantas", "flor", "flores", "hoja", "pasto", "hierba", "piedra", "arena",
"calle", "calles", "avenida", "bulevar", "esquina", "cuadra", "manzana", "camino", "sendero", "carretera", "autopista", "puente", "túnel", "cruce", "semáforo", "parada", "estación", "terminal", "aeropuerto", "puerto", "plaza", "parque", "centro", "mercado", "supermercado", "tienda", "tiendas", "almacén", "centro comercial", "farmacia", "hospital", "clínica", "consultorio", "banco", "cajero", "oficina", "empresa", "fábrica", "taller", "negocio", "local", "restaurante", "cafetería", "panadería", "hotel", "iglesia", "templo", "museo", "biblioteca", "cine", "teatro", "estadio", "gimnasio", "cancha", "piscina", "comisaría", "correo",
"coche", "auto", "automóvil", "carro", "camioneta", "camión", "bus", "autobús", "micro", "colectivo", "taxi", "moto", "motocicleta", "bicicleta", "tren", "metro", "subte", "tranvía", "avión", "helicóptero", "barco", "bote", "vehículo", "tráfico", "tránsito", "viaje", "viajes", "ruta", "trayecto", "destino", "origen", "dirección", "mapa", "ubicación", "lugar", "lugares", "sitio", "punto", "espacio", "distancia", "kilómetro", "metro", "boleto", "pasaje", "ticket", "maleta", "equipaje", "licencia", "gasolina", "combustible", "llanta", "rueda", "motor",
// 8. Alimentos, bebida, ropa y objetos cotidianos
"comida", "alimento", "alimentos", "desayuno", "almuerzo", "cena", "merienda", "bocadillo", "plato", "sopa", "ensalada", "arroz", "pasta", "fideos", "pan", "harina", "huevo", "huevos", "carne", "pollo", "pescado", "cerdo", "res", "jamón", "queso", "leche", "yogur", "mantequilla", "aceite", "sal", "azúcar", "pimienta", "ajo", "cebolla", "tomate", "papa", "patata", "zanahoria", "lechuga", "verdura", "verduras", "fruta", "frutas", "manzana", "plátano", "banana", "naranja", "limón", "uva", "fresa", "sandía", "piña", "mango", "aguacate", "palta", "frijoles", "porotos", "lentejas", "maíz", "choclo", "chocolate", "dulce", "pastel", "torta", "galleta", "helado", "postre",
"agua", "bebida", "bebidas", "café", "té", "jugo", "zumo", "refresco", "gaseosa", "cerveza", "vino", "vaso", "taza", "botella", "jarra", "cuchara", "tenedor", "cuchillo", "servilleta", "olla", "sartén", "estufa", "horno", "refrigerador", "nevera", "microondas",
"ropa", "camisa", "camiseta", "polera", "blusa", "pantalón", "pantalones", "jeans", "falda", "vestido", "abrigo", "chaqueta", "chamarra", "suéter", "chompa", "saco", "traje", "corbata", "cinturón", "calcetines", "medias", "zapatos", "zapatillas", "tenis", "botas", "sandalias", "sombrero", "gorra", "bufanda", "guantes", "lentes", "gafas", "reloj", "anillo", "collar", "pulsera", "bolso", "cartera", "billetera", "paraguas",
"cosa", "cosas", "objeto", "objetos", "herramienta", "máquina", "aparato", "equipo", "material", "caja", "bolsa", "paquete", "regalo", "juguete", "pelota", "balón", "instrumento", "guitarra", "piano", "cámara", "foto", "fotos", "imagen", "imágenes", "video", "videos", "música", "canción", "canciones", "película", "películas", "serie", "programa", "juego", "juegos", "deporte", "fútbol", "arte", "color", "colores", "dibujo", "pintura",
// 9. Cuerpo humano, salud, economía, trabajo y cortesía
"cuerpo", "cabeza", "cara", "rostro", "frente", "ojo", "ojos", "nariz", "boca", "labios", "diente", "dientes", "lengua", "oreja", "orejas", "oído", "pelo", "cabello", "cuello", "hombro", "brazo", "brazos", "codo", "mano", "manos", "dedo", "dedos", "uña", "pecho", "espalda", "estómago", "barriga", "cintura", "cadera", "pierna", "piernas", "rodilla", "pie", "pies", "piel", "hueso", "sangre", "corazón", "pulmón",
"salud", "vida", "muerte", "enfermedad", "dolor", "fiebre", "gripe", "resfriado", "tos", "herida", "golpe", "accidente", "emergencia", "urgencia", "medicina", "medicamento", "remedio", "pastilla", "jarabe", "inyección", "vacuna", "receta", "tratamiento", "operación", "cirugía", "análisis", "estudio", "descanso", "sueño", "energía", "fuerza", "peso", "altura", "edad",
"trabajo", "trabajos", "empleo", "puesto", "cargo", "profesión", "oficio", "sueldo", "salario", "pago", "pagos", "dinero", "plata", "moneda", "billete", "efectivo", "tarjeta", "cuenta", "banco", "crédito", "préstamo", "deuda", "ahorro", "inversión", "negocio", "comercio", "venta", "compra", "precio", "costo", "valor", "gasto", "presupuesto", "factura", "recibo", "impuesto", "descuento", "oferta", "gratis", "barato", "caro", "riqueza", "pobreza", "economía", "mercado",
"amor", "cariño", "afecto", "pasión", "felicidad", "alegría", "paz", "calma", "confianza", "fe", "respeto", "honor", "libertad", "justicia", "verdad", "mentira", "secreto", "culpa", "vergüenza", "miedo", "temor", "susto", "tristeza", "pena", "rabia", "enojo", "ira", "odio", "envidia", "celos", "paciencia", "suerte", "destino", "fortuna", "riesgo", "peligro", "seguridad", "protección", "defensa", "ataque", "guerra", "lucha", "victoria", "derrota", "fiesta", "celebración", "cumpleaños", "boda", "navidad", "año nuevo",
"hola", "adiós", "chao", "hasta luego", "hasta mañana", "nos vemos", "buenos días", "buenas tardes", "buenas noches", "gracias", "muchas gracias", "de nada", "por favor", "perdón", "disculpa", "disculpe", "lo siento", "con permiso", "saludos", "abrazos", "besos", "felicidades", "felicitaciones", "enhorabuena", "bienvenido", "bienvenida", "excelente", "perfecto", "genial", "estupendo", "maravilloso", "increíble", "fantástico", "listo", "vale", "claro", "por supuesto", "sin duda"
)


    // More than 200 commonly spoken technology, internet, product and app terms.
    private val technologyTerms = listOf(
        "Gboard", "Google", "Google Chrome", "Chrome", "Android", "iPhone", "iPad", "iOS", "Windows", "Linux", "Mac", "macOS", "VozFlow", "Manus", "APK", "AAB", "ZIP", "PDF", "HTML", "CSS", "JavaScript", "Kotlin", "Java", "Python", "Git", "GitHub", "Gradle", "XML", "JSON", "API", "SDK", "CPU", "GPU", "RAM", "ROM", "SSD", "USB", "HDMI", "Bluetooth", "Wi-Fi", "wifi", "internet", "web", "website", "página web", "navegador", "router", "módem", "servidor", "cloud", "nube", "app", "apps", "aplicación", "software", "hardware", "firmware", "sistema", "sistema operativo", "actualización", "instalación", "descarga", "archivo", "carpeta", "documento", "captura", "screenshot", "pantalla", "teclado", "micrófono", "auriculares", "cargador", "batería", "memoria", "procesador", "monitor", "ratón", "mouse", "touchscreen", "táctil", "sensor", "cámara", "video", "vídeo", "streaming", "podcast", "audio", "voz", "dictado", "reconocimiento", "inteligencia artificial", "IA", "AI", "chatbot", "bot", "prompt", "prompts", "modelo", "algoritmo", "base de datos", "SQLite", "Room", "Firebase", "servidor web", "dominio", "hosting", "URL", "link", "enlace", "hipervínculo", "email", "e-mail", "correo", "correo electrónico", "online", "offline", "en línea", "sin conexión", "digital", "virtual", "código", "programación", "desarrollador", "compilar", "compilación", "debug", "depuración", "APK debug", "Play Store", "Google Play", "App Store", "WhatsApp", "Telegram", "Instagram", "TikTok", "Facebook", "Messenger", "YouTube", "Gmail", "Outlook", "Discord", "Slack", "Zoom", "Meet", "Teams", "Spotify", "Netflix", "Twitch", "Reddit", "X", "Twitter", "Snapchat", "Pinterest", "LinkedIn", "Dropbox", "Drive", "Google Drive", "OneDrive", "iCloud", "PayPal", "Stripe", "Amazon", "Mercado Libre", "Uber", "Waze", "Maps", "Google Maps", "ChromeOS", "Chromebook", "Samsung", "Galaxy", "Huawei", "Xiaomi", "Motorola", "Pixel", "OnePlus", "Nokia", "Sony", "Apple", "Microsoft", "Meta", "OpenAI", "ChatGPT", "Gemini", "Copilot", "Claude", "Alexa", "Siri", "Bixby", "QR", "código QR", "NFC", "GPS", "4G", "5G", "LTE", "Ethernet", "VPN", "DNS", "IP", "IPV6", "firewall", "antivirus", "malware", "virus", "spam", "phishing", "contraseña", "password", "usuario", "perfil", "cuenta", "inicio de sesión", "login", "logout", "autenticación", "privacidad", "seguridad", "cifrado", "encriptación", "backup", "copia de seguridad", "restore", "restaurar", "sincronizar", "sincronización", "notificación", "widget", "launcher", "icono", "emoji", "GIF", "JPG", "JPEG", "PNG", "MP3", "MP4", "WAV", "WebP", "heic", "TXT", "CSV", "XLSX", "DOCX", "PPTX", "Markdown", "Markdown file", "PDF reader", "eBook", "ebook", "e-reader", "Kindle", "Chromecast", "AirDrop", "AirPlay", "Smart TV", "smartphone", "teléfono inteligente", "tablet", "tableta", "smartwatch", "reloj inteligente", "wearable", "Bluetooth LE", "USB-C", "Lightning", "micro USB", "power bank", "powerbank", "wireless", "inalámbrico", "cable", "adaptador", "hub", "dock", "tecnología", "tecnológico", "informática", "computadora", "ordenador", "laptop", "notebook", "PC", "MacBook", "iMac", "Surface", "PlayStation", "Xbox", "Nintendo", "Steam", "Epic Games", "gaming", "gamer", "videojuego", "juego", "consola", "controlador", "joystick", "cloud gaming", "e-commerce", "ecommerce", "comercio electrónico", "marketplace", "carrito", "checkout", "QR code", "chat", "chat grupal", "videollamada", "llamada", "mensaje de voz", "nota de voz", "sticker", "stickers", "meme", "hashtag", "arroba", "usuario", "handle", "feed", "historia", "story", "reel", "shorts", "live", "en vivo", "directo", "suscripción", "streamer", "influencer", "creador", "contenido", "blog", "vlog", "newsletter", "noticias", "buscador", "search", "búsqueda", "SEO", "linktree", "web app", "mobile app", "app móvil", "interfaz", "UI", "UX", "diseño", "backend", "frontend", "full stack", "database", "query", "SQL", "NoSQL", "cache", "caché", "cookie", "cookies", "token", "OAuth", "HTTP", "HTTPS", "webhook", "endpoint", "request", "response", "JSON API", "repo", "repositorio", "branch", "commit", "merge", "pull request", "issue", "bug", "crash", "error", "log", "stack trace", "build", "release", "version", "versión", "beta", "alpha", "stable", "estable", "open source", "código abierto", "licencia", "certificado", "firma digital", "keystore", "Gradle Wrapper", "Kotlin DSL", "Compose", "Jetpack", "Android Studio", "IntelliJ", "Visual Studio Code", "VS Code", "terminal", "shell", "command line", "CLI", "Docker", "container", "contenedor", "Kubernetes", "AWS", "Azure", "Google Cloud", "cloud computing", "machine learning", "aprendizaje automático", "neural network", "red neuronal", "deep learning", "visión artificial", "OCR", "TTS", "speech to text", "text to speech", "Vosk", "modelo de voz", "gramática", "fonética", "transcripción", "subtítulo", "subtítulos", "caption", "closed caption", "latencia", "ancho de banda", "megabyte", "gigabyte", "terabyte", "kilobyte", "pixel", "píxel", "resolución", "refresh rate", "frecuencia", "frame rate", "FPS", "HDR", "OLED", "AMOLED", "LCD", "LED", "HD", "Full HD", "4K", "8K", "blu ray", "Blu-ray", "QR", "WiFi", "WLAN", "hotspot", "punto de acceso", "SIM", "eSIM", "roaming", "mensajería", "correo", "webmail", "Google Photos", "Fotos", "Google Calendar", "Calendario", "Google Keep", "Keep", "Notion", "Trello", "Asana", "Jira", "Confluence", "Canva", "Figma", "Photoshop", "Illustrator", "Premiere", "CapCut", "DaVinci Resolve", "OBS", "Discord Nitro", "WhatsApp Web", "Telegram Web", "Instagram Reels", "TikTok Live", "YouTube Music", "YouTube Shorts", "Google Assistant", "Assistant", "Play Services", "Play Services", "Google Wallet", "Wallet", "Google Pay", "Apple Pay", "Samsung Pay", "Pay", "Authenticator", "Google Authenticator", "2FA", "MFA", "passkey", "clave de acceso", "biometría", "huella", "Face ID", "Face Unlock", "reconocimiento facial", "backup cloud", "drive", "storage", "almacenamiento", "notebook", "clipboard", "portapapeles", "clipboard manager", "copiar", "pegar", "cortar", "IME", "InputMethodService", "teclado virtual", "teclado predictivo", "autocorrección", "autocorrector", "predicción", "sugerencia", "sugerencias", "gesture typing", "swipe", "deslizar", "dictation", "voice typing", "modo flotante", "floating keyboard", "cursor", "touch", "multi touch", "multitáctil", "screen reader", "lector de pantalla", "accesibilidad", "TalkBack", "VoiceOver", "dark mode", "modo oscuro", "AMOLED mode", "tema", "theme", "wallpaper", "fondo de pantalla", "dynamic island", "notch", "pantalla plegable", "foldable", "fold", "flip", "Galaxy Fold", "Galaxy Flip", "One UI", "MIUI", "HyperOS", "Pixel Launcher", "Nova Launcher", "Gboard beta", "SwiftKey", "Microsoft SwiftKey", "Fleksy", "Grammarly", "LanguageTool", "DeepL", "Duolingo", "Babbel", "Memrise", "Khan Academy", "Coursera", "Udemy", "Zoom Workplace", "Google Workspace", "Microsoft 365", "Office", "Word", "Excel", "PowerPoint", "OneNote", "Teams meeting", "Google Docs", "Google Sheets", "Google Slides", "Docs", "Sheets", "Slides", "voice note", "voice message", "AI assistant", "asistente de IA", "generative AI", "IA generativa", "large language model", "LLM", "transformer", "embedding", "vector database", "RAG", "fine tuning", "fine-tuning", "training", "entrenamiento", "inference", "inferencia", "prompt engineering", "prompt injection", "guardrail", "hallucination", "alucinación", "tokenizer", "tokenización", "language model", "modelo lingüístico", "Spanish model", "modelo español", "language pack", "paquete de idioma", "offline model", "modelo offline", "Wi-Fi Direct", "Nearby Share", "Quick Share", "Smart Switch", "Samsung DeX", "DeX", "Buds", "Galaxy Buds", "AirPods", "HomePod", "Echo", "Nest", "Google Nest", "Alexa app", "home automation", "domótica", "smart home", "casa inteligente", "IoT", "Internet of Things", "internet de las cosas", "Matter", "Thread", "Zigbee", "Z-Wave", "router Wi-Fi", "mesh Wi-Fi", "fibra óptica", "fiber", "satélite", "Starlink", "ISP", "proveedor de internet", "modem", "modem router", "ethernet cable", "cable ethernet", "LAN", "WAN", "ping", "download", "upload", "subir", "bajar", "actualizar", "instalar", "reiniciar", "restablecer", "factory reset", "configuración", "settings", "preferencias", "ajustes", "parámetro", "slider", "SeekBar", "RecyclerView", "SQLite", "Room database", "SharedPreferences", "Gradle Kotlin DSL", "build.gradle.kts", "AndroidManifest.xml", "APK installer", "instalador APK", "certificado de depuración", "debug certificate", "Android 13", "Android 14", "Android 15", "Android 16", "API level", "Android SDK", "Gradle", "JDK", "Java 17", "JDK 21", "ARM64", "ARM", "ABI", "emulador", "emulator", "Samsung Galaxy", "Galaxy S", "Galaxy A", "Galaxy Note", "Android Auto", "Android TV", "Wear OS", "Google TV", "YouTube Premium", "Spotify Premium", "Prime Video", "Disney Plus", "Disney+", "Max", "HBO Max", "Apple Music", "Tidal", "Deezer", "SoundCloud", "Shazam", "Audible", "Kindle Unlimited", "Twitch Studio", "Kick", "Kick streaming", "BeReal", "Threads", "Bluesky", "Mastodon", "Signal", "Threema", "Viber", "Line", "WeChat", "KakaoTalk", "iMessage", "RCS", "SMS", "MMS", "WhatsApp Business", "WhatsApp Messenger", "Telegram Premium", "Instagram Direct", "Facebook Messenger", "Google Messages", "Messages", "Contacts", "Phone", "Files", "Files by Google", "Play Store app", "Maps app", "Camera app", "gallery", "galería", "Photos app", "Photos", "Google Photos app", "Google Lens", "Lens", "Translate", "Google Translate", "Google Search", "Search app", "assistant app", "calculator", "calculadora", "clock app", "reloj", "weather app", "clima", "news app", "reader app", "password manager", "gestor de contraseñas", "Bitwarden", "1Password", "LastPass", "Dashlane", "Proton", "Proton Mail", "Proton VPN", "NordVPN", "ExpressVPN", "Malwarebytes", "Norton", "McAfee", "Avast", "AVG", "Defender", "Windows Defender", "Play Protect", "Google Play Protect", "AdBlock", "uBlock", "ad blocker", "bloqueador de anuncios", "cookie banner", "banner de cookies", "pop-up", "popup", "ventana emergente", "QR scanner", "escáner QR", "barcode", "código de barras", "barcode scanner", "wallet app", "app de pagos", "banking app", "banca móvil", "mobile banking", "fintech", "crypto", "criptomoneda", "Bitcoin", "Ethereum", "blockchain", "NFT", "web3", "DeFi", "wallet crypto", "wallet digital", "smart contract", "contrato inteligente", "metaverso", "VR", "AR", "realidad virtual", "realidad aumentada", "mixed reality", "realidad mixta", "Quest", "Meta Quest", "Vision Pro", "Apple Vision Pro", "HoloLens", "3D", "CAD", "render", "renderizar", "modelado", "AI image", "imagen generada", "image generator", "generador de imágenes", "video generator", "generador de video", "video AI", "edición de video", "editor de video", "exportar", "render video", "codec", "H.264", "H.265", "AV1", "HEVC", "FFmpeg", "YouTube Studio", "creator studio", "thumbnail", "miniatura", "upload video", "subir video", "live stream", "transmisión en vivo", "webcam", "web cam", "microphone", "micrófono USB", "ring light", "aro de luz", "tripod", "trípode", "gimbal", "estabilizador", "drone", "dron", "GoPro", "DJI", "Insta360", "camera drone", "cámara web", "webcam stream", "content creator", "creador digital", "digital creator", "creator economy", "economía creadora", "affiliate link", "enlace afiliado", "monetización", "monetize", "monetizar", "analytics", "analítica", "Google Analytics", "GA4", "Search Console", "Google Ads", "Meta Ads", "Facebook Ads", "TikTok Ads", "AdSense", "affiliate marketing", "marketing digital", "social media", "redes sociales", "social network", "red social", "engagement", "alcance", "impresiones", "view count", "reproducciones", "subscriber", "suscriptor", "follower", "seguidor", "following", "seguidores", "like", "me gusta", "comment", "comentario", "share", "compartir", "DM", "mensaje directo", "inbox", "bandeja", "chatbot AI", "AI chat", "chat de IA", "voice assistant", "asistente de voz", "smart speaker", "altavoz inteligente", "wireless earbuds", "audífonos inalámbricos", "earbuds", "earphones", "headphones", "headset", "headphones Bluetooth", "noise cancelling", "cancelación de ruido", "ANC", "codec audio", "AAC", "aptX", "LDAC", "Dolby Atmos", "spatial audio", "audio espacial", "lossless", "sin pérdida", "hi-res audio", "alta resolución", "MP3 player", "reproductor MP3", "music player", "reproductor de música", "playlist", "lista de reproducción", "queue", "cola", "shuffle", "aleatorio", "repeat", "repetir", "equalizer", "ecualizador", "volume", "volumen", "soundbar", "barra de sonido", "subwoofer", "home theater", "cine en casa", "projector", "proyector", "laser projector", "proyector láser", "monitor gaming", "gaming monitor", "mechanical keyboard", "teclado mecánico", "switches", "teclas mecánicas", "keycap", "tecla", "mouse pad", "alfombrilla", "gaming mouse", "ratón gaming", "gaming headset", "auriculares gaming", "controller", "mando", "gamepad", "game pass", "Xbox Game Pass", "PlayStation Plus", "PS5", "PS4", "Xbox Series X", "Xbox Series S", "Switch OLED", "Nintendo Switch", "Steam Deck", "ROG Ally", "Legion Go", "handheld", "portátil", "portable console", "consola portátil", "cloud save", "guardado en la nube", "game save", "partida guardada", "multiplayer", "multijugador", "crossplay", "cross-platform", "plataforma cruzada", "DLC", "season pass", "pase de temporada", "in-game", "dentro del juego", "microtransaction", "microtransacción", "loot box", "caja de botín", "esports", "e-sports", "deportes electrónicos", "ranked", "clasificatoria", "server", "servidor de juego", "matchmaking", "emparejamiento", "latency", "lag", "ping alto", "jitter", "packet loss", "pérdida de paquetes", "FPS drop", "caída de FPS", "frame drop", "stutter", "tirones", "crash report", "informe de fallo", "crash logger", "registro de errores", "bug report", "informe de errores", "hotfix", "parche", "patch notes", "notas del parche", "changelog", "registro de cambios", "release notes", "notas de versión", "roadmap", "hoja de ruta", "feature", "función", "feature request", "solicitud de función", "user story", "historia de usuario", "workflow", "flujo de trabajo", "automation", "automatización", "shortcut", "atajo", "snippet", "fragmento de texto", "template", "plantilla", "clipboard history", "historial del portapapeles", "clipboard manager", "gestor de portapapeles", "screen capture", "captura de pantalla", "screenshot folder", "carpeta de capturas", "image preview", "vista previa de imagen", "thumbnail preview", "vista miniatura", "paste image", "pegar imagen", "drag handle", "asa de arrastre", "resize handle", "controlador de tamaño", "floating window", "ventana flotante", "touch region", "región táctil", "insets", "Insets", "InputConnection", "commitContent", "content URI", "URI de contenido", "MIME type", "tipo MIME", "Android manifest", "manifiesto Android", "input method", "método de entrada", "input method editor", "editor de método de entrada", "accessibility service", "servicio de accesibilidad", "foreground service", "servicio en primer plano", "background service", "servicio en segundo plano", "battery saver", "ahorro de batería", "power saving", "ahorro de energía", "wakelock", "wake lock", "Doze", "App Standby", "background restriction", "restricción en segundo plano", "privacy sandbox", "sandbox de privacidad", "permissions", "permisos", "permission manager", "gestor de permisos", "runtime permission", "permiso en tiempo de ejecución", "selected photos", "fotos seleccionadas", "media store", "MediaStore", "content observer", "observador de contenido", "clipboard listener", "listener de portapapeles", "listener", "callback", "event listener", "gestor de eventos", "touch event", "evento táctil", "MotionEvent", "ACTION_DOWN", "ACTION_UP", "multi-touch", "gesture detector", "detector de gestos", "swipe typing", "escritura deslizando", "glide typing", "gesture typing", "escritura gestual", "word prediction", "predicción de palabras", "next word", "siguiente palabra", "language model", "modelo de lenguaje", "trie", "trie dictionary", "diccionario trie", "frequency dictionary", "diccionario de frecuencias", "personal dictionary", "diccionario personal", "phonetic alias", "alias fonético", "phonetic matching", "coincidencia fonética", "Levenshtein", "distance", "distancia de Levenshtein", "Metaphone", "Soundex", "Spanish Soundex", "fonema", "fonemas", "pronunciación", "pronunciation", "speech recognition", "reconocimiento de voz", "voice recognition", "reconocimiento vocal", "speech model", "modelo acústico", "acoustic model", "modelo fonético", "Vosk model", "Vosk small Spanish", "modelo pequeño español", "offline speech recognition", "dictado sin conexión", "on-device", "en el dispositivo", "local processing", "procesamiento local", "privacy first", "privacidad primero", "no cloud", "sin nube", "no internet", "sin internet", "automatic capitalization", "mayúscula automática", "autocapitalize", "capitalización automática", "sentence case", "mayúscula de oración", "punctuation", "puntuación", "voice punctuation", "puntuación por voz", "silence threshold", "umbral de silencio", "silence timeout", "tiempo de silencio", "voice pause", "pausa de voz", "noise suppression", "supresión de ruido", "noise gate", "puerta de ruido", "automatic gain control", "control automático de ganancia", "audio input", "entrada de audio", "audio stream", "flujo de audio", "PCM", "WAV file", "archivo WAV", "sample rate", "frecuencia de muestreo", "16 kilohertz", "16 kHz", "mono audio", "audio mono", "stereo audio", "audio estéreo", "microphone permission", "permiso de micrófono", "voice profile", "perfil de voz", "speech calibration", "calibración de voz", "voice trainer", "entrenador de voz", "training phrase", "frase de entrenamiento", "pronunciation practice", "práctica de pronunciación", "accent", "acento", "regional accent", "acento regional", "Spanish accent", "acento español", "Latin American Spanish", "español latinoamericano", "Mexican Spanish", "español mexicano", "Argentinian Spanish", "español argentino", "Colombian Spanish", "español colombiano", "Castilian Spanish", "español castellano", "es-MX", "es-ES", "es-AR", "es-CO", "voice typing settings", "ajustes de dictado", "speech settings", "ajustes de voz", "voice command", "comando de voz", "voice commands", "comandos de voz", "comma", "coma", "period", "punto", "question mark", "signo de interrogación", "exclamation mark", "signo de exclamación", "new line", "nueva línea", "new paragraph", "nuevo párrafo", "delete last word", "borrar última palabra", "edit text", "editar texto", "undo", "deshacer", "redo", "rehacer", "select all", "seleccionar todo", "copy all", "copiar todo", "paste", "pegar", "cut", "cortar", "delete", "borrar", "backspace", "retroceso", "enter key", "tecla enter", "spacebar", "barra espaciadora", "shift key", "tecla shift", "caps lock", "bloq mayús", "number row", "fila numérica", "QWERTY", "Spanish keyboard", "teclado español", "Spanish layout", "distribución española", "Spanish QWERTY", "QWERTY español", "ñ", "á", "é", "í", "ó", "ú", "ü", "¿", "¡", "emoji keyboard", "teclado emoji", "emoji picker", "selector de emoji", "symbol keyboard", "teclado de símbolos", "long press", "pulsación larga", "key preview", "vista previa de tecla", "haptic feedback", "respuesta háptica", "vibration", "vibración", "tap target", "área táctil", "hitbox", "hit box", "smart hitbox", "zona inteligente", "one hand mode", "modo una mano", "left handed", "zurdo", "right handed", "diestro", "thumb typing", "escritura con pulgares", "large keys", "teclas grandes", "key height", "altura de tecla", "keyboard height", "altura del teclado", "keyboard size", "tamaño del teclado", "keyboard resize", "redimensionar teclado", "keyboard settings", "ajustes del teclado", "keyboard theme", "tema del teclado", "dark theme", "tema oscuro", "AMOLED black", "negro AMOLED", "accent color", "color de acento", "warm theme", "tema cálido", "light theme", "tema claro", "floating mode", "modo flotante", "dock keyboard", "acoplar teclado", "keyboard dock", "teclado acoplado", "screen bounds", "límites de pantalla", "screen size", "tamaño de pantalla", "display metrics", "métricas de pantalla", "screen resolution", "resolución de pantalla", "density independent pixels", "píxeles independientes de densidad", "dp", "sp", "pixel density", "densidad de píxeles", "DPI", "density", "densidad", "responsive UI", "interfaz adaptable", "scalable layout", "diseño escalable", "layout params", "parámetros de diseño", "FrameLayout", "LinearLayout", "ConstraintLayout", "ViewGroup", "Android View", "native view", "vista nativa", "XML layout", "layout XML", "resource", "recurso", "drawable", "vector drawable", "selector drawable", "shape drawable", "ripple effect", "efecto ripple", "touch feedback", "respuesta al toque", "pressed state", "estado presionado", "focus state", "estado enfocado", "layout stability", "estabilidad visual", "flicker", "parpadeo", "jumping layout", "salto de diseño", "reflow", "reflujo", "window insets", "Insets API", "WindowManager", "window manager", "decor view", "vista decor", "touchable region", "región táctil", "touchable insets", "content insets", "visible insets", "window gravity", "gravedad de ventana", "keyboard window", "ventana del teclado", "IME window", "ventana IME", "InputMethodService", "input view", "vista de entrada", "onStartInputView", "onFinishInputView", "onWindowHidden", "onComputeInsets", "onCreateInputView", "service lifecycle", "ciclo de vida del servicio", "activity lifecycle", "ciclo de vida de Activity", "MainActivity", "VozFlowIME", "VozFlowApplication", "CrashHandler", "crash logger", "auditor de errores", "diagnóstico", "diagnostic report", "informe diagnóstico", "stacktrace", "stack trace", "exception", "excepción", "uncaught exception", "excepción no controlada", "SharedPreferences", "preferencias compartidas", "SQLiteOpenHelper", "SQLite database", "base de datos SQLite", "database migration", "migración de base de datos", "data persistence", "persistencia de datos", "local database", "base de datos local", "expiration", "expiración", "time to live", "TTL", "one hour", "una hora", "pinned item", "elemento fijado", "pin", "fijar", "unpin", "desfijar", "recent item", "elemento reciente", "clipboard card", "tarjeta de portapapeles", "staggered grid", "cuadrícula escalonada", "RecyclerView", "StaggeredGridLayoutManager", "CardView", "MaterialCardView", "item decoration", "decoración de elemento", "adapter", "adaptador", "view holder", "ViewHolder", "image view", "ImageView", "center crop", "miniatura", "thumbnail", "image preview", "vista previa", "content description", "descripción de contenido", "image URI", "URI de imagen", "content URI", "URI de contenido", "MediaStore.Images.Media.EXTERNAL_CONTENT_URI", "External Storage", "almacenamiento externo", "READ_MEDIA_IMAGES", "READ_MEDIA_VISUAL_USER_SELECTED", "READ_EXTERNAL_STORAGE", "photo picker", "selector de fotos", "selected photos access", "acceso a fotos seleccionadas", "screenshot detection", "detección de capturas", "passive cleanup", "limpieza pasiva", "no background process", "sin procesos en segundo plano", "background work", "trabajo en segundo plano", "battery optimization", "optimización de batería", "battery life", "duración de batería", "idle", "reposo", "keyboard hidden", "teclado oculto", "keyboard visible", "teclado visible", "observer cleanup", "limpieza de observadores", "listener cleanup", "limpieza de listeners", "clipboard observer", "observador del portapapeles", "clipboard changed", "portapapeles modificado", "primary clip", "clip principal", "ClipData", "ClipDescription", "InputContentInfo", "commitContent", "MIME description", "descripción MIME", "image content", "contenido de imagen", "image insertion", "inserción de imagen", "image paste", "pegado de imagen", "rich content", "contenido enriquecido", "editor compatibility", "compatibilidad del editor", "WhatsApp image", "imagen de WhatsApp", "Telegram image", "imagen de Telegram", "browser input", "campo de navegador", "text input", "campo de texto", "edit field", "campo editable", "password field", "campo de contraseña", "secure field", "campo seguro", "incognito", "modo incógnito", "private field", "campo privado", "sensitive text", "texto sensible", "clipboard privacy", "privacidad del portapapeles", "clipboard history setting", "ajuste del historial", "clipboard toggle", "interruptor del portapapeles", "selection mode", "modo selección", "multi select", "selección múltiple", "bulk delete", "borrado múltiple", "edit clipboard", "editar portapapeles", "delete clipboard item", "eliminar elemento", "copy text", "copiar texto", "pasted text", "texto pegado", "text expansion", "expansión de texto", "text shortcut", "atajo de texto", "personal snippet", "fragmento personal", "word replacement", "reemplazo de palabra", "autocorrect suggestion", "sugerencia de autocorrección", "undo correction", "deshacer corrección", "undo deletion", "deshacer borrado", "undo history", "historial de deshacer", "editor action", "acción del editor", "context menu", "menú contextual", "toolbar", "barra de herramientas", "toolbar icon", "icono de barra", "trash icon", "icono de papelera", "delete button", "botón borrar", "copy button", "botón copiar", "paste button", "botón pegar", "cut button", "botón cortar", "microphone button", "botón micrófono", "grid button", "botón de menú", "scissors button", "botón de tijeras", "keyboard action", "acción de teclado", "hardware key", "tecla física", "soft keyboard", "teclado en pantalla", "virtual keyboard", "teclado virtual", "on-screen keyboard", "teclado táctil", "touch keyboard", "typing", "escritura", "fast typing", "escritura rápida", "typing speed", "velocidad de escritura", "typing accuracy", "precisión de escritura", "word frequency", "frecuencia de palabra", "language corpus", "corpus lingüístico", "Spanish vocabulary", "vocabulario español", "Spanish lexicon", "léxico español", "word list", "lista de palabras", "word completion", "completar palabra", "word suggestion", "sugerencia de palabra", "prediction model", "modelo predictivo", "predictive text", "texto predictivo", "next word prediction", "predicción de siguiente palabra", "contextual prediction", "predicción contextual", "language context", "contexto lingüístico", "prefix matching", "coincidencia de prefijo", "fuzzy matching", "coincidencia aproximada", "phonetic dictionary", "diccionario fonético", "phonetic correction", "corrección fonética", "Spanish phonetics", "fonética española", "Spanish phoneme", "fonema español", "Spanish grapheme", "grafema español", "regional pronunciation", "pronunciación regional", "accent adaptation", "adaptación de acento", "user learning", "aprendizaje del usuario", "local learning", "aprendizaje local", "user vocabulary", "vocabulario personal", "custom vocabulary", "vocabulario personalizado", "user alias", "alias de usuario", "pronunciation alias", "alias de pronunciación", "transcription alias", "alias de transcripción", "phonetic training", "entrenamiento fonético", "speech calibration phrase", "frase de calibración", "training progress", "progreso de entrenamiento", "ProgressBar", "SeekBar", "Slider", "control deslizante", "pause threshold", "umbral de pausa", "capitalization threshold", "umbral de mayúscula", "0.5 seconds", "medio segundo", "3 seconds", "tres segundos", "10 seconds", "diez segundos", "instant capitalization", "mayúscula inmediata", "sentence punctuation", "puntuación de oración", "proper noun", "nombre propio", "technical term", "término tecnológico", "internet vocabulary", "vocabulario de internet", "modern word", "palabra moderna", "anglicism", "anglicismo", "tech word", "palabra tecnológica", "product name", "nombre de producto", "application name", "nombre de aplicación", "brand name", "nombre de marca", "capitalization", "mayúsculas", "lowercase", "minúsculas", "uppercase", "mayúsculas sostenidas", "title case", "capitalización de título", "case sensitive", "sensible a mayúsculas", "case folding", "normalización de mayúsculas", "accent stripping", "eliminación de tildes", "diacritic", "diacrítico", "Unicode", "UTF-8", "locale", "configuración regional", "Locale", "Spanish locale", "idioma español", "español", "castellano", "LatAm", "Latinoamérica", "España", "México", "Argentina", "Colombia", "Chile", "Perú", "Venezuela", "Ecuador", "Uruguay", "Paraguay", "Bolivia", "Costa Rica", "Panamá", "Guatemala", "Honduras", "El Salvador", "Nicaragua", "Puerto Rico", "República Dominicana", "Estados Unidos", "Estados Unidos de América", "California", "Texas", "Florida", "Nueva York", "Madrid", "Barcelona", "Buenos Aires", "Ciudad de México", "Bogotá", "Lima", "Santiago", "Montevideo", "Quito", "Caracas", "San Juan", "Manus AI", "VozFlow Pro", "VozFlow", "Gboard", "Google", "WhatsApp", "Android", "Bluetooth", "Wi-Fi", "YouTube", "TikTok", "Instagram", "Telegram", "Facebook", "Gmail", "APK", "GIF", "ZIP", "PDF", "prompt", "link", "email", "online", "offline", "streaming", "software", "app", "apps", "chat", "IA", "AI", "Google Chrome", "WhatsApp Web", "Play Store", "App Store", "iPhone", "Samsung Galaxy", "Google Drive", "Google Photos", "ChatGPT", "OpenAI", "Vosk", "Kotlin", "Gradle", "GitHub", "Bluetooth", "router", "cloud", "smartphone", "screenshot", "captura", "Manus", "VozFlow Pro", "Android Studio", "Instagram Reels", "TikTok Live", "Google Maps", "Microsoft", "Apple", "Xiaomi", "Huawei", "Motorola", "Netflix", "Spotify", "Zoom", "Discord", "Slack", "YouTube Music", "Google Meet", "Google Docs", "Google Sheets", "Microsoft Teams", "OneDrive", "iCloud", "Dropbox", "Canva", "Figma", "CapCut", "Amazon", "Uber", "Waze", "PayPal", "Bitcoin", "Ethereum", "Meta", "Threads", "Bluesky", "Signal", "Snapchat", "Pinterest", "LinkedIn", "Reddit", "Twitch", "Steam", "PlayStation", "Xbox", "Nintendo Switch", "Chromecast", "AirPods", "Galaxy Buds", "Alexa", "Siri", "Bixby", "Gemini", "Copilot", "Claude", "Perplexity", "DeepL", "Duolingo", "Notion", "Trello", "Asana", "Jira", "WordPress", "Shopify", "Stripe", "Mercado Pago", "Rappi", "PedidosYa", "Didi", "Cabify", "Google Wallet", "Apple Pay", "Samsung Pay", "Google Authenticator", "Bitwarden", "Proton Mail", "NordVPN", "ExpressVPN", "YouTube Shorts", "Prime Video", "Disney+", "Max", "HBO Max", "Apple Music", "SoundCloud", "Shazam", "Audible", "Kindle", "Google Assistant", "Google Nest", "Samsung DeX", "One UI", "Android Auto", "Android TV", "Wear OS", "ChromeOS", "MacBook", "iPad", "MacBook Air", "MacBook Pro", "Galaxy S24", "Galaxy A55", "Pixel 9", "Pixel 10", "Samsung Galaxy S", "Google Pixel", "VozFlow Pro APK", "app VozFlow", "teclado VozFlow", "dictado VozFlow", "Gboard keyboard", "teclado Gboard", "Android keyboard", "teclado Android", "Google keyboard", "teclado Google", "internet móvil", "datos móviles", "red Wi-Fi", "señal Bluetooth", "conexión online", "modo offline", "archivo ZIP", "documento PDF", "código QR", "enlace URL", "página HTML", "archivo JSON", "base SQLite", "proyecto Android", "aplicación Kotlin", "código Kotlin", "plugin Gradle", "Android Manifest", "debug APK", "APK firmado", "teléfono Samsung", "Android trece", "Android catorce", "Android quince", "Galaxy Android", "G-board", "Gee board", "GeeBord", "Gibor", "Gboard Pro", "Whats app", "Whatsap", "Guasap", "Gugul", "Gúgol", "Youtube", "Yutub", "TicTok", "Tiktok", "Bluetú", "Bluetooth", "Wai fai", "Wifi", "Guugle", "Google app", "Apeca", "Apk", "Prompt engineering", "pront", "Chiboard", "Chi board", "Yibor", "Yiboard", "Gibord", "Gboard", "Gugul", "Gugel", "Watsap", "Wasap", "guasap", "tei gram", "telegrama", "insta gran", "tiktok", "yutu", "yutub", "androi", "androide", "blutu", "blu tu", "wai fai", "wifai", "router wifi", "ruter", "linkear", "lin", "emel", "imeil", "aplicasion", "aplicacion", "software", "sotgüer", "chatgpt", "chat ge pe te", "manus", "vosflou", "voz flow", "captura de pantalla", "gugul chrome", "guasap web", "google drive", "facebook", "feisbu", "instagram", "instagran", "telegram", "telegran", "whatsapp", "watsap", "you tube", "youtube music", "facebuk", "twitch", "twich", "spotify", "spotifai", "netflix", "netfli", "discord", "discor", "zoom", "zum", "bluetooth speaker", "batería", "screenshot", "escrinshot", "zip", "síp", "pe de efe", "pdf", "gepeg", "jota pe ge", "ene pe tres", "eme pe cuatro", "gip", "gif", "guif", "guif animado", "qr", "código qr", "código de barras", "usb", "u ese be", "wifi", "wi fi", "bluetooth", "internet", "interné", "online", "on line", "offline", "of line", "streaming", "strimin", "prompt", "pront", "apeka", "APK", "app", "apps", "chat", "chatbot", "emai", "mail", "email", "link", "lin", "cloud", "claud", "nube", "smartphone", "esmartfon", "tablet", "tableta", "laptop", "laptó", "notebook", "notbuk", "browser", "navegador", "google chrome", "chrome", "safari", "firefox", "opera", "edge", "duckduckgo", "bing", "search", "buscador", "google maps", "maps", "mapas", "google fotos", "google photos", "gmail", "yimeil", "correo", "outlook", "hotmail", "drive", "gdrive", "one drive", "onedrive", "icloud", "dropbox", "telegram", "whatsapp", "instagram", "tiktok", "facebook", "messenger", "discord", "slack", "teams", "meet", "zoom", "skype", "signal", "viber", "line", "wechat", "snapchat", "reddit", "pinterest", "linkedin", "threads", "bluesky", "twitter", "equis", "x", "youtube", "yt", "twitch", "kick", "spotify", "netflix", "prime video", "disney plus", "disney", "max", "hbo", "apple music", "soundcloud", "deezer", "tidal", "shazam", "kindle", "audible", "canva", "figma", "photoshop", "capcut", "premiere", "davinci", "obs", "notion", "trello", "asana", "jira", "confluence", "evernote", "keep", "google keep", "todoist", "calendar", "calendario", "google calendar", "docs", "documentos", "sheets", "hojas de cálculo", "excel", "word", "powerpoint", "slides", "presentaciones", "office", "microsoft 365", "workspace", "google workspace", "chatgpt", "chat gpt", "open ai", "openai", "gemini", "yémini", "copilot", "co pilot", "claude", "clod", "perplexity", "deepseek", "grok", "llama", "mistral", "prompt", "promt", "prom", "inteligencia artificial", "ia", "ai", "modelo de lenguaje", "robot", "bot", "asistente", "asistente virtual", "vozflow", "voz flow", "manus", "vosflou", "vosflow", "voz flo", "gboard", "g board", "g-board", "giboard", "yi bor", "yibor", "yibord", "chiboard", "chi board", "chivo art", "gugul", "gugel", "guugle", "gugle", "google", "pront", "prontt", "prompt", "apeca", "a pe ka", "apeká", "apk", "watsap", "guasap", "guasap", "whats app", "wa sap", "watsap", "whatsapp", "tele gran", "telegrán", "telegram", "insta gran", "instagram", "tik tok", "tiktok", "yu tub", "yutub", "youtube", "androi", "android", "blu tu", "blutu", "bluetooth", "wai fai", "waifai", "wifi", "wi fi", "router", "ruter", "enlace", "link", "emel", "imeil", "email", "en línea", "online", "sin conexión", "offline", "strimin", "streaming", "aplicación", "app", "aplicación móvil", "software", "sotgüer", "chat", "chad", "chat", "ia", "inteligencia artificial"
    ).distinct()

    private val exactAliases = mapOf(
        "yibor" to "Gboard", "yibord" to "Gboard", "yi bor" to "Gboard", "g board" to "Gboard", "g-board" to "Gboard", "gibord" to "Gboard", "giboard" to "Gboard", "chiboard" to "Gboard", "chi board" to "Gboard", "chivo art" to "Gboard", "gugul" to "Google", "gugel" to "Google", "guugle" to "Google", "gugle" to "Google", "pront" to "prompt", "prontt" to "prompt", "apeca" to "APK", "a pe ka" to "APK", "apeká" to "APK", "guasap" to "WhatsApp", "watsap" to "WhatsApp", "wasap" to "WhatsApp", "wa sap" to "WhatsApp", "whats app" to "WhatsApp", "tei gram" to "Telegram", "tele gran" to "Telegram", "telegrán" to "Telegram", "insta gran" to "Instagram", "yutub" to "YouTube", "yu tub" to "YouTube", "androi" to "Android", "androide" to "Android", "blutu" to "Bluetooth", "blu tu" to "Bluetooth", "wai fai" to "Wi-Fi", "waifai" to "Wi-Fi", "wifai" to "Wi-Fi", "ruter" to "router", "imeil" to "email", "emel" to "email", "sotgüer" to "software", "vosflou" to "VozFlow", "voz flow" to "VozFlow", "vosflow" to "VozFlow", "escrinshot" to "screenshot", "spotifai" to "Spotify", "discor" to "Discord", "zum" to "Zoom", "twich" to "Twitch", "chat ge pe te" to "ChatGPT", "open ai" to "OpenAI", "co pilot" to "Copilot", "yémini" to "Gemini", "deepl" to "DeepL"
    )

    // Compile stable Android ICU-compatible patterns once when this object is initialized.
    private val phraseAliasPatterns: List<Pair<Regex, String>> = exactAliases.entries
        .filter { it.key.contains(' ') }
        .sortedByDescending { it.key.length }
        .map { (wrong, right) -> Regex("(?i)(?<![\\p{L}\\p{N}])${Regex.escape(wrong)}(?![\\p{L}\\p{N}])") to right }
    private val wordTokenPattern = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}]+)?")
    private val combiningMarksPattern = Regex("\\p{Mn}+")
    private val softCPattern = Regex("c(?=[ei])")
    private val whitespacePattern = Regex("\\s+")
    private val repeatedCharacterPattern = Regex("(.)\\1+")

    private val canonical = technologyTerms.distinctBy { normalize(it) }.associateBy { normalize(it) }
    private val properNames = canonical.values.filter { it.any(Char::isUpperCase) }.map { normalize(it) }.toSet()

    fun correct(raw: String, personalWords: Collection<String> = emptyList()): String {
        if (raw.isBlank()) return raw
        var output = raw
        for ((pattern, right) in phraseAliasPatterns) {
            output = pattern.replace(output, right)
        }
        val words = (technologyTerms + personalWords).distinct()
        return wordTokenPattern.replace(output) { match ->
            val token = match.value
            val norm = normalize(token)
            exactAliases[norm]?.let { return@replace preserveCase(token, it) }
            canonical[norm]?.let { return@replace preserveCase(token, it) }
            if (token.length < 4) return@replace token
            val candidates = words.asSequence().filter { candidate ->
                    normalize(candidate).length >= 4 && (candidate in personalWords || candidate.any(Char::isUpperCase) && !candidate.all { !it.isLetter() || it.isUpperCase() })
                }
                .map { it to distance(phonetic(norm), phonetic(normalize(it))) }
                .filter { (candidate, d) -> d <= max(1, minOf(norm.length, normalize(candidate).length) / 4) }
                .sortedBy { it.second }
                .take(2)
                .toList()
            val best = candidates.firstOrNull()?.first ?: return@replace token
            val personal = personalWords.firstOrNull { normalize(it) == normalize(best) }
            preserveCase(token, personal ?: canonical[normalize(best)] ?: best)
        }
    }

        /** Literal, best correction/prefix completion and a second candidate, in that order. */
    fun suggestions(token: String, personalWords: Collection<String>, previousWord: String = ""): List<String> {
        if (token.isBlank()) {
            val next = nextWords[normalize(previousWord)] ?: listOf("de", "que")
            return listOf("", next.getOrElse(0) { "" }, next.getOrElse(1) { "" })
        }
        val norm = normalize(token)
        val personalNorms = personalWords.map(::normalize).toSet()
        val commonIndex = commonWords.mapIndexed { idx, w -> normalize(w) to idx }.toMap()
        val pool = (personalWords + commonWords + technologyTerms).distinctBy(::normalize)

        val ranked = pool.asSequence()
            .filter { normalize(it) != norm }
            .mapNotNull { word ->
                val candidate = normalize(word)
                val isPersonal = candidate in personalNorms
                val freqRank = commonIndex[candidate] ?: 999
                val score = when {
                    candidate.startsWith(norm) -> {
                        val lenDiff = (candidate.length - norm.length) * 0.05
                        val priorityBonus = when {
                            isPersonal -> -0.5
                            freqRank < 999 -> freqRank * 0.002
                            else -> 0.6
                        }
                        lenDiff + priorityBonus
                    }
                    else -> {
                        val dist = distance(candidate, norm)
                        val maxAllowedDist = if (norm.length <= 3) 1 else max(2, norm.length / 3)
                        if (dist > maxAllowedDist) return@mapNotNull null
                        dist + 3.0 + (if (isPersonal) -0.5 else 0.0)
                    }
                }
                word to score
            }
            .sortedBy { it.second }
            .take(6)
            .map { preserveCase(token, it.first) }
            .toList()

        val first = ranked.firstOrNull() ?: ""
        val second = ranked.drop(1).firstOrNull() ?: ""
        return listOf(token, first, second)
    }


    fun autoCorrectCandidate(token: String, personalWords: Collection<String>): String? {
        if (token.length < 4) return null
        val normalized = normalize(token)
        val known = (personalWords + technologyTerms.filter { it.any(Char::isUpperCase) && !it.all { char -> !char.isLetter() || char.isUpperCase() } }).distinct()
        val candidates = known.asSequence().filter { normalize(it) != normalized }
            .map { it to distance(phonetic(normalized), phonetic(normalize(it))) }
            .filter { (word, d) -> d <= max(1, minOf(normalized.length, normalize(word).length) / 4) }
            .sortedBy { it.second }.take(2).toList()
        val winner = candidates.firstOrNull()?.first ?: return null
        val personal = personalWords.firstOrNull { normalize(it) == normalize(winner) }
        val canonicalWord = personal ?: canonical[normalize(winner)] ?: winner
        return preserveCase(token, canonicalWord).takeIf { normalize(it) != normalized }
    }

    fun hasCorrection(raw: String, personalWords: Collection<String> = emptyList()): Boolean = correct(raw, personalWords) != raw

    fun isKnownProperNoun(word: String): Boolean = normalize(word) in properNames

    fun nextLetterScores(prefix: String, personalWords: Collection<String>): Map<Char, Int> {
        val key = normalize(prefix)
        if (key.isEmpty()) return emptyMap()
        val scores = mutableMapOf<Char, Int>()
        (commonWords + personalWords + technologyTerms).forEach { word ->
            val candidate = normalize(word)
            if (candidate.startsWith(key) && candidate.length > key.length) {
                val next = candidate[key.length]
                scores[next] = (scores[next] ?: 0) + if (word in personalWords) 5 else 1
            }
        }
        return scores
    }

    private fun preserveCase(original: String, replacement: String): String = when {
        original.all { !it.isLetter() || it.isUpperCase() } -> replacement.uppercase(Locale.ROOT)
        original.firstOrNull()?.isUpperCase() == true -> replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        else -> replacement
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(combiningMarksPattern, "").replace('’', '\'').trim()

    /** Spanish-oriented phonetic fold (b/v, c/qu/k, soft c/z, silent h, ll/y, ch). */
    private fun phonetic(value: String): String {
        var s = normalize(value).replace("qu", "k").replace("ll", "y").replace("ch", "x").replace("ph", "f")
        s = s.replace('v', 'b').replace('w', 'b').replace('z', 's').replace('j', 'x').replace('h', ' ')
        s = s.replace(softCPattern, "s").replace('c', 'k').replace('g', 'k')
            .replace(whitespacePattern, "")
        return s.replace(repeatedCharacterPattern, "$1")
    }

    private fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            previous = current
        }
        return previous[b.length]
    }

    private val nextWords = mapOf(
        "hola" to listOf("cómo", "qué"), "quiero" to listOf("que", "hacer"), "voy" to listOf("a", "con"),
        "buenos" to listOf("días", "tardes"), "buenas" to listOf("noches", "tardes"),
        "por" to listOf("favor", "la"), "muchas" to listOf("gracias", "veces"), "te" to listOf("quiero", "envío"),
        "el" to listOf("mensaje", "archivo"), "la" to listOf("app", "aplicación"), "en" to listOf("la", "el"),
        "abre" to listOf("Google", "WhatsApp"), "descarga" to listOf("el", "la"), "busca" to listOf("en", "Google")
    )
}
