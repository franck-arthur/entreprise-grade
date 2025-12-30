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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.ERROR);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
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
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.ERROR);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
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
        setupBasicJoinPoint();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(true);
        when(technicalLogging.includeExecutionTime()).thenReturn(true);
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(technicalLogging.prefix()).thenReturn("");
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

    @Test
    @DisplayName("Doit ne pas logger quand l'annotation est null")
    void shouldNotLogWhenAnnotationIsNull() throws Throwable {
        // Given
        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getTarget()).thenReturn(new TestClass());

        // When
        Object result = aspect.logMethodExecution(joinPoint, null);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("Doit utiliser l'annotation au niveau classe quand l'annotation méthode est null")
    void shouldUseClassLevelAnnotationWhenMethodAnnotationIsNull() throws Throwable {
        // Given
        TestClassWithAnnotation testInstance = new TestClassWithAnnotation();
        when(joinPoint.getTarget()).thenReturn(testInstance);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClassWithAnnotation.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, null);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        // Should log because class has annotation with default values
        assertThat(logAppender.list).hasSize(2); // entry + exit
    }

    @Test
    @DisplayName("Doit logger avec prefix personnalisé")
    void shouldLogWithCustomPrefix() throws Throwable {
        // Given
        setupBasicJoinPoint();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.ERROR);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(true);
        when(technicalLogging.includeExecutionTime()).thenReturn(true);
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(technicalLogging.prefix()).thenReturn("[CUSTOM]");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(2);
        ILoggingEvent entryLog = logAppender.list.get(0);
        assertThat(entryLog.getFormattedMessage()).startsWith("[CUSTOM] |");
    }

    @Test
    @DisplayName("Doit logger à différents niveaux")
    void shouldLogAtDifferentLevels() throws Throwable {
        // Given - Test TRACE level
        setupBasicJoinPoint();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.TRACE);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.INFO);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.WARN);
        when(technicalLogging.includeArgs()).thenReturn(false);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // Set logger to TRACE to capture all levels
        logger.setLevel(Level.TRACE);

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(2);
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.TRACE);
        assertThat(logAppender.list.get(1).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    @DisplayName("Doit formater les arguments vides")
    void shouldFormatEmptyArguments() throws Throwable {
        // Given
        setupBasicJoinPoint();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("()");
    }

    @Test
    @DisplayName("Doit formater les valeurs nulles")
    void shouldFormatNullValues() throws Throwable {
        // Given
        setupJoinPointWithNullArgs();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(true);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn(null);

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(2);
        // Entry log should show null args
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("arg0=null");
        // Exit log should show null result
        assertThat(logAppender.list.get(1).getFormattedMessage()).contains("→ null");
    }

    @Test
    @DisplayName("Doit tronquer les valeurs longues")
    void shouldTruncateLongValues() throws Throwable {
        // Given
        setupJoinPointWithLongValue();
        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(true);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");

        String longResult = "x".repeat(150);
        when(joinPoint.proceed()).thenReturn(longResult);

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(2);
        // Entry log should show truncated arg
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("...");
        // Exit log should show truncated result
        assertThat(logAppender.list.get(1).getFormattedMessage()).contains("→ " + "x".repeat(97) + "...");
    }

    @Test
    @DisplayName("Doit évaluer une condition SpEL valide qui retourne true")
    void shouldEvaluateValidSpelConditionReturningTrue() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(technicalLogging.condition()).thenReturn("#args.length == 0"); // Should be true
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        assertThat(logAppender.list).hasSize(2); // Should log because condition is true
    }

    @Test
    @DisplayName("Doit gérer les conditions SpEL qui retournent null")
    void shouldHandleSpelConditionReturningNull() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(technicalLogging.condition()).thenReturn("#someNullValue"); // Will return null
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        assertThat(logAppender.list).isEmpty(); // Should not log when condition returns null
    }

    @Test
    @DisplayName("Doit nettoyer MDC même en cas d'erreur")
    void shouldClearMdcEvenOnError() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThatThrownBy(() -> aspect.logMethodExecution(joinPoint, technicalLogging))
            .isEqualTo(exception);

        // MDC should be cleared even after exception
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("method")).isNull();
    }

    @Test
    @DisplayName("Doit détecteur paramètres sensibles avec patterns variés")
    void shouldDetectSensitiveParametersWithVariousPatterns() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithSensitiveParams");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key123", "token456", "secret789", "normalValue"});

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(true);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("arg0=***"); // "key" pattern
        assertThat(logMessage).contains("arg1=***"); // "token" pattern
        assertThat(logMessage).contains("arg2=***"); // "secret" pattern
        assertThat(logMessage).contains("arg3=normalValue"); // no sensitive pattern
    }

    @Test
    @DisplayName("Doit créer un correlation ID unique pour chaque appel")
    void shouldCreateUniqueCorrelationIdForEachCall() throws Throwable {
        // Given
        setupBasicJoinPoint();
        setupTechnicalLogging();
        when(joinPoint.proceed()).thenReturn("result");

        // When - Premier appel
        aspect.logMethodExecution(joinPoint, technicalLogging);
        String firstCorrelationId = extractCorrelationId(logAppender.list.get(0));

        logAppender.list.clear();

        // When - Deuxième appel
        aspect.logMethodExecution(joinPoint, technicalLogging);
        String secondCorrelationId = extractCorrelationId(logAppender.list.get(0));

        // Then
        assertThat(firstCorrelationId).isNotEqualTo(secondCorrelationId);
        assertThat(firstCorrelationId).hasSize(8); // UUID substring
        assertThat(secondCorrelationId).hasSize(8);
    }

    @Test
    @DisplayName("Doit tester la résolution de méthodes avec signatures exactes")
    void shouldTestMethodResolutionWithExactSignatures() throws Throwable {
        // Given
        TestClass testInstance = new TestClass();
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithPrimitives");
        when(joinPoint.getTarget()).thenReturn(testInstance);
        when(joinPoint.getArgs()).thenReturn(new Object[]{Integer.valueOf(42), Boolean.TRUE, Double.valueOf(3.14)});

        // Mock MethodSignature pour retourner la vraie méthode
        try {
            Method realMethod = TestClass.class.getMethod("testMethodWithPrimitives", Integer.class, Boolean.class, Double.class);
            when(methodSignature.getMethod()).thenReturn(realMethod);
        } catch (NoSuchMethodException e) {
            // fallback
        }

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should have resolved method correctly
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("intValue=42");
        assertThat(logMessage).contains("boolValue=true");
        assertThat(logMessage).contains("doubleValue=3.14");
    }

    @Test
    @DisplayName("Doit tester la résolution de méthodes avec paramètres incompatibles")
    void shouldTestMethodResolutionWithIncompatibleParameters() throws Throwable {
        // Given - Mock une signature qui ne correspond à aucune méthode
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("nonExistentMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"incompatible", Integer.valueOf(123)});

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should fallback to generic arg names
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("arg0=incompatible");
        assertThat(logMessage).contains("arg1=123");
    }

    @Test
    @DisplayName("Doit tester la compatibilité des types boxés avec des primitives")
    void shouldTestBoxingCompatibilityWithMixedTypes() throws Throwable {
        // Given - Setup avec types mixtes pour déclencher les vérifications de boxing
        TestClassWithPrimitives testInstance = new TestClassWithPrimitives();
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClassWithPrimitives.class);
        when(methodSignature.getName()).thenReturn("methodWithMixedPrimitives");
        when(joinPoint.getTarget()).thenReturn(testInstance);

        // Mix de wrappers et primitives pour couvrir isBoxingCompatible
        when(joinPoint.getArgs()).thenReturn(new Object[]{
            Integer.valueOf(42),    // Integer -> int
            Long.valueOf(123L),     // Long -> long
            Boolean.TRUE,           // Boolean -> boolean
            Double.valueOf(3.14),   // Double -> double
            Float.valueOf(2.5f),    // Float -> float
            Short.valueOf((short)10), // Short -> short
            Byte.valueOf((byte)5),  // Byte -> byte
            Character.valueOf('A')  // Character -> char
        });

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should work with boxing
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("42");
        assertThat(logMessage).contains("123");
        assertThat(logMessage).contains("true");
        assertThat(logMessage).contains("3.14");
        assertThat(logMessage).contains("2.5");
        assertThat(logMessage).contains("10");
        assertThat(logMessage).contains("5");
        assertThat(logMessage).contains("A");
    }

    @Test
    @DisplayName("Doit tester getMethod avec signature non-MethodSignature")
    void shouldTestGetMethodWithNonMethodSignature() throws Throwable {
        // Given - Mock une signature qui n'est pas MethodSignature
        org.aspectj.lang.Signature nonMethodSig = mock(org.aspectj.lang.Signature.class);
        when(nonMethodSig.getName()).thenReturn("testMethod");
        when(nonMethodSig.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(joinPoint.getSignature()).thenReturn(nonMethodSig);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should still work but use fallback method resolution
        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("()");
    }

    @Test
    @DisplayName("Doit tester avec null dans les arguments pour types incompatibles")
    void shouldTestMethodCompatibilityWithNullArgs() throws Throwable {
        // Given - setup pour tester la compatibilité avec null
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithParams");
        when(joinPoint.getTarget()).thenReturn(new TestClass());

        // Args avec null pour tester le cas "null compatible avec types non-primitifs"
        when(joinPoint.getArgs()).thenReturn(new Object[]{null, "valid"});

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should handle null correctly
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("arg0=null");
        assertThat(logMessage).contains("arg1=valid");
    }

    @Test
    @DisplayName("Doit tester getMethod avec paramètres de taille différente")
    void shouldTestGetMethodWithDifferentParameterSize() throws Throwable {
        // Given - Method avec un nombre différent de paramètres
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        // Passer des arguments alors que testMethod() n'en attend pas - cela devrait faire échouer la résolution
        when(joinPoint.getArgs()).thenReturn(new Object[]{"unexpected", "params"});

        when(technicalLogging.entryLevel()).thenReturn(TechnicalLogging.LogLevel.DEBUG);
        when(technicalLogging.exitLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.errorLevel()).thenReturn(TechnicalLogging.LogLevel.OFF);
        when(technicalLogging.includeArgs()).thenReturn(true);
        when(technicalLogging.includeResult()).thenReturn(false);
        when(technicalLogging.includeExecutionTime()).thenReturn(false);
        when(technicalLogging.maskSensitiveData()).thenReturn(false);
        when(technicalLogging.prefix()).thenReturn("");
        when(technicalLogging.condition()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.logMethodExecution(joinPoint, technicalLogging);

        // Then - should fallback to generic parameter names
        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("arg0=unexpected");
        assertThat(logMessage).contains("arg1=params");
    }

    private String extractCorrelationId(ILoggingEvent logEvent) {
        String message = logEvent.getFormattedMessage();
        int startIndex = message.indexOf("[") + 1;
        int endIndex = message.indexOf("]");
        return message.substring(startIndex, endIndex);
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
    }

    private void setupJoinPointWithNullArgs() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithNullArg");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{null});
    }

    private void setupJoinPointWithLongValue() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class<?>) TestClass.class);
        when(methodSignature.getName()).thenReturn("testMethodWithLongArg");
        when(joinPoint.getTarget()).thenReturn(new TestClass());

        String longString = "x".repeat(150);
        when(joinPoint.getArgs()).thenReturn(new Object[]{longString});
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

    // Classes de test pour les mocks
    public static class TestClass {
        public String testMethod() {
            return "result";
        }

        public String testMethodWithParams(String username, String password) {
            return "result";
        }

        public String testMethodWithNullArg(String arg) {
            return "result";
        }

        public String testMethodWithLongArg(String longArg) {
            return "result";
        }

        public String testMethodWithSensitiveParams(String apiKey, String userToken, String secretValue, String normalParam) {
            return "result";
        }

        public String testMethodWithPrimitives(Integer intValue, Boolean boolValue, Double doubleValue) {
            return "result";
        }
    }

    public static class TestClassWithPrimitives {
        public String methodWithMixedPrimitives(int intVal, long longVal, boolean boolVal, double doubleVal,
                                               float floatVal, short shortVal, byte byteVal, char charVal) {
            return "result";
        }
    }

    @TechnicalLogging
    public static class TestClassWithAnnotation {
        public String testMethod() {
            return "result";
        }
    }
}