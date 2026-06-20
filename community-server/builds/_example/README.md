# Costruzione di esempio

Questa cartella mostra il formato di una costruzione del server faro. Le cartelle
che iniziano con `_` sono trattate come **esempi/template** dalla CI: il manifest
viene validato, ma il file `region.schem` non e' richiesto.

## Anatomia di una costruzione reale

```
community-server/builds/<slug>/
  region.schem   # schematic WorldEdit/FAWE (Sponge v3), versionato con Git LFS
  build.yml      # manifest: anchor, size, rotation, licenza, ...
  preview.png    # screenshot opzionale per la review (LFS)
```

## Come si crea (in breve)

1. Costruisci sul server di staging (creative).
2. Seleziona l'area con WorldEdit e salva lo schematic:
   `//copy` poi `//schem save region.schem`
   (oppure, con il plugin del server, `/orp-build export <slug>`).
3. Copia questa cartella in `builds/<slug>/`, compila `build.yml`.
4. Aggiungi il claim in `community-server/registry/claims.yml`.
5. Apri una PR verso `dev`.

La guida completa: [`../../CONTRIBUTING-BUILDS.md`](../../CONTRIBUTING-BUILDS.md).
