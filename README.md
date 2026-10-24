# Git Wallpaper

App Android nativo (Kotlin + Jetpack Compose) que transforma o seu **gráfico de contribuições do GitHub** no papel de parede da **tela de bloqueio**, da **tela inicial** ou das duas, e o mantém **atualizado automaticamente**.

Tudo roda no celular. Não há servidor, backend nem serviço em nuvem do app: ele fala direto com a API do GitHub.

## Como funciona

1. Toque em **Entrar com GitHub**. O app mostra um código; toque em **Copiar código e abrir o GitHub**, cole o código e autorize.
2. Escolha onde aplicar (bloqueio, início ou ambas), o tema, o período (3, 6 ou 12 meses), o formato dos quadrados e a posição.
3. Toque em **Aplicar wallpaper**. A partir daí o app verifica o GitHub no intervalo escolhido (30 min a 12 h) e só troca o wallpaper quando algo mudou.

> Dica: para atualizações pontuais, permita que o app rode sem restrição de bateria (o próprio app mostra um atalho para isso).

## Instalação

Baixe o APK mais recente em [Releases](../../releases) direto no celular e instale (será preciso permitir instalação de fontes desconhecidas).

Requisitos: Android 8.0 (API 26) ou superior.

## Funcionalidades

- Login com GitHub via **OAuth Device Flow** (sem servidor e sem client secret) ou com **Personal Access Token** (escopo `read:user`).
- Wallpaper gerado no aparelho, na resolução real da tela, com grade de contribuições, rótulos de meses e estatísticas (total no período, sequência atual e maior sequência).
- 5 temas: Escuro (GitHub), Claro, AMOLED, Oceano e Pôr do sol. Quadrados arredondados ou círculos.
- Período de 12 meses empilhado em duas linhas para caber bem em telas de celular.
- Atualização automática com WorkManager: exige rede, tem backoff exponencial, sobrevive a reinício do aparelho e evita trocas desnecessárias comparando um fingerprint SHA-256 do que seria desenhado.
- Interface em português (pt-BR) e inglês.

## Privacidade e segurança

- O token do GitHub fica criptografado com **AES-256-GCM** usando uma chave do **Android Keystore** e nunca sai do aparelho, exceto nas chamadas HTTPS para `api.github.com`.
- Escopo mínimo: `read:user`.
- Backup do app desativado, tráfego em texto claro proibido.
- Para incluir contribuições de repositórios privados no gráfico, ative **Private contributions** no seu perfil do GitHub.

## Desenvolvimento

Stack: Kotlin 2.4, AGP 9.4, Gradle 9.8, Compose BOM 2026.09, WorkManager, DataStore, OkHttp 5, kotlinx.serialization. `compileSdk 37`, `targetSdk 36`, `minSdk 26`.

```bash
./gradlew testDebugUnitTest   # 123 testes (JVM + Robolectric)
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
├── data/            GitHub GraphQL, Device Flow, sessão criptografada, DataStore
├── wallpaper/       grade, layout, renderização em Canvas, fingerprint e aplicação
├── work/            WorkManager (worker, factory e agendamento)
└── ui/              Compose: login, tela principal e tema
```
