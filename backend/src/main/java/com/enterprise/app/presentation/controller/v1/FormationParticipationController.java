package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.FormationParticipation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/formations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Formation Participations", description = "Formation participation management API")
@SecurityRequirement(name = "bearerAuth")
public class FormationParticipationController {

    private final FormationService formationService;
    private final FormationParticipationMapper participationMapper;

    @PostMapping("/{formationId}/inscriptions/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD') or #userId.toString() == authentication.name")
    @Operation(
        summary = "Inscrire un utilisateur à une formation",
        description = "Inscrire un utilisateur spécifique à une formation. Les utilisateurs peuvent s'inscrire eux-mêmes ou être inscrits par un administrateur."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Inscription créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Formation complète ou inscription impossible"),
        @ApiResponse(responseCode = "404", description = "Formation ou utilisateur non trouvé"),
        @ApiResponse(responseCode = "409", description = "Utilisateur déjà inscrit"),
        @ApiResponse(responseCode = "401", description = "Non autorisé"),
        @ApiResponse(responseCode = "403", description = "Interdit")
    })
    public ResponseEntity<FormationParticipationDTO> inscrireUtilisateur(
            @Parameter(description = "ID de la formation") @PathVariable UUID formationId,
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId,
            Authentication authentication) {

        FormationParticipation participation = formationService.inscrireUtilisateur(formationId, userId);
        FormationParticipationDTO participationDTO = participationMapper.toDTO(participation);

        log.info("Utilisateur {} inscrit à la formation {}", userId, formationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participationDTO);
    }

    @DeleteMapping("/{formationId}/inscriptions/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD') or #userId.toString() == authentication.name")
    @Operation(
        summary = "Désinscrire un utilisateur d'une formation",
        description = "Désinscrire un utilisateur d'une formation. Les utilisateurs peuvent se désinscrire eux-mêmes ou être désincrits par un administrateur."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Désinscription réussie"),
        @ApiResponse(responseCode = "400", description = "Désinscription impossible"),
        @ApiResponse(responseCode = "404", description = "Inscription non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé"),
        @ApiResponse(responseCode = "403", description = "Interdit")
    })
    public ResponseEntity<Void> desinscrireUtilisateur(
            @Parameter(description = "ID de la formation") @PathVariable UUID formationId,
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId) {

        formationService.desinscrireUtilisateur(formationId, userId);
        log.info("Utilisateur {} désinscrit de la formation {}", userId, formationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/inscriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD') or #userId.toString() == authentication.name")
    @Operation(
        summary = "Obtenir les formations d'un utilisateur",
        description = "Récupérer toutes les formations auxquelles un utilisateur est inscrit."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formations récupérées avec succès"),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé"),
        @ApiResponse(responseCode = "401", description = "Non autorisé"),
        @ApiResponse(responseCode = "403", description = "Interdit")
    })
    public ResponseEntity<Page<FormationParticipationDTO>> getFormationsUtilisateur(
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<FormationParticipation> participations = formationService.getFormationsUtilisateur(userId, pageable);
        Page<FormationParticipationDTO> participationDTOs = participations.map(participationMapper::toDTO);

        return ResponseEntity.ok(participationDTOs);
    }

    @PutMapping("/{formationId}/presence/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD')")
    @Operation(
        summary = "Marquer la présence d'un utilisateur",
        description = "Marquer un utilisateur comme présent ou absent à une formation. Requires ADMIN, MANAGER, or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Présence marquée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
        @ApiResponse(responseCode = "404", description = "Participation non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé"),
        @ApiResponse(responseCode = "403", description = "Interdit")
    })
    public ResponseEntity<FormationParticipationDTO> marquerPresence(
            @Parameter(description = "ID de la formation") @PathVariable UUID formationId,
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId,
            @Valid @RequestBody PresenceRequest request) {

        FormationParticipation participation = formationService.marquerPresence(
                formationId, userId, request.getPresent());

        if (request.getCommentaire() != null) {
            participation.setCommentaire(request.getCommentaire());
        }

        FormationParticipationDTO participationDTO = participationMapper.toDTO(participation);

        log.info("Présence marquée pour l'utilisateur {} à la formation {}: {}",
                userId, formationId, request.getPresent() ? "présent" : "absent");

        return ResponseEntity.ok(participationDTO);
    }
}