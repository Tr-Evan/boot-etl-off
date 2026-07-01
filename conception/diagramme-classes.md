# Diagramme de classes métier — etl-off

```mermaid
classDiagram
    direction LR

    class Product {
        +Long id
        +String nom
        +String nutritionGradeFr
        +Integer presenceHuilePalme
    }

    class Nutriments {
        <<Embeddable>>
        +Double energie100g
        +Double graisse100g
        +Double sucres100g
        +Double fibres100g
        +Double proteines100g
        +Double sel100g
        +Double vitA100g..vitB12100g
        +Double calcium100g
        +Double magnesium100g
        +Double iron100g
        +Double fer100g
        +Double betaCarotene100g
    }

    class Category {
        +Long id
        +String name «unique»
    }

    class Brand {
        +Long id
        +String name «unique»
    }

    class Ingredient {
        +Long id
        +String name «unique»
    }

    class Allergen {
        +Long id
        +String name «unique»
    }

    class Additive {
        +Long id
        +String name «unique»
    }

    Product "1" *-- "1" Nutriments : embedded

    Product "*" --> "0..1" Category : category
    Product "*" --> "0..1" Brand : brand

    Product "*" --> "*" Ingredient : ingredients
    Product "*" --> "*" Allergen : allergens
    Product "*" --> "*" Additive : additives
```

## Lecture

- **`Product`** est l'entité centrale. Elle porte ses attributs propres (`nom`,
  `nutritionGradeFr` = score nutritionnel A→E, `presenceHuilePalme`) et **compose** un
  objet valeur **`Nutriments`** (les valeurs pour 100 g).
- **`Category`** et **`Brand`** : associations `*..0..1` (plusieurs produits pour une
  même catégorie/marque ; un produit peut ne pas en avoir).
- **`Ingredient`**, **`Allergen`**, **`Additive`** : associations `*..*`. Chaque référence
  est **unique** en base et partagée par tous les produits qui la contiennent.
- Toutes les entités de référence partagent le même contrat (`id`, `name` unique,
  `equals/hashCode` sur `name`), factorisé dans une classe abstraite `AbstractReferenceEntity`.
