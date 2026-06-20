# Server faro (community-server)

Questo layer e' il **server condiviso** di Open Roleplay: un server roleplay
concreto, costruito in pubblico, a cui chiunque puo' contribuire — codice,
costruzioni e modelli — **senza griefare**.

E' diverso dal resto del repo: i moduli `open-*` sono il **framework** neutro
(la base riusabile, "il mondo e' tuo"); qui invece il mondo e' **uno solo e
condiviso**, e si sviluppa insieme. Vedi [`FILOSOFIA-SERVER.md`](FILOSOFIA-SERVER.md).

## Regola d'oro: anti-grief

> Nessuno modifica il mondo di produzione direttamente. **Tutto entra in gioco
> solo passando da una pull request approvata.**

Costruisci → fatti approvare → entra in gioco. Il mondo live e' intoccabile a
mano: ci si arriva solo tramite il deploy automatico che incolla cio' che e'
stato revisionato.

## Struttura

```
community-server/
  README.md                 # questo file
  FILOSOFIA-SERVER.md        # perche' un server condiviso accanto al framework
  CONTRIBUTING-BUILDS.md     # come contribuire una costruzione
  CONTRIBUTING-MODELS.md     # come contribuire un modello/asset
  DEPLOY.md                  # come il repo aggiorna i server live
  server.yml                 # confini del mondo e parametri deploy
  builds/                    # una cartella per costruzione (schematic + manifest)
    _example/                # template di riferimento
  registry/
    claims.yml               # aree prenotate (rileva le sovrapposizioni)
    model-ids.yml            # id custom_model_data allocati
  assets/resource-pack/      # resource pack del server (modelli/texture)
```

## Branch e flusso

- Le PR di contribuzione puntano a **`dev`** (integrazione).
- `main` e' la **produzione**: ci si arriva solo con la promozione `dev -> main`.
- Merge in `dev` -> aggiorna il server di **staging/creative** (il laboratorio).
- Promozione su `main` + tag -> aggiorna la **produzione** e incolla le build.

Vedi [`../docs/governance.md`](../docs/governance.md).
