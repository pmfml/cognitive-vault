import { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { api } from '../services/api';
import type { NoteRequest, NoteType, NoteResponse } from '../types';
import { Save, AlertCircle, Loader2, X, Plus, CheckCircle2 } from 'lucide-react';
import { FileUploader } from './FileUploader';
import { toast } from 'react-hot-toast';

interface NoteEditorProps {
  initialNote?: NoteResponse | null;
  onSaveSuccess: () => void;
}

export function NoteEditor({ initialNote, onSaveSuccess }: NoteEditorProps) {
  const [title, setTitle] = useState('');
  const [type, setType] = useState<NoteType>('TECHNICAL_NOTE');
  const [language, setLanguage] = useState('');
  const [content, setContent] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  
  const [tagInput, setTagInput] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedNoteId, setSavedNoteId] = useState<string | null>(null);

  useEffect(() => {
    if (initialNote) {
      setTitle(initialNote.title);
      setType(initialNote.type);
      setLanguage(initialNote.language || '');
      setContent(initialNote.content);
      setTags(initialNote.tags || []);
    }
  }, [initialNote]);

  const handleAddTag = () => {
    const trimmed = tagInput.trim();
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(t => t !== tagToRemove));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddTag();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("Title and Content are required.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const payload: NoteRequest = {
      title: title.trim(),
      content: content.trim(),
      type,
      language: type === 'CODE_SNIPPET' ? language.trim() : undefined,
      tags
    };

    try {
      let response;
      if (initialNote) {
        response = await api.updateNote(initialNote.id, payload);
      } else {
        response = await api.createNote(payload);
      }
      setSavedNoteId(response.id);
      toast.success(initialNote ? 'Note updated successfully!' : 'Note created successfully!');
    } catch (err: any) {
      console.error("Failed to save note:", err);
      setError(err.message || "An unexpected error occurred while saving.");
      toast.error(err.message || "Failed to save the note.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (savedNoteId) {
    return (
      <div className="flex flex-col h-full bg-bg-card animate-in slide-in-from-right duration-300">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-bold text-text-notion flex items-center gap-2">
              <CheckCircle2 className="w-6 h-6 text-green-500" />
              Note {initialNote ? 'Updated' : 'Created'}!
            </h2>
            <p className="text-sm text-text-notion-muted mt-1">You can now attach files or finish.</p>
          </div>
          <button
            onClick={onSaveSuccess}
            className="bg-brand-blue text-white px-4 py-2 rounded-md font-medium text-sm hover:bg-brand-blue/90 transition-colors"
          >
            Finish & Return
          </button>
        </div>
        
        <div className="bg-bg-app border border-border-notion rounded-xl p-6">
          <FileUploader noteId={savedNoteId} onUploadComplete={onSaveSuccess} />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-bg-card animate-in fade-in duration-200">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-text-notion">{initialNote ? 'Edit Note' : 'Create Note'}</h2>
        <button 
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="flex items-center gap-2 bg-brand-blue text-white px-4 py-2 rounded-md font-medium text-sm hover:bg-brand-blue/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          {isSubmitting ? 'Saving...' : 'Save Note'}
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md flex items-start gap-3">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div className="text-sm">{error}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-6 flex-1 overflow-hidden">
        
        {/* Top Metadata Section */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 shrink-0">
          <div className="flex flex-col gap-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted">Title *</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. React Hooks Architecture"
              className="px-3 py-2 bg-bg-app border border-border-notion rounded-md text-text-notion focus:outline-none focus:border-brand-blue focus:ring-1 focus:ring-brand-blue transition-shadow"
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted">Note Type *</label>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="type"
                  value="TECHNICAL_NOTE"
                  checked={type === 'TECHNICAL_NOTE'}
                  onChange={(e) => setType(e.target.value as NoteType)}
                  className="text-brand-blue focus:ring-brand-blue cursor-pointer"
                />
                <span className="text-sm text-text-notion font-medium">Technical Note</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="type"
                  value="CODE_SNIPPET"
                  checked={type === 'CODE_SNIPPET'}
                  onChange={(e) => setType(e.target.value as NoteType)}
                  className="text-brand-blue focus:ring-brand-blue cursor-pointer"
                />
                <span className="text-sm text-text-notion font-medium">Code Snippet</span>
              </label>
            </div>
          </div>

          {type === 'CODE_SNIPPET' && (
            <div className="flex flex-col gap-2 animate-in fade-in duration-200">
              <label className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted">Language</label>
              <input
                type="text"
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                placeholder="e.g. typescript, python"
                className="px-3 py-2 bg-bg-app border border-border-notion rounded-md text-text-notion focus:outline-none focus:border-brand-blue focus:ring-1 focus:ring-brand-blue transition-shadow"
              />
            </div>
          )}

          <div className="flex flex-col gap-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted">Tags</label>
            <div className="flex flex-wrap gap-2 items-center bg-bg-app border border-border-notion rounded-md px-2 py-1.5 focus-within:border-brand-blue focus-within:ring-1 focus-within:ring-brand-blue transition-shadow">
              {tags.map(tag => (
                <span key={tag} className="flex items-center gap-1 px-2 py-0.5 bg-bg-sidebar border border-border-notion rounded text-xs text-text-notion">
                  #{tag}
                  <button type="button" onClick={() => handleRemoveTag(tag)} className="text-text-notion-muted hover:text-red-500">
                    <X className="w-3 h-3" />
                  </button>
                </span>
              ))}
              <input
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Type and press Enter..."
                className="flex-1 min-w-[120px] bg-transparent text-sm text-text-notion focus:outline-none py-0.5 px-1"
              />
              <button 
                type="button" 
                onClick={handleAddTag}
                className="p-1 text-text-notion-muted hover:text-brand-blue shrink-0"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Bottom Markdown Section (Split Screen) */}
        <div className="flex-1 flex flex-col gap-2 min-h-[400px]">
          <label className="text-xs font-semibold uppercase tracking-wider text-text-notion-muted flex justify-between items-end">
            <span>Content * (Markdown)</span>
            <span className="text-[10px] bg-bg-sidebar px-2 py-0.5 rounded border border-border-notion">Live Preview</span>
          </label>
          <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4 border border-border-notion rounded-md overflow-hidden bg-bg-app">
            
            {/* Editor */}
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="# Enter your markdown content here..."
              className="w-full h-full resize-none bg-transparent p-4 text-sm font-mono text-text-notion focus:outline-none border-r border-border-notion"
              required
            />

            {/* Live Preview */}
            <div className="w-full h-full overflow-y-auto p-4 bg-bg-card">
              {content ? (
                <div className="prose prose-sm prose-slate prose-img:rounded-xl prose-a:text-brand-blue hover:prose-a:text-brand-blue/80 prose-code:text-brand-blue prose-pre:bg-bg-sidebar prose-pre:border prose-pre:border-border-notion max-w-none text-text-notion">
                  <ReactMarkdown>{content}</ReactMarkdown>
                </div>
              ) : (
                <div className="h-full flex items-center justify-center text-text-notion-muted text-sm italic">
                  Preview will appear here...
                </div>
              )}
            </div>
            
          </div>
        </div>

      </form>
    </div>
  );
}
