# Architecture Documentation - Cognitive Vault

This document provides a detailed overview of the system architecture, design patterns, components, and data models of the **Cognitive Vault** application.

---

## 1. System Overview

Cognitive Vault is a personal knowledge management platform designed to store technical notes, snippets, and files. It goes beyond traditional note-taking by dynamically calculating semantic relationships between notes and automatically suggesting study reviews.

The system utilizes a hybrid storage and retrieval approach:
1. **Relational + Vector Database (PostgreSQL with pgvector):** Stores note metadata, tags, relationships, and vector embeddings (384-dimension) generated for semantic similarity searches.
2. **Object Storage (MinIO / S3):** Stores raw attachments (PDFs, images, TXT files).
3. **Full-Text Search Engine (Elasticsearch):** Indexes raw text of attachments and notes to support fast keyword searching.

---

## 2. Component Architecture

The application is structured into a clean layered architecture:

```mermaid
graph TD
    Client([REST Clients / UI]) -->|HTTP REST API| Controller[Controllers / API Layer]
    Controller -->|DTO Records| Service[Services / Business Logic Layer]
    
    subgraph Data Access Layer
        Service -->|Repository Interfaces| Repositories[Repositories / Spring Data JPA]
    end

    subgraph Infrastructure Services
        Service -->|MinIO API| S3Client[S3 Client / AWS SDK v2]
        Service -->|Spring AI / Local Embeddings| EmbeddingClient[Embedding Generator]
    end

    Repositories -->|JDBC Port 5434| DB[(PostgreSQL + pgvector)]
    S3Client -->|S3 Protocol Port 9000| MinIO[(MinIO Object Storage)]
    Service -->|Elastic API Port 9200| ES[(Elasticsearch)]

    %% Styling
    classDef client fill:#e2e3e5,stroke:#6c757d,stroke-width:2px,color:#000000;
    classDef app fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000;
    classDef layer fill:#f8f9fa,stroke:#343a40,stroke-width:2px,color:#000000;
    classDef infra fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000;

    class Client client;
    class Controller,Service,Repositories app;
    class S3Client,EmbeddingClient layer;
    class DB,MinIO,ES infra;
```

### Layer Responsibilities
- **Controllers:** Expose standard REST endpoints, validate input payloads, translate requests into DTO records, and return standard JSON responses with precise HTTP statuses.
- **Services:** Coordinate business rules, orchestrate transactions, resolve dependencies (such as tag management), perform file content extractions, and trigger relationship calculations.
- **Repositories:** Standard Spring Data JPA interfaces. Includes native queries utilizing PostgreSQL extension operators (such as `<=>` cosine distance) to perform vector semantic lookups.

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
        Instant lastAccessedAt "Updated on read"
        Instant lastReviewedAt "Manual study timestamp"
    }

    Tag {
        UUID id PK
        String name "Unique, Not Null"
    }

    Attachment {
        UUID id PK
        String fileName "Not Null"
        String s3Key "Unique, Not Null"
        String contentType "Not Null"
        Long fileSize "Not Null"
        String extractedText "Text content extracted from document"
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
Since PostgreSQL's `vector` is a specialized type, JPA lacks direct mapping. We implemented a custom `VectorConverter` class extending `AttributeConverter<float[], String>`. 
- **Java representation:** A native `float[]` array.
- **Database representation:** A string format `[v1,v2,v3,...]` which PostgreSQL implicitly casts to a `vector`.

### 2. Isolation of DTOs
Entities are strictly kept internal to the database and business logic layers. Data transferred to and from API clients uses immutable Java `record` types (`NoteRequest` and `NoteResponse`), preventing serialization loops and decoupling API schemas from database refactorings.

### 3. Decoupled and Isolated Tests
- **Service Layer Tests:** Built with JUnit 5 and Mockito, mocking the database/repositories to ensure blazing fast, logic-only test validation.
- **Controller Layer Tests:** Utilizes `@WebMvcTest` with `MockMvc` and `@MockitoBean`, validating routing, serialization, HTTP status codes, and exceptions in isolation without booting the database context.
- **Integration Tests:** Boots the entire Spring context (`@SpringBootTest`) and utilizes the Spring Boot Docker Compose integration to connect to the active local PostgreSQL database container automatically.

---

## 5. Architectural Lifecycle Progression
- **Phase 1 (Completed):** Setup database schema, model mappings, and REST API CRUD endpoints for notes/snippets.
- **Phase 2 (Next):** Implement S3 Attachment storage with local MinIO, including file metadata tracking.
- **Phase 3 (Upcoming):** Integrate Elasticsearch keyword indexing for note search.
- **Phase 4 (Upcoming):** Auto-link semantically related content via embedding matching and calculate review queues.
