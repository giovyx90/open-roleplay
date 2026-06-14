# OpenWeapons - guida sviluppatori

Questa base e' pensata per essere modificata da altri server. Le armi, le
texture e gli strumenti pubblici non sono hardcoded nel resource pack: devi
tenere allineati config, model data e dispatcher.

## File principali

- `src/main/resources/weapons.yml`: definizioni armi, danni, categorie, model data, stati visuali.
- `src/main/resources/ammo.yml`: munizioni, stack e penetrazione.
- `src/main/resources/attachments.yml`: accessori montabili dal banco armi.
- `src/main/resources/armor.yml`: armature e caschi.
- `src/main/resources/grenades.yml`: granate e C4.
- `src/main/resources/config.yml`: impostazioni runtime generali.
- `assets/resource-pack/assets/minecraft/items/*.json`: dispatcher Minecraft 1.21.x per CustomModelData.
- `assets/resource-pack/assets/minecraft/models/...`: modelli item.
- `assets/resource-pack/assets/minecraft/textures/...`: PNG usati dai modelli.

## Sostituire una texture esistente

1. Trova l'arma in `weapons.yml`, per esempio `m4a1`.
2. Leggi `material` e `custom-model-data`.
3. Apri il dispatcher corrispondente al materiale, di solito:

```text
assets/resource-pack/assets/minecraft/items/crossbow.json
```

4. Cerca la soglia uguale al `custom-model-data` dell'arma.
5. Apri il modello indicato in `model`, per esempio:

```text
assets/resource-pack/assets/minecraft/models/custom/tools/crime/weapons/ar/m4a1/m4a1_static_empty.json
```

6. Cambia il percorso texture nel modello oppure sostituisci il PNG referenziato.
7. Ricostruisci la pack:

```bash
cd "/home/giovyx90/NEXT/Open Roleplay"
bash scripts/build-resource-packs.sh
```

## Aggiungere una nuova arma

1. Aggiungi una nuova chiave in `weapons.yml`.
2. Scegli un `id` stabile in snake_case, per esempio `my_rifle`.
3. Scegli una categoria:

```text
PISTOL, SMG, ASSAULT_RIFLE, SEMI_AUTO_RIFLE, SNIPER, SHOTGUN, TASER, MELEE
```

4. Scegli il materiale. Le armi da fuoco usano normalmente `CROSSBOW`.
5. Riserva CustomModelData liberi nel dispatcher del materiale.
6. Aggiungi i modelli nel resource pack.
7. Configura almeno:

```yaml
my_rifle:
  display-name: "My Rifle"
  category: ASSAULT_RIFLE
  material: CROSSBOW
  custom-model-data: 6001
  visual-states:
    idle: 6001
    aiming: 7001
    reloading: 8001
  magazine-visual-offset: 2
  magazine-model-data: 33
  damage: 5.5
  headshot-multiplier: 2.0
  fire-rate-ticks: 4
  reload-time-ticks: 55
  magazine-size: 30
  max-distance: 75
  ammo-type: 556nato
  sound-shoot: entity.firework_rocket.blast
  sound-reload: block.iron_door.close
  recoil: 0.012
  hipfire-spread-deg: 4.0
  ads-spread-deg: 0.45
  moving-spread-multiplier: 1.75
  sneak-spread-multiplier: 0.75
  jump-spread-multiplier: 4.0
  falloff-start-distance: 50
  falloff-end-distance: 100
  falloff-min-multiplier: 0.65
  automatic: true
  fire-modes:
    - semi
    - auto
```

8. Aggiungi il caricatore in `assets/resource-pack/assets/minecraft/items/iron_nugget.json` se usi `magazine-model-data`.
9. Avvia `/oggetti ricarica` oppure riavvia il server.
10. Testa in game con `/armi`, munizioni e caricatore.

## Stati visuali consigliati

Per una buona esperienza crea sempre una matrice completa:

- `idle`: arma in mano.
- `aiming`: arma mentre il player mira.
- `reloading`: arma durante la ricarica.
- variante `empty`: senza caricatore.
- variante `magazine`: caricatore inserito.
- varianti con accessori se usi ottiche o grip.

Se mancano varianti, OpenWeapons prova un fallback, ma il cambio caricatore puo'
risultare visivamente incoerente.

## Caricatori e ricarica

Le armi non-shotgun usano item caricatore separati:

- `magazine-size`: capienza runtime.
- `magazine-model-data`: CustomModelData dell'item caricatore su `IRON_NUGGET`.
- clic destro sull'arma: inserisce un caricatore compatibile e carico.
- `F`: estrae il caricatore inserito.
- clic destro sul caricatore: lo riempie con munizioni compatibili.

Le shotgun usano cartucce sciolte e non richiedono un item caricatore.

## Aggiungere una munizione

1. Aggiungi una voce in `ammo.yml`.
2. Aggiungi il modello nel dispatcher `iron_nugget.json`.
3. Usa lo stesso id in `weapons.yml` come `ammo-type`.

Esempio:

```yaml
300blk:
  display-name: "Munizioni .300 BLK"
  material: IRON_NUGGET
  custom-model-data: 55
  max-stack: 64
  penetration-class: rifle
  armor-durability-damage: 2
  flesh-damage-multiplier: 1.0
  shield-durability-damage: 3
```

## Aggiungere uno strumento utility pubblico

La base espone solo pochi strumenti in `/armi > Strumenti RP`.

Per aggiungerne uno:

1. Aggiungi o riusa una voce in `UtilityItemType`.
2. Inseriscila in `UtilityItemType.openWeaponsCatalogTypes()`.
3. Implementa la logica in `UtilityItemListener` o in un listener dedicato.
4. Aggiungi modello e texture al dispatcher del materiale scelto.
5. Aggiorna questa guida e `OPENWEAPONS_GUIDE.md`.

## Resource pack build

Build solo pack:

```bash
cd "/home/giovyx90/NEXT/Open Roleplay"
bash scripts/build-resource-packs.sh
```

Build plugin e pack:

```bash
cd "/home/giovyx90/NEXT/Open Roleplay"
mvn -B -ntp -pl open-weapons -am package
```

Output principali:

```text
target/resource-packs/open-weapons-resource-pack.zip
open-weapons/target/open-weapons-0.1.0-SNAPSHOT.jar
```

## Checklist prima di consegnare

- Il `custom-model-data` in YAML esiste nel dispatcher.
- Il modello JSON punta a PNG esistenti.
- Le varianti `idle`, `aiming`, `reloading` sono coerenti.
- Il caricatore ha `magazine-model-data` e un modello su `iron_nugget.json`.
- `/oggetti ricarica` non stampa errori.
- `/armi` mostra la nuova voce nella categoria corretta.
- Il resource pack zip contiene PNG, modelli e `pack.png`.
