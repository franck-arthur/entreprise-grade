package com.enterprise.app.domain.validation;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.enterprise.app.testing.fixtures.FormationFixtures.createDefaultFormationEnLigneRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour ModaliteFieldsValidator")
@ExtendWith(MockitoExtension.class)
class ModaliteFieldsValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    private ModaliteFieldsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ModaliteFieldsValidator();

        lenient().when(context.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(any(String.class))).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("Doit retourner vrai quand l'objet est null")
    void shouldReturnTrueForNullObject() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Doit retourner vrai quand la modalité est null")
    void shouldReturnTrueForNullModalite() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest().toBuilder()
            .modalite(null)
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Doit valider une formation présentielle avec des champs valides")
    void shouldValidatePresentielFormationWithValidFields() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
        verify(context).disableDefaultConstraintViolation();
        verifyNoMoreInteractions(context);
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle sans ville")
    void shouldRejectPresentielFormationWithoutVille() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .ville(null)
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("La ville est obligatoire pour une formation en présentiel");
        verify(violationBuilder).addPropertyNode("ville");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle avec une ville vide")
    void shouldRejectPresentielFormationWithEmptyVille() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .ville("")
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("La ville est obligatoire pour une formation en présentiel");
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle avec une ville contenant uniquement des espaces")
    void shouldRejectPresentielFormationWithBlankVille() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .ville(" ")
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("La ville est obligatoire pour une formation en présentiel");
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle sans lieu")
    void shouldRejectPresentielFormationWithoutLieu() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .lieu(null)
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lieu est obligatoire pour une formation en présentiel");
        verify(violationBuilder).addPropertyNode("lieu");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle avec un lieu vide")
    void shouldRejectPresentielFormationWithEmptyLieu() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .lieu("")
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lieu est obligatoire pour une formation en présentiel");
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle avec un lieu contenant uniquement des espaces")
    void shouldRejectPresentielFormationWithBlankLieu() {
        CreateFormationRequest request = CreateFormationRequest.builder()
            .modalite(ModaliteFormation.PRESENTIEL)
            .lieu("   ")
            .ville("Paris")
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lieu est obligatoire pour une formation en présentiel");
    }

    @Test
    @DisplayName("Doit rejeter une formation présentielle sans lieu ni ville")
    void shouldRejectPresentielFormationWithoutLieuAndVille() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest()
            .toBuilder()
            .lieu(null)
            .ville(null)
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context, times(2)).buildConstraintViolationWithTemplate(any(String.class));
        verify(violationBuilder).addPropertyNode("ville");
        verify(violationBuilder).addPropertyNode("lieu");
        verify(nodeBuilder, times(2)).addConstraintViolation();
    }

    @Test
    @DisplayName("Doit valider une formation en ligne avec un lien de participation valide")
    void shouldValidateOnlineFormationWithValidLienParticipation() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
        verify(context).disableDefaultConstraintViolation();
        verifyNoMoreInteractions(context);
    }

    @Test
    @DisplayName("Doit rejeter une formation en ligne sans lien de participation")
    void shouldRejectOnlineFormationWithoutLienParticipation() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
            .lienParticipation(null)
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lien de participation est obligatoire pour une formation en ligne");
        verify(violationBuilder).addPropertyNode("lienParticipation");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("Doit rejeter une formation en ligne avec un lien de participation vide")
    void shouldRejectOnlineFormationWithEmptyLienParticipation() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
            .lienParticipation("")
            .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lien de participation est obligatoire pour une formation en ligne");
    }

    @Test
    @DisplayName("Doit rejeter une formation en ligne avec un lien de participation contenant uniquement des espaces")
    void shouldRejectOnlineFormationWithBlankLienParticipation() {
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .lienParticipation("  ")
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Le lien de participation est obligatoire pour une formation en ligne");
    }

    @Test
    @DisplayName("Doit valider une formation en ligne avec des champs supplémentaires")
    void shouldValidateOnlineFormationWithExtraFields() {
        // Test d'une formation en ligne avec des champs de présentiel (qui seront ignorés)
        CreateFormationRequest request = createDefaultFormationEnLigneRequest().toBuilder()
                .lieu("1 rue Serpentine")
                .ville("Courbevoie")
                .build();

        boolean result = validator.isValid(request, context);

        assertThat(result).isTrue();
        verify(context).disableDefaultConstraintViolation();
        verifyNoMoreInteractions(context);
    }

}