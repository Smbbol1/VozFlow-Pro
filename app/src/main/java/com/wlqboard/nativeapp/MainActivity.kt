package com.wlqboard.nativeapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.log10

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var content: LinearLayout
    private var activeSection: LinearLayout? = null
    private var trainingController: VoiceDictationController? = null
    private val bg = Color.rgb(26, 25, 24)
    private val card = Color.rgb(37, 34, 32)
    private val bone = Color.rgb(234, 230, 225)
    private val muted = Color.rgb(180, 172, 164)
    private val accentChoices = listOf("#D8A76A", "#D88772", "#8AAE9B", "#8CA7D0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.install(this)
        try {
            prefs = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "MainActivity: carga de preferencias")
            showEmergencyDiagnostics()
            return
        }
        try { render() }
        catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "MainActivity: renderizado de configuración")
            showEmergencyDiagnostics()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) {
            try { render() }
            catch (failure: Exception) {
                CrashHandler.recordException(this, failure, "MainActivity: actualización al reanudar")
                showEmergencyDiagnostics()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 48) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            prefs.edit().putBoolean("clipboard_screenshots", granted).apply()
            toast(if (granted) "Capturas recientes habilitadas" else "Permiso de imágenes no concedido")
            render()
        } else if (requestCode == 47) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) showVoiceTrainingDialog()
            else toast("Sin permiso de micrófono no se puede grabar la frase de calibración.")
        }
    }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(32))
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        setContentView(scroll)

        text("Configuración de VozFlow Pro", 27, true).also {
            content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
        }
        addCrashDiagnosticsCard()
        if (!hasSetupComplete()) addSetupBanner()

        addSection("🌐", "Idiomas") {
            text("Español (QWERTY con Ñ)", 16, true)
            text("Diseño activo · Español", 13, false, muted)
        }
        addSection("🎛", "Preferencias") {
            addSeek("Altura del teclado", "height_percent", 70, 145, 100, "%")
            addSeek("Espaciado horizontal", "key_spacing", 1, 8, 4, " dp")
            addSeek("Tamaño de letra", "key_font_size", 14, 26, 19, " sp")
            addToggle("Modo dedos grandes / Evitar errores de pulsación", "big_keys_enabled", false)
            addSeek("Ampliación de teclas", "big_keys_scale", 100, 150, 120, "%")
            addSeek("Sensibilidad táctil", "touch_sensitivity", 1, 10, 5, " / 10")
            addOneHandSelector()
            addToggle("Fila de números superior", "number_row", true)
            addToggle("Vibración háptica al pulsar", "haptics", true)
            addToggle("Vista previa de letra", "key_preview", true)
        }
        addSection("🎨", "Tema") {
            val themes = listOf("Oscuro Cálido", "Claro", "Negro Puro AMOLED")
            val group = RadioGroup(this@MainActivity).apply { orientation = RadioGroup.VERTICAL }
            val current = prefs.getString("keyboard_theme", "warm") ?: "warm"
            listOf("warm", "light", "amoled").forEachIndexed { index, value ->
                val rb = RadioButton(this@MainActivity).apply {
                    text = themes[index]
                    setTextColor(bone)
                    buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
                    isChecked = current == value
                    setOnClickListener { prefs.edit().putString("keyboard_theme", value).apply() }
                }
                group.addView(rb)
            }
            addView(group)
            text("Color de acento", 14, true).also {
                addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(6) })
            }
            val colors = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            accentChoices.forEachIndexed { i, hex ->
                val chip = View(this@MainActivity).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(hex)) }
                    contentDescription = "Color de acento ${i + 1}"
                    setOnClickListener { prefs.edit().putInt("accent_index", i).apply() }
                }
                val lp = LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(14) }
                colors.addView(chip, lp)
            }
            addView(colors)
        }
        addSection("✓", "Correcciones y sugerencias") {
            addToggle("Autocorrección al pulsar espacio", "autocorrect", true)
            addToggle("Sugerencias en vivo", "suggestions", true)
            addToggle("Mayúscula automática", "auto_cap", true)
            addToggle("Puntuación automática", "auto_punctuation", false)
        }
        addSection("〰", "Escritura gestual") {
            addToggle("Mover cursor deslizando en espacio", "space_cursor", true)
            addToggle("Borrar palabras completas al mantener", "quick_delete", true)
        }
                addSection("🎙", "Dictado por voz") {
            text("Reconocimiento local en español · el audio no se envía a internet.", 13, false, muted)
            addSeek("Apagado por silencio", "silence_timeout_ms", 1500, 7000, 3500, " ms")
            addPauseCapsSeek()
            addToggle("Supresión de ruido ambiental", "noise_suppression", true)
            
            // NUEVO: Interruptor para desactivar la autocorrección en dictado
            addToggle("Desactivar corrección al dictar por voz", "disable_dictation_correction", false)
            
            text("Comandos: “coma”, “punto”, “signo de interrogación” y “nueva línea”.", 13, false, muted)
            button("Entrenar y calibrar voz") { showVoiceTrainingDialog() }
            button("Ver / gestionar palabras entrenadas") { manageVoiceAliases() }
        }
        addSection("📋", "Portapapeles") {
            addToggle("Guardar historial de textos copiados", "clipboard_enabled", true)
            addSeek("Límite de historial", "clipboard_limit", 1, 200, 20, " elementos")
            addScreenshotToggle()
            button("Borrar historial del portapapeles") {
                try { ClipboardStore(this@MainActivity).use { it.clear() } }
                catch (failure: Exception) { CrashHandler.recordException(this@MainActivity, failure, "MainActivity: vaciar base de portapapeles") }
                prefs.edit().putString("clipboard_history", "[]").putStringSet("clipboard_pinned", emptySet()).apply()
                toast("Historial eliminado")
            }
        }
        addSection("📖", "Diccionario personal") {
            text("Palabras, nombres y atajos locales (atajo → texto expandido).", 13, false, muted)
            val dictionary = readWords()
            if (dictionary.isNotEmpty()) text(dictionary.joinToString(" · "), 14, false)
            button("Agregar palabra") { editWord(null) }
            if (dictionary.isNotEmpty()) button("Editar o eliminar palabras") { manageWords() }
            button("Agregar atajo de texto") { editSnippet(null, null) }
            val snippets = readSnippets()
            if (snippets.isNotEmpty()) {
                text(snippets.entries.joinToString(" · ") { "${it.key} → ${it.value}" }, 13, false, muted)
                button("Editar o eliminar atajos") { manageSnippets() }
            }
        }
        text("VozFlow Pro · IME nativo para Android · sin permiso de internet", 12, false, muted)
            .also { content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) }) }
    }

    private fun addCrashDiagnosticsCard() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(12))
            background = rounded(Color.rgb(43, 36, 30), 14)
        }
        box.addView(text("🛠️ Auditor de Errores (Diagnóstico)", 17, true))
        val report = CrashHandler.getLastReport(this).ifBlank { "Sin errores registrados" }
        val reportScroll = ScrollView(this).apply {
            isFillViewport = true
            background = rounded(Color.rgb(27, 25, 24), 9)
        }
        val reportText = text(report, 12, false, muted).apply {
            gravity = Gravity.TOP or Gravity.START
            setTextIsSelectable(true)
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        reportScroll.addView(reportText, android.widget.FrameLayout.LayoutParams(-1, -2))
        box.addView(reportScroll, LinearLayout.LayoutParams(-1, dp(190)).apply {
            topMargin = dp(10)
            bottomMargin = dp(8)
        })
        val copy = Button(this).apply {
            text = "Copiar Informe de Error"
            isAllCaps = false
            setTextColor(Color.rgb(26, 25, 24))
            background = rounded(Color.rgb(216, 167, 106), 10)
            setOnClickListener {
                val latest = CrashHandler.getLastReport(this@MainActivity).ifBlank { "Sin errores registrados" }
                val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                manager.setPrimaryClip(ClipData.newPlainText("Informe de error VozFlow Pro", latest))
                toast("Informe de error copiado")
            }
        }
        box.addView(copy, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(3) })
        val clear = Button(this).apply {
            text = "Limpiar Registro"
            isAllCaps = false
            setTextColor(bone)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                CrashHandler.clear(this@MainActivity)
                toast("Registro de errores eliminado")
                render()
            }
        }
        box.addView(clear, LinearLayout.LayoutParams(-1, dp(44)))
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
    }

    private fun showEmergencyDiagnostics() {
        try {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(24), dp(20), dp(24))
                setBackgroundColor(bg)
            }
            layout.addView(text("🛠️ Auditor de Errores (Diagnóstico)", 20, true))
            val report = CrashHandler.getLastReport(this).ifBlank { "Sin errores registrados" }
            val scroll = ScrollView(this)
            scroll.addView(text(report, 13, false, muted).apply {
                gravity = Gravity.TOP or Gravity.START
                setTextIsSelectable(true)
            })
            layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            val copy = Button(this).apply {
                text = "Copiar Informe de Error"
                isAllCaps = false
                setOnClickListener {
                    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("Informe de error VozFlow Pro", report))
                    toast("Informe de error copiado")
                }
            }
            layout.addView(copy)
            setContentView(layout)
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "MainActivity: pantalla de diagnóstico de emergencia")
            setContentView(TextView(this).apply {
                text = "VozFlow Pro · No se pudo cargar la configuración. El fallo se guardó si el almacenamiento estaba disponible."
                setTextColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
            })
        }
    }

    private fun addSetupBanner() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(card, 14)
        }
        box.addView(text("Configuración rápida", 17, true))
        val micGranted = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val enabled = isImeEnabled()
        val selected = isImeSelected()
        setupButton(box, if (micGranted) "✓ Micrófono concedido" else "1 · Conceder permiso de micrófono", micGranted) {
            if (Build.VERSION.SDK_INT >= 23 && !micGranted) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 47)
        }
        setupButton(box, if (enabled) "✓ Teclado habilitado" else "2 · Habilitar teclado", enabled) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        setupButton(box, if (selected) "✓ Teclado predeterminado" else "3 · Seleccionar como predeterminado", selected) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
    }

    private fun hasSetupComplete(): Boolean {
        val mic = Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return mic && isImeEnabled() && isImeSelected()
    }

    private fun isImeEnabled(): Boolean {
        return try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.any { it.packageName == packageName }
        } catch (failure: SecurityException) {
            CrashHandler.recordException(this, failure, "MainActivity: consulta de IME habilitados")
            false
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "MainActivity: consulta de IME habilitados")
            false
        }
    }

    private fun isImeSelected(): Boolean {
        return try {
            val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            selected?.substringBefore('/') == packageName
        } catch (failure: SecurityException) {
            CrashHandler.recordException(this, failure, "MainActivity: consulta de IME predeterminado")
            false
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "MainActivity: consulta de IME predeterminado")
            false
        }
    }

    private fun setupButton(parent: LinearLayout, label: String, done: Boolean, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(if (done) Color.rgb(176, 207, 177) else bone)
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(46)))
    }

    private fun addSection(icon: String, title: String, body: LinearLayout.() -> Unit) {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14)
            clipToOutline = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(12), 0)
        }
        header.addView(text(icon, 20, false), LinearLayout.LayoutParams(dp(38), dp(54)))
        header.addView(text(title, 16, true), LinearLayout.LayoutParams(0, dp(54), 1f))
        header.addView(text("⌄", 20, false, muted))
        val inside = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(14))
            visibility = View.GONE
        }
        activeSection = inside
        inside.body()
        activeSection = null
        header.setOnClickListener { inside.visibility = if (inside.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        outer.addView(header)
        outer.addView(inside)
        content.addView(outer, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }

    private fun addSeek(label: String, key: String, min: Int, max: Int, default: Int, suffix: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(9), 0, dp(4)) }
        val labelView = text("$label · ${prefs.getInt(key, default)}$suffix", 14, false)
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = (prefs.getInt(key, default) - min).coerceIn(0, this.max)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = min + progress
                    labelView.text = "$label · $value$suffix"
                    if (fromUser) prefs.edit().putInt(key, value).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        row.addView(labelView)
        row.addView(seek)
        activeSection?.addView(row)
    }

    private fun addPauseCapsSeek() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(9), 0, dp(4)) }
        val valueMs = prefs.getInt("voice_auto_caps_pause_ms", 3000).coerceIn(0, 10_000)
        val labelView = text("Pausa para mayúscula automática · ${formatPause(valueMs)}", 14, false)
        row.addView(labelView)
        row.addView(text("Tiempo de silencio necesario al dictar para iniciar la siguiente palabra con mayúscula.", 12, false, muted))
        val seek = SeekBar(this).apply {
            max = 20 // 0 = inmediata; 1..20 = 0,5..10,0 segundos.
            progress = if (valueMs == 0) 0 else ((valueMs + 250) / 500).coerceIn(1, 20)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress.coerceIn(0, 20) * 500
                    labelView.text = "Pausa para mayúscula automática · ${formatPause(value)}"
                    if (fromUser) prefs.edit().putInt("voice_auto_caps_pause_ms", value).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        row.addView(seek)
        activeSection?.addView(row)
    }

    private fun formatPause(valueMs: Int): String = if (valueMs == 0) "Inmediato" else String.format(java.util.Locale.ROOT, "%.1f seg", valueMs / 1000.0)

    private fun addToggle(label: String, key: String, default: Boolean) {
        val toggle = Switch(this).apply {
            text = label
            textSize = 14f
            setTextColor(bone)
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
            minHeight = dp(48)
            thumbTintList = android.content.res.ColorStateList.valueOf(if (isChecked) Color.rgb(216, 167, 106) else Color.LTGRAY)
            trackTintList = android.content.res.ColorStateList.valueOf(Color.rgb(90, 78, 68))
        }
        activeSection?.addView(toggle, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addOneHandSelector() {
        activeSection?.addView(text("Modo una sola mano", 14, true))
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("left" to "Izquierda", "off" to "Desactivado", "right" to "Derecha").forEach { (value, label) ->
            val button = Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 11f
                setOnClickListener { prefs.edit().putString("one_hand_mode", value).apply() }
            }
            choices.addView(button, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        activeSection?.addView(choices)
    }

    private fun addScreenshotToggle() {
        val toggle = Switch(this).apply {
            text = "Incluir capturas recientes al abrir teclado/portapapeles"
            textSize = 14f
            setTextColor(bone)
            isChecked = prefs.getBoolean("clipboard_screenshots", false)
            minHeight = dp(48)
            setOnCheckedChangeListener { _, checked ->
                if (!checked) {
                    prefs.edit().putBoolean("clipboard_screenshots", false).apply()
                } else {
                    val imagePermissions = when {
                        Build.VERSION.SDK_INT >= 34 -> arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                        Build.VERSION.SDK_INT >= 33 -> arrayOf("android.permission.READ_MEDIA_IMAGES")
                        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    if (Build.VERSION.SDK_INT < 23 || imagePermissions.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
                        prefs.edit().putBoolean("clipboard_screenshots", true).apply()
                    } else {
                        prefs.edit().putBoolean("clipboard_screenshots", false).apply()
                        requestPermissions(imagePermissions, 48)
                    }
                }
            }
        }
        activeSection?.addView(toggle, LinearLayout.LayoutParams(-1, -2))
    }

    private fun showVoiceTrainingDialog() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            toast("Concede permiso de micrófono antes de entrenar la voz")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 47)
            return
        }
        val prompts = mutableListOf(
            "VozFlow Pro funciona sin conexión a internet." to "VozFlow Pro funciona sin conexión a internet",
            "Hoy hace buen tiempo y saldré a caminar." to "Hoy hace buen tiempo y saldré a caminar",
            "Por favor, escribe una coma, dos puntos y un punto." to "Por favor escribe una coma dos puntos y un punto",
            "Manus ayuda a organizar mis ideas y tareas." to "Manus ayuda a organizar mis ideas y tareas",
            "Esta es una prueba de dictado en español." to "Esta es una prueba de dictado en español",
            "Abre Android y revisa la configuración del teclado." to "Abre Android y revisa la configuración del teclado",
            "Conecta el teléfono por Bluetooth y Wi-Fi." to "Conecta el teléfono por Bluetooth y Wi-Fi",
            "Envíame un mensaje por WhatsApp y Telegram." to "Envíame un mensaje por WhatsApp y Telegram",
            "Busca la dirección en Google Maps." to "Busca la dirección en Google Maps",
            "El archivo APK está en la carpeta de descargas." to "El archivo APK está en la carpeta de descargas",
            "Gboard reconoce mi voz y escribe en español." to "Gboard reconoce mi voz y escribe en español",
            "La batería del smartphone está cargando." to "La batería del smartphone está cargando",
            "Google Chrome muestra una página web." to "Google Chrome muestra una página web",
            "Quiero instalar una aplicación desde Play Store." to "Quiero instalar una aplicación desde Play Store",
            "El dictado convierte mi voz en texto." to "El dictado convierte mi voz en texto"
        )
        var step = 0
        var recognized = ""
        var isRecording = false
        var readyToAdvance = false
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(4)) }
        val progress = text("Frase 1 de ${prompts.size}", 13, false, muted)
        val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = prompts.size
            this.progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(216, 167, 106))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(70, 64, 58))
        }
        val prompt = text(prompts.first().first, 18, true)
        val result = text("Pulsa Grabar y lee la frase en voz alta.", 14, false, muted)
        val wordInput = android.widget.EditText(this).apply { hint = "Palabra difícil o nombre propio"; setSingleLine(true) }
        val addWord = Button(this).apply { text = "Entrenar esta palabra dos veces"; isAllCaps = false }
        val record = Button(this).apply { text = "Grabar frase"; isAllCaps = false }
        layout.addView(text("Lee las frases. El audio se procesa localmente y no se guarda.", 13, false, muted))
        layout.addView(progress)
        layout.addView(progressBar, LinearLayout.LayoutParams(-1, dp(8)).apply { bottomMargin = dp(8) })
        layout.addView(prompt)
        layout.addView(result)
        layout.addView(record)
        layout.addView(wordInput)
        layout.addView(addWord)
        val dialog = AlertDialog.Builder(this).setTitle("Entrenar y calibrar voz").setView(layout)
            .setNegativeButton("Cerrar", null).create()
        trainingController = VoiceDictationController(
            context = this,
            inputConnection = { null },
            onState = { active, state -> runOnUiThread {
                isRecording = active
                if (dialog.isShowing) {
                    if (recognized.isBlank()) result.text = state
                    if (active) readyToAdvance = false
                    else {
                        readyToAdvance = false
                        layout.postDelayed({
                            if (dialog.isShowing && !isRecording) {
                                readyToAdvance = true
                                record.text = if (recognized.isNotBlank()) "Guardar resultado y continuar" else "Grabar frase"
                            }
                        }, 500)
                    }
                    record.text = when {
                        active && recognized.isNotBlank() -> "Esperando fin de frase…"
                        active -> "Detener grabación"
                        !readyToAdvance -> "Procesando…"
                        recognized.isNotBlank() -> "Guardar resultado y continuar"
                        else -> "Grabar frase"
                    }
                }
            } },
            onAmplitude = {},
            onError = { message -> runOnUiThread { if (dialog.isShowing) result.text = message } },
            onRecognized = { raw -> runOnUiThread {
                val fragment = raw.trim()
                if (fragment.isNotBlank()) recognized = listOf(recognized, fragment).filter(String::isNotBlank).joinToString(" ")
                if (dialog.isShowing) {
                    result.text = if (recognized.isBlank()) "No se reconoció texto; inténtalo otra vez." else "Escuchado: $recognized"
                    record.text = if (isRecording) "Esperando fin de frase…" else if (readyToAdvance) "Guardar resultado y continuar" else "Procesando…"
                }
            } },
            onCalibrationStats = { noise, pause ->
                val oldTimeout = prefs.getInt("silence_timeout_ms", 3500)
                val calibrated = (pause + 900).coerceIn(1800, 7000).toInt()
                val threshold = (noise * 2.5).coerceIn(0.006, 0.03).toFloat()
                prefs.edit().putFloat("voice_ambient_rms", noise.toFloat())
                    .putFloat("voice_ambient_db", (20.0 * log10(noise.coerceAtLeast(1e-9))).toFloat())
                    .putLong("voice_pause_ms", pause).putFloat("voice_start_rms", threshold)
                    .putInt("silence_timeout_ms", ((oldTimeout + calibrated) / 2).coerceIn(1800, 7000)).apply()
            }
        )
        record.setOnClickListener {
            if (isRecording) {
                record.text = "Procesando…"
                trainingController?.stop()
            } else if (recognized.isBlank()) {
                result.text = "Preparando motor offline…"
                record.text = "Escuchando…"
                isRecording = true
                readyToAdvance = false
                trainingController?.start()
            } else {
                if (!readyToAdvance) {
                    result.text = "Espera a que termine la transcripción antes de avanzar."
                    return@setOnClickListener
                }
                val expected = prompts.getOrNull(step)?.second.orEmpty()
                if (expected.isNotBlank()) learnVoiceErrors(expected, recognized)
                step++
                progressBar.progress = step
                recognized = ""
                if (step >= prompts.size) {
                    progress.text = "Calibración completada · ${prompts.size} frases y ${readVoiceRules().size} reglas locales"
                    prompt.text = "Perfil de Voz actualizado en este dispositivo."
                    result.text = "Puedes cerrar el asistente."
                    record.visibility = View.GONE
                } else {
                    progress.text = "Frase ${step + 1} de ${prompts.size}"
                    prompt.text = prompts[step].first
                    result.text = "Pulsa Grabar y lee la frase."
                    record.text = "Grabar frase"
                }
            }
        }
        addWord.setOnClickListener {
            val word = wordInput.text.toString().trim()
            if (word.isNotBlank()) {
                    prompts.add("Pronuncia «$word» una vez" to word)
                    prompts.add("Pronuncia «$word» por segunda vez" to word)
                    progressBar.max = prompts.size
                    wordInput.text.clear()
                progress.text = "Frase ${step + 1} de ${prompts.size}"
                result.text = "Palabra añadida dos veces al entrenamiento."
            }
        }
        dialog.setOnDismissListener {
            val controller = trainingController
            trainingController = null
            controller?.stop()
            Thread({ controller?.shutdown() }, "voice-training-cleanup").start()
        }
        dialog.show()
    }

    private fun readVoiceRules(): Map<String, String> = try {
        val json = JSONObject(prefs.getString("voice_profile_rules", "{}") ?: "{}")
        json.keys().asSequence().mapNotNull { key -> json.optString(key).takeIf(String::isNotBlank)?.let { key to it } }.toMap()
    } catch (_: Exception) { emptyMap() }

    private fun manageVoiceAliases() {
        val entries = readVoiceRules().toList().sortedBy { it.first }
        val labels = entries.map { "${it.first}  →  ${it.second}" }.toTypedArray()
        val builder = AlertDialog.Builder(this).setTitle("Alias fonéticos entrenados")
            .setMessage(if (entries.isEmpty()) "Aún no hay alias aprendidos. Puedes añadir una forma reconocida y su palabra correcta." else "Toca una regla para editarla o eliminarla.")
            .setPositiveButton("Añadir alias") { _, _ -> editVoiceAlias() }
            .setNegativeButton("Cerrar", null)
        if (entries.isNotEmpty()) builder.setItems(labels) { _, index -> editVoiceAlias(entries[index].first, entries[index].second) }
        builder.show()
    }

    private fun editVoiceAlias(source: String = "", target: String = "") {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val from = android.widget.EditText(this).apply { hint = "Forma que reconoce Vosk"; setText(source); setSingleLine(true) }
        val to = android.widget.EditText(this).apply { hint = "Palabra o nombre correcto"; setText(target); setSingleLine(true) }
        box.addView(from)
        box.addView(to)
        val builder = AlertDialog.Builder(this).setTitle(if (source.isBlank()) "Añadir alias" else "Editar alias")
            .setView(box)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
        if (source.isNotBlank()) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val wrong = from.text.toString().trim().lowercase(java.util.Locale.ROOT)
                val correct = to.text.toString().trim()
                if (wrong.isBlank() || correct.isBlank()) { toast("Completa ambas palabras."); return@setOnClickListener }
                val rules = readVoiceRules().toMutableMap()
                if (source.isNotBlank()) rules.remove(source)
                rules[wrong] = correct
                saveVoiceRules(rules)
                dialog.dismiss()
                manageVoiceAliases()
            }
            if (source.isNotBlank()) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val rules = readVoiceRules().toMutableMap()
                rules.remove(source)
                saveVoiceRules(rules)
                dialog.dismiss()
                manageVoiceAliases()
            }
        }
        dialog.show()
    }

    private fun saveVoiceRules(rules: Map<String, String>) {
        val json = JSONObject()
        rules.forEach { (wrong, correct) -> json.put(wrong, correct) }
        prefs.edit().putString("voice_profile_rules", json.toString()).apply()
    }

    private fun learnVoiceErrors(expected: String, heard: String) {
        val expectedWords = expected.split(Regex("[^\\p{L}\\p{N}]+" )).filter(String::isNotBlank)
        val heardWords = heard.split(Regex("[^\\p{L}\\p{N}]+" )).filter(String::isNotBlank)
        if (expectedWords.isEmpty() || heardWords.isEmpty()) return
        val rules = readVoiceRules().toMutableMap()
        val remaining = expectedWords.toMutableList()
        for (spokenWord in heardWords) {
            val match = remaining.minByOrNull { levenshtein(it.lowercase(), spokenWord.lowercase()) } ?: continue
            val distance = levenshtein(match.lowercase(), spokenWord.lowercase())
            if (!match.equals(spokenWord, true) && distance <= maxOf(1, match.length / 2)) {
                rules[spokenWord.lowercase(java.util.Locale.ROOT)] = match
                remaining.remove(match)
            } else if (match.equals(spokenWord, true)) remaining.remove(match)
        }
        saveVoiceRules(rules)
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            previous = current
        }
        return previous[b.length]
    }

    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(bone)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { action() }
        }
        activeSection?.addView(b, LinearLayout.LayoutParams(-1, dp(48)))
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int = bone): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun editWord(existing: String?) {
        val input = android.widget.EditText(this).apply {
            setText(existing.orEmpty())
            setTextColor(bone)
            setHintTextColor(muted)
            hint = "Palabra"
            setSingleLine(true)
        }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Agregar palabra" else "Editar palabra")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val word = input.text.toString().trim()
                if (word.isNotEmpty()) {
                    val list = readWords().toMutableSet().apply {
                        existing?.let(::remove)
                        add(word)
                    }
                    saveWords(list)
                    render()
                }
            }.show().also { styleDialog(it) }
    }

    private fun manageWords() {
        val words = readWords().toMutableList()
        if (words.isEmpty()) return
        AlertDialog.Builder(this).setTitle("Diccionario personal")
            .setItems(words.toTypedArray()) { _, which ->
                val word = words[which]
                AlertDialog.Builder(this).setItems(arrayOf("Editar", "Eliminar")) { _, action ->
                    if (action == 0) editWord(word) else {
                        words.remove(word)
                        saveWords(words.toSet())
                        render()
                    }
                }.show().also { styleDialog(it) }
            }.setNegativeButton("Cerrar", null).show().also { styleDialog(it) }
    }

    private fun readSnippets(): Map<String, String> = try {
        val json = JSONObject(prefs.getString("text_snippets", "{}") ?: "{}")
        json.keys().asSequence().mapNotNull { key ->
            json.optString(key).takeIf(String::isNotBlank)?.let { key to it }
        }.toMap()
    } catch (_: Exception) { emptyMap() }

    private fun editSnippet(existingShortcut: String?, existingText: String?) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val shortcut = android.widget.EditText(this).apply { setText(existingShortcut.orEmpty()); hint = "Atajo (por ejemplo @@)"; setSingleLine(true) }
        val expanded = android.widget.EditText(this).apply { setText(existingText.orEmpty()); hint = "Texto expandido" }
        form.addView(shortcut)
        form.addView(expanded)
        AlertDialog.Builder(this).setTitle("Atajo de texto").setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val key = shortcut.text.toString().trim()
                val value = expanded.text.toString().trim()
                if (key.isNotBlank() && value.isNotBlank()) {
                    val map = readSnippets().toMutableMap()
                    existingShortcut?.let(map::remove)
                    map[key] = value
                    val json = JSONObject()
                    map.forEach { (k, v) -> json.put(k, v) }
                    prefs.edit().putString("text_snippets", json.toString()).apply()
                    render()
                }
            }.show().also { styleDialog(it) }
    }

    private fun manageSnippets() {
        val entries = readSnippets().entries.toList()
        if (entries.isEmpty()) return
        AlertDialog.Builder(this).setTitle("Atajos de texto")
            .setItems(entries.map { "${it.key} → ${it.value}" }.toTypedArray()) { _, which ->
                val entry = entries[which]
                AlertDialog.Builder(this).setItems(arrayOf("Editar", "Eliminar")) { _, action ->
                    if (action == 0) editSnippet(entry.key, entry.value) else {
                        val map = readSnippets().toMutableMap().apply { remove(entry.key) }
                        val json = JSONObject()
                        map.forEach { (key, value) -> json.put(key, value) }
                        prefs.edit().putString("text_snippets", json.toString()).apply()
                        render()
                    }
                }.show().also { styleDialog(it) }
            }.setNegativeButton("Cerrar", null).show().also { styleDialog(it) }
    }

    private fun readWords(): Set<String> = try {
        val array = JSONArray(prefs.getString("personal_dictionary", "[]"))
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.toSet()
    } catch (_: Exception) { emptySet() }

    private fun saveWords(words: Set<String>) {
        val array = JSONArray()
        words.forEach(array::put)
        prefs.edit().putString("personal_dictionary", array.toString()).apply()
    }

    private fun styleDialog(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.background_dark)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.rgb(216, 167, 106))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.rgb(216, 167, 106))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
