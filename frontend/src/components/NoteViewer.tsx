import { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import type { NoteResponse, AttachmentResponse, RelationshipResponse } from '../types';
import { api } from '../services/api';
import { X, Calendar, Target, FileText, Code2, Link, Paperclip, Loader2, Plus, Trash2, Edit3 } from 'lucide-react';
import { FileUploader } from './FileUploader';
import { toast } from 'react-hot-toast';

interface NoteViewerProps {
  note: NoteResponse;
  rrfRank?: number;
  onClose: () => void;
  onEditNote?: (note: NoteResponse) => void;
  onDeleteSuccess?: () => void;
}

export function NoteViewer({ note, rrfRank, onClose, onEditNote, onDeleteSuccess }: NoteViewerProps) {
  const [attachments, setAttachments] = useState<AttachmentResponse[]>([]);
  const [relatedNotes, setRelatedNotes] = useState<RelationshipResponse[]>([]);
  const [isLoadingContext, setIsLoadingContext] = useState(true);
  const [isUploadingMode, setIsUploadingMode] = useState(false);

  const fetchContext = async () => {
    setIsLoadingContext(true);
    try {
      const [attData, relData] = await Promise.all([
        api.getAttachmentsByNoteId(note.id),
        api.getRelatedNotes(note.id)
      ]);
      setAttachments(attData || []);
      setRelatedNotes(relData || []);
    } catch (err) {
      console.error("Failed to fetch note context", err);
    } finally {
      setIsLoadingContext(false);
    }
  };

  useEffect(() => {
    fetchContext();
  }, [note.id]);

  const handleDeleteNote = async () => {
    if (window.confirm('Are you sure you want to permanently delete this note?')) {
      try {
        await api.deleteNote(note.id);
        toast.success('Note deleted permanently.');
        if (onDeleteSuccess) onDeleteSuccess();
      } catch (err) {
        console.error("Failed to delete note", err);
        toast.error("Failed to delete the note. Please try again.");
      }
    }
  };

  const handleDeleteAttachment = async (e: React.MouseEvent, attId: string) => {
    e.preventDefault();
    if (window.confirm('Delete this attachment permanently?')) {
      try {
        await api.deleteAttachment(attId);
        toast.success('Attachment deleted.');
        fetchContext(); // Refresh attachments
      } catch (err) {
        console.error("Failed to delete attachment", err);
        toast.error("Failed to delete attachment.");
      }
    }
  };

  const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <div className="flex h-full w-full bg-bg-app animate-in fade-in zoom-in-95 duration-200">
      {/* Left Pane: Markdown Reader */}
      <div className="flex-1 flex flex-col h-full border-r border-border-notion bg-bg-card overflow-y-auto">
        
        {/* Header */}
        <div className="h-14 border-b border-border-notion px-6 flex items-center justify-between shrink-0 bg-bg-card">
          <div className="flex items-center gap-3">
            <button onClick={onClose} className="p-1.5 hover:bg-bg-hover rounded-md text-text-notion-muted transition-colors">
              <X className="w-4 h-4" />
            </button>
            <span className="text-sm font-medium text-text-notion">Reading View</span>
          </div>
          <div className="flex items-center gap-2">
            {onEditNote && (
              <button 
                onClick={() => onEditNote(note)}
                className="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-notion-muted hover:text-text-notion hover:bg-bg-hover rounded-md transition-colors"
                title="Edit Note"
              >
                <Edit3 className="w-4 h-4" />
                Edit
              </button>
            )}
            <button 
              onClick={handleDeleteNote}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-500 hover:bg-red-500/10 rounded-md transition-colors"
              title="Delete Note"
            >
              <Trash2 className="w-4 h-4" />
              Delete
            </button>
          </div>
        </div>

        <div className="max-w-3xl mx-auto w-full px-12 py-10">
          <h1 className="text-3xl font-bold text-text-notion leading-tight mb-8">
            {note.title}
          </h1>
            
            <div className="flex flex-wrap items-center gap-4 text-sm text-text-notion-muted font-medium mb-8">
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

            <hr className="border-border-notion my-8" />

            {/* Markdown Content */}
            <div className="prose prose-slate prose-img:rounded-xl prose-a:text-brand-blue hover:prose-a:text-brand-blue/80 prose-code:text-brand-blue prose-pre:bg-bg-sidebar prose-pre:border prose-pre:border-border-notion max-w-none text-text-notion pb-16">
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
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted flex items-center gap-2">
                <Paperclip className="w-3.5 h-3.5" />
                Attachments
              </h3>
              {!isUploadingMode && (
                <button 
                  onClick={() => setIsUploadingMode(true)}
                  className="text-text-notion-muted hover:text-brand-blue"
                  title="Upload new attachment"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {isUploadingMode ? (
              <div className="animate-in fade-in slide-in-from-top-2 duration-200">
                <FileUploader 
                  noteId={note.id} 
                  onUploadComplete={() => {
                    setIsUploadingMode(false);
                    fetchContext(); // Refresh the list
                  }} 
                />
                <button 
                  onClick={() => setIsUploadingMode(false)}
                  className="w-full mt-2 py-1.5 text-xs text-text-notion-muted hover:text-text-notion transition-colors"
                >
                  Cancel Upload
                </button>
              </div>
            ) : isLoadingContext ? (
              <div className="flex justify-center p-4">
                <Loader2 className="w-5 h-5 text-text-notion-muted animate-spin" />
              </div>
            ) : attachments.length > 0 ? (
              <div className="flex flex-col gap-2">
                {attachments.map((att) => (
                  <div key={att.id} className="flex items-center justify-between p-2.5 bg-bg-card border border-border-notion rounded-lg hover:border-brand-blue/30 transition-colors group">
                    <a 
                      href={api.getAttachmentDownloadUrl(att.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-2 overflow-hidden flex-1 cursor-pointer"
                      title="Download file"
                    >
                      <Paperclip className="w-4 h-4 text-text-notion-muted shrink-0" />
                      <span className="text-xs font-medium truncate text-text-notion group-hover:text-brand-blue transition-colors">{att.fileName}</span>
                    </a>
                    <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={(e) => handleDeleteAttachment(e, att.id)}
                        className="p-1 text-red-500 hover:bg-red-500/10 rounded"
                        title="Delete attachment"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-xs text-text-notion-muted border border-border-notion border-dashed rounded-lg p-3 text-center bg-bg-app/50">
                No attachments found.
              </div>
            )}
          </div>

          {/* Related Notes Section */}
          <div className="flex flex-col gap-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted flex items-center gap-2">
              <Link className="w-3.5 h-3.5" />
              Semantic Relationships
            </h3>
            {isLoadingContext ? (
              <div className="flex justify-center p-4">
                <Loader2 className="w-5 h-5 text-text-notion-muted animate-spin" />
              </div>
            ) : relatedNotes.length > 0 ? (
              <div className="flex flex-col gap-2">
                {relatedNotes.map((rel) => (
                  <div 
                    key={rel.relationshipId}
                    className="flex flex-col gap-1.5 p-3 bg-bg-card border border-border-notion rounded-lg hover:border-brand-blue/30 transition-colors cursor-pointer group"
                  >
                    <h4 className="text-xs font-medium text-text-notion leading-tight group-hover:text-brand-blue transition-colors">
                      {rel.targetNote.title}
                    </h4>
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-text-notion-muted tracking-wide uppercase">
                        {rel.targetNote.type === 'CODE_SNIPPET' ? 'Snippet' : 'Note'}
                      </span>
                      <span className="text-[10px] font-semibold bg-bg-sidebar px-1.5 py-0.5 rounded border border-border-notion text-text-notion-muted">
                        {Math.round(rel.similarityScore * 100)}% match
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-xs text-text-notion-muted border border-border-notion border-dashed rounded-lg p-3 text-center bg-bg-app/50">
                No related notes found.
              </div>
            )}
          </div>

        </div>
      </div>
    </div>
  );
}
