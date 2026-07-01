package com.etloff.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stateless, thread-safe helper that turns the raw, noisy free-text columns of the
 * Open Food Facts CSV into clean, deduplicated tokens ready to become reference
 * entities.
 *
 * <p>All regular expressions are precompiled as {@code static final} {@link Pattern}s
 * so the hot ETL loop pays the compilation cost exactly once, not once per row.</p>
 *
 * <h2>Cleaning pipeline (ingredients / allergens)</h2>
 * <ol>
 *     <li><b>Strip parenthesised content</b> — including the parentheses themselves,
 *         handling nesting and stray/unbalanced brackets. Done <em>before</em> splitting
 *         so that commas inside parentheses (e.g. {@code "Pâte (Farine 50%, Sucre 20%)"})
 *         cannot break the list apart.</li>
 *     <li><b>Split</b> on the real list separators of this dataset — commas, semicolons
 *         and newlines.</li>
 *     <li><b>Per token</b>: drop percentages ({@code "15%"}, {@code "99 % minimum"}),
 *         strip parasitic characters ({@code * _ : . ( )} …), drop standalone numbers and
 *         collapse whitespace. The <em>original casing is preserved</em>, exactly matching
 *         the TP examples ({@code "Sucre*, farine, _Maïs_"} → {@code "Sucre, farine, Maïs"}).</li>
 * </ol>
 *
 * <h2>Additives</h2>
 * Additives are handled separately: they are split on commas only and kept verbatim
 * (just whitespace-normalized), because the leading E-code and its label
 * (e.g. {@code "E500 - Carbonates de sodium"}) form the stable business key and must
 * not be lower-cased or dash-split.
 */
public final class StringCleaner {

    /** A balanced {@code (...)} group with no nested parentheses — removed iteratively. */
    private static final Pattern BALANCED_PARENTHESES = Pattern.compile("\\([^()]*\\)");

    /** Any leftover stray parenthesis after balanced groups are gone. */
    private static final Pattern STRAY_PARENTHESES = Pattern.compile("[()\\[\\]{}]");

    /** A percentage: an integer/decimal number optionally spaced before a {@code %}. */
    private static final Pattern PERCENTAGE = Pattern.compile("\\d+(?:[.,]\\d+)?\\s*%");

    /** A leading two-letter language prefix on allergens, e.g. {@code "en:"} / {@code "fr:"}. */
    private static final Pattern LANG_PREFIX = Pattern.compile("^[A-Za-z]{2}:");

    /** Characters that are not letters, digits, spaces, apostrophes or dashes. */
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s'\\-]");

    /** A standalone number (not glued to a letter, so codes like {@code "B3"} survive). */
    private static final Pattern STANDALONE_NUMBER = Pattern.compile("(?<![\\p{L}])\\d+(?:[.,]\\d+)?(?![\\p{L}])");

    /** Runs of whitespace. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * List separators used for ingredients and allergens: commas, semicolons, newlines,
     * <em>and</em> spaced dashes ({@code " - "}). The dash alternative requires surrounding
     * whitespace so that compound words such as {@code "agar-agar"} are preserved.
     */
    private static final Pattern LIST_SEPARATORS = Pattern.compile("[,;\\n\\r]+|\\s+[-–]\\s+");

    /** Separator used for the additive column (commas / newlines only). */
    private static final Pattern ADDITIVE_SEPARATORS = Pattern.compile("[,\\n\\r]+");

    /** Leading/trailing apostrophes or dashes left after cleaning. */
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^['\\-\\s]+|['\\-\\s]+$");

    private static final int MIN_TOKEN_LENGTH = 2;

    /**
     * Upper bound on a token's length. Anything longer is an un-splittable free-text blob
     * (multi-language descriptions, storage instructions, …), not a real ingredient/label,
     * so it is discarded. Also keeps every value within the {@code name} column width.
     */
    private static final int MAX_TOKEN_LENGTH = 255;

    private StringCleaner() {
        // Utility class — not instantiable.
    }

    /**
     * Normalizes a simple single-valued field such as a category or brand name:
     * trims, collapses internal whitespace and preserves the original casing
     * (these are proper nouns). Returns {@code null} for blank input.
     *
     * @param raw the raw cell value (may be {@code null})
     * @return the cleaned name, or {@code null} if empty
     */
    public static String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = WHITESPACE.matcher(raw.trim()).replaceAll(" ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Parses the free-text {@code ingredients} column into a set of clean ingredient names.
     *
     * @param raw the raw ingredients cell (may be {@code null})
     * @return an ordered, deduplicated set of cleaned ingredient names (never {@code null})
     */
    public static Set<String> parseIngredients(String raw) {
        return parseWordList(raw);
    }

    /**
     * Parses the {@code allergenes} column into a set of clean allergen names,
     * additionally stripping language prefixes such as {@code "en:"}.
     *
     * @param raw the raw allergens cell (may be {@code null})
     * @return an ordered, deduplicated set of cleaned allergen names (never {@code null})
     */
    public static Set<String> parseAllergens(String raw) {
        if (isBlank(raw)) {
            return new LinkedHashSet<>();
        }
        String withoutParens = stripParentheses(raw);
        Set<String> result = new LinkedHashSet<>();
        for (String rawToken : LIST_SEPARATORS.split(withoutParens)) {
            // Strip a leading language prefix (e.g. "en:") without altering the casing.
            String token = LANG_PREFIX.matcher(rawToken.trim()).replaceFirst("");
            String cleaned = cleanWord(token);
            if (cleaned != null) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Parses the {@code additifs} column, keeping each additive's full label
     * (E-code + name) verbatim aside from whitespace normalization.
     *
     * @param raw the raw additives cell (may be {@code null})
     * @return an ordered, deduplicated set of additive labels (never {@code null})
     */
    public static Set<String> parseAdditives(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (isBlank(raw)) {
            return result;
        }
        for (String rawToken : ADDITIVE_SEPARATORS.split(raw)) {
            String cleaned = WHITESPACE.matcher(rawToken.trim()).replaceAll(" ");
            // Drop a trailing sentence period but keep internal punctuation / the E-code.
            if (cleaned.endsWith(".")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
            }
            if (cleaned.length() >= MIN_TOKEN_LENGTH && cleaned.length() <= MAX_TOKEN_LENGTH) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Shared word-list pipeline for ingredients and allergens.
     */
    private static Set<String> parseWordList(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (isBlank(raw)) {
            return result;
        }
        String withoutParens = stripParentheses(raw);
        for (String rawToken : LIST_SEPARATORS.split(withoutParens)) {
            String cleaned = cleanWord(rawToken);
            if (cleaned != null) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Cleans a single token and returns its canonical form, or {@code null} when the
     * token carries no usable content (empty, too short, or purely numeric/punctuation).
     *
     * @param rawToken the raw token (already split from a list)
     * @return the canonical token, or {@code null} to discard it
     */
    public static String cleanWord(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        String token = rawToken;
        token = PERCENTAGE.matcher(token).replaceAll(" ");
        token = STRAY_PARENTHESES.matcher(token).replaceAll(" ");
        token = NON_WORD.matcher(token).replaceAll(" ");          // drops * _ : . etc.
        token = STANDALONE_NUMBER.matcher(token).replaceAll(" ");
        token = WHITESPACE.matcher(token).replaceAll(" ").trim();
        token = EDGE_PUNCTUATION.matcher(token).replaceAll("");

        if (token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH || !hasLetter(token)) {
            return null;
        }
        // Original casing is preserved (see class Javadoc / TP examples).
        return token;
    }

    /**
     * Removes parenthesised content and any leftover stray brackets. Applied
     * repeatedly to unwind nested parentheses.
     */
    private static String stripParentheses(String input) {
        String current = input;
        String previous;
        do {
            previous = current;
            current = BALANCED_PARENTHESES.matcher(current).replaceAll(" ");
        } while (!current.equals(previous));
        return STRAY_PARENTHESES.matcher(current).replaceAll(" ");
    }

    private static boolean hasLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
