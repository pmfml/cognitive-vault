import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { NoteResponse } from '../types';
import { SearchBar } from './SearchBar';
import { NoteCard } from './NoteCard';
import { AlertCircle, Sparkles } from 'lucide-react';

export function HybridSearch() {
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [results, setResults] = useState<NoteResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Debounce the query input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(query);
    }, 400);

    return () => {
      clearTimeout(handler);
    };
  }, [query]);

  // Execute the search whenever the debounced query changes
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setResults([]);
      return;
    }

    async function executeSearch() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await api.searchNotes(debouncedQuery, 20); // limit 20 for full screen layout
        setResults(data);
      } catch (err: any) {
        setError(err.message || 'Failed to perform search.');
        setResults([]);
      } finally {
        setIsLoading(false);
      }
    }

    executeSearch();
  }, [debouncedQuery]);

  return (
    <div className="flex flex-col h-full gap-8 max-w-3xl mx-auto w-full pb-8">
      {/* Header and Search Input */}
      <div className="flex flex-col gap-3">
        <SearchBar 
          value={query} 
          onChange={setQuery} 
          isLoading={isLoading} 
          placeholder="Search semantic notes or technical snippets..."
        />
        {error && (
          <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <p>{error}</p>
          </div>
        )}
      </div>

      {/* Results or Empty State */}
      <div className="flex-1 overflow-y-auto pr-2 flex flex-col gap-4 min-h-[50vh]">
        {!debouncedQuery.trim() && !isLoading && (
          <div className="flex flex-col items-center justify-center h-full text-center text-text-notion-muted py-20 opacity-80">
            <Sparkles className="w-10 h-10 mb-3 text-border-notion" />
            <p className="font-medium text-text-notion">Ready for your query</p>
            <p className="text-sm mt-1 max-w-sm">
              Use keywords to trigger Elasticsearch, or natural sentences to engage pgvector semantic matching.
            </p>
          </div>
        )}

        {debouncedQuery.trim() && !isLoading && results.length === 0 && !error && (
          <div className="text-center py-20 text-text-notion-muted">
            <p className="font-medium text-text-notion">No results found</p>
            <p className="text-sm mt-1">Try adjusting your search terms.</p>
          </div>
        )}

        {results.map((note, index) => (
          <NoteCard 
            key={note.id} 
            note={note} 
            rrfRank={index + 1} // The array order maps exactly to the RRF rank calculated in the backend!
          />
        ))}
      </div>
    </div>
  );
}
