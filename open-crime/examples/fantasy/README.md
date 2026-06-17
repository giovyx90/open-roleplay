# Esempio: fantasy

Una gilda delle ombre invece di un cartello. **Stesso plugin, stesso codice** — cambia solo
la config.

- **Beni proibiti:** veleno di vipera nera, pozioni proibite, artefatti interdetti, tomi di
  magia nera
- **Gerarchia:** Adepto → Assassino → Maestro delle Ombre → Gran Maestro
- **Produzione:** giardini di erbe proibite, laboratori alchemici, scriptoria
- **Riciclaggio:** mercante ambulante (perdita 35%), asta clandestina (25%), gilda
  mercantile di facciata (18%, richiede un CompanyAdapter)
- **Tributo:** pizzo settimanale con escalation Minaccia → Maledizione → Visita notturna

L'`informant_protection` è attivo: l'identità di chi tradisce la gilda resta fuori dal
database.

## Uso

Copia i file di questa cartella in `plugins/OpenCrime/`, poi `/opencrime reload`. Senza un
backend di regioni il territorio e la produzione usano le regioni sintetiche per-chunk; il
racket resta inerte finché non registri un CompanyAdapter.
