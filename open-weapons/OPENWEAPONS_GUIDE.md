# OpenWeapons - guida comandi e test server

OpenWeapons contiene solo il modulo armi/crime pubblico:

- armi da fuoco, melee, munizioni, caricatori e accessori;
- armature, caschi, scudi, granate e C4;
- strumenti roleplay pubblici: passamontagna, C4, telecomando C4, manette, tronchesi, taser, corda e forbici;
- rapine;
- perquisizioni;
- liberazione forzata di un giocatore immobilizzato.

Non contiene telefoni, SOS, radio FDO, arresti, cauzioni, admin arresti o ricercati/wanted. I vecchi comandi `/arrest`, `/bail`, `/arrests`, `/wanted`, `/sos` e `/lawradio` non sono registrati in OpenWeapons.

## Comandi principali

### Catalogo oggetti

```text
/oggetti
/armi
```

`/armi` e' un alias di `/oggetti`.

Apre la GUI catalogo con solo categorie popolate: armi da fuoco, corpo a corpo, munizioni, caricatori, accessori, protezioni e strumenti RP.

Permessi utili:

- `openrp.weapons.view`: apre e consulta il catalogo.
- `openrp.weapons.give`: permette di dare oggetti dal catalogo.
- `openrp.weapons.debug`: strumenti debug e reload.
- `openrp.weapons.admin`: include i permessi principali del modulo.

Sottocomandi:

```text
/oggetti cerca <query> [pagina]
/oggetti lista <categoria> [pagina]
/oggetti dai <id> [quantita] [giocatore]
/oggetti ricarica
/oggetti bersaglio [vita] [preset]
```

`cerca` apre la ricerca nel catalogo.

`lista` apre una lista filtrata per categoria. Le categorie valide sono:

- `all`
- `firearms`
- `melee`
- `ammo`
- `magazines`
- `attachments`
- `equipment`
- `utilities`

`dai` consegna un item del catalogo. Da console devi indicare anche il giocatore.

`ricarica` ricarica le configurazioni di armi, munizioni, accessori, cosmetici, armature e granate.

`bersaglio` genera uno zombie dummy per provare danni e protezioni. Preset utili:

- `plain`
- `vest_light`
- `vest_heavy`
- `vest_heavy_plated`
- `helmet_ballistic`
- `helmet_riot`
- `helmet_sf`
- `light_full`
- `heavy_full`
- `plated_full`

Esempi:

```text
/oggetti cerca m4
/oggetti lista utilities
/oggetti dai m4a1 1 Giocatore
/oggetti bersaglio 200 heavy_full
```

### Ricarica e caricatori

Le armi a caricatore usano un flusso pensato per essere rapido in test:

- clic destro con l'arma in mano: inserisce il primo caricatore compatibile e carico trovato in offhand o inventario;
- `F` con l'arma in mano: estrae il caricatore inserito e lo restituisce con i colpi rimasti;
- clic destro con un caricatore in mano: lo riempie usando munizioni compatibili dall'inventario;
- se un caricatore e' vuoto o incompatibile, l'actionbar spiega cosa manca.

Le shotgun usano ricarica a cartucce sciolte: clic destro carica direttamente le cartucce compatibili.

### Strumenti RP inclusi

La categoria `utilities` contiene solo:

- Passamontagna
- C4
- Telecomando C4
- Manette
- Tronchesi
- Taser
- Corda
- Forbici

### Config armi

```text
/configarmi
```

Apre la GUI di configurazione armi/protezioni.

Permesso:

- `openrp.weapons.config`

GUI:

```text
/configarmi
/configarmi interfaccia
/configarmi interfaccia armi
/configarmi interfaccia protezioni
```

Armi:

```text
/configarmi ricarica
/configarmi lista
/configarmi campi <arma>
/configarmi leggi <arma> <percorso>
/configarmi imposta <arma> <percorso> <valore>
/configarmi rimuovi <arma> <percorso>
```

Armature:

```text
/configarmi armatura ricarica
/configarmi armatura lista
/configarmi armatura campi <armatura>
/configarmi armatura leggi <armatura> <percorso>
/configarmi armatura imposta <armatura> <percorso> <valore>
/configarmi armatura rimuovi <armatura> <percorso>
```

Caschi:

```text
/configarmi casco ricarica
/configarmi casco lista
/configarmi casco campi <casco>
/configarmi casco leggi <casco> <percorso>
/configarmi casco imposta <casco> <percorso> <valore>
/configarmi casco rimuovi <casco> <percorso>
```

### Banco accessori arma

```text
/bancoarmi
```

Apre il banco per montare o gestire accessori sulle armi.

Permesso:

- `openrp.weapons.workbench`

### Rapina

```text
/rapina <giocatore>
```

Avvia una rapina roleplay verso un giocatore vicino.

Permessi:

- `openrp.rob.use`: usa la rapina.
- `openrp.robbery.admin`: bypass staff/admin per limiti rapina.

Regole principali:

- devi essere un giocatore;
- devi impugnare un'arma da fuoco o melee riconosciuta da OpenWeapons;
- il target deve essere online, vicino e diverso da te;
- se la vittima scappa o non collabora, viene segnata come `Abbattibile` per il tempo previsto dalla logica rapina;
- se la vittima si disconnette durante la rapina, il suo inventario viene consegnato al rapinatore.

### Perquisizione

```text
/perquisisci <giocatore>
/perquisisci accetta
/perquisisci rifiuta
```

Permessi:

- `openrp.frisk.use`: permette di perquisire.
- `openrp.frisk.arrestable`: se il target rifiuta, viene marcato come `Arrestabile` invece che `Abbattibile`.

Regole principali:

- devi essere un giocatore;
- devi essere armato;
- il target deve essere online, vicino e diverso da te;
- la perquisizione apre una GUI sola lettura con equipaggiamento e inventario del target;
- il target puo' accettare o rifiutare;
- se rifiuta, compare sopra di lui:
  - `Abbattibile`, comportamento base;
  - `Arrestabile`, se chi perquisisce ha `openrp.frisk.arrestable`.

### Libera

```text
/libera <giocatore_immobilizzato>
```

Serve per minacciare chi ha immobilizzato un giocatore, obbligandolo a liberarlo.

Permesso:

- `openrp.weapons.uncuff`

Regole principali:

- devi essere un giocatore;
- il target deve essere ammanettato/immobilizzato;
- devi impugnare un'arma da fuoco;
- chi ha immobilizzato il target deve essere online e vicino;
- se non libera il target entro 15 secondi, diventa `Abbattibile` per 5 minuti.

## Cosa e' stato rimosso da OpenWeapons

Queste parti non fanno parte del modulo pubblico:

- telefoni e app telefono;
- SOS e GPS dispatch da telefono;
- radio FDO;
- comandi e GUI arresto;
- cauzioni;
- gestione admin arresti;
- ricercati/wanted;
- item `mobile_phone` e `law_radio`;
- asset phone/arrest/FDO nel resource pack pubblico.
- utility legacy non pubbliche come scanner, tracker, borsone, barriere, chiodi, paracadute, rampino, estintore, strumenti forensi e maschere speciali.

Se ti servono queste feature, tienile in un plugin dedicato e collegale a
OpenWeapons tramite integrazioni opzionali.

## Sviluppo e personalizzazione

Per sostituire armi, aggiungere texture, creare nuovi model data o ampliare il
catalogo, usa:

```text
DEVELOPER_GUIDE.md
```

La guida copre `weapons.yml`, `ammo.yml`, dispatcher del resource pack,
caricatori, stati visuali e checklist di build.

## Deploy per test su server

Da una shell nella root della suite Open Roleplay:

```bash
cd "/home/giovyx90/NEXT/Open Roleplay"
mvn -B -ntp -pl open-weapons -am package
```

Il jar viene generato qui:

```text
open-weapons/target/open-weapons-0.1.0-SNAPSHOT.jar
```

Per testarlo su un server Paper:

1. Ferma il server di test.
2. Rimuovi eventuali jar vecchi di OpenWeapons da `plugins/`, cosi' non carichi due versioni.
3. Copia il jar nuovo:

```bash
cp "open-weapons/target/open-weapons-0.1.0-SNAPSHOT.jar" "/percorso/server-test/plugins/OpenWeapons.jar"
```

4. Se testi anche il resource pack, ricostruisci o copia il pack aggiornato generato dal workflow della suite.
5. Avvia il server.
6. Controlla la console: non devono comparire errori su comandi mancanti o classi telefono/arresto/wanted.
7. In game prova:

```text
/oggetti
/armi
/configarmi
/bancoarmi
/rapina <player>
/perquisisci <player>
/libera <player>
```

Dipendenze consigliate sul server di test:

- Paper 1.21.x compatibile con la build;
- OpenCore, se vuoi registrazione modulo completa;
- OpenCosmetics, se vuoi skin/cosmetici armi;
- Nexo, se usi il resource pack con item custom;
- WorldGuard, se testi utility che leggono regioni;
- packetevents, se usi le animazioni/packet hook armi.

Per un test minimo puoi avviare anche senza alcune softdepend, ma la console deve restare pulita e le feature collegate a quelle dipendenze potrebbero essere limitate.
