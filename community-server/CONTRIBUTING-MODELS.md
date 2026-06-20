# Contribuire un modello / asset

I modelli del server faro sono **JSON** (formato aperto, mergeabile). Lo stesso
flusso delle costruzioni: si propongono, si revisionano, si approvano via PR.

## Il conflitto dei modelli: gli id

Due PR diverse possono scegliere lo stesso `custom_model_data` sullo stesso base
item: e' il "conflitto di merge" dei modelli. Per evitarlo c'e' un registro che
alloca gli id: [`registry/model-ids.yml`](registry/model-ids.yml).

## Passi

1. **Riserva un id** (consigliato): apri una issue *"Proposta modello"*. Un bot
   commenta il prossimo `custom_model_data` libero per il base item scelto e lo
   prenota.
2. **Aggiungi i file** sotto
   `community-server/assets/resource-pack/assets/minecraft/models/custom/community/...`
   (sempre namespaced sotto `custom/`, mai sopra i modelli vanilla).
3. **Registra l'id** in `registry/model-ids.yml`:
   ```yaml
   base_items:
     minecraft:paper:
       - { cmd: 1000, owner: community-server, model: custom/community/example_card, pr: <pr> }
   ```
4. **Verifica in locale**:
   ```
   bash scripts/build-resource-packs.sh
   ```
5. **Apri la PR verso `dev`** con il template "model".

## Cosa controlla la CI (`validate-models`)

- I JSON aggiunti sono validi.
- I path sono namespaced sotto `custom/`.
- Texture e `parent` referenziati esistono nel pack.
- **Nessun `cmd` duplicato** sullo stesso base item in `model-ids.yml`.
- `build-resource-packs.sh` produce lo zip senza errori.

## Modelli del framework vs del server

- I modelli **del framework** restano nei rispettivi moduli
  (`open-weapons/assets/...`, `open-cosmetics/assets/...`).
- I modelli **del server faro** stanno qui in `community-server/assets/...`.

Entrambi i pack vengono raccolti da `scripts/build-resource-packs.sh`; il
registro `model-ids.yml` tiene traccia degli id usati ovunque per evitare
collisioni tra pack.
