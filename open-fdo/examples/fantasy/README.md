# Configurazione di esempio — medievale fantasy

Guardia Cittadina + Ordine dei Cavalieri, nessuna economia, reati come "Stregoneria
proibita", detenzione nelle segrete. Dimostra il test di neutralita': lo stesso core del
realistico italiano serve un'ambientazione completamente diversa cambiando **solo** la
config.

Per usarla, copia questi `*.yml` in `plugins/OpenFDO/` (sovrascrivendo i default) e ricarica
con `/fdo reload`.

- Nessun corpo possiede `ECONOMIC_AUDIT` e nessun `EconomyAuditAdapter` e' installato →
  l'atto `audit` non esiste in questo mondo.
- La detenzione "segrete" e' un `DetentionAdapter` separato (region + bendaggio); senza, la
  condanna resta nel fascicolo.
- L'id fascicolo usa il pattern `{sigla_corpo}-{anno}-{numero}` (es. `GC-2026-1`) per mostrare
  che anche il formato e' config.
