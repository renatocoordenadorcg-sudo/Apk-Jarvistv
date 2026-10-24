# JARVIS Android / Android TV (APK via GitHub Actions)

App nativo mínimo (Kotlin + WebView, sem bibliotecas externas) que abre o painel JARVIS do seu servidor em tela cheia. Funciona em **Android TV**, celular e tablet.

## Gerar o APK no GitHub
1. Crie um repositório e envie **a pasta inteira** (arraste a pasta, não os arquivos soltos; se subirem soltos, o workflow se organiza sozinho).
2. Aba **Actions** › *Build APK* (roda sozinho). Em ~4–7 min há dois APKs em **Artifacts › JARVIS-apk**:
   - `JARVIS.apk` → versão **otimizada** (release: código reduzido, mais leve e rápida). **Use esta.**
   - `JARVIS-debug.apk` → reserva, caso a otimizada falhe.
3. Para baixar direto na TV: crie uma **Release** com tag `v1.1.0` (o APK é anexado) e use o link
   `https://github.com/SEU_USUARIO/SEU_REPO/releases/latest/download/JARVIS.apk`.

## Instalar na TV
- **Downloader** (app gratuito da loja da TV): digite o link da release acima; ou
- pendrive USB com o `JARVIS.apk` + um gerenciador de arquivos; ou `adb connect IP-DA-TV` + `adb install JARVIS.apk`.
- Ative "Instalar apps de fontes desconhecidas" para o Downloader/gerenciador quando a TV pedir.
- O app aparece no menu da TV com o ícone **JARVIS**.

## Controle remoto
| Tecla | Ação |
|---|---|
| Setas | navegar (painel, janelas e tela cheia) |
| OK | abrir/ampliar a câmera; OK de novo volta |
| Voltar | fecha câmera ampliada › tela cheia › janelas; sem nada aberto, abre o menu |
| Menu | menu do app: Recarregar · Alterar servidor · **Abrir ao iniciar** · Sair |
| Play/Pause | pausa/retoma a ronda |
| CH+/CH− (ou ⏩/⏪) | próxima/anterior da ronda |

**Abrir ao iniciar**: escolha *Painel*, *Tela cheia* ou *Ronda automática* — a TV já liga mostrando as câmeras.

## Leveza e fluidez
- O app liga sozinho o **Modo leve** na TV (sem brilhos, animações, desfoques e linhas de varredura) e aumenta a interface para a distância da sala.
- TV ligada o dia todo: reconecta sozinha se o servidor cair, recria a tela se o sistema matar o WebView por falta de memória e recarrega o painel a cada 6 h.
- Para a TV não engasgar: prefira **Ronda** (4 câmeras por vez) ou o fluxo **sub** das câmeras; evite 9+ vídeos ao mesmo tempo em TV Box simples. Câmeras em H.265 exigem conversão no servidor (CPU).
- Cadastrar câmeras é mais fácil pelo celular/PC; a TV serve para ver. (A ronda configurada fica salva só na TV em que foi criada.)

## Observações
- Debug e release usam a chave de depuração: instalam normalmente, mas não servem para a Play Store.
- O app carrega a página do servidor: novidades do painel chegam **sem gerar APK novo**.
- Nome do pacote: `com.jarvis.cameras`.
