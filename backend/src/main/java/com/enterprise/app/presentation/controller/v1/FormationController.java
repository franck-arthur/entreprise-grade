package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.*;
import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/formations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Formations", description = "Formation management API")
@SecurityRequirement(name = "bearerAuth")
public class FormationController {

    private final FormationService formationService;
    private final FormationMapper formationMapper;
    private final FormationParticipationMapper participationMapper;

    @GetMapping
    @Operation(
        summary = "Get all formations (optimized)",
        description = "Retrieve a paginated list of formations with optional filters using optimized query"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<FormationDTO>> getAllFormations(
            @Parameter(description = "Secteur filter") @RequestParam(required = false) String secteur,
            @Parameter(description = "Region filter") @RequestParam(required = false) String region,
            @Parameter(description = "Modalite filter") @RequestParam(required = false) ModaliteFormation modalite,
            @Parameter(description = "Statut filter") @RequestParam(required = false) FormationStatut statut,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<FormationProjection> projections = formationService.searchFormationProjections(secteur, region, modalite, statut, pageable);
        Page<FormationDTO> formationDTOs = projections.map(formationMapper::toDTO);
        return ResponseEntity.ok(formationDTOs);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get formation by ID",
        description = "Retrieve a specific formation by its ID with optimized query"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formation retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Formation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FormationDTO> getFormationById(
            @Parameter(description = "Formation ID") @PathVariable UUID id) {

        var formationProjection = formationService.getFormationProjectionById(id);
        FormationDTO formationDTO = formationMapper.toDTO(formationProjection);
        return ResponseEntity.ok(formationDTO);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Create formation",
        description = "Create a new formation. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Formation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<FormationDTO> createFormation(
            @Valid @RequestBody CreateFormationRequest request) {

        request.validerModaliteEtChamps();
        Formation formation = formationMapper.toEntity(request);
        Formation createdFormation = formationService.createFormation(formation);
        FormationDTO formationDTO = formationMapper.toDTO(createdFormation);

        log.info("Formation créée avec l'ID: {}", createdFormation.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(formationDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Update formation",
        description = "Update an existing formation. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Formation updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Formation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<FormationDTO> updateFormation(
            @Parameter(description = "Formation ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateFormationRequest request) {

        request.validerModaliteEtChamps();
        Formation existingFormation = formationService.getFormationById(id);
        formationMapper.updateEntityFromDTO(request, existingFormation);
        Formation updatedFormation = formationService.updateFormation(id, existingFormation);
        FormationDTO formationDTO = formationMapper.toDTO(updatedFormation);

        log.info("Formation mise à jour avec l'ID: {}", id);
        return ResponseEntity.ok(formationDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Delete formation",
        description = "Delete an existing formation. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Formation deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Formation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteFormation(
            @Parameter(description = "Formation ID") @PathVariable UUID id) {

        formationService.deleteFormation(id);
        log.info("Formation supprimée avec l'ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD')")
    @Operation(
        summary = "Get formation participants",
        description = "Get all participants for a specific formation. Requires ADMIN, MANAGER, or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participants retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Formation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<FormationParticipationDTO>> getFormationParticipants(
            @Parameter(description = "Formation ID") @PathVariable UUID id) {

        List<FormationParticipation> participants = formationService.getParticipantsFormation(id);
        List<FormationParticipationDTO> participantDTOs = participants.stream()
                .map(participationMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(participantDTOs);
    }
}