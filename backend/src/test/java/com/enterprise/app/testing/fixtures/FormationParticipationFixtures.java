package com.enterprise.app.testing.fixtures;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import java.time.LocalDateTime;

/**
 * Fixtures centralisées pour les données de test FormationParticipation.
 */
public final class FormationParticipationFixtures {

    private FormationParticipationFixtures() {
    }

    // IDs constants pour les tests
    public static final Long PARTICIPATION_ID_1 = 1L;
    public static final Long PARTICIPATION_ID_2 = 2L;

    /**
     * Participation standard (absent par défaut).
     */
    public static FormationParticipation defaultParticipation() {
        return Instancio.of(FormationParticipation.class)
                .set(field(FormationParticipation::getId), PARTICIPATION_ID_1)
                .set(field(FormationParticipation::getFormation), FormationFixtures.defaultFormationPresentiel())
                .set(field(FormationParticipation::getUser), UserFixtures.defaultUser())
                .set(field(FormationParticipation::getStatutParticipation), StatutParticipation.ABSENT)
                .set(field(FormationParticipation::getDatePresence), null)
                .create();
    }

    /**
     * Participation avec présence confirmée.
     */
    public static FormationParticipation participationPresent() {
        return Instancio.of(FormationParticipation.class)
                .set(field(FormationParticipation::getId), PARTICIPATION_ID_2)
                .set(field(FormationParticipation::getFormation), FormationFixtures.tirageAuSortSecretaireEnCours())
                .set(field(FormationParticipation::getUser), UserFixtures.defaultUser())
                .set(field(FormationParticipation::getStatutParticipation), StatutParticipation.PRESENT)
                .set(field(FormationParticipation::getDatePresence), LocalDateTime.now())
                .create();
    }

    /**
     * Participation avec présence confirmée.
     */
    public static FormationParticipation participationPresentEnLigne() {
        return Instancio.of(FormationParticipation.class)
                .set(field(FormationParticipation::getId), PARTICIPATION_ID_2)
                .set(field(FormationParticipation::getFormation), FormationFixtures.tirageAuSortEnLigneRG())
                .set(field(FormationParticipation::getUser), UserFixtures.defaultUser())
                .set(field(FormationParticipation::getStatutParticipation), StatutParticipation.PRESENT)
                .set(field(FormationParticipation::getDatePresence), LocalDateTime.now())
                .create();
    }

    /**
     * Participation DTO standard (absent par défaut).
     */
    public static FormationParticipationDTO defaultParticipationDTO() {
        FormationParticipation formationParticipation = defaultParticipation();
        return FormationParticipationDTO.builder()
                .id(formationParticipation.getId())
                .formationId(formationParticipation.getFormation().getId())
                .statutParticipation(formationParticipation.getStatutParticipation())
                .commentaire(formationParticipation.getCommentaire())
                .userId(formationParticipation.getUser().getId())
                .build();
    }
}