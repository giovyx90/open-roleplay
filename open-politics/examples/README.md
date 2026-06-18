# Open Politics — configurazioni di riferimento

Lo stesso plugin, tre ambientazioni completamente diverse. Open Politics non sa cosa sia un
Sindaco o un Re: tutto vive nella config. Copia il contenuto di una cartella in
`plugins/OpenPolitics/` e ricarica con `/openpolitics reload`.

| Cartella | Governo | Meccanismo dominante |
|---|---|---|
| _(default nel JAR)_ | Comune di San Valdino, Sindaco + Consiglio | Elezione democratica |
| [`fantasy/`](fantasy/) | Regno, Re + Consiglio dei Lord + Gran Cancelliere | Conquista, nomina reale, ereditarietà |
| [`oligarchia/`](oligarchia/) | Consiglio dei Cinque, cariche paritarie | Nomina interna, mandato illimitato |

Ogni cartella contiene `config.yml`, `governments.yml`, `charges.yml`, `act_types.yml` e
`law_categories.yml`. Il default bundlato nel JAR è la configurazione "realistico italiano": un
Comune con elezioni ogni 30 giorni, descritta nel README del modulo.

> Il plugin certifica, non governa. Nessuna di queste configurazioni esegue le conseguenze di una
> legge o di una carica — le vivono i giocatori.
