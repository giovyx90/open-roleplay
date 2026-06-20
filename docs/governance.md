# Governance e branch

Questo documento descrive come e' organizzato lo sviluppo di Open Roleplay, sia
del framework sia del server faro.

## Branch

- **`main`** — produzione. E' cio' che gira sul server live. Protetto: ci si
  arriva solo con una PR di promozione da `dev`.
- **`dev`** — integrazione. E' il branch di **default** per le PR. Ci confluiscono
  i contributi di codice, costruzioni e modelli.

I contributori lavorano su un branch personale e aprono PR verso `dev`. La
promozione `dev → main` e' una PR periodica ("treno di rilascio") fatta dai
maintainer quando la CI e' verde.

```
feature/* ──PR──▶ dev ──PR (release train)──▶ main ──tag v*──▶ deploy
```

## Branch protection (impostazioni GitHub)

Da configurare in Settings → Branches:

**`main`**
- Richiedi PR prima del merge.
- Richiedi i check di stato: `build`, `validate-builds`, `validate-models`.
- Richiedi 1 approvazione di un maintainer.
- Niente force-push, niente cancellazione.
- (Consigliato) consenti merge solo da `dev`.

**`dev`**
- Richiedi PR prima del merge.
- Richiedi i check di stato: `build`, `validate-builds`, `validate-models`.
- Richiedi review dei CODEOWNERS sui path protetti (vedi `.github/CODEOWNERS`).

## Controllo qualita' (CI)

| Workflow | Quando | Cosa fa |
| --- | --- | --- |
| `build.yml` | push su `dev`/`main`, ogni PR | `mvn verify` (compila + test) e prepara gli asset |
| `validate-builds.yml` | PR/push che toccano `community-server/` | valida manifest, schematic e **sovrapposizioni** dei claim |
| `validate-models.yml` | PR/push che toccano modelli/registro | valida JSON, namespacing e **id duplicati** |
| `allocate-model-id.yml` | issue/PR con label modello | riserva e commenta il prossimo id libero |
| `deploy.yml` | tag `v*` su `main` | incolla le costruzioni approvate in produzione (RCON) |
| `release.yml` | tag `v*` | costruisce gli artifact e crea la release |

## Tre modi di contribuire

1. **Al framework** — migliorare i moduli `open-*` (codice, stabilita', API).
   Vedi [`../CONTRIBUTING.md`](../CONTRIBUTING.md).
2. **Per il tuo server** — configurazione e adapter nel *tuo* plugin, non qui.
   Vedi [`costruire-sopra.md`](costruire-sopra.md).
3. **Al server faro** — mondo, costruzioni e modelli condivisi. Vedi
   [`../community-server/README.md`](../community-server/README.md).

## Anti-grief

Il mondo di produzione del server faro **non si modifica a mano**. Ogni cambiamento
al mondo entra solo tramite PR approvata e deploy automatico. Questa e' la
garanzia che rende possibile collaborare a un mondo unico senza rischiare grief.
