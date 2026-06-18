# openroleplay.it — sito portfolio

Sito statico (HTML/CSS/JS, **nessun build step**) che presenta i moduli della
suite Open Roleplay e linka i JAR sulle Release di GitHub. Pensato per essere
servito su **openroleplay.it**.

## Struttura

```text
site/
├── index.html              # landing: hero, filosofia, moduli, architettura, installazione, download
├── moduli/
│   ├── open-weapons.html
│   ├── open-access.html
│   ├── open-cosmetics.html
│   ├── open-vending.html
│   ├── open-companies.html
│   ├── open-fdo.html
│   ├── open-crime.html
│   ├── open-jobs.html
│   ├── open-politics.html
│   ├── open-gestionale.html
│   ├── open-core.html
│   └── open-core-api.html
├── assets/
│   ├── css/styles.css      # design system: tema chiaro + accento verde, toggle dark
│   ├── js/modules.js       # registry canonico: moduli, download, footer, conteggi
│   ├── js/main.js          # tema, menu mobile, rendering moduli, copia-codice, reveal
│   └── img/                # favicon.svg, og-cover.svg
├── CNAME                   # openroleplay.it
├── .nojekyll               # disattiva Jekyll su GitHub Pages
├── robots.txt
├── sitemap.xml
└── 404.html
```

## Anteprima locale

Nessuna dipendenza: basta un server statico.

```bash
cd site
python3 -m http.server 8080
# apri http://localhost:8080
```

## Personalizzazione

- **Colori / tema:** le variabili sono in cima a `assets/css/styles.css`
  (`:root` per il tema chiaro, `[data-theme="dark"]` per quello scuro).
- **Contenuti moduli:** `assets/js/modules.js` è la fonte dati unica per
  conteggi, griglia homepage, card download e footer. Ogni pagina in `moduli/`
  resta autonoma per il contenuto lungo; per aggiungere un modulo crea la pagina,
  aggiorna `modules.js` e inserisci la URL in `sitemap.xml`.
- **Download:** i pulsanti puntano agli asset stabili della release GitHub più
  recente, ad esempio
  `https://github.com/giovyx90/open-roleplay/releases/latest/download/open-weapons.jar`.
  La workflow `.github/workflows/release.yml` pubblica automaticamente i JAR,
  gli zip dei resource pack e `SHA256SUMS.txt` quando viene creato un tag `v*`.

Asset attesi dal sito:

```text
open-weapons.jar
open-access.jar
open-cosmetics.jar
open-vending-machines.jar
open-companies.jar
open-fdo.jar
open-crime.jar
open-jobs.jar
open-politics.jar
open-core-paper.jar
open-core-api.jar
open-weapons-resource-pack.zip
open-cosmetics-resource-pack.zip
SHA256SUMS.txt
```

## Deploy su Nginx (openroleplay.it)

La cartella `site/` è statica e può essere sincronizzata così:

```bash
rsync -az --delete site/ giovyx-server:/var/www/openroleplay.it/
```

Il virtual host Nginx deve puntare a `/var/www/openroleplay.it` per
`openroleplay.it` e `www.openroleplay.it`.

## Deploy su GitHub Pages (custom domain openroleplay.it)

1. Repo **Settings → Pages → Build and deployment**: sorgente *GitHub Actions*
   (consigliato) oppure *Deploy from a branch* puntando alla cartella `site/`.
2. Il file `CNAME` imposta già il dominio `openroleplay.it`. Nel pannello DNS del
   dominio aggiungi un record `CNAME` `www → giovyx90.github.io` e i record `A`
   dell'apex verso gli IP di GitHub Pages (oppure un `ALIAS`/`ANAME` se il tuo
   provider lo supporta).

Workflow GitHub Actions di esempio (`.github/workflows/pages.yml`):

```yaml
name: Deploy site
on:
  push:
    branches: [main]
    paths: ["site/**"]
permissions:
  contents: read
  pages: write
  id-token: write
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: github-pages
    steps:
      - uses: actions/checkout@v4
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with: { path: site }
      - uses: actions/deploy-pages@v4
```

In alternativa, la cartella `site/` è servibile così com'è da qualsiasi host
statico (Vercel, Netlify, Cloudflare Pages, Nginx).
