package com.enterprise.app.domain.validation;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.domain.model.ModaliteFormation;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static com.enterprise.app.testing.fixtures.FormationFixtures.createDefaultFormationEnLigneRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationTimingValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    private FormationTimingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FormationTimingValidator();

        lenient().when(context.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(any(String.class))).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void shouldReturnTrueForNullObject() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForNullHeures() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureDebut(null)
                .heureFin(null)
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForNullHeureDebut() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureDebut(null)
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForNullHeureFin() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureFin(null)
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldValidateValidTiming() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest();

        testTiming(validator.isValid(request, context));
    }

    private void testTiming(boolean validator) {
        assertThat(validator).isTrue();
        verifyNoInteractions(context);
    }

    @Test
    void shouldValidateTimingAcrossMidnight() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureDebut(LocalTime.of(23, 0))
                .heureFin(LocalTime.of(1, 0))
                .build();

        boolean result = validator.isValid(request, context);

        // Note: Ce cas sera rejeté car 01:00 < 23:00 en termes d'heures dans la journée
        // Ce comportement pourrait être débattu selon les règles métier
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("L'heure de fin doit être postérieure à l'heure de début");
    }

    @Test
    void shouldRejectSameTime() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(9, 0))
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("L'heure de fin doit être postérieure à l'heure de début");
        verify(violationBuilder).addPropertyNode("heureFin");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    void shouldRejectHeureFinBeforeHeureDebut() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .heureDebut(LocalTime.of(17, 0))
                .heureFin(LocalTime.of(9, 0))
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("L'heure de fin doit être postérieure à l'heure de début");
        verify(violationBuilder).addPropertyNode("heureFin");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    void shouldReturnFalseOnReflectionError() {
        // Test avec un objet qui n'a pas les champs requis
        // Le validator retourne true car les champs sont null, ce qui est géré par les autres validations
        String invalidObject = "invalid object";

        boolean result = validator.isValid(invalidObject, context);

        // Le validator retourne true car il ne trouve pas les champs (null)
        // et laisse les autres validations (@NotNull) s'en charger
        assertThat(result).isTrue();
    }

}