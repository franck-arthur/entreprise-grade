package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour ColumnSqlMapping")
class ColumnSqlMappingTest {

    @Test
    @DisplayName("Doit créer une instance valide avec le builder")
    void builder_ShouldCreateValidInstance() {
        // Given & When
        ColumnSqlMapping mapping = ColumnSqlMapping.builder()
                .codeVariable("TEST_VAR")
                .sourceTable("formation")
                .sourceColumn("nom")
                .build();

        // Then
        assertThat(mapping).isNotNull();
        assertThat(mapping.getCodeVariable()).isEqualTo("TEST_VAR");
        assertThat(mapping.getSourceTable()).isEqualTo("formation");
        assertThat(mapping.getSourceColumn()).isEqualTo("nom");
        assertThat(mapping.getRequiresSubquery()).isFalse(); // Valeur par défaut
        assertThat(mapping.getIsAggregation()).isFalse(); // Valeur par défaut
    }

    @Test
    @DisplayName("Doit créer un mapping simple avec la méthode factory ofSimple")
    void ofSimple_ShouldCreateSimpleMapping() {
        // Given & When
        ColumnSqlMapping mapping = ColumnSqlMapping.ofSimple("NOM_PERSONNE", "personne", "nom");

        // Then
        assertThat(mapping).isNotNull();
        assertThat(mapping.getCodeVariable()).isEqualTo("NOM_PERSONNE");
        assertThat(mapping.getSourceTable()).isEqualTo("personne");
        assertThat(mapping.getSourceColumn()).isEqualTo("nom");
        assertThat(mapping.getSqlExpression()).isNull();
    }

    @Test
    @DisplayName("Doit créer un mapping complexe avec la méthode factory ofExpression")
    void ofExpression_ShouldCreateComplexMapping() {
        // Given
        String expression = "CASE WHEN f.statut = 'ACTIF' THEN 'Oui' ELSE 'Non' END";

        // When
        ColumnSqlMapping mapping = ColumnSqlMapping.ofExpression("FORMATION_ACTIVE", expression);

        // Then
        assertThat(mapping).isNotNull();
        assertThat(mapping.getCodeVariable()).isEqualTo("FORMATION_ACTIVE");
        assertThat(mapping.getSqlExpression()).isEqualTo(expression);
        assertThat(mapping.getSourceTable()).isNull();
        assertThat(mapping.getSourceColumn()).isNull();
    }

    @Test
    @DisplayName("Doit identifier correctement un mapping de colonne simple")
    void isSimpleColumn_ShouldReturnTrueForSimpleMapping() {
        // Given
        ColumnSqlMapping simpleMapping = ColumnSqlMapping.ofSimple("CODE", "formation", "code");
        ColumnSqlMapping complexMapping = ColumnSqlMapping.ofExpression("COMPLEX", "SELECT COUNT(*) FROM table");

        // When & Then
        assertThat(simpleMapping.isSimpleColumn()).isTrue();
        assertThat(complexMapping.isSimpleColumn()).isFalse();
    }

    @Test
    @DisplayName("Doit identifier correctement un mapping avec expression complexe")
    void isComplexExpression_ShouldReturnTrueForComplexMapping() {
        // Given
        ColumnSqlMapping simpleMapping = ColumnSqlMapping.ofSimple("CODE", "formation", "code");
        ColumnSqlMapping complexMapping = ColumnSqlMapping.ofExpression("COMPLEX", "SELECT COUNT(*) FROM table");

        // When & Then
        assertThat(complexMapping.isComplexExpression()).isTrue();
        assertThat(simpleMapping.isComplexExpression()).isFalse();
    }

    @Test
    @DisplayName("Doit générer un alias SQL correct pour une colonne simple")
    void getSqlAlias_ShouldGenerateCorrectAliasForSimpleColumn() {
        // Given
        ColumnSqlMapping mapping = ColumnSqlMapping.ofSimple("NOM_FORMATION", "formation", "nom");

        // When
        String alias = mapping.getSqlAlias();

        // Then
        assertThat(alias).isEqualTo("f.nom AS NOM_FORMATION");
    }

    @Test
    @DisplayName("Doit générer un alias SQL correct pour une expression complexe")
    void getSqlAlias_ShouldGenerateCorrectAliasForComplexExpression() {
        // Given
        String expression = "CASE WHEN f.date_fin < NOW() THEN 'Terminée' ELSE 'En cours' END";
        ColumnSqlMapping mapping = ColumnSqlMapping.ofExpression("STATUT_FORMATION", expression);

        // When
        String alias = mapping.getSqlAlias();

        // Then
        assertThat(alias).isEqualTo("(" + expression + ") AS STATUT_FORMATION");
    }

    @Test
    @DisplayName("Doit retourner NULL AS pour un mapping invalide")
    void getSqlAlias_ShouldReturnNullForInvalidMapping() {
        // Given
        ColumnSqlMapping invalidMapping = ColumnSqlMapping.builder()
                .codeVariable("INVALID")
                .build(); // Ni table/colonne ni expression

        // When
        String alias = invalidMapping.getSqlAlias();

        // Then
        assertThat(alias).isEqualTo("NULL AS INVALID");
    }

    @Test
    @DisplayName("Doit générer les alias de table corrects")
    void getSqlAlias_ShouldUseCorrectTableAliases() {
        // Given & When & Then
        assertThat(ColumnSqlMapping.ofSimple("CODE", "questionnaire", "code").getSqlAlias())
                .isEqualTo("q.code AS CODE");

        assertThat(ColumnSqlMapping.ofSimple("NOM", "personne", "nom").getSqlAlias())
                .isEqualTo("p.nom AS NOM");

        assertThat(ColumnSqlMapping.ofSimple("VILLE", "adresse", "ville").getSqlAlias())
                .isEqualTo("a.ville AS VILLE");

        assertThat(ColumnSqlMapping.ofSimple("TITRE", "formation", "titre").getSqlAlias())
                .isEqualTo("f.titre AS TITRE");

        assertThat(ColumnSqlMapping.ofSimple("STATUT", "formation_participation", "statut").getSqlAlias())
                .isEqualTo("fp.statut AS STATUT");

        assertThat(ColumnSqlMapping.ofSimple("SECTEUR", "secteur", "nom").getSqlAlias())
                .isEqualTo("s.nom AS SECTEUR");

        assertThat(ColumnSqlMapping.ofSimple("REGION", "region", "nom").getSqlAlias())
                .isEqualTo("r.nom AS REGION");

        // Table inconnue - utilise la première lettre
        assertThat(ColumnSqlMapping.ofSimple("AUTRE", "autre_table", "colonne").getSqlAlias())
                .isEqualTo("a.colonne AS AUTRE");
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur codeVariable")
    void equals_ShouldBeBasedOnCodeVariable() {
        // Given
        ColumnSqlMapping mapping1 = ColumnSqlMapping.builder()
                .codeVariable("SAME_CODE")
                .sourceTable("table1")
                .build();

        ColumnSqlMapping mapping2 = ColumnSqlMapping.builder()
                .codeVariable("SAME_CODE")
                .sourceTable("table2") // Table différente mais même codeVariable
                .build();

        ColumnSqlMapping mapping3 = ColumnSqlMapping.builder()
                .codeVariable("DIFFERENT_CODE")
                .sourceTable("table1")
                .build();

        // When & Then
        assertThat(mapping1).isEqualTo(mapping2);
        assertThat(mapping1).isNotEqualTo(mapping3);
        assertThat(mapping1.hashCode()).isEqualTo(mapping2.hashCode());
    }

    @Test
    @DisplayName("Doit utiliser les valeurs par défaut pour requiresSubquery et isAggregation")
    void builder_ShouldUseDefaultValues() {
        // Given & When
        ColumnSqlMapping mapping = ColumnSqlMapping.builder()
                .codeVariable("TEST")
                .build();

        // Then
        assertThat(mapping.getRequiresSubquery()).isFalse();
        assertThat(mapping.getIsAggregation()).isFalse();
    }

    @Test
    @DisplayName("Doit permettre de définir requiresSubquery et isAggregation à true")
    void builder_ShouldAllowSettingBooleanFields() {
        // Given & When
        ColumnSqlMapping mapping = ColumnSqlMapping.builder()
                .codeVariable("TEST")
                .requiresSubquery(true)
                .isAggregation(true)
                .build();

        // Then
        assertThat(mapping.getRequiresSubquery()).isTrue();
        assertThat(mapping.getIsAggregation()).isTrue();
    }

    @Test
    @DisplayName("Doit utiliser AllArgsConstructor correctement")
    void allArgsConstructor_ShouldSetAllFields() {
        // Given
        Long id = 1L;
        String codeVariable = "TEST_CODE";
        String sourceTable = "test_table";
        String sourceColumn = "test_column";
        String sqlExpression = "test_expression";
        String[] joinTables = {"table1", "table2"};
        Boolean requiresSubquery = true;
        Boolean isAggregation = false;
        String description = "Test description";
        String exempleValeur = "Exemple";
        LocalDateTime dateCreation = LocalDateTime.now();
        LocalDateTime dateModification = LocalDateTime.now();

        // When
        ColumnSqlMapping mapping = new ColumnSqlMapping(
                id, codeVariable, sourceTable, sourceColumn, sqlExpression,
                joinTables, requiresSubquery, isAggregation, description,
                exempleValeur, dateCreation, dateModification
        );

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getCodeVariable()).isEqualTo(codeVariable);
        assertThat(mapping.getSourceTable()).isEqualTo(sourceTable);
        assertThat(mapping.getSourceColumn()).isEqualTo(sourceColumn);
        assertThat(mapping.getSqlExpression()).isEqualTo(sqlExpression);
        assertThat(mapping.getJoinTables()).isEqualTo(joinTables);
        assertThat(mapping.getRequiresSubquery()).isEqualTo(requiresSubquery);
        assertThat(mapping.getIsAggregation()).isEqualTo(isAggregation);
        assertThat(mapping.getDescription()).isEqualTo(description);
        assertThat(mapping.getExempleValeur()).isEqualTo(exempleValeur);
        assertThat(mapping.getDateCreation()).isEqualTo(dateCreation);
        assertThat(mapping.getDateModification()).isEqualTo(dateModification);
    }

    @Test
    @DisplayName("Doit permettre la modification des propriétés via les setters")
    void setters_ShouldModifyProperties() {
        // Given
        ColumnSqlMapping mapping = new ColumnSqlMapping();
        String nouveauCode = "NOUVEAU_CODE";
        String nouvelleTable = "nouvelle_table";

        // When
        mapping.setCodeVariable(nouveauCode);
        mapping.setSourceTable(nouvelleTable);
        mapping.setRequiresSubquery(true);

        // Then
        assertThat(mapping.getCodeVariable()).isEqualTo(nouveauCode);
        assertThat(mapping.getSourceTable()).isEqualTo(nouvelleTable);
        assertThat(mapping.getRequiresSubquery()).isTrue();
    }

    @Test
    @DisplayName("Doit gérer les joinTables comme un tableau de chaînes")
    void joinTables_ShouldHandleStringArray() {
        // Given
        String[] tables = {"formation", "personne", "adresse"};
        ColumnSqlMapping mapping = ColumnSqlMapping.builder()
                .codeVariable("TEST")
                .joinTables(tables)
                .build();

        // When
        String[] result = mapping.getJoinTables();

        // Then
        assertThat(result).isEqualTo(tables);
        assertThat(result).hasSize(3);
        assertThat(result).contains("formation", "personne", "adresse");
    }
}