package com.enterprise.app.domain.model.csvexport;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entité de staging pour l'import des définitions Excel.
 * Table volatile utilisée lors de l'import avant normalisation.
 */
@Entity
@Table(
    name = "excel_csv_definition_stg",
    schema = "export_csv",
    indexes = {
        @Index(name = "idx_excel_stg_row_order", columnList = "nom_csv, excel_row_number"),
        @Index(name = "idx_excel_stg_import_date", columnList = "date_import")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"nomCsv", "excelRowNumber", "codeVariable", "indexValeur"})
public class ExcelDefinitionStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_csv", nullable = false, length = 50)
    private String nomCsv;

    @Column(name = "excel_row_number", nullable = false)
    private Integer excelRowNumber;

    @Column(name = "code_variable", nullable = false, length = 100)
    private String codeVariable;

    @Column(name = "libelle", length = 255)
    private String libelle;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "type_question", length = 50)
    private String typeQuestion;

    @Column(name = "regle_gestion", length = 100)
    private String regleGestion;

    @Column(name = "index_valeur")
    private Integer indexValeur;

    @Column(name = "valeur_possible", length = 500)
    private String valeurPossible;

    @CreatedDate
    @Column(name = "date_import", nullable = false, updatable = false)
    private LocalDateTime dateImport;

    @Column(name = "fichier_source", length = 500)
    private String fichierSource;

    /**
     * Vérifie si cet enregistrement contient une valeur possible.
     */
    public boolean hasValeurPossible() {
        return valeurPossible != null && !valeurPossible.trim().isEmpty();
    }

    /**
     * Méthode utilitaire pour créer un enregistrement de base.
     */
    public static ExcelDefinitionStaging ofBase(String nomCsv, Integer excelRowNumber, String codeVariable) {
        return ExcelDefinitionStaging.builder()
                .nomCsv(nomCsv)
                .excelRowNumber(excelRowNumber)
                .codeVariable(codeVariable)
                .dateImport(LocalDateTime.now())
                .build();
    }

    /**
     * Méthode utilitaire pour créer un enregistrement avec valeur possible.
     */
    public static ExcelDefinitionStaging ofWithValue(String nomCsv, Integer excelRowNumber, String codeVariable,
                                                      Integer indexValeur, String valeurPossible) {
        return ExcelDefinitionStaging.builder()
                .nomCsv(nomCsv)
                .excelRowNumber(excelRowNumber)
                .codeVariable(codeVariable)
                .indexValeur(indexValeur)
                .valeurPossible(valeurPossible)
                .dateImport(LocalDateTime.now())
                .build();
    }
}