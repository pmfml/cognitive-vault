# Architecture Documentation - Cognitive Vault

This document provides a detailed overview of the system architecture, design patterns, components, and data models of the **Cognitive Vault** application.

---

## 1. System Overview

Cognitive Vault is a personal knowledge management platform designed to store technical notes, code snippets, and files. It goes beyond traditional note-taking by dynamically calculating semantic relationships between notes, automatically suggesting spaced repetition study reviews, and providing a hybrid search experience.

The system utilizes a hybrid storage and retrieval approach:
1. **Frontend (React/Vite):** A modern, component-based Single Page Application providing a dashboard, Markdown live-preview editors, concurrent file uploaders, and interactive data visualization (Recharts).
2. **Relational + Vector Database (PostgreSQL with pgvector):** Stores note metadata, tags, relationships, and vector embeddings (384-dimension) generated for semantic similarity searches.
3. **Object Storage (MinIO / S3):** Stores raw attachments (PDFs, images, TXT files).
4. **Full-Text Search Engine (Elasticsearch):** Indexes raw text of attachments and notes to support fast keyword searching.
5. **Local Embedding Model (ONNX):** Generates 384-dimension vector embeddings locally via Spring AI using the `all-MiniLM-L6-v2` model, with no external API calls.
6. **Document Processing (Apache Tika):** Extracts textual content from rich document formats (PDF, DOCX, etc.) to feed both full-text and semantic search pipelines.

---

## 2. Component Architecture

The application is structured into a clean layered architecture with event-driven decoupling between the primary data store and the search engine:

```mermaid
graph TD
    Client([REST Clients / UI]) -->|HTTP Basic Auth| Security[Security Filter Chain]
    Security -->|Authenticated Requests| Controller[Controllers / API Layer]
    Controller -->|DTO Records| Service[Services / Business Logic Layer]

    subgraph Data Access Layer
        Service -->|Repository Interfaces| Repositories[Repositories / Spring Data JPA]
    end

    subgraph Infrastructure Services
        Service -->|MinIO API| S3Client[S3 Client / AWS SDK v2]
        Service -->|Spring AI / Local Embeddings| EmbeddingClient[Embedding Generator / ONNX]
        Service -->|Apache Tika| DocProcessor[Document Processor]
    end

    subgraph Event-Driven Indexing
        Service -->|Publishes Events| EventBus[Application Events]
        EventBus -->|After Commit| Listener[ES Indexing Listener]
        Listener -->|Elastic API Port 9205| ES[(Elasticsearch)]
    end

    Repositories -->|JDBC Port 5434| DB[(PostgreSQL + pgvector)]
    S3Client -->|S3 Protocol Port 9000| MinIO[(MinIO Object Storage)]

    %% Styling
    classDef client fill:#e2e3e5,stroke:#6c757d,stroke-width:2px,color:#000000;
    classDef security fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000;
    classDef app fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000;
    classDef layer fill:#f8f9fa,stroke:#343a40,stroke-width:2px,color:#000000;
    classDef infra fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000;
    classDef event fill:#e8daef,stroke:#8e44ad,stroke-width:2px,color:#000000;

    class Client client;
    class Security security;
    class Controller,Service,Repositories app;
    class S3Client,EmbeddingClient,DocProcessor layer;
    class DB,MinIO,ES infra;
    class EventBus,Listener event;
```

### Layer Responsibilities
- **Security Filter Chain:** HTTP Basic authentication via Spring Security. Stateless sessions (no cookies), CSRF disabled. Credentials are externalized via environment variables. Health endpoint remains public.
- **Controllers:** Expose standard REST endpoints, enforce Bean Validation on incoming payloads (`@Valid`, `@Validated`, `@NotBlank`, `@Min`, `@Max`), translate requests into DTO records, and return standard JSON responses with precise HTTP statuses.
- **Services:** Coordinate business rules, orchestrate transactions, resolve dependencies (such as tag management), perform file content extractions via Apache Tika, trigger relationship calculations, manage the spaced repetition decay logic, and publish indexing events.
- **Event System:** Services publish `NoteIndexRequestedEvent` / `NoteUnindexRequestedEvent` within the transaction. A `@TransactionalEventListener(AFTER_COMMIT)` listener processes them only after the database commit succeeds, decoupling Elasticsearch availability from write operations.
- **Repositories:** Standard Spring Data JPA interfaces. Includes native queries utilizing PostgreSQL extension operators (such as `<=>` cosine distance) to perform vector semantic lookups and JPQL queries for the review decay engine.
- **Utilities (`NoteMapper`, `VectorUtils`):** Shared utility classes that centralize common operations (entity-to-DTO mapping, float array to pgvector string conversion) to eliminate duplication across services.

---

## 3. Data Model

The relational schema is mapped via Hibernate and initialized with pgvector configurations.

```mermaid
erDiagram
    Note ||--o{ Attachment : "contains"
    Note }o--o{ Tag : "is tagged with"
    Note ||--o{ Relationship : "acts as source or target"

    Note {
        UUID id PK
        String title "Not Null"
        String content "Not Null"
        String type "NoteType Enum (TECHNICAL_NOTE, CODE_SNIPPET)"
        String language "Nullable (e.g. 'java', 'go')"
        String summary "Nullable"
        vector embedding "vector(384)"
        Instant createdAt "CreationTimestamp"
        Instant lastAccessedAt "Updated on every read/search"
        Instant lastReviewedAt "Updated on manual review"
    }

    Tag {
        UUID id PK
        String name "Unique, Not Null"
    }

    Attachment {
        UUID id PK
        String fileName "Not Null"
        String s3Key "Unique, Not Null (internal, not exposed via API)"
        String contentType "Not Null"
        Long fileSize "Not Null"
        String extractedText "Text extracted via Apache Tika (internal, not exposed via API)"
        Instant createdAt "CreationTimestamp"
        UUID note_id FK
    }

    Relationship {
        UUID id PK
        UUID source_note_id FK "Not Null"
        UUID target_note_id FK "Not Null"
        Double similarityScore "Cosine similarity metric"
        Instant createdAt "CreationTimestamp"
    }
```

---

## 4. Key Design Patterns & Technical Decisions

### 1. Vector Mapping with JPA
Since PostgreSQL's `vector` is a specialized type, JPA lacks direct mapping. The `Note` entity uses `@ColumnTransformer(write = "CAST(? AS vector)")` on the `float[]` field to handle the cast at persistence time. A `VectorUtils` utility centralizes the string serialization (`"[0.1,0.2,...]"`) for native queries.

### 2. Isolation of DTOs
Entities are strictly kept internal to the database and business logic layers. Data transferred to and from API clients uses immutable Java `record` types (`NoteRequest`, `NoteResponse`, `AttachmentResponse`). The `AttachmentResponse` intentionally omits internal fields (`s3Key`, `extractedText`) to avoid leaking infrastructure details.

### 3. Bean Validation Pipeline
All incoming API requests are validated at the controller layer using Jakarta Bean Validation (`@Valid` + `@NotBlank`, `@NotNull`). The search endpoint additionally uses `@Validated` with `@Min`/`@Max` constraints on the `limit` parameter (capped at 50) to prevent abuse. Structured `400 Bad Request` responses are generated via `GlobalExceptionHandler`.

### 4. Structured Exception Handling
A `@ControllerAdvice` (`GlobalExceptionHandler`) centralizes all error responses:
- `ResourceNotFoundException` → `404 Not Found`
- `IllegalArgumentException` → `400 Bad Request`
- `MethodArgumentNotValidException` → `400 Bad Request` with field-level error details
- `ConstraintViolationException` → `400 Bad Request` with constraint messages
- `HandlerMethodValidationException` → `400 Bad Request` with validation messages
- `MissingServletRequestParameterException` → `400 Bad Request` with parameter name
- `StorageException` → `503 Service Unavailable`
- `Exception` (catch-all) → `500 Internal Server Error`

### 5. Reciprocal Rank Fusion (RRF) for Hybrid Search
The `HybridSearchService` combines two independent result sets — one from pgvector semantic search and one from Elasticsearch full-text — using the RRF formula `1 / (k + rank)` where `k = 60`. Notes appearing in both result sets receive a higher fused score, producing a ranking that captures both semantic intent and keyword relevance.

### 6. Spaced Repetition Decay Engine
The `findNotesNeedingReview` JPQL query implements three independent decay rules:
1. **Never reviewed:** `lastReviewedAt IS NULL AND createdAt < 24h ago`
2. **Accessed since last review:** `lastAccessedAt > lastReviewedAt`
3. **Periodic review:** `lastReviewedAt < 30 days ago`

### 7. Event-Driven Elasticsearch Indexing
Instead of calling Elasticsearch directly within database transactions (which would couple search-engine availability to write operations), services publish lightweight application events (`NoteIndexRequestedEvent`, `NoteUnindexRequestedEvent`). The `NoteDocument` payload is pre-built inside the transaction while lazy associations are still accessible. A `@TransactionalEventListener(phase = AFTER_COMMIT)` listener then performs the actual index/delete, with try/catch resilience — a search outage never blocks note persistence.

### 8. S3 Rollback Compensation
When uploading an attachment, the file is sent to S3 before the database record is committed. To prevent orphaned objects if the transaction rolls back, a `TransactionSynchronization` hook is registered that deletes the uploaded S3 object on `afterCompletion(STATUS_ROLLED_BACK)`. This keeps object storage consistent with the database without requiring distributed transactions.

### 9. Document Processing with Apache Tika
The `DocumentProcessor` service uses a dual-path strategy:
- **Plain text files** (`.txt`, `.md`, `.json`, `text/*`): decoded directly as UTF-8 for maximum speed and fidelity.
- **Rich documents** (PDF and others): delegated to Apache Tika, which detects the format and extracts textual content. Parsing failures degrade gracefully to an empty string without interrupting the upload flow.

### 10. HTTP Basic Authentication
Spring Security is configured with HTTP Basic, stateless sessions, and CSRF disabled (appropriate for a REST API). A single user with credentials externalized via environment variables protects all `/api/**` endpoints, while `/actuator/health` remains public for infrastructure probes.

### 11. Decoupled and Isolated Tests
- **Service Layer Tests:** JUnit 5 + Mockito, mocking repositories and verifying event publication for fast, logic-only validation.
- **Controller Layer Tests:** `@WebMvcTest` + `MockMvc` + `@MockitoBean` + `@AutoConfigureMockMvc(addFilters = false)`, validating routing, serialization, HTTP status codes, Bean Validation, and exception handling in isolation (security filters bypassed to focus on controller logic).
- **Integration Tests:** `@SpringBootTest` + `@Tag("integration")` + Testcontainers (PostgreSQL/pgvector). Excluded from CI by default via Maven Surefire configuration; run locally with Docker.
- **Frontend Tests:** Vitest + React Testing Library, covering component rendering, API service mocking, and user interaction flows.

---

## 5. Sequence Diagrams

The diagrams below illustrate the three most critical runtime flows in the system: creating a note with full embedding and indexing, executing a hybrid search with RRF fusion, and uploading an attachment with S3 rollback compensation.

### 5.1 Note Creation Flow

This sequence shows the complete lifecycle of creating a new note — from the initial HTTP request through tag resolution, ONNX embedding generation, database persistence, relationship calculation, and event-driven Elasticsearch indexing.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as NoteController
    participant Validation as Bean Validation
    participant Service as NoteService
    participant TagRepo as TagRepository
    participant ONNX as EmbeddingModel<br/>(ONNX all-MiniLM-L6-v2)
    participant NoteRepo as NoteRepository
    participant RelService as RelationshipService
    participant EventBus as ApplicationEventPublisher
    participant Listener as ES Indexing Listener
    participant ES as Elasticsearch

    Client->>Controller: POST /api/v1/notes (NoteRequest JSON)
    Controller->>Validation: @Valid NoteRequest
    alt Validation fails
        Validation-->>Client: 400 Bad Request (field errors)
    end
    Controller->>Service: createNote(request)

    Note over Service: @Transactional begins

    Service->>TagRepo: resolveTags(tagNames)
    loop For each tag name
        TagRepo->>TagRepo: findByName() or save() new Tag
    end
    TagRepo-->>Service: Set of Tag entities

    Service->>ONNX: embed(title + content)
    ONNX-->>Service: float[384] embedding vector

    Service->>NoteRepo: save(Note entity)
    NoteRepo-->>Service: Saved Note (with generated UUID)

    Service->>RelService: recalculateRelationships(savedNote)
    RelService->>NoteRepo: findSimilarNotes(vectorString, limit)
    NoteRepo-->>RelService: List of similar Notes
    RelService->>RelService: Create/update Relationship records

    Service->>EventBus: publish(NoteIndexRequestedEvent)

    Note over Service: @Transactional commits

    EventBus->>Listener: @TransactionalEventListener(AFTER_COMMIT)
    Listener->>ES: Index NoteDocument
    ES-->>Listener: 200 OK

    Service-->>Controller: NoteResponse DTO
    Controller-->>Client: 201 Created (JSON body)
```

### 5.2 Hybrid Search Flow (Reciprocal Rank Fusion)

This sequence illustrates how a single user query is fanned out to two independent search backends — pgvector for semantic similarity and Elasticsearch for keyword matching — and the results are then fused using Reciprocal Rank Fusion (RRF) to produce a unified ranking.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as SearchController
    participant Service as HybridSearchService
    participant ONNX as EmbeddingModel<br/>(ONNX all-MiniLM-L6-v2)
    participant PG as PostgreSQL<br/>(pgvector)
    participant ES as Elasticsearch
    participant NoteRepo as NoteRepository

    Client->>Controller: GET /api/v1/search?query=...&limit=10
    Controller->>Controller: @Validated @Min(1) @Max(50) limit
    Controller->>Service: search(queryText, limit)

    par Semantic Search
        Service->>ONNX: embed(queryText)
        ONNX-->>Service: float[384] query vector
        Service->>PG: findSimilarNotes(vectorString, candidateLimit)
        PG-->>Service: List of Note candidates (ranked by cosine distance)
    and Full-Text Search
        Service->>ES: searchNotes(queryText)
        ES-->>Service: List of NoteDocument candidates (ranked by BM25)
    end

    Note over Service: Reciprocal Rank Fusion (k=60)
    Service->>Service: Score each candidate:<br/>score += 1/(60 + rank) per list
    Service->>Service: Sort by fused score descending
    Service->>Service: Limit to top N results

    Service->>NoteRepo: findAllById(rankedIds)
    NoteRepo-->>Service: List of Note entities

    Service->>NoteRepo: Update lastAccessedAt (transparent audit)
    NoteRepo-->>Service: Saved

    Service-->>Controller: List of NoteResponse (RRF-ranked)
    Controller-->>Client: 200 OK (JSON array)
```

### 5.3 Attachment Upload Flow (with S3 Rollback Compensation)

This sequence captures the multi-system coordination involved in uploading a file attachment — including MinIO storage, Apache Tika text extraction, database persistence, and the compensating rollback action that prevents orphaned S3 objects.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as AttachmentController
    participant Service as AttachmentService
    participant NoteRepo as NoteRepository
    participant MinIO as MinIO (S3)
    participant TxManager as TransactionSynchronizationManager
    participant Tika as DocumentProcessor<br/>(Apache Tika)
    participant AttachRepo as AttachmentRepository
    participant EventBus as ApplicationEventPublisher
    participant Listener as ES Indexing Listener
    participant ES as Elasticsearch

    Client->>Controller: POST /api/v1/notes/{noteId}/attachments<br/>(multipart file)
    Controller->>Service: uploadAttachment(noteId, filename, contentType, bytes)

    Note over Service: @Transactional begins

    Service->>NoteRepo: findById(noteId)
    NoteRepo-->>Service: Note entity

    Service->>MinIO: uploadFile(s3Key, bytes, contentType)
    MinIO-->>Service: Upload OK

    Service->>TxManager: registerSynchronization(rollback hook)
    Note over TxManager: If TX rolls back → delete S3 object

    Service->>Tika: extractText(bytes, contentType, filename)
    alt Plain text file (.txt, .md)
        Tika-->>Service: UTF-8 decoded text
    else Rich document (PDF, DOCX)
        Tika->>Tika: Apache Tika AutoDetectParser
        Tika-->>Service: Extracted text content
    else Parsing failure
        Tika-->>Service: Empty string (graceful degradation)
    end

    Service->>AttachRepo: save(Attachment entity)
    AttachRepo-->>Service: Saved Attachment (with UUID)

    Service->>EventBus: publish(NoteIndexRequestedEvent)

    Note over Service: @Transactional commits

    EventBus->>Listener: @TransactionalEventListener(AFTER_COMMIT)
    Listener->>ES: Re-index parent NoteDocument<br/>(now includes attachment text)
    ES-->>Listener: 200 OK

    Service-->>Controller: AttachmentResponse DTO
    Controller-->>Client: 201 Created (JSON body)

    Note over TxManager: On rollback (alternate path):
    rect rgb(255, 235, 235)
        TxManager->>MinIO: deleteFile(s3Key)
        MinIO-->>TxManager: Orphaned object removed
    end
```

---

## 6. Architectural Capabilities & Resilience Design

The architecture addresses specific distributed systems and retrieval challenges through targeted design choices:

### 6.1 Dual-Write Mitigation & Eventual Consistency
- **Search Engine Decoupling:** Writing synchronously to Elasticsearch within database transactions risks data drift if the database transaction aborts after the index request succeeds. Using `@TransactionalEventListener(phase = AFTER_COMMIT)` guarantees that only persisted notes trigger search index mutations.
- **S3 Orphan Compensation:** When uploading file attachments, binary payloads must be written to MinIO before entity persistence completes. A custom `TransactionSynchronization` listener hooks into `afterCompletion(STATUS_ROLLED_BACK)` to execute compensating deletions on MinIO if the JPA transaction fails.

### 6.2 Reciprocal Rank Fusion (RRF) Rationale
Directly combining Elasticsearch BM25 scores (unbounded positive floats) and pgvector cosine similarities (normalized floats between $-1$ and $1$) via linear weights requires complex per-query calibration. Reciprocal Rank Fusion completely bypasses score calibration by evaluating item ranks rather than raw scores:
\[
RRF(d) = \sum_{m \in M} \frac{1}{k + r_m(d)}
\]
where $M = \{\text{pgvector}, \text{Elasticsearch}\}$, $k = 60$ (smoothing constant), and $r_m(d)$ is the 1-based rank position of document $d$ within result set $m$. Notes that perform well across both retrieval modalities naturally float to the top.

### 6.3 Local Embedded Inference vs External APIs
Generating vector embeddings locally via Spring AI with ONNX (`all-MiniLM-L6-v2`, 384 dimensions) provides:
- **Zero Network Overhead:** Embedding queries during search and ingestion run in-process without outbound HTTP latency.
- **Data Privacy & Air-Gapped Operation:** User notes, confidential code snippets, and attachments never leave the local environment.
- **Deterministic Cost & Availability:** No rate limits, token billing, or external service downtime.

### 6.4 Spaced Repetition Temporal Decay Heuristics
The spaced repetition engine detects learning staleness using three non-overlapping evaluation criteria in a single JPQL query:
- **Unreviewed New Notes:** Created more than 24 hours ago with `lastReviewedAt IS NULL`.
- **Accessed Since Last Review:** `lastAccessedAt > lastReviewedAt`, indicating active retrieval or query engagement since the last explicit study session.
- **Temporal Staleness:** `lastReviewedAt < (NOW - 30 days)`, preventing established knowledge from fading over time.

---

## 7. Codebase Layout & Structural Mapping

```
cognitive-vault/
├── compose.yaml                          # Infrastructure (PostgreSQL 16 + pgvector, MinIO, Elasticsearch 8.12)
├── pom.xml                               # Maven project definition (Spring Boot 3.5.14, Spring AI, Tika)
├── docs/
│   └── ARCHITECTURE.md                   # System architecture and technical decisions (this document)
├── frontend/                             # React 19 Single Page Application
│   ├── src/
│   │   ├── components/                   # UI components (Dashboard, NoteEditor, NoteViewer, etc.)
│   │   ├── services/api.ts               # Typed REST client with error normalization
│   │   └── types/index.ts                # TypeScript interfaces aligned with backend DTO records
│   └── vite.config.ts                    # Vite dev proxy configuration with Basic Auth injection
└── src/
    ├── main/java/com/pmfml/cognitive_vault/
    │   ├── config/                       # Spring Security, MinIO S3 client, and Elasticsearch configuration
    │   ├── controllers/                  # REST controllers with Bean Validation (@Valid, @Min, @Max)
    │   ├── dtos/                         # Immutable Java record DTOs (NoteRequest, NoteResponse, etc.)
    │   ├── entities/                     # JPA entities (Note, Tag, Attachment, Relationship)
    │   ├── events/                       # Spring application events (NoteIndexRequestedEvent)
    │   ├── exceptions/                   # Centralized GlobalExceptionHandler and custom exceptions
    │   ├── listeners/                    # Post-commit transactional event listeners
    │   ├── repositories/                 # Spring Data JPA repositories with pgvector native queries
    │   └── services/                     # HybridSearchService (RRF), DocumentProcessor, NoteService
    └── test/java/com/pmfml/cognitive_vault/
        ├── controllers/                  # MockMvc controller slice tests
        ├── repositories/                 # Testcontainers-backed repository tests
        └── services/                     # Unit test suites with Mockito
```
