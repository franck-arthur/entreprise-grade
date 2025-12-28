package com.enterprise.app.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ModaliteFieldsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidModaliteFields {
    String message() default "Les champs requis pour la modalité ne sont pas renseignés";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}