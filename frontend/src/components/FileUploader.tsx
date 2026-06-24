import { useState, useRef } from 'react';
import { api } from '../services/api';
import { UploadCloud, File, X, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export type FileStatus = 'pending' | 'uploading' | 'success' | 'error';

export interface UploadableFile {
  id: string;
  file: File;
  status: FileStatus;
}

interface FileUploaderProps {
  noteId: string;
  onUploadComplete?: () => void;
}

export function FileUploader({ noteId, onUploadComplete }: FileUploaderProps) {
  const [files, setFiles] = useState<UploadableFile[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      addFiles(Array.from(e.dataTransfer.files));
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(Array.from(e.target.files));
    }
  };

  const addFiles = (newFiles: File[]) => {
    const uploadableFiles: UploadableFile[] = newFiles.map(file => ({
      id: Math.random().toString(36).substring(7),
      file,
      status: 'pending'
    }));
    setFiles(prev => [...prev, ...uploadableFiles]);
  };

  const removeFile = (id: string) => {
    setFiles(prev => prev.filter(f => f.id !== id));
  };

  const updateFileStatus = (id: string, status: FileStatus) => {
    setFiles(prev => prev.map(f => f.id === id ? { ...f, status } : f));
  };

  const handleUploadClick = async () => {
    const filesToUpload = files.filter(f => f.status === 'pending' || f.status === 'error');
    if (filesToUpload.length === 0) return;

    let hasError = false;

    // Upload files in parallel
    const promises = filesToUpload.map(async (item) => {
      updateFileStatus(item.id, 'uploading');
      try {
        await api.uploadAttachment(noteId, item.file);
        updateFileStatus(item.id, 'success');
      } catch (err) {
        console.error(`Error uploading ${item.file.name}:`, err);
        updateFileStatus(item.id, 'error');
        hasError = true;
      }
    });

    await Promise.all(promises);

    if (!hasError && onUploadComplete) {
      onUploadComplete();
    }
  };

  return (
    <div className="flex flex-col gap-4 w-full">
      {/* Dropzone */}
      <div 
        className={`border-2 border-dashed rounded-xl p-8 flex flex-col items-center justify-center text-center transition-colors cursor-pointer
          ${isDragging ? 'border-brand-blue bg-brand-blue/5' : 'border-border-notion bg-bg-app hover:bg-bg-sidebar hover:border-brand-blue/30'}
        `}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <UploadCloud className={`w-10 h-10 mb-3 ${isDragging ? 'text-brand-blue' : 'text-text-notion-muted'}`} />
        <h3 className="text-sm font-semibold text-text-notion mb-1">Click or drag files to upload</h3>
        <p className="text-xs text-text-notion-muted">PDFs, Images, TXT files up to 10MB</p>
        <input 
          type="file" 
          multiple 
          className="hidden" 
          ref={fileInputRef}
          onChange={handleFileInput}
        />
      </div>

      {/* File List */}
      {files.length > 0 && (
        <div className="flex flex-col gap-2 mt-2">
          {files.map(item => (
            <div key={item.id} className="flex items-center justify-between p-3 bg-bg-card border border-border-notion rounded-lg">
              <div className="flex items-center gap-3 overflow-hidden">
                <File className="w-4 h-4 shrink-0 text-brand-blue" />
                <span className="text-sm text-text-notion truncate">{item.file.name}</span>
                <span className="text-xs text-text-notion-muted shrink-0">
                  {(item.file.size / 1024 / 1024).toFixed(2)} MB
                </span>
              </div>
              
              <div className="flex items-center gap-2 shrink-0">
                {item.status === 'pending' && (
                  <button onClick={() => removeFile(item.id)} className="text-text-notion-muted hover:text-red-500 p-1">
                    <X className="w-4 h-4" />
                  </button>
                )}
                {item.status === 'uploading' && <Loader2 className="w-4 h-4 text-brand-blue animate-spin" />}
                {item.status === 'success' && <CheckCircle2 className="w-4 h-4 text-green-500" />}
                {item.status === 'error' && <AlertCircle className="w-4 h-4 text-red-500" />}
              </div>
            </div>
          ))}
          
          <button 
            onClick={handleUploadClick}
            disabled={files.every(f => f.status === 'success')}
            className="mt-2 w-full py-2 bg-brand-blue text-white rounded-md text-sm font-medium hover:bg-brand-blue/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Start Upload
          </button>
        </div>
      )}
    </div>
  );
}
