package com.amalfis.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Escuta o evento de "tablet ligou" (BOOT_COMPLETED, e variações usadas por
 * alguns fabricantes como QUICKBOOT_POWERON) e abre o MainActivity
 * automaticamente, sem precisar de nenhuma ação do usuário.
 *
 * Isso funciona junto com o registro do app como tela inicial (Home) padrão
 * feito em MainActivity — a combinação dos dois garante que o Android nunca
 * mostre o launcher do sistema ou outra tela antes do Amalfis abrir.
 */
class BootReceiver : BroadcastReceiver() {

    private val actionsAceitas = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        "android.intent.action.QUICKBOOT_POWERON",
        "com.htc.intent.action.QUICKBOOT_POWERON"
    )

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in actionsAceitas) {
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            context.startActivity(launchIntent)
        }
    }
}
