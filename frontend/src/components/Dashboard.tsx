import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { NoteResponse } from '../types';
import { BookOpen, CheckSquare, Hash, Loader2 } from 'lucide-react';

export function Dashboard() {
  const [isLoading, setIsLoading] = useState(true);
  const [notes, setNotes] = useState<NoteResponse[]>([]);
  const [pendingReviews, setPendingReviews] = useState<NoteResponse[]>([]);

  useEffect(() => {
    async function fetchStats() {
      setIsLoading(true);
      try {
        const [allNotes, reviews] = await Promise.all([
          api.getNotes(),
          api.getNotesNeedingReview()
        ]);
        setNotes(allNotes || []);
        setPendingReviews(reviews || []);
      } catch (err) {
        console.error("Failed to fetch dashboard data:", err);
      } finally {
        setIsLoading(false);
      }
    }
    fetchStats();
  }, []);

  // Aggregations
  const totalNotes = notes.length;
  
  // Calculate unique tags
  const uniqueTags = new Set<string>();
  notes.forEach(note => {
    note.tags.forEach(tag => uniqueTags.add(tag));
  });
  const totalTags = uniqueTags.size;

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="w-8 h-8 text-text-notion-muted animate-spin" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-text-notion">Dashboard Overview</h2>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        <div className="bg-bg-card border border-border-notion rounded-xl p-5 flex flex-col gap-3 shadow-sm hover:border-brand-blue/30 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-text-notion-muted uppercase tracking-wider">Total Notes</span>
            <div className="p-2 bg-brand-blue/10 text-brand-blue rounded-lg">
              <BookOpen className="w-5 h-5" />
            </div>
          </div>
          <span className="text-3xl font-bold text-text-notion">{totalNotes}</span>
        </div>

        <div className="bg-bg-card border border-border-notion rounded-xl p-5 flex flex-col gap-3 shadow-sm hover:border-red-500/30 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-text-notion-muted uppercase tracking-wider">Pending Reviews</span>
            <div className="p-2 bg-red-500/10 text-red-500 rounded-lg">
              <CheckSquare className="w-5 h-5" />
            </div>
          </div>
          <span className="text-3xl font-bold text-text-notion">{pendingReviews.length}</span>
        </div>

        <div className="bg-bg-card border border-border-notion rounded-xl p-5 flex flex-col gap-3 shadow-sm hover:border-emerald-500/30 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-text-notion-muted uppercase tracking-wider">Unique Tags</span>
            <div className="p-2 bg-emerald-500/10 text-emerald-500 rounded-lg">
              <Hash className="w-5 h-5" />
            </div>
          </div>
          <span className="text-3xl font-bold text-text-notion">{totalTags}</span>
        </div>

      </div>

      {/* Chart Placeholders for 6.3.2 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-2">
         <div className="h-72 border border-dashed border-border-notion bg-bg-app/50 rounded-xl flex items-center justify-center text-sm text-text-notion-muted">
           Charts will be implemented in Parte 6.3.2
         </div>
      </div>
    </div>
  );
}
