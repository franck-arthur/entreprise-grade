package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FormationDTO {


    private Long id;
    private String libelle;
    private String formateurs;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFormation;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    private SecteurDTO secteur;
    private RegionDTO region;
    private ModaliteFormation modalite;
    private Integer nbParticipants;
    private String lieu;
    private String ville;
    private String lienParticipation;
    private FormationStatut statut;
    private Integer nbParticipantsInscrits;
    private Boolean complet;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}