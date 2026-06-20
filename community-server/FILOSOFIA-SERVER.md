# Filosofia del server faro

Open Roleplay nasce come **framework**: fondamenta neutre su cui ognuno
costruisce il *suo* server (vedi [`../FILOSOFIA.md`](../FILOSOFIA.md)). Quella
scelta resta valida e non cambia.

Accanto al framework esiste pero' una cosa diversa: un **server faro**, concreto
e condiviso, sviluppato in pubblico. Non e' "una base da sviluppare per conto
tuo": e' **questo** server, e l'invito e' "aiuta a svilupparlo insieme".

## Due cose diverse, nello stesso repo

- **Il framework (`open-*`)** — neutro rispetto all'ambientazione, riusabile,
  "il mondo e' tuo". Lo migliori senza decidere cose che spettano al singolo
  server.
- **Il server faro (`community-server/`)** — un server preciso, con il suo
  mondo, i suoi modelli, la sua ambientazione. Qui le decisioni editoriali si
  prendono *insieme*, e il mondo e' di tutti.

Il server faro **usa** il framework, esattamente come farebbe un server esterno.
E' la dimostrazione vivente che la base funziona, e il luogo dove la community
collabora a un mondo unico.

## I principi del server condiviso

1. **Anti-grief per costruzione.** Il mondo di produzione non si tocca a mano.
   Ogni modifica al mondo entra solo tramite PR approvata e deploy automatico.
2. **Tutto revisionabile.** Una costruzione e' uno schematic + un manifest di
   testo; un modello e' JSON. Si leggono, si discutono, si approvano come
   codice.
3. **Conflitti meccanici, non litigi.** Due costruzioni non possono occupare lo
   stesso volume: la sovrapposizione e' un "conflitto di merge" che la CI rileva
   da sola. Si risolve spostando o coordinando, non discutendo chi e' arrivato
   prima.
4. **Open davvero.** Formati aperti (YAML, JSON, schematic), licenze esplicite
   sugli asset, storia pubblica delle decisioni nei commit e nelle PR.

## In una riga

Il framework e' la base che ognuno puo' riusare; il server faro e' il mondo che
costruiamo **insieme**, in chiaro, una PR alla volta.
