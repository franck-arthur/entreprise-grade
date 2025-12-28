package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.validation.ValidFormationTiming;
import com.enterprise.app.domain.validation.ValidModaliteFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating formation data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidFormationTiming
@ValidModaliteFields
public class UpdateFormationRequest {

  @NotBlank(message = "Le libellé est obligatoire")
  @Size(max = 200, message = "Le libellé ne peut pas dépasser 200 caractères")
  private String libelle;

  @NotBlank(message = "Les formateurs sont obligatoires")
  @Size(max = 500, message = "Les formateurs ne peuvent pas dépasser 500 caractères")
  private String formateurs;

  @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
  private String description;

  @NotNull(message = "La date de formation est obligatoire")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateFormation;

  @NotNull(message = "L'heure de début est obligatoire")
  @JsonFormat(pattern = "HH:mm")
  private LocalTime heureDebut;

  @NotNull(message = "L'heure de fin est obligatoire")
  @JsonFormat(pattern = "HH:mm")
  private LocalTime heureFin;

  @NotNull(message = "Le secteur est obligatoire")
  private Long secteurId;

  @NotNull(message = "La région est obligatoire")
  private Long regionId;

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
  @Pattern(regexp = "^(https?://).*", message = "Le lien doit être une URL valide")
  private String lienParticipation;

}