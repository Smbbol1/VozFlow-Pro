package com.wlqboard.nativeapp

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Region
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.json.JSONArray

class VozFlowIME : InputMethodService() {
    private lateinit var prefs: SharedPreferences
    private lateinit var clipboard: ClipboardManager
    private var root: ViewGroup? = null
    private var keyboardCard: LinearLayout? = null
    private var toolbar: LinearLayout? = null
    private var suggestionBar: LinearLayout? = null
        private var floatingHandle: View? = null
    private var dragHandle: View? = null
    private var closeFloatingBtn: TextView? = null
    private var floatingBottom: View? = null
    private var floatingBottomDrag: View? = null
    private var resizeHandle: View? = null
    private var sizeTopHandle: View? = null
    private var sizeControls: View? = null
    private var sizePercentLabel: TextView? = null
    private var sizePositionHandle: View? = null
    private var keyRows: LinearLayout? = null
    private var passwordField = false
    private var numericField = false
    private var urlField = false
    private var shiftState = 0 // 0 lower, 1 one-shot, 2 caps lock
    private var lastShiftTap = 0L
    private var symbolPage = 0
    private var showingClipboard = false
    private var quickPanel: View? = null
    private var micButton: TextView? = null
    private var listening = false
    private var heldDelete = false
    private var freeSymbolLongPressed = false
    private var freeSymbolRunnable: Runnable? = null
    private var previewWindow: PopupWindow? = null
    private var activePopup: PopupWindow? = null
    private var fallbackMode = false
    private var fallbackShift = false
    private var floatingMode = false
    private var sizeEditing = false
    private var previewHeightPercent: Int? = null
    private var previewBottomMargin: Int? = null
    private var floatingX = 0
    private var floatingY = 0
    private var floatingWidthPercent = 78
    private var floatingHeightPercent = 100
    private var clipboardVisible = false
    private var clipboardStore: ClipboardStore? = null
    private var clipboardAdapter: ClipboardGridAdapter? = null
    private val selectedClipboardIds = mutableSetOf<Long>()
    private var clipboardSelectionMode = false
    private var toolbarToolsExpanded = false
    private var skipNextAutoCorrect = false
    private var undoAvailableUntil = 0L
    private var deleteSwipeActive = false
    private var deleteSwipeStartX = 0f
    private var deleteSwipeCursorStart = 0
    private var oneHandMode = "off"
    private var panelMode = ""
    private var lastSelectionStart = -1
    private var lastSelectionEnd = -1
    private var suppressSelectionRestore = false
    private var lastKeyClickAt = 0L
    private val undoRecords = ArrayDeque<UndoRecord>()
    private var cachedPersonalWords: List<String> = emptyList()
    private var cachedSnippets: Map<String, String> = emptyMap()
    private var deleteHandler = Handler(Looper.getMainLooper())
    private var modelReleaseHandler = Handler(Looper.getMainLooper())
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var voice: VoiceDictationController? = null
    private var audioPermissionPending = false
    private val main = Handler(Looper.getMainLooper())
    private val symbolsOne = listOf("@ # $ _ & - + ( ) /", "* \" ' : ; ! ? ¿ ¡ =", "% ^ ~ ` | \\ < > [ ]", "{ } € £ ¥ • … § ¶ ™")
    private val symbolsTwo = listOf("+ − × ÷ = ≠ < > ≤ ≥", "€ $ £ ¥ ¢ ₹ ₽ ₩ ₺", "% ‰ ^ √ π ∞ ≈", "© ® ™ ° • … ± ÷")
    private data class UndoRecord(val removed: String, val inserted: String, val left: String, val right: String)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        try {
            prefs = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: carga de preferencias")
        }
        try {
            clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                try { captureClipboard() }
                catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: portapapeles") }
            }
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: inicialización del portapapeles")
        }
    }

    override fun onCreateInputView(): View {
        return try {
            val view = layoutInflater.inflate(R.layout.ime_keyboard, null) as FrameLayout
            root = view
            keyboardCard = view.findViewById(R.id.ime_card)
            toolbar = view.findViewById(R.id.ime_toolbar)
            suggestionBar = view.findViewById(R.id.ime_suggestions)
                                    floatingHandle = view.findViewById(R.id.ime_floating_handle)
            dragHandle = view.findViewById(R.id.ime_drag_handle)
            
                        // NUEVO: Crear el botón de forma independiente (SIN insertarlo en la barra)
            closeFloatingBtn = TextView(this).apply {
                text = "←"
                textSize = 22f
                setTextColor(textColor())
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener {
                    setFloatingMode(false)
                    prefs.edit()
                        .putString("one_hand_mode", "off")
                        .putInt("floating_width_percent", 78)
                        .putInt("floating_height_percent", 100)
                        .putInt("height_percent", 100)
                        .putInt("keyboard_bottom_margin", 0)
                        .putInt("floating_x_dp", 0)
                        .putInt("floating_y_dp", 0)
                        .apply()
                    floatingX = 0
                    floatingY = 0
                    root?.let(::applyKeyboardHeight)
                    rebuildKeys()
                    rebuildToolbar()
                }
            }
            floatingBottom = view.findViewById(R.id.ime_floating_bottom)
            floatingBottomDrag = view.findViewById(R.id.ime_floating_bottom_drag)
            resizeHandle = view.findViewById(R.id.ime_resize_handle)
            sizeTopHandle = view.findViewById(R.id.ime_size_top_handle)
            sizeControls = view.findViewById(R.id.ime_size_controls)
            sizePercentLabel = view.findViewById(R.id.ime_size_percent)
            sizePositionHandle = view.findViewById(R.id.ime_size_position_handle)
            keyRows = view.findViewById(R.id.ime_key_rows)
            if (keyboardCard == null || toolbar == null || suggestionBar == null || floatingHandle == null || keyRows == null) throw IllegalStateException("Faltan contenedores del layout del teclado")
            floatingMode = prefs.getBoolean("floating_mode", false)
            floatingX = dp(prefs.getInt("floating_x_dp", 0))
            floatingY = dp(prefs.getInt("floating_y_dp", 0))
            floatingWidthPercent = prefs.getInt("floating_width_percent", 78).coerceIn(60, 95)
            floatingHeightPercent = prefs.getInt("floating_height_percent", 100).coerceIn(70, 145)
            applyKeyboardHeight(view)
            installWindowGestureHandles()
            installSizeEditorControls()
            rebuildToolbar()
            rebuildKeys()
            fallbackMode = false
            view
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: onCreateInputView / inflación del teclado")
            createFallbackInputView()
        }
    }

    private fun createFallbackInputView(): View {
        fallbackMode = true
        fallbackShift = false
        val container = LinearLayout(this)
        root = container
        keyboardCard = null
        toolbar = null
        keyRows = null
        try {
            populateFallbackKeyboard(container)
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: construcción del teclado de reserva")
            container.removeAllViews()
            container.setBackgroundColor(Color.rgb(26, 25, 24))
            container.addView(TextView(this).apply {
                text = "VozFlow Pro · modo seguro"
                setTextColor(Color.WHITE)
                setPadding(16, 16, 16, 16)
            })
        }
        return container
    }

    private fun activateFallbackKeyboard() {
        fallbackMode = true
        fallbackShift = false
        val host = root
        val container = when (host) {
            is LinearLayout -> host
            is FrameLayout -> LinearLayout(this).also {
                host.removeAllViews()
                host.setBackgroundColor(Color.TRANSPARENT)
                host.addView(it, FrameLayout.LayoutParams(-1, dp(270), Gravity.BOTTOM))
            }
            else -> LinearLayout(this).also { root = it; setInputView(it) }
        }
        keyboardCard = null
        toolbar = null
        keyRows = null
        try {
            populateFallbackKeyboard(container)
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: activación del teclado de reserva")
        }
    }

    private fun installWindowGestureHandles() {
        var downRawX = 0f
        var downRawY = 0f
        var startLeft = 0
        var startTop = 0
        val moveCard: (View?, MotionEvent) -> Boolean = move@{ _, event ->
            if (!floatingMode) return@move false
            val parent = root as? FrameLayout ?: return@move false
            val card = keyboardCard ?: return@move false
            val lp = (card.layoutParams as? FrameLayout.LayoutParams) ?: return@move false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    startLeft = lp.leftMargin; startTop = lp.topMargin
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.leftMargin = (startLeft + event.rawX - downRawX).toInt()
                    lp.topMargin = (startTop + event.rawY - downRawY).toInt()
                    card.layoutParams = lp
                    clampFloatingCard(false)
                    true
                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { 
                    clampFloatingCard(true)
                    // Guardar posición final en preferencias para que persista al cambiar de ventana
                    prefs.edit()
                        .putInt("floating_x_dp", (floatingX / resources.displayMetrics.density).toInt())
                        .putInt("floating_y_dp", (floatingY / resources.displayMetrics.density).toInt())
                        .apply()
                    true 
                }
                else -> false
            }
        }
        dragHandle?.setOnTouchListener(moveCard)
        floatingBottomDrag?.setOnTouchListener(moveCard)
        var startWidthPercent = floatingWidthPercent
        var startHeightPercent = floatingHeightPercent
                resizeHandle?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startWidthPercent = floatingWidthPercent
                    startHeightPercent = floatingHeightPercent
                    downRawX = event.rawX; downRawY = event.rawY
                    floatingMode
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!floatingMode) return@setOnTouchListener false
                    // Calculamos el nuevo ancho y alto sin límites (solo mínimo de 10% para que no desaparezca)
                    val newWidth = (startWidthPercent + ((event.rawX - downRawX) * 100f / resources.displayMetrics.widthPixels).toInt()).coerceAtLeast(10)
                    val newHeight = (startHeightPercent + ((event.rawY - downRawY) * 100f / dp(270)).toInt()).coerceAtLeast(10)
                    
                    floatingWidthPercent = newWidth
                    floatingHeightPercent = newHeight
                    
                    prefs.edit().putInt("floating_width_percent", floatingWidthPercent).putInt("floating_height_percent", floatingHeightPercent).apply()
                    root?.let(::applyKeyboardHeight)
                    rebuildKeys()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { clampFloatingCard(true); true }
                else -> false
            }
        }
    }

    private fun setFloatingMode(enabled: Boolean) {
        floatingMode = enabled
        prefs.edit().putBoolean("floating_mode", enabled).apply()
        if (enabled && floatingX == 0 && floatingY == 0) {
            floatingWidthPercent = prefs.getInt("floating_width_percent", 78).coerceIn(60, 95)
            floatingX = ((resources.displayMetrics.widthPixels - resources.displayMetrics.widthPixels * floatingWidthPercent / 100) / 2)
            floatingY = (resources.displayMetrics.heightPixels - dp(270) - dp(80)).coerceAtLeast(dp(32))
        }
        floatingHandle?.visibility = if (enabled) View.VISIBLE else View.GONE
        floatingBottom?.visibility = if (enabled) View.VISIBLE else View.GONE
        root?.let(::applyKeyboardHeight)
    }

    private fun clampFloatingCard(persist: Boolean) {
        val parent = root as? FrameLayout ?: return
        val card = keyboardCard ?: return
        val lp = card.layoutParams as? FrameLayout.LayoutParams ?: return
        val maxX = (parent.width - card.width).coerceAtLeast(0)
        val maxY = (parent.height - card.height).coerceAtLeast(0)
        lp.leftMargin = lp.leftMargin.coerceIn(0, maxX)
        lp.topMargin = lp.topMargin.coerceIn(0, maxY)
        card.layoutParams = lp
        floatingX = lp.leftMargin
        floatingY = lp.topMargin
        if (persist) prefs.edit().putInt("floating_x_dp", (floatingX / resources.displayMetrics.density).toInt())
            .putInt("floating_y_dp", (floatingY / resources.displayMetrics.density).toInt()).apply()
    }

    private fun applyKeyboardHeight(view: View) {
        val host = view as? ViewGroup ?: root ?: return
        val card = keyboardCard
        val oneHand = prefs.getString("one_hand_mode", "off") ?: "off"
        oneHandMode = oneHand
        val heightPercent = (previewHeightPercent ?: prefs.getInt("height_percent", 100)).coerceIn(70, 145)
        val bigScale = if (prefs.getBoolean("big_keys_enabled", false)) prefs.getInt("big_keys_scale", 120).coerceIn(100, 150) else 100
        val baseHeight = dp(270 * heightPercent * bigScale / 10_000)
        val bottomGap = (previewBottomMargin ?: prefs.getInt("keyboard_bottom_margin", 0)).coerceIn(0, 60).let(::dp)
        host.setBackgroundColor(Color.TRANSPARENT)
        host.clipChildren = false
        host.clipToPadding = false
        if (card == null) {
            host.layoutParams = FrameLayout.LayoutParams(-1, baseHeight)
            return
        }
                if (floatingMode) {
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            // Permitimos que el ancho vaya desde un 10% hasta el 100% de la pantalla
            val width = (screenW * floatingWidthPercent / 100).coerceIn(dp(100), screenW)
            val editExtra = if (sizeEditing) dp(56) else 0
            // Permitimos que el alto sea libre
            val height = ((baseHeight + editExtra) * floatingHeightPercent / 100).coerceAtLeast(dp(100))
            
            host.layoutParams = FrameLayout.LayoutParams(-1, screenH)
            card.layoutParams = FrameLayout.LayoutParams(width, height).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                // Recuperamos la posición guardada en preferencias si existe
                val savedX = prefs.getInt("floating_x_dp", 0)
                val savedY = prefs.getInt("floating_y_dp", 0)
                leftMargin = if (savedX > 0) dp(savedX) else floatingX
                topMargin = if (savedY > 0) dp(savedY) else floatingY
            }
                                                // FORZAR BORDES REDONDEADOS SIN USAR OUTLINEPROVIDER (Más rápido)
            val bgDrawable = GradientDrawable().apply {
                setColor(backgroundColor())
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(60, 55, 50))
            }
            card.background = bgDrawable
            card.clipToOutline = false // Desactivamos el recorte pesado
            card.elevation = dp(10).toFloat()
            
            // Aseguramos que los márgenes internos no se salgan de los bordes
            card.setPadding(dp(4), dp(4), dp(4), dp(4))
            
            floatingHandle?.visibility = View.VISIBLE
            floatingBottom?.visibility = View.VISIBLE
            
            // AÑADIR EL BOTÓN A LA BARRA SUPERIOR SIN ROMPER EL ARRASTRE
            closeFloatingBtn?.let { btn ->
                val parentBar = floatingHandle?.parent as? ViewGroup
                if (parentBar != null && btn.parent == null) {
                    // Lo insertamos al principio de la barra de arrastre
                    parentBar.addView(btn, 0, ViewGroup.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT))
                }
                btn.visibility = View.VISIBLE
            }
            
            getWindow().window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            getWindow().window?.setLayout(-1, screenH)
        } else {
            val width: Int
            val gravity: Int
            when (oneHand) {
                "right" -> { width = (resources.displayMetrics.widthPixels * 0.82f).toInt(); gravity = Gravity.BOTTOM or Gravity.RIGHT }
                "left" -> { width = (resources.displayMetrics.widthPixels * 0.82f).toInt(); gravity = Gravity.BOTTOM or Gravity.LEFT }
                else -> { width = -1; gravity = Gravity.BOTTOM }
            }
            val cardHeight = baseHeight + if (sizeEditing) dp(56) else 0
            host.layoutParams = FrameLayout.LayoutParams(-1, cardHeight + bottomGap)
            card.layoutParams = FrameLayout.LayoutParams(width, cardHeight).apply { this.gravity = gravity; bottomMargin = bottomGap }
            card.background = if (sizeEditing) GradientDrawable().apply { setColor(backgroundColor()); cornerRadius = dp(14).toFloat(); setStroke(dp(2), accentColor()) } else GradientDrawable().apply { setColor(backgroundColor()) }
                                    card.elevation = 0f
            floatingHandle?.visibility = View.GONE
            floatingBottom?.visibility = View.GONE
            
            // ELIMINAR EL BOTÓN DE LA BARRA PARA QUE NO SE QUEDE FLOTANDO EN MODO NORMAL
            closeFloatingBtn?.let { btn ->
                btn.visibility = View.GONE
                (btn.parent as? ViewGroup)?.removeView(btn)
            }
            
            getWindow().window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            getWindow().window?.setLayout(-1, cardHeight + bottomGap)
        }
        sizeTopHandle?.visibility = if (sizeEditing) View.VISIBLE else View.GONE
        sizeControls?.visibility = if (sizeEditing) View.VISIBLE else View.GONE
        if (!floatingMode) floatingBottom?.visibility = View.GONE
        host.requestLayout()
        keyboardCard?.requestLayout()
        clampFloatingCard(false)
    }

    private fun installSizeEditorControls() {
        keyboardCard?.findViewById<View>(R.id.ime_size_reset)?.setOnClickListener {
            previewHeightPercent = 100
            previewBottomMargin = 0
            if (floatingMode) {
                floatingWidthPercent = 78
                floatingHeightPercent = 100
                floatingX = ((resources.displayMetrics.widthPixels - resources.displayMetrics.widthPixels * floatingWidthPercent / 100) / 2)
                floatingY = (resources.displayMetrics.heightPixels - dp(270) - dp(80)).coerceAtLeast(dp(32))
                    prefs.edit().putInt("floating_width_percent", floatingWidthPercent).putInt("floating_height_percent", floatingHeightPercent)
                    .putInt("floating_x_dp", (floatingX / resources.displayMetrics.density).toInt())
                    .putInt("floating_y_dp", (floatingY / resources.displayMetrics.density).toInt()).apply()
            }
            sizePercentLabel?.text = "100%"
            root?.let(::applyKeyboardHeight)
            rebuildKeys()
        }
        keyboardCard?.findViewById<View>(R.id.ime_size_done)?.setOnClickListener {
            prefs.edit().putInt("height_percent", previewHeightPercent ?: 100)
                .putInt("keyboard_bottom_margin", previewBottomMargin ?: 0).apply()
            sizeEditing = false
            previewHeightPercent = null
            previewBottomMargin = null
            root?.let(::applyKeyboardHeight)
            rebuildKeys()
            rebuildToolbar()
        }
        var downY = 0f
        var initialHeight = 100
        sizeTopHandle?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = event.rawY; initialHeight = previewHeightPercent ?: prefs.getInt("height_percent", 100); true }
                MotionEvent.ACTION_MOVE -> {
                    val change = ((downY - event.rawY) * 100f / dp(270)).toInt()
                    previewHeightPercent = (initialHeight + change).coerceIn(70, 145)
                    sizePercentLabel?.text = "${previewHeightPercent}%"
                    root?.let(::applyKeyboardHeight)
                    rebuildKeys()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        var initialMargin = 0
        var initialFloatingTop = 0
        var initialFloatingLeft = 0
        var positionDownX = 0f
        sizePositionHandle?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY; positionDownX = event.rawX
                    initialMargin = previewBottomMargin ?: prefs.getInt("keyboard_bottom_margin", 0)
                    val lp = keyboardCard?.layoutParams as? FrameLayout.LayoutParams
                    initialFloatingTop = lp?.topMargin ?: floatingY; initialFloatingLeft = lp?.leftMargin ?: floatingX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (floatingMode) {
                        val card = keyboardCard ?: return@setOnTouchListener false
                        val lp = card.layoutParams as? FrameLayout.LayoutParams ?: return@setOnTouchListener false
                        lp.topMargin = (initialFloatingTop + (event.rawY - downY).toInt())
                        lp.leftMargin = (initialFloatingLeft + (event.rawX - positionDownX).toInt())
                        card.layoutParams = lp
                        clampFloatingCard(false)
                    } else {
                        previewBottomMargin = (initialMargin + ((downY - event.rawY) / resources.displayMetrics.density).toInt()).coerceIn(0, 60)
                        root?.let(::applyKeyboardHeight)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (floatingMode) clampFloatingCard(true); true }
                MotionEvent.ACTION_CANCEL -> { if (floatingMode) clampFloatingCard(false); true }
                else -> false
            }
        }
    }

    private fun enterSizeEditor() {
        sizeEditing = true
        previewHeightPercent = prefs.getInt("height_percent", 100).coerceIn(70, 145)
        previewBottomMargin = prefs.getInt("keyboard_bottom_margin", 0).coerceIn(0, 60)
        sizePercentLabel?.text = "${previewHeightPercent}%"
        panelMode = ""
        quickPanel = null
        applyKeyboardHeight(root ?: return)
        rebuildKeys()
        rebuildToolbar()
    }

    private fun populateFallbackKeyboard(container: LinearLayout) {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(dp(3), dp(3), dp(3), dp(4))
        container.setBackgroundColor(Color.rgb(26, 25, 24))
        container.layoutParams = FrameLayout.LayoutParams(-1, dp(250))
        val title = TextView(this).apply {
            text = "VozFlow Pro · modo seguro"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(216, 167, 106))
        }
        container.addView(title, LinearLayout.LayoutParams(-1, dp(28)))
        fun row(height: Int = 38, build: (LinearLayout) -> Unit) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            build(line)
            container.addView(line, LinearLayout.LayoutParams(-1, dp(height)))
        }
        fun key(parent: LinearLayout, label: String, weight: Float = 1f, special: Boolean = false, action: () -> Unit) {
            val view = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = if (label.length > 1) 13f else 17f
                setTextColor(Color.rgb(234, 230, 225))
                background = rounded(if (special) Color.rgb(61, 50, 40) else Color.rgb(44, 42, 41), 6)
                setOnClickListener { action() }
            }
            parent.addView(view, LinearLayout.LayoutParams(0, -1, weight).apply {
                leftMargin = dp(2); rightMargin = dp(2); topMargin = dp(2); bottomMargin = dp(2)
            })
        }
        fun commit(value: String) {
            try {
                currentInputConnection?.commitText(value, 1)
            } catch (failure: Exception) {
                CrashHandler.recordException(this, failure, "VozFlowIME: tecla del teclado de reserva")
            }
        }
        row { line -> "1234567890".forEach { char -> key(line, char.toString()) { commit(char.toString()) } } }
        row { line -> "qwertyuiop".forEach { char -> key(line, char.toString()) { commit(if (fallbackShift) char.uppercaseChar().toString() else char.toString()); fallbackShift = false } } }
        row { line -> "asdfghjkl".forEach { char -> key(line, char.toString()) { commit(if (fallbackShift) char.uppercaseChar().toString() else char.toString()); fallbackShift = false } } }
        row { line ->
            key(line, "⇧", 1.2f, true) { fallbackShift = !fallbackShift }
            "zxcvbnm".forEach { char -> key(line, char.toString()) { commit(if (fallbackShift) char.uppercaseChar().toString() else char.toString()); fallbackShift = false } }
            key(line, "⌫", 1.2f, true) {
                try { currentInputConnection?.deleteSurroundingText(1, 0) }
                catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: borrar en teclado de reserva") }
            }
        }
        row(42) { line ->
            key(line, "?123", 1.1f, true) { }
            key(line, ",", 0.8f, true) { commit(",") }
            key(line, "Español", 2.8f) { commit(" ") }
            key(line, ".", 0.8f, true) { commit(".") }
            key(line, "↵", 1.1f, true) { commit("\n") }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        try {
            val type = attribute?.inputType ?: InputType.TYPE_CLASS_TEXT
            val inputClass = type and InputType.TYPE_MASK_CLASS
            val variation = type and InputType.TYPE_MASK_VARIATION
            passwordField = inputClass == InputType.TYPE_CLASS_TEXT &&
                (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) ||
                inputClass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            numericField = inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_PHONE
            urlField = variation == InputType.TYPE_TEXT_VARIATION_URI || ((attribute?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEARCH
            if (fallbackMode) return
            shiftState = if (prefs.getBoolean("auto_cap", true) && !passwordField) 1 else 0
            symbolPage = 0
            showingClipboard = false
            refreshViewsSafely("VozFlowIME: onStartInput")
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: onStartInput / preferencias del editor")
            activateFallbackKeyboard()
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val moved = (lastSelectionStart >= 0 && (newSelStart != lastSelectionStart || newSelEnd != lastSelectionEnd)) ||
            (oldSelStart != newSelStart || oldSelEnd != newSelEnd)
        lastSelectionStart = newSelStart
        lastSelectionEnd = newSelEnd
        if (!suppressSelectionRestore && moved && panelMode.isNotBlank()) restoreKeys()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (fallbackMode) return
        try {
            modelReleaseHandler.removeCallbacks(releaseModelRunnable)
            cachedPersonalWords = readPersonalWords()
            cachedSnippets = readTextSnippets()
            root?.let(::applyKeyboardHeight)
            if (prefs.getBoolean("clipboard_enabled", true)) {
                registerClipboardListener()
                try { getClipboardStore().migrateLegacyAndPrune(prefs.getInt("clipboard_limit", 20).coerceIn(1, 200)) }
                catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: preparación de historial opcional") }
            }
            scanRecentScreenshots()
            refreshViewsSafely("VozFlowIME: onStartInputView")
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: onStartInputView")
            activateFallbackKeyboard()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        unregisterClipboardListener()
        stopVoice()
        modelReleaseHandler.removeCallbacks(releaseModelRunnable)
        releaseVoiceModelNow()
        closeClipboardStore()
        clearTransientUi()
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        unregisterClipboardListener()
        stopVoice()
        modelReleaseHandler.removeCallbacks(releaseModelRunnable)
        releaseVoiceModelNow()
        closeClipboardStore()
        clearTransientUi()
        super.onWindowHidden()
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        try {
            val decor = getWindow().window?.decorView
            val host = root
            val card = keyboardCard
            if (floatingMode && host != null && card != null) {
                val rect = Rect()
                val location = IntArray(2)
                card.getLocationInWindow(location)
                rect.set(location[0], location[1], location[0] + card.width, location[1] + card.height)
                val windowHeight = (decor?.height ?: host.height).coerceAtLeast(host.height)
                outInsets.contentTopInsets = windowHeight
                outInsets.visibleTopInsets = windowHeight
                outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
                outInsets.touchableRegion.set(rect)
            } else {
                val windowHeight = (decor?.height ?: host?.height ?: 0).coerceAtLeast(0)
                val contentHeight = card?.height ?: host?.height ?: 0
                val stableTop = (windowHeight - contentHeight).coerceAtLeast(0)
                outInsets.contentTopInsets = stableTop
                outInsets.visibleTopInsets = stableTop
                outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
            }
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: cálculo de insets/touch region")
        }
    }

    private fun registerClipboardListener() {
        if (clipboardVisible) return
        try {
            clipboardListener?.let(clipboard::addPrimaryClipChangedListener)
            clipboardVisible = true
        } catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: registrar listener visible del portapapeles") }
    }

    private fun unregisterClipboardListener() {
        if (!clipboardVisible) return
        try { clipboardListener?.let(clipboard::removePrimaryClipChangedListener) }
        catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: retirar listener del portapapeles") }
        clipboardVisible = false
    }

    private fun getClipboardStore(): ClipboardStore = clipboardStore ?: ClipboardStore(this).also { clipboardStore = it }

    private fun closeClipboardStore() {
        try { clipboardStore?.close() }
        catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: cerrar SQLite al ocultar teclado") }
        clipboardStore = null
    }

    private fun releaseVoiceModelNow() {
        try { voice?.shutdown() } catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: liberar modelo al ocultar teclado") }
        voice = null
    }

    private fun clearTransientUi() {
        main.removeCallbacksAndMessages(null)
        deleteHandler.removeCallbacksAndMessages(null)
        dismissKeyPreview()
        activePopup?.dismiss()
        activePopup = null
        sizeEditing = false
        previewHeightPercent = null
        previewBottomMargin = null
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            try {
                voice?.shutdown()
                voice = null
                listening = false
                refreshViewsSafely("VozFlowIME: actualización por presión de memoria")
            } catch (failure: Exception) {
                CrashHandler.recordException(this, failure, "VozFlowIME: liberación por presión de memoria")
                voice = null
            }
        }
    }

    override fun onDestroy() {
        unregisterClipboardListener()
        stopVoice()
        modelReleaseHandler.removeCallbacksAndMessages(null)
        try { clipboardListener?.let { clipboard.removePrimaryClipChangedListener(it) } }
        catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: limpieza del portapapeles") }
        try { voice?.shutdown() }
        catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: cierre del motor de voz") }
        voice = null
        try { clipboardStore?.close() } catch (_: Exception) { }
        clipboardStore = null
        super.onDestroy()
    }

    private val releaseModelRunnable = Runnable {
        try {
            voice?.shutdown()
            voice = createVoiceControllerSafely()
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: liberación diferida del modelo")
            voice = null
        }
    }

    private fun createVoiceControllerSafely(): VoiceDictationController? = try {
        newVoiceController()
    } catch (failure: Exception) {
        CrashHandler.recordException(this, failure, "VozFlowIME: creación del motor de voz")
        null
    }

    private fun newVoiceController() = VoiceDictationController(
        context = this,
        inputConnection = { currentInputConnection },
        onState = { active, _ -> main.post { listening = active; refreshViewsSafely("VozFlowIME: estado de dictado") } },
        onAmplitude = { level -> main.post { animateMic(level) } },
        onError = { message -> main.post { showToast(message) } }
    )

    private fun refreshViewsSafely(stage: String) {
        if (fallbackMode) return
        try {
            rebuildToolbar()
            rebuildKeys()
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, stage)
            activateFallbackKeyboard()
        }
    }

    private fun stopVoice() {
        try {
            listening = false
            voice?.stop()
            if (root != null) refreshViewsSafely("VozFlowIME: cierre de dictado")
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: detener dictado")
            listening = false
        }
    }

    private fun rebuildToolbar() {
        val bar = toolbar ?: return
        bar.removeAllViews()
        bar.setBackgroundColor(backgroundColor())
        suggestionBar?.visibility = View.GONE
        val token = currentToken()
        val hasNextWordContext = token.isEmpty() && currentInputConnection?.getTextBeforeCursor(80, 0)?.toString()?.let { it.isNotBlank() && it.lastOrNull()?.isWhitespace() == true } == true
        val suggestionsOn = !passwordField && prefs.getBoolean("suggestions", true) && !listening && (token.isNotEmpty() || hasNextWordContext)
        val showUndo = SystemClock.uptimeMillis() < undoAvailableUntil
        if (suggestionsOn && !toolbarToolsExpanded && !showUndo) {
            addToolbarButton(bar, "▦", 0.82f) { showModesPanel() }
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
            val mic = addToolbarButton(bar, "🎙", 0.9f) { toggleVoice() }
            styleMic(mic)
        } else if (showUndo) {
            addToolbarButton(bar, "▦", 0.85f) { showModesPanel() }
            val undo = addToolbarButton(bar, "↶ Deshacer", 3.8f) { undoAvailableUntil = 0L; undoLastEdit() }
            undo.textSize = 13f
            addToolbarButton(bar, "⇄", 0.85f) { undoAvailableUntil = 0L; toolbarToolsExpanded = false; rebuildToolbar() }
            val mic = addToolbarButton(bar, "🎙", 0.9f) { toggleVoice() }
            styleMic(mic)
        } else {
            addToolbarButton(bar, "▦", 0.75f) { if (token.isNotEmpty()) { toolbarToolsExpanded = false; rebuildToolbar() } else showModesPanel() }
            addToolbarButton(bar, "✂", 0.9f) { if (panelMode == "tools") restoreKeys() else showEditPanel() }
            addToolbarButton(bar, "📋", 0.9f) { toggleClipboard() }
            addToolbarButton(bar, "▣", 0.85f) { copyCurrentText() }
            addToolbarButton(bar, "▤", 0.85f) { trackedClipboardAction(paste = true) }
            addToolbarButton(bar, "⌫", 0.9f) { deleteCurrentFieldOrSelection() }
            val mic = addToolbarButton(bar, "🎙", 0.85f) { toggleVoice() }
            styleMic(mic)
        }
    }

    private fun styleMic(mic: TextView) {
        mic.background = RippleDrawable(ColorStateList.valueOf(accentColor()), InsetDrawable(circle(if (listening) Color.rgb(50, 94, 58) else Color.TRANSPARENT), dp(7), dp(4), dp(7), dp(4)), null)
        mic.setTextColor(if (listening) Color.rgb(130, 220, 145) else textColor())
        mic.setPadding(dp(3), dp(3), dp(3), dp(3))
        mic.textSize = 19f
        micButton = mic
    }

    private fun rebuildSuggestions() {
        val bar = suggestionBar ?: return
        bar.removeAllViews()
        bar.setBackgroundColor(backgroundColor())
        if (passwordField || (!listening && !prefs.getBoolean("suggestions", true))) { bar.visibility = View.GONE; return }
        if (listening) {
            bar.visibility = View.VISIBLE
            bar.addView(TextView(this).apply { text = "Escuchando…"; gravity = Gravity.CENTER; setTextColor(Color.rgb(130, 220, 145)) }, LinearLayout.LayoutParams(-1, -1))
            return
        }
        val words = candidateWords().filter { it != "😊" && it != "👍" && it != "❤️" }
        if (words.isEmpty() || currentToken().isEmpty()) { bar.visibility = View.GONE; return }
        bar.visibility = View.VISIBLE
        words.take(4).forEach { word ->
            val suggestion = TextView(this).apply {
                text = word
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(textColor())
                setOnClickListener { replaceCurrentWord(word) }
            }
            bar.addView(suggestion, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun currentToken(): String = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        .takeLastWhile { it.isLetterOrDigit() || it == '\'' || it == '’' }

    private fun selectAllAndShowActions() {
        val ic = currentInputConnection
        if (ic != null) {
            suppressSelectionRestore = true
            ic.performContextMenuAction(android.R.id.selectAll)
            main.postDelayed({ suppressSelectionRestore = false }, 300)
        }
        showSelectActionsPanel()
    }

    private fun addSeparator(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(58, 54, 52)) }, LinearLayout.LayoutParams(dp(1), dp(24)).apply { gravity = Gravity.CENTER_VERTICAL })
    }

    private fun addToolbarButton(parent: LinearLayout, label: String, weight: Float, action: () -> Unit): TextView {
        val tv = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 19f
            setTextColor(textColor())
            isClickable = true
            isFocusable = false
            background = RippleDrawable(ColorStateList.valueOf(accentColor()), rounded(Color.TRANSPARENT, 6), null)
            setOnClickListener {
                if (prefs.getBoolean("haptics", true)) performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                action()
            }
        }
        parent.addView(tv, LinearLayout.LayoutParams(0, -1, weight))
        return tv
    }

    private fun rebuildKeys() {
        val rows = keyRows ?: return
        keyboardCard?.setBackgroundColor(backgroundColor())
        rows.removeAllViews()
        rows.visibility = View.VISIBLE
        quickPanel = null
        showingClipboard = false
        if (numericField) {
            buildNumericPad(rows)
            return
        }
        if (prefs.getBoolean("number_row", true)) addKeyRow(rows, ("1234567890").map(Char::toString), KeyKind.CHAR)
        val row2: List<String> = if (symbolPage == 0) "qwertyuiop".map(Char::toString) else (if (symbolPage == 1) symbolsOne[0] else symbolsTwo[0]).split(" ")
        val row3: List<String> = if (symbolPage == 0) "asdfghjklñ".map(Char::toString) else (if (symbolPage == 1) symbolsOne[1] else symbolsTwo[1]).split(" ")
        val row4: List<String> = if (symbolPage == 0) "zxcvbnm".map(Char::toString) else (if (symbolPage == 1) symbolsOne[2] else symbolsTwo[2]).split(" ")
        addKeyRow(rows, row2, KeyKind.CHAR)
        addKeyRow(rows, row3, KeyKind.CHAR, sidePadding = true)
        addSpecialRow(rows, row4)
        addBottomRow(rows)
    }

    private enum class KeyKind { CHAR, SHIFT, DELETE, ACTION, SYMBOL_TOGGLE, CONTEXT, EMOJI, SPACE, PERIOD, SYMBOL_FREE }

    private inner class KeyTouchRow : LinearLayout(this) {
        private val pointerTargets = mutableMapOf<Int, View>()

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (childCount == 0) return super.dispatchTouchEvent(event)
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val child = chooseChild(event.getX(index)) ?: return super.dispatchTouchEvent(event)
                pointerTargets[id] = child
                dispatchPointer(event, id, child, forceCenter = prefs.getBoolean("big_keys_enabled", false) && (child.tag as? String).orEmpty().isNotEmpty())
                return true
            }
            if (pointerTargets.isEmpty()) return super.dispatchTouchEvent(event)
            if (action == MotionEvent.ACTION_MOVE) {
                pointerTargets.toMap().forEach { (id, child) -> dispatchPointer(event, id, child, forceCenter = prefs.getBoolean("big_keys_enabled", false) && (child.tag as? String).orEmpty().isNotEmpty()) }
                return true
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                val id = event.getPointerId(event.actionIndex)
                pointerTargets[id]?.let { dispatchPointer(event, id, it, forceCenter = prefs.getBoolean("big_keys_enabled", false) && (it.tag as? String).orEmpty().isNotEmpty()) }
                pointerTargets.remove(id)
                return true
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                pointerTargets.toMap().forEach { (id, child) -> dispatchPointer(event, id, child, forceCenter = true, cancel = true) }
                pointerTargets.clear()
                return true
            }
            return true
        }

        private fun chooseChild(x: Float): View? {
            val available = (0 until childCount).map { getChildAt(it) }.filter { it.visibility == View.VISIBLE }
            if (available.isEmpty()) return null
            val sorted = available.sortedBy { kotlin.math.abs(x - (it.left + it.width / 2f)) }
            val nearest = sorted.first()
            if (!prefs.getBoolean("big_keys_enabled", false) && x in nearest.left.toFloat()..nearest.right.toFloat()) return nearest
            if (sorted.size < 2) return nearest
            val d0 = kotlin.math.abs(x - (sorted[0].left + sorted[0].width / 2f))
            val d1 = kotlin.math.abs(x - (sorted[1].left + sorted[1].width / 2f))
            val sensitivity = prefs.getInt("touch_sensitivity", 5).coerceIn(1, 10)
            val ambiguous = kotlin.math.abs(d0 - d1) <= dp(4 + sensitivity)
            if (ambiguous && currentToken().isNotEmpty()) {
                val scores = SpanishPredictionEngine.nextLetterScores(currentToken(), readPersonalWords())
                val score0 = scores[(sorted[0].tag as? String)?.firstOrNull()?.lowercaseChar()] ?: 0
                val score1 = scores[(sorted[1].tag as? String)?.firstOrNull()?.lowercaseChar()] ?: 0
                if (score0 != score1) return if (score0 > score1) sorted[0] else sorted[1]
            }
            return nearest
        }

        private fun dispatchPointer(event: MotionEvent, pointerId: Int, child: View, forceCenter: Boolean, cancel: Boolean = false) {
            val index = event.findPointerIndex(pointerId)
            if (index < 0) return
            val x = if (forceCenter) child.width / 2f else (event.getX(index) - child.left).coerceIn(-dp(48).toFloat(), child.width + dp(48).toFloat())
            val y = if (forceCenter) child.height / 2f else (event.getY(index) - child.top).coerceIn(-dp(30).toFloat(), child.height + dp(30).toFloat())
            val sourceAction = if (cancel) MotionEvent.ACTION_CANCEL else when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val forwarded = MotionEvent.obtain(event.downTime, event.eventTime, sourceAction, x, y, event.metaState)
            child.dispatchTouchEvent(forwarded)
            forwarded.recycle()
        }
    }

    private fun newKeyRow() = KeyTouchRow().apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }

    private fun addKeyRow(parent: LinearLayout, values: List<String>, kind: KeyKind, sidePadding: Boolean = false) {
        val row = newKeyRow().apply { setPadding(if (sidePadding) dp(10) else 0, 0, if (sidePadding) dp(10) else 0, 0) }
        values.forEach { value -> row.addView(makeKey(displayed(value), KeyKind.CHAR, value), keyLayout(1f)) }
        parent.addView(row, rowLayout())
    }

    private fun addSpecialRow(parent: LinearLayout, values: List<String>) {
        val row = newKeyRow()
        if (symbolPage == 0) row.addView(makeKey(if (shiftState == 2) "⇧̲" else "⇧", KeyKind.SHIFT, ""), keyLayout(1.35f, true))
        values.forEach { value -> row.addView(makeKey(displayed(value), KeyKind.CHAR, value), keyLayout(1f)) }
        if (symbolPage != 0) row.addView(makeKey("=\\<", KeyKind.SYMBOL_TOGGLE, ""), keyLayout(1.25f, true))
        row.addView(makeKey("⌫", KeyKind.DELETE, ""), keyLayout(1.35f, true))
        parent.addView(row, rowLayout())
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = newKeyRow()
        row.addView(makeKey(if (symbolPage == 0) "?123" else "ABC", KeyKind.SYMBOL_TOGGLE, ""), keyLayout(1.15f, true))
        row.addView(makeKey(if (urlField) "/" else ",", KeyKind.CONTEXT, if (urlField) "/" else ","), keyLayout(0.8f, true))
        row.addView(makeKey("☺", KeyKind.EMOJI, ""), keyLayout(0.8f, true))
        row.addView(makeKey(prefs.getString("free_symbol", "@") ?: "@", KeyKind.SYMBOL_FREE, ""), keyLayout(0.8f, true))
        row.addView(makeKey("Español", KeyKind.SPACE, " "), keyLayout(3.1f))
        row.addView(makeKey(".", KeyKind.PERIOD, "."), keyLayout(0.8f, true))
        row.addView(makeKey(actionLabel(), KeyKind.ACTION, ""), keyLayout(1.2f, true))
        parent.addView(row, rowLayout())
    }

    private fun buildNumericPad(parent: LinearLayout) {
        val matrix = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#", "⌫"))
        matrix.forEachIndexed { index, values ->
            val row = newKeyRow()
            values.forEach { key ->
                val view = makeKey(key, if (key == "⌫") KeyKind.DELETE else KeyKind.CHAR, key)
                row.addView(view, keyLayout(1f, key == "⌫"))
            }
            parent.addView(row, LinearLayout.LayoutParams(-1, 0, if (index == 3) 1.05f else 1f).apply { topMargin = dp(3); bottomMargin = dp(3) })
        }
    }

    private fun makeKey(label: String, kind: KeyKind, value: String): TextView {
        val key = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            val bigScale = if (prefs.getBoolean("big_keys_enabled", false)) prefs.getInt("big_keys_scale", 120).coerceIn(100, 150) else 100
            val floatScale = if (floatingMode) (floatingWidthPercent / 78f).coerceIn(0.82f, 1.22f) else 1f
            val heightScale = ((previewHeightPercent ?: prefs.getInt("height_percent", 100)) / 100f) * (if (floatingMode) floatingHeightPercent / 100f else 1f)
            textSize = prefs.getInt("key_font_size", 19).toFloat() * bigScale / 100f * floatScale * heightScale.coerceIn(0.70f, 1.45f)
                        setTextColor(textColor())
            isClickable = true
            isFocusable = false
            background = keyRipple(when (kind) {
                KeyKind.CHAR -> keyColor()
                KeyKind.ACTION -> actionColor()
                else -> specialColor()
            })
            tag = value
            setPadding(dp(2), dp(2), dp(2), dp(2))
            if (prefs.getBoolean("key_preview", true)) setOnTouchListener { v, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    showKeyPreview(v, label)
                    // Si es una letra, escribimos INMEDIATAMENTE al tocar para quitar el lag
                    if (kind == KeyKind.CHAR) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        commit(value)
                    }
                }
                if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
                    dismissKeyPreview()
                }
                false
            }
        }
        when (kind) {
            KeyKind.SHIFT -> {
                if (shiftState == 2) key.background = keyRipple(accentColor())
                key.setOnClickListener { performKeyboardHaptic(key); toggleShift(); rebuildKeys(); rebuildToolbar() }
            }
            KeyKind.DELETE -> {
                key.setOnClickListener { performKeyboardHaptic(key); deleteOneTracked() }
                key.setOnLongClickListener {
                    performKeyboardHaptic(key)
                    if (prefs.getBoolean("quick_delete", true)) deleteWord()
                    heldDelete = true
                    deleteHandler.postDelayed(deleteRepeat, 180)
                    true
                }
                key.setOnTouchListener { _, event ->
                    val ic = currentInputConnection
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            deleteSwipeActive = false
                            lastSwipeWordCount = 0
                            deleteSwipeStartX = event.x
                            deleteSwipeCursorStart = extractedCursorPosition()
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - deleteSwipeStartX
                            if (dx < -dp(14)) {
                                deleteSwipeActive = true
                                val wordCount = (kotlin.math.abs(dx) / dp(44)).toInt().coerceAtLeast(1)
                                if (wordCount != lastSwipeWordCount) {
                                    lastSwipeWordCount = wordCount
                                    val before = ic?.getTextBeforeCursor(140, 0)?.toString().orEmpty()
                                    val availableWords = before.trimEnd().takeLastWhile { !it.isWhitespace() }.length
                                    val trimmed = before.trimEnd()
                                    val wordStart = trimmed.indexOfLast { it.isWhitespace() }
                                    var start = if (wordStart < 0) 0 else wordStart + 1
                                    var remaining = wordCount - 1
                                    while (remaining > 0 && start > 0) {
                                        start = trimmed.substring(0, start).trimEnd().indexOfLast { it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
                                        remaining--
                                    }
                                    val toDelete = (before.length - start).coerceAtLeast(availableWords)
                                    ic?.setSelection((deleteSwipeCursorStart - toDelete).coerceAtLeast(0), deleteSwipeCursorStart)
                                }
                                true
                            } else deleteSwipeActive
                        }
                        MotionEvent.ACTION_UP -> {
                            heldDelete = false
                            deleteHandler.removeCallbacks(deleteRepeat)
                            if (deleteSwipeActive) {
                                val selected = ic?.getSelectedText(0)?.toString().orEmpty()
                                if (selected.isNotEmpty()) {
                                    val left = ic?.getTextBeforeCursor(40, 0)?.toString().orEmpty()
                                    val right = ic?.getTextAfterCursor(40, 0)?.toString().orEmpty()
                                    ic?.commitText("", 1)
                                    pushUndo(selected, "", left, right)
                                    exposeUndoForFiveSeconds()
                                    performKeyboardHaptic(key)
                                    rebuildToolbar()
                                }
                                deleteSwipeActive = false
                                true
                            } else false
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            heldDelete = false
                            deleteHandler.removeCallbacks(deleteRepeat)
                            if (deleteSwipeActive) ic?.setSelection(deleteSwipeCursorStart, deleteSwipeCursorStart)
                            val consumed = deleteSwipeActive
                            deleteSwipeActive = false
                            consumed
                        }
                        else -> false
                    }
                }
            }
            KeyKind.SYMBOL_TOGGLE -> key.setOnClickListener {
                performKeyboardHaptic(key)
                if (label == "?123") symbolPage = 1 else if (label == "=\\<") symbolPage = 2 else symbolPage = 0
                rebuildKeys()
            }
                        KeyKind.SPACE -> key.setOnTouchListener { v, event -> 
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.isPressed = true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.isPressed = false
                }
                handleSpaceTouch(event)
                true 
            }
            KeyKind.CONTEXT -> key.setOnClickListener { commit(value) }
            KeyKind.EMOJI -> key.setOnClickListener { performKeyboardHaptic(key); showPopup(key, listOf("😀", "😂", "🥰", "😊", "😉", "😍", "👍", "❤️", "🙏", "🔥")) }
                        KeyKind.SYMBOL_FREE -> key.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.isPressed = true // <-- NUEVO: Resalta la tecla al tocarla
                        freeSymbolLongPressed = false
                        freeSymbolRunnable?.let(main::removeCallbacks)
                        freeSymbolRunnable = Runnable {
                            freeSymbolLongPressed = true
                            showPopup(view, listOf("@", "#", "$", "%", "-", "_", "&", "+", "=", "/")) { chosen ->
                                prefs.edit().putString("free_symbol", chosen).apply()
                                rebuildKeys()
                            }
                        }
                        main.postDelayed(freeSymbolRunnable!!, 1000)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        view.isPressed = false // <-- NUEVO: Quita el resaltado al soltar
                        freeSymbolRunnable?.let(main::removeCallbacks)
                        if (!freeSymbolLongPressed) commit(prefs.getString("free_symbol", "@") ?: "@")
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> { 
                        view.isPressed = false // <-- NUEVO: Quita el resaltado si se cancela el toque
                        freeSymbolRunnable?.let(main::removeCallbacks); 
                        true 
                    }
                    else -> true
                }
            }
            KeyKind.PERIOD -> {
                key.setOnClickListener { commit(".") }
                key.setOnLongClickListener { dismissKeyPreview(); showPopup(key, listOf(",", "?", "!", "¿", "¡", "@")); true }
            }
            KeyKind.ACTION -> key.setOnClickListener { performKeyboardHaptic(key); performAction() }
                        KeyKind.CHAR -> {
                // ELIMINADO el OnClickListener porque ahora se escribe en el OnTouchListener (ACTION_DOWN)
                // key.setOnClickListener { commit(value) } 
                val accents = when (value.lowercase()) {
                    "a" -> listOf("á", "à", "â", "ä")
                    "e" -> listOf("é", "è", "ê", "ë")
                    "i" -> listOf("í", "ì", "î", "ï")
                    "o" -> listOf("ó", "ò", "ô", "ö")
                    "u" -> listOf("ú", "ù", "û", "ü")
                    else -> emptyList()
                }
                if (accents.isNotEmpty()) key.setOnLongClickListener { dismissKeyPreview(); showPopup(key, accents); true }
            }
        }
        return key
    }

    private var lastSwipeWordCount = 0

    private val deleteRepeat = object : Runnable {
        override fun run() {
            if (!heldDelete) return
            if (prefs.getBoolean("quick_delete", true)) deleteWord() else deleteOneTracked()
            deleteHandler.postDelayed(this, 48)
        }
    }

    private fun handleSpaceTouch(event: MotionEvent) {
        val x = event.rawX
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                spaceDownX = x
                spaceLastX = x
                spaceMoved = false
                spaceDownTime = SystemClock.uptimeMillis()
                currentInputConnection?.let { ic ->
                    val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
                    if (extracted != null) {
                        spaceCursorMin = extracted.startOffset
                        spaceCursorMax = extracted.startOffset + extracted.text.length
                        spaceSelectionStart = extracted.selectionStart
                        spaceSelectionEnd = extracted.selectionEnd
                        spaceHadSelection = extracted.selectionStart != extracted.selectionEnd
                        spaceCursorPosition = extracted.startOffset + if (spaceHadSelection) minOf(extracted.selectionStart, extracted.selectionEnd) else extracted.selectionStart
                    } else {
                        spaceCursorMin = 0
                        spaceCursorPosition = ic.getTextBeforeCursor(4096, 0)?.length ?: 0
                        spaceCursorMax = spaceCursorPosition + (ic.getTextAfterCursor(4096, 0)?.length ?: 0)
                        spaceHadSelection = false
                    }
                }
                main.postDelayed(spaceLongPress, 1000)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - spaceLastX
                if (kotlin.math.abs(x - spaceDownX) > dp(8) && prefs.getBoolean("space_cursor", true)) {
                    spaceMoved = true
                    main.removeCallbacks(spaceLongPress)
                    val chars = (dx / dp(8)).toInt()
                    if (chars != 0) {
                        if (spaceHadSelection) {
                            spaceCursorPosition = spaceCursorMin + if (chars > 0) maxOf(spaceSelectionStart, spaceSelectionEnd) else minOf(spaceSelectionStart, spaceSelectionEnd)
                            spaceHadSelection = false
                        }
                        spaceCursorPosition = (spaceCursorPosition + chars).coerceIn(spaceCursorMin, spaceCursorMax)
                        currentInputConnection?.setSelection(spaceCursorPosition, spaceCursorPosition)
                        spaceLastX += chars * dp(8)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                main.removeCallbacks(spaceLongPress)
                if (!spaceMoved && SystemClock.uptimeMillis() - spaceDownTime < 1000) commit(" ")
            }
        }
    }

    private var spaceDownX = 0f
    private var spaceLastX = 0f
    private var spaceMoved = false
    private var spaceDownTime = 0L
    private var spaceCursorPosition = 0
    private var spaceCursorMin = 0
    private var spaceCursorMax = 0
    private var spaceSelectionStart = 0
    private var spaceSelectionEnd = 0
    private var spaceHadSelection = false
    private val spaceLongPress = Runnable {
        if (!spaceMoved) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun extractedCursorPosition(): Int = try {
        val extracted = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0)
        extracted?.let { it.startOffset + it.selectionEnd } ?: (currentInputConnection?.getTextBeforeCursor(4096, 0)?.length ?: 0)
    } catch (_: Exception) { currentInputConnection?.getTextBeforeCursor(4096, 0)?.length ?: 0 }

    private fun toggleShift() {
        val now = SystemClock.uptimeMillis()
        shiftState = if (shiftState == 1 && now - lastShiftTap < 360) 2 else if (shiftState == 2) 0 else 1
        lastShiftTap = now
    }

    private fun displayed(key: String): String = if (shiftState > 0) key.uppercase() else key

    private fun performKeyboardHaptic(view: View?) {
        if (prefs.getBoolean("haptics", true)) view?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun commit(value: String) {
        val ic = currentInputConnection ?: return
        val isLetter = value.length == 1 && value[0].isLetter()
        val isBoundary = value == " " || value in setOf(".", ",", "!", "?", ";", ":")
        val shouldCheckAutocorrect = isBoundary && !passwordField && prefs.getBoolean("autocorrect", true)
        val shouldUpdateSuggestions = !passwordField && prefs.getBoolean("suggestions", true) && !listening

        var text = if (shiftState > 0 && isLetter) value.uppercase() else value
        val before = if (shouldCheckAutocorrect || shouldUpdateSuggestions) {
            ic.getTextBeforeCursor(120, 0)?.toString().orEmpty()
        } else {
            ""
        }
        val selected = if (shouldCheckAutocorrect) ic.getSelectedText(0)?.toString().orEmpty() else ""
        var removedForUndo = selected
        var leftForUndo = before
        var didAutocorrect = false

        if (shouldCheckAutocorrect) {
            val token = before.takeLastWhile { it.isLetterOrDigit() || it == '\'' || it == '’' }
            if (token.isNotEmpty()) {
                val snippets = snippetsCache()
                val personal = personalWordsCache()
                val expansion = snippets.entries.firstOrNull { it.key.equals(token, ignoreCase = true) }?.value
                val corrections = mapOf("qeu" to "que", "donde" to "dónde", "tambien" to "también", "mas" to "más", "teh" to "the", "ola" to "hola")
                val corrected = if (skipNextAutoCorrect) null else expansion ?: corrections[token.lowercase()] ?: SpanishPredictionEngine.autoCorrectCandidate(token, personal)
                if (corrected != null) {
                    val after = ic.getTextAfterCursor(40, 0)?.toString().orEmpty()
                    ic.deleteSurroundingText(token.length, 0)
                    removedForUndo = token
                    leftForUndo = before.dropLast(token.length)
                    val replacement = if (expansion == null && token.firstOrNull()?.isUpperCase() == true) corrected.replaceFirstChar { it.uppercase() } else corrected
                    text = replacement + if (value == " ") " " else value
                    didAutocorrect = true
                    pushUndo(removedForUndo, text, leftForUndo, after)
                }
            }
        }
        if (isBoundary) skipNextAutoCorrect = false
        if (text in setOf(".", ",", "!", "?", ";", ":") && prefs.getBoolean("auto_punctuation", false)) text += " "

        ic.commitText(text, 1)
        if (!didAutocorrect && selected.isNotEmpty()) {
            pushUndo(selected, text, "", "")
        }
        if (didAutocorrect) exposeUndoForFiveSeconds() else undoAvailableUntil = 0L

        if (shiftState == 1 && isLetter) {
            shiftState = 0
            keyRows?.let { rows ->
                for (i in 0 until rows.childCount) {
                    val row = rows.getChildAt(i) as? ViewGroup ?: continue
                    for (j in 0 until row.childCount) {
                        val keyView = row.getChildAt(j) as? TextView ?: continue
                        val tag = keyView.tag as? String ?: continue
                        if (tag.length == 1 && tag[0].isLetter()) {
                            keyView.text = displayed(tag)
                        }
                    }
                }
            }
        }

        if (shouldUpdateSuggestions) {
            rebuildToolbar()
        }
    }

    private fun deleteOne() = deleteOneTracked()

    private fun deleteWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(80, 0)?.toString().orEmpty()
        val selected = ic.getSelectedText(0)?.toString().orEmpty()
        if (selected.isNotEmpty()) {
            ic.commitText("", 1)
            pushUndo(selected, "", before, after)
            exposeUndoForFiveSeconds()
            rebuildToolbar()
            return
        }
        if (before.isEmpty()) return
        val trailing = before.takeLastWhile { it.isWhitespace() }.length
        val rest = before.dropLast(trailing)
        val word = rest.takeLastWhile { !it.isWhitespace() }.length
        val count = (trailing + word).coerceAtLeast(1)
        val removed = before.takeLast(count)
        ic.deleteSurroundingText(count, 0)
        pushUndo(removed, "", before.dropLast(count), after)
        exposeUndoForFiveSeconds()
        rebuildToolbar()
    }

    private fun deleteOneTracked() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString().orEmpty()
        if (selected.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
            val after = ic.getTextAfterCursor(40, 0)?.toString().orEmpty()
            ic.commitText("", 1)
            pushUndo(selected, "", before, after)
            exposeUndoForFiveSeconds()
            rebuildToolbar()
            return
        }
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (before.isEmpty()) return
        val charCount = Character.charCount(before.codePointBefore(before.length))
        ic.deleteSurroundingText(charCount, 0)
        undoAvailableUntil = 0L
        if (!heldDelete && !passwordField && prefs.getBoolean("suggestions", true) && !listening) {
            rebuildToolbar()
        }
    }

    private fun pushUndo(removed: String, inserted: String, left: String, right: String) {
        undoRecords.addLast(UndoRecord(removed, inserted, left.takeLast(40), right.take(40)))
        while (undoRecords.size > 30) undoRecords.removeFirst()
    }

    private fun exposeUndoForFiveSeconds() {
        undoAvailableUntil = SystemClock.uptimeMillis() + 5000L
        main.postDelayed({
            if (root != null && SystemClock.uptimeMillis() >= undoAvailableUntil) rebuildToolbar()
        }, 5050L)
    }

    private fun undoLastEdit() {
        val ic = currentInputConnection ?: return
        val record = undoRecords.removeLastOrNull()
        if (record == null) { showToast("No hay cambios recientes para deshacer."); return }
        val before = ic.getTextBeforeCursor(300, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(300, 0)?.toString().orEmpty()
        val beforeMatches = if (record.inserted.isEmpty() && record.left.isEmpty()) before.isEmpty()
            else before.endsWith(record.left + record.inserted)
        val afterMatches = if (record.right.isEmpty()) after.isEmpty() else after.startsWith(record.right)
        if (!beforeMatches || !afterMatches) {
            undoRecords.addLast(record)
            showToast("El cursor cambió; no se puede deshacer con seguridad.")
            return
        }
        ic.deleteSurroundingText(record.inserted.length, 0)
        if (record.removed.isNotEmpty()) ic.commitText(record.removed, 1)
        rebuildToolbar()
    }

    private fun trackedClipboardAction(paste: Boolean) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(300, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(300, 0)?.toString().orEmpty()
        if (paste) {
            val clipText = try { clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty() } catch (_: Exception) { "" }
            ic.performContextMenuAction(android.R.id.paste)
            if (clipText.isNotEmpty()) pushUndo("", clipText, before, after)
        } else {
            val selected = ic.getSelectedText(0)?.toString().orEmpty()
            ic.performContextMenuAction(android.R.id.cut)
            if (selected.isNotEmpty()) pushUndo(selected, "", before, after)
        }
        restoreKeys()
    }

    private fun readTextSnippets(): Map<String, String> = try {
        val json = org.json.JSONObject(prefs.getString("text_snippets", "{}") ?: "{}")
        json.keys().asSequence().mapNotNull { key -> json.optString(key).takeIf(String::isNotBlank)?.let { key to it } }.toMap()
    } catch (_: Exception) { emptyMap() }

    private fun personalWordsCache(): List<String> {
        if (cachedPersonalWords.isEmpty()) cachedPersonalWords = readPersonalWords()
        return cachedPersonalWords
    }

    private fun snippetsCache(): Map<String, String> {
        if (cachedSnippets.isEmpty()) cachedSnippets = readTextSnippets()
        return cachedSnippets
    }

    private fun performAction() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
    }

    private fun actionLabel(): String = "↵"

    private fun hasTextAroundCursor(): Boolean = !currentInputConnection?.getTextBeforeCursor(1, 0).isNullOrEmpty() || !currentInputConnection?.getTextAfterCursor(1, 0).isNullOrEmpty()

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

    private fun readPersonalWords(): List<String> = try {
        val arr = JSONArray(prefs.getString("personal_dictionary", "[]") ?: "[]")
        (0 until arr.length()).map { arr.optString(it) }.filter(String::isNotBlank)
    } catch (_: Exception) { emptyList() }

    private fun copyCurrentText() {
        val ic = currentInputConnection ?: return
        if (ic.getSelectedText(0)?.isNotEmpty() == true) ic.performContextMenuAction(android.R.id.copy)
        else { ic.performContextMenuAction(android.R.id.selectAll); ic.performContextMenuAction(android.R.id.copy) }
        restoreKeys()
    }

    private fun deleteCurrentFieldOrSelection() {
        val ic = currentInputConnection ?: return
        try {
            val selected = ic.getSelectedText(0)?.toString().orEmpty()
            if (selected.isNotEmpty()) {
                val before = ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
                val after = ic.getTextAfterCursor(40, 0)?.toString().orEmpty()
                ic.commitText("", 1)
                pushUndo(selected, "", before, after)
                exposeUndoForFiveSeconds()
            } else {
                val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString().orEmpty()
                if (extracted.isNotEmpty() && extracted.length < 100_000) {
                    ic.performContextMenuAction(android.R.id.selectAll)
                    ic.commitText("", 1)
                    pushUndo(extracted, "", "", "")
                    exposeUndoForFiveSeconds()
                } else {
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
                }
            }
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: acción borrar de la barra")
            try {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
            } catch (_: Exception) { }
        }
        restoreKeys()
    }

    private fun replaceCurrentWord(word: String) {
        val ic = currentInputConnection ?: return
        skipNextAutoCorrect = false
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(80, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { it.isLetterOrDigit() || it == '\'' || it == '’' }
        if (token.isNotEmpty()) {
            ic.deleteSurroundingText(token.length, 0)
            ic.commitText(word, 1)
            pushUndo(token, word, before.dropLast(token.length), after)
        }
        suggestionBar?.visibility = View.GONE
        rebuildToolbar()
    }

    private fun showPopup(anchor: View, values: List<String>, onSelect: (String) -> Unit = { commit(it) }) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(5), dp(6), dp(5))
            background = rounded(specialColor(), 10)
        }
        val popup = PopupWindow(line, WindowManager.LayoutParams.WRAP_CONTENT, dp(48), true).apply { elevation = dp(6).toFloat() }
        activePopup = popup
        panelMode = "popup"
        values.forEach { value ->
            val item = TextView(this).apply {
                text = value
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(textColor())
                setPadding(dp(9), 0, dp(9), 0)
                setOnClickListener { onSelect(value); popup.dismiss(); activePopup = null; restoreKeys() }
            }
            line.addView(item, LinearLayout.LayoutParams(dp(43), -1))
        }
        popup.showAsDropDown(anchor, 0, -dp(68))
    }

    private fun showKeyPreview(anchor: View, label: String) {
        dismissKeyPreview()
        val preview = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 25f
            setTextColor(textColor())
            background = rounded(specialColor(), 12)
        }
        previewWindow = PopupWindow(preview, dp(48), dp(54), false).apply {
            elevation = dp(5).toFloat()
            showAsDropDown(anchor, 0, -dp(62))
        }
    }

    private fun dismissKeyPreview() {
        previewWindow?.dismiss()
        previewWindow = null
    }

    private fun toggleVoice() {
        if (listening) { stopVoice(); return }
        if (fallbackMode) { showToast("Dictado no disponible en modo seguro; las teclas siguen activas."); return }
        if (passwordField) { showToast("El dictado no está disponible en campos de contraseña."); return }
        try {
            val granted = Build.VERSION.SDK_INT < 23 || checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                showToast("Concede permiso de micrófono en VozFlow Pro antes de dictar.")
                try {
                    startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (failure: Exception) {
                    CrashHandler.recordException(this, failure, "VozFlowIME: abrir ajustes de permiso de audio")
                }
                return
            }
            modelReleaseHandler.removeCallbacks(releaseModelRunnable)
            val controller = voice ?: createVoiceControllerSafely()?.also { voice = it }
                ?: throw IllegalStateException("No se pudo crear el controlador de dictado")
            controller.start()
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: arranque del motor/modelo de voz")
            listening = false
            showToast("No se pudo iniciar el dictado; el teclado básico sigue disponible.")
            refreshViewsSafely("VozFlowIME: recuperación tras fallo de audio")
        }
    }

    private fun animateMic(level: Float) {
        val intensity = (level.coerceIn(0f, 1f) * 120).toInt()
        micButton?.setTextColor(Color.rgb(90 + intensity, 170 + (intensity / 2), 110 + (intensity / 3)))
    }

    private fun showQuickTools() {
        showEditPanel()
    }

    private fun showEditPanel() {
        showPanel(listOf(
            "Copiar todo" to { currentInputConnection?.performContextMenuAction(android.R.id.selectAll); currentInputConnection?.performContextMenuAction(android.R.id.copy) },
            "Pegar" to { trackedClipboardAction(paste = true) },
            "Cortar" to { trackedClipboardAction(paste = false) },
            "Portapapeles" to { toggleClipboard() },
            "Deshacer" to { undoLastEdit() },
            "↕ Cambiar tamaño" to { enterSizeEditor() },
            "Ajustes" to { openSettings() }
        ), "tools")
    }

    private fun showSelectActionsPanel() {
        showPanel(listOf(
            "Copiar" to { currentInputConnection?.performContextMenuAction(android.R.id.copy) },
            "Pegar" to { trackedClipboardAction(paste = true) }
        ), "select")
    }

    private fun showModesPanel() {
        showPanel(listOf(
            "Una mano izquierda" to { setFloatingMode(false); prefs.edit().putString("one_hand_mode", "left").apply(); root?.let(::applyKeyboardHeight) },
            "Una mano derecha" to { setFloatingMode(false); prefs.edit().putString("one_hand_mode", "right").apply(); root?.let(::applyKeyboardHeight) },
            "Teclado acoplado" to { setFloatingMode(false); prefs.edit().putString("one_hand_mode", "off").apply(); root?.let(::applyKeyboardHeight) },
            "Teclado flotante" to { setFloatingMode(true) }
        ), "modes")
    }

    private fun showPanel(actions: List<Pair<String, () -> Unit>>, mode: String) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(backgroundColor(), 12) }
        actions.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (label, action) ->
                val button = TextView(this).apply {
                    text = label
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(textColor())
                    background = rounded(keyColor(), 8)
                    setPadding(dp(5), dp(4), dp(5), dp(4))
                    setOnClickListener {
                        action()
                        if (label != "Portapapeles") restoreKeys()
                    }
                }
                row.addView(button, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(44), 1f))
            panel.addView(row)
        }
        val back = TextView(this).apply {
            text = "↩ Volver a letras (ABC)"
            gravity = Gravity.CENTER
            setTextColor(accentColor())
            setOnClickListener { restoreKeys() }
        }
        panel.addView(back, LinearLayout.LayoutParams(-1, dp(42)))
        showPanelView(panel, mode)
    }

    private fun toggleClipboard() {
        if (showingClipboard) { restoreKeys(); return }
        scanRecentScreenshots()
        showingClipboard = true
        clipboardSelectionMode = false
        selectedClipboardIds.clear()
        renderClipboardPanel()
    }

    private data class ClipboardCell(val header: String? = null, val entry: ClipboardStore.Entry? = null)

    private fun renderClipboardPanel() {
        try { renderClipboardPanelContent() }
        catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: render del panel de portapapeles")
            showToast("El portapapeles no está disponible; el teclado sigue activo.")
            restoreKeys()
        }
    }

    private fun renderClipboardPanelContent() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(5), dp(8), dp(6)); background = rounded(backgroundColor(), 12) }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = TextView(this).apply { text = "←"; textSize = 20f; setTextColor(accentColor()); gravity = Gravity.CENTER; setOnClickListener { restoreKeys() } }
        heading.addView(back, LinearLayout.LayoutParams(dp(36), dp(42)))
        heading.addView(TextView(this).apply { text = "Portapapeles"; textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(textColor()) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        val multi = TextView(this).apply {
            text = if (clipboardSelectionMode) "✓" else "✎"
            textSize = 18f; setTextColor(accentColor()); gravity = Gravity.CENTER
            setOnClickListener { clipboardSelectionMode = !clipboardSelectionMode; selectedClipboardIds.clear(); renderClipboardPanel() }
        }
        heading.addView(multi, LinearLayout.LayoutParams(dp(38), dp(42)))
        if (clipboardSelectionMode) {
            heading.addView(TextView(this).apply {
                text = "Borrar ${selectedClipboardIds.size}"; setTextColor(accentColor()); gravity = Gravity.CENTER
                setOnClickListener { getClipboardStore().remove(selectedClipboardIds); selectedClipboardIds.clear(); renderClipboardPanel() }
            }, LinearLayout.LayoutParams(dp(72), dp(42)))
        }
        val enabledSwitch = Switch(this).apply {
            text = ""
            isChecked = prefs.getBoolean("clipboard_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("clipboard_enabled", checked).apply()
                if (checked && !clipboardVisible) registerClipboardListener()
                if (!checked) unregisterClipboardListener()
            }
        }
        heading.addView(enabledSwitch, LinearLayout.LayoutParams(dp(52), dp(42)))
        panel.addView(heading)

        val recycler = RecyclerView(this).apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setBackgroundColor(Color.TRANSPARENT)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.set(dp(4), dp(4), dp(4), dp(4))
                }
            })
        }
        val db = getClipboardStore()
        db.migrateLegacyAndPrune(prefs.getInt("clipboard_limit", 20).coerceIn(1, 200))
        val rows = mutableListOf<ClipboardCell>()
        val all = db.entries()
        val pinned = all.filter { it.pinned }
        val recent = all.filterNot { it.pinned }
        if (pinned.isNotEmpty()) { rows += ClipboardCell(header = "Fijados"); pinned.forEach { rows += ClipboardCell(entry = it) } }
        if (recent.isNotEmpty()) { rows += ClipboardCell(header = "Recientes"); recent.forEach { rows += ClipboardCell(entry = it) } }
        val adapter = ClipboardGridAdapter(rows)
        clipboardAdapter = adapter
        recycler.adapter = adapter
        panel.addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))
        if (all.isEmpty()) panel.addView(TextView(this).apply {
            text = if (prefs.getBoolean("clipboard_enabled", true)) "Copia texto o imágenes para guardarlos aquí." else "El historial está pausado. Actívalo para guardar elementos."
            setTextColor(textColor()); gravity = Gravity.CENTER; textSize = 13f
        }, LinearLayout.LayoutParams(-1, dp(48)))
        showPanelView(panel, "clipboard")
    }

    private inner class ClipboardGridAdapter(private val cells: List<ClipboardCell>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val typeHeader = 0
        private val typeItem = 1
        override fun getItemViewType(position: Int) = if (cells[position].header != null) typeHeader else typeItem
        override fun getItemCount() = cells.size
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == typeHeader) {
                val label = TextView(this@VozFlowIME).apply { textSize = 12f; setTypeface(typeface, Typeface.BOLD); setTextColor(accentColor()); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(3), dp(3), dp(3), dp(3)) }
                return object : RecyclerView.ViewHolder(label) {}
            }
            val card = CardView(this@VozFlowIME).apply { radius = dp(12).toFloat(); cardElevation = dp(3).toFloat(); setCardBackgroundColor(keyColor()); useCompatPadding = true }
            val body = LinearLayout(this@VozFlowIME).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(9), dp(10), dp(9)) }
            card.addView(body, FrameLayout.LayoutParams(-1, -2))
            return object : RecyclerView.ViewHolder(card) { val content = body }
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val cell = cells[position]
            if (cell.header != null) {
                (holder.itemView as TextView).text = cell.header
                (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
                return
            }
            val entry = cell.entry ?: return
            val card = holder.itemView as CardView
            val body = (holder as? ClipboardItemHolder)?.content ?: (card.getChildAt(0) as? LinearLayout)
            val target = body ?: return
            target.removeAllViews()
            if (entry.image) {
                val image = ImageView(this@VozFlowIME).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                    background = rounded(keyColor(), 9)
                    contentDescription = "Imagen del portapapeles"
                    try { setImageBitmap(loadClipboardThumbnail(Uri.parse(entry.payload.removePrefix(ClipboardStore.IMAGE_PREFIX)))) }
                    catch (failure: Exception) { CrashHandler.recordException(this@VozFlowIME, failure, "VozFlowIME: miniatura de portapapeles") }
                }
                target.addView(image, LinearLayout.LayoutParams(-1, dp(100)))
            } else {
                target.addView(TextView(this@VozFlowIME).apply {
                    text = entry.payload.replace("\n", " ").take(180); textSize = 13f; setTextColor(textColor()); maxLines = 7
                }, LinearLayout.LayoutParams(-1, -2))
            }
            target.addView(TextView(this@VozFlowIME).apply {
                text = when { selectedClipboardIds.contains(entry.id) -> "✓ Seleccionado"; entry.pinned -> "📌 Fijado"; entry.image -> "Imagen"; else -> "Tocar para pegar · mantener para editar" }
                textSize = 10f; setTextColor(accentColor()); setPadding(0, dp(6), 0, 0)
            })
            card.setOnClickListener {
                if (clipboardSelectionMode) {
                    if (!selectedClipboardIds.add(entry.id)) selectedClipboardIds.remove(entry.id)
                    renderClipboardPanel()
                } else if (entry.image) pasteClipboardImage(entry) else {
                    currentInputConnection?.commitText(entry.payload, 1)
                    restoreKeys()
                }
            }
            card.setOnLongClickListener { showClipboardActions(entry); true }
        }
        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder.itemViewType == typeHeader) (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
        }
    }

    private inner class ClipboardItemHolder(val content: LinearLayout) : RecyclerView.ViewHolder(content)

    private fun showClipboardActions(entry: ClipboardStore.Entry) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(backgroundColor(), 12) }
        panel.addView(TextView(this).apply { text = if (entry.image) "Acciones de imagen" else "Acciones del texto"; textSize = 16f; setTextColor(textColor()); gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-1, dp(40)))
        fun option(label: String, action: () -> Unit) {
            panel.addView(TextView(this).apply { text = label; textSize = 14f; setTextColor(textColor()); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, dp(40)))
        }
        option(if (entry.pinned) "Desfijar" else "Fijar arriba") { getClipboardStore().setPinned(entry.id, !entry.pinned); renderClipboardPanel() }
        if (!entry.image) option("Editar texto") { showClipboardEditor(entry) }
        option("Eliminar") { getClipboardStore().remove(entry.id); renderClipboardPanel() }
        option("← Volver") { renderClipboardPanel() }
        showPanelView(panel, "clipboard_action")
    }

    private fun showClipboardEditor(entry: ClipboardStore.Entry) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(backgroundColor(), 12) }
        panel.addView(TextView(this).apply { text = "Editar elemento"; textSize = 16f; setTextColor(textColor()) })
        val input = EditText(this).apply { setText(entry.payload); setTextColor(textColor()); setHintTextColor(textColor()); minLines = 3; gravity = Gravity.TOP; setSelection(text.length) }
        panel.addView(input, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(TextView(this).apply { text = "Cancelar"; gravity = Gravity.CENTER; setTextColor(textColor()); setOnClickListener { renderClipboardPanel() } }, LinearLayout.LayoutParams(0, dp(42), 1f))
        actions.addView(TextView(this).apply {
            text = "Guardar"; gravity = Gravity.CENTER; setTextColor(accentColor())
            setOnClickListener {
                try { getClipboardStore().updateText(entry.id, input.text.toString()); renderClipboardPanel() }
                catch (failure: Exception) { CrashHandler.recordException(this@VozFlowIME, failure, "VozFlowIME: actualizar elemento SQLite"); showToast("No se pudo actualizar este elemento.") }
            }
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        panel.addView(actions)
        showPanelView(panel, "clipboard_edit")
    }

    private fun pasteClipboardImage(entry: ClipboardStore.Entry) {
        if (Build.VERSION.SDK_INT < 25) { showToast("Este Android no admite pegar imágenes desde el teclado."); return }
        val uri = Uri.parse(entry.payload.removePrefix(ClipboardStore.IMAGE_PREFIX))
        val mime = try { contentResolver.getType(uri) ?: "image/*" } catch (_: Exception) { "image/*" }
        val supported = currentInputEditorInfo?.contentMimeTypes?.any { it == "image/*" || it.equals(mime, true) } == true
        if (!supported) { showToast("Este campo no admite imágenes pegadas."); return }
        try {
            val content = InputContentInfo(uri, ClipDescription("Imagen", arrayOf(mime)), null)
            val accepted = currentInputConnection?.commitContent(content, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, Bundle()) == true
            if (accepted) restoreKeys() else { content.releasePermission(); showToast("La aplicación no aceptó la imagen.") }
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: commitContent de imagen")
            showToast("No se pudo pegar la imagen en este campo.")
        }
    }

    private fun loadClipboardThumbnail(uri: Uri): Bitmap? {
        val targetW = dp(210).coerceAtLeast(1)
        val targetH = dp(110).coerceAtLeast(1)
        if (Build.VERSION.SDK_INT >= 29) {
            try { return contentResolver.loadThumbnail(uri, Size(targetW, targetH), null) }
            catch (_: Exception) { }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetW * 2 || bounds.outHeight / sample > targetH * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1); inPreferredConfig = Bitmap.Config.RGB_565 }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun showPanelView(panel: View, mode: String = "panel") {
        quickPanel = panel
        panelMode = mode
        keyRows?.removeAllViews()
        keyRows?.addView(panel, LinearLayout.LayoutParams(-1, -1))
    }

    private fun restoreKeys() {
        activePopup?.dismiss()
        activePopup = null
        showingClipboard = false
        quickPanel = null
        panelMode = ""
        rebuildKeys()
        rebuildToolbar()
    }

    private fun captureClipboard() {
        if (!clipboardVisible || passwordField || !prefs.getBoolean("clipboard_enabled", true)) return
        try {
            val clip = clipboard.primaryClip ?: return
            val store = getClipboardStore()
            val limit = prefs.getInt("clipboard_limit", 20).coerceIn(1, 200)
            for (index in 0 until clip.itemCount) {
                val item = clip.getItemAt(index)
                val uri = item.uri
                if (uri != null && contentResolver.getType(uri)?.startsWith("image/") == true) {
                    store.add(ClipboardStore.IMAGE_PREFIX + uri, true, limit)
                } else {
                    val value = item.coerceToText(this)?.toString()?.trim().orEmpty()
                    if (value.isNotBlank()) store.add(value, false, limit)
                }
            }
            if (showingClipboard) renderClipboardPanel()
        } catch (failure: Exception) { CrashHandler.recordException(this, failure, "VozFlowIME: registrar clip visible") }
    }

    private fun scanRecentScreenshots() {
        if (!prefs.getBoolean("clipboard_screenshots", false) || passwordField) return
        val imagePermissions = when {
            Build.VERSION.SDK_INT >= 34 -> listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
            Build.VERSION.SDK_INT >= 33 -> listOf("android.permission.READ_MEDIA_IMAGES")
            else -> listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= 23 && imagePermissions.none { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) return
        try {
            val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
            val seen = prefs.getStringSet("seen_screenshot_uris", emptySet()).orEmpty().toMutableSet()
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                var examined = 0
                while (cursor.moveToNext() && examined < 40) {
                    examined++
                    val name = cursor.getString(nameColumn).orEmpty().lowercase()
                    if (name.contains("screenshot") || name.contains("screen_shot") || name.contains("captura")) {
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)).toString()
                        if (seen.add(uri)) getClipboardStore().add(ClipboardStore.IMAGE_PREFIX + uri, true, prefs.getInt("clipboard_limit", 20).coerceIn(1, 200))
                    }
                }
            }
            getClipboardStore().migrateLegacyAndPrune(prefs.getInt("clipboard_limit", 20).coerceIn(1, 200))
            prefs.edit().putStringSet("seen_screenshot_uris", seen.take(200).toSet()).apply()
        } catch (failure: Exception) {
            CrashHandler.recordException(this, failure, "VozFlowIME: búsqueda puntual de capturas")
        }
    }

    private fun openSettings() {
        try { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { showToast("Abre VozFlow Pro desde el lanzador para ver ajustes.") }
    }

    private fun rowLayout() = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(3); bottomMargin = dp(3) }
    private fun keyLayout(weight: Float, special: Boolean = false) = LinearLayout.LayoutParams(0, -1, weight).apply {
        val baseGap = prefs.getInt("key_spacing", 4).coerceIn(1, 8)
        val gap = if (prefs.getBoolean("big_keys_enabled", false)) (baseGap - 1).coerceAtLeast(1) else baseGap
        leftMargin = dp((gap / 2).coerceAtLeast(1)); rightMargin = dp((gap / 2).coerceAtLeast(1)); topMargin = dp(2); bottomMargin = dp(2)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun keyRipple(color: Int) = RippleDrawable(
        ColorStateList.valueOf(Color.argb(85, Color.red(accentColor()), Color.green(accentColor()), Color.blue(accentColor()))),
        rounded(color, 6),
        rounded(Color.WHITE, 6)
    )
    private fun circle(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
    private fun currentTheme() = prefs.getString("keyboard_theme", "warm") ?: "warm"
    private fun backgroundColor() = when (currentTheme()) { "light" -> Color.rgb(236, 239, 241); "amoled" -> Color.BLACK; else -> Color.rgb(26, 25, 24) }
    private fun keyColor() = when (currentTheme()) { "light" -> Color.WHITE; "amoled" -> Color.rgb(20, 20, 20); else -> Color.rgb(44, 42, 41) }
    private fun specialColor() = when (currentTheme()) { "light" -> Color.rgb(220, 224, 228); "amoled" -> Color.rgb(30, 30, 30); else -> Color.rgb(36, 33, 30) }
    private fun actionColor(): Int {
        if (prefs.getInt("accent_index", 0) > 0) return accentColor()
        return when (currentTheme()) { "light" -> Color.rgb(235, 211, 177); else -> Color.rgb(61, 50, 40) }
    }
    private fun textColor() = if (currentTheme() == "light") Color.rgb(31, 35, 38) else Color.rgb(234, 230, 225)
    private fun accentColor(): Int {
        val colors = listOf("#D8A76A", "#D88772", "#8AAE9B", "#8CA7D0")
        return try { Color.parseColor(colors[prefs.getInt("accent_index", 0).coerceIn(0, colors.lastIndex)]) } catch (_: Exception) { Color.rgb(216, 167, 106) }
    }
    private fun showToast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    companion object { private const val IMAGE_CLIP_PREFIX = "__VOICEFLOW_IMAGE__:" }
}
