# Conception — etl-off

Dossier de conception (Objectif 1 du TP).

- [`diagramme-classes.md`](diagramme-classes.md) — **diagramme de classes métier** (UML, Mermaid).
- [`mld.md`](mld.md) — **modèle logique / physique de données** (MLD, diagramme entité-association + description des tables).

Les diagrammes sont écrits en **Mermaid** : ils se rendent automatiquement sur
GitHub/GitLab, dans l'aperçu Markdown d'IntelliJ/VS Code, ou sur
<https://mermaid.live>.

## Choix de conception principaux

| Décision | Justification |
|---|---|
| 5 entités de référence **uniques** (Category, Brand, Ingredient, Allergen, Additive) | Règles de gestion du TP + déduplication → contrainte `UNIQUE` sur `name` |
| `Product` ▸ `@ManyToOne` Category / Brand | Un produit a exactement une catégorie et une marque |
| `Product` ▸ `@ManyToMany` Ingredient / Allergen / Additive | Un produit a plusieurs ingrédients/allergènes/additifs, partagés entre produits |
| `Nutriments` en `@Embedded` | Regroupe les ~22 colonnes nutritionnelles sans table supplémentaire |
| Toutes les associations `LAZY`, sans cascade | Performance : les références sont persistées une seule fois par l'ETL, jamais en cascade |
| Clés primaires techniques (`SEQUENCE`) | Les séquences autorisent le batching JDBC (contrairement à `IDENTITY`) |
