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
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenPrefs.applyOrientation(this)
        setContentView(R.layout.activity_settings)
        ImmersiveMode.apply(this)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        mostrarBateria()
        configurarBrilho()
        configurarRotacao()
        configurarTemporizador()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnWifi).setOnClickListener { abrirConfiguracoesWifi() }
        findViewById<View>(R.id.btnAirplane).setOnClickListener { abrirModoAviao() }
        findViewById<View>(R.id.btnExitApp).setOnClickListener { pedirSenhaParaSair() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            ImmersiveMode.apply(this)
        }
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
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val nivel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: run {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val textoBateria = if (isCharging) {
            "$nivel% ⚡"
        } else {
            "$nivel%"
        }
        findViewById<TextView>(R.id.txtBattery).text = textoBateria
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

    /**
     * Sair do app agora exige a senha de administrador, do mesmo jeito que
     * antes era exigida na engrenagem da tela principal.
     */
    private fun pedirSenhaParaSair() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(50, 30, 50, 30)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val input = EditText(this).apply {
            hint = getString(R.string.admin_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }

        val btnToggle = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            background = null
            setPadding(10, 10, 10, 10)
            contentDescription = "Mostrar/Ocultar senha"
            alpha = 0.6f
        }

        var isVisible = false
        btnToggle.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                input.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                btnToggle.alpha = 1.0f
            } else {
                input.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                btnToggle.alpha = 0.6f
            }
            input.setSelection(input.text.length)
        }

        container.addView(input)
        container.addView(btnToggle)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.admin_password_title))
            .setMessage(getString(R.string.admin_password_message))
            .setView(container)
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

    private fun configurarTemporizador() {
        val switchSchedule = findViewById<Switch>(R.id.switchSchedule)

        val isEnabled = SchedulePrefs.isEnabled(this)
        switchSchedule.isChecked = isEnabled

        atualizarTextosTemporizador()

        switchSchedule.setOnCheckedChangeListener { _, isChecked ->
            SchedulePrefs.setEnabled(this, isChecked)
            ScheduleManager.updateAlarms(this)
            Toast.makeText(this, if (isChecked) "Temporizador ativado" else "Temporizador desativado", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.layoutScheduleDays).setOnClickListener {
            mostrarSeletorDias()
        }

        findViewById<View>(R.id.layoutPowerOn).setOnClickListener {
            mostrarSeletorHorario(true)
        }

        findViewById<View>(R.id.layoutPowerOff).setOnClickListener {
            mostrarSeletorHorario(false)
        }
    }

    private fun atualizarTextosTemporizador() {
        atualizarChipsDias()

        val (onH, onM) = SchedulePrefs.getPowerOnTime(this)
        findViewById<TextView>(R.id.txtPowerOn)?.text = String.format(java.util.Locale.getDefault(), "%02d:%02d", onH, onM)

        val (offH, offM) = SchedulePrefs.getPowerOffTime(this)
        findViewById<TextView>(R.id.txtPowerOff)?.text = String.format(java.util.Locale.getDefault(), "%02d:%02d", offH, offM)
    }

    private fun atualizarChipsDias() {
        val days = SchedulePrefs.getDays(this)
        val chips = mapOf(
            Calendar.MONDAY to findViewById<TextView>(R.id.chipSeg),
            Calendar.TUESDAY to findViewById<TextView>(R.id.chipTer),
            Calendar.WEDNESDAY to findViewById<TextView>(R.id.chipQua),
            Calendar.THURSDAY to findViewById<TextView>(R.id.chipQui),
            Calendar.FRIDAY to findViewById<TextView>(R.id.chipSex),
            Calendar.SATURDAY to findViewById<TextView>(R.id.chipSab),
            Calendar.SUNDAY to findViewById<TextView>(R.id.chipDom)
        )

        chips.forEach { (calDay, view) ->
            if (view != null) {
                if (days.contains(calDay)) {
                    view.setBackgroundResource(R.drawable.chip_active)
                    view.setTextColor(android.graphics.Color.WHITE)
                } else {
                    view.setBackgroundResource(R.drawable.chip_inactive)
                    view.setTextColor(android.graphics.Color.parseColor("#5B6B7C"))
                }
            }
        }
    }

    private fun mostrarSeletorDias() {
        val items = arrayOf("Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo")
        val calendarDays = arrayOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        
        val currentDays = SchedulePrefs.getDays(this)
        val checkedItems = BooleanArray(items.size) { i -> currentDays.contains(calendarDays[i]) }

        AlertDialog.Builder(this)
            .setTitle("Selecionar dias da semana")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Salvar") { _, _ ->
                val newDays = mutableSetOf<Int>()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        newDays.add(calendarDays[i])
                    }
                }
                SchedulePrefs.setDays(this, newDays)
                atualizarTextosTemporizador()
                ScheduleManager.updateAlarms(this)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarSeletorHorario(isPowerOn: Boolean) {
        val (h, m) = if (isPowerOn) SchedulePrefs.getPowerOnTime(this) else SchedulePrefs.getPowerOffTime(this)

        android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
            if (isPowerOn) {
                SchedulePrefs.setPowerOnTime(this, hourOfDay, minute)
            } else {
                SchedulePrefs.setPowerOffTime(this, hourOfDay, minute)
            }
            atualizarTextosTemporizador()
            ScheduleManager.updateAlarms(this)
        }, h, m, true).show()
    }

    companion object {
        private const val TAG = "AmalfisKiosk"
    }
}
