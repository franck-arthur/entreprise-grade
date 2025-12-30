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
            Class<?> targetClass = joinPoint.getTarget().getClass();

            // Première tentative : utiliser la signature exacte si disponible
            if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
                org.aspectj.lang.reflect.MethodSignature methodSignature =
                    (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                return methodSignature.getMethod();
            }

            // Deuxième tentative : rechercher par nom et nombre de paramètres
            Object[] args = joinPoint.getArgs();
            Method[] methods = targetClass.getMethods();

            for (Method method : methods) {
                if (method.getName().equals(methodName) &&
                    method.getParameterCount() == args.length) {

                    // Vérifier la compatibilité des types de paramètres
                    if (isMethodCompatible(method, args)) {
                        return method;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Could not resolve method for logging aspect: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si une méthode est compatible avec les arguments fournis.
     */
    private boolean isMethodCompatible(Method method, Object[] args) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            Class<?> paramType = paramTypes[i];

            // null est compatible avec tous les types non-primitifs
            if (arg == null) {
                if (paramType.isPrimitive()) {
                    return false;
                }
                continue;
            }

            // Vérifier la compatibilité de type (incluant l'héritage)
            if (!paramType.isAssignableFrom(arg.getClass()) &&
                !isBoxingCompatible(paramType, arg.getClass())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie la compatibilité entre types primitifs et leurs wrappers.
     */
    private boolean isBoxingCompatible(Class<?> paramType, Class<?> argType) {
        if (paramType.isPrimitive()) {
            if (paramType == int.class && argType == Integer.class) return true;
            if (paramType == long.class && argType == Long.class) return true;
            if (paramType == boolean.class && argType == Boolean.class) return true;
            if (paramType == double.class && argType == Double.class) return true;
            if (paramType == float.class && argType == Float.class) return true;
            if (paramType == short.class && argType == Short.class) return true;
            if (paramType == byte.class && argType == Byte.class) return true;
            if (paramType == char.class && argType == Character.class) return true;
        } else if (argType.isPrimitive()) {
            if (argType == int.class && paramType == Integer.class) return true;
            if (argType == long.class && paramType == Long.class) return true;
            if (argType == boolean.class && paramType == Boolean.class) return true;
            if (argType == double.class && paramType == Double.class) return true;
            if (argType == float.class && paramType == Float.class) return true;
            if (argType == short.class && paramType == Short.class) return true;
            if (argType == byte.class && paramType == Byte.class) return true;
            if (argType == char.class && paramType == Character.class) return true;
        }
        return false;
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