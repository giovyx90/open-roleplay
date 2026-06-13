# Resource Pack Open Weapons

Snapshot minimale del resource pack per Open Weapons.

La cartella `assets/minecraft/` e' pronta per essere copiata dentro un resource
pack Minecraft 1.21.x. Il file `pack.mcmeta` qui incluso usa una descrizione
generica Open Roleplay e non contiene riferimenti al brand privato originale.

Gli item dispatcher inclusi sono:

- `crossbow.json`
- `firework_star.json`
- `iron_nugget.json`
- `paper.json`
- `shield.json`

I modelli e le texture sono stati copiati solo se referenziati da questi
dispatcher o dalle GUI/suoni del modulo.

Gli asset cosmetici delle armi vivono ora in
`../../open-cosmetics/assets/resource-pack/`. Questo pack mantiene i dispatcher
item e le varianti custom model data, mentre Open Cosmetics fornisce i modelli,
le texture e i suoni delle personalizzazioni.

Per generare uno zip pronto per Minecraft dalla radice della repository:

```bash
bash scripts/build-resource-packs.sh
```
