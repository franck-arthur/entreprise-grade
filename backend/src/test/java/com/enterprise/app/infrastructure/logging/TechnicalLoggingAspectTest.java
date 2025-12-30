package com.enterprise.app.infrastructure.logging;

import com.enterprise.app.testing.helpers.BaseUnitTest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicalLoggingAspectTest extends BaseUnitTest {

    @InjectMocks
    private TechnicalLoggingAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private TechnicalLogging technicalLogging;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(TechnicalLoggingAspect.class);
        logger.setLevel(Level.DEBUG);

        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        logger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("Doit logger l'entrée et la sortie de méthode quand l'annotation est présente")
    void shouldLogMethodEntryAndExit() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(joinPoint.proceed()).thenReturn("test result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(result).isEqualTo("test result");
        verify(joinPoint).proceed();

        // Vérifier les logs
        assertThat(logAppender.list).hasSize(2);

        // Log d'entrée
        ILoggingEvent entryLog = logAppender.list.get(0);
        assertThat(entryLog.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(entryLog.getFormattedMessage()).contains("TestClass.testMethod");
        assertThat(entryLog.getFormattedMessage()).contains("→");

        // Log de sortie
        ILoggingEvent exitLog = logAppender.list.get(1);
        assertThat(exitLog.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(exitLog.getFormattedMessage()).contains("TestClass.testMethod");
        assertThat(exitLog.getFormattedMessage()).contains("←");
        assertThat(exitLog.getFormattedMessage()).contains("test result");
        assertThat(exitLog.getFormattedMessage()).contains("took");
    }

    @Test
    @DisplayName("Doit logger l'erreur quand la méthode lève une exception")
    void shouldLogErrorWhenMethodThrowsException() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();

        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThatThrownBy(() -> aspect.logMethodExecution(joinPoint, technicalLogging))
            .isEqualTo(exception);

        // Vérifier les logs (entrée + erreur)
        assertThat(logAppender.list).hasSize(2);

        // Log d'erreur
        ILoggingEvent errorLog = logAppender.list.get(1);
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage()).contains("TestClass.testMethod failed");
        assertThat(errorLog.getFormattedMessage()).contains("RuntimeException: Test error");
        assertThat(errorLog.getFormattedMessage()).contains("✗");
    }

    @Test
    @DisplayName("Doit masquer les paramètres sensibles")
    void shouldMaskSensitiveParameters() throws Throwable {
        // Given
        setupJoinPointWithSensitiveParams();
        setupTechnicalLoggingWithArgs();
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent entryLog = logAppender.list.get(0);
        assertThat(entryLog.getFormattedMessage()).contains("password=***");
        assertThat(entryLog.getFormattedMessage()).contains("username=testuser");
        assertThat(entryLog.getFormattedMessage()).doesNotContain("secret123");
    }

    @Test
    @DisplayName("Doit ne pas masquer les paramètres sensibles quand désactivé")
    void shouldNotMaskSensitiveParametersWhenDisabled() throws Throwable {
        // Given
        setupJoinPointWithSensitiveParams();
        setupTechnicalLoggingWithArgs();
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent entryLog = logAppender.list.get(0);
        assertThat(entryLog.getFormattedMessage()).contains("password=secret123");
        assertThat(entryLog.getFormattedMessage()).contains("username=testuser");
    }

    @Test
    @DisplayName("Doit ne pas logger quand la condition SpEL est fausse")
    void shouldNotLogWhenSpelConditionIsFalse() throws Throwable {
        // Given
        when(technicalLogging.condition()).thenReturn("#args.length > 5");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("Doit gérer gracieusement les conditions SpEL invalides")
    void shouldHandleInvalidSpelConditionGracefully() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(technicalLogging.condition()).thenReturn("invalid.spel.expression");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();

        // Should log warning about invalid SpEL and still proceed with logging
        boolean hasSpelWarning = logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Failed to evaluate logging condition"));
        assertThat(hasSpelWarning).isTrue();
    }

    @Test
    @DisplayName("Doit ne pas logger quand le niveau est OFF")
    void shouldNotLogWhenLevelIsOff() throws Throwable {
        // Given
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("Doit définir et nettoyer le contexte MDC")
    void shouldSetAndClearMdcContext() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - MDC should be cleared after execution
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("method")).isNull();
    }

    private void setupBasicJoinPoint() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
    }

    private void setupJoinPointWithSensitiveParams() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithParams");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testuser", "secret123"});

        try {
            TestClass testClass = new TestClass();
            when(joinPoint.getTarget().getClass().getMethod("testMethodWithParams", String.class, String.class))
                .thenReturn(TestClass.class.getMethod("testMethodWithParams", String.class, String.class));
        } catch (NoSuchMethodException e) {
            // Mock fallback
        }
    }

    private void setupTechnicalLogging() {
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.ERROR);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(true);
        when(technicalLogging.includeExecutionTime()).thenReturn(true);
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
    }

    private void setupTechnicalLoggingWithArgs() {
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.ERROR);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
    }

    // Classe de test pour les mocks
    public static class TestClass {
        public String testMethod() {
            return "result";
        }

        public String testMethodWithParams(String username, String password) {
            return "result";
        }
    }
}