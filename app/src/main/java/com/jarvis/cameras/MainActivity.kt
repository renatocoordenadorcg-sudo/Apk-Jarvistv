package com.jarvis.cameras

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast

/**
 * Abre o painel JARVIS (servidor na sua rede) em tela cheia. Funciona em celular, tablet e Android TV.
 * Controle remoto: setas navegam · OK seleciona · Voltar fecha/sai · Menu abre as opções ·
 * Play/Pause pausa a ronda · CH+/CH- (ou avançar/voltar) trocam as câmeras da ronda.
 */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var custom: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var dialogOpen = false
    private val prefs by lazy { getSharedPreferences("jarvis", Context.MODE_PRIVATE) }
    private val bg = Color.parseColor("#02060D")
    private val starts = arrayOf("", "#wall", "#ronda")
    private val startNames = arrayOf("Painel", "Tela cheia (todas as câmeras)", "Ronda automática")

    private val isTv: Boolean by lazy {
        val ui = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        web = WebView(this).apply {
            setBackgroundColor(bg)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                setSupportZoom(false)
                builtInZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                setSupportMultipleWindows(false)
                // o painel detecta "JARVIS-TV" e liga o modo leve + interface para controle remoto
                if (isTv) userAgentString = "$userAgentString JARVIS-TV"
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                    if (r.isForMainFrame) connectionError()
                }
                // se o sistema matar o processo de renderização (pouca memória), recria a tela em vez de fechar o app
                override fun onRenderProcessGone(v: WebView, d: RenderProcessGoneDetail): Boolean {
                    runOnUiThread { recreate() }
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                    if (custom != null) { cb.onCustomViewHidden(); return }
                    custom = view; customCb = cb
                    root.addView(view, ViewGroup.LayoutParams(-1, -1))
                    web.visibility = View.GONE
                }
                override fun onHideCustomView() = hideCustom()
            }
        }
        root.addView(web, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)
        web.requestFocus()
        val u = target()
        if (u == null) askUrl(first = true) else web.loadUrl(u)
    }

    /** Endereço salvo + destino inicial escolhido (#wall / #ronda). */
    private fun target(): String? {
        val u = prefs.getString("url", null)
        if (u.isNullOrBlank()) return null
        return u + starts[prefs.getInt("start", 0).coerceIn(0, starts.size - 1)]
    }

    private fun hideCustom() {
        custom?.let { root.removeView(it) }
        custom = null
        customCb?.onCustomViewHidden(); customCb = null
        web.visibility = View.VISIBLE
        web.requestFocus()
    }

    private fun askUrl(first: Boolean = false) {
        if (dialogOpen) return
        dialogOpen = true
        val input = EditText(this).apply {
            hint = "192.168.3.46:7070"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
            setText(prefs.getString("url", "") ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("Endereço do servidor JARVIS")
            .setMessage("IP e porta do painel. Ex.: 192.168.3.46:7070\nFora de casa, use o IP do Tailscale.")
            .setView(input)
            .setCancelable(!first)
            .setPositiveButton("Conectar") { _, _ ->
                dialogOpen = false
                var u = input.text.toString().trim()
                if (u.isEmpty()) { askUrl(first); return@setPositiveButton }
                if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
                prefs.edit().putString("url", u).apply()
                target()?.let { web.loadUrl(it) }
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    private fun connectionError() {
        // TV ligada o dia todo: tenta reconectar sozinha (servidor reiniciando, rede caindo) sem pedir nada
        if (isTv) {
            web.postDelayed({ web.reload() }, 8000)
            return
        }
        if (dialogOpen) return
        dialogOpen = true
        AlertDialog.Builder(this)
            .setTitle("Sem conexão com o JARVIS")
            .setMessage("Verifique se o servidor está ligado e se você está na mesma rede (ou no Tailscale).")
            .setPositiveButton("Tentar de novo") { _, _ -> dialogOpen = false; web.reload() }
            .setNegativeButton("Alterar servidor") { _, _ -> dialogOpen = false; askUrl() }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    private fun showMenu() {
        if (dialogOpen) return
        dialogOpen = true
        val start = startNames[prefs.getInt("start", 0).coerceIn(0, startNames.size - 1)]
        AlertDialog.Builder(this)
            .setTitle("JARVIS")
            .setItems(arrayOf("Recarregar", "Alterar servidor", "Abrir ao iniciar: $start", "Sair")) { _, i ->
                dialogOpen = false
                when (i) {
                    0 -> web.reload()
                    1 -> askUrl()
                    2 -> pickStart()
                    3 -> finish()
                }
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    private fun pickStart() {
        if (dialogOpen) return
        dialogOpen = true
        AlertDialog.Builder(this)
            .setTitle("Abrir ao iniciar")
            .setSingleChoiceItems(startNames, prefs.getInt("start", 0)) { d, i ->
                prefs.edit().putInt("start", i).apply()
                dialogOpen = false
                d.dismiss()
                Toast.makeText(this, "Vale a partir da próxima abertura do app", Toast.LENGTH_SHORT).show()
            }
            .setOnCancelListener { dialogOpen = false }
            .show()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (custom != null) { hideCustom(); return }
        if (dialogOpen) return
        // o painel trata o Voltar primeiro (fecha janela, câmera ampliada ou tela cheia); se não tratar, abre o menu
        web.evaluateJavascript("(function(){try{return window.__jarvisBack?window.__jarvisBack():false}catch(e){return false}})()") { r ->
            if (r != "true") showMenu()
        }
    }

    private fun key(k: String) {
        web.evaluateJavascript("window.__jarvisKey&&window.__jarvisKey('$k')", null)
    }

    // teclas do controle remoto que o WebView não usa
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> { showMenu(); return true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> { key("pause"); return true }
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { key("next"); return true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND -> { key("prev"); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) { immersive(); web.requestFocus() }
    }
    override fun onPause() { web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onDestroy() { root.removeAllViews(); web.destroy(); super.onDestroy() }
}
