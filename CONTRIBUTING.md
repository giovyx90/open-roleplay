# Contribuire a Open Roleplay

Grazie per l'interesse nel progetto. Questa repository accetta contributi tramite
pull request guidate.

Prima di tutto: Open Roleplay e' una **base**, non un prodotto chiavi in mano
(vedi [`FILOSOFIA.md`](FILOSOFIA.md)). Questo cambia cosa significa "un buon
contributo". Una buona PR rende il framework piu' capace o piu' stabile **senza
decidere cose che spettano al singolo server**.

## Due modi di partecipare

Capire quale dei due stai facendo evita la maggior parte degli equivoci in review:

- **Estendere per il tuo server** — nuovi lavori, reati, aziende, prezzi,
  ambientazione. Quasi sempre **non e' codice**: e' configurazione, e vive nel
  tuo server, non in questa repo. Se ti serve agganciare i tuoi sistemi
  (economia, storage, …) scrivi un adapter nel *tuo* plugin. Vedi
  [`docs/costruire-sopra.md`](docs/costruire-sopra.md).
- **Contribuire alla base** — rendere il framework piu' capace, piu' stabile o
  piu' leggibile per tutti. E' questo che accogliamo qui sotto forma di PR.

Se la tua estensione e' grande e autonoma, valuta un **modulo a se'** che vive
accanto a Open Roleplay invece che dentro: il punto 4 di
[`docs/costruire-sopra.md`](docs/costruire-sopra.md) spiega come.

## I quattro pilastri valgono in review

Ogni PR alla base viene letta attraverso i pilastri del progetto. Se una proposta
li viola, di norma va riportata in configurazione o dietro un adapter, non
aggiunta al core.

- **Adapter-first** — niente economia/storage/permessi cablati. Se serve un
  sistema trasversale, passa da un adapter con un default che degrada in modo
  pulito quando il backend reale manca.
- **Neutralita' d'ambientazione** — il **test di neutralita'**:
  *funzionerebbe identica su un server fantasy medievale e su uno cyberpunk?* Se
  la risposta e' no, la scelta e' editoriale e va in config (con un esempio in
  `examples/`), non nel codice.
- **RP-First** — niente sistemi nascosti che "sanno" cose che nessun personaggio
  potrebbe sapere. La logica rispetta la causalita' del roleplay.
- **Trasparenza** — formati aperti, codice leggibile, nessun lock-in.

## Regole pratiche

- Mantieni le modifiche piccole e revisionabili.
- Descrivi il **comportamento** cambiato, non solo i file modificati.
- Se tocchi un'API pubblica o un'interfaccia adapter, dillo esplicitamente: sono
  contratti su cui altri costruiscono e vanno trattati come tali.
- Quando aggiungi una manopola di config, aggiorna gli esempi
  (`examples/realistico-it/` e, dove esiste, `examples/fantasy/`).
- Non includere segreti, token, IP privati, dump database, log reali o dati
  utente.
- Non aggiungere asset, modelli, texture, suoni o marchi se non sei certo di
  poterne concedere la licenza (vedi [`TRADEMARKS.md`](TRADEMARKS.md)).
- Per codice Paper/Bukkit, evita I/O pesante sul main thread e torna sul main
  thread prima di usare API Bukkit non thread-safe.

## Validazione

Build e test completi, in locale come in CI:

```bash
mvn -B -ntp package
```

Per validare un singolo modulo con le sue dipendenze di reactor:

```bash
mvn -B -ntp -pl <modulo> -am package
```

I moduli che hanno test (`src/test/java/...`) sono il posto giusto dove
proteggere le logiche delicate — calcolo paghe, catena di custodia dei fascicoli,
controllo del territorio. Se aggiungi comportamento al core, aggiungi anche il
test che lo blocca.
