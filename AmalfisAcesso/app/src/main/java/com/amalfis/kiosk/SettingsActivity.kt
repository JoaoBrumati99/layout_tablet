package com.amalfis.kiosk

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenPrefs.applyOrientation(this)
        ModoImersivo.aplicar(this)
        setContentView(R.layout.activity_settings)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        mostrarBateria()
        configurarBrilho()
        configurarRotacao()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnWifi).setOnClickListener { abrirConfiguracoesWifi() }
        findViewById<View>(R.id.btnAirplane).setOnClickListener { abrirModoAviao() }
        findViewById<View>(R.id.btnRestart).setOnClickListener { confirmarReiniciar() }
        findViewById<View>(R.id.btnShutdown).setOnClickListener { confirmarDesligar() }
        findViewById<View>(R.id.btnExitApp).setOnClickListener { pedirSenhaParaSair() }
    }

    override fun onResume() {
        super.onResume()
        ModoImersivo.aplicar(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        ModoImersivo.aoGanharFoco(this, hasFocus)
    }

    /**
     * Abre a tela de Wi-Fi do Android por cima do kiosk. Funciona sem sair
     * do modo travado porque "com.android.settings" está liberado na lista
     * de pacotes do lock task (veja enterKioskMode() em MainActivity).
     */
    private fun abrirConfiguracoesWifi() {
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir as configurações de Wi-Fi", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Abre a tela de Modo avião do Android por cima do kiosk, pelo mesmo
     * motivo do Wi-Fi acima.
     */
    private fun abrirModoAviao() {
        try {
            startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir o Modo avião", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarBateria() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val nivel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        findViewById<TextView>(R.id.txtBattery).text = getString(R.string.battery_value, nivel)
    }

    private fun configurarBrilho() {
        val seekBar = findViewById<SeekBar>(R.id.seekBrightness)
        val txtBrightness = findViewById<TextView>(R.id.txtBrightness)

        val brilhoAtual = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            125
        }
        seekBar.progress = brilhoAtual
        txtBrightness.text = getString(R.string.battery_value, percentualBrilho(brilhoAtual))

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtBrightness.text = getString(R.string.battery_value, percentualBrilho(progress))
                if (!fromUser) return
                if (!Settings.System.canWrite(this@SettingsActivity)) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Autorize o ajuste de brilho na próxima tela",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    return
                }
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun percentualBrilho(progress: Int): Int {
        return (progress * 100 / 255).coerceIn(0, 100)
    }

    /**
     * Liga/desliga a rotação livre de tela. Quando desativada (padrão), o app
     * fica travado em paisagem, como sempre foi.
     */
    private fun configurarRotacao() {
        val switchRotation = findViewById<Switch>(R.id.switchRotation)
        val txtCaption = findViewById<TextView>(R.id.txtRotationCaption)

        val ativado = ScreenPrefs.isRotationEnabled(this)
        switchRotation.isChecked = ativado
        txtCaption.text = getString(
            if (ativado) R.string.rotation_caption_on else R.string.rotation_caption_off
        )

        switchRotation.setOnCheckedChangeListener { _, isChecked ->
            ScreenPrefs.setRotationEnabled(this, isChecked)
            ScreenPrefs.applyOrientation(this)
            txtCaption.text = getString(
                if (isChecked) R.string.rotation_caption_on else R.string.rotation_caption_off
            )
        }
    }

    private fun confirmarReiniciar() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restart_device))
            .setMessage("Tem certeza que deseja reiniciar o dispositivo?")
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> reiniciarDispositivo() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun confirmarDesligar() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.shutdown_device))
            .setMessage("Tem certeza que deseja desligar o dispositivo?")
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> desligarDispositivo() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun reiniciarDispositivo() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            try {
                devicePolicyManager.reboot(adminComponent)
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível reiniciar o dispositivo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Recurso disponível apenas em dispositivos gerenciados", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Tenta desligar o tablet completamente. IMPORTANTE: diferente do
     * reiniciar, o Android não oferece uma API oficial de "desligar" para
     * apps Device Owner — só existe DevicePolicyManager.reboot(). Um
     * desligamento completo por software normalmente exige a permissão de
     * sistema android.permission.SHUTDOWN, que só apps assinados com a
     * chave da própria ROM possuem (não é algo que o Device Owner consegue
     * se auto-conceder). Por isso tentamos dois caminhos por baixo dos
     * panos, mas em muitos aparelhos (inclusive Xiaomi/MIUI) nenhum dos
     * dois funciona, e a única forma confiável de desligar continua sendo
     * segurar o botão físico Power.
     */
    private fun desligarDispositivo() {
        if (tentarShutdownViaIntent() || tentarShutdownViaReflexao()) {
            return
        }
        Toast.makeText(
            this,
            "Este tablet não permite desligar por software. Segure o botão físico Power para desligar.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun tentarShutdownViaIntent(): Boolean {
        return try {
            val intent = Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN")
            intent.putExtra("android.intent.extra.KEY_CONFIRM", false)
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tentarShutdownViaReflexao(): Boolean {
        return try {
            val powerManager = getSystemService(POWER_SERVICE)
            val metodo = powerManager.javaClass.getMethod(
                "shutdown",
                Boolean::class.javaPrimitiveType,
                String::class.java,
                Boolean::class.javaPrimitiveType
            )
            metodo.invoke(powerManager, false, null, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sair do app agora exige a senha de administrador, do mesmo jeito que
     * antes era exigida na engrenagem da tela principal.
     */
    private fun pedirSenhaParaSair() {
        val input = EditText(this)
        input.hint = getString(R.string.admin_password_hint)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.admin_password_title))
            .setMessage(getString(R.string.admin_password_message))
            .setView(input)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                if (input.text.toString() == AppConfig.ADMIN_PASSWORD) {
                    sairDoApp()
                } else {
                    Toast.makeText(this, getString(R.string.wrong_password), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun sairDoApp() {
        Log.d(TAG, "sairDoApp: iniciando saída")

        try {
            stopLockTask()
            Log.d(TAG, "sairDoApp: stopLockTask OK")
        } catch (e: Exception) {
            Log.e(TAG, "sairDoApp: falhou stopLockTask", e)
        }

        // Libera este app como tela inicial padrão, para o Android voltar a
        // mostrar o launcher normal do sistema em vez de reabrir o Amalfis.
        // Da próxima vez que o app for aberto, ele volta a se registrar como
        // Home (veja tornarAppTelaInicialPadrao() em MainActivity).
        try {
            devicePolicyManager.clearPackagePersistentPreferredActivities(
                adminComponent,
                packageName
            )
            Log.d(TAG, "sairDoApp: clearPackagePersistentPreferredActivities OK")
        } catch (e: Exception) {
            Log.e(TAG, "sairDoApp: falhou clearPackagePersistentPreferredActivities", e)
        }

        // Tentamos primeiro achar outro launcher instalado (visual melhor,
        // parece "sair de verdade" pro tablet normal). Se não existir nenhum
        // no aparelho, caímos para as Configurações do Android — que existem
        // garantidamente em 100% dos dispositivos Android, então sempre
        // temos um destino certo pra ir, mesmo que o Amalfis seja o único
        // app instalado no tablet.
        val abriuOutroLauncher = abrirOutroLauncher()
        Log.d(TAG, "sairDoApp: abrirOutroLauncher retornou $abriuOutroLauncher")

        val abriuAlgumDestino = if (abriuOutroLauncher) {
            true
        } else {
            val abriuConfig = abrirConfiguracoesAndroid()
            Log.d(TAG, "sairDoApp: abrirConfiguracoesAndroid retornou $abriuConfig")
            abriuConfig
        }

        if (!abriuAlgumDestino) {
            Log.e(TAG, "sairDoApp: NENHUM destino conseguiu abrir — permanecendo no Amalfis")
            Toast.makeText(
                this,
                "Não foi possível sair do aplicativo neste tablet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // IMPORTANTE: usamos moveTaskToBack em vez de finishAffinity().
        // Destruir a task com finishAffinity() força o Android a resolver
        // "quem é a tela Home agora" na hora, e nesse instante a limpeza da
        // preferência acima pode ainda não ter se propagado — daí ele
        // reabre o Amalfis por cima do que acabamos de iniciar. Mandando só
        // para o segundo plano, o app que já abrimos permanece em primeiro
        // plano sem disputa nenhuma. Um pequeno atraso dá tempo do Android
        // terminar de trazer a outra tela para frente antes da nossa sair.
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "sairDoApp: chamando moveTaskToBack")
            moveTaskToBack(true)
        }, 250)
    }

    /**
     * Procura por outro app com categoria HOME instalado no tablet (o
     * launcher original do fabricante, por exemplo) e abre ele diretamente.
     *
     * Além de abrir, registramos esse outro launcher explicitamente como o
     * novo Home padrão do sistema (em vez de só limpar a nossa preferência).
     * Isso é importante: alguns launchers de fábrica (MIUI Home incluso),
     * ao carregar, conferem quem está registrado como Home padrão — se a
     * resposta ainda for "ambígua" (preferência só limpa, não substituída),
     * eles podem devolver o controle pro app que estava lá antes. Registrando
     * explicitamente o novo padrão, isso deixa de acontecer.
     *
     * Retorna true se conseguiu abrir algum.
     */
    private fun abrirOutroLauncher(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val candidatos = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        Log.d(TAG, "abrirOutroLauncher: candidatos encontrados = ${candidatos.map { it.activityInfo.packageName }}")
        val outro = candidatos.firstOrNull { it.activityInfo.packageName != packageName } ?: return false

        try {
            val filter = IntentFilter(Intent.ACTION_MAIN)
            filter.addCategory(Intent.CATEGORY_HOME)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            devicePolicyManager.addPersistentPreferredActivity(
                adminComponent,
                filter,
                ComponentName(outro.activityInfo.packageName, outro.activityInfo.name)
            )
            Log.d(TAG, "abrirOutroLauncher: registrado ${outro.activityInfo.packageName} como novo Home padrão")
        } catch (e: Exception) {
            Log.e(TAG, "abrirOutroLauncher: falhou ao registrar novo Home padrão", e)
        }

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                component = ComponentName(outro.activityInfo.packageName, outro.activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            Log.d(TAG, "abrirOutroLauncher: startActivity chamado para ${outro.activityInfo.packageName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "abrirOutroLauncher: falhou ao iniciar ${outro.activityInfo.packageName}", e)
            false
        }
    }

    private fun abrirConfiguracoesAndroid(): Boolean {
        return try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Log.d(TAG, "abrirConfiguracoesAndroid: startActivity chamado")
            true
        } catch (e: Exception) {
            Log.e(TAG, "abrirConfiguracoesAndroid: falhou", e)
            false
        }
    }

    companion object {
        private const val TAG = "AmalfisKiosk"
    }
}
