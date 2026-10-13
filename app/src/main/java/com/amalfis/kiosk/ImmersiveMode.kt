package com.amalfis.kiosk

import android.app.Activity
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controla a exibição das barras de sistema (status e navegação).
 *
 * Por padrão elas ficam ocultas, evitando aquela faixa branca sobreposta
 * à tela do app. Ao arrastar o dedo da borda da tela (de baixo para cima,
 * por exemplo), o Android mostra as barras temporariamente por cima do
 * conteúdo; assim que o usuário toca em qualquer outro lugar fora delas,
 * elas somem de novo sozinhas. Esse é o comportamento padrão do modo
 * imersivo "por gesto" do Android (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE),
 * então nenhum código extra de toque é necessário.
 */
object ImmersiveMode {

    /**
     * Aplica o modo imersivo na activity. Chame no onCreate (após
     * setContentView) e também no onResume/onWindowFocusChanged, já que o
     * Android tende a "devolver" as barras depois de diálogos, o teclado
     * virtual, ou quando o app volta ao primeiro plano.
     */
    fun apply(activity: Activity) {
        val window = activity.window

        // Deixa o conteúdo do app ocupar a tela toda, por trás de onde as
        // barras de sistema ficariam.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // Esconde barra de status (topo) e barra de navegação (embaixo/gestos).
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // Permite reaparecer temporariamente com um swipe a partir da borda,
        // e some de novo automaticamente ao tocar fora dela.
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Mantém a tela ligada (tempo de tela ilimitado enquanto o app estiver ativo)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
