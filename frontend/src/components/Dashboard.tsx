import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { NoteResponse } from '../types';
import { BookOpen, CheckSquare, Hash, Loader2 } from 'lucide-react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

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
  
  const tagCounts: Record<string, number> = {};
  notes.forEach(note => {
    note.tags.forEach(tag => {
      tagCounts[tag] = (tagCounts[tag] || 0) + 1;
    });
  });
  const totalTags = Object.keys(tagCounts).length;

  const barData = Object.entries(tagCounts)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  const technicalCount = notes.filter(n => n.type === 'TECHNICAL_NOTE').length;
  const snippetCount = notes.filter(n => n.type === 'CODE_SNIPPET').length;
  
  const pieData = [
    { name: 'Technical Notes', value: technicalCount },
    { name: 'Code Snippets', value: snippetCount }
  ];
  
  const COLORS = ['#2563eb', '#10b981']; // Brand Blue, Emerald

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

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-2">
         {/* Pie Chart */}
         <div className="bg-bg-card border border-border-notion rounded-xl p-6 flex flex-col shadow-sm">
           <h3 className="text-sm font-semibold text-text-notion mb-4 uppercase tracking-wider">Note Distribution</h3>
           <div className="flex-1 min-h-[250px]">
             <ResponsiveContainer width="100%" height="100%">
               <PieChart>
                 <Pie
                   data={pieData}
                   innerRadius={60}
                   outerRadius={80}
                   paddingAngle={5}
                   dataKey="value"
                 >
                   {pieData.map((_, index) => (
                     <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                   ))}
                 </Pie>
                 <Tooltip 
                   contentStyle={{ backgroundColor: '#1e1e1e', border: '1px solid #333', borderRadius: '8px' }}
                   itemStyle={{ color: '#e5e5e5' }}
                 />
               </PieChart>
             </ResponsiveContainer>
           </div>
           <div className="flex justify-center gap-6 mt-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-brand-blue"></div>
                <span className="text-xs text-text-notion-muted">Technical Notes ({technicalCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-emerald-500"></div>
                <span className="text-xs text-text-notion-muted">Code Snippets ({snippetCount})</span>
              </div>
           </div>
         </div>

         {/* Bar Chart */}
         <div className="bg-bg-card border border-border-notion rounded-xl p-6 flex flex-col shadow-sm">
           <h3 className="text-sm font-semibold text-text-notion mb-4 uppercase tracking-wider">Top 5 Tags</h3>
           <div className="flex-1 min-h-[250px]">
             <ResponsiveContainer width="100%" height="100%">
               <BarChart data={barData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                 <XAxis dataKey="name" stroke="#525252" fontSize={12} tickLine={false} axisLine={false} />
                 <YAxis stroke="#525252" fontSize={12} tickLine={false} axisLine={false} allowDecimals={false} />
                 <Tooltip 
                   cursor={{ fill: '#333333', opacity: 0.4 }}
                   contentStyle={{ backgroundColor: '#1e1e1e', border: '1px solid #333', borderRadius: '8px' }}
                   itemStyle={{ color: '#e5e5e5' }}
                 />
                 <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} />
               </BarChart>
             </ResponsiveContainer>
           </div>
         </div>
      </div>
    </div>
  );
}
