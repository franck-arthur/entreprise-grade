package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.User;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/formations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Formation Participations", description = "Formation participation management API")
@SecurityRequirement(name = "bearerAuth")
public class FormationParticipationController {

    private final FormationService formationService;
    private final FormationParticipationMapper participationMapper;
    private final UserService userService;

    @PostMapping("/{formationId}/inscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "S'inscrire à une formation",
        description = "Inscription de l'utilisateur connecté à une formation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Inscription créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Formation complète ou inscription impossible"),
        @ApiResponse(responseCode = "404", description = "Formation non trouvée"),
        @ApiResponse(responseCode = "409", description = "Utilisateur déjà inscrit"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    public ResponseEntity<FormationParticipationDTO> inscrireUtilisateur(
            @Parameter(description = "ID de la formation") @PathVariable Long formationId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        FormationParticipation participation = formationService.inscrireUtilisateur(formationId, currentUser.getId());
        FormationParticipationDTO participationDTO = participationMapper.toDTO(participation);

        log.info("Utilisateur {} inscrit à la formation {}", currentUser.getId(), formationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participationDTO);
    }

    @DeleteMapping("/{formationId}/inscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Se désinscrire d'une formation",
        description = "Désinscription de l'utilisateur connecté d'une formation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Désinscription réussie"),
        @ApiResponse(responseCode = "400", description = "Désinscription impossible"),
        @ApiResponse(responseCode = "404", description = "Inscription non trouvée"),
        @ApiResponse(responseCode = "401", description = "Non autorisé")
    })
    public ResponseEntity<Void> desinscrireUtilisateur(
            @Parameter(description = "ID de la formation") @PathVariable Long formationId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        formationService.desinscrireUtilisateur(formationId, currentUser.getId());
        log.info("Utilisateur {} désinscrit de la formation {}", currentUser.getId(), formationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{formationId}/participants/{userId}/presence")
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
            @Parameter(description = "ID de la formation") @PathVariable Long formationId,
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long userId,
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

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String username = jwt.getClaimAsString("preferred_username");

        if (username == null) {
            username = jwt.getSubject();
        }

        return userService.getUserEntityByUsername(username);
    }
}