# Architecture Hybride des Logs

## Vue d'ensemble

Cette solution implémente une approche hybride optimale combinant :
- **Logs techniques automatiques** via AOP pour un code métier pur
- **Logs métier explicites** pour la traçabilité et conformité

## Architecture Hybride

### 1. Logs Techniques Automatiques (AOP)

#### Mécanisme : `@TechnicalLogging`
- **Fonctionnement** : Aspect AOP interceptant les méthodes annotées
- **Sortie** : Console + `logs/application.log` (30j, 1GB)
- **Format** : Logs structurés avec ID de corrélation
- **Avantages** : Code métier pur, configuration déclarative

#### Fonctionnalités AOP
- Logs d'entrée/sortie automatiques
- Mesure du temps d'exécution
- Masquage des données sensibles
- Conditions SpEL pour logging conditionnel
- ID de corrélation pour traçage

### 2. Logs Métier Explicites

#### Mécanisme : `BusinessAuditLogger`
- **Usage** : Traçabilité métier, audit, conformité
- **Sortie** : `logs/business-audit.log` (365j, 10GB)
- **Format** : JSON structuré avec MDC enrichi
- **Caractère** : Explicite et intentionnel

### 2. Configuration Logback

```xml
<!-- Logger métier séparé avec configuration dédiée -->
<logger name="BUSINESS_AUDIT" level="INFO" additivity="false">
    <appender-ref ref="ASYNC_BUSINESS_AUDIT"/>
</logger>

<!-- Appender spécifique avec rotation longue durée -->
<appender name="BUSINESS_AUDIT_FILE">
    <file>logs/business-audit.log</file>
    <maxHistory>365</maxHistory>
    <totalSizeCap>10GB</totalSizeCap>
</appender>
```

## Utilisation de l'Approche Hybride

### 1. Service Standard avec Logging Automatique

```java
@Service
@RequiredArgsConstructor
@TechnicalLogging(  // Activation des logs techniques automatiques
    entryLevel = TechnicalLogging.LogLevel.DEBUG,
    exitLevel = TechnicalLogging.LogLevel.DEBUG,
    includeExecutionTime = true,
    prefix = "USER_SERVICE"
)
public class UserService {

    private final BusinessAuditLogger businessAuditLogger;

    public User createUser(String username, String email) {
        // Code métier pur - AUCUN log technique manuel !
        validateUserData(username, email);

        User user = User.builder()
            .id(UUID.randomUUID())
            .username(username)
            .email(email)
            .build();

        // Seul le log métier est explicite pour la traçabilité
        businessAuditLogger.logBusinessAction(
            getCurrentUserId(),
            getCurrentUsername(),
            "USER_CREATION",
            "User",
            user.getId().toString(),
            String.format("Created user %s with email %s", username, email)
        );

        return user;
    }
    // L'AOP génère automatiquement :
    // [abc123] → USER_SERVICE | UserService.createUser(username=john, email=john@example.com)
    // [abc123] ← USER_SERVICE | UserService.createUser → User{id=..., username=john} (took 45ms)
}
```

### 2. Gestion des Opérations Sensibles

```java
@TechnicalLogging(
    entryLevel = TechnicalLogging.LogLevel.INFO,
    includeArgs = false,  // Masquer les paramètres sensibles
    includeResult = true,
    prefix = "SECURITY"
)
public boolean updateUserPassword(UUID userId, String oldPassword, String newPassword) {
    // Logique métier pure
    User user = findUserById(userId);
    validatePassword(oldPassword, user);
    user.updatePassword(newPassword);

    // Log métier pour traçabilité sécuritaire
    businessAuditLogger.logSecurityEvent(
        "PASSWORD_CHANGE",
        user.getUsername(),
        getCurrentUserIp(),
        true,
        "Password successfully updated"
    );

    return true;
    // AOP génère : [xyz] → SECURITY | updateUserPassword() (sans exposer les mots de passe)
}
```

### 3. Logging Conditionnel avec SpEL

```java
@TechnicalLogging(
    condition = "#roles.size() > 1",  // Log seulement si assignation multiple
    entryLevel = TechnicalLogging.LogLevel.WARN,
    prefix = "MULTI_ROLE"
)
public void assignRoles(UUID userId, List<String> roles) {
    User user = findUserById(userId);
    user.setRoles(roles);

    // Traçabilité métier obligatoire
    businessAuditLogger.logBusinessAction(
        getCurrentUserId(),
        getCurrentUsername(),
        "ROLE_ASSIGNMENT",
        "User",
        userId.toString(),
        "Assigned roles: " + String.join(", ", roles)
    );
}
```

### 4. Gestion d'Erreur avec Double Logging

```java
public void deleteUser(UUID userId) {
    try {
        User user = findUserById(userId);
        performUserDeletion(user);

        // Traçabilité métier du succès
        businessAuditLogger.logBusinessAction(
            getCurrentUserId(),
            getCurrentUsername(),
            "USER_DELETION",
            "User",
            userId.toString(),
            "User deleted: " + user.getUsername()
        );

    } catch (Exception ex) {
        // Traçabilité métier de l'erreur
        businessAuditLogger.logBusinessError(
            "USER_DELETION_FAILED",
            "DELETE_USER_OPERATION",
            ex.getMessage(),
            getCurrentUsername()
        );

        throw ex;  // AOP gère automatiquement l'erreur technique
    }
    // L'AOP génère automatiquement les logs d'entrée, sortie et erreur technique
}
```

### 2. Types de Logs Métier

#### Actions Utilisateur
```java
businessAuditLogger.logBusinessAction(
    userId,
    username,
    "USER_UPDATE",
    "User",
    targetUserId,
    "Updated user profile information"
);
```

#### Événements Sécuritaires
```java
businessAuditLogger.logSecurityEvent(
    "LOGIN_FAILURE",
    username,
    ipAddress,
    false,
    "Invalid password provided"
);
```

#### Processus Métier
```java
Map<String, Object> metadata = Map.of(
    "formationType", "TECHNICAL",
    "duration", "2 days",
    "participants", 15
);

businessAuditLogger.logBusinessProcess(
    "FORMATION_CREATED",
    formationId,
    "COMPLETED",
    initiatedBy,
    metadata
);
```

#### Erreurs Métier
```java
businessAuditLogger.logBusinessError(
    "VALIDATION_ERROR",
    "USER_REGISTRATION",
    "Email already exists in system",
    attemptedUsername
);
```

#### Événements Personnalisés
```java
Map<String, Object> context = Map.of(
    "reportType", "MONTHLY",
    "recordCount", 1500,
    "executionTime", "45s"
);

businessAuditLogger.logCustomBusinessEvent(
    "REPORT_GENERATED",
    context
);
```

## Intégration avec l'Audit Existant

La solution complète l'architecture d'audit existante :

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAuditService {

    private final AuditCommandService auditCommandService; // DB persistence
    private final BusinessAuditLogger businessAuditLogger; // File logging

    public void auditUserCreation(User performedBy, User targetUser, boolean success) {
        // 1. Log technique pour monitoring
        log.debug("Processing user creation audit...");

        // 2. Log métier pour traçabilité
        businessAuditLogger.logBusinessAction(/*...*/);

        // 3. Persistance en base (système existant)
        auditCommandService.recordEvent(createCommand);

        // 4. Confirmation technique
        log.info("User creation audit completed");
    }
}
```

## Structure des Logs Métier (JSON)

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "app": "enterprise-app",
  "version": "1.0.0",
  "environment": "prod",
  "logType": "BUSINESS_AUDIT",
  "level": "INFO",
  "message": "ACTION:USER_CREATION | USER:john.doe | ENTITY:User#123 | DETAILS:Created user with admin role",
  "mdc": {
    "businessEventType": "USER_ACTION",
    "userId": "456",
    "username": "admin.user",
    "entityType": "User",
    "entityId": "123",
    "sessionId": "sess_abc123"
  }
}
```

## Avantages de cette Architecture

### Séparation des Préoccupations
- **Logs techniques** : Développement, debugging, monitoring applicatif
- **Logs métier** : Compliance, audit, traçabilité légale

### Flexibilité de Gestion
- **Rétention différenciée** : 30j techniques vs 365j métier
- **Destinations séparées** : Monitoring vs archive légale
- **Niveaux adaptés** : DEBUG technique vs INFO métier

### Performance
- **Appenders asynchrones** pour éviter l'impact sur les performances
- **Buffers optimisés** (512 technique vs 1024 métier)

### Conformité
- **Format JSON structuré** pour l'analyse automatisée
- **MDC enrichi** avec contexte complet
- **Traçabilité complète** des actions utilisateur

## Migration des Services Existants

1. **Ajouter l'injection** du `BusinessAuditLogger`
2. **Identifier les logs métier** dans le code existant
3. **Migrer progressivement** vers le nouveau logger
4. **Conserver `@Slf4j`** pour les logs techniques

## Exemple Concret

```java
// AVANT
@Slf4j
public class UserService {
    public void createUser(User user) {
        log.info("Creating user: {}", user.getUsername()); // Mélange technique/métier
        // ...
    }
}

// APRÈS
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final BusinessAuditLogger businessAuditLogger;

    public void createUser(User user) {
        log.debug("Starting user creation process"); // Technique

        try {
            // ... logique

            businessAuditLogger.logBusinessAction( // Métier
                currentUser.getId().toString(),
                currentUser.getUsername(),
                "USER_CREATION",
                "User",
                user.getId().toString(),
                "Created user with roles: " + user.getRoles()
            );

            log.info("User creation completed successfully"); // Technique

        } catch (Exception ex) {
            log.error("User creation failed", ex); // Technique

            businessAuditLogger.logBusinessError( // Métier
                "USER_CREATION_FAILED",
                "USER_SERVICE",
                ex.getMessage(),
                currentUser.getUsername()
            );
        }
    }
}
```

## Avantages de l'Approche Hybride

### ✅ **Séparation des Préoccupations**
- **Code métier pur** : Aucun log technique manuel dans la logique business
- **Logs automatiques** : AOP gère transparemment les logs techniques
- **Traçabilité explicite** : BusinessAuditLogger pour les actions métier

### ⚡ **Performance et Maintenabilité**
- **Moins de boilerplate** : Réduction drastique du code répétitif
- **Configuration déclarative** : Paramétrage via annotations
- **Logs asynchrones** : Performance optimisée avec appenders async

### 🔒 **Sécurité et Conformité**
- **Masquage automatique** : Données sensibles protégées via AOP
- **Traçabilité complète** : Double logging technique + métier
- **Rétention différenciée** : 30j techniques vs 365j métier

## Structure des Logs

### Logs Techniques (AOP)
```
[correlationId] → PREFIX | ClassName.methodName(param1=value1, param2=***)
[correlationId] ← PREFIX | ClassName.methodName → ResultType{...} (took 45ms)
[correlationId] ✗ PREFIX | ClassName.methodName failed (after 12ms) - Exception: message
```

### Logs Métier (JSON Structuré)
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "app": "enterprise-app",
  "logType": "BUSINESS_AUDIT",
  "level": "INFO",
  "message": "ACTION:USER_CREATION | USER:admin.user | ENTITY:User#123",
  "mdc": {
    "businessEventType": "USER_ACTION",
    "userId": "456",
    "username": "admin.user",
    "entityType": "User",
    "entityId": "123"
  }
}
```

## Migration Recommandée

### 🚀 **Nouveaux Services**
1. Ajouter `@TechnicalLogging` au niveau classe
2. Injecter `BusinessAuditLogger`
3. Utiliser uniquement les logs métier explicites

### 🔄 **Services Existants**
1. **Analyser** les logs `@Slf4j` existants
2. **Identifier** les logs techniques vs métier
3. **Migrer** progressivement vers l'approche hybride
4. **Remplacer** les logs techniques par `@TechnicalLogging`
5. **Convertir** les logs métier vers `BusinessAuditLogger`

### ⚙️ **Configuration**
```java
// Activation AOP
@EnableAspectJAutoProxy

// Service avec logging hybride
@TechnicalLogging  // Logs techniques automatiques
public class MonService {
    private final BusinessAuditLogger businessAuditLogger; // Logs métier explicites
}
```

Cette approche hybride élimine la pollution du code tout en maintenant une traçabilité complète et performante.