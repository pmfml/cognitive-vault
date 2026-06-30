# Cognitive Vault 🧠📂

**Cognitive Vault** is a smart Personal Organizer and Semantic Knowledge Repository designed to catalog notes, snippets, and attachments. It automatically evaluates the semantic similarity between notes to suggest content linkages and highlights outdated topics to guide study review cycles.

This application is built as a portfolio piece showcasing Java backend engineering, relational-vector hybrid databases, and object storage integration.

---

## 🚀 Key Features

*   **Hybrid Note & Snippet Catalog:** Store standard markdown text notes and code snippets with language syntax highlights.
*   **Semantic Relationships:** Uses vector embeddings (384-dimension) to dynamically connect notes on related topics.
*   **Spaced Repetition Engine:** Automatically identifies notes requiring review based on three intelligent decay rules (never reviewed, accessed after last review, or not reviewed in 30+ days).
*   **Hybrid Search:** Search notes and attachments by keyword (Elasticsearch) and semantic meaning (pgvector cosine similarity), combined via Reciprocal Rank Fusion (RRF).
*   **Document Analysis:** Upload attachments (PDFs, images, TXT files) whose textual content is extracted and indexed for search.
*   **Transparent Access Audit:** Every search and direct note access automatically updates the `lastAccessedAt` timestamp, feeding the review engine with behavioral data.

---

## 🛠️ Technology Stack

*   **Backend:** Java 21 (Records, pattern matching, modern switch expressions)
*   **Framework:** Spring Boot 3.5.x with Spring Data JPA
*   **Frontend:** React 19 + TypeScript + Vite + Tailwind CSS v4 + Recharts
*   **Database:** PostgreSQL 16 + `pgvector` extension (vector similarity search)
*   **Semantic Embeddings:** Spring AI with local ONNX model (`all-MiniLM-L6-v2`, 384 dimensions)
*   **Object Storage:** MinIO (Local S3 compatibility via AWS SDK v2)
*   **Search Engine:** Elasticsearch 8.x (full-text keyword indexing)
*   **Local Containerization:** Docker / Docker Compose
*   **Testing:** JUnit 5, Mockito, Spring WebMvcTest (Backend)

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
git clone https://github.com/<your-username>/cognitive-vault.git
cd cognitive-vault
```

### 2. Infrastructure Setup (Docker)
The project includes a `compose.yaml` file defining all required services:
- **PostgreSQL** + pgvector extension on host port **5434**
- **MinIO** object storage on host ports **9000** (API) / **9001** (Console)
- **Elasticsearch** on host port **9200**

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
| `ELASTICSEARCH_URIS` | Elasticsearch endpoint URI | `http://localhost:9200` |
| `AWS_S3_ENDPOINT` | MinIO/S3 endpoint URL | `http://localhost:9000` |
| `AWS_REGION` | AWS region for the S3 client | `us-east-1` |
| `AWS_ACCESS_KEY` | MinIO/S3 access key | `minioadmin` |
| `AWS_SECRET_KEY` | MinIO/S3 secret key | `minioadmin` |
| `S3_BUCKET` | Bucket name for attachments | `cognitive-vault-attachments` |
| `SERVER_PORT` | Backend HTTP port | `8081` |

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
| **GET** | `/api/v1/search?query=...&limit=10` | Hybrid semantic + textual search via RRF | `200 OK` |

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

---

## 📂 Project Structure & Coding Standards

Coding styles and patterns are governed by standard guidelines located under the `.project-standards/` folder:
*   [global-standards.md](.project-standards/global-standards.md): Language conventions (English codebase/docs), database mappings, constructor injections, and commit rules.
*   [cognitive-vault-standards.md](.project-standards/cognitive-vault-standards.md): Domain modeling details, pgvector custom types mapping, and SDK settings.
*   [ARCHITECTURE.md](ARCHITECTURE.md): Structural layered diagrams, database ER diagrams, and system lifecycle progressions.
