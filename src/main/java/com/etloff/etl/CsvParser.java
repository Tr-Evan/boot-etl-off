package com.etloff.etl;

import com.etloff.model.Nutriments;
import com.etloff.util.StringCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the pipe-delimited Open Food Facts CSV and turns each line into a fully
 * cleaned {@link ProductRow}.
 *
 * <p>The file is pipe-delimited with no quoting (values contain commas but never the
 * {@code '|'} separator), so a plain {@code split('|')} per line is both correct and
 * fast. All expensive string cleaning happens here, once, so the downstream parallel
 * write phase is pure database I/O.</p>
 */
@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);

    private static final char DELIMITER = '|';

    // Column indices in the source file (30 columns + a trailing empty field).
    private static final int COL_CATEGORIE = 0;
    private static final int COL_MARQUE = 1;
    private static final int COL_NOM = 2;
    private static final int COL_GRADE = 3;
    private static final int COL_INGREDIENTS = 4;
    private static final int COL_ENERGIE = 5;
    private static final int COL_GRAISSE = 6;
    private static final int COL_SUCRES = 7;
    private static final int COL_FIBRES = 8;
    private static final int COL_PROTEINES = 9;
    private static final int COL_SEL = 10;
    private static final int COL_VIT_A = 11;
    private static final int COL_VIT_D = 12;
    private static final int COL_VIT_E = 13;
    private static final int COL_VIT_K = 14;
    private static final int COL_VIT_C = 15;
    private static final int COL_VIT_B1 = 16;
    private static final int COL_VIT_B2 = 17;
    private static final int COL_VIT_PP = 18;
    private static final int COL_VIT_B6 = 19;
    private static final int COL_VIT_B9 = 20;
    private static final int COL_VIT_B12 = 21;
    private static final int COL_CALCIUM = 22;
    private static final int COL_MAGNESIUM = 23;
    private static final int COL_IRON = 24;
    private static final int COL_FER = 25;
    private static final int COL_BETA_CAROTENE = 26;
    private static final int COL_HUILE_PALME = 27;
    private static final int COL_ALLERGENES = 28;
    private static final int COL_ADDITIFS = 29;

    private static final int MIN_COLUMNS = 30;

    /**
     * Parses the whole file into a list of cleaned rows, skipping the header and any
     * malformed line (too few columns or a blank product name).
     *
     * @param csvPath path to the CSV file
     * @return the parsed rows, in file order
     * @throws UncheckedIOException if the file cannot be read
     */
    public List<ProductRow> parse(Path csvPath) {
        List<ProductRow> rows = new ArrayList<>(16_384);
        int skipped = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                ProductRow row = parseLine(line);
                if (row == null) {
                    skipped++;
                } else {
                    rows.add(row);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read CSV file: " + csvPath, e);
        }

        log.info("Parsed {} rows from {} ({} lines skipped)", rows.size(), csvPath, skipped);
        return rows;
    }

    /**
     * Parses a single physical line into a {@link ProductRow}, or {@code null} if the
     * line is unusable.
     */
    private ProductRow parseLine(String line) {
        // -1 keeps trailing empty fields so column indices stay aligned.
        String[] cols = splitPipe(line);
        if (cols.length < MIN_COLUMNS) {
            return null;
        }

        String nom = StringCleaner.normalizeName(cols[COL_NOM]);
        if (nom == null) {
            return null; // a product must have a name
        }

        Nutriments nutriments = new Nutriments();
        nutriments.setEnergie100g(parseDouble(cols[COL_ENERGIE]));
        nutriments.setGraisse100g(parseDouble(cols[COL_GRAISSE]));
        nutriments.setSucres100g(parseDouble(cols[COL_SUCRES]));
        nutriments.setFibres100g(parseDouble(cols[COL_FIBRES]));
        nutriments.setProteines100g(parseDouble(cols[COL_PROTEINES]));
        nutriments.setSel100g(parseDouble(cols[COL_SEL]));
        nutriments.setVitA100g(parseDouble(cols[COL_VIT_A]));
        nutriments.setVitD100g(parseDouble(cols[COL_VIT_D]));
        nutriments.setVitE100g(parseDouble(cols[COL_VIT_E]));
        nutriments.setVitK100g(parseDouble(cols[COL_VIT_K]));
        nutriments.setVitC100g(parseDouble(cols[COL_VIT_C]));
        nutriments.setVitB1100g(parseDouble(cols[COL_VIT_B1]));
        nutriments.setVitB2100g(parseDouble(cols[COL_VIT_B2]));
        nutriments.setVitPP100g(parseDouble(cols[COL_VIT_PP]));
        nutriments.setVitB6100g(parseDouble(cols[COL_VIT_B6]));
        nutriments.setVitB9100g(parseDouble(cols[COL_VIT_B9]));
        nutriments.setVitB12100g(parseDouble(cols[COL_VIT_B12]));
        nutriments.setCalcium100g(parseDouble(cols[COL_CALCIUM]));
        nutriments.setMagnesium100g(parseDouble(cols[COL_MAGNESIUM]));
        nutriments.setIron100g(parseDouble(cols[COL_IRON]));
        nutriments.setFer100g(parseDouble(cols[COL_FER]));
        nutriments.setBetaCarotene100g(parseDouble(cols[COL_BETA_CAROTENE]));

        return new ProductRow(
                nom,
                normalizeGrade(cols[COL_GRADE]),
                parseInteger(cols[COL_HUILE_PALME]),
                StringCleaner.normalizeName(cols[COL_CATEGORIE]),
                StringCleaner.normalizeName(cols[COL_MARQUE]),
                nutriments,
                StringCleaner.parseIngredients(cols[COL_INGREDIENTS]),
                StringCleaner.parseAllergens(cols[COL_ALLERGENES]),
                StringCleaner.parseAdditives(cols[COL_ADDITIFS]));
    }

    /**
     * Splits a line on the pipe delimiter, preserving trailing empty fields.
     * Hand-rolled (rather than {@link String#split}) to avoid regex overhead on
     * every one of the 13k+ lines.
     */
    private static String[] splitPipe(String line) {
        List<String> parts = new ArrayList<>(32);
        int start = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == DELIMITER) {
                parts.add(line.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(line.substring(start));
        return parts.toArray(new String[0]);
    }

    /** Normalizes the Nutri-Score grade to a single lower-case letter, or {@code null}. */
    private static String normalizeGrade(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String grade = raw.trim().toLowerCase();
        return grade.length() == 1 ? grade : null;
    }

    /** Parses a nullable decimal, tolerating comma decimal separators. */
    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses a nullable integer flag. */
    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
