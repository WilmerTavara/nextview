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
 * Pantalla principal de ViewSite.
 *
 * Esta versión cambia la presentación visual, pero conserva las funciones existentes:
 * - Cargar y guardar los dominios en SharedPreferences.
 * - Evitar que se agregue dos veces el mismo dominio.
 * - Tocar un dominio para abrirlo en WebActivity.
 * - Mantener pulsado un dominio para eliminarlo.
 *
 * Se usan vistas nativas de Android para no añadir dependencias externas.
 */
class MainActivity : Activity() {

    // Lista en memoria de los dominios guardados en el teléfono.
    private val domains = ArrayList<String>()

    // Contenedor donde se dibujan las tarjetas de los dominios.
    private lateinit var domainsContainer: LinearLayout

    // Mensaje que se muestra cuando todavía no hay dominios guardados.
    private lateinit var emptyHint: TextView

    // Etiqueta que muestra cuántos sitios están guardados.
    private lateinit var countLabel: TextView

    // Campo donde el usuario escribe un dominio nuevo.
    private lateinit var domainInput: EditText

    // Colores de la interfaz. Centralizarlos facilita cambiar el estilo después.
    private val colorPrimary = Color.rgb(37, 99, 235)       // Azul principal
    private val colorText = Color.rgb(31, 41, 55)           // Texto oscuro
    private val colorSecondaryText = Color.rgb(107, 114, 128)
    private val colorBackground = Color.rgb(246, 248, 252)  // Fondo gris muy claro
    private val colorBorder = Color.rgb(229, 234, 242)
    private val colorWhite = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargamos los dominios guardados antes de construir la pantalla.
        loadDomains()

        // Convierte medidas en dp a píxeles, para que el diseño mantenga proporciones
        // similares en teléfonos con distintas densidades de pantalla.
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
        // o cuando la lista contiene muchos dominios.
        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            setFillViewport(false)
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Título de la sección de agregar dominios.
        content.addView(TextView(this).apply {
            text = "Agregar un sitio"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorText)
        })

        content.addView(TextView(this).apply {
            text = "Escribe el dominio que deseas guardar."
            textSize = 13f
            setTextColor(colorSecondaryText)
            setPadding(0, dp(4), 0, dp(12))
        })

        // Tarjeta blanca que contiene el campo de entrada y el botón.
        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground(colorWhite, dp(1), colorBorder, 18, 18)
        }

        domainInput = EditText(this).apply {
            hint = "ejemplo.com"
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

        val addButton = Button(this).apply {
            text = "＋  Agregar dominio"
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
            text = "Todavía no tienes sitios guardados.\nAgrega tu primer dominio arriba."
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

        // Aquí se crearán las tarjetas individuales de cada dominio.
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

        // Acción de agregar: se conserva la validación y el guardado existentes.
        addButton.setOnClickListener {
            val url = normalize(domainInput.text.toString().trim())

            when {
                url == null -> {
                    Toast.makeText(this, "Dominio no válido", Toast.LENGTH_SHORT).show()
                }

                domains.contains(url) -> {
                    Toast.makeText(
                        this,
                        "Ese dominio ya está en la lista",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    domains.add(url)
                    saveDomains()
                    domainInput.setText("")
                    updateDomainsView()
                    Toast.makeText(this, "Dominio agregado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Dibuja las tarjetas iniciales con los dominios que se cargaron al abrir la app.
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
        countLabel.text = "${domains.size} ${if (domains.size == 1) "sitio" else "sitios"}"

        emptyHint.visibility = if (domains.isEmpty()) View.VISIBLE else View.GONE
        domainsContainer.removeAllViews()

        domains.forEachIndexed { index, domain ->
            // Cada dominio se muestra en una tarjeta blanca con un icono de globo.
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

            // Nombre y dirección del sitio.
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(6), 0)
            }

            val title = TextView(this).apply {
                text = displayDomain(domain)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(colorText)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            details.addView(title)

            val subtitle = TextView(this).apply {
                text = domain
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

            // Tocar la tarjeta conserva el comportamiento original: abrir WebActivity.
            card.setOnClickListener {
                startActivity(Intent(this, WebActivity::class.java).putExtra("url", domains[index]))
            }

            // Mantener pulsada la tarjeta conserva la función original de eliminación.
            card.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar dominio?")
                    .setMessage(domains[index])
                    .setPositiveButton("Eliminar") { _, _ ->
                        domains.removeAt(index)
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
     * Devuelve una versión corta del dominio para el título de la tarjeta.
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
     * Normaliza el dominio antes de guardarlo.
     * Se conserva la lógica original para no cambiar el comportamiento actual.
     */
    private fun normalize(raw: String): String? {
        if (raw.isEmpty()) return null
        var value = raw
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://" + value
        val uri = Uri.parse(value)
        val host = uri.host ?: return null
        if (!host.contains(".")) return null
        return uri.scheme.toString() + "://" + host
    }

    /**
     * Recupera los dominios guardados localmente en el teléfono.
     * Se conserva el mismo nombre de preferencias y la misma clave que antes.
     */
    private fun loadDomains() {
        domains.clear()
        val saved = getSharedPreferences("viewsite", MODE_PRIVATE).getString("domains", "")
        if (!saved.isNullOrEmpty()) {
            domains.addAll(saved.split("\n"))
        }
    }

    /**
     * Guarda la lista en el almacenamiento privado de la aplicación.
     * Se mantiene el formato original para conservar los datos existentes.
     */
    private fun saveDomains() {
        getSharedPreferences("viewsite", MODE_PRIVATE)
            .edit().putString("domains", domains.joinToString("\n")).apply()
    }
}

/**
 * Pantalla que muestra el sitio web elegido por el usuario.
 *
 * Esta clase se mantiene funcionalmente igual que en la versión original.
 * No se cambia aquí la configuración HTTP ni el comportamiento de navegación.
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
