# Cognitive Vault - Frontend 🖥️

Modern Single Page Application (SPA) for **Cognitive Vault**, built with React 19, TypeScript, Vite 8, and Tailwind CSS v4.

---

## 🚀 Overview & Features

- **Hybrid Search Interface:** Unified search bar supporting both lexical and semantic vector queries with real-time RRF score visualization and tag filtering.
- **Interactive Knowledge Dashboard:** Powered by Recharts, showing note distribution, tag frequency, review health, and access metrics.
- **Live Markdown Note Editor:** Split-view editor with live Markdown rendering, code snippet syntax highlighting, dynamic tag management, and multi-file drag-and-drop attachment upload.
- **Note Viewer & Context Links:** Reader overlay displaying rendered Markdown, direct attachment downloads, and auto-computed semantically related notes.
- **Spaced Repetition Review Deck:** Dedicated view displaying notes flagged by the decay engine, with one-click review completion.
- **Theme & UI Polish:** Seamless Dark/Light mode toggle with persistence, responsive navigation, and non-blocking toast notifications.

---

## 🛠️ Tech Stack

- **Framework:** React 19 + TypeScript
- **Bundler & Dev Server:** Vite 8
- **Styling:** Tailwind CSS v4
- **Charts & Data Visualization:** Recharts
- **Icons:** Lucide React
- **Unit & Component Testing:** Vitest + jsdom + React Testing Library

---

## ⚙️ Getting Started

### Prerequisites
- Node.js 20.19+ (or LTS)
- Running Cognitive Vault backend (default: `http://localhost:8081`)

### Installation
```bash
npm install
```

### Running Development Server
```bash
npm run dev
```
The application will be accessible at `http://localhost:5173`.

> **API Proxy Configuration:**
> During development, Vite's dev server proxies all `/api` requests to `http://localhost:8081` and automatically attaches HTTP Basic authentication (`admin:admin`). This eliminates CORS friction and allows transparent development against the secured Spring Boot backend.

### Running Tests
```bash
# Run unit & component tests
npm test

# Run tests with interactive UI
npx vitest --ui
```

### Production Build
```bash
npm run build
```
Build output will be generated in the `dist/` directory.

---

## 📂 Architecture & Directory Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── AllNotes.tsx          # Full note repository browser with search and filters
│   │   ├── Dashboard.tsx         # Analytical dashboard with Recharts visualizations
│   │   ├── FileUploader.tsx      # Multi-file concurrent upload with S3 status tracking
│   │   ├── HybridSearch.tsx      # Search view with RRF score badges and result cards
│   │   ├── NoteCard.tsx          # Individual note card component
│   │   ├── NoteEditor.tsx        # Creation and editing form with live Markdown preview
│   │   ├── NoteViewer.tsx        # Full-page note reader overlay with related note navigation
│   │   ├── PendingReviews.tsx    # Spaced repetition study queue
│   │   └── SearchBar.tsx         # Debounced global search input
│   ├── services/
│   │   ├── api.ts                # Axios/Fetch API client with error handling
│   │   └── api.test.ts           # API client integration tests
│   ├── types/
│   │   └── index.ts              # TypeScript models aligned with backend DTO records
│   ├── App.tsx                   # Main layout, view routing, and theme provider
│   ├── index.css                 # Tailwind CSS directives and theme variables
│   └── setupTests.ts             # Vitest test setup and DOM matchers
├── vite.config.ts                # Vite config & API reverse proxy
└── package.json                  # Dependencies and scripts
```
