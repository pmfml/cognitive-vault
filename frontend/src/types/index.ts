export type NoteType = 'TECHNICAL_NOTE' | 'CODE_SNIPPET';

export interface NoteResponse {
  id: string;
  title: string;
  content: string;
  type: NoteType;
  language: string | null;
  summary: string | null;
  tags: string[];
  createdAt: string;
  lastAccessedAt: string;
  lastReviewedAt: string | null;
}

export interface NoteRequest {
  title: string;
  content: string;
  type: NoteType;
  language?: string | null;
  tags?: string[];
}

export interface AttachmentResponse {
  id: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  noteId: string;
}

export interface RelationshipResponse {
  relationshipId: string;
  targetNote: NoteResponse;
  similarityScore: number;
}
