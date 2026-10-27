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

    // Colores de la interfaz. Centralizarlos facilita cambiar el estilo después.
    private val colorPrimary = Color.rgb(37, 99, 235)       // Azul principal
    private val colorText = Color.rgb(31, 41, 55)           // Texto oscuro
    private val colorSecondaryText = Color.rgb(107, 114, 128)
    private val colorBackground = Color.rgb(246, 248, 252)  // Fondo gris muy claro
    private val colorBorder = Color.rgb(229, 234, 242)
    private val colorWhite = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargamos los sitios guardados antes de construir la pantalla.
        loadDomains()

        val dp = { value: Int -> (value * resources.displayMetrics.density).toInt() }

        // Contenedor principal de la pantalla.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBackground)
        }

        // Encabezado azul con el nombre de la aplicación y una breve descripción.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(26))
            background = roundedBackground(colorPrimary, 0, 0, 24, 24)
        }

        header.addView(TextView(this).apply {
            text = "ViewSite"
            textSize = 29f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorWhite)
        })

        header.addView(TextView(this).apply {
            text = "Tus sitios web, en un solo lugar"
            textSize = 14f
            setTextColor(Color.rgb(225, 235, 255))
            setPadding(0, dp(5), 0, 0)
        })

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // El contenido desplazable permite que la pantalla funcione en móviles pequeños
        // o cuando la lista contiene muchos sitios.
        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            setFillViewport(false)
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Título de la sección de agregar sitios.
        content.addView(TextView(this).apply {
            text = "Agregar un sitio"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorText)
        })

        content.addView(TextView(this).apply {
            text = "Escribe la dirección y, si quieres, un alias para reconocerla."
            textSize = 13f
            setTextColor(colorSecondaryText)
            setPadding(0, dp(4), 0, dp(12))
        })

        // Tarjeta blanca que contiene los campos de entrada y el botón.
        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground(colorWhite, dp(1), colorBorder, 18, 18)
        }

        domainInput = EditText(this).apply {
            hint = "ejemplo.com  o  192.168.1.10:8086"
            setSingleLine(true)
            textSize = 15f
            setTextColor(colorText)
            setHintTextColor(Color.rgb(156, 163, 175))
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = roundedBackground(colorBackground, 0, 0, 12, 12)
        }

        inputCard.addView(
            domainInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        aliasInput = EditText(this).apply {
            hint = "Alias opcional (ej: Casa, Trabajo)"
            setSingleLine(true)
            textSize = 15f
            setTextColor(colorText)
            setHintTextColor(Color.rgb(156, 163, 175))
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = roundedBackground(colorBackground, 0, 0, 12, 12)
        }

        inputCard.addView(
            aliasInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        val addButton = Button(this).apply {
            text = "＋  Agregar sitio"
            textSize = 15f
            isAllCaps = false
            setTextColor(colorWhite)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(colorPrimary, 0, 0, 12, 12)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }

        val addButtonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(50)
        ).apply {
            topMargin = dp(12)
        }
        inputCard.addView(addButton, addButtonParams)

        val inputCardParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(24)
        }
        content.addView(inputCard, inputCardParams)

        // Título y contador de la lista de sitios.
        val sitesHeading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        sitesHeading.addView(TextView(this).apply {
            text = "Mis sitios"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorText)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        countLabel = TextView(this).apply {
            textSize = 12f
            setTextColor(colorPrimary)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = roundedBackground(Color.rgb(219, 234, 254), 0, 0, 20, 20)
        }
        sitesHeading.addView(countLabel)
        content.addView(sitesHeading)

        // Mensaje mostrado si la lista está vacía.
        emptyHint = TextView(this).apply {
            text = "Todavía no tienes sitios guardados.\nAgrega tu primer sitio arriba."
            textSize = 14f
            setTextColor(colorSecondaryText)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(28), dp(18), dp(28))
            background = roundedBackground(colorWhite, dp(1), colorBorder, 18, 18)
        }

        val emptyParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12)
        }
        content.addView(emptyHint, emptyParams)

        // Aquí se crean las tarjetas individuales de cada sitio.
        domainsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(domainsContainer)

        // Agrega el contenido al área desplazable usando parámetros compatibles.
        scrollView.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // El encabezado permanece arriba; el contenido puede desplazarse debajo.
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // Acción de agregar: valida la dirección, evita duplicados y guarda todo.
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
        val dp = { value: Int -> (value * resources.displayMetrics.density).toInt() }

        // Actualiza el contador de sitios guardados.
        countLabel.text = "${sites.size} ${if (sites.size == 1) "sitio" else "sitios"}"

        emptyHint.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE
        domainsContainer.removeAllViews()

        sites.forEachIndexed { index, site ->
            // Cada sitio se muestra en una tarjeta blanca con un icono.
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(13), dp(12), dp(13))
                background = roundedBackground(colorWhite, dp(1), colorBorder, 16, 16)
                isClickable = true
                isFocusable = true
            }

            // Círculo azul que identifica visualmente cada sitio.
            val siteIcon = TextView(this).apply {
                text = "↗"
                textSize = 23f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(colorPrimary)
                background = roundedBackground(Color.rgb(239, 246, 255), 0, 0, 14, 14)
            }
            card.addView(
                siteIcon,
                LinearLayout.LayoutParams(dp(48), dp(48))
            )

            // Alias y dirección del sitio.
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(6), 0)
            }

            // Título: el alias si existe; si no, el dominio.
            val title = TextView(this).apply {
                text = if (site.alias.isNotBlank()) site.alias else displayDomain(site.url)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(colorText)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            details.addView(title)

            // Subtítulo: la dirección completa.
            val subtitle = TextView(this).apply {
                text = site.url
                textSize = 12f
                setTextColor(colorSecondaryText)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(4), 0, 0)
            }
            details.addView(subtitle)

            card.addView(
                details,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )

            // Indicador visual de que la tarjeta se puede abrir.
            card.addView(TextView(this).apply {
                text = "›"
                textSize = 28f
                setTextColor(Color.rgb(156, 163, 175))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT))

            // Tocar la tarjeta abre el sitio en WebActivity.
            card.setOnClickListener {
                startActivity(Intent(this, WebActivity::class.java).putExtra("url", sites[index].url))
            }

            // Mantener pulsada la tarjeta elimina el sitio.
            card.setOnLongClickListener {
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

            val cardParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            domainsContainer.addView(card, cardParams)
        }
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
class WebActivity : Activity() {

    private lateinit var webView: WebView
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

        // Crea el navegador integrado y conserva las opciones originales.
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val host = request.url.host ?: return false
                val base = baseHost
                val sameSite = base != null && (host == base || host.endsWith("." + base))
                return if (sameSite) {
                    // Los enlaces del mismo sitio continúan dentro de ViewSite.
                    false
                } else {
                    // Los enlaces externos se abren con la aplicación correspondiente.
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                }
            }
        }

        setContentView(webView)
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
