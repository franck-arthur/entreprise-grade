package com.enterprise.app.domain.model.csvexport;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entité représentant les valeurs possibles d'une colonne CSV.
 * Stocke les valeurs possibles extraites depuis Excel (valeur_possible_1, valeur_possible_2, etc.).
 */
@Entity
@Table(
    name = "csv_column_values",
    schema = "export_csv",
    indexes = {
        @Index(name = "idx_csv_values_col_pos", columnList = "column_definition_id, position")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_csv_values_col_pos", columnNames = {"column_definition_id", "position"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"columnDefinition"})
@EqualsAndHashCode(of = {"columnDefinition", "position"})
public class CsvColumnValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_definition_id", nullable = false)
    private CsvColumnDefinition columnDefinition;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "valeur_possible", nullable = false, length = 500)
    private String valeurPossible;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Méthode utilitaire pour créer une valeur possible.
     */
    public static CsvColumnValues of(CsvColumnDefinition definition, Integer position, String valeur) {
        return CsvColumnValues.builder()
                .columnDefinition(definition)
                .position(position)
                .valeurPossible(valeur)
                .build();
    }
}