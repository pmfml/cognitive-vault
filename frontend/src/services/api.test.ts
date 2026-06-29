import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from './api';
import type { NoteResponse } from '../types';

describe('API Service', () => {
  beforeEach(() => {
    // Intercept global fetch so we don't make real network calls
    global.fetch = vi.fn();
  });

  it('getNotes should fetch notes list from backend', async () => {
    const mockNotes: NoteResponse[] = [
      { id: '1', title: 'Test 1', content: '...', type: 'TECHNICAL_NOTE', tags: [], createdAt: '', lastAccessedAt: '' }
    ];
    
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockNotes
    });

    const result = await api.getNotes();
    expect(result).toEqual(mockNotes);
    expect(global.fetch).toHaveBeenCalledWith('/api/v1/notes');
  });

  it('createNote should post payload and return response object', async () => {
    const mockResponse: NoteResponse = { 
      id: '2', title: 'New', content: 'Content', type: 'TECHNICAL_NOTE', tags: ['new'], createdAt: '', lastAccessedAt: '' 
    };
    
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse
    });

    const payload = { title: 'New', content: 'Content', type: 'TECHNICAL_NOTE', tags: ['new'] };
    const result = await api.createNote(payload as any);
    
    expect(result).toEqual(mockResponse);
    expect(global.fetch).toHaveBeenCalledWith('/api/v1/notes', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(payload)
    }));
  });
});
