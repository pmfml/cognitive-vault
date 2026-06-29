import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NoteViewer } from './NoteViewer';
import { api } from '../services/api';
import type { NoteResponse, RelationshipResponse } from '../types';

vi.mock('../services/api', () => ({
  api: {
    getAttachmentsByNoteId: vi.fn(),
    getRelatedNotes: vi.fn(),
    getAttachmentDownloadUrl: vi.fn(() => '#'),
  },
}));

describe('NoteViewer Component', () => {
  const note: NoteResponse = {
    id: 'note-1',
    title: 'Primary Note',
    content: 'Primary content',
    type: 'TECHNICAL_NOTE',
    language: null,
    summary: null,
    tags: [],
    createdAt: new Date().toISOString(),
    lastAccessedAt: new Date().toISOString(),
    lastReviewedAt: null,
  };

  const relatedTarget: NoteResponse = {
    ...note,
    id: 'note-2',
    title: 'Related Target Note',
    content: 'Related content',
  };

  const relationship: RelationshipResponse = {
    relationshipId: 'rel-1',
    targetNote: relatedTarget,
    similarityScore: 0.91,
  };

  beforeEach(() => {
    vi.mocked(api.getAttachmentsByNoteId).mockResolvedValue([]);
    vi.mocked(api.getRelatedNotes).mockResolvedValue([relationship]);
  });

  it('navigates to a related note when its card is clicked', async () => {
    const onOpenNote = vi.fn();
    render(<NoteViewer note={note} onClose={vi.fn()} onOpenNote={onOpenNote} />);

    // The related note card is loaded asynchronously via the context fetch.
    const relatedCard = await screen.findByText('Related Target Note');
    fireEvent.click(relatedCard);

    expect(onOpenNote).toHaveBeenCalledTimes(1);
    expect(onOpenNote).toHaveBeenCalledWith(relatedTarget);
  });
});
