# Cognitive Vault 🧠📂

**Cognitive Vault** is a high-performance Knowledge Management & Semantic Retrieval engine designed to catalog notes, snippets, and rich document attachments. It automatically analyzes semantic similarity to discover latent connections between ideas, orchestrates an intelligent spaced repetition review engine, and executes hybrid search using Reciprocal Rank Fusion (RRF).

---

## 🚀 Key Features

> **All capabilities listed below are fully implemented and verified via unit, slice, and integration test suites.**

- ✅ **Hybrid Search via RRF:** Combines lexical search (Elasticsearch BM25) and semantic vector search (PostgreSQL `pgvector` cosine distance) using Reciprocal Rank Fusion ($k = 60$) for optimal ranking across keywords and conceptual intent.
- ✅ **Local Vector Embeddings:** Powered by Spring AI with an embedded ONNX transformer (`all-MiniLM-L6-v2`, 384 dimensions) running locally with zero external API dependencies or latency.
- ✅ **Document Analysis & Ingestion:** Apache Tika 3.x extracts clean text from PDFs and documents, indexing extracted text in Elasticsearch while storing binary payloads in MinIO (S3-compatible).
- ✅ **S3 Compensating Transactions:** Multi-phase upload with Spring's `TransactionSynchronizationManager` triggers automatic rollback compensation, deleting orphaned S3 blobs if database persistence fails.
- ✅ **Spaced Repetition Decay Engine:** Automated decay algorithms detect topics requiring review based on three distinct lifecycle criteria (unreviewed creation, post-review access, or 30+ days staleness).
- ✅ **Transparent Access Auditing:** Read actions and hybrid search queries transparently update the `lastAccessedAt` timestamp without disrupting read performance, feeding continuous behavioral data into the spaced repetition engine.
- ✅ **Event-Driven Decoupled Indexing:** Search engine indexing is decoupled from database transactions using `@TransactionalEventListener(phase = AFTER_COMMIT)`, ensuring primary persistence is resilient to search cluster availability.
- ✅ **Production-Grade API Contract:** Strictly typed DTO records, centralized Bean Validation with parameterized limits, custom error formats, and stateless HTTP Basic security.
- ✅ **Modern Web UI:** Full-featured React 19 + TypeScript + Tailwind CSS v4 dashboard featuring interactive Recharts analytics, real-time Markdown preview, concurrent attachment uploads, and dark/light modes.
- ✅ **Comprehensive Test Coverage:** Decoupled test architecture including unit tests, MockMvc slices, Testcontainers for real PostgreSQL/pgvector integration, and Vitest + React Testing Library for frontend components.

---

## 🛠️ Technology Stack

- **Backend:** Java 21 (Records, Pattern Matching, modern switch expressions)
- **Framework:** Spring Boot 3.5.14 with Spring Data JPA & Spring Security
- **Vector & Embeddings:** Spring AI 1.0.9 (Local ONNX `all-MiniLM-L6-v2`, 384 dimensions)
- **Document Processing:** Apache Tika 3.3.1 (PDF and multi-format text extraction)
- **Databases & Storage:**
  - **Relational + Vector:** PostgreSQL 16 + `pgvector` extension (`cosine` distance `<=>`)
  - **Search Engine:** Elasticsearch 8.12.2 (BM25 lexical scoring)
  - **Object Storage:** MinIO / AWS SDK v2 (`software.amazon.awssdk:s3`)
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS v4, Lucide Icons, Recharts
- **Testing:** JUnit 5, Mockito, Spring WebMvcTest, Testcontainers (PostgreSQL), Vitest, React Testing Library
- **CI/CD:** GitHub Actions automated build & test workflows

---

## 📋 Prerequisites

To run this application locally, you will need:

1.  **Java JDK 21** or higher.
2.  **Node.js 20.19+** (required by Vite v8+).
3.  **Docker & Docker Compose** installed and running.
4.  **Maven** (or use the included `./mvnw` wrapper).

---

## ⚙️ How to Get Started

### 1. Clone the repository
```bash
git clone https://github.com/pmfml/cognitive-vault.git
cd cognitive-vault
```

### 2. Infrastructure Setup (Docker)
The project includes a `compose.yaml` file defining all required services:
- **PostgreSQL** + pgvector extension on host port **5434**
- **MinIO** object storage on host ports **9000** (API) / **9001** (Console)
- **Elasticsearch** on host port **9205**

To start the infrastructure services, run:
```bash
docker compose up -d
```
Verify that all services are running:
```bash
docker ps
```

### 3. Environment Variables (Optional)
The application uses sensible defaults for local development, so **no environment variables are required to run it locally**. Every setting below falls back to a default baked into `application.properties`. To override credentials or connection endpoints in other environments, set the following before running:

| Variable | Description | Default |
|:---|:---|:---|
| `DB_URL` | Full JDBC connection URL | `jdbc:postgresql://localhost:5434/cognitive_vault` |
| `DB_USERNAME` | PostgreSQL username | `myuser` |
| `DB_PASSWORD` | PostgreSQL password | `secret` |
| `ELASTICSEARCH_URIS` | Elasticsearch endpoint URI | `http://localhost:9205` |
| `AWS_S3_ENDPOINT` | MinIO/S3 endpoint URL | `http://localhost:9000` |
| `AWS_REGION` | AWS region for the S3 client | `us-east-1` |
| `AWS_ACCESS_KEY` | MinIO/S3 access key | `minioadmin` |
| `AWS_SECRET_KEY` | MinIO/S3 secret key | `minioadmin` |
| `S3_BUCKET` | Bucket name for attachments | `cognitive-vault-attachments` |
| `SERVER_PORT` | Backend HTTP port | `8081` |
| `APP_USERNAME` | HTTP Basic auth username | `admin` |
| `APP_PASSWORD` | HTTP Basic auth password | `admin` |

### 4. Build & Run Tests
The test suite is divided into two categories:

- **Unit & slice tests** (no infrastructure required): service logic, controller routing, document processing, and search validation.
- **Integration tests** (require running Docker services): `@Tag("integration")` — context load and repository tests against a real PostgreSQL/pgvector container.

Run the fast suite (unit + slice only — what CI runs):
```bash
./mvnw test
```

Run only the integration tests (Docker must be running):
```bash
./mvnw test -DincludedGroups=integration
```

Run the full suite (unit + integration):
```bash
./mvnw test -DexcludedGroups=""
```

### 5. Running the Backend
Once the Docker containers are healthy and tests pass, start the Spring Boot application:
```bash
./mvnw spring-boot:run
```
The REST API will be available at `http://localhost:8081`.

> **Authentication:** All `/api/**` endpoints require HTTP Basic credentials. The default local credentials are `admin:admin`. The Vite dev proxy injects these automatically, so the frontend works transparently during development.

For a production-like run with SQL logging disabled, start with the `prod` profile:
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 6. Running the Frontend
In a new terminal window, navigate to the frontend directory, install dependencies, and start the Vite dev server:
```bash
cd frontend
npm install
npm run dev
```
The User Interface will be available at `http://localhost:5173`.

---

## 📡 REST API Documentation

> All endpoints below require HTTP Basic authentication. Use the configured credentials (default: `admin:admin`).

### Note Management Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/notes` | Retrieves all notes | `200 OK` |
| **GET** | `/api/v1/notes/{id}` | Retrieves a note by UUID (updates last accessed time) | `200 OK` |
| **POST** | `/api/v1/notes` | Creates a new Note or Snippet | `201 Created` |
| **PUT** | `/api/v1/notes/{id}` | Updates note content, title, or tags | `200 OK` |
| **DELETE** | `/api/v1/notes/{id}` | Deletes a note and all its relationships | `204 No Content` |

### Relationship Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/notes/{id}/relationships` | Lists semantically related notes (auto-computed) | `200 OK` |

### Study Review Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/notes/review-pending` | Lists notes that need a study review | `200 OK` |
| **POST** | `/api/v1/notes/{id}/review` | Marks a note as reviewed (updates timestamp) | `200 OK` |

### Search Endpoint

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/search?query=...&limit=10` | Hybrid semantic + textual search via RRF (limit: 1–50) | `200 OK` |

### Attachment Management Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/notes/{noteId}/attachments` | Uploads a file attachment linked to the note | `201 Created` |
| **GET** | `/api/v1/attachments/{id}` | Retrieves metadata of a specific attachment | `200 OK` |
| **GET** | `/api/v1/attachments/{id}/download` | Downloads the raw binary file content | `200 OK` |
| **DELETE** | `/api/v1/attachments/{id}` | Deletes an attachment from database and storage | `204 No Content` |

### Sample JSON Payloads

#### Create a Code Snippet (`POST /api/v1/notes`)
```json
{
  "title": "Reverse Array in Java",
  "content": "public static void reverse(int[] a) { ... }",
  "type": "CODE_SNIPPET",
  "language": "java",
  "tags": ["java", "algorithms", "arrays"]
}
```

#### Error Responses
All validation and not-found errors return a structured JSON body:
```json
{
  "timestamp": "2026-06-22T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "messages": ["title: Note title cannot be empty"]
}
```

### 🧪 Quick Verification with cURL

```bash
# 1. Create a note (HTTP Basic Auth admin:admin)
curl -s -u admin:admin -X POST http://localhost:8081/api/v1/notes \
  -H "Content-Type: application/json" \
  -d '{"title":"PostgreSQL pgvector Guide","content":"Indexing vectors with HNSW and IVFFlat for fast similarity search.","type":"TECHNICAL_NOTE","tags":["database","vector"]}'

# 2. Run a Hybrid Search (Elasticsearch + pgvector via RRF)
curl -s -u admin:admin "http://localhost:8081/api/v1/search?query=vector+indexing&limit=5" | jq

# 3. Retrieve pending reviews (Spaced Repetition Decay Engine)
curl -s -u admin:admin "http://localhost:8081/api/v1/notes/review-pending" | jq

# 4. Upload an attachment with automatic Tika text extraction
curl -s -u admin:admin -X POST http://localhost:8081/api/v1/notes/<NOTE_UUID>/attachments \
  -F "file=@document.pdf"
```

---

## 📂 Project Structure

```
cognitive-vault/
├── compose.yaml                  # Local infrastructure (PostgreSQL 16 + pgvector, MinIO, Elasticsearch 8)
├── pom.xml                       # Maven reactor & dependency management (Spring Boot 3.5.14, Spring AI, Tika)
├── docs/
│   └── ARCHITECTURE.md           # System architecture, ER diagrams, design patterns & sequence flows
├── frontend/                     # React 19 + TypeScript + Vite SPA
│   ├── README.md                 # Frontend architecture, dev proxy, and testing instructions
│   ├── src/
│   │   ├── components/           # UI components (Dashboard, NoteEditor, NoteViewer, HybridSearch, etc.)
│   │   ├── services/             # Axios/Fetch API client layer & Vitest specs
│   │   └── types/                # TypeScript interfaces matching backend DTO records
│   └── vite.config.ts            # Vite config with backend proxy and Basic Auth injection
└── src/                          # Spring Boot application
    ├── main/java/com/pmfml/cognitive_vault/
    │   ├── config/               # Security (Basic Auth), S3 MinIO Client, Elasticsearch Config
    │   ├── controllers/          # REST API endpoints (/api/v1/...) with Bean Validation
    │   ├── dtos/                 # Immutable Java records for request/response contracts
    │   ├── entities/             # JPA Entities (Note, Tag, Attachment, Relationship) & Converters
    │   ├── events/               # Domain application events for decoupled indexing
    │   ├── exceptions/           # Global exception handler & custom exceptions
    │   ├── listeners/            # Post-commit Transactional Event Listeners
    │   ├── repositories/         # Spring Data JPA & Elasticsearch Repositories
    │   └── services/             # Hybrid search (RRF), embeddings, Tika extraction, spaced repetition
    └── test/                     # JUnit 5, Mockito, MockMvc, and Testcontainers suites
```

For complete architectural details, entity relationships, and sequence diagrams, refer to [ARCHITECTURE.md](docs/ARCHITECTURE.md). For frontend setup and test execution, refer to [frontend/README.md](frontend/README.md).
