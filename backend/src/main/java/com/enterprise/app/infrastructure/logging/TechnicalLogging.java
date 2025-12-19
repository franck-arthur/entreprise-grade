package com.enterprise.app.infrastructure.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour activer le logging technique automatique via AOP.
 *
 * Cette annotation permet de configurer finement le logging technique
 * sans polluer le code métier avec des appels explicites à log.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TechnicalLogging {

    /**
     * Niveau de log pour l'entrée de méthode.
     */
    LogLevel entryLevel() default LogLevel.DEBUG;

    /**
     * Niveau de log pour la sortie de méthode.
     */
    LogLevel exitLevel() default LogLevel.DEBUG;

    /**
     * Niveau de log pour les erreurs.
     */
    LogLevel errorLevel() default LogLevel.ERROR;

    /**
     * Inclure les paramètres d'entrée dans les logs.
     */
    boolean includeArgs() default true;

    /**
     * Inclure la valeur de retour dans les logs.
     */
    boolean includeResult() default false;

    /**
     * Inclure le temps d'exécution.
     */
    boolean includeExecutionTime() default true;

    /**
     * Masquer les paramètres sensibles (mots de passe, etc.).
     */
    boolean maskSensitiveData() default true;

    /**
     * Préfixe personnalisé pour les messages de log.
     */
    String prefix() default "";

    /**
     * Activer le logging uniquement si cette condition est vraie.
     * Utilise SpEL (Spring Expression Language).
     */
    String condition() default "";

    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, OFF
    }
}