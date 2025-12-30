package com.enterprise.app.domain.model.csvexport;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant la définition d'une colonne CSV (partie métier depuis Excel).
 * Contient les informations fonctionnelles de chaque colonne d'export.
 */
@Entity
@Table(
    name = "csv_column_definition",
    schema = "export_csv",
    indexes = {
        @Index(name = "idx_csv_def_nom_ordre", columnList = "nom_csv, ordre_colonne"),
        @Index(name = "idx_csv_def_code_var", columnList = "code_variable"),
        @Index(name = "idx_csv_def_actif", columnList = "actif")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_csv_column_nom_code", columnNames = {"nom_csv", "code_variable"}),
        @UniqueConstraint(name = "uk_csv_column_nom_ordre", columnNames = {"nom_csv", "ordre_colonne"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"valeursPossibles"})
@EqualsAndHashCode(of = {"nomCsv", "codeVariable"})
public class CsvColumnDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_csv", nullable = false, length = 50)
    private String nomCsv;

    @Column(name = "ordre_colonne", nullable = false)
    private Integer ordreColonne;

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

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @OneToMany(mappedBy = "columnDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<CsvColumnValues> valeursPossibles = new ArrayList<>();

    /**
     * Ajoute une valeur possible à la position spécifiée.
     */
    public void addValeurPossible(Integer position, String valeur) {
        CsvColumnValues value = CsvColumnValues.builder()
                .columnDefinition(this)
                .position(position)
                .valeurPossible(valeur)
                .build();
        this.valeursPossibles.add(value);
    }

    /**
     * Supprime toutes les valeurs possibles.
     */
    public void clearValeursPossibles() {
        this.valeursPossibles.clear();
    }

    /**
     * Vérifie si cette colonne a un mapping technique associé.
     */
    public boolean hasMapping() {
        return true;
    }

    /**
     * Retourne la valeur possible à la position donnée.
     */
    public String getValeurPossibleAtPosition(Integer position) {
        return valeursPossibles.stream()
                .filter(v -> position.equals(v.getPosition()))
                .map(CsvColumnValues::getValeurPossible)
                .findFirst()
                .orElse(null);
    }
}