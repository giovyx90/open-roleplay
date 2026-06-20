# Filosofia di Open Roleplay

> Open Roleplay non è un server roleplay pronto all'uso. È l'**infrastruttura
> aperta** su cui costruire il tuo.

Questo documento spiega *cosa* è Open Roleplay, *cosa non è*, e perché questa
distinzione è la scelta di progetto più importante che abbiamo fatto.

## Una base, non un prodotto chiavi in mano

C'è una tentazione comune in questo spazio: impacchettare un server roleplay
"finito" — ambientazione decisa, lavori decisi, economia decisa — e distribuirlo
perché chiunque ci clicchi sopra "installa". È una strada che porta a un prodotto
che non fa bella figura quasi mai: ogni server è diverso, e un pacchetto rigido o
è troppo per chi vuole poco, o è troppo poco per chi vuole tanto. Finisce
incollato con script e fork, e smette di essere mantenibile.

Open Roleplay sceglie la strada opposta. È un **framework**: un insieme di moduli
indipendenti, contratti pubblici stabili e implementazioni di riferimento che
risolvono i problemi *strutturali* di un server roleplay — il ciclo di vita dei
plugin, l'accesso ai dati, le aziende, i lavori, le forze dell'ordine, il crimine,
gli accessi — lasciando a te ogni decisione *editoriale*: che mondo racconti, quali
lavori esistono, quanto vale una moneta, come si chiamano i corpi dello stato.

L'analogia giusta è quella tra un **framework** e un **sito preconfezionato**.
Open Roleplay è il framework: ti dà le fondamenta giuste e ti toglie il lavoro
ingrato, ma il server che i tuoi giocatori vedranno lo costruisci tu sopra. Chi
cerca un pulsante "installa e gioca" troverà di meglio altrove. Chi vuole
costruire qualcosa di solido senza reinventare ogni volta le fondamenta, è qui
che deve stare.

## Cosa significa "finito"

In un framework, "finito" non vuol dire "completo di ogni feature". Vuol dire che
le **fondamenta sono stabili**: i contratti pubblici non cambiano sotto i piedi,
ogni modulo si compila e si avvia da solo, e ciò che costruisci sopra non si
rompe al prossimo aggiornamento.

- **Il framework è finito** quando le sue API sono stabili, documentate e
  testate.
- **Il tuo server è finito** quando *tu* hai deciso ambientazione, contenuti e
  regole sopra quel framework.

Questi sono due lavori diversi, e Open Roleplay si occupa solo del primo. È un
confine deliberato: tenere il framework piccolo e onesto è ciò che lo rende
affidabile come base.

## I quattro pilastri

Ogni decisione di progetto passa da questi quattro principi. Se una proposta li
viola, di norma va riportata in configurazione o dietro un adapter, non aggiunta
al core.

### 1. Adapter-first

I sistemi trasversali — economia, storage, permessi, regioni, identità,
notifiche, audit — non sono cablati dentro i moduli. Passano da **adapter
sostituibili**. Porti la tua economia (Vault o altro), il tuo storage, i tuoi
permessi, senza fare il fork di niente.

Corollario: la **degradazione silenziosa**. Se un modulo opzionale non è
presente, la feature collegata sparisce in modo pulito invece di bloccare
l'avvio. Un server con solo Open Jobs deve funzionare; lo stesso server con
Open Companies accanto guadagna le funzioni che richiedono le aziende — senza
configurazione obbligatoria, senza errori.

### 2. Neutralità d'ambientazione

Il core non conosce nessuna ambientazione. Non sa cosa si mina, come si chiamano
i corpi dello stato, quanto vale una moneta. Conosce solo **concetti astratti** —
un lavoratore che svolge un'attività e viene pagato, un atto firmato e depositato,
un'organizzazione che controlla un territorio — e lascia tutto il resto a
configurazione e adapter.

Il **test di neutralità** è la domanda che ogni feature deve superare:
*funzionerebbe identica su un server fantasy medievale e su uno cyberpunk?*

- "Minatore" → sì → core (con config per nome e risorse).
- "Raccoglitore di adamantio" → no → config.
- "Lavoratore che estrae risorse da una location e riceve una paga" → sì → core.

Il realistico italiano incluso è solo la *configurazione di riferimento*, non il
prodotto. Ogni modulo che lo prevede include anche un esempio `fantasy` sullo
stesso identico codice, proprio per dimostrare che l'ambientazione vive nella
config.

### 3. RP-First

La logica rispetta la causalità del roleplay. Niente sistemi nascosti che
"sanno" cose che nessun personaggio potrebbe sapere. L'esempio canonico è
Open Crime: **non esiste un "heat"** automatico. Le forze dell'ordine non
ricevono notifiche dal nulla; apprendono solo tramite *Discovery* generate da
azioni RP concrete — una denuncia, una scoperta fisica, un arresto, un
informatore, un'indagine.

Lo stesso principio guida Open Jobs: **si paga l'attività reale, non il tempo**.
Stare fermi in miniera non è lavorare. La sessione traccia ciò che fai
fisicamente, non i minuti passati nella region.

### 4. Trasparenza

Open, perché è trasparente. Formati aperti (config in YAML, resource pack in
JSON + PNG senza strumenti proprietari), codice leggibile, nessuna scatola nera,
nessun lock-in. Trovi un bug, prendi il codice e lo sistemi. Non ti piace
qualcosa, lo modifichi. È gratuito, e lo sarà per sempre (Apache 2.0).

## Conseguenze pratiche di questa scelta

Capire che Open Roleplay è una base cambia il modo giusto di usarlo e di
contribuire:

- **Per chi gestisce un server:** non ti aspettare di "accendere" un mondo già
  scritto. Ti aspetti fondamenta solide e una configurazione di riferimento da
  cui partire e che riscriverai a tua immagine. Vedi
  [`docs/costruire-sopra.md`](docs/costruire-sopra.md).
- **Per chi sviluppa estensioni:** estendi *attraverso le API pubbliche e gli
  adapter*, non con fork del core. Le tue cose vivono accanto a Open Roleplay,
  non dentro.
- **Per chi contribuisce alla base:** una buona proposta rende il framework più
  capace o più stabile *senza* decidere cose che spettano al singolo server. In
  caso di dubbio, vince la config. Vedi [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Il framework e il server faro

C'è una seconda cosa, accanto al framework, e conviene non confonderle.

Il **framework** (`open-*`) resta quello descritto qui sopra: neutro, riusabile,
"il mondo è tuo". Non cambia. Ma esiste anche un **server faro**
(`community-server/`): un server roleplay *concreto e condiviso*, costruito in
pubblico, a cui chiunque può collaborare — codice, costruzioni, modelli. Lì la
filosofia è diversa e dichiarata: non "è una base da sviluppare per conto tuo",
ma "**aiuta a sviluppare questo server, insieme**".

Le due cose non si contraddicono: il server faro *usa* il framework esattamente
come farebbe un server esterno, ed è la dimostrazione vivente che le fondamenta
funzionano. La differenza sta in chi possiede le decisioni:

- nel **framework**, le scelte editoriali non si prendono qui (vincono config e
  adapter);
- nel **server faro**, le scelte editoriali si prendono *insieme*, e il mondo è
  di tutti.

Il principio che rende possibile collaborare a un mondo unico senza grief è uno
solo: **nessuno modifica il mondo di produzione a mano; tutto entra in gioco solo
tramite pull request approvata**. Vedi
[`community-server/FILOSOFIA-SERVER.md`](community-server/FILOSOFIA-SERVER.md).

## In una riga

Open Roleplay ti dà le fondamenta giuste per un server roleplay serio: usale per
costruire il *tuo* mondo (il framework è tuo), oppure aiutaci a costruire quello
*condiviso* (il server faro, insieme). Le fondamenta sono il prodotto; il mondo è
tuo — o di tutti, se scegli il server faro.
