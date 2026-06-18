# Esempio — Fantasy medievale

Un **Regno** sullo stesso codice del Comune realistico. Mostra tutti e quattro i meccanismi di
assegnazione delle cariche nello stesso governo.

- **Re** — per **conquista**: appartiene a chi controlla la region WorldGuard `sala_del_trono`
  (serve un `RegionAdapter` reale; senza, la carica resta vacante).
- **Consiglio dei Lord** — per **nomina** reale, organo **collegiale** (5 seggi, quorum 60%) che vota
  i decreti.
- **Gran Cancelliere** — per **nomina** reale.
- **Comandante della Guardia** — per **ereditarietà**: designa il successore con
  `/politica successore <player>`.

Atti: Editto Reale (il Re fa legge da solo), Decreto del Consiglio (richiede voto dei Lord, vetoabile
dal Re), Proclama, Lettera Patente, Sentenza Reale.

Copia i cinque file in `plugins/OpenPolitics/` e lancia `/openpolitics reload`.
