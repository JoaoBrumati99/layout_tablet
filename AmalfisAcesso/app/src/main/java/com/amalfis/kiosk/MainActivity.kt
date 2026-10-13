package com.amalfis.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        ScreenPrefs.applyOrientation(this)
        ModoImersivo.aplicar(this)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.cardCamera).setOnClickListener { openCamera() }
        findViewById<View>(R.id.cardGallery).setOnClickListener { openGallery() }
        findViewById<View>(R.id.cardSite).setOnClickListener { openSite() }
        findViewById<View>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        enterKioskMode()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        ScreenPrefs.applyOrientation(this)
        ModoImersivo.aplicar(this)
        enterKioskMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        ModoImersivo.aoGanharFoco(this, hasFocus)
    }

    /**
     * Ativa o modo kiosk (Lock Task). Se o app for Device Owner, isso bloqueia
     * completamente Home, Recentes e a barra de notificações.
     *
     * Incluímos o pacote real das Configurações do Android na lista de
     * pacotes permitidos, para que seja possível abrir Wi-Fi, Modo avião e
     * "Sair do aplicativo" sem depender de outro launcher existir no tablet.
     */
    private fun enterKioskMode() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Log.d(TAG, "enterKioskMode: aplicando lock task")
            val pacotesPermitidos = mutableSetOf(packageName)
            pacotesPermitidos.add(resolverPacoteConfiguracoes())
            // Também liberamos qualquer launcher instalado, caso exista, para
            // que "Sair do aplicativo" possa levar até ele.
            pacotesPermitidos.addAll(resolverPacotesLauncher())
            Log.d(TAG, "enterKioskMode: pacotes permitidos = $pacotesPermitidos")

            devicePolicyManager.setLockTaskPackages(
                adminComponent,
                pacotesPermitidos.toTypedArray()
            )

            // Opcional, mas recomendado: esconde barra de status/navegação enquanto travado
            devicePolicyManager.setLockTaskFeatures(
                adminComponent,
                DevicePolicyManager.LOCK_TASK_FEATURE_NONE
            )

            tornarAppTelaInicialPadrao()
            bloquearRotacaoAutomaticaGlobal()

            try {
                startLockTask()
                Log.d(TAG, "enterKioskMode: startLockTask OK")
            } catch (e: Exception) {
                Log.e(TAG, "enterKioskMode: falhou startLockTask", e)
            }
        }
    }

    /**
     * Desliga a rotação automática do sistema como um todo (o mesmo toggle
     * de "Girar automaticamente" que existe nas Configurações do Android).
     *
     * Isso evita que apps de terceiros sejam recriados do
     * zero — e percam sessão/login — só porque o tablet foi fisicamente
     * virado. O nosso próprio recurso "Rotação de tela" continua
     * funcionando normalmente, porque cada Activity do Amalfis pede
     * orientação por sensor para si mesma (requestedOrientation), o que
     * funciona independente desse toggle global.
     */
    private fun bloquearRotacaoAutomaticaGlobal() {
        try {
            devicePolicyManager.setSystemSetting(
                adminComponent,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                "0"
            )
            Log.d(TAG, "bloquearRotacaoAutomaticaGlobal: rotação automática do sistema desativada")
        } catch (e: Exception) {
            Log.e(TAG, "bloquearRotacaoAutomaticaGlobal: falhou", e)
        }
    }

    /**
     * Descobre o pacote real do app de Configurações do Android instalado
     * neste tablet, em vez de fixar "com.android.settings" — alguns
     * fabricantes usam variações desse nome.
     */
    private fun resolverPacoteConfiguracoes(): String {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName ?: "com.android.settings"
        } catch (e: Exception) {
            "com.android.settings"
        }
    }

    /**
     * Descobre quais apps do tablet respondem como tela Home (o launcher de
     * fábrica, por exemplo), para liberá-los no lock task também.
     */
    private fun resolverPacotesLauncher(): Set<String> {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Registra este app como a tela inicial (Home) padrão do dispositivo,
     * sem mostrar diálogo de escolha para o usuário. Assim, ao ligar,
     * reiniciar ou desligar/ligar o tablet, o Android abre o Amalfis
     * diretamente — nunca o launcher do sistema ou outras telas.
     *
     * Antes de registrar a nós mesmos, limpamos a preferência de qualquer
     * outro launcher (ex: MIUI Home) que "Sair do aplicativo" possa ter
     * registrado como Home temporário — evita dois registros conflitando.
     */
    private fun tornarAppTelaInicialPadrao() {
        try {
            resolverPacotesLauncher()
                .filter { it != packageName }
                .forEach { pacoteOutroLauncher ->
                    try {
                        devicePolicyManager.clearPackagePersistentPreferredActivities(
                            adminComponent,
                            pacoteOutroLauncher
                        )
                    } catch (e: Exception) {
                        // ignora, não é crítico
                    }
                }

            val filter = IntentFilter(Intent.ACTION_MAIN)
            filter.addCategory(Intent.CATEGORY_HOME)
            filter.addCategory(Intent.CATEGORY_DEFAULT)

            devicePolicyManager.addPersistentPreferredActivity(
                adminComponent,
                filter,
                ComponentName(this, MainActivity::class.java)
            )
            Log.d(TAG, "tornarAppTelaInicialPadrao: registrado como Home")
        } catch (e: Exception) {
            // Se não for possível (ex: app ainda não é device owner), ignora.
            Log.e(TAG, "tornarAppTelaInicialPadrao: falhou", e)
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE
            )
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "AMALFIS_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AmalfisAcesso")
        }

        val photoUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Nenhum app de câmera encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_MAIN)
        galleryIntent.addCategory(Intent.CATEGORY_APP_GALLERY)
        if (galleryIntent.resolveActivity(packageManager) != null) {
            startActivity(galleryIntent)
            return
        }

        val knownPackages = listOf(
            "com.miui.gallery",
            "com.google.android.apps.photos",
            "com.android.gallery3d"
        )
        for (pkg in knownPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                startActivity(launchIntent)
                return
            }
        }

        val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
        pickIntent.type = "image/*"
        if (pickIntent.resolveActivity(packageManager) != null) {
            startActivity(pickIntent)
            return
        }

        Toast.makeText(this, "Nenhum app de galeria encontrado", Toast.LENGTH_SHORT).show()
    }

    private fun openSite() {
        startActivity(Intent(this, WebActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val TAG = "AmalfisKiosk"
    }
}