import { useState } from 'react';
import { Search, BookOpen, PlusCircle, CheckSquare, Sparkles } from 'lucide-react';
import { HybridSearch } from './components/HybridSearch';
import { NoteViewer } from './components/NoteViewer';
import { NoteEditor } from './components/NoteEditor';
import type { NoteResponse } from './types';

function App() {
  const [activeTab, setActiveTab] = useState<'search' | 'review' | 'all' | 'create'>('search');
  const [selectedNote, setSelectedNote] = useState<{note: NoteResponse, rank?: number} | null>(null);

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-bg-app text-text-notion font-sans relative">
      {/* Sidebar */}
      <aside className="w-64 border-r border-border-notion bg-bg-sidebar flex flex-col justify-between shrink-0 select-none">
        <div className="p-4 flex flex-col gap-6">
          {/* Logo / Header */}
          <div className="flex items-center gap-2.5 px-2">
            <div className="w-8 h-8 rounded-lg bg-text-notion flex items-center justify-center text-bg-app font-bold text-lg">
              C
            </div>
            <div>
              <h1 className="font-semibold text-sm leading-tight text-text-notion">Cognitive Vault</h1>
              <span className="text-[10px] text-text-notion-muted font-medium tracking-wider uppercase">Knowledge Base</span>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex flex-col gap-0.5">
            <button
              onClick={() => setActiveTab('search')}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                activeTab === 'search'
                  ? 'bg-bg-hover text-text-notion'
                  : 'text-text-notion-muted hover:bg-bg-hover/50 hover:text-text-notion'
              }`}
            >
              <Search className="w-4 h-4" />
              Hybrid Search
            </button>
            <button
              onClick={() => setActiveTab('review')}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                activeTab === 'review'
                  ? 'bg-bg-hover text-text-notion'
                  : 'text-text-notion-muted hover:bg-bg-hover/50 hover:text-text-notion'
              }`}
            >
              <CheckSquare className="w-4 h-4" />
              Pending Reviews
            </button>
            <button
              onClick={() => setActiveTab('all')}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                activeTab === 'all'
                  ? 'bg-bg-hover text-text-notion'
                  : 'text-text-notion-muted hover:bg-bg-hover/50 hover:text-text-notion'
              }`}
            >
              <BookOpen className="w-4 h-4" />
              All Notes
            </button>
            <button
              onClick={() => setActiveTab('create')}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                activeTab === 'create'
                  ? 'bg-bg-hover text-text-notion'
                  : 'text-text-notion-muted hover:bg-bg-hover/50 hover:text-text-notion'
              }`}
            >
              <PlusCircle className="w-4 h-4" />
              Create Note
            </button>
          </nav>
        </div>

        {/* Sidebar Footer */}
        <div className="p-4 border-t border-border-notion flex items-center gap-2 text-xs text-text-notion-muted">
          <Sparkles className="w-3.5 h-3.5 text-brand-blue" />
          <span>Spring Boot + React + Tailwind v4</span>
        </div>
      </aside>

      {/* Main Workspace */}
      <main className="flex-1 flex flex-col overflow-hidden bg-bg-card relative">
        {/* Workspace Top Header */}
        <header className="h-14 border-b border-border-notion flex items-center justify-between px-6 shrink-0 select-none">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-text-notion capitalize">
              {activeTab === 'search' ? 'Hybrid Search' : activeTab === 'review' ? 'Pending Reviews' : activeTab === 'all' ? 'All Notes' : 'New Note'}
            </span>
          </div>
        </header>

        {/* Workspace Content */}
        <div className="flex-1 overflow-y-auto p-8 relative">
          <div className="max-w-4xl mx-auto w-full">
            {activeTab === 'search' && (
              <HybridSearch onNoteClick={(note, rank) => setSelectedNote({ note, rank })} />
            )}

            {activeTab === 'review' && (
              <div className="flex flex-col gap-6 justify-center items-center h-[50vh] text-center">
                <div className="p-4 bg-bg-sidebar rounded-2xl border border-border-notion">
                  <CheckSquare className="w-8 h-8 text-text-notion-muted mx-auto" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-text-notion mb-1">Spaced Repetition Engine</h2>
                  <p className="text-sm text-text-notion-muted max-w-md mx-auto">
                    Here, notes pending review will be listed based on access decay. The review panel and widget will be configured shortly.
                  </p>
                </div>
              </div>
            )}

            {activeTab === 'all' && (
              <div className="flex flex-col gap-6 justify-center items-center h-[50vh] text-center">
                <div className="p-4 bg-bg-sidebar rounded-2xl border border-border-notion">
                  <BookOpen className="w-8 h-8 text-text-notion-muted mx-auto" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-text-notion mb-1">All Notes</h2>
                  <p className="text-sm text-text-notion-muted max-w-md mx-auto">
                    View the complete catalog of registered notes and snippets.
                  </p>
                </div>
              </div>
            )}

            {activeTab === 'create' && (
              <NoteEditor onSaveSuccess={() => setActiveTab('search')} />
            )}
          </div>
        </div>

        {/* NoteViewer Overlay */}
        {selectedNote && (
          <div className="absolute inset-0 z-50 bg-bg-app animate-in fade-in zoom-in-95 duration-200">
            <NoteViewer 
              note={selectedNote.note} 
              rrfRank={selectedNote.rank}
              onClose={() => setSelectedNote(null)} 
            />
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
