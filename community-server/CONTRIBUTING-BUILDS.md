# Contribuire una costruzione

Le costruzioni del server faro entrano in gioco con il flusso
**Costruisci → fatti approvare → entra in gioco**. Nessuno modifica il mondo
live a mano: ci si arriva solo via PR approvata e deploy.

## Perche' schematic e non "git del mondo"

Il mondo Minecraft e' fatto di file `.mca` **binari**: git non sa fonderli, e
due persone che toccano la stessa region producono conflitti illeggibili anche
se costruiscono in punti diversi. Per questo ogni costruzione e' un **artefatto
isolato**:

- uno **schematic** (`region.schem`) — autoconsistente, indipendente dalla
  posizione;
- un **manifest** (`build.yml`) — dove va, quanto e' grande, come ruota;
- un **claim** in `registry/claims.yml` — il volume che prenota nel mondo.

Il "merge git su Minecraft" diventa cosi': **prenotazione spaziale** invece di
plot rigidi, e il conflitto = **due volumi che si sovrappongono**, rilevato
dalla CI. Su un mondo non piatto questo e' molto piu' comodo dei plot.

## Prerequisiti

- **Git LFS** installato: `git lfs install` (gli `.schem` sono versionati con LFS).
- WorldEdit o FAWE sul server di staging.

## Passi

1. **Costruisci** sul server di staging/creative (il laboratorio alimentato da `dev`).
2. **Esporta** lo schematic. Con WorldEdit:
   ```
   //pos1   //pos2        # seleziona l'area
   //copy                 # copia rispetto a un'origine nota
   //schem save region.schem
   ```
   Oppure, con il plugin del server:
   ```
   /orp-build export <slug>
   ```
   che salva schematic + uno scheletro di `build.yml` con `size` e `anchor`
   gia' compilati (evita gli errori manuali piu' comuni).
3. **Crea la cartella** `community-server/builds/<slug>/` con dentro
   `region.schem` e `build.yml`. Parti da [`builds/_example/`](builds/_example/).
4. **Compila `build.yml`**:
   - `anchor` — angolo **minimo** del paste nel mondo di produzione `{x,y,z}`.
   - `size` — estensione `{x,y,z}` in blocchi (deve combaciare con lo schematic).
   - `rotation` — 0/90/180/270.
   - `license` + `attribution` — usa solo asset che puoi licenziare.
5. **Registra il claim** in `community-server/registry/claims.yml`:
   ```yaml
   claims:
     - id: <slug>
       world: world
       min: { x: <anchor.x>, y: <anchor.y>, z: <anchor.z> }
       max: { x: <anchor.x + size.x>, y: <anchor.y + size.y>, z: <anchor.z + size.z> }
       pr: <numero-pr>
       status: pending
       deployed: false
   ```
   (Con `rotation` 90/270, ricordati di scambiare le estensioni x e z nel `max`.)
6. **Apri la PR verso `dev`** usando il template "build".

## Cosa controlla la CI (`validate-builds`)

- Lo schematic esiste ed e' un file valido (gzip/NBT).
- Il manifest ha tutti i campi e `size` e' coerente.
- L'AABB calcolato sta **entro i confini** del mondo (`server.yml`).
- Il claim in `claims.yml` combacia con l'AABB del manifest.
- **Nessuna sovrapposizione** con altri claim nello stesso mondo.

Se la CI segnala una sovrapposizione: e' il conflitto di merge spaziale. Sposta
l'`anchor` o coordina con l'autore del claim in conflitto, poi aggiorna la PR.

## Dopo l'approvazione

Merge in `dev` → la build e' disponibile sullo staging. Promozione `dev → main`
+ tag → il deploy incolla automaticamente lo schematic in produzione e segna
`deployed: true`. Vedi [`DEPLOY.md`](DEPLOY.md).
