package com.viewsite.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.widget.ProgressBar

/**
 * Un sitio guardado: su dirección y, si el usuario escribió uno, su alias.
 */
data class Site(val url: String, val alias: String)

/**
 * Pantalla principal de ViewSite.
 *
 * Esta versión agrega un alias opcional para cada sitio, para reconocer
 * más fácilmente cada tarjeta. El resto de funciones se conserva:
 * - Cargar y guardar los sitios en SharedPreferences.
 * - Evitar que se agregue dos veces la misma dirección.
 * - Tocar una tarjeta para abrir el sitio en WebActivity.
 * - Mantener pulsada una tarjeta para eliminarla.
 */
class MainActivity : Activity() {

    // Lista en memoria de los sitios guardados en el teléfono.
    private val sites = ArrayList<Site>()

    // Contenedor donde se dibujan las tarjetas de los sitios.
    private lateinit var domainsContainer: LinearLayout

    // Mensaje que se muestra cuando todavía no hay sitios guardados.
    private lateinit var emptyHint: TextView

    // Etiqueta que muestra cuántos sitios están guardados.
    private lateinit var countLabel: TextView

    // Campo donde el usuario escribe una dirección nueva.
    private lateinit var domainInput: EditText

    // Campo opcional donde el usuario escribe el alias del sitio.
    private lateinit var aliasInput: EditText

    // Paleta estilo iOS. Centralizarlos facilita cambiar el estilo después.
    private val colorIOSBlue = Color.rgb(0, 122, 255)              // Azul iOS
    private val colorBlueTint = Color.rgb(229, 240, 255)            // Fondo pill del contador
    private val colorBackground = Color.rgb(242, 242, 247)          // Gris de fondo iOS
    private val colorText = Color.rgb(0, 0, 0)                     // Texto principal negro
    private val colorSecondaryText = Color.rgb(142, 142, 147)       // Texto secundario gris iOS
    private val colorSeparator = Color.rgb(229, 229, 234)          // Separadores finos
    private val colorCard = Color.WHITE                            // Tarjetas
    private val colorChevron = Color.rgb(199, 199, 204)            // Chevron gris claro
    private val colorPlaceholder = Color.rgb(199, 199, 204)        // Placeholder inputs
    private val colorSectionLabel = Color.rgb(109, 109, 114)       // Labels de sección

    // Paletas para los iconos de los sitios (cíclicas).
    private val iconPalettes = listOf(
        Pair(Color.rgb(94, 158, 255), Color.rgb(0, 122, 255)),    // Azul
        Pair(Color.rgb(168, 85, 247), Color.rgb(124, 58, 237)),    // Violeta
        Pair(Color.rgb(52, 211, 153), Color.rgb(5, 150, 105))     // Verde
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargamos los sitios guardados antes de construir la pantalla.
        loadDomains()

        // Helper de densidad. Usamos Float para soportar valores como 0.5f.
        val dp = { value: Float -> (value * resources.displayMetrics.density).toInt() }

        // Contenedor principal de la pantalla.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBackground)
        }

        // El contenido desplazable permite que la pantalla funcione en móviles pequeños
        // o cuando la lista contiene muchos sitios.
        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            setFillViewport(true)
            setPadding(dp(16f), dp(8f), dp(16f), dp(30f))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Large title estilo iOS (34px, peso bold, espaciado negativo).
        content.addView(TextView(this).apply {
            text = "ViewSite"
            textSize = 34f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorText)
            letterSpacing = -0.02f
            setPadding(dp(4f), dp(8f), dp(4f), dp(2f))
        })

        // Subtítulo debajo del título.
        content.addView(TextView(this).apply {
            text = "Tus sitios web, en un solo lugar"
            textSize = 15f
            setTextColor(colorSecondaryText)
            setPadding(dp(4f), 0, dp(4f), dp(22f))
        })

        // Label de sección en mayúsculas pequeñas grises, estilo iOS.
        content.addView(TextView(this).apply {
            text = "AGREGAR SITIO"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorSectionLabel)
            letterSpacing = 0.04f
            setPadding(dp(4f), dp(18f), dp(4f), dp(8f))
        })

        // Tarjeta agrupada que contiene los campos de entrada.
        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(colorCard, 0, 0, 14, 14)
        }

        // Fila URL: label a la izquierda + input.
        val urlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        }
        urlRow.addView(TextView(this).apply {
            text = "URL"
            textSize = 16f
            setTextColor(colorText)
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(dp(64f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(12f)
        })

        domainInput = EditText(this).apply {
            hint = "ejemplo.com  ·  192.168.1.10:8086"
            setSingleLine(true)
            textSize = 16f
            setTextColor(colorText)
            setHintTextColor(colorPlaceholder)
            background = null  // Sin subrayado de Android, estilo iOS.
            setPadding(0, 0, 0, 0)
        }
        urlRow.addView(
            domainInput,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        // Separador de 0.5dp entre campos (alineado con el contenido del input).
        val sepUrl = View(this).apply { setBackgroundColor(colorSeparator) }

        inputCard.addView(
            urlRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        inputCard.addView(
            sepUrl,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(0.5f))
            ).apply {
                leftMargin = dp(92f)
                rightMargin = dp(16f)
            }
        )

        // Fila Alias: label a la izquierda + input.
        val aliasRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        }
        aliasRow.addView(TextView(this).apply {
            text = "Alias"
            textSize = 16f
            setTextColor(colorText)
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(dp(64f), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(12f)
        })

        aliasInput = EditText(this).apply {
            hint = "Opcional  ·  ej: Casa, Trabajo"
            setSingleLine(true)
            textSize = 16f
            setTextColor(colorText)
            setHintTextColor(colorPlaceholder)
            background = null
            setPadding(0, 0, 0, 0)
        }
        aliasRow.addView(
            aliasInput,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        inputCard.addView(
            aliasRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            inputCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Botón azul iOS lleno, esquinas 12dp.
        val addButton = Button(this).apply {
            text = "Agregar sitio"
            textSize = 17f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(colorIOSBlue, 0, 0, 12, 12)
            setPadding(0, dp(15f), 0, dp(15f))
            elevation = dp(4f).toFloat()
        }

        val addButtonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(50f)
        ).apply {
            topMargin = dp(16f)
        }
        content.addView(addButton, addButtonParams)

        // Encabezado de la lista: título + contador.
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listHeader.addView(TextView(this).apply {
            text = "MIS SITIOS"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorSectionLabel)
            letterSpacing = 0.04f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        countLabel = TextView(this).apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorIOSBlue)
            setPadding(dp(10f), dp(3f), dp(10f), dp(3f))
            background = roundedBackground(colorBlueTint, 0, 0, 10, 10)
        }
        listHeader.addView(countLabel)

        val listHeaderParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(22f)
            bottomMargin = dp(8f)
        }
        content.addView(listHeader, listHeaderParams)

        // Mensaje mostrado si la lista está vacía.
        emptyHint = TextView(this).apply {
            text = "Todavía no tienes sitios guardados.\nAgrega tu primer sitio arriba."
            textSize = 14f
            setTextColor(colorSecondaryText)
            gravity = Gravity.CENTER
            setPadding(dp(18f), dp(28f), dp(18f), dp(28f))
            background = roundedBackground(colorCard, 0, 0, 14, 14)
        }
        content.addView(
            emptyHint,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Aquí se crean las tarjetas individuales de cada sitio.
        domainsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(domainsContainer)

        // Hint sutil al final.
        content.addView(TextView(this).apply {
            text = "Mantén pulsada una tarjeta para eliminarla"
            textSize = 12f
            setTextColor(colorSecondaryText)
            gravity = Gravity.CENTER
            setPadding(dp(4f), dp(24f), dp(4f), dp(4f))
        })

        // Agrega el contenido al área desplazable usando parámetros compatibles.
        scrollView.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // El contenido puede desplazarse dentro de la pantalla.
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // Acción de agregar: valida la dirección, evita duplicados y guarda todo.
        // Lógica idéntica a la versión anterior.
        addButton.setOnClickListener {
            val url = normalize(domainInput.text.toString().trim())

            when {
                url == null -> {
                    Toast.makeText(this, "Dirección no válida", Toast.LENGTH_SHORT).show()
                }

                sites.any { it.url == url } -> {
                    Toast.makeText(
                        this,
                        "Esa dirección ya está en la lista",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    sites.add(Site(url, aliasInput.text.toString().trim()))
                    saveDomains()
                    domainInput.setText("")
                    aliasInput.setText("")
                    updateDomainsView()
                    Toast.makeText(this, "Sitio agregado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Dibuja las tarjetas iniciales con los sitios que se cargaron al abrir la app.
        updateDomainsView()

        setContentView(root)
    }

    /**
     * Actualiza el contador, el mensaje de lista vacía y todas las tarjetas.
     * Este método solo controla la presentación de la lista.
     */
    private fun updateDomainsView() {
        val dp = { value: Float -> (value * resources.displayMetrics.density).toInt() }

        // Actualiza el contador de sitios guardados.
        countLabel.text = "${sites.size} ${if (sites.size == 1) "sitio" else "sitios"}"

        emptyHint.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE
        domainsContainer.removeAllViews()

        // Tarjeta agrupada con esquinas 14dp estilo "inset grouped" de iOS.
        val groupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(colorCard, 0, 0, 14, 14)
        }

        sites.forEachIndexed { index, site ->
            // Separador de 0.5dp entre items, indentado para alinear con el texto.
            if (index > 0) {
                groupCard.addView(
                    View(this).apply { setBackgroundColor(colorSeparator) },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.max(1, dp(0.5f))
                    ).apply {
                        leftMargin = dp(74f)
                        rightMargin = dp(16f)
                    }
                )
            }

            // Cada sitio se muestra en una fila con icono cuadrado redondeado.
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16f), dp(11f), dp(16f), dp(11f))
                isClickable = true
                isFocusable = true
            }

            // Icono cuadrado (44dp, radius 11dp) con degradado y letra inicial.
            val (startColor, endColor) = iconPalettes[index % iconPalettes.size]
            val iconText = (if (site.alias.isNotBlank()) site.alias else displayDomain(site.url))
                .firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            val icon = TextView(this).apply {
                text = iconText
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(startColor, endColor)
                ).apply {
                    cornerRadii = floatArrayOf(
                        dp(11f).toFloat(), dp(11f).toFloat(),
                        dp(11f).toFloat(), dp(11f).toFloat(),
                        dp(11f).toFloat(), dp(11f).toFloat(),
                        dp(11f).toFloat(), dp(11f).toFloat()
                    )
                }
            }
            item.addView(icon, LinearLayout.LayoutParams(dp(44f), dp(44f)))

            // Alias y dirección del sitio.
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14f), 0, dp(8f), 0)
            }

            // Título: el alias si existe; si no, el dominio.
            val title = TextView(this).apply {
                text = if (site.alias.isNotBlank()) site.alias else displayDomain(site.url)
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(colorText)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            details.addView(title)

            // Subtítulo: la dirección completa.
            val subtitle = TextView(this).apply {
                text = site.url
                textSize = 13f
                setTextColor(colorSecondaryText)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2f), 0, 0)
            }
            details.addView(subtitle)

            item.addView(
                details,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )

            // Chevron discreto a la derecha.
            item.addView(TextView(this).apply {
                text = "›"
                textSize = 20f
                setTextColor(colorChevron)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(16f), ViewGroup.LayoutParams.WRAP_CONTENT))

            // Tocar la tarjeta abre el sitio en WebActivity.
            item.setOnClickListener {
                startActivity(Intent(this, WebActivity::class.java).putExtra("url", sites[index].url))
            }

            // Mantener pulsada la tarjeta elimina el sitio.
            item.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar sitio?")
                    .setMessage(
                        if (site.alias.isNotBlank()) site.alias + "\n" + site.url else site.url
                    )
                    .setPositiveButton("Eliminar") { _, _ ->
                        sites.removeAt(index)
                        saveDomains()
                        updateDomainsView()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }

            groupCard.addView(
                item,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        domainsContainer.addView(groupCard)
    }

    /**
     * Devuelve una versión corta de la dirección para el título de la tarjeta.
     * No modifica la URL almacenada ni la que se abre.
     */
    private fun displayDomain(url: String): String {
        return try {
            Uri.parse(url).host ?: url
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Crea fondos con esquinas redondeadas y, opcionalmente, un borde.
     * Se utiliza en tarjetas, campos y botones para mantener un estilo uniforme.
     */
    private fun roundedBackground(
        fillColor: Int,
        strokeWidthPx: Int,
        strokeColor: Int,
        topRadiusDp: Int,
        bottomRadiusDp: Int
    ): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadii = floatArrayOf(
                topRadiusDp * density, topRadiusDp * density,
                topRadiusDp * density, topRadiusDp * density,
                bottomRadiusDp * density, bottomRadiusDp * density,
                bottomRadiusDp * density, bottomRadiusDp * density
            )
            if (strokeWidthPx > 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    /**
     * Normaliza la dirección antes de guardarla.
     * Respeta el puerto y la ruta que el usuario escribió.
     */
    private fun normalize(raw: String): String? {
        if (raw.isEmpty()) return null
        var value = raw
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://" + value
        val uri = Uri.parse(value)
        val host = uri.host ?: return null
        if (!host.contains(".")) return null
        return value
    }

    /**
     * Recupera los sitios guardados localmente en el teléfono.
     * Formato nuevo: "url|alias". Los registros viejos (solo url, sin "|")
     * se cargan igual que siempre, quedando sin alias.
     */
    private fun loadDomains() {
        sites.clear()
        val saved = getSharedPreferences("viewsite", MODE_PRIVATE).getString("domains", "")
        if (!saved.isNullOrEmpty()) {
            for (line in saved.split("\n")) {
                if (line.isBlank()) continue
                if (line.contains("|")) {
                    val parts = line.split("|", limit = 2)
                    sites.add(Site(parts[0], parts[1]))
                } else {
                    sites.add(Site(line, ""))
                }
            }
        }
    }

    /**
     * Guarda la lista en el almacenamiento privado de la aplicación.
     * Los sitios con alias se guardan como "url|alias" y los demás solo como "url".
     */
    private fun saveDomains() {
        val stored = sites.joinToString("\n") { site ->
            if (site.alias.isBlank()) site.url else site.url + "|" + site.alias
        }
        getSharedPreferences("viewsite", MODE_PRIVATE)
            .edit().putString("domains", stored).apply()
    }
}

/**
 * Pantalla que muestra el sitio web elegido por el usuario.
 *
 * Esta clase se mantiene funcionalmente igual que en la versión original.
 */
/**
 * Pantalla que muestra el sitio web elegido por el usuario.
 *
 * Muestra un indicador de carga mientras la página se descarga:
 * los mensajes cambian según el progreso real y desaparecen al terminar.
 */
class WebActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var loadingText: TextView
    private var baseHost: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera la dirección que se envió al tocar una tarjeta.
        val url = intent.getStringExtra("url")
        if (url.isNullOrEmpty()) {
            finish()
            return
        }
        baseHost = Uri.parse(url).host

        val dp = { value: Float -> (value * resources.displayMetrics.density).toInt() }

        // Crea el navegador integrado y conserva las opciones originales.
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        
                // Mensaje de carga con estilo suave, tipo iOS.
        loadingText = TextView(this).apply {
            text = "Cargando…"
            textSize = 14f
            letterSpacing = 0.3f
            setTextColor(Color.rgb(142, 142, 147))
            gravity = Gravity.CENTER
        }

        // Círculo azul estilo iOS.
        val spinner = ProgressBar(this).apply {
            indeterminateTintList =
                android.content.res.ColorStateList.valueOf(Color.rgb(0, 122, 255))
        }

        // Tarjeta translúcida con esquinas redondeadas que contiene todo.
        val loadingCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32f), dp(28f), dp(32f), dp(28f))
            background = GradientDrawable().apply {
                setColor(Color.argb(230, 255, 255, 255))
                cornerRadius = dp(22f) * 1f
                setStroke(dp(1f), Color.rgb(229, 229, 234))
            }
        }

        loadingCard.addView(spinner, LinearLayout.LayoutParams(dp(36f), dp(36f)))
        loadingCard.addView(loadingText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(16f)
        })

        // Fondo con degradado suave de blanco a gris claro.
        loadingOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                colors = intArrayOf(Color.WHITE, Color.rgb(242, 242, 247))
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
            }
        }
        loadingOverlay.addView(loadingCard)

        // Contenedor: el WebView debajo y la pantalla de carga encima.
        val frame = FrameLayout(this)
        frame.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        frame.addView(
            loadingOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

                // Botón flotante "Inicio": círculo translúcido en la esquina superior
        // derecha que regresa a la lista de sitios sin afectar la vista de la página.
        val homeButton = TextView(this).apply {
            text = "🏠"
            textSize = 17f
            gravity = Gravity.CENTER
            isClickable = true
            background = GradientDrawable().apply {
                setColor(Color.argb(150, 255, 255, 255))
                cornerRadius = dp(22f) * 1f
                setStroke(dp(1f), Color.argb(60, 0, 0, 0))
            }
        }
        homeButton.setOnClickListener { finish() }

        frame.addView(
            homeButton,
            FrameLayout.LayoutParams(dp(44f), dp(44f)).apply {
                topMargin = dp(14f)
                marginEnd = dp(14f)
                gravity = Gravity.TOP or Gravity.END
            }
        )

        // Los mensajes avanzan con el progreso real de la descarga.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                when {
                    newProgress < 55 -> loadingText.text = "Cargando…"
                    newProgress < 89 -> loadingText.text = "Casi listo…"
                    newProgress < 100 -> loadingText.text = "Un momento…"
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val host = request.url.host ?: return false
                val base = baseHost
                val sameSite = base != null && (host == base || host.endsWith("." + base))
                return if (sameSite) {
                    // Navegación interna: volvemos a mostrar la pantalla de carga.
                    loadingOverlay.visibility = View.VISIBLE
                                        loadingOverlay.alpha = 1f
                    loadingText.text = "Cargando…"
                    false
                } else {
                    // Los enlaces externos se abren con la aplicación correspondiente.
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                }
            }

                        override fun onPageFinished(view: WebView?, urlFinish: String?) {
                // La página terminó de cargar: desvanecimiento suave.
                loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction { loadingOverlay.visibility = View.GONE }
            }
        }

        setContentView(frame)
        webView.loadUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Si el sitio tiene historial, retrocede dentro del WebView.
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
