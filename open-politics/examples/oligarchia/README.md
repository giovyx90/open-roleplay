# Esempio — Oligarchia

Un **Consiglio dei Cinque**: cinque cariche paritarie, nessuna superiore alle altre, mandato
illimitato. Niente Sindaco, niente Re, niente elezioni popolari.

- Una sola carica, **Consigliere**, con `max_holders: 5` — è quindi un organo collegiale.
- I cinque **cooptano** i nuovi membri per nomina interna: chiunque sia già Consigliere ha `APPOINT`.
- Ogni **Delibera** richiede il voto collegiale: quorum `1.0` (tutti partecipano), maggioranza `0.5`
  → servono **3 voti su 5**. La delibera approvata diventa un Editto del Consiglio.
- Un'unica categoria di legge.

Mostra che "una carica" e "un governo" sono primitivi: cinque persone con la stessa carica e un voto
interno bastano a fare uno stato. Copia i cinque file in `plugins/OpenPolitics/` e lancia
`/openpolitics reload`.
