## Costruzione per il server faro

Vedi la guida: `community-server/CONTRIBUTING-BUILDS.md`.

### Cosa ho costruito

<!-- Nome, scopo RP, dove va nel mondo. Allega screenshot se possibile. -->

- **Slug cartella:** `community-server/builds/<slug>/`
- **Mondo:** <!-- es. world -->
- **Anchor (angolo minimo):** x= , y= , z=
- **Dimensione (size):** x= , y= , z=
- **Rotazione:** <!-- 0/90/180/270 -->

### Checklist (la CI controlla questi punti)

- [ ] Cartella `community-server/builds/<slug>/` con `region.schem` (Git LFS) e `build.yml`.
- [ ] `build.yml` ha tutti i campi obbligatori e `size` coerente con lo schematic.
- [ ] Ho aggiunto la voce in `community-server/registry/claims.yml` con `min`/`max`.
- [ ] L'area **non si sovrappone** ad altri claim esistenti (nessun grief: niente costruzioni sopra le altre).
- [ ] Ho dichiarato `license` e `attribution` (asset che posso davvero licenziare).
- [ ] La PR punta a `dev`.

> Se la CI segnala una sovrapposizione, e' il nostro "conflitto di merge" spaziale:
> sposta l'anchor o coordina con l'autore del claim in conflitto.
