package com.amalfis.kiosk

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Modo imersivo do kiosk.
 *
 * Esconde a barra de navegação do Android (aquela faixa branca no rodapé
 * do tablet, com o botão de voltar). Com ela escondida, o conteúdo do app
 * passa a ocupar também esse espaço — os cards "sobem" e o rodapé
 * "Amalfis © 2026" fica na borda de baixo da tela.
 *
 * O comportamento usado é o "transient bars by swipe" (imersivo pegajoso):
 * a barra continua existindo, mas só aparece — de forma translúcida e por
 * alguns segundos — quando o usuário **arrasta o dedo de baixo para cima**
 * a partir da borda inferior. Depois ela some sozinha de novo.
 *
 * Como o app roda em Lock Task (Device Owner), nesse gesto o Android
 * mostra apenas o botão de voltar; Home e Recentes continuam bloqueados.
 */
object ModoImersivo {

    /**
     * Se ficar `true`, além da barra de baixo também esconde a barra de
     * status (a faixa no topo da tela). Deixe `false` para manter o
     * comportamento atual do topo.
     */
    private const val ESCONDER_TAMBEM_BARRA_DE_STATUS = false

    /**
     * Aplica o modo imersivo na Activity. Pode ser chamado quantas vezes
     * for necessário (onCreate, onResume, ao voltar de um diálogo etc.).
     */
    fun aplicar(activity: Activity) {
        val janela = activity.window ?: return

        // Evita que o sistema pinte um "fundo de contraste" claro por baixo
        // da barra de navegação quando ela reaparecer no gesto.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            janela.isNavigationBarContrastEnforced = false
        }

        val controlador = WindowCompat.getInsetsController(janela, janela.decorView)

        // Barras só reaparecem no gesto de arrastar a partir da borda,
        // e voltam a sumir automaticamente.
        controlador.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val tipos = if (ESCONDER_TAMBEM_BARRA_DE_STATUS) {
            WindowInsetsCompat.Type.systemBars()
        } else {
            WindowInsetsCompat.Type.navigationBars()
        }
        controlador.hide(tipos)
    }

    /**
     * Reaplica o modo imersivo quando a janela volta a ter o foco.
     * Chame de dentro de `onWindowFocusChanged`, senão a barra volta a
     * ficar fixa depois de abrir a câmera, um diálogo ou o teclado.
     */
    fun aoGanharFoco(activity: Activity, temFoco: Boolean) {
        if (temFoco) {
            aplicar(activity)
        }
    }
}
