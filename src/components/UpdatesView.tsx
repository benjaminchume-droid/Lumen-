import { Bell, Check, Eye, Trash2, Sliders } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { UpdateRecord, MediaItem } from "../types";

interface UpdatesViewProps {
  updates: UpdateRecord[];
  catalog: MediaItem[];
  onMarkAsRead: (updateId: string) => void;
  onMarkAllAsRead: () => void;
  onSelectItem: (item: MediaItem) => void;
  onResumeReading: (mediaId: string, chapterId: string) => void;
}

export default function UpdatesView({ updates, catalog, onMarkAsRead, onMarkAllAsRead, onSelectItem, onResumeReading }: UpdatesViewProps) {
  
  const unreadCount = updates.filter((u) => !u.isRead).length;

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      
      {/* Background illumination aura */}
      <div className="absolute top-0 right-10 w-[350px] h-[350px] rounded-full bg-cyan-500/5 blur-[120px] pointer-events-none" />

      <div className="max-w-3xl mx-auto px-6 space-y-8 relative z-10">
        
        {/* Header segment */}
        <div className="flex justify-between items-end">
          <div className="space-y-1">
            <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">Feed Alerts</span>
            <h1 className="text-3xl font-sans font-semibold tracking-tight flex items-center gap-2">
              Recent Updates
              {unreadCount > 0 && (
                <span className="text-xs font-mono bg-[#7BC6FF]/20 text-[#7BC6FF] px-2.5 py-1 rounded-full border border-[#7BC6FF]/30 tracking-tight">
                  {unreadCount} unread
                </span>
              )}
            </h1>
          </div>

          {unreadCount > 0 && (
            <button
              id="btn-mark-all-read"
              onClick={onMarkAllAsRead}
              className="px-4 py-2 border border-white/5 bg-white/5 hover:border-[#7BC6FF]/30 hover:bg-[#7BC6FF]/10 text-xs text-[#7BC6FF] font-mono rounded-xl cursor-pointer transition-all flex items-center gap-1.5"
            >
              <Check className="w-4 h-4" />
              <span>MARK ALL READ</span>
            </button>
          )}
        </div>

        {/* FEED BOX LISTS */}
        {updates.length > 0 ? (
          <div className="space-y-4">
            <AnimatePresence mode="popLayout">
              {updates.map((update) => {
                const targetMedia = catalog.find((c) => c.id === update.mediaId);
                
                return (
                  <motion.div
                    layout
                    id={`update-row-${update.id}`}
                    key={update.id}
                    initial={{ opacity: 0, y: 15 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    transition={{ type: "spring", stiffness: 400, damping: 30 }}
                    className={`relative p-4 rounded-3xl border transition-all duration-300 flex gap-4 ${
                      update.isRead 
                        ? 'bg-slate-900/10 border-white/5 opacity-75 hover:bg-slate-900/20' 
                        : 'bg-slate-900/40 border-white/10 hover:border-[#7BC6FF]/20 shadow-[0_4px_16px_rgba(0,0,0,0.15)]'
                    }`}
                  >
                    
                    {/* Glow unread dot indicator in page margin */}
                    {!update.isRead && (
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-[#7BC6FF] shadow-[0_0_8px_2px_rgba(123,198,255,0.7)]" />
                    )}

                    {/* Book Cover thumbnail */}
                    <div 
                      onClick={() => targetMedia && onSelectItem(targetMedia)}
                      className="w-14 h-18 rounded-lg overflow-hidden flex-shrink-0 shadow cursor-pointer ml-3 relative group"
                    >
                      <img src={update.mediaCover} alt={update.mediaTitle} className="w-full h-full object-cover group-hover:scale-105 duration-300 transition-all" referrerPolicy="no-referrer" />
                      <div className="absolute inset-0 bg-black/25" />
                    </div>

                    {/* Meta labels content */}
                    <div className="flex-1 min-w-0 pr-4 flex flex-col justify-between py-1">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <h4 
                            onClick={() => targetMedia && onSelectItem(targetMedia)}
                            className="font-semibold text-sm text-neutral-200 hover:text-[#7BC6FF] cursor-pointer transition-colors truncate"
                          >
                            {update.mediaTitle}
                          </h4>
                        </div>
                        <p className="text-xs text-slate-400 mt-0.5 font-sans font-medium">
                          New release: Chapter {update.chapterNumber} — <span className="italic">{update.chapterTitle}</span>
                        </p>
                      </div>

                      <div className="flex justify-between items-center text-[10px] text-slate-500 font-mono mt-2">
                        <span>Released {update.updatedAt}</span>
                        <span className="uppercase tracking-wider font-semibold text-slate-400">{update.type}</span>
                      </div>
                    </div>

                    {/* Quick interactive Actions */}
                    <div className="flex flex-col justify-center items-end gap-2 pr-1">
                      {!update.isRead && (
                        <button
                          id={`update-row-mark-${update.id}`}
                          onClick={() => onMarkAsRead(update.id)}
                          className="p-2 border border-white/5 bg-slate-950/40 text-slate-400 hover:text-[#7BC6FF] hover:border-[#7BC6FF]/25 rounded-2xl cursor-pointer transition-all flex items-center gap-1 text-[10px] font-mono"
                          title="Mark as Read"
                        >
                          <Check className="w-3.5 h-3.5" />
                        </button>
                      )}

                      <button
                        id={`update-row-read-${update.id}`}
                        onClick={() => onResumeReading(update.mediaId, update.chapterId)}
                        className="p-2 border border-white/5 bg-slate-950/40 text-[#7BC6FF] hover:bg-[#7BC6FF]/10 hover:border-[#7BC6FF]/30 hover:text-white rounded-2xl cursor-pointer transition-all flex items-center gap-1 text-[10px] font-mono"
                        title="Read Now"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span className="hidden md:inline pr-1">Read</span>
                      </button>
                    </div>

                  </motion.div>
                );
              })}
            </AnimatePresence>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-28 border border-white/5 bg-slate-900/10 rounded-3xl backdrop-blur-sm text-center space-y-4 shadow-inner">
            <Bell className="w-10 h-10 text-slate-700 animate-wiggle" />
            <div>
              <h3 className="text-lg font-semibold text-slate-400">Notification channels are silent</h3>
              <p className="text-xs text-slate-500 max-w-sm mt-1">When any active series in your local archives or synced feeds drops a new volume chapter, notifications will refresh immediately.</p>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
