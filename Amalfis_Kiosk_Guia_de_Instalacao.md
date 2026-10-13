# Guia de Instalação e Configuração — Amalfis Kiosk (Android)

Documento de referência para configurar um tablet Android (Redmi Pad SE ou similar) do zero até rodar o app Amalfis em modo kiosk (travado), com acesso administrativo protegido por senha.

---

## 1. Pré-requisitos

- **PC com Windows**, com o pacote **platform-tools** (ADB) baixado e extraído.
  Download: https://developer.android.com/tools/releases/platform-tools
- **Cabo USB** compatível com o tablet.
- **Arquivo `.apk`** do Amalfis Kiosk mais recente (gerado no Android Studio, em `app/build/outputs/apk/debug/app-debug.apk`).
- O tablet **sem nenhuma conta Google/Mi cadastrada** ainda (o registro como Device Owner só funciona em um aparelho "limpo", sem contas configuradas).

> Todos os comandos abaixo são digitados no **Prompt de Comando (cmd)** do Windows, dentro da pasta do `platform-tools`. Exemplo de como entrar na pasta:
> ```
> cd "C:\Users\SEU_USUARIO\Downloads\platform-tools-latest-windows\platform-tools"
> ```

---

## 2. Preparar o tablet (aparelho novo ou recém-formatado)

1. Ligue o tablet e siga a configuração inicial **sem fazer login em nenhuma conta** (Google, Mi Account, etc.) — pode pular essas etapas.
2. Vá em **Configurações > Sobre o tablet** e toque **7 vezes seguidas** em "Versão do MIUI/HyperOS" até aparecer a mensagem "Agora você é um desenvolvedor!".
3. Volte em **Configurações > Configurações adicionais > Opções do desenvolvedor** e ative:
   - **Depuração USB**
   - **Instalar via USB** (pode pedir login com conta Mi + internet só pra liberar esse toggle — não precisa manter a conta depois)
4. Conecte o tablet no PC via cabo USB.
5. Na tela do tablet, autorize a depuração USB quando o pop-up aparecer (marque "sempre permitir deste computador").

---

## 3. Confirmar que o PC enxerga o tablet

```
adb devices
```
Deve aparecer o número de série do tablet com a palavra `device` ao lado. Se aparecer `unauthorized`, olhe a tela do tablet e aceite o pop-up de autorização.

---

## 4. Registrar o app como Device Owner

Esse passo só funciona com o tablet limpo (sem contas), e só precisa ser feito **uma única vez** por aparelho.

```
adb shell dpm set-device-owner com.amalfis.kiosk/.AdminReceiver
```

Resultado esperado:
```
Success: Device owner set to package com.amalfis.kiosk
Active admin set to component {com.amalfis.kiosk/com.amalfis.kiosk.AdminReceiver}
```

Para confirmar depois, a qualquer momento:
```
adb shell dpm list-owners
```
Deve mostrar `com.amalfis.kiosk/.AdminReceiver` como `DeviceOwner`.

> ⚠️ Se aparecer erro dizendo que já existe um device owner, é porque esse passo já foi feito antes — não precisa repetir.

---

## 5. Instalar o app

Com o APK dentro da pasta do `platform-tools` (ou usando o caminho completo entre aspas):

```
adb install -r app-debug.apk
```

Deve terminar com `Success`.

> Se der erro `INSTALL_FAILED_USER_RESTRICTED`, veja a seção **7. Problemas comuns**.

---

## 6. Primeira abertura e verificação

1. Abra o app manualmente uma vez no tablet (ou via `adb shell am start -n com.amalfis.kiosk/.MainActivity`).
2. Isso já registra automaticamente o Amalfis como **tela inicial (Home) padrão** do sistema — não precisa fazer mais nada.
3. Confirme que funcionou:
```
adb shell cmd shortcut get-default-launcher
```
Deve retornar:
```
Launcher: ComponentInfo{com.amalfis.kiosk/com.amalfis.kiosk.MainActivity}
Success
```

### Checklist de testes finais

| Teste | Como fazer | Resultado esperado |
|---|---|---|
| Boot automático | Reinicie o tablet (fisicamente ou pelo botão do app) | Abre direto no Amalfis, sem passar pelo launcher do Android |
| Câmera / Galeria / Site | Toque nos cards da tela principal | Abrem normalmente |
| Configurações | Toque no card "Configuração" | Abre sem pedir senha |
| Bateria / Brilho | Dentro de Configurações | Mostram valores reais e o brilho responde ao slider |
| Rotação de tela | Ative o switch "Rotação de tela" | Tela passa a girar com o sensor (desligado = trava em paisagem) |
| Wi-Fi | Botão "Wi-Fi" em Configurações | Abre a tela de Wi-Fi do Android por cima do app, sem sair do kiosk |
| Modo avião | Botão "Modo avião" em Configurações | Abre a tela de Modo avião do Android por cima do app |
| Reiniciar dispositivo | Botão "Reiniciar dispositivo" | Tablet reinicia e volta direto no Amalfis |
| Desligar dispositivo | Botão "Desligar dispositivo" | Tenta desligar por software; se o aparelho não permitir, mostra aviso pra usar o botão físico Power (ver seção 8) |
| Sair do aplicativo | Botão "Sair do aplicativo" + senha do admin | Sai do Amalfis e abre o launcher normal (MIUI Home) ou as Configurações do Android |

---

## 7. Problemas comuns (troubleshooting)

### `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`
A MIUI está bloqueando instalação via ADB.
- Confirme que **Instalar via USB** está ativado em Opções do desenvolvedor.
- Se não conseguir acessar essa tela porque o kiosk já está travando o aparelho, entre em **Modo de Segurança** (veja seção 9), ative o toggle lá, reinicie normalmente e tente instalar de novo.
- Alternativa: em vez de `adb install`, copie o APK pro tablet e instale por dentro:
  ```
  adb push app-debug.apk /data/local/tmp/app-debug.apk
  adb shell pm install -r /data/local/tmp/app-debug.apk
  ```
  (Não use `/sdcard/` — o sistema não tem permissão de leitura lá para instalação; use sempre `/data/local/tmp/`.)

### `adb.exe: failed to stat app-debug.apk: No such file or directory`
O terminal não está na pasta onde o APK está. Ou copie o APK pra pasta do `platform-tools`, ou use o caminho completo entre aspas.

### `Error: Activity not started, unknown error code 101`
O tablet está em modo Lock Task (kiosk) e bloqueia até comandos do ADB de abrirem outro app por cima. Isso é esperado — use o próprio botão "Sair do aplicativo" dentro do app (com senha) em vez de tentar abrir outro app via `adb shell am start`.

### App não é reconhecido pelo `am start`/`force-stop`
```
adb shell pm list packages | findstr amalfis
```
Se não retornar nada, o app não está instalado — reinstale (seção 5).

---

## 8. Sobre o botão "Desligar dispositivo"

O Android **não oferece uma API oficial de desligamento remoto** para apps Device Owner — só existe para reiniciar (`DevicePolicyManager.reboot()`). Um desligamento completo por software normalmente exige uma permissão de sistema que só apps assinados com a chave da própria ROM possuem.

O app tenta dois caminhos técnicos por baixo dos panos, mas em muitos tablets (inclusive Xiaomi/MIUI) nenhum funciona — nesse caso, ele mostra o aviso **"Segure o botão físico Power para desligar"**, que é o único método garantido nesses aparelhos.

---

## 9. Modo de Segurança (Safe Mode) — para emergências

Use apenas se precisar recuperar acesso ao sistema (ex: ativar "Instalar via USB" com o kiosk já travando a tela).

1. Desligue o tablet completamente (segure o Power por 10–15 segundos se a tela não responder).
2. Ligue de novo apertando Power.
3. Assim que a logo "Redmi" aparecer, **segure o botão Volume -** e mantenha até o Android terminar de carregar.
4. Deve aparecer **"Modo de segurança"** no canto da tela — nesse estado, o Amalfis fica desativado temporariamente e o launcher/Configurações do Android funcionam normalmente.
5. Faça o que precisar (ex: ativar "Instalar via USB").
6. Reinicie o tablet normalmente para sair do Modo de Segurança.

> Em tablets configurados como Device Owner, esse método pode não funcionar (trava de segurança corporativa) — isso é esperado e normal, não é um bug.

---

## 10. Atualizando o app (versões futuras)

Não precisa repetir o passo do Device Owner — ele permanece registrado mesmo depois de reinstalar o app.

```
adb install -r app-debug.apk
adb shell am force-stop com.amalfis.kiosk
adb shell am start -n com.amalfis.kiosk/.MainActivity
```

---

## 11. Senha de administrador

A senha usada em "Sair do aplicativo" fica definida no código-fonte, no arquivo:
```
app/src/main/java/com/amalfis/kiosk/AppConfig.kt
```
```kotlin
object AppConfig {
    const val ADMIN_PASSWORD = "amalfis2026"
}
```
Para trocar a senha, edite esse valor, gere um novo APK e reinstale nos tablets.

---

## 12. Referência rápida de comandos ADB

| Comando | Função |
|---|---|
| `adb devices` | Lista dispositivos conectados |
| `adb install -r app-debug.apk` | Instala/atualiza o app |
| `adb shell dpm set-device-owner com.amalfis.kiosk/.AdminReceiver` | Registra o app como Device Owner (só 1x, aparelho limpo) |
| `adb shell dpm list-owners` | Confirma quem é o Device Owner |
| `adb shell cmd shortcut get-default-launcher` | Confirma qual app é a tela Home padrão |
| `adb shell am force-stop com.amalfis.kiosk` | Força o app a fechar |
| `adb shell am start -n com.amalfis.kiosk/.MainActivity` | Abre o app manualmente |
| `adb shell pm list packages \| findstr amalfis` | Confirma se o app está instalado |
| `adb logcat -c` | Limpa o log antes de um teste |
| `adb logcat -d \| findstr AmalfisKiosk` | Mostra os logs de diagnóstico do app |
