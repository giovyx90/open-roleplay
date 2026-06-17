# Open Gestionale — Frontend

SPA React (Vite). File statici: nessuna logica server. Carica il layout
dall'API e renderizza i widget. È solo uno dei client possibili dell'API.

## Sviluppo

```bash
npm install
cp .env.example .env        # imposta VITE_API_URL (default http://localhost:3000)
npm run dev                 # http://localhost:5173
```

## Build

```bash
npm run build               # genera dist/ (statico)
npm run preview             # anteprima locale del build
```

`VITE_API_URL` è risolto a **build time**: per deploy diversi rigenera il build
con l'URL dell'API corretto (sul pannello Vercel/Netlify è una variabile
d'ambiente del progetto).

## Deploy

- **CDN (consigliato):** Vercel / Netlify / Cloudflare Pages. Carica `dist/` o
  collega il repo; imposta `VITE_API_URL`.
- **nginx / Docker:** vedi [`Dockerfile`](Dockerfile) e [`nginx.conf`](nginx.conf)
  (fallback SPA su `index.html`).

## Widget system

- [`src/widgets/Widget.jsx`](src/widgets/Widget.jsx) — wrapper con i quattro
  stati (loading / unavailable / error / data), fetch su `GET /widget/<id>`.
- [`src/widgets/registry.jsx`](src/widgets/registry.jsx) — mappa `widget id →
  componente`. Un id sconosciuto ricade su una vista JSON grezza, così un widget
  custom di un server mostra comunque qualcosa.
- I componenti per modulo stanno in `src/widgets/<modulo>.jsx`.

Il tema (`--accent`) è impostato a runtime dal `theme_color` del server, quindi
la stessa build si reskina per ogni server.
