# Batch Import Feature - Multithreading CSV Processing

## Overview

The Batch Import feature allows administrators to import multiple users at once by uploading a CSV file. This feature leverages **multithreading** to process large files efficiently, making it suitable for enterprise-scale user imports.

### Key Features

- ✅ **Multithreaded Processing**: Uses Java's `ThreadPoolExecutor` and `CompletableFuture` for parallel line processing
- ✅ **Real-time Progress Tracking**: Monitor import progress with live updates
- ✅ **Error Handling**: Track success and failure for each line individually
- ✅ **Asynchronous Processing**: Non-blocking file upload and processing
- ✅ **Internationalization**: Full i18n support (English and French)
- ✅ **DSFR UI Components**: Professional French government design system

## Architecture

### Backend Architecture (Hexagonal Pattern)

```
└── Backend
    ├── Domain Layer
    │   ├── model/
    │   │   ├── BatchImport.java           # Main entity
    │   │   ├── BatchImportLine.java       # Individual line result
    │   │   └── BatchImportStatus.java     # Status enum
    │   └── port/
    │       └── BatchImportPort.java       # Persistence port
    ├── Application Layer
    │   ├── service/
    │   │   └── BatchImportService.java    # Core business logic
    │   ├── dto/
    │   │   ├── CsvUserLine.java           # Parsed CSV line
    │   │   ├── BatchImportResponse.java
    │   │   └── BatchImportDetailResponse.java
    │   └── mapper/
    │       └── BatchImportMapper.java
    ├── Infrastructure Layer
    │   ├── config/
    │   │   └── AsyncConfig.java           # ThreadPool configuration
    │   └── persistence/
    │       ├── repository/
    │       │   └── BatchImportRepository.java
    │       └── adapter/
    │           └── BatchImportAdapter.java
    └── Presentation Layer
        └── controller/
            └── BatchImportController.java  # REST API endpoints
```

### Frontend Architecture (Angular + NgRx)

```
└── Frontend
    ├── Core
    │   ├── models/
    │   │   └── batch-import.model.ts      # TypeScript models
    │   └── services/
    │       └── batch-import.service.ts    # HTTP service
    ├── Store (NgRx)
    │   └── batch-import/
    │       ├── batch-import.actions.ts
    │       ├── batch-import.reducer.ts
    │       ├── batch-import.selectors.ts
    │       └── batch-import.effects.ts
    └── Features
        └── batch-import/
            └── components/
                ├── batch-import-upload/   # File upload component
                └── batch-import-list/     # Import list with progress
```

## Multithreading Implementation

### Thread Pool Configuration

Two thread pools are configured in `AsyncConfig.java`:

1. **Batch Import Executor** (Main process)
   - Core pool size: 5 threads
   - Max pool size: 20 threads
   - Queue capacity: 100
   - Used for overall batch import orchestration

2. **CSV Processor Executor** (Line processing)
   - Core pool size: 10 threads
   - Max pool size: 50 threads
   - Queue capacity: 500
   - Used for parallel CSV line processing

### Processing Flow

```
1. File Upload
   ├─> BatchImport entity created (Status: PENDING)
   └─> Async processing started

2. Async Processing (@Async)
   ├─> Parse CSV file into CsvUserLine objects
   ├─> Create CompletableFuture for each line
   ├─> Submit all futures to CSV Processor Executor
   └─> Process lines in parallel

3. For Each Line (Parallel)
   ├─> Validate data
   ├─> Check uniqueness (username, email)
   ├─> Create User entity
   ├─> Save to database
   ├─> Create BatchImportLine (success/failure)
   └─> Return result

4. Completion
   ├─> Wait for all futures (CompletableFuture.allOf)
   ├─> Collect results
   ├─> Update BatchImport status
   └─> Save all BatchImportLine entities
```

### Key Multithreading Code

**Service Method** (`BatchImportService.java`):

```java
// Process lines in parallel using CompletableFuture
List<CompletableFuture<BatchImportLine>> futures = csvLines.stream()
    .map(csvLine -> processLineAsync(batchImport, csvLine))
    .collect(Collectors.toList());

// Wait for all futures to complete
CompletableFuture<Void> allFutures = CompletableFuture.allOf(
    futures.toArray(new CompletableFuture[0])
);

allFutures.join(); // Wait for all lines to be processed
```

**Async Line Processing**:

```java
private CompletableFuture<BatchImportLine> processLineAsync(
    BatchImport batchImport, CsvUserLine csvLine) {

    return CompletableFuture.supplyAsync(() -> {
        // Process line: validate, create user, handle errors
        // Each line runs in a separate thread from CSV Processor pool
    }, csvProcessorExecutor);
}
```

## API Endpoints

### Upload CSV File

```http
POST /api/v1/batch-imports/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

Body: file (CSV file)

Response: 201 Created
{
  "id": "uuid",
  "fileName": "users.csv",
  "status": "PENDING",
  "totalLines": 0,
  ...
}
```

### Get Batch Import Status

```http
GET /api/v1/batch-imports/{id}/status
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "status": "PROCESSING",
  "totalLines": 100,
  "processedLines": 45,
  "successLines": 42,
  "failedLines": 3,
  "progressPercentage": 45.0
}
```

### Get Batch Import Details

```http
GET /api/v1/batch-imports/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "status": "COMPLETED",
  "lines": [
    {
      "lineNumber": 1,
      "success": true,
      "createdUsername": "jdoe"
    },
    {
      "lineNumber": 2,
      "success": false,
      "errorMessage": "Email already in use: existing@example.com"
    }
  ]
}
```

### List All Batch Imports

```http
GET /api/v1/batch-imports?page=0&size=20
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [...],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

### Cancel Batch Import

```http
POST /api/v1/batch-imports/{id}/cancel
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "status": "CANCELLED"
}
```

## CSV File Format

### Required Format

```csv
username,email,firstName,lastName,phoneNumber,roles
jdoe,john.doe@example.com,John,Doe,+33612345678,USER
asmith,alice.smith@example.com,Alice,Smith,,ADMIN,USER
bwilson,bob@example.com,Bob,Wilson,+33698765432,USER
```

### Column Specifications

| Column | Required | Description | Validation |
|--------|----------|-------------|------------|
| username | ✅ Yes | Unique username | 3-50 characters |
| email | ✅ Yes | Email address | Valid email format |
| firstName | ❌ No | First name | Max 50 characters |
| lastName | ❌ No | Last name | Max 50 characters |
| phoneNumber | ❌ No | Phone number | Max 20 characters |
| roles | ❌ No | Comma-separated roles | USER, ADMIN, MANAGER, TECH_LEAD |

### Validation Rules

- **Header line**: First line is skipped as header
- **Empty lines**: Ignored
- **Username**: Must be unique across the system
- **Email**: Must be unique and valid format
- **Roles**: Invalid roles default to USER
- **Empty fields**: Optional fields can be empty

## Frontend Usage

### Uploading a File

```typescript
// Component
export class BatchImportUploadComponent {
  constructor(private store: Store) {}

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files[0];
    this.store.dispatch(uploadCsvFile({ file }));
  }
}
```

### Monitoring Progress

```typescript
// Component with automatic polling
export class BatchImportListComponent implements OnInit {
  ngOnInit(): void {
    // Poll for updates every 3 seconds
    interval(3000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshInProgressImports();
      });
  }

  private refreshInProgressImports(): void {
    this.imports$
      .pipe(filter(imports => imports.length > 0))
      .subscribe(imports => {
        imports
          .filter(imp => imp.status === BatchImportStatus.PROCESSING)
          .forEach(imp => {
            this.store.dispatch(refreshBatchImportStatus({ id: imp.id }));
          });
      });
  }
}
```

## Performance Considerations

### Scalability

- **Small files (<100 lines)**: Near-instant processing
- **Medium files (100-1000 lines)**: ~10-30 seconds
- **Large files (1000-10000 lines)**: ~1-5 minutes
- **Very large files (>10000 lines)**: Consider chunking

### Optimization Tips

1. **Adjust Thread Pool Sizes**:
   ```java
   executor.setCorePoolSize(20);  // Increase for more parallelism
   executor.setMaxPoolSize(100);  // Higher max for bursts
   ```

2. **Batch Database Operations**:
   - Consider using batch inserts for better performance
   - Current implementation uses individual saves for error isolation

3. **Memory Management**:
   - Maximum file size: 10 MB
   - Lines are processed incrementally
   - CompletableFutures released after completion

### Database Considerations

- Use database connection pooling (HikariCP configured)
- Monitor connection pool size during high load
- Consider read replicas for queries during imports

## Error Handling

### Line-level Errors

Each line is processed independently. Errors on one line don't affect others:

- **Validation errors**: Invalid format, missing required fields
- **Uniqueness errors**: Duplicate username or email
- **Database errors**: Constraint violations

### Import-level Errors

Fatal errors that stop the entire import:

- **File format errors**: Not a CSV file, encoding issues
- **System errors**: Database connection failure, out of memory

### Status Flow

```
PENDING → PROCESSING → {COMPLETED, COMPLETED_WITH_ERRORS, FAILED}
           ↓
        CANCELLED (if user cancels)
```

## Testing

### Unit Tests

```java
@Test
void testProcessLineAsync_Success() {
    CsvUserLine csvLine = CsvUserLine.builder()
        .username("testuser")
        .email("test@example.com")
        .build();

    CompletableFuture<BatchImportLine> future =
        batchImportService.processLineAsync(batchImport, csvLine);

    BatchImportLine result = future.join();
    assertTrue(result.isSuccess());
}
```

### Integration Tests

```java
@Test
void testFullBatchImportFlow() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "test.csv", "text/csv",
        "username,email\njdoe,john@example.com".getBytes()
    );

    BatchImport batchImport = batchImportService.createBatchImport(file, user);

    // Wait for async processing
    await().atMost(5, SECONDS)
        .until(() -> batchImportRepository.findById(batchImport.getId())
            .map(BatchImport::isCompleted)
            .orElse(false));

    BatchImport completed = batchImportRepository.findById(batchImport.getId()).get();
    assertEquals(BatchImportStatus.COMPLETED, completed.getStatus());
    assertEquals(1, completed.getSuccessLines());
}
```

## Monitoring and Logging

### Log Levels

```properties
# Application logs
logging.level.com.enterprise.app.application.service.BatchImportService=DEBUG

# Thread pool monitoring
logging.level.org.springframework.scheduling=DEBUG
```

### Key Metrics to Monitor

- Total imports processed
- Average processing time per import
- Success/failure rates
- Thread pool utilization
- Database connection pool usage

## Security

### Access Control

- Required role: `ADMIN`, `TECH_LEAD`, or `MANAGER`
- Users can only view their own imports (unless admin)

### Input Validation

- File size limit: 10 MB
- File type: CSV only
- Content validation: Each field validated
- SQL injection prevention: Parameterized queries

## Internationalization

All error messages and UI text support English and French:

```properties
# English (messages_en.properties)
error.batch.user.already.exists=User already exists with username: {0}

# French (messages_fr.properties)
error.batch.user.already.exists=Un utilisateur existe déjà avec le nom d'utilisateur : {0}
```

## Troubleshooting

### Common Issues

**Issue**: Import stuck in PROCESSING
- **Cause**: Thread pool exhausted or database deadlock
- **Solution**: Check logs, restart application if needed

**Issue**: High memory usage
- **Cause**: Very large file with many concurrent imports
- **Solution**: Reduce max pool size or implement file size limits

**Issue**: Slow processing
- **Cause**: Database performance bottleneck
- **Solution**: Add database indexes, optimize queries

## Future Enhancements

- [ ] Support for other file formats (Excel, JSON)
- [ ] Bulk update operations (not just create)
- [ ] Scheduled imports
- [ ] Email notification on completion
- [ ] Advanced filtering and search for import history
- [ ] Export failed lines to CSV for correction
- [ ] Duplicate detection strategies (skip, update, fail)

## References

- Java ThreadPoolExecutor: [Oracle Docs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html)
- CompletableFuture: [Oracle Docs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- Spring @Async: [Spring Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling-annotation-support-async)
- DSFR: [Design System FR](https://www.systeme-de-design.gouv.fr/)
