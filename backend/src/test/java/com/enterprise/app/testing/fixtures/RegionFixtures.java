package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.Region;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import java.time.LocalDateTime;

/**
 * Fixtures centralisées pour les données de test Region.
 * Fournit des instances prédéfinies et réutilisables pour les tests.
 */
public final class RegionFixtures {

    private RegionFixtures() {}

    // IDs constants pour les tests
    public static final Long REGION_ID_IDF = 1L;
    public static final Long REGION_ID_AURA = 2L;
    public static final Long REGION_ID_MARTINIQUE = 3L;

    // Codes constants
    public static final String CODE_IDF = "IDF";
    public static final String CODE_AURA = "AURA";
    public static final String CODE_MARTINIQUE = "MTQ";

    // Noms constants
    public static final String NOM_IDF = "Île-de-France";
    public static final String NOM_AURA = "Auvergne-Rhône-Alpes";
    public static final String NOM_MARTINIQUE = "Martinique";

    /**
     * Région Île-de-France par défaut.
     */
    public static Region defaultRegionIDF() {
        return Region.builder()
                .id(REGION_ID_IDF)
                .code(CODE_IDF)
                .nom(NOM_IDF)
                .description("Région parisienne et départements limitrophes")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Région Auvergne-Rhône-Alpes par défaut.
     */
    public static Region defaultRegionAURA() {
        return Region.builder()
                .id(REGION_ID_AURA)
                .code(CODE_AURA)
                .nom(NOM_AURA)
                .description("Région du sud-est de la France")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Région Martinique par défaut.
     */
    public static Region defaultRegionMartinique() {
        return Region.builder()
                .id(REGION_ID_MARTINIQUE)
                .code(CODE_MARTINIQUE)
                .nom(NOM_MARTINIQUE)
                .description("Région d'outre-mer des Antilles")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Région inactive pour les tests.
     */
    public static Region regionInactive() {
        return Region.builder()
                .id(99L)
                .code("INACTIF")
                .nom("Région Inactive")
                .description("Région désactivée pour les tests")
                .actif(false)
                .createdAt(LocalDateTime.now().minusDays(365))
                .updatedAt(LocalDateTime.now().minusDays(30))
                .version(2L)
                .build();
    }

    /**
     * Région avec données minimales.
     */
    public static Region regionMinimale() {
        return Region.builder()
                .code("MIN")
                .nom("Minimale")
                .actif(true)
                .build();
    }

    /**
     * Génère une région aléatoire avec Instancio.
     */
    public static Region randomRegion() {
        return Instancio.of(Region.class)
                .set(field(Region::getActif), true)
                .create();
    }

    /**
     * Région avec code personnalisé.
     */
    public static Region regionWithCode(String code) {
        return Region.builder()
                .code(code)
                .nom("Région " + code)
                .description("Description pour région " + code)
                .actif(true)
                .build();
    }

    /**
     * Région avec nom personnalisé.
     */
    public static Region regionWithNom(String nom) {
        return Region.builder()
                .code(nom.toUpperCase().substring(0, Math.min(3, nom.length())))
                .nom(nom)
                .description("Description pour " + nom)
                .actif(true)
                .build();
    }
}