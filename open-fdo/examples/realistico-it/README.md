# Configurazione di riferimento — realistico italiano

Questa configurazione riproduce un realistico italiano: **Polizia di Stato** e **Guardia di
Finanza** (con `ECONOMIC_AUDIT`), **Arma dei Carabinieri** con giurisdizione sugli altri
corpi (indagini interne) e **Magistratura** come autorita' giudicante (`ISSUE_VERDICT`,
`ISSUE_WARRANT`). Custodia cautelare 48h, fermo 6h, catalogo reati basato sul codice penale,
livelli ricercato L1-L3.

E' gia' la **configurazione di default** del plugin: i file in
`open-fdo/src/main/resources/` (`config.yml`, `corps.yml`, `ranks.yml`, `acts.yml`,
`crimes.yml`, `wanted.yml`) implementano esattamente questo scenario e vengono generati al
primo avvio in `plugins/OpenFDO/`. Per partire da qui non devi copiare nulla: avvia il
plugin e modifica i file generati.

L'`audit` economico richiede un `EconomyAuditAdapter` (per esempio sopra Open Companies): se
non e' presente, la capability `ECONOMIC_AUDIT` e l'atto `audit` semplicemente non compaiono.
La detenzione richiede un `DetentionAdapter` (un modulo carcere): senza, la condanna resta
registrata nel fascicolo e l'esecuzione e' lasciata al RP manuale.
