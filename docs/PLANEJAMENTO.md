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
| F3 | Ler o gráfico de contribuições | GraphQL `viewer.contributionsCollection.contributionCalendar` |
| F4 | Gerar o wallpaper | Renderização em `Canvas` na resolução física da tela |
| F5 | Aplicar em bloqueio/início/ambas | `WallpaperManager.setBitmap` com `FLAG_LOCK` / `FLAG_SYSTEM` |
| F6 | Atualizar automaticamente | `PeriodicWorkRequest` único (30 min a 12 h) com rede obrigatória |
| F7 | Personalização | Tema, período, formato, posição vertical e estatísticas |
| F8 | Pré-visualização | Mesmo renderizador em escala reduzida, recalculado a cada ajuste |
| F9 | Sair da conta | Limpa sessão, cache e status, e cancela o agendamento |

### Não funcionais

- **Segurança**: token cifrado com AES-256-GCM (chave no Android Keystore), escopo mínimo, sem backup, sem cleartext, `Session.toString()` não expõe o token.
- **Bateria e rede**: uma única chamada GraphQL por ciclo; o wallpaper só é reescrito quando o fingerprint SHA-256 do conteúdo muda; WorkManager respeita Doze e App Standby.
- **Robustez**: falhas de rede geram `retry` com backoff exponencial (máx. 3 tentativas por ciclo); token revogado encerra a sessão e cancela o agendamento; cache local permite aplicar offline.
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
│ ContributionRepository ── GitHubGraphQlClient │   │ ContributionGrid  │
│ SettingsRepository / UpdateStatusRepository   │   │ WallpaperLayout   │
│ (DataStore Preferences)                       │   │ Renderer (Canvas) │
└───────────────────────────────────────────────┘   │ Fingerprint       │
                                                    │ WallpaperApplier  │
┌──────────────── work ─────────────────────────┐   └─────────▲─────────┘
│ WorkManagerWallpaperScheduler                 │             │
│ WallpaperUpdateWorker ────────────────────────┼─────────────┘
│ WallpaperWorkerFactory (Configuration.Provider)│
└───────────────────────────────────────────────┘
```

### Fluxo de atualização

1. `WallpaperUpdateWorker` dispara (periódico, com rede) e chama `WallpaperUpdater.update(force = false)`.
2. `ContributionRepository.refresh()` lê o token da sessão e busca o calendário no GraphQL; o resultado vai para o cache.
3. O fingerprint (calendário + configurações visuais + tamanho da tela + idioma + versão do render) é comparado com o último aplicado.
4. Se mudou: renderiza o bitmap no tamanho físico da tela, aplica via `WallpaperManager`, recicla o bitmap e grava o novo status.
5. Resultado do worker: `success` (aplicado ou sem mudanças), `retry` (rede/GitHub), `failure` + cancelamento (sessão perdida).

### Layout do wallpaper

- Grade semanal começando no domingo, como no GitHub.
- 3 e 6 meses: uma linha de semanas. 12 meses: duas linhas empilhadas (27 + 26 semanas) para manter quadrados legíveis no retrato.
- Margens laterais de 7%, área segura de 8% em cima e embaixo (relógio e atalhos da tela de bloqueio), posição vertical ajustável de 25% a 80%.
- Rótulos de meses e bloco de estatísticas formatados com ICU `MessageFormat` (plural correto em pt-BR e inglês, inclusive para zero).

## 4. Decisões técnicas

| Decisão | Alternativas consideradas | Motivo |
|---------|---------------------------|--------|
| OAuth Device Flow | OAuth web com redirect; scraping do perfil | Não precisa de client secret nem servidor; funciona 100% no aparelho |
| Wallpaper estático (`setBitmap`) | Live Wallpaper | Única forma confiável de mirar só a tela de bloqueio em todas as versões |
| WorkManager periódico | AlarmManager exato | Respeita bateria, sobrevive a reboot, não exige permissão de alarme exato |
| DataStore Preferences | SharedPreferences, Room | API assíncrona com Flow; volume de dados pequeno |
| Keystore + AES-GCM próprio | `EncryptedSharedPreferences` | A biblioteca `security-crypto` foi descontinuada |
| Container de DI manual | Hilt/Koin | Poucos componentes; evita kapt/ksp e reduz superfície de build |
| GraphQL `viewer` | REST + scraping HTML | API oficial, uma chamada traz login e calendário juntos |

## 5. Testes

123 testes em `app/src/test` (JVM + Robolectric), rodando em CI a cada push:

- **Domínio puro**: estatísticas e sequências, grade semanal e rótulos de meses, layout em todas as posições e períodos, fingerprint.
- **Integração HTTP (MockWebServer)**: GraphQL (sucesso, 401, 5xx, erros GraphQL, conexão derrubada, enum desconhecido) e Device Flow (pending, slow_down, negado, expirado, tempo esgotado, falha de rede transitória).
- **Persistência real (DataStore em arquivo temporário)**: sessão cifrada, corrupção de chave, configurações com valores inválidos, cache corrompido, status.
- **Renderização com pixels reais (Robolectric Native Graphics)**: tamanho do bitmap, cor de fundo por tema, cor de cada nível, círculo vs. quadrado, estatísticas ligadas/desligadas.
- **Orquestração**: updater (aplicar, pular sem mudança, forçar, fallback offline, sessão revogada, falha de wallpaper), worker (mapeamento de resultados e limite de tentativas), agendamento único no WorkManager.
- **UI**: ViewModels de login e principal e telas Compose (Robolectric).

## 6. Entrega e CI/CD

- `.github/workflows/android.yml`: testes + lint + APK debug em todo push/PR; em tags `v*`, gera APK release assinado (R8 + shrink de recursos) e publica no GitHub Releases.
- Chave de assinatura fora do repositório (`keystore.properties` local e secrets no GitHub).

## 7. Riscos conhecidos

- **Fabricantes agressivos com bateria** (Xiaomi, Samsung, Huawei) podem atrasar o WorkManager; o app sugere liberar a otimização de bateria.
- **Launchers com rolagem de wallpaper** podem ampliar a imagem na tela inicial; o recorte é enviado como dica para o sistema.
- **Contribuições privadas** só aparecem se o usuário habilitar a opção no perfil do GitHub.
- **Limite da API GraphQL** (5.000 pontos/h por usuário): o intervalo mínimo de 30 min consome ~2 chamadas/h, muito abaixo do limite.

## 8. Próximos passos sugeridos

- Widget de tela inicial com o mesmo renderizador.
- Suporte a telas dobráveis (render separado por display).
- Temas personalizados pelo usuário e modo que segue o tema claro/escuro do sistema.
- Publicação na Play Store (política de privacidade, ícones em alta, screenshots).
