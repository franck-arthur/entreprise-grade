package com.enterprise.app.domain.validation;

import com.enterprise.app.domain.model.ModaliteFormation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.enterprise.app.domain.model.ModaliteFormation.EN_LIGNE;
import static com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL;
import static org.springframework.util.StringUtils.hasText;

public class ModaliteFieldsValidator implements ConstraintValidator<ValidModaliteFields, Object> {

    @Override
    public void initialize(ValidModaliteFields constraintAnnotation) {
        // Initialisation si nécessaire
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            ModaliteFormation modalite = getFieldValue(obj, "modalite");
            String lieu = getFieldValue(obj, "lieu");
            String ville = getFieldValue(obj, "ville");
            String lienParticipation = getFieldValue(obj, "lienParticipation");

            if (modalite == null) {
                return true; // Les validations @NotNull s'en chargeront
            }

            context.disableDefaultConstraintViolation();
            boolean isValid = true;

            if (modalite == PRESENTIEL) {
                if (!hasText(ville)) {
                    context.buildConstraintViolationWithTemplate(
                                    "La ville est obligatoire pour une formation en présentiel")
                            .addPropertyNode("ville")
                            .addConstraintViolation();
                    isValid = false;
                }
                if (!hasText(lieu)) {
                    context.buildConstraintViolationWithTemplate(
                                    "Le lieu est obligatoire pour une formation en présentiel")
                            .addPropertyNode("lieu")
                            .addConstraintViolation();
                    isValid = false;
                }
            }
            if(modalite == EN_LIGNE && !hasText(lienParticipation)) {
                    context.buildConstraintViolationWithTemplate(
                        "Le lien de participation est obligatoire pour une formation en ligne")
                        .addPropertyNode("lienParticipation")
                        .addConstraintViolation();
                    isValid = false;
                }


            return isValid;
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