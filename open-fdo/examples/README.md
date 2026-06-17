# Open FDO — esempi di configurazione

OpenFDO non conosce ambientazioni: tutto cio' che le definisce vive nei file YAML. Qui
trovi due punti di partenza completi.

- [`realistico-it/`](realistico-it/) — realistico italiano (Polizia di Stato, Carabinieri
  con giurisdizione, Guardia di Finanza con audit, Magistratura). E' anche la configurazione
  **di default** del plugin: parti da qui se vuoi un realistico.
- [`fantasy/`](fantasy/) — medievale fantasy (Guardia Cittadina + Ordine dei Cavalieri,
  nessuna economia, reati come "Stregoneria proibita", detenzione nelle segrete). Mostra che
  lo stesso identico core serve un'ambientazione completamente diversa senza toccare il codice.

Per usare un esempio, copia i suoi `*.yml` nella cartella dati del plugin
(`plugins/OpenFDO/`) e ricarica con `/fdo reload`.
