package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.Secteur;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import java.time.LocalDateTime;

/**
 * Fixtures centralisées pour les données de test Secteur.
 * Fournit des instances prédéfinies et réutilisables pour les tests.
 */
public final class SecteurFixtures {

    private SecteurFixtures() {}

    // IDs constants pour les tests
    public static final Long SECTEUR_ID_IT = 1L;
    public static final Long SECTEUR_ID_RG = 2L;
    public static final Long SECTEUR_ID_MSA = 3L;

    // Codes constants
    public static final String CODE_IT = "IT";
    public static final String CODE_RG = "RG";
    public static final String CODE_MSA = "MSA";

    // Noms constants
    public static final String NOM_IT = "Informatique";
    public static final String NOM_RG = "Regime général";
    public static final String NOM_MSA = "MSA";

    /**
     * Secteur IT par défaut.
     */
    public static Secteur defaultSecteurIT() {
        return Secteur.builder()
                .id(SECTEUR_ID_IT)
                .code(CODE_IT)
                .nom(NOM_IT)
                .description("Secteur informatique et technologies")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Secteur Régime Général par défaut.
     */
    public static Secteur defaultSecteurRG() {
        return Secteur.builder()
                .id(SECTEUR_ID_RG)
                .code(CODE_RG)
                .nom(NOM_RG)
                .description("Régime général de sécurité sociale")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Secteur MSA par défaut.
     */
    public static Secteur defaultSecteurMSA() {
        return Secteur.builder()
                .id(SECTEUR_ID_MSA)
                .code(CODE_MSA)
                .nom(NOM_MSA)
                .description("Mutualité Sociale Agricole")
                .actif(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .version(1L)
                .build();
    }

    /**
     * Secteur inactif pour les tests.
     */
    public static Secteur secteurInactif() {
        return Secteur.builder()
                .id(99L)
                .code("INACTIF")
                .nom("Secteur Inactif")
                .description("Secteur désactivé pour les tests")
                .actif(false)
                .createdAt(LocalDateTime.now().minusDays(365))
                .updatedAt(LocalDateTime.now().minusDays(30))
                .version(2L)
                .build();
    }

    /**
     * Secteur avec données minimales.
     */
    public static Secteur secteurMinimal() {
        return Secteur.builder()
                .code("MIN")
                .nom("Minimal")
                .actif(true)
                .build();
    }

    /**
     * Génère un secteur aléatoire avec Instancio.
     */
    public static Secteur randomSecteur() {
        return Instancio.of(Secteur.class)
                .set(field(Secteur::getActif), true)
                .create();
    }

    /**
     * Secteur avec code personnalisé.
     */
    public static Secteur secteurWithCode(String code) {
        return Secteur.builder()
                .code(code)
                .nom("Secteur " + code)
                .description("Description pour secteur " + code)
                .actif(true)
                .build();
    }

    /**
     * Secteur avec nom personnalisé.
     */
    public static Secteur secteurWithNom(String nom) {
        return Secteur.builder()
                .code(nom.toUpperCase().substring(0, Math.min(3, nom.length())))
                .nom(nom)
                .description("Description pour " + nom)
                .actif(true)
                .build();
    }
}