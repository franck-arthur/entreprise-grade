package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.ConcurrentUpdateException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.domain.repository.SecteurRepository;
import com.enterprise.app.domain.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FormationService {
    private final FormationRepository formationRepository;
    private final FormationParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final SecteurRepository secteurRepository;
    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public Formation getFormationById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation non trouvée avec l'ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Formation> searchFormations(Long secteurId, Long regionId, ModaliteFormation modalite,
                                          FormationStatut statut, Pageable pageable) {
        return formationRepository.findByFilters(secteurId, regionId, modalite, statut, pageable);
    }

    public Formation createFormation(Formation formation) {
        log.info("Création d'une nouvelle formation: {}", formation.getLibelle());
        return formationRepository.save(formation);
    }

    public Formation updateFormation(Long id, Formation formationData) {
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

    public void deleteFormation(Long id) {
        Formation formation = getFormationById(id);

        if (formation.getStatut() == FormationStatut.EN_COURS) {
            throw new BusinessException("Impossible de supprimer une formation en cours");
        }

        log.info("Suppression de la formation: {}", id);
        formationRepository.deleteById(id);
    }

    public FormationParticipation inscrireUtilisateur(Long formationId, Long userId) {
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
                .statutParticipation(StatutParticipation.ABSENT)
                .build();

        log.info("Inscription de l'utilisateur {} à la formation {}", userId, formationId);
        return participationRepository.save(participation);
    }

    public void desinscrireUtilisateur(Long formationId, Long userId) {
        FormationParticipation participation = participationRepository.findByFormationIdAndUserId(formationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription non trouvée"));

        if (!participation.peutEtreModifiee()) {
            throw new BusinessException("Impossible de se désinscrire: formation déjà commencée ou terminée");
        }

        log.info("Désinscription de l'utilisateur {} de la formation {}", userId, formationId);
        participationRepository.deleteById(participation.getId());
    }

    @Transactional(readOnly = true)
    public List<FormationParticipation> getParticipantsFormation(Long formationId) {
        if (!formationRepository.existsById(formationId)) {
            throw new ResourceNotFoundException("Formation non trouvée avec l'ID: " + formationId);
        }
        return participationRepository.findByFormationId(formationId);
    }

    public FormationParticipation marquerPresence(Long formationId, Long userId, boolean present) {
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
    // MÉTHODES UTILITAIRES POUR SECTEUR ET RÉGION
    // ================================
    public Formation createFormationWithIds(Formation formation, Long secteurId, Long regionId) {
        log.info("Création d'une nouvelle formation: {}", formation.getLibelle());

        Secteur secteur = secteurRepository.findById(secteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Secteur non trouvé avec l'ID: " + secteurId));

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée avec l'ID: " + regionId));

        formation.setSecteur(secteur);
        formation.setRegion(region);

        return formationRepository.save(formation);
    }

    public Formation updateFormationWithIds(Long id, Formation formationData, Long secteurId, Long regionId) {
        Formation existingFormation = getFormationById(id);

        if (existingFormation.getStatut() == FormationStatut.TERMINEE) {
            throw new BusinessException("Impossible de modifier une formation terminée");
        }

        Secteur secteur = secteurRepository.findById(secteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Secteur non trouvé avec l'ID: " + secteurId));

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée avec l'ID: " + regionId));

        existingFormation.setLibelle(formationData.getLibelle());
        existingFormation.setFormateurs(formationData.getFormateurs());
        existingFormation.setDescription(formationData.getDescription());
        existingFormation.setDateFormation(formationData.getDateFormation());
        existingFormation.setHeureDebut(formationData.getHeureDebut());
        existingFormation.setHeureFin(formationData.getHeureFin());
        existingFormation.setSecteur(secteur);
        existingFormation.setRegion(region);
        existingFormation.setModalite(formationData.getModalite());
        existingFormation.setNbParticipants(formationData.getNbParticipants());
        existingFormation.setLieu(formationData.getLieu());
        existingFormation.setVille(formationData.getVille());
        existingFormation.setLienParticipation(formationData.getLienParticipation());

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
}