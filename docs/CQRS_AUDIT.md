# CQRS Audit System - Command Query Responsibility Segregation

## Overview

This implementation demonstrates the **CQRS (Command Query Responsibility Segregation)** pattern through a real-world enterprise use case: **Audit Logging System**.

### What is CQRS?

CQRS is an architectural pattern that separates read and write operations into different models:
- **Command Model** (Write Side): Optimized for data modifications (INSERT, UPDATE, DELETE)
- **Query Model** (Read Side): Optimized for data retrieval and complex queries

### Why Use CQRS?

✅ **Performance**: Separate optimization for reads and writes
✅ **Scalability**: Scale read and write models independently
✅ **Complexity Management**: Handle complex business logic separately
✅ **Security**: Different access patterns for commands vs queries
✅ **Audit & Compliance**: Perfect for event sourcing and audit trails

### Real-World Use Case: Audit Logging

Audit logging is a perfect CQRS use case because:
- **Writes are append-only** (never update/delete audit events)
- **Reads are complex** (filtering, aggregations, statistics)
- **Different access patterns** (fast writes, complex queries)
- **Compliance requirements** (GDPR, ISO 27001, SOC 2)

## Architecture

### CQRS Pattern Implementation

```
┌─────────────────────────────────────────────────────────────────┐
│                        APPLICATION LAYER                          │
│                                                                   │
│  ┌────────────────────────┐      ┌─────────────────────────┐   │
│  │  AuditCommandService   │      │  AuditQueryService      │   │
│  │  (WRITE operations)    │      │  (READ operations)      │   │
│  │                        │      │                         │   │
│  │  - recordEvent()       │      │  - findEvents()         │   │
│  │  - projectEventAsync() │      │  - getStatistics()      │   │
│  └────────┬───────────────┘      └───────────┬─────────────┘   │
│           │                                   │                  │
│           │                                   │                  │
└───────────┼───────────────────────────────────┼──────────────────┘
            │                                   │
            ▼                                   ▼
┌───────────────────────┐          ┌──────────────────────────┐
│  COMMAND MODEL        │          │  QUERY MODEL             │
│  (Write-Optimized)    │          │  (Read-Optimized)        │
│                       │          │                          │
│  AuditEventCommand    │──Async──▶│  AuditEventProjection    │
│                       │Projection│                          │
│  - Minimal indexes    │          │  - Multiple indexes      │
│  - Fast inserts       │          │  - Denormalized fields   │
│  - Simple structure   │          │  - Aggregation support   │
│                       │          │  - Complex queries       │
└───────────────────────┘          └──────────────────────────┘
     │                                        │
     │ audit_events_command                   │ audit_events_projection
     ▼                                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATABASE                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. Domain Models

**Command Model** (`AuditEventCommand.java`):
```java
@Entity
@Table(name = "audit_events_command")
public class AuditEventCommand {
    // Write-optimized: minimal indexes, fast inserts
    @Index(name = "idx_audit_cmd_timestamp", columnList = "timestamp")
}
```

**Query Model** (`AuditEventProjection.java`):
```java
@Entity
@Table(
    name = "audit_events_projection",
    indexes = {
        @Index(name = "idx_audit_proj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_proj_user", columnList = "user_id, timestamp"),
        @Index(name = "idx_audit_proj_type", columnList = "event_type, timestamp"),
        @Index(name = "idx_audit_proj_entity", columnList = "target_entity_type, target_entity_id"),
        // ... more indexes for different query patterns
    }
)
public class AuditEventProjection {
    // Read-optimized: multiple indexes, denormalized fields
    private String eventCategory;      // Denormalized
    private String targetEntityName;   // Denormalized
    private Date eventDate;            // Denormalized for date queries
    private Integer eventHour;         // Denormalized for hourly aggregations
}
```

#### 2. Services

**Command Service** (`AuditCommandService.java`):
- Handles all write operations
- Saves to command model first (fast write)
- Asynchronously projects to query model
- Ensures eventual consistency

```java
@Service
public class AuditCommandService {
    @Transactional
    public AuditEventCommand recordEvent(CreateAuditEventCommand command) {
        // 1. Save to command model (fast write)
        AuditEventCommand event = commandRepository.save(auditEvent);

        // 2. Project to query model asynchronously
        projectEventAsync(event, targetEntityName);

        return event;
    }

    @Async("batchImportExecutor")
    @Transactional
    public void projectEventAsync(AuditEventCommand commandEvent, String targetEntityName) {
        // Create read-optimized projection
        AuditEventProjection projection = AuditEventProjection.fromCommand(commandEvent);

        // Add denormalized fields
        projection.setEventCategory(...);
        projection.setEventDate(...);

        projectionRepository.save(projection);
    }
}
```

**Query Service** (`AuditQueryService.java`):
- Handles all read operations
- Queries from projection model only
- Supports complex filtering and aggregations
- Uses caching for performance

```java
@Service
@Transactional(readOnly = true)
public class AuditQueryService {
    public Page<AuditEventDTO> findEvents(AuditEventQuery query, Pageable pageable) {
        // Query using read-optimized projection with indexes
        return projectionRepository.findByFilters(
            query.getEventTypes(),
            query.getEventCategory(),
            query.getUserId(),
            // ... multiple filter criteria
            pageable
        );
    }

    @Cacheable("auditStatistics")
    public AuditStatisticsDTO getStatistics() {
        // Complex aggregations without impacting writes
        // Uses multiple grouped queries on indexed fields
    }
}
```

#### 3. Repositories

**Command Repository**:
```java
public interface AuditEventCommandRepository extends JpaRepository<AuditEventCommand, UUID> {
    // Minimal methods - only for writing
    // No complex queries
}
```

**Query Repository**:
```java
public interface AuditEventProjectionRepository extends JpaRepository<AuditEventProjection, UUID> {
    // Complex query methods
    Page<AuditEventProjection> findByFilters(...);
    List<Object[]> countByEventTypeGrouped();
    List<Object[]> countByHourForDate(Date date);
    List<Object[]> findTopUsersByEventCount(Pageable pageable);
    // ... many more query methods
}
```

## CQRS Principles Demonstrated

### 1. Separate Models

**Write Model (Command)**:
- Simple structure
- Append-only (no updates/deletes)
- Minimal indexes
- Fast inserts

**Read Model (Projection)**:
- Complex structure
- Multiple indexes
- Denormalized data
- Optimized for queries

### 2. Eventual Consistency

Events are written to command model immediately, then **asynchronously** projected to query model:

```java
// Write happens immediately
AuditEventCommand savedEvent = commandRepository.save(auditEvent);

// Projection happens asynchronously (@Async)
projectEventAsync(savedEvent, targetEntityName);
```

This ensures:
- Fast write operations (no blocking)
- Resilience (write succeeds even if projection fails)
- Scalability (projections can run on separate threads/servers)

### 3. Different Access Patterns

**Command Side**:
```java
// Simple insert
auditCommandService.recordEvent(command);
```

**Query Side**:
```java
// Complex filtering
auditQueryService.findEvents(
    AuditEventQuery.builder()
        .eventTypes(List.of(USER_CREATED, USER_UPDATED))
        .eventCategory("USER")
        .userId(userId)
        .fromDate(startDate)
        .toDate(endDate)
        .success(true)
        .build(),
    pageable
);

// Aggregations
AuditStatisticsDTO stats = auditQueryService.getStatistics();
```

### 4. Independent Scaling

- Command side: Scale writes with connection pooling
- Query side: Scale reads with caching, read replicas, or separate database

### 5. Event Sourcing (Light)

Command model stores all events immutably:
- Complete audit trail
- Can rebuild query model from command model
- Time-travel debugging possible

```java
public void rebuildProjections() {
    // Rebuild entire query model from command model
    commandRepository.findAll().forEach(command -> {
        AuditEventProjection projection = AuditEventProjection.fromCommand(command);
        projectionRepository.save(projection);
    });
}
```

## API Endpoints

All endpoints are **read-only** (Query side):

```http
GET /api/v1/audit
  ?eventTypes=USER_CREATED,USER_UPDATED
  &eventCategory=USER
  &userId={uuid}
  &fromDate=2024-01-01T00:00:00
  &toDate=2024-12-31T23:59:59
  &success=true
  &page=0&size=50

GET /api/v1/audit/user/{userId}
GET /api/v1/audit/type/{eventType}
GET /api/v1/audit/entity/{entityType}/{entityId}
GET /api/v1/audit/statistics
GET /api/v1/audit/statistics/range?fromDate=...&toDate=...
GET /api/v1/audit/statistics/hourly?date=2024-01-15
```

**Note**: No POST/PUT/DELETE endpoints - commands are handled internally by services.

## Usage Example

### Recording Events (Command Side)

```java
@Service
public class UserService {
    private final AuditService auditService;

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // Business logic
        User user = userRepository.save(newUser);

        // Audit event (COMMAND)
        auditService.auditUserCreation(currentUser, user, true);

        return userMapper.toDTO(user);
    }
}
```

### Querying Events (Query Side)

```java
// Complex filtering
AuditEventQuery query = AuditEventQuery.builder()
    .eventTypes(List.of(USER_CREATED, USER_UPDATED, USER_DELETED))
    .eventCategory("USER")
    .userId(userId)
    .fromDate(LocalDateTime.now().minusDays(30))
    .success(true)
    .build();

Page<AuditEventDTO> events = auditQueryService.findEvents(query, pageable);

// Statistics and aggregations
AuditStatisticsDTO stats = auditQueryService.getStatistics();
System.out.println("Total events: " + stats.getTotalEvents());
System.out.println("Success rate: " +
    (stats.getSuccessfulEvents() * 100.0 / stats.getTotalEvents()) + "%");
```

## Event Types

The system tracks various event types:

- **User Management**: `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`, `USER_ACTIVATED`, `USER_DEACTIVATED`
- **Authentication**: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`, `PASSWORD_CHANGED`
- **Batch Operations**: `BATCH_IMPORT_STARTED`, `BATCH_IMPORT_COMPLETED`, `BATCH_IMPORT_FAILED`
- **Security**: `UNAUTHORIZED_ACCESS`, `FORBIDDEN_ACCESS`
- **System**: `SYSTEM_ERROR`, `CONFIGURATION_CHANGED`

## Database Schema

### Command Table (Write-Optimized)

```sql
CREATE TABLE audit_events_command (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    username VARCHAR(50),
    target_entity_type VARCHAR(100),
    target_entity_id UUID,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL,

    -- Minimal index for time-based queries
    INDEX idx_audit_cmd_timestamp (timestamp)
);
```

### Projection Table (Read-Optimized)

```sql
CREATE TABLE audit_events_projection (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50),          -- Denormalized
    user_id UUID,
    username VARCHAR(50),
    target_entity_type VARCHAR(100),
    target_entity_id UUID,
    target_entity_name VARCHAR(200),     -- Denormalized
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL,
    event_date DATE,                     -- Denormalized
    event_hour INTEGER,                  -- Denormalized

    -- Multiple indexes for different query patterns
    INDEX idx_audit_proj_timestamp (timestamp),
    INDEX idx_audit_proj_user (user_id, timestamp),
    INDEX idx_audit_proj_type (event_type, timestamp),
    INDEX idx_audit_proj_entity (target_entity_type, target_entity_id),
    INDEX idx_audit_proj_success (success, timestamp),
    INDEX idx_audit_proj_date (event_date),
    INDEX idx_audit_proj_category (event_category, timestamp)
);
```

## Benefits of CQRS for Audit Logging

### 1. Performance

**Write Performance**:
- Command model has minimal indexes (fast inserts)
- No complex joins or constraints
- Asynchronous projection doesn't block writes

**Read Performance**:
- Query model has multiple indexes for different patterns
- Denormalized data reduces joins
- Caching strategies possible

### 2. Scalability

- **Horizontal**: Read replicas for query model
- **Vertical**: Different hardware for command vs query
- **Async**: Projections can lag without impacting writes

### 3. Flexibility

- Add new query patterns without modifying command model
- Add new indexes for new reports without impacting writes
- Rebuild projections for new requirements

### 4. Compliance & Audit

- Immutable command model (perfect for audit)
- Complete event history (event sourcing)
- Can prove compliance with regulations

### 5. Debugging & Recovery

- Replay events from command model
- Rebuild corrupted query model
- Time-travel debugging

## Performance Considerations

### Write Path

1. **Command Model Insert**: ~1-2ms (single indexed insert)
2. **Async Projection**: 5-10ms (multiple indexes, denormalization)
3. **Total User Impact**: ~1-2ms (async projection doesn't block)

### Read Path

1. **Simple Query**: ~5-10ms (indexed read)
2. **Complex Filter**: ~20-50ms (multiple indexes)
3. **Aggregation**: ~50-200ms (depends on data volume, uses caching)

### Optimization Strategies

1. **Write Side**:
   - Batch projections for high-volume events
   - Use message queue for projections
   - Partition command table by time

2. **Read Side**:
   - Cache statistics and aggregations
   - Pre-compute common queries
   - Use materialized views
   - Read replicas for scaling

## CQRS vs Traditional CRUD

### Traditional CRUD (Single Model)

```
┌─────────────────────┐
│   Single Model      │
│                     │
│   ┌──────────────┐  │
│   │  AuditEvent  │  │
│   │              │  │
│   │ - Multiple   │  │
│   │   indexes    │  │
│   │ - Slow       │  │
│   │   writes     │  │
│   │ - Slow       │  │
│   │   reads      │  │
│   └──────────────┘  │
└─────────────────────┘
```

**Problems**:
- Indexes slow down writes
- Complex queries slow down reads
- Same schema for different access patterns
- Difficult to scale

### CQRS (Separate Models)

```
┌──────────────────┐      ┌──────────────────┐
│  Command Model   │──▶   │   Query Model    │
│  (Fast Writes)   │ Async│  (Fast Reads)    │
│                  │      │                  │
│ - No indexes     │      │ - Many indexes   │
│ - Simple         │      │ - Denormalized   │
│ - Fast inserts   │      │ - Complex queries│
└──────────────────┘      └──────────────────┘
```

**Benefits**:
- Independent optimization
- Scalable (scale reads/writes separately)
- Flexible (add query patterns without changing writes)
- Resilient (eventual consistency)

## Common Pitfalls & Solutions

### Pitfall 1: Eventual Consistency Complexity

**Problem**: Query model might lag behind command model

**Solution**:
- Accept eventual consistency for non-critical operations
- Use sync projection for critical operations
- Add timestamp/version checks if needed

### Pitfall 2: Data Duplication

**Problem**: Same data stored twice (command + query models)

**Solution**:
- Accept it - storage is cheap, performance is expensive
- Focus on different optimization goals
- Use data lifecycle policies (archive old command events)

### Pitfall 3: Increased Complexity

**Problem**: Two models instead of one

**Solution**:
- Only use CQRS where it adds value
- Not every entity needs CQRS
- Good for: audit logs, reporting, analytics, event sourcing

### Pitfall 4: Projection Failures

**Problem**: Async projection might fail

**Solution**:
- Retry logic with exponential backoff
- Dead letter queue for failed projections
- Monitoring and alerting
- Rebuild projections from command model

## Testing

### Command Side Tests

```java
@Test
void testRecordAuditEvent() {
    CreateAuditEventCommand command = CreateAuditEventCommand.builder()
        .eventType(AuditEventType.USER_CREATED)
        .userId(userId)
        .success(true)
        .build();

    AuditEventCommand event = auditCommandService.recordEvent(command);

    assertNotNull(event.getId());
    assertEquals(AuditEventType.USER_CREATED, event.getEventType());
}
```

### Query Side Tests

```java
@Test
void testFindEventsByUser() {
    Page<AuditEventDTO> events = auditQueryService.findEventsByUser(
        userId, PageRequest.of(0, 10));

    assertFalse(events.isEmpty());
    assertTrue(events.stream().allMatch(e -> e.getUserId().equals(userId)));
}

@Test
void testGetStatistics() {
    AuditStatisticsDTO stats = auditQueryService.getStatistics();

    assertNotNull(stats);
    assertTrue(stats.getTotalEvents() > 0);
    assertNotNull(stats.getEventsByType());
}
```

### Integration Tests

```java
@Test
void testEventualConsistency() {
    // Record event
    auditCommandService.recordEvent(command);

    // Wait for async projection
    await().atMost(5, SECONDS)
        .until(() -> auditQueryService.findEvents(...).getTotalElements() > 0);

    // Verify projection
    Page<AuditEventDTO> events = auditQueryService.findEvents(...);
    assertEquals(1, events.getTotalElements());
}
```

## Monitoring & Observability

### Key Metrics

1. **Command Side**:
   - Write throughput (events/second)
   - Write latency (p50, p95, p99)
   - Failed writes

2. **Query Side**:
   - Read throughput (queries/second)
   - Read latency by query type
   - Cache hit rate

3. **Projection**:
   - Projection lag (time between command and query)
   - Failed projections
   - Projection throughput

### Logging

```java
log.debug("Recording audit event: {}", command.getEventType());
log.debug("Projecting audit event to query model: {}", commandEvent.getId());
log.error("Failed to project audit event: {}", commandEvent.getId(), e);
```

## Future Enhancements

- [ ] Message queue for projections (Kafka, RabbitMQ)
- [ ] Separate database for query model
- [ ] Real-time event streaming
- [ ] Advanced analytics with Elasticsearch
- [ ] Data retention policies
- [ ] Automated projection rebuilds
- [ ] Event replay for debugging
- [ ] GraphQL API for flexible queries

## References

- Martin Fowler - CQRS: https://martinfowler.com/bliki/CQRS.html
- Microsoft - CQRS Pattern: https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs
- Greg Young - CQRS Documents: https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf
