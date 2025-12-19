package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.ModaliteFormation;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormationRequest {

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 200, message = "Le libellé ne peut pas dépasser 200 caractères")
    private String libelle;

    @NotBlank(message = "Les formateurs sont obligatoires")
    @Size(max = 500, message = "Les formateurs ne peuvent pas dépasser 500 caractères")
    private String formateurs;

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description;

    @NotNull(message = "La date de formation est obligatoire")
    @Future(message = "La date de formation doit être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFormation;

    @NotNull(message = "L'heure de début est obligatoire")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    @NotBlank(message = "Le secteur est obligatoire")
    @Size(max = 100, message = "Le secteur ne peut pas dépasser 100 caractères")
    private String secteur;

    @NotBlank(message = "La région est obligatoire")
    @Size(max = 100, message = "La région ne peut pas dépasser 100 caractères")
    private String region;

    @NotNull(message = "La modalité est obligatoire")
    private ModaliteFormation modalite;

    @NotNull(message = "Le nombre de participants est obligatoire")
    @Min(value = 1, message = "Le nombre de participants doit être au moins de 1")
    @Max(value = 1000, message = "Le nombre de participants ne peut pas dépasser 1000")
    private Integer nbParticipants;

    @Size(max = 200, message = "Le lieu ne peut pas dépasser 200 caractères")
    private String lieu;

    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String ville;

    @Size(max = 500, message = "Le lien de participation ne peut pas dépasser 500 caractères")
    @Pattern(regexp = "^(https?://).*", message = "Le lien doit être une URL valide", groups = {})
    private String lienParticipation;

    public void validerModaliteEtChamps() {
        if (modalite == null) {
            throw new IllegalArgumentException("La modalité de formation est obligatoire");
        }

        switch (modalite) {
            case EN_PRESENTIEL:
                if (ville == null || ville.trim().isEmpty()) {
                    throw new IllegalArgumentException("La ville est obligatoire pour une formation en présentiel");
                }
                if (lieu == null || lieu.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le lieu est obligatoire pour une formation en présentiel");
                }
                break;
            case EN_LIGNE:
                if (lienParticipation == null || lienParticipation.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le lien de participation est obligatoire pour une formation en ligne");
                }
                break;
        }
    }
}