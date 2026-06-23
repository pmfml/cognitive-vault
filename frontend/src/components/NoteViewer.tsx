
import ReactMarkdown from 'react-markdown';
import type { NoteResponse } from '../types';
import { X, Calendar, Target, FileText, Code2, Link, Paperclip } from 'lucide-react';

interface NoteViewerProps {
  note: NoteResponse;
  onClose: () => void;
  rrfRank?: number;
}

export function NoteViewer({ note, onClose, rrfRank }: NoteViewerProps) {
  const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <div className="flex h-full w-full bg-bg-app animate-in fade-in zoom-in-95 duration-200">
      {/* Left Pane: Markdown Reader */}
      <div className="flex-1 flex flex-col h-full border-r border-border-notion bg-bg-card overflow-y-auto">
        <div className="max-w-3xl mx-auto w-full px-12 py-10">
          
          {/* Header */}
          <div className="mb-8 flex flex-col gap-4">
            <button 
              onClick={onClose}
              className="flex items-center gap-1.5 text-sm text-text-notion-muted hover:text-brand-blue transition-colors self-start mb-2"
            >
              <X className="w-4 h-4" />
              Close Viewer
            </button>
            
            <h1 className="text-3xl font-bold text-text-notion leading-tight">
              {note.title}
            </h1>
            
            <div className="flex flex-wrap items-center gap-4 text-sm text-text-notion-muted font-medium">
              <span className="flex items-center gap-1.5">
                {note.type === 'CODE_SNIPPET' ? <Code2 className="w-4 h-4" /> : <FileText className="w-4 h-4" />}
                {note.type === 'CODE_SNIPPET' ? note.language || 'Code Snippet' : 'Technical Note'}
              </span>
              <span className="flex items-center gap-1.5">
                <Calendar className="w-4 h-4" />
                {formattedDate}
              </span>
              {rrfRank !== undefined && (
                <span className="flex items-center gap-1.5 bg-[#f5f3f0] text-[#787774] px-2 py-0.5 rounded border border-[#e9e9e7]">
                  <Target className="w-3.5 h-3.5" />
                  RRF Rank #{rrfRank}
                </span>
              )}
            </div>

            {/* Tags */}
            {note.tags && note.tags.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {note.tags.map((tag) => (
                  <span 
                    key={tag} 
                    className="px-2.5 py-1 bg-bg-app text-text-notion text-xs rounded-md border border-border-notion shadow-sm"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            )}
          </div>

          <hr className="border-border-notion mb-8" />

          {/* Markdown Content */}
          <div className="prose prose-slate prose-img:rounded-xl prose-a:text-brand-blue hover:prose-a:text-brand-blue/80 prose-code:text-brand-blue prose-pre:bg-bg-sidebar prose-pre:border prose-pre:border-border-notion max-w-none text-text-notion">
            <ReactMarkdown>
              {note.content}
            </ReactMarkdown>
          </div>
        </div>
      </div>

      {/* Right Pane: Context & Attachments (Placeholders for 5.3.2) */}
      <div className="w-80 shrink-0 bg-bg-sidebar flex flex-col h-full overflow-y-auto">
        <div className="p-6 flex flex-col gap-8">
          
          {/* Attachments Section */}
          <div className="flex flex-col gap-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted flex items-center gap-2">
              <Paperclip className="w-3.5 h-3.5" />
              Attachments
            </h3>
            <div className="text-sm text-text-notion border border-border-notion border-dashed rounded-lg p-4 text-center bg-bg-app/50">
              Fetching attachments...
            </div>
          </div>

          {/* Related Notes Section */}
          <div className="flex flex-col gap-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted flex items-center gap-2">
              <Link className="w-3.5 h-3.5" />
              Semantic Relationships
            </h3>
            <div className="text-sm text-text-notion border border-border-notion border-dashed rounded-lg p-4 text-center bg-bg-app/50">
              Calculating vectors...
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
