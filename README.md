Bulk Data Import & Validation System – Advanced
A Spring Boot backend service for uploading, validating, transforming, and persisting large CSV/Excel datasets efficiently using streaming, async processing, and batch inserts.
---
Table of Contents
Overview
Tech Stack
Architecture
Database Schema 
Project Structure
Prerequisites
Setup Instructions
Configuration
Running the Application
API Endpoints
Sample Request/Response
Sample Input Files
Testing
Handling Large Files & Edge Cases
Performance Optimizations
Idempotency Strategy
Troubleshooting
---
Overview
This system allows enterprise users to upload large CSV/Excel files containing bulk data. Each record is streamed, validated using Bean Validation, and persisted in batches. The system tracks import jobs and provides a summary of successful and failed records without requiring the entire file to be loaded into memory.
Core Capabilities
Upload CSV/Excel files via REST API
Stream-parse and validate each record
Persist valid records; reject and log invalid ones with error reasons
Asynchronous job processing (non-blocking upload response)
Batch database inserts for performance
Idempotent file handling (duplicate uploads are detected and skipped)
Import summary reporting (success count, failure count, per-record errors)
---
Tech Stack
Layer	Technology
Language	Java 17
Framework	Spring Boot 3.x
Persistence	Spring Data JPA (Hibernate)
Database	MySQL / PostgreSQL
Validation	Jakarta Bean Validation
File Parsing	Apache Commons CSV / Apache POI (streaming SXSSF/SAX for Excel)
Async Processing	Spring `@Async` + `ThreadPoolTaskExecutor`
Build Tool	Maven
API Docs	Swagger / springdoc-openapi
Testing	JUnit 5, Mockito, Testcontainers
---
Architecture
``
Client
  │
  ▼
Controller Layer (ImportController)
  │  - Accepts file upload
  │  - Creates ImportJob record (status: PENDING)
  │  - Returns jobId immediately
  ▼
Service Layer (ImportService) [runs @Async]
  │  - Streams file (row by row)
  │  - Validates each record (Bean Validation)
  │  - Buffers valid records into batches
  │  - Persists batches (JPA batch insert)
  │  - Logs invalid records with error_message
  │  - Updates ImportJob status (PROCESSING → COMPLETED/FAILED/PARTIALLY_COMPLETED)
  ▼
Repository Layer (Spring Data JPA)
  │
  ▼
Database (ImportJob, ImportRecord tables)
```
Layered Structure
Controller – REST endpoints, request/response DTOs
Service – business logic, orchestrates parsing + validation + persistence
Parser/Strategy – pluggable CSV/Excel readers (Strategy pattern based on file type)
Validator – Bean Validation + custom business rule checks
Repository – Spring Data JPA repositories
Async Executor Config – dedicated thread pool for import jobs
---
Database Schema
`import_job`
Column	Type	Notes
id	BIGINT (PK)	Auto-generated
file_name	VARCHAR	Original file name
file_hash	VARCHAR	SHA-256 hash of file content (used for idempotency)
status	VARCHAR	`PENDING`, `PROCESSING`, `COMPLETED`, `PARTIALLY_COMPLETED`, `FAILED`
total_records	INT	Total rows detected
success_count	INT	Records inserted successfully
failed_count	INT	Records rejected
created_at	TIMESTAMP	Job creation time
updated_at	TIMESTAMP	Last status update time
`import_record`
Column	Type	Notes
id	BIGINT (PK)	Auto-generated
job_id	BIGINT (FK)	References `import_job.id`
row_number	INT	Line number in source file
data	JSON/TEXT	Raw or parsed record data
status	VARCHAR	`SUCCESS`, `FAILED`
error_message	TEXT	Validation/error details (nullable)
created_at	TIMESTAMP	Insert time
A unique constraint on `import_job.file_hash` prevents reprocessing of an identical file.
---
Project Structure
```
bulk-import-system/
├── src/main/java/com/example/bulkimport/
│   ├── controller/
│   │   └── ImportController.java
│   ├── service/
│   │   ├── ImportService.java
│   │   └── FileParserFactory.java
│   ├── parser/
│   │   ├── CsvFileParser.java
│   │   └── ExcelFileParser.java
│   ├── validator/
│   │   └── RecordValidator.java
│   ├── model/
│   │   ├── ImportJob.java
│   │   └── ImportRecord.java
│   ├── repository/
│   │   ├── ImportJobRepository.java
│   │   └── ImportRecordRepository.java
│   ├── dto/
│   │   ├── ImportJobResponse.java
│   │   └── ImportSummaryResponse.java
│   ├── config/
│   │   ├── AsyncConfig.java
│   │   └── SwaggerConfig.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/ (Flyway scripts, if used)
├── src/test/java/...
├── sample-files/
│   ├── sample_valid.csv
│   ├── sample_with_errors.csv
│   └── sample_large_1L_records.csv
├── postman/
│   └── Bulk_Import_API.postman_collection.json
├── pom.xml
└── README.md
```
---
Prerequisites
JDK 17+
Maven 3.8+
MySQL 8.x or PostgreSQL 14+ (running instance)
Postman (optional, for API testing)
Minimum 2 GB free heap recommended for large file tests
---
Setup Instructions
Clone the repository
```bash
   git clone <repository-url>
   cd bulk-import-system
   ```
Create the database
```sql
   CREATE DATABASE bulk_import_db;
   ```
Configure database credentials
Update `src/main/resources/application.yml` (see Configuration).
Build the project
```bash
   mvn clean install
   ```
Run database migrations (if using Flyway/Liquibase)
```bash
   mvn flyway:migrate
   ```
---
Configuration
`application.yml` (key properties):
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bulk_import_db
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 500
        order_inserts: true
        order_updates: true
    show-sql: false
  servlet:
    multipart:
      max-file-size: 200MB
      max-request-size: 200MB

import:
  async:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 50
  batch:
    size: 500
  storage:
    temp-dir: ./uploads/temp
```
---
Running the Application
```bash
mvn spring-boot:run
```
Application starts at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`
---
API Endpoints
Method	Endpoint	Description
POST	`/api/imports/upload`	Upload a CSV/Excel file to start an import job
GET	`/api/imports/{jobId}`	Get job status and summary
GET	`/api/imports/{jobId}/records?status=FAILED`	Get paginated records (optionally filtered by status)
GET	`/api/imports`	List all import jobs (paginated)
GET	`/api/imports/{jobId}/errors/export`	Download failed records as CSV
---
Sample Request/Response
Upload a file
```bash
curl -X POST http://localhost:8080/api/imports/upload \
  -F "file=@sample-files/sample_with_errors.csv"
```
Response (202 Accepted)
```json
{
  "jobId": 101,
  "fileName": "sample_with_errors.csv",
  "status": "PROCESSING",
  "message": "File accepted. Processing started asynchronously."
}
```
Check job status
```bash
curl http://localhost:8080/api/imports/101
```
Response
```json
{
  "jobId": 101,
  "fileName": "sample_with_errors.csv",
  "status": "PARTIALLY_COMPLETED",
  "totalRecords": 1000,
  "successCount": 950,
  "failedCount": 50,
  "createdAt": "2026-07-14T10:15:00Z",
  "updatedAt": "2026-07-14T10:16:32Z"
}
```
Duplicate file re-upload response (409 Conflict)
```json
{
  "error": "DUPLICATE_FILE",
  "message": "This file has already been processed under jobId 101.",
  "existingJobId": 101
}
```
---
Sample Input Files
Located under `/sample-files`:
File	Purpose
`sample_valid.csv`	All records pass validation
`sample_with_errors.csv`	Mix of valid and invalid rows (missing fields, bad formats)
`sample_large_1L_records.csv`	~100,000 rows for load/performance testing
CSV format example:
```csv
name,email,phone,age
John Doe,john@example.com,9876543210,29
Jane Smith,invalid-email,9876543211,31
,missing-name@example.com,9876543212,25
```
---
Testing
Run all unit and integration tests:
```bash
mvn test
```
Test coverage includes:
Bean Validation rules (unit tests)
CSV/Excel streaming parsers (unit tests with sample files)
Service-layer batch insert logic (integration tests with Testcontainers)
Idempotency check (duplicate file hash rejection)
Async job completion polling test
Large file simulation test (100k+ rows) for memory/performance benchmarking
---
Handling Large Files & Edge Cases
Scenario	Handling Strategy
Large file (1L+ records)	Streaming parser (Commons CSV iterator / POI SAX API); never loads full file into memory
Invalid file format	Rejected at upload with `400 Bad Request` before job creation
Duplicate records within file	Flagged as `FAILED` with error message; job continues
Duplicate file upload	Detected via SHA-256 file hash; returns existing job info instead of reprocessing
System crash mid-processing	Job status remains `PROCESSING`; a recovery/reconciliation job on startup marks stale jobs and allows safe resume or re-trigger
Partial row failures	Row-level try/catch; failed rows logged to `import_record` with reason, valid rows continue processing
---
Performance Optimizations
Streaming I/O: CSV read via buffered iterator; Excel read via POI's streaming (SAX/event) API — constant memory usage regardless of file size
Batch Inserts: Hibernate `hibernate.jdbc.batch_size`, `order_inserts`, and `order_updates` enabled; records flushed and cleared from persistence context every batch cycle
Async Processing: Upload endpoint returns immediately with `jobId`; actual processing runs on a dedicated `@Async` thread pool, keeping the API responsive
Chunked Transactions: Each batch is committed independently to avoid one large long-running transaction and to limit rollback blast radius
Connection Pooling: HikariCP tuned pool size to match batch concurrency
---
Idempotency Strategy
On upload, compute a SHA-256 hash of the file content.
Check `import_job` table for an existing row with the same `file_hash`.
If found: return the existing job's details instead of creating a new job or reprocessing.
If not found: create a new `ImportJob` and proceed with processing.
This guarantees the same file is never processed twice, even under concurrent upload attempts (enforced via a unique DB constraint on `file_hash`).
---
Troubleshooting
Issue	Possible Fix
`MaxUploadSizeExceededException`	Increase `spring.servlet.multipart.max-file-size` / `max-request-size`
Job stuck in `PROCESSING`	Check async thread pool logs; verify startup recovery job ran
Slow inserts on large files	Confirm Hibernate batch properties are active; check DB indexes on `import_record`
Duplicate file not detected	Ensure `file_hash` column has a unique constraint and hashing covers full file bytes
---
Deliverables Checklist
[ ] Spring Boot project source code
[ ] API documentation (Swagger UI / Postman collection)
[ ] Sample input files (`/sample-files`)
[ ] Database schema (DDL / migration scripts)
[ ] This README with setup and execution steps
