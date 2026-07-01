package com.etloff.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link StringCleaner} against the exact cleaning examples given in the TP
 * ("Cas 1/2/3") plus the additional real-world cases of this dataset.
 */
class StringCleanerTest {

    @Test
    @DisplayName("Cas 1 — suppression des caractères parasites (* et _), casse préservée")
    void shouldRemoveParasiticCharacters() {
        List<String> result = List.copyOf(StringCleaner.parseIngredients("Sucre*, farine, _Maïs_"));
        assertThat(result).containsExactly("Sucre", "farine", "Maïs");
    }

    @Test
    @DisplayName("Cas 2 — suppression des pourcentages")
    void shouldRemovePercentages() {
        List<String> result = List.copyOf(StringCleaner.parseIngredients("Sucre 15%, farine 50%, Maïs 35%"));
        assertThat(result).containsExactly("Sucre", "farine", "Maïs");
    }

    @Test
    @DisplayName("Cas 3 — suppression du contenu entre parenthèses")
    void shouldRemoveParenthesesContent() {
        List<String> result = List.copyOf(
                StringCleaner.parseIngredients("Sucre, banane, Pâte (Farine 50%, Sucre 20%, Œufs 30%)"));
        assertThat(result).containsExactly("Sucre", "banane", "Pâte");
    }

    @Test
    @DisplayName("Séparateur alternatif : le tiret espacé")
    void shouldSplitOnSpacedDashes() {
        List<String> result = List.copyOf(StringCleaner.parseIngredients("Sucre - farine - banane"));
        assertThat(result).containsExactly("Sucre", "farine", "banane");
    }

    @Test
    @DisplayName("Les mots composés (agar-agar) ne sont pas coupés")
    void shouldPreserveCompoundWords() {
        List<String> result = List.copyOf(StringCleaner.parseIngredients("agar-agar, sel"));
        assertThat(result).containsExactly("agar-agar", "sel");
    }

    @Test
    @DisplayName("Les additifs conservent leur code E et leur libellé complet")
    void shouldKeepAdditiveLabels() {
        List<String> result = List.copyOf(
                StringCleaner.parseAdditives("E500 - Carbonates de sodium,E330 - Acide citrique"));
        assertThat(result).containsExactly("E500 - Carbonates de sodium", "E330 - Acide citrique");
    }

    @Test
    @DisplayName("Les allergènes perdent leur préfixe de langue (en:)")
    void shouldStripLanguagePrefixFromAllergens() {
        List<String> result = List.copyOf(
                StringCleaner.parseAllergens("en:sulphur-dioxide-and-sulphites, lait"));
        assertThat(result).containsExactly("sulphur-dioxide-and-sulphites", "lait");
    }

    @Test
    @DisplayName("Entrée vide ou nulle => ensemble vide")
    void shouldHandleBlankInput() {
        assertThat(StringCleaner.parseIngredients(null)).isEmpty();
        assertThat(StringCleaner.parseIngredients("")).isEmpty();
        assertThat(StringCleaner.parseAdditives("   ")).isEmpty();
    }
}
