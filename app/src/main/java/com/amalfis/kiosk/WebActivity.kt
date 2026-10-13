package com.amalfis.kiosk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {

    private var siteUrl: String = DEFAULT_URL
    private var allowedHost: String = DEFAULT_HOST

    private lateinit var webView: WebView

    // Necessário para o upload de arquivos/fotos funcionar dentro do WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenPrefs.applyOrientation(this)
        setContentView(R.layout.activity_web)
        ImmersiveMode.apply(this)

        siteUrl = intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL
        allowedHost = intent.getStringExtra(EXTRA_ALLOWED_HOST) ?: DEFAULT_HOST

        // Registra o "ouvinte" que recebe o resultado do seletor de arquivos/câmera
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val results: Array<Uri>? =
                if (result.resultCode == RESULT_OK && data != null) {
                    WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
                } else {
                    null
                }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            // Chamado quando o site tenta abrir o seletor de arquivo/foto (o clip)
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Cancela qualquer callback pendente anterior
                this@WebActivity.filePathCallback?.onReceiveValue(null)
                this@WebActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent()
                intent?.addCategory(Intent.CATEGORY_OPENABLE)

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    this@WebActivity.filePathCallback = null
                    false
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = Uri.parse(url)
                return uri.host != allowedHost
            }
        }

        webView.loadUrl(siteUrl)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            ImmersiveMode.apply(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_ALLOWED_HOST = "extra_allowed_host"

        private const val DEFAULT_URL = "https://taylo.amalfis.com.br/amalfis/login"
        private const val DEFAULT_HOST = "taylo.amalfis.com.br"

        /**
         * Monta a Intent para abrir o WebActivity com uma URL específica.
         * Use isso ao adicionar novos cards que abrem outros sites.
         */
        fun intentFor(context: android.content.Context, url: String, allowedHost: String): Intent {
            return Intent(context, WebActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_ALLOWED_HOST, allowedHost)
            }
        }
    }
}