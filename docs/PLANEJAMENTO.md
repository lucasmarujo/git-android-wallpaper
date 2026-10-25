# Planejamento — Git Wallpaper

## 1. Visão

O usuário entra com a conta do GitHub, escolhe onde aplicar o wallpaper (tela de bloqueio, tela inicial ou ambas) e pronto: o gráfico de contribuições vira o papel de parede e se atualiza sozinho conforme ele faz commits.

Restrições do produto:

- **100% no aparelho**: nenhum backend, banco ou função em nuvem do app.
- **Kotlin nativo**, sem frameworks multiplataforma.
- **Login com GitHub** sem servidor intermediário.

## 2. Requisitos

### Funcionais

| # | Requisito | Como foi atendido |
|---|-----------|-------------------|
| F1 | Login com GitHub | OAuth Device Flow (`/login/device/code` + polling em `/login/oauth/access_token`) |
| F2 | Alternativa de login | Personal Access Token com escopo `read:user` |
| F3 | Ler o gráfico de contribuições | GraphQL `viewer { login avatarUrl contributionsCollection.contributionCalendar }` |
| F4 | Gerar o wallpaper | Grade vertical com um quadrado por dia, renderizada em `Canvas` na resolução física da tela |
| F5 | Aplicar em bloqueio/início/ambas | `WallpaperManager.setBitmap` com `FLAG_LOCK` / `FLAG_SYSTEM` |
| F6 | Atualizar automaticamente, sem abrir o app | `PeriodicWorkRequest` único (30 min a 12 h), sem exigir rede, + atalho para liberar a otimização de bateria |
| F7 | Personalização | Tema, cor dos commits (RGB), período (este ano / últimos 12 meses), formato e estatísticas |
| F8 | Pré-visualização | Mesmo renderizador em escala reduzida, recalculado a cada ajuste |
| F9 | Sair da conta | Limpa sessão, cache e status, e cancela o agendamento |
| F10 | Ajustar zoom e posição | Prévia em tela cheia da tela de bloqueio com pinça e arrastar; o enquadramento é salvo nas configurações e aplicado pelo renderizador em toda atualização |
| F11 | Widget redimensionável na tela inicial | `AppWidgetProvider` com `resizeMode` horizontal e vertical a partir de 1 célula; adicionado pelo app (`requestPinAppWidget`) ou pela lista de widgets do sistema; desenhado pelo mesmo renderizador no tamanho de cada widget |

### Não funcionais

- **Segurança**: token cifrado com AES-256-GCM (chave no Android Keystore), escopo mínimo, sem backup, sem cleartext, `Session.toString()` não expõe o token.
- **Bateria e rede**: uma única chamada GraphQL por ciclo; o wallpaper só é reescrito quando o fingerprint SHA-256 do conteúdo muda; WorkManager respeita Doze e App Standby.
- **Robustez**: falhas de rede geram `retry` com backoff exponencial (máx. 3 tentativas por ciclo), mas antes o worker redesenha a partir do cache se o dia virou; token revogado encerra a sessão e cancela o agendamento; o job persiste a reinício e a atualização do APK (verificado no emulador).
- **Concorrência**: um `Mutex` serializa atualizações do worker e da UI no mesmo processo.
- **Compatibilidade**: Android 8.0+ (API 26), `targetSdk 36`.

## 3. Arquitetura

Camadas simples, sem framework de DI (container manual), seguindo o guia oficial de arquitetura Android: UI → ViewModel → Repositórios/Serviços → Fontes de dados.

```
┌──────────────────────────── UI (Compose) ────────────────────────────┐
│ LoginScreen ◄── LoginViewModel          HomeScreen ◄── HomeViewModel │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
┌──────────────── data ─────────┴───────────────┐   ┌──── wallpaper ────┐
│ AuthRepository ── DeviceFlowClient (OkHttp)   │   │ DefaultWallpaper- │
│ SessionRepository ── KeystoreSecretCipher     │◄──┤ Updater (Mutex)   │
│ ContributionRepository ── GitHubGraphQlClient │   │ DayGrid           │
│ CachedAvatarRepository (disco + memória)      │   │ WallpaperLayout   │
│ Settings/UpdateStatus (DataStore Preferences) │   │ Renderer (Canvas) │
└───────────────────────────────────────────────┘   │ Fingerprint       │
                                                    │ WallpaperApplier  │
┌──────────────── work ─────────────────────────┐   └─────────▲─────────┘
│ WorkManagerWallpaperScheduler                 │             │
│ WallpaperUpdateWorker ────────────────────────┼─────────────┘
│ WallpaperWorkerFactory (Configuration.Provider)│
└───────────────────────────────────────────────┘
```

### Fluxo de atualização

1. `WallpaperUpdateWorker` dispara (periódico, com ou sem rede) e chama `WallpaperUpdater.update(force = false)`. O app não precisa estar aberto.
2. `ContributionRepository.refresh()` lê o token da sessão e busca o calendário no GraphQL; o resultado vai para o cache. Sem rede, usa o calendário em cache.
3. O fingerprint (calendário + avatar + data de hoje + configurações visuais + tamanho da tela + idioma + versão do render) é comparado com o último aplicado.
4. Se mudou (commit novo, virada do dia, nova versão do app): carrega o avatar (cache), renderiza o bitmap no tamanho físico da tela, aplica via `WallpaperManager`, recicla o bitmap e grava o novo status.
5. Resultado do worker: `success` (aplicado ou sem mudanças), `retry` (rede/GitHub, mesmo depois de redesenhar do cache), `failure` + cancelamento (sessão perdida).
6. Em toda execução o worker também redesenha os widgets a partir do cache. O agendamento existe enquanto houver wallpaper aplicado **ou** widget na tela inicial (`AutoUpdateSync`), e a atualização automática nunca aplica um wallpaper que o usuário ainda não aplicou.

### Layout do widget

- O gráfico nunca quebra linha. Com até ~100 dp de altura útil (uma linha do launcher, o padrão 4×1), usa a grade do GitHub: uma coluna por semana, domingo a sábado de cima para baixo, numa única faixa; esticar para os lados mostra mais semanas, sempre terminando na atual.
- Esticado na vertical, vira a mesma grade do wallpaper: um quadrado por dia, da esquerda para a direita e de cima para baixo, com o número de colunas escolhido para maximizar o quadrado.
- Widgets muito pequenos mostram os dias mais recentes que cabem com células de pelo menos 6 dp.
- Cabeçalho com o total de contribuições quando há espaço e as estatísticas estão ligadas; cantos com o raio de widget do sistema no Android 12+.

### Layout do wallpaper

- Grade **vertical**: um quadrado por dia, da esquerda para a direita e de cima para baixo. "Este ano" vai de 1º de janeiro a 31 de dezembro (dias futuros esmaecidos); "Últimos 12 meses" termina hoje.
- O número de colunas (10 a 30) é escolhido para maximizar o tamanho do quadrado dentro da área disponível; em celulares 19,5:9 fica em torno de 17 colunas × 22 linhas, e telas mais largas ganham mais colunas.
- Hoje recebe um anel na cor de destaque do tema.
- Zonas seguras definidas na tela **visível**: o bloco fica entre 27% (abaixo do relógio e da data) e 80% da altura (acima do ícone de desbloqueio e dos atalhos), com largura máxima de 70%. Como o Android amplia o wallpaper ~1,1× na tela de bloqueio (`config_wallpaperMaxScale`), essas medidas são convertidas para coordenadas da imagem antes do desenho.
- Rodapé: "**N contribuições** em 2026" (número e palavra na cor de destaque), sequência atual e recorde, avatar circular + @usuário. Textos formatados com ICU `MessageFormat` (plural correto em pt-BR e inglês, inclusive para zero) e reduzidos automaticamente se não couberem.

## 4. Decisões técnicas

| Decisão | Alternativas consideradas | Motivo |
|---------|---------------------------|--------|
| OAuth Device Flow | OAuth web com redirect; scraping do perfil | Não precisa de client secret nem servidor; funciona 100% no aparelho |
| Wallpaper estático (`setBitmap`) | Live Wallpaper | Única forma confiável de mirar só a tela de bloqueio em todas as versões |
| Grade vertical por dia | Grade semanal horizontal do GitHub | Aproveita o formato retrato do celular com quadrados bem maiores |
| Atalho para as configurações de bateria | Pedido direto de isenção (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | O pedido direto é restrito pela política da Google Play para esse tipo de app |
| WorkManager periódico | AlarmManager exato | Respeita bateria, sobrevive a reboot, não exige permissão de alarme exato |
| DataStore Preferences | SharedPreferences, Room | API assíncrona com Flow; volume de dados pequeno |
| Keystore + AES-GCM próprio | `EncryptedSharedPreferences` | A biblioteca `security-crypto` foi descontinuada |
| Container de DI manual | Hilt/Koin | Poucos componentes; evita kapt/ksp e reduz superfície de build |
| GraphQL `viewer` | REST + scraping HTML | API oficial, uma chamada traz login e calendário juntos |

## 5. Testes

143 testes em `app/src/test` (JVM + Robolectric), rodando em CI a cada push:

- **Domínio puro**: estatísticas e sequências, grade de dias (ano atual, ano bissexto, últimos 12 meses, fuso do GitHub adiantado), layout vertical (zonas seguras após o zoom, colunas adaptativas, proporção do preview), fingerprint.
- **Integração HTTP (MockWebServer)**: GraphQL (sucesso, avatar, 401, 5xx, erros GraphQL, conexão derrubada, enum desconhecido), Device Flow (pending, slow_down, negado, expirado, tempo esgotado, falha de rede transitória) e avatar (download, cache em disco entre instâncias, URL nova, erro, imagem inválida, tamanho excessivo, host fora do CDN do GitHub).
- **Persistência real (DataStore em arquivo temporário)**: sessão cifrada, corrupção de chave, configurações com valores inválidos ou de versões antigas, cache corrompido, status.
- **Renderização com pixels reais (Robolectric Native Graphics)**: tamanho do bitmap, cor de fundo por tema, cor de cada nível, dias futuros, anel de hoje, círculo vs. quadrado, estatísticas ligadas/desligadas, avatar.
- **Orquestração**: updater (aplicar, pular sem mudança, forçar, virada do dia online e offline, fallback do cache, avatar só ao redesenhar, sessão revogada, falha de wallpaper), worker (mapeamento de resultados e limite de tentativas), agendamento único no WorkManager sem exigir rede.
- **UI**: ViewModels de login e principal e telas Compose (Robolectric).

## 6. Entrega e CI/CD

- `.github/workflows/android.yml`: testes + lint + APK debug em todo push/PR; em tags `v*`, gera APK release assinado (R8 + shrink de recursos) e publica no GitHub Releases.
- Chave de assinatura fora do repositório (`keystore.properties` local e secrets no GitHub). Na Google Play, essa chave é a chave de upload e o Google gerencia a chave de assinatura do app (Play App Signing).
- Google Play: pacote `dev.marujo.gitwallpaper` na conta Alt Software, distribuído primeiro em teste fechado via AAB (`./gradlew bundleRelease`).
- Política de privacidade em `docs/privacy/index.md`, publicada pelo GitHub Pages em https://lucasmarujo.github.io/git-android-wallpaper/privacy/ e linkada no login e na tela principal.

## 7. Riscos conhecidos

- **Fabricantes agressivos com bateria** (Xiaomi, Samsung, Huawei) podem atrasar o WorkManager; o app mostra um aviso com atalho para as configurações de otimização de bateria enquanto o app não estiver liberado. Se o usuário **forçar a parada** do app nas configurações, o Android cancela os agendamentos até o app ser aberto de novo (regra do sistema).
- **Zoom do wallpaper**: as zonas seguras assumem o zoom padrão do AOSP (1,1×); em aparelhos sem zoom o conteúdo fica só um pouco mais compacto.
- **Política da Play Store**: o pedido direto de isenção de bateria (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) é restrito na Play Store, por isso o app só abre as configurações e o usuário libera manualmente.
- **Acesso para revisão**: o login exige uma conta GitHub; as instruções de acesso da Play orientam o revisor a usar a própria conta gratuita.
- **Launchers com rolagem de wallpaper** podem ampliar a imagem na tela inicial; o recorte é enviado como dica para o sistema.
- **Contribuições privadas** só aparecem se o usuário habilitar a opção no perfil do GitHub.
- **Limite da API GraphQL** (5.000 pontos/h por usuário): o intervalo mínimo de 30 min consome ~2 chamadas/h, muito abaixo do limite.
- **Widget e launchers**: alguns launchers não aceitam `requestPinAppWidget`; nesse caso o app esconde o botão e orienta a adicionar pela lista de widgets do sistema. O bitmap de cada widget é limitado a 1 milhão de pixels para ficar dentro do limite de memória do `RemoteViews`.

## 8. Próximos passos sugeridos

- Suporte a telas dobráveis (render separado por display).
- Fundo personalizado pelo usuário e modo que segue o tema claro/escuro do sistema.
- Produção na Play Store depois do teste fechado (contas pessoais exigem 12 testadores por 14 dias).
