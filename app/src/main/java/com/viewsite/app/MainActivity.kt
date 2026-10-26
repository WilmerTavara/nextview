package com.viewsite.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val domains = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var emptyHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadDomains()

        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(30), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        root.addView(TextView(this).apply {
            text = "ViewSite"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E88E5"))
        })

        root.addView(TextView(this).apply {
            text = "Agrega un dominio y tócalo para abrirlo en pantalla completa."
            textSize = 14f
            setTextColor(Color.GRAY)
        })

        val input = EditText(this).apply {
            hint = "ejemplo.com"
            setSingleLine()
        }
        root.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val addBtn = Button(this).apply { text = "＋ Agregar dominio" }
        root.addView(addBtn)

        emptyHint = TextView(this).apply {
            text = "Aún no hay dominios guardados."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(30), 0, 0)
        }
        root.addView(emptyHint)

        val list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, domains)
        list.adapter = adapter
        updateEmpty()

        addBtn.setOnClickListener {
            val url = normalize(input.text.toString().trim())
            when {
                url == null ->
                    Toast.makeText(this, "Dominio no válido", Toast.LENGTH_SHORT).show()
                domains.contains(url) ->
                    Toast.makeText(this, "Ese dominio ya está en la lista", Toast.LENGTH_SHORT).show()
                else -> {
                    domains.add(url)
                    saveDomains()
                    adapter.notifyDataSetChanged()
                    input.setText("")
                    updateEmpty()
                }
            }
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, WebActivity::class.java).putExtra("url", domains[pos]))
        }

        list.setOnItemLongClickListener { _, _, pos, _ ->
            AlertDialog.Builder(this)
                .setTitle("¿Eliminar dominio?")
                .setMessage(domains[pos])
                .setPositiveButton("Eliminar") { _, _ ->
                    domains.removeAt(pos)
                    saveDomains()
                    adapter.notifyDataSetChanged()
                    updateEmpty()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            true
        }

        setContentView(root)
    }

    private fun updateEmpty() {
        emptyHint.visibility = if (domains.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun normalize(raw: String): String? {
        if (raw.isEmpty()) return null
        var value = raw
        if (!value.startsWith("http")) value = "https://" + value
        val uri = Uri.parse(value)
        val host = uri.host ?: return null
        if (!host.contains(".")) return null
        return uri.scheme.toString() + "://" + host
    }

    private fun loadDomains() {
        domains.clear()
        val saved = getSharedPreferences("viewsite", MODE_PRIVATE).getString("domains", "")
        if (!saved.isNullOrEmpty()) {
            domains.addAll(saved.split("\n"))
        }
    }

    private fun saveDomains() {
        getSharedPreferences("viewsite", MODE_PRIVATE)
            .edit().putString("domains", domains.joinToString("\n")).apply()
    }
}

class WebActivity : Activity() {

    private lateinit var webView: WebView
    private var baseHost: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("url")
        if (url.isNullOrEmpty()) {
            finish()
            return
        }
        baseHost = Uri.parse(url).host

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host ?: return false
                val base = baseHost
                val sameSite = base != null && (host == base || host.endsWith("." + base))
                return if (sameSite) {
                    false
                } else {
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
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
