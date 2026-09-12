-- Initialize pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create HNSW index for fast approximate nearest neighbor (ANN) search on embeddings
CREATE INDEX IF NOT EXISTS idx_notes_embedding 
ON notes USING hnsw (embedding vector_cosine_ops);
