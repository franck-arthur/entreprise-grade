package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fixtures centralisées pour les données de test FormationParticipation.
 */
public final class FormationParticipationFixtures {

    private FormationParticipationFixtures() {}

    // IDs constants pour les tests
    public static final UUID PARTICIPATION_ID_1 = UUID.fromString("789e0123-e89b-12d3-a456-426614174001");
    public static final UUID PARTICIPATION_ID_2 = UUID.fromString("789e0123-e89b-12d3-a456-426614174002");

    /**
     * Participation standard (absent par défaut).
     */
    public static FormationParticipation defaultParticipation() {
        return FormationParticipation.builder()
                .id(PARTICIPATION_ID_1)
                .formation(FormationFixtures.defaultFormationPresentiel())
                .user(UserFixtures.defaultUser())
                .statutParticipation(StatutParticipation.ABSENT)
                .build();
    }

    /**
     * Participation avec présence confirmée.
     */
    public static FormationParticipation participationPresent() {
        return FormationParticipation.builder()
                .id(PARTICIPATION_ID_1)
                .formation(FormationFixtures.defaultFormationPresentiel())
                .user(UserFixtures.defaultUser())
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .build();
    }

    /**
     * Participation inscrite (en attente).
     */
    public static FormationParticipation participationInscrite() {
        return FormationParticipation.builder()
                .id(PARTICIPATION_ID_2)
                .formation(FormationFixtures.defaultFormationPresentiel())
                .user(UserFixtures.adminUser())
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();
    }

    /**
     * Participation absente.
     */
    public static FormationParticipation participationAbsente() {
        return FormationParticipation.builder()
                .id(PARTICIPATION_ID_1)
                .formation(FormationFixtures.defaultFormationPresentiel())
                .user(UserFixtures.defaultUser())
                .statutParticipation(StatutParticipation.ABSENT)
                .build();
    }

    /**
     * Participation pour formation en ligne.
     */
    public static FormationParticipation participationFormationEnLigne() {
        return FormationParticipation.builder()
                .id(PARTICIPATION_ID_2)
                .formation(FormationFixtures.defaultFormationEnLigne())
                .user(UserFixtures.managerUser())
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();
    }
}