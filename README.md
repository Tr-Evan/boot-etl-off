# etl-off

ETL + API REST autour des données Open Food Facts (Spring Boot, Java 21).

## Lancer

```bash
mvn spring-boot:run
```

L'ingestion se lance au démarrage. API sur http://localhost:8080.

## Avancement par rapport au TP

| Objectif | État |
|---|---|
| Obj.1 — Conception (diagramme de classes + MLD) | ✅ dossier `conception/` |
| Obj.2 — Entités JPA + couche service + DAO + Javadoc | ✅ |
| Obj.3 — Cache + Virtual Threads + battre 3 min 45 s | ✅ **~4 s** |
| Obj.3 — Analyse perfs (temps, threads, mémoire) | ✅ log `Perf —` + Prometheus |
| Obj.4 — Les 6 endpoints REST | ✅ |
| Nettoyage (parenthèses, %, caractères parasites, séparateurs) | ✅ |
| Bonus — Actuator + Prometheus | ✅ |

## Reste à faire / améliorable

- Pas encore de tests d'intégration (seulement des tests unitaires sur le nettoyage).
- Versionnage git à mettre en place.
- Variante « JDBC pur » possible pour aller encore plus vite (non retenue : hors cadre JPA).
