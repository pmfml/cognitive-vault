
import type { NoteResponse } from '../types';
import { FileText, Code2, Calendar, Target } from 'lucide-react';

interface NoteCardProps {
  note: NoteResponse;
  rrfRank?: number; // Optional visual rank indicator
  onClick?: () => void;
}

export function NoteCard({ note, rrfRank, onClick }: NoteCardProps) {
  // Format the date concisely
  const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });

  // Truncate content for the snippet preview
  const snippet = note.summary 
    ? note.summary 
    : note.content.length > 150 
      ? note.content.substring(0, 150) + '...' 
      : note.content;

  return (
    <div 
      onClick={onClick}
      className="bg-bg-card border border-border-notion rounded-xl p-5 hover:border-brand-blue/30 hover:shadow-md transition-all cursor-pointer group flex flex-col gap-3"
    >
      <div className="flex justify-between items-start gap-4">
        {/* Title and Icon */}
        <div className="flex items-start gap-3">
          <div className="mt-1 text-text-notion-muted group-hover:text-brand-blue transition-colors">
            {note.type === 'CODE_SNIPPET' ? <Code2 className="w-5 h-5" /> : <FileText className="w-5 h-5" />}
          </div>
          <div>
            <h3 className="font-semibold text-text-notion text-base leading-tight group-hover:text-brand-blue transition-colors">
              {note.title}
            </h3>
            <div className="flex items-center gap-3 mt-1.5 text-xs text-text-notion-muted font-medium">
              <span className="flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                {formattedDate}
              </span>
              <span className="bg-bg-app border border-border-notion px-1.5 py-0.5 rounded text-[10px] tracking-wide uppercase">
                {note.type === 'CODE_SNIPPET' ? note.language || 'CODE' : 'NOTE'}
              </span>
            </div>
          </div>
        </div>

        {/* Optional RRF Rank Badge */}
        {rrfRank !== undefined && (
          <div className="flex items-center gap-1.5 bg-[#f5f3f0] text-[#787774] px-2 py-1 rounded text-xs font-semibold border border-[#e9e9e7]">
            <Target className="w-3.5 h-3.5" />
            <span>RRF Rank #{rrfRank}</span>
          </div>
        )}
      </div>

      {/* Snippet Preview */}
      <p className="text-sm text-text-notion-muted leading-relaxed line-clamp-2">
        {snippet}
      </p>

      {/* Tags */}
      {note.tags && note.tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-1">
          {note.tags.map((tag) => (
            <span 
              key={tag} 
              className="px-2 py-0.5 bg-bg-app text-text-notion-muted text-xs rounded-md border border-border-notion"
            >
              #{tag}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
