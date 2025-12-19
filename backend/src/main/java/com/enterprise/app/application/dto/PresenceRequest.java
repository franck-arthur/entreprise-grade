package com.enterprise.app.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceRequest {

    @NotNull(message = "Le statut de présence est obligatoire")
    private Boolean present;

    private String commentaire;
}