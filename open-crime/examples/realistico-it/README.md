# Esempio: realistico italiano

I **default del plugin** riproducono già questa ambientazione:

- **Beni:** cocaina, eroina, marijuana, metanfetamina, documenti falsi
- **Gerarchia:** Picciotto → Soldato → Caporegime → Consigliere → Underboss → Boss
- **Produzione:** piantagioni (campo), laboratori chimici, stamperie
- **Riciclaggio:** attività in contanti, azienda fantasma, scommesse clandestine,
  compravendita immobili
- **Estorsione:** pizzo settimanale con escalation a tre livelli

Quindi per il realistico-it **non serve copiare nulla**: installa il plugin e parti.

## Override per un setup più severo

`config.yml` in questa cartella alza l'asticella verso un server realistico maturo:

- `territory.require_worldguard: true` — territorio e produzione richiedono regioni
  WorldGuard reali con i tag `production_field` / `production_lab` / `production_printshop`
- `laundering.requires_bank_adapter: true` — il riciclaggio richiede un bridge bancario
  reale (Open Bank), non il ledger interno
- `discovery` — raggio denuncia 15, finestra evento 180 min, finestra arresto 480 min

Per richiedere un vero equipaggio di fondazione, in `syndicate.yml` imposta
`founding.min_members: 3`: `/syndicate fonda` conterà il fondatore più i giocatori liberi
fisicamente vicini.
