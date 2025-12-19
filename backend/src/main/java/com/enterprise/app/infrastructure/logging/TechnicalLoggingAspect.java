package com.enterprise.app.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aspect AOP pour le logging technique automatique.
 *
 * Cet aspect intercepte les méthodes annotées avec @TechnicalLogging
 * et génère automatiquement les logs d'entrée, sortie et erreur.
 */
@Aspect
@Component
@Slf4j
public class TechnicalLoggingAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern SENSITIVE_PARAM_PATTERN =
        Pattern.compile(".*(password|pwd|secret|token|key|credential).*", Pattern.CASE_INSENSITIVE);

    /**
     * Pointcut pour les méthodes annotées avec @TechnicalLogging.
     */
    @Pointcut("@annotation(technicalLogging)")
    public void technicalLoggingMethod(TechnicalLogging technicalLogging) {}

    /**
     * Pointcut pour les classes annotées avec @TechnicalLogging.
     */
    @Pointcut("@within(technicalLogging) && execution(public * *(..))")
    public void technicalLoggingClass(TechnicalLogging technicalLogging) {}

    /**
     * Around advice pour le logging complet avec gestion des erreurs et timing.
     */
    @Around("technicalLoggingMethod(technicalLogging) || technicalLoggingClass(technicalLogging)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint, TechnicalLogging technicalLogging) throws Throwable {
        if (technicalLogging == null) {
            technicalLogging = getClassLevelAnnotation(joinPoint);
        }

        if (technicalLogging == null || !shouldLog(technicalLogging, joinPoint)) {
            return joinPoint.proceed();
        }

        String methodName = getMethodName(joinPoint);
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        try {
            // Enrichir MDC
            MDC.put("correlationId", correlationId);
            MDC.put("method", methodName);

            // Log d'entrée
            logEntry(joinPoint, technicalLogging, correlationId);

            // Exécution de la méthode
            Object result = joinPoint.proceed();

            // Log de sortie
            logExit(joinPoint, technicalLogging, correlationId, result, startTime);

            return result;

        } catch (Throwable throwable) {
            // Log d'erreur
            logError(joinPoint, technicalLogging, correlationId, throwable, startTime);
            throw throwable;

        } finally {
            MDC.remove("correlationId");
            MDC.remove("method");
        }
    }

    /**
     * Log d'entrée de méthode.
     */
    private void logEntry(JoinPoint joinPoint, TechnicalLogging config, String correlationId) {
        if (config.entryLevel() == TechnicalLogging.LogLevel.OFF) return;

        String methodName = getMethodName(joinPoint);
        String args = config.includeArgs() ? formatArguments(joinPoint, config.maskSensitiveData()) : "";
        String prefix = StringUtils.hasText(config.prefix()) ? config.prefix() + " | " : "";

        String message = String.format("%s[%s] → %s%s", prefix, correlationId, methodName, args);
        logAtLevel(config.entryLevel(), message);
    }

    /**
     * Log de sortie de méthode.
     */
    private void logExit(JoinPoint joinPoint, TechnicalLogging config, String correlationId,
                        Object result, long startTime) {
        if (config.exitLevel() == TechnicalLogging.LogLevel.OFF) return;

        String methodName = getMethodName(joinPoint);
        String resultStr = config.includeResult() ? formatResult(result) : "";
        String executionTime = config.includeExecutionTime() ?
            String.format(" (took %dms)", System.currentTimeMillis() - startTime) : "";
        String prefix = StringUtils.hasText(config.prefix()) ? config.prefix() + " | " : "";

        String message = String.format("%s[%s] ← %s%s%s", prefix, correlationId, methodName, resultStr, executionTime);
        logAtLevel(config.exitLevel(), message);
    }

    /**
     * Log d'erreur.
     */
    private void logError(JoinPoint joinPoint, TechnicalLogging config, String correlationId,
                         Throwable throwable, long startTime) {
        if (config.errorLevel() == TechnicalLogging.LogLevel.OFF) return;

        String methodName = getMethodName(joinPoint);
        String executionTime = config.includeExecutionTime() ?
            String.format(" (failed after %dms)", System.currentTimeMillis() - startTime) : "";
        String prefix = StringUtils.hasText(config.prefix()) ? config.prefix() + " | " : "";

        String message = String.format("%s[%s] ✗ %s failed%s - %s: %s",
            prefix, correlationId, methodName, executionTime,
            throwable.getClass().getSimpleName(), throwable.getMessage());

        logAtLevel(config.errorLevel(), message, throwable);
    }

    /**
     * Évalue la condition SpEL pour déterminer si le logging doit être activé.
     */
    private boolean shouldLog(TechnicalLogging config, JoinPoint joinPoint) {
        if (!StringUtils.hasText(config.condition())) {
            return true;
        }

        try {
            Expression expression = parser.parseExpression(config.condition());
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Ajouter les arguments au contexte SpEL
            Object[] args = joinPoint.getArgs();
            context.setVariable("args", args);
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
            }

            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;

        } catch (Exception ex) {
            log.warn("Failed to evaluate logging condition '{}': {}", config.condition(), ex.getMessage());
            return true; // Par défaut, logger si l'évaluation échoue
        }
    }

    /**
     * Formate les arguments de méthode pour l'affichage.
     */
    private String formatArguments(JoinPoint joinPoint, boolean maskSensitive) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(");
        Method method = getMethod(joinPoint);
        Parameter[] parameters = method != null ? method.getParameters() : new Parameter[0];

        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            String paramName = i < parameters.length ? parameters[i].getName() : "arg" + i;
            Object value = args[i];

            if (maskSensitive && isSensitiveParameter(paramName)) {
                sb.append(paramName).append("=***");
            } else {
                sb.append(paramName).append("=").append(formatValue(value));
            }
        }

        return sb.append(")").toString();
    }

    /**
     * Formate la valeur de retour.
     */
    private String formatResult(Object result) {
        if (result == null) {
            return " → null";
        }
        return " → " + formatValue(result);
    }

    /**
     * Formate une valeur pour l'affichage (avec limitation de taille).
     */
    private String formatValue(Object value) {
        if (value == null) return "null";

        String str = value.toString();
        if (str.length() > 100) {
            return str.substring(0, 97) + "...";
        }
        return str;
    }

    /**
     * Détermine si un paramètre est sensible (mot de passe, etc.).
     */
    private boolean isSensitiveParameter(String paramName) {
        return SENSITIVE_PARAM_PATTERN.matcher(paramName).matches();
    }

    /**
     * Obtient le nom complet de la méthode.
     */
    private String getMethodName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName() +
               "." + joinPoint.getSignature().getName();
    }

    /**
     * Obtient la méthode Java correspondante.
     */
    private Method getMethod(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] paramTypes = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg != null ? arg.getClass() : Object.class)
                .toArray(Class<?>[]::new);
            return joinPoint.getTarget().getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Obtient l'annotation au niveau de la classe.
     */
    private TechnicalLogging getClassLevelAnnotation(JoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getAnnotation(TechnicalLogging.class);
    }

    /**
     * Log au niveau spécifié.
     */
    private void logAtLevel(TechnicalLogging.LogLevel level, String message) {
        logAtLevel(level, message, null);
    }

    private void logAtLevel(TechnicalLogging.LogLevel level, String message, Throwable throwable) {
        switch (level) {
            case TRACE:
                if (throwable != null) log.trace(message, throwable);
                else log.trace(message);
                break;
            case DEBUG:
                if (throwable != null) log.debug(message, throwable);
                else log.debug(message);
                break;
            case INFO:
                if (throwable != null) log.info(message, throwable);
                else log.info(message);
                break;
            case WARN:
                if (throwable != null) log.warn(message, throwable);
                else log.warn(message);
                break;
            case ERROR:
                if (throwable != null) log.error(message, throwable);
                else log.error(message);
                break;
            case OFF:
            default:
                // Ne rien faire
                break;
        }
    }
}