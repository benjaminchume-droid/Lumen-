import { useMemo } from "react";
import { Clock, Play, Trash, History, CalendarDays } from "lucide-react";
import { motion } from "motion/react";
import { HistoryEntry } from "../types";

interface HistoryViewProps {
  history: HistoryEntry[];
  onResumeReading: (mediaId: string, chapterId: string) => void;
  onClearHistory: () => void;
}

export default function HistoryView({ history, onResumeReading, onClearHistory }: HistoryViewProps) {
  
  // Chronological timeline sorter
  const groupedTimeline = useMemo(() => {
    const today: HistoryEntry[] = [];
    const yesterday: HistoryEntry[] = [];
    const thisWeek: HistoryEntry[] = [];
    const earlier: HistoryEntry[] = [];

    const now = Date.now();
    const oneDay = 24 * 60 * 60 * 1000;
    const sevenDays = 7 * oneDay;

    // Sort descending
    const sortedHistory = [...history].sort((a, b) => b.lastReadTimestamp - a.lastReadTimestamp);

    sortedHistory.forEach((entry) => {
      const diff = now - entry.lastReadTimestamp;
      if (diff < oneDay) {
        today.push(entry);
      } else if (diff < 2 * oneDay) {
        yesterday.push(entry);
      } else if (diff < sevenDays) {
        thisWeek.push(entry);
      } else {
        earlier.push(entry);
      }
    });

    return { today, yesterday, thisWeek, earlier };
  }, [history]);

  // Read time helper
  const formatDuration = (ms: number) => {
    const mins = Math.floor(ms / 60000);
    if (mins < 1) return "under a minute";
    if (mins === 1) return "1 minute";
    return `${mins} minutes`;
  };

  const hasHistory = history.length > 0;

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      
      {/* Glow highlight */}
      <div className="absolute top-0 left-10 w-[300px] h-[300px] rounded-full bg-blue-500/5 blur-[100px] pointer-events-none" />

      <div className="max-w-4xl mx-auto px-6 space-y-8 relative z-10">
        
        {/* Header navigation bar */}
        <div className="flex justify-between items-end">
          <div className="space-y-1">
            <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">Activity Timeline</span>
            <h1 className="text-3xl font-sans font-semibold tracking-tight">Reading History</h1>
          </div>

          {hasHistory && (
            <button
              id="btn-clear-history"
              onClick={onClearHistory}
              className="px-4 py-2 border border-white/5 bg-white/5 hover:border-rose-500/30 hover:bg-rose-950/20 hover:text-rose-400 text-xs font-mono rounded-xl cursor-pointer transition-all flex items-center gap-1.5"
            >
              <Trash className="w-3.5 h-3.5" />
              <span>CLEAR TIMELINE</span>
            </button>
          )}
        </div>

        {/* Timeline structure panels */}
        {hasHistory ? (
          <div className="relative pl-6 space-y-10 border-l border-white/10 ml-3">
            
            {/* Group: TODAY */}
            {groupedTimeline.today.length > 0 && (
              <TimelineSection 
                title="Today" 
                entries={groupedTimeline.today} 
                onResume={onResumeReading} 
                formatDuration={formatDuration} 
              />
            )}

            {/* Group: YESTERDAY */}
            {groupedTimeline.yesterday.length > 0 && (
              <TimelineSection 
                title="Yesterday" 
                entries={groupedTimeline.yesterday} 
                onResume={onResumeReading} 
                formatDuration={formatDuration} 
              />
            )}

            {/* Group: THIS WEEK */}
            {groupedTimeline.thisWeek.length > 0 && (
              <TimelineSection 
                title="This Week" 
                entries={groupedTimeline.thisWeek} 
                onResume={onResumeReading} 
                formatDuration={formatDuration} 
              />
            )}

            {/* Group: EARLIER */}
            {groupedTimeline.earlier.length > 0 && (
              <TimelineSection 
                title="Earlier" 
                entries={groupedTimeline.earlier} 
                onResume={onResumeReading} 
                formatDuration={formatDuration} 
              />
            )}

          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-28 border border-white/5 bg-slate-900/10 rounded-3xl backdrop-blur-sm shadow-inner text-center space-y-4">
            <History className="w-10 h-10 text-slate-700 animate-pulse" />
            <div>
              <h3 className="text-lg font-semibold text-slate-400">Timeline database remains clear</h3>
              <p className="text-xs text-slate-500 max-w-sm mt-1">When you load a manga, light novel, or hybrid element and progress through its pages, your timeline reads will persist here securely.</p>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}

// Inner timeline visual node component
interface TimelineSectionProps {
  title: string;
  entries: HistoryEntry[];
  onResume: (mediaId: string, chapterId: string) => void;
  formatDuration: (ms: number) => string;
}

function TimelineSection({ title, entries, onResume, formatDuration }: TimelineSectionProps) {
  return (
    <div className="space-y-4 relative">
      
      {/* Pulsing indicator node on the glass timeline rail */}
      <div className="absolute -left-[31px] top-1.5 w-4 h-4 rounded-full bg-[#0A0C0F] border-2 border-[#7BC6FF] shadow-[0_0_8px_rgba(123,198,255,0.8)] z-10" />

      <h3 className="text-xs uppercase font-mono tracking-[0.2em] font-bold text-slate-400 flex items-center gap-2">
        <CalendarDays className="w-3.5 h-3.5 text-[#7BC6FF]" />
        {title}
      </h3>

      <div className="space-y-4">
        {entries.map((entry) => (
          <div
            id={`history-row-${entry.id}`}
            key={entry.id}
            onClick={() => onResume(entry.mediaId, entry.chapterId)}
            className="group flex flex-col md:flex-row gap-4 p-4 rounded-2xl border border-white/5 bg-slate-900/20 hover:border-[#7BC6FF]/20 hover:bg-slate-900/40 backdrop-blur-md transition-all duration-300 cursor-pointer shadow-md"
            style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
          >
            <div className="w-16 h-20 rounded-lg overflow-hidden flex-shrink-0 shadow-sm relative self-start">
              <img src={entry.mediaCover} alt={entry.mediaTitle} className="w-full h-full object-cover group-hover:scale-103 duration-300 transition-all" referrerPolicy="no-referrer" />
              <div className="absolute inset-0 bg-black/40" />
            </div>

            <div className="flex-1 flex flex-col justify-between min-w-0">
              <div className="space-y-1">
                <div className="flex justify-between items-start gap-3">
                  <h4 className="font-semibold text-sm text-white group-hover:text-[#7BC6FF] transition-colors truncate">
                    {entry.mediaTitle}
                  </h4>
                  <span className="text-[10px] uppercase font-mono px-2 py-0.5 bg-white/5 text-slate-400 rounded-md">
                    {entry.type}
                  </span>
                </div>
                <p className="text-xs text-slate-400">
                  Chapter {entry.chapterNumber}: {entry.chapterTitle}
                </p>
                <p className="text-[10px] text-slate-500 font-mono">
                  Spent {formatDuration(entry.readingDurationMs)} reading
                </p>
              </div>

              {/* Progress Slider */}
              <div className="space-y-2 mt-3 block">
                <div className="flex justify-between items-center text-[10px] text-slate-500 font-mono">
                  <span>Reading index: {entry.progressPercent}%</span>
                  <span className="text-[#7BC6FF] flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                    Instant Resume <Play className="w-2.5 h-2.5 fill-current" />
                  </span>
                </div>
                <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-[#7BC6FF] to-indigo-500 rounded-full"
                    style={{ width: `${entry.progressPercent}%` }}
                  />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
