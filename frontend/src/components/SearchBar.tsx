
import { Search, Loader2 } from 'lucide-react';

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  isLoading?: boolean;
  placeholder?: string;
}

export function SearchBar({ value, onChange, isLoading = false, placeholder = "Search notes..." }: SearchBarProps) {
  return (
    <div className="relative w-full max-w-2xl mx-auto group">
      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
        {isLoading ? (
          <Loader2 className="h-5 w-5 text-brand-blue animate-spin" />
        ) : (
          <Search className="h-5 w-5 text-text-notion-muted group-focus-within:text-brand-blue transition-colors" />
        )}
      </div>
      <input
        type="text"
        className="block w-full pl-10 pr-3 py-3 border border-border-notion rounded-xl leading-5 bg-bg-card placeholder-text-notion-muted focus:outline-none focus:ring-2 focus:ring-brand-blue/20 focus:border-brand-blue transition-all sm:text-sm shadow-sm"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      {/* Optional: Keyboard shortcut hint (CMD+K style aesthetic) */}
      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
        <span className="text-xs text-text-notion-muted border border-border-notion rounded px-1.5 py-0.5 bg-bg-app">
          /
        </span>
      </div>
    </div>
  );
}
