import { useState } from 'react';
import { Search, BookOpen, PlusCircle, CheckSquare, Sparkles, LayoutDashboard } from 'lucide-react';
import { HybridSearch } from './components/HybridSearch';
import { NoteViewer } from './components/NoteViewer';
import { NoteEditor } from './components/NoteEditor';
import { Dashboard } from './components/Dashboard';
import { PendingReviews } from './components/PendingReviews';
import { AllNotes } from './components/AllNotes';
import type { NoteResponse } from './types';

function App() {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'search' | 'review' | 'all' | 'create'>('dashboard');
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
              onClick={() => setActiveTab('dashboard')}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                activeTab === 'dashboard'
                  ? 'bg-bg-hover text-text-notion'
                  : 'text-text-notion-muted hover:bg-bg-hover/50 hover:text-text-notion'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              Dashboard
            </button>
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
              {activeTab === 'dashboard' ? 'Overview' : activeTab === 'search' ? 'Hybrid Search' : activeTab === 'review' ? 'Pending Reviews' : activeTab === 'all' ? 'All Notes' : 'New Note'}
            </span>
          </div>
        </header>

        {/* Workspace Content */}
        <div className="flex-1 overflow-y-auto p-8 relative">
          <div className="max-w-4xl mx-auto w-full">
            {activeTab === 'dashboard' && (
              <Dashboard />
            )}

            {activeTab === 'search' && (
              <HybridSearch onNoteClick={(note, rank) => setSelectedNote({ note, rank })} />
            )}

            {activeTab === 'review' && (
              <PendingReviews onNoteClick={(note) => setSelectedNote({ note })} />
            )}

            {activeTab === 'all' && (
              <AllNotes onNoteClick={(note) => setSelectedNote({ note })} />
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
