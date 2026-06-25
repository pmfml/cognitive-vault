import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { NoteResponse } from '../types';
import { Loader2, CheckCircle, BrainCircuit, Calendar } from 'lucide-react';
import { toast } from 'react-hot-toast';

interface PendingReviewsProps {
  onNoteClick: (note: NoteResponse) => void;
}

export function PendingReviews({ onNoteClick }: PendingReviewsProps) {
  const [notes, setNotes] = useState<NoteResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [reviewingId, setReviewingId] = useState<string | null>(null);

  const fetchPending = async () => {
    setIsLoading(true);
    try {
      const data = await api.getNotesNeedingReview();
      setNotes(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPending();
  }, []);

  const handleReview = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation(); // prevent opening the note viewer
    setReviewingId(id);
    try {
      await api.reviewNote(id);
      toast.success('Concept marked as reviewed!');
      // Optimistically remove from list
      setNotes(prev => prev.filter(n => n.id !== id));
    } catch (err) {
      console.error("Failed to mark as reviewed:", err);
      toast.error("Failed to register review.");
    } finally {
      setReviewingId(null);
    }
  };

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
        <CheckCircle className="w-12 h-12 mb-4 text-emerald-500 opacity-80" />
        <h2 className="text-xl font-bold text-text-notion mb-2">All Caught Up!</h2>
        <p className="text-sm text-center max-w-sm">
          Your Spaced Repetition queue is empty. You have reviewed all necessary concepts for today.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-text-notion flex items-center gap-2">
            <BrainCircuit className="w-6 h-6 text-brand-blue" />
            Spaced Repetition
          </h2>
          <p className="text-sm text-text-notion-muted mt-1">
            {notes.length} {notes.length === 1 ? 'concept requires' : 'concepts require'} your attention based on memory decay algorithms.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {notes.map(note => (
          <div 
            key={note.id}
            onClick={() => onNoteClick(note)}
            className="group bg-bg-card border border-border-notion rounded-xl p-5 hover:border-brand-blue/50 transition-all cursor-pointer flex flex-col justify-between h-48 shadow-sm"
          >
            <div>
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-semibold px-2 py-1 rounded bg-bg-app text-text-notion-muted uppercase tracking-wider">
                  {note.type.replace('_', ' ')}
                </span>
                <span className="text-[10px] text-text-notion-muted flex items-center gap-1 opacity-70">
                  <Calendar className="w-3 h-3" />
                  {note.lastReviewedAt ? new Date(note.lastReviewedAt).toLocaleDateString() : 'Never Reviewed'}
                </span>
              </div>
              <h3 className="font-semibold text-text-notion line-clamp-2 leading-snug">{note.title}</h3>
              <div className="flex flex-wrap gap-1.5 mt-3">
                {note.tags.slice(0, 3).map(tag => (
                  <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded-md bg-brand-blue/10 text-brand-blue font-medium">
                    #{tag}
                  </span>
                ))}
                {note.tags.length > 3 && (
                  <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-bg-app text-text-notion-muted">
                    +{note.tags.length - 3}
                  </span>
                )}
              </div>
            </div>
            
            <button
              onClick={(e) => handleReview(e, note.id)}
              disabled={reviewingId === note.id}
              className="mt-4 w-full py-2 rounded-lg bg-bg-app border border-border-notion text-sm font-medium text-text-notion-muted hover:bg-emerald-500/10 hover:text-emerald-500 hover:border-emerald-500/30 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {reviewingId === note.id ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <CheckCircle className="w-4 h-4" />
                  Mark as Reviewed
                </>
              )}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
