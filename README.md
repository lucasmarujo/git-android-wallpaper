# Git Wallpaper

App Android nativo (Kotlin + Jetpack Compose) que transforma o seu **gráfico de contribuições do GitHub** no papel de parede da **tela de bloqueio**, da **tela inicial** ou das duas, e o mantém **atualizado automaticamente**.

Tudo roda no celular. Não há servidor, backend nem serviço em nuvem do app: ele fala direto com a API do GitHub.

## Como funciona

1. Toque em **Entrar com GitHub**. O app mostra o link e um código; toque em **Copiar código e abrir o GitHub**, cole o código e autorize. Se preferir autorizar em outro aparelho, use **Abrir link** ou **Copiar link**.
2. Escolha onde aplicar (bloqueio, início ou ambas), o tema, a cor dos commits (do tema ou qualquer cor no seletor RGB), o período (este ano ou últimos 12 meses) e o formato dos quadrados.
3. Toque em **Aplicar wallpaper**. Abre uma prévia em tela cheia da tela de bloqueio: faça pinça para dar zoom e arraste para posicionar o gráfico, depois toque em **Aplicar**. Se o app mostrar o aviso de bateria, toque em **Abrir configurações de bateria** e marque o Git Wallpaper como "Não otimizar" (ou "Sem restrições").

Pronto: você não precisa mais abrir o app. O zoom e a posição escolhidos ficam salvos e são usados em todas as atualizações. Ele verifica o GitHub sozinho no intervalo escolhido (30 min a 12 h), redesenha o wallpaper quando entra um commit novo e também na virada do dia (mesmo sem internet, usando o cache), e continua agendado depois de reiniciar o celular ou atualizar o app.

## Instalação

Pela Google Play (teste fechado, pacote `dev.marujo.gitwallpaper`) ou baixando o APK mais recente em [Releases](../../releases) direto no celular (será preciso permitir instalação de fontes desconhecidas).

Requisitos: Android 8.0 (API 26) ou superior.

## Funcionalidades

- Login com GitHub via **OAuth Device Flow** (sem servidor e sem client secret) ou com **Personal Access Token** (escopo `read:user`).
- Wallpaper vertical gerado no aparelho, na resolução real da tela: um quadrado por dia do ano (ou dos últimos 12 meses), com o dia de hoje destacado por um anel, dias futuros esmaecidos e o rodapé com o total de contribuições, as sequências, o seu avatar e o @usuário.
- Layout pensado para a tela de bloqueio: o conteúdo fica entre o relógio/data e o ícone de desbloqueio/atalhos, já compensando o zoom de ~1,1× que o Android aplica ao wallpaper. O número de colunas se adapta à proporção da tela.
- 5 temas: Escuro (GitHub), Claro, AMOLED, Oceano e Pôr do sol. Quadrados arredondados ou círculos.
- Cor dos commits personalizada: seletor RGB com prévia; o tema continua definindo o fundo e os textos, e os quatro níveis de contribuição são derivados da cor escolhida.
- Ajuste de enquadramento: prévia em tela cheia simulando a tela de bloqueio (com relógio), pinça para zoom (0,5× a 2,5×) e arrastar para posicionar; o ajuste é salvo e aplicado também nas atualizações automáticas.
- Atualização automática com WorkManager: periódica, com backoff exponencial, funciona offline para a virada do dia, sobrevive a reinício e a atualização do app, e evita trocas desnecessárias comparando um fingerprint SHA-256 do que seria desenhado.
- Atalho para as configurações de otimização de bateria, para o Android não adiar as atualizações (sem pedir a isenção direta, seguindo a política da Google Play).
- Interface em português (pt-BR) e inglês.

## Privacidade e segurança

- O token do GitHub fica criptografado com **AES-256-GCM** usando uma chave do **Android Keystore** e nunca sai do aparelho, exceto nas chamadas HTTPS para `api.github.com`.
- Escopo mínimo: `read:user`.
- Backup do app desativado, tráfego em texto claro proibido.
- Para incluir contribuições de repositórios privados no gráfico, ative **Private contributions** no seu perfil do GitHub.
- [Política de privacidade](https://lucasmarujo.github.io/git-android-wallpaper/privacy/) (fonte em `docs/privacy/index.md`, publicada pelo GitHub Pages).

## Desenvolvimento

Stack: Kotlin 2.4, AGP 9.4, Gradle 9.8, Compose BOM 2026.09, WorkManager, DataStore, OkHttp 5, kotlinx.serialization. `compileSdk 37`, `targetSdk 36`, `minSdk 26`.

```bash
./gradlew testDebugUnitTest   # 143 testes (JVM + Robolectric)
./gradlew lintDebug
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

### Client ID do OAuth App

O Client ID do OAuth App fica em `gradle.properties` (`gitWallpaper.githubClientId`). Ele não é segredo: o Device Flow não usa client secret. Para usar o seu próprio app:

1. Crie um OAuth App em <https://github.com/settings/applications/new> e marque **Enable Device Flow**.
2. Troque o valor de `gitWallpaper.githubClientId` ou passe `-PgitWallpaper.githubClientId=SEU_ID` no build.

Sem Client ID o app continua funcionando, apenas com login por Personal Access Token.

### Release assinada

Builds de release são assinados quando existe um `keystore.properties` na raiz (fora do Git):

```properties
storeFile=C:/caminho/para/release.jks
storePassword=...
keyAlias=git-wallpaper
keyPassword=...
```

No CI, as mesmas informações vêm dos secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` e `SIGNING_KEY_PASSWORD`. Ao criar uma tag `v*`, o workflow gera o APK assinado e publica o Release.

## Arquitetura

Detalhes completos em [docs/PLANEJAMENTO.md](docs/PLANEJAMENTO.md).

```
app/src/main/java/com/lucasmarujo/gitwallpaper
├── domain/          modelos e regras puras (calendário, estatísticas, configurações)
├── data/            GitHub GraphQL, avatar, Device Flow, sessão criptografada, DataStore
├── wallpaper/       grade, layout, renderização em Canvas, fingerprint e aplicação
├── work/            WorkManager (worker, factory e agendamento)
└── ui/              Compose: login, tela principal e tema
```
