# Deploy del server faro

Come il repo aggiorna i server live. Il principio anti-grief: **il mondo di
produzione cambia solo tramite questo flusso automatico**, mai a mano.

## Due server

| | Staging / Creative | Produzione |
| --- | --- | --- |
| Scopo | I builder costruiscono e testano | I giocatori entrano e fanno RP |
| Mondo | Copia creative / usa e getta | Mondo condiviso live |
| Alimentato da | branch `dev` | branch `main` |
| Edit a mano | Permessi (e' il laboratorio) | **Vietati** |

I server Minecraft veri vivono fuori dal repo (su VPS). Il repo e' la **fonte di
verita'** e il cancello: si entra in produzione solo passando di qui.

## Flusso

1. PR approvata e merge in **`dev`** → lo staging riceve plugin, resource pack e
   le build da provare.
2. Promozione **`dev → main`** (PR del "treno di rilascio") + tag `v*` →
   il workflow di deploy aggiorna la produzione.

## Cosa fa il deploy delle costruzioni

`scripts/deploy-builds.sh` legge `registry/claims.yml` e, per ogni claim con
`status: approved` e `deployed: false`, ordina al server di produzione di
incollare lo schematic alla sua `anchor`/`rotation`:

```
orp-build import <slug>
```

eseguito via **RCON**. Dopo il paste, il claim viene segnato `deployed: true`
con un commit di follow-up, cosi' non viene reincollato.

### Dry-run (sicuro, senza segreti)

```
scripts/deploy-builds.sh --dry-run
```

Elenca cosa verrebbe incollato senza connettersi a nulla. Utile in PR e in
locale.

### Deploy reale

Richiede questi **GitHub Secrets** (Settings → Secrets → Actions):

- `PROD_RCON_HOST`
- `PROD_RCON_PORT`
- `PROD_RCON_PASSWORD`

Il workflow [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml)
gira sui tag `v*` (tagliati da `main`) ed esegue lo script con i secret.

> ⚠️ **Prima di un deploy reale fai un backup del mondo di produzione.** Il
> paste e' additivo ma irreversibile senza backup.

## Requisiti sul server di produzione

- Il plugin `open-build` installato (fornisce `/orp-build import`).
- WorldEdit/FAWE installato.
- RCON abilitato (`server.properties`: `enable-rcon=true`, `rcon.password=...`).
- Gli schematic approvati disponibili al server (sync del repo o artifact del tag).
