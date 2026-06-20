## Modello / resource pack per il server faro

Vedi la guida: `community-server/CONTRIBUTING-MODELS.md`.

### Cosa ho aggiunto

<!-- Nome del modello, a cosa serve, base-item su cui si aggancia. -->

- **Base item:** <!-- es. minecraft:paper -->
- **custom_model_data allocato:** <!-- numero preso da registry/model-ids.yml -->
- **Path modello:** `assets/minecraft/models/custom/...`

### Checklist (la CI controlla questi punti)

- [ ] I JSON aggiunti sono validi e namespaced sotto `custom/` (niente override del vanilla).
- [ ] Texture/parent referenziati esistono nel pack.
- [ ] Ho registrato l'`id` in `community-server/registry/model-ids.yml` (nessun id duplicato sullo stesso base item).
- [ ] `bash scripts/build-resource-packs.sh` genera lo zip senza errori.
- [ ] La PR punta a `dev`.

> Non sai quale id usare? Apri prima una issue `model-proposal`: il bot ti riserva
> il prossimo id libero.
