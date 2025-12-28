package com.enterprise.app.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = FormationTimingValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFormationTiming {
    String message() default "Les horaires de la formation ne sont pas cohérents";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}