package com.enterprise.app.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalTime;

public class FormationTimingValidator implements ConstraintValidator<ValidFormationTiming, Object> {

    @Override
    public void initialize(ValidFormationTiming constraintAnnotation) {
        // Initialisation si nécessaire
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            // Utilise réflexion pour accéder aux champs heureDebut et heureFin
            LocalTime heureDebut = getFieldValue(obj, "heureDebut");
            LocalTime heureFin = getFieldValue(obj, "heureFin");

            if (heureDebut == null || heureFin == null) {
                return true; // Les validations @NotNull s'en chargeront
            }

            if (heureFin.isBefore(heureDebut) || heureFin.equals(heureDebut)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "L'heure de fin doit être postérieure à l'heure de début")
                    .addPropertyNode("heureFin")
                    .addConstraintViolation();
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
}