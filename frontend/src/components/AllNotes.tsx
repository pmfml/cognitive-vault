import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { NoteResponse } from '../types';
import { Loader2, BookOpen } from 'lucide-react';
import { NoteCard } from './NoteCard';

interface AllNotesProps {
  onNoteClick: (note: NoteResponse) => void;
}

export function AllNotes({ onNoteClick }: AllNotesProps) {
  const [notes, setNotes] = useState<NoteResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchNotes() {
      setIsLoading(true);
      try {
        const data = await api.getNotes();
        setNotes(data || []);
      } catch (err) {
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    }
    fetchNotes();
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="w-8 h-8 text-brand-blue animate-spin" />
      </div>
    );
  }

  if (notes.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64 text-text-notion-muted animate-in fade-in zoom-in-95 duration-300">
        <BookOpen className="w-12 h-12 mb-4 opacity-50 text-text-notion-muted" />
        <h2 className="text-xl font-bold text-text-notion mb-2">No Notes Found</h2>
        <p className="text-sm">Your vault is currently empty. Go create some knowledge!</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-text-notion flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-brand-blue" />
            All Notes
          </h2>
          <p className="text-sm text-text-notion-muted mt-1">
            Browse through your entire cognitive repository ({notes.length} items).
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {notes.map(note => (
          <NoteCard 
            key={note.id} 
            note={note} 
            onClick={() => onNoteClick(note)} 
          />
        ))}
      </div>
    </div>
  );
}
