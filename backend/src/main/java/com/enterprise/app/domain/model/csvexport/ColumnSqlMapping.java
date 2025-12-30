package com.enterprise.app.domain.model.csvexport;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entité représentant le mapping technique SQL pour les colonnes CSV.
 * Gérée par les développeurs pour faire le lien entre les code_variable et la technique.
 */
@Entity
@Table(
    name = "column_sql_mapping",
    schema = "export_csv",
    indexes = {
        @Index(name = "idx_column_mapping_code", columnList = "code_variable")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"codeVariable"})
public class ColumnSqlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_variable", nullable = false, unique = true, length = 100)
    private String codeVariable;

    // Option 1 : Colonne simple
    @Column(name = "source_table", length = 100)
    private String sourceTable;

    @Column(name = "source_column", length = 100)
    private String sourceColumn;

    // Option 2 : Expression SQL complexe
    @Column(name = "sql_expression", columnDefinition = "TEXT")
    private String sqlExpression;

    // Métadonnées
    @Column(name = "join_tables", columnDefinition = "TEXT[]")
    private String[] joinTables;

    @Column(name = "requires_subquery", nullable = false)
    @Builder.Default
    private Boolean requiresSubquery = false;

    @Column(name = "is_aggregation", nullable = false)
    @Builder.Default
    private Boolean isAggregation = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "exemple_valeur", length = 255)
    private String exempleValeur;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    /**
     * Vérifie si ce mapping utilise une colonne simple (table + colonne).
     */
    public boolean isSimpleColumn() {
        return sourceTable != null && sourceColumn != null && sqlExpression == null;
    }

    /**
     * Vérifie si ce mapping utilise une expression SQL complexe.
     */
    public boolean isComplexExpression() {
        return sqlExpression != null && sourceTable == null && sourceColumn == null;
    }

    /**
     * Retourne l'alias SQL pour cette colonne.
     */
    public String getSqlAlias() {
        if (isSimpleColumn()) {
            String tableAlias = getTableAlias(sourceTable);
            return tableAlias + "." + sourceColumn + " AS " + codeVariable;
        } else if (isComplexExpression()) {
            return "(" + sqlExpression + ") AS " + codeVariable;
        } else {
            return "NULL AS " + codeVariable;
        }
    }

    /**
     * Génère l'alias de table standard (première lettre).
     */
    private String getTableAlias(String tableName) {
        return switch (tableName) {
            case "questionnaire" -> "q";
            case "personne" -> "p";
            case "adresse" -> "a";
            case "formation" -> "f";
            case "formation_participation" -> "fp";
            case "secteur" -> "s";
            case "region" -> "r";
            default -> tableName.substring(0, 1);
        };
    }

    /**
     * Méthode utilitaire pour créer un mapping simple.
     */
    public static ColumnSqlMapping ofSimple(String codeVariable, String sourceTable, String sourceColumn) {
        return ColumnSqlMapping.builder()
                .codeVariable(codeVariable)
                .sourceTable(sourceTable)
                .sourceColumn(sourceColumn)
                .build();
    }

    /**
     * Méthode utilitaire pour créer un mapping complexe.
     */
    public static ColumnSqlMapping ofExpression(String codeVariable, String sqlExpression) {
        return ColumnSqlMapping.builder()
                .codeVariable(codeVariable)
                .sqlExpression(sqlExpression)
                .build();
    }
}