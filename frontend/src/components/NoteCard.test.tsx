import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { NoteCard } from './NoteCard';
import type { NoteResponse } from '../types';

describe('NoteCard Component', () => {
  const mockNote: NoteResponse = {
    id: '123',
    title: 'Testing React Component',
    content: 'This is a test note to verify component rendering.',
    type: 'TECHNICAL_NOTE',
    tags: ['react', 'vitest'],
    createdAt: new Date().toISOString(),
    lastAccessedAt: new Date().toISOString(),
  };

  it('renders title and type correctly', () => {
    render(<NoteCard note={mockNote} onClick={vi.fn()} />);
    
    expect(screen.getByText('Testing React Component')).toBeInTheDocument();
    expect(screen.getByText('NOTE')).toBeInTheDocument();
  });

  it('renders tags perfectly formatted', () => {
    render(<NoteCard note={mockNote} onClick={vi.fn()} />);
    
    expect(screen.getByText('#react')).toBeInTheDocument();
    expect(screen.getByText('#vitest')).toBeInTheDocument();
  });

  it('shows RRF rank badge only when provided by search', () => {
    render(<NoteCard note={mockNote} rrfRank={1} onClick={vi.fn()} />);
    
    expect(screen.getByText('RRF Rank #1')).toBeInTheDocument();
  });
});
