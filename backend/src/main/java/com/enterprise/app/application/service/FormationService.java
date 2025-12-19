package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.ConcurrentUpdateException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FormationService {

    private final FormationRepository formationRepository;
    private final FormationParticipationRepository participationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Formation getFormationById(UUID id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation non trouvée avec l'ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Formation> getAllFormations(Pageable pageable) {
        return formationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Formation> searchFormations(String secteur, String region, ModaliteFormation modalite,
                                          FormationStatut statut, Pageable pageable) {
        return formationRepository.findByFilters(secteur, region, modalite, statut, pageable);
    }

    public Formation createFormation(Formation formation) {
        log.info("Création d'une nouvelle formation: {}", formation.getLibelle());

        formation.validerCoherenceDates();
        formation.validerNbParticipants();
        formation.validerModaliteEtChamps();

        return formationRepository.save(formation);
    }

    public Formation updateFormation(UUID id, Formation formationData) {
        Formation existingFormation = getFormationById(id);

        if (existingFormation.getStatut() == FormationStatut.TERMINEE) {
            throw new BusinessException("Impossible de modifier une formation terminée");
        }

        existingFormation.setLibelle(formationData.getLibelle());
        existingFormation.setFormateurs(formationData.getFormateurs());
        existingFormation.setDescription(formationData.getDescription());
        existingFormation.setDateFormation(formationData.getDateFormation());
        existingFormation.setHeureDebut(formationData.getHeureDebut());
        existingFormation.setHeureFin(formationData.getHeureFin());
        existingFormation.setSecteur(formationData.getSecteur());
        existingFormation.setRegion(formationData.getRegion());
        existingFormation.setModalite(formationData.getModalite());
        existingFormation.setNbParticipants(formationData.getNbParticipants());
        existingFormation.setLieu(formationData.getLieu());
        existingFormation.setVille(formationData.getVille());
        existingFormation.setLienParticipation(formationData.getLienParticipation());

        existingFormation.validerCoherenceDates();
        existingFormation.validerNbParticipants();
        existingFormation.validerModaliteEtChamps();

        try {
            log.info("Mise à jour de la formation: {}", id);
            return formationRepository.save(existingFormation);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Conflit de concurrence détecté lors de la mise à jour de la formation: {}", id);
            throw new ConcurrentUpdateException(
                "La formation a été modifiée par un autre utilisateur. Veuillez recharger la page et réessayer.",
                e
            );
        }
    }

    public void deleteFormation(UUID id) {
        Formation formation = getFormationById(id);

        if (formation.getStatut() == FormationStatut.EN_COURS) {
            throw new BusinessException("Impossible de supprimer une formation en cours");
        }

        log.info("Suppression de la formation: {}", id);
        formationRepository.deleteById(id);
    }

    public FormationParticipation inscrireUtilisateur(UUID formationId, UUID userId) {
        Formation formation = getFormationById(formationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        if (participationRepository.existsByFormationIdAndUserId(formationId, userId)) {
            throw new DuplicateResourceException("L'utilisateur est déjà inscrit à cette formation");
        }

        if (!formation.peutAccepterInscription()) {
            throw new BusinessException("Inscription impossible: formation complète ou non ouverte aux inscriptions");
        }

        int participantsInscrits = formationRepository.countParticipantsInscrits(formationId);
        if (participantsInscrits >= formation.getNbParticipants()) {
            throw new BusinessException("Formation complète: nombre maximum de participants atteint");
        }

        FormationParticipation participation = FormationParticipation.builder()
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        log.info("Inscription de l'utilisateur {} à la formation {}", userId, formationId);
        return participationRepository.save(participation);
    }

    public void desinscrireUtilisateur(UUID formationId, UUID userId) {
        FormationParticipation participation = participationRepository.findByFormationIdAndUserId(formationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        if (!participation.peutEtreModifiee()) {
            throw new BusinessException("Impossible de se désinscrire: formation déjà commencée ou terminée");
        }

        log.info("Désinscription de l'utilisateur {} de la formation {}", userId, formationId);
        participationRepository.deleteById(participation.getId());
    }

    @Transactional(readOnly = true)
    public List<FormationParticipation> getParticipantsFormation(UUID formationId) {
        if (!formationRepository.existsById(formationId)) {
            throw new ResourceNotFoundException("Formation non trouvée avec l'ID: " + formationId);
        }
        return participationRepository.findByFormationId(formationId);
    }

    @Transactional(readOnly = true)
    public Page<FormationParticipation> getFormationsUtilisateur(UUID userId, Pageable pageable) {
        if (!userRepository.findById(userId).isPresent()) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + userId);
        }
        return participationRepository.findByUserId(userId, pageable);
    }

    public FormationParticipation marquerPresence(UUID formationId, UUID userId, boolean present) {
        FormationParticipation participation = participationRepository.findByFormationIdAndUserId(formationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation non trouvée"));

        Formation formation = participation.getFormation();
        if (formation.getStatut() != FormationStatut.EN_COURS) {
            throw new BusinessException("Il n'est possible de marquer la présence/absence que sur les formations en cours");
        }

        User user = participation.getUser();

        if (present) {
            participation.marquerPresent();
            user.updateDateDerniereFormation();
        } else {
            participation.marquerAbsent();
            user.resetDateDerniereFormation();
        }

        userRepository.save(user);

        log.info("Marquage de présence pour l'utilisateur {} à la formation {}: {}",
                userId, formationId, present ? "présent" : "absent");

        return participationRepository.save(participation);
    }

    // ================================
    // MÉTHODES OPTIMISÉES AVEC PROJECTIONS
    // ================================

    @Transactional(readOnly = true)
    public FormationProjection getFormationProjectionById(UUID id) {
        return formationRepository.findProjectionById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation non trouvée avec l'ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<FormationProjection> getAllFormationProjections() {
        return formationRepository.findAllProjections();
    }

    @Transactional(readOnly = true)
    public Page<FormationProjection> searchFormationProjections(String secteur, String region,
                                                               ModaliteFormation modalite, FormationStatut statut, Pageable pageable) {
        if (statut == null) {
            // Si pas de filtre statut, utilisation directe de la pagination en base
            return formationRepository.findProjectionsByFilters(secteur, region, modalite, pageable);
        } else {
            // Si filtre statut présent, récupération de toutes les données puis filtrage côté application
            // Note: Pour une vraie production, il faudrait implémenter une pagination custom plus sophistiquée
            Page<FormationProjection> allProjections = formationRepository.findProjectionsByFilters(secteur, region, modalite, Pageable.unpaged());

            List<FormationProjection> filteredProjections = allProjections.getContent().stream()
                    .filter(projection -> {
                        // Le statut est calculé dynamiquement basé sur les dates et participants
                        if (projection.getDateFormation().isBefore(java.time.LocalDate.now())) {
                            return statut == FormationStatut.TERMINEE;
                        } else if (projection.getDateFormation().equals(java.time.LocalDate.now())) {
                            return statut == FormationStatut.EN_COURS;
                        } else {
                            // Formation à venir
                            return statut == FormationStatut.A_VENIR;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Pagination manuelle des résultats filtrés
            int start = Math.min((int) pageable.getOffset(), filteredProjections.size());
            int end = Math.min((start + pageable.getPageSize()), filteredProjections.size());
            List<FormationProjection> pageContent = filteredProjections.subList(start, end);

            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredProjections.size());
        }
    }
}