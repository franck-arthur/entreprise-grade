package com.enterprise.app.application.dto.csvexport;

import java.util.HashMap;
import java.util.Map;

/**
 * Record représentant une ligne lue depuis un fichier Excel.
 * Préserve le numéro de ligne Excel pour maintenir l'ordre des colonnes.
 * Utilise les Records Java 14+ pour une approche plus moderne et immutable.
 */
public record ExcelRowDTO(
    String nomCsv,

    /**
     * Numéro de ligne Excel (2, 3, 4...) pour préserver l'ordre.
     * La ligne 1 (header) est ignorée.
     */
    Integer excelRowNumber,

    String codeVariable,
    String libelle,
    String format,
    String typeQuestion,
    String regleGestion,

    /**
     * Map des valeurs possibles : position → valeur.
     * Correspond aux colonnes valeur_possible_1, valeur_possible_2, etc. dans Excel.
     */
    Map<Integer, String> valeursPossibles
) {

    /**
     * Constructeur compact avec validation et initialisation par défaut.
     */
    public ExcelRowDTO {
        valeursPossibles = valeursPossibles != null ? Map.copyOf(valeursPossibles) : Map.of();
    }

    /**
     * Factory method pour créer un ExcelRowDTO avec valeurs par défaut.
     */
    public static ExcelRowDTO of(String nomCsv, Integer excelRowNumber, String codeVariable) {
        return new ExcelRowDTO(
            nomCsv,
            excelRowNumber,
            codeVariable,
            null,
            null,
            null,
            null,
            new HashMap<>()
        );
    }

    /**
     * Factory method pour créer un ExcelRowDTO complet.
     */
    public static ExcelRowDTO create(String nomCsv, Integer excelRowNumber, String codeVariable,
                                   String libelle, String format, String typeQuestion,
                                   String regleGestion, Map<Integer, String> valeursPossibles) {
        return new ExcelRowDTO(
            nomCsv,
            excelRowNumber,
            codeVariable,
            libelle,
            format,
            typeQuestion,
            regleGestion,
            valeursPossibles != null ? valeursPossibles : new HashMap<>()
        );
    }

    /**
     * Crée une nouvelle instance avec une valeur possible ajoutée.
     * Approche immutable : retourne un nouvel objet.
     */
    public ExcelRowDTO withValeurPossible(Integer position, String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return this;
        }

        var newValeurs = new HashMap<>(this.valeursPossibles);
        newValeurs.put(position, valeur.trim());

        return new ExcelRowDTO(
            nomCsv,
            excelRowNumber,
            codeVariable,
            libelle,
            format,
            typeQuestion,
            regleGestion,
            newValeurs
        );
    }

    /**
     * Vérifie si cette ligne contient des valeurs possibles.
     */
    public boolean hasValeursPossibles() {
        return valeursPossibles != null && !valeursPossibles.isEmpty();
    }

    /**
     * Retourne le nombre de valeurs possibles.
     */
    public int getValeursPossiblesCount() {
        return valeursPossibles.size();
    }

    /**
     * Vérifie si tous les champs obligatoires sont présents.
     * Utilise pattern matching pour la validation.
     */
    public boolean isValid() {
        return switch (codeVariable) {
            case null -> false;
            case String code when code.trim().isEmpty() -> false;
            default -> excelRowNumber != null && excelRowNumber >= 1; // Row 0 = header, Row 1+ = data
        };
    }

    /**
     * Retourne une représentation condensée pour les logs.
     * Utilise les text blocks Java 15+ si nécessaire.
     */
    public String toLogString() {
        return "[%s] ligne %d: %s (%d valeurs possibles)".formatted(
            nomCsv, excelRowNumber, codeVariable, valeursPossibles.size());
    }
}