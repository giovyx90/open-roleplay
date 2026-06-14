# Open Core API

`open-core-api` contiene i contratti pubblici minimi condivisi dai plugin Open
Roleplay.

Il modulo non avvia un plugin Paper e non contiene logica di gameplay. Serve a
evitare dipendenze dirette dal vecchio core privato e offre tipi stabili per:

- lifecycle dei moduli;
- accesso opzionale al database condiviso;
- messaggi e permessi comuni;
- stato temporaneo HUD;
- utility base per item Bukkit.

## Uso nei moduli

Un plugin che vuole integrarsi con Open Core dovrebbe dichiarare questa
dipendenza come `provided` e recuperare il servizio con Bukkit Services:

```java
RegisteredServiceProvider<OpenRoleplayCore> provider =
        Bukkit.getServicesManager().getRegistration(OpenRoleplayCore.class);
```

I moduli devono restare difensivi: Open Core puo' non essere installato o il
database puo' essere disabilitato.
