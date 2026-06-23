import type { NoteResponse, NoteRequest, RelationshipResponse, AttachmentResponse } from '../types';

const BASE_URL = '/api/v1';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = 'An error occurred';
    try {
      const errorData = await response.json();
      errorMessage = errorData.messages ? errorData.messages.join(', ') : errorData.error || errorMessage;
    } catch {
      errorMessage = response.statusText || errorMessage;
    }
    throw new Error(errorMessage);
  }
  
  if (response.status === 204) {
    return null as unknown as T;
  }
  return response.json();
}

export const api = {
  // Notes CRUD
  async getNotes(): Promise<NoteResponse[]> {
    const response = await fetch(`${BASE_URL}/notes`);
    return handleResponse<NoteResponse[]>(response);
  },

  async getNoteById(id: string): Promise<NoteResponse> {
    const response = await fetch(`${BASE_URL}/notes/${id}`);
    return handleResponse<NoteResponse>(response);
  },

  async createNote(request: NoteRequest): Promise<NoteResponse> {
    const response = await fetch(`${BASE_URL}/notes`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return handleResponse<NoteResponse>(response);
  },

  async updateNote(id: string, request: NoteRequest): Promise<NoteResponse> {
    const response = await fetch(`${BASE_URL}/notes/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return handleResponse<NoteResponse>(response);
  },

  async deleteNote(id: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/notes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  // Relationships
  async getRelatedNotes(id: string): Promise<RelationshipResponse[]> {
    const response = await fetch(`${BASE_URL}/notes/${id}/relationships`);
    return handleResponse<RelationshipResponse[]>(response);
  },

  // Study / Spaced Repetition Reviews
  async getNotesNeedingReview(): Promise<NoteResponse[]> {
    const response = await fetch(`${BASE_URL}/notes/review-pending`);
    return handleResponse<NoteResponse[]>(response);
  },

  async reviewNote(id: string): Promise<NoteResponse> {
    const response = await fetch(`${BASE_URL}/notes/${id}/review`, {
      method: 'POST',
    });
    return handleResponse<NoteResponse>(response);
  },

  // Hybrid Search
  async searchNotes(query: string, limit = 10): Promise<NoteResponse[]> {
    const response = await fetch(`${BASE_URL}/search?query=${encodeURIComponent(query)}&limit=${limit}`);
    return handleResponse<NoteResponse[]>(response);
  },

  // Attachments
  async uploadAttachment(noteId: string, file: File): Promise<AttachmentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await fetch(`${BASE_URL}/notes/${noteId}/attachments`, {
      method: 'POST',
      body: formData, // Fetch automatically sets the correct boundary header for FormData
    });
    return handleResponse<AttachmentResponse>(response);
  },

  async getAttachmentMetadata(id: string): Promise<AttachmentResponse> {
    const response = await fetch(`${BASE_URL}/attachments/${id}`);
    return handleResponse<AttachmentResponse>(response);
  },

  async deleteAttachment(id: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/attachments/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  getAttachmentDownloadUrl(id: string): string {
    return `${BASE_URL}/attachments/${id}/download`;
  },
};

/**
 * Calculates study decay reasons based on business logic rules.
 * 1. Never reviewed and created > 24 hours ago
 * 2. Accessed recently after the last review
 * 3. Last reviewed > 30 days ago
 */
export function getDecayReasons(note: NoteResponse): string[] {
  const reasons: string[] = [];
  const now = new Date();
  const createdDate = new Date(note.createdAt);
  const accessedDate = new Date(note.lastAccessedAt);
  const reviewedDate = note.lastReviewedAt ? new Date(note.lastReviewedAt) : null;

  if (!reviewedDate) {
    const hoursSinceCreation = (now.getTime() - createdDate.getTime()) / (1000 * 60 * 60);
    if (hoursSinceCreation >= 24) {
      reasons.push('Created over 24h ago and never reviewed');
    }
  } else {
    if (accessedDate.getTime() > reviewedDate.getTime()) {
      reasons.push('Accessed after the last review');
    }
    const daysSinceReview = (now.getTime() - reviewedDate.getTime()) / (1000 * 60 * 60 * 24);
    if (daysSinceReview >= 30) {
      reasons.push('Periodic review pending (over 30 days)');
    }
  }

  return reasons;
}
