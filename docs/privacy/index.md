---
title: Git Wallpaper - Política de Privacidade / Privacy Policy
permalink: /privacy/
---

# Política de Privacidade do Git Wallpaper

Vigente desde 25 de outubro de 2026.

O Git Wallpaper é um app Android publicado pela Alt Software que transforma o seu gráfico de contribuições do GitHub no wallpaper da tela de bloqueio e/ou da tela inicial. Esta política explica quais dados o app acessa e como eles são tratados.

## Resumo

- O app **não tem servidor próprio**. Tudo é processado no seu celular.
- O app **não envia dados ao desenvolvedor** e **não compartilha dados com terceiros**.
- O app **não tem anúncios, analytics, rastreamento nem SDKs de terceiros**.

## Dados acessados

Depois que você autoriza o app no GitHub (permissão `read:user`), ele lê da API do GitHub:

- seu nome de usuário (login);
- a URL e a imagem do seu avatar;
- o seu calendário de contribuições (quantidade de contribuições por dia).

Esses dados são usados apenas para desenhar o wallpaper e a prévia dentro do app.

## Onde os dados ficam

- O token de acesso do GitHub é guardado **criptografado** (AES-256-GCM com chave do Android Keystore) no armazenamento privado do app.
- O calendário de contribuições, o avatar e as suas preferências ficam em cache no armazenamento privado do app, para funcionar sem internet.
- Esses dados **não entram em backup na nuvem** nem em transferência entre aparelhos.

## Comunicação de rede

O app se comunica somente com os serviços do GitHub, sempre por HTTPS:

- `github.com` (login pelo Device Flow);
- `api.github.com` (leitura das contribuições);
- `avatars.githubusercontent.com` (imagem do avatar).

O uso desses serviços também está sujeito à [Declaração de Privacidade do GitHub](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

## Permissões do Android

- **Internet**: para falar com o GitHub.
- **Definir wallpaper**: para aplicar a imagem gerada na tela de bloqueio e/ou inicial.

## Como apagar seus dados

- Toque em **Sair** no app: o token e o cache de contribuições são apagados do aparelho e as atualizações automáticas param.
- A imagem do avatar em cache e as preferências visuais (tema, formato etc.) ficam no aparelho até você desinstalar o app ou limpar os dados dele nas configurações do Android, o que remove todos os dados.
- Você pode revogar o acesso do app a qualquer momento em [github.com/settings/applications](https://github.com/settings/applications).

## Crianças

O app não é direcionado a menores de 13 anos, idade mínima exigida pelo GitHub.

## Alterações

Mudanças nesta política serão publicadas nesta página, com a nova data de vigência.

## Contato

Alt Software: [lucas.m.amadeu@gmail.com](mailto:lucas.m.amadeu@gmail.com)

---

# Git Wallpaper Privacy Policy

Effective October 25, 2026.

Git Wallpaper is an Android app published by Alt Software that turns your GitHub contribution graph into your lock screen and/or home screen wallpaper. This policy explains which data the app accesses and how it is handled.

## Summary

- The app has **no backend of its own**. Everything is processed on your phone.
- The app **does not send any data to the developer** and **does not share data with third parties**.
- The app has **no ads, analytics, tracking or third-party SDKs**.

## Data accessed

After you authorize the app on GitHub (`read:user` scope), it reads from the GitHub API:

- your username (login);
- your avatar URL and image;
- your contribution calendar (number of contributions per day).

This data is used only to draw the wallpaper and the in-app preview.

## Where the data is stored

- The GitHub access token is stored **encrypted** (AES-256-GCM with an Android Keystore key) in the app's private storage.
- The contribution calendar, the avatar and your preferences are cached in the app's private storage so the app works offline.
- This data is **excluded from cloud backups** and device-to-device transfers.

## Network communication

The app only talks to GitHub services, always over HTTPS:

- `github.com` (Device Flow sign-in);
- `api.github.com` (reading contributions);
- `avatars.githubusercontent.com` (avatar image).

Use of these services is also subject to the [GitHub General Privacy Statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

## Android permissions

- **Internet**: to talk to GitHub.
- **Set wallpaper**: to apply the generated image to the lock and/or home screen.

## Deleting your data

- Tap **Sign out** in the app: the token and the contributions cache are deleted from the device and automatic updates stop.
- The cached avatar image and your visual preferences (theme, shape, etc.) stay on the device until you uninstall the app or clear its data in the Android settings, which removes all of its data.
- You can revoke the app's access at any time at [github.com/settings/applications](https://github.com/settings/applications).

## Children

The app is not directed to children under 13, the minimum age required by GitHub.

## Changes

Changes to this policy will be published on this page with a new effective date.

## Contact

Alt Software: [lucas.m.amadeu@gmail.com](mailto:lucas.m.amadeu@gmail.com)
