import { useState, useEffect, useMemo } from "react";
import { 
  X, Heart, Sparkles, BookOpen, Star, 
  Trash2, Play, ChevronLeft, Calendar, UserCheck 
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

import { MediaItem, Chapter, HistoryEntry, UpdateRecord, ReaderSettings } from "./types";

// Import modular screens & widgets
import GlassDock from "./components/GlassDock";
import HomeView from "./components/HomeView";
import LibraryView from "./components/LibraryView";
import HistoryView from "./components/HistoryView";
import UpdatesView from "./components/UpdatesView";
import MoreView from "./components/MoreView";
import ReaderEngine from "./components/ReaderEngine";
import LumenForge from "./components/LumenForge";
import Gateway from "./components/Gateway";
import SourceManagerView from "./components/SourceManagerView";

const INITIAL_SETTINGS: ReaderSettings = {
  motionReduction: false,
  themeMode: 'dark',
  liquidIntensity: 18,
  fontSize: 105,
  lineHeight: 1.65,
  fontFamily: 'serif',
  columnWidth: 'medium',
  warmMode: false,
  mangaMode: 'vertical-scroll',
  panelZoomEnabled: true,
  adaptiveAmbientBlur: true,
};

export default function App() {
  const [activeTab, setActiveTab] = useState<'home' | 'library' | 'history' | 'updates' | 'more'>('home');
  
  // Onboarding & Gateway Session states
  const [onboarded, setOnboarded] = useState<boolean>(false);
  const [userSession, setUserSession] = useState<{ name: string, email: string, isGuest: boolean } | null>(null);
  const [genreInterests, setGenreInterests] = useState<string[]>([]);
  
  // Extension Ingestion states — start empty (no simulated sources)
  const [activeSourceIds, setActiveSourceIds] = useState<string[]>([]);
  const [showSourceManager, setShowSourceManager] = useState<boolean>(false);

  // DB Local persist States — production empty start
  const [library, setLibrary] = useState<MediaItem[]>([]);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [updates, setUpdates] = useState<UpdateRecord[]>([]);
  const [favorites, setFavorites] = useState<string[]>([]);
  const [settings, setSettings] = useState<ReaderSettings>(INITIAL_SETTINGS);

  // Focus View states
  const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null);
  const [activeReadingItem, setActiveReadingItem] = useState<MediaItem | null>(null);
  const [activeReadingChapter, setActiveReadingChapter] = useState<Chapter | null>(null);
  const [showForge, setShowForge] = useState(false);

  // Load state from local persistence on boot
  useEffect(() => {
    try {
      // Load gateway auth criteria
      const persistedOnboarded = localStorage.getItem("lumen_onboarded") === "true";
      const persistedSession = localStorage.getItem("lumen_user_session");
      const persistedInterests = localStorage.getItem("lumen_genre_interests");
      
      setOnboarded(persistedOnboarded);
      if (persistedSession) setUserSession(JSON.parse(persistedSession));
      if (persistedInterests) setGenreInterests(JSON.parse(persistedInterests));

      const persistedSourceIds = localStorage.getItem("lumen_active_sources");
      if (persistedSourceIds) setActiveSourceIds(JSON.parse(persistedSourceIds));

      const persistedLibrary = localStorage.getItem("lumen_library_v2");
      if (persistedLibrary) {
        setLibrary(JSON.parse(persistedLibrary));
      } else {
        // Production: start empty. No demo catalog seeding.
        localStorage.setItem("lumen_library_v2", JSON.stringify([]));
        setLibrary([]);
      }

      const persistedHistory = localStorage.getItem("lumen_history");
      if (persistedHistory) setHistory(JSON.parse(persistedHistory));

      const persistedUpdates = localStorage.getItem("lumen_updates");
      if (persistedUpdates) {
        setUpdates(JSON.parse(persistedUpdates));
      } else {
        // Production: no fake update feed
        localStorage.setItem("lumen_updates", JSON.stringify([]));
        setUpdates([]);
      }

      const persistedFavorites = localStorage.getItem("lumen_favorites");
      if (persistedFavorites) setFavorites(JSON.parse(persistedFavorites));

      const persistedSettings = localStorage.getItem("lumen_settings");
      if (persistedSettings) setSettings(JSON.parse(persistedSettings));
    } catch (e) {
      console.error("Local Database recovery failed on boot:", e);
    }
  }, []);

  // Sync Library values
  const syncLibrary = (newLibrary: MediaItem[]) => {
    setLibrary(newLibrary);
    localStorage.setItem("lumen_library_v2", JSON.stringify(newLibrary));
  };

  // Safe toggler favorites
  const handleToggleFavorite = (mediaId: string) => {
    const updated = favorites.includes(mediaId)
      ? favorites.filter((id) => id !== mediaId)
      : [...favorites, mediaId];
    setFavorites(updated);
    localStorage.setItem("lumen_favorites", JSON.stringify(updated));
  };

  // Safe delete library elements (including custom / forged materials)
  const handleDeleteMedia = (mediaId: string) => {
    const updated = library.filter((item) => item.id !== mediaId);
    syncLibrary(updated);
    
    // Clear matches from history and favorites
    const updatedHistory = history.filter((h) => h.mediaId !== mediaId);
    setHistory(updatedHistory);
    localStorage.setItem("lumen_history", JSON.stringify(updatedHistory));

    if (favorites.includes(mediaId)) {
      setFavorites(favorites.filter(id => id !== mediaId));
    }
    
    if (selectedItem?.id === mediaId) {
      setSelectedItem(null);
    }
  };

  // Add a newly forged AI book into shelves
  const handleSettleForgedBook = (series: MediaItem) => {
    const exists = library.some((item) => item.id === series.id);
    if (!exists) {
      const updated = [series, ...library];
      syncLibrary(updated);
      
      // Inject alert notification update records out of high craftsmanship details!
      const newUpdate: UpdateRecord = {
        id: `upd-${series.id}-1`,
        mediaId: series.id,
        mediaTitle: series.title,
        mediaCover: series.coverUrl,
        type: series.type,
        chapterId: series.chapters[0]?.id || "chapter-1",
        chapterTitle: series.chapters[0]?.title || "Chapter 1",
        chapterNumber: 1,
        updatedAt: "Just now forged",
        isRead: false
      };
      const updatedUpdates = [newUpdate, ...updates];
      setUpdates(updatedUpdates);
      localStorage.setItem("lumen_updates", JSON.stringify(updatedUpdates));
    }
    setShowForge(false);
  };

  // Updates screen handlers
  const handleMarkAsRead = (updateId: string) => {
    const updated = updates.map((u) => u.id === updateId ? { ...u, isRead: true } : u);
    setUpdates(updated);
    localStorage.setItem("lumen_updates", JSON.stringify(updated));
  };

  const handleMarkAllAsRead = () => {
    const updated = updates.map((u) => ({ ...u, isRead: true }));
    setUpdates(updated);
    localStorage.setItem("lumen_updates", JSON.stringify(updated));
  };

  // Core History tracker and sync mechanism
  const handleSaveHistoryPercent = (mediaId: string, chapterId: string, progressPercent: number, elapsedMs: number) => {
    const targetItem = library.find((c) => c.id === mediaId);
    const targetChapter = targetItem?.chapters.find((ch) => ch.id === chapterId);

    if (!targetItem || !targetChapter) return;

    const newHistoryEntry: HistoryEntry = {
      id: `hist-${mediaId}-${chapterId}`,
      mediaId,
      mediaTitle: targetItem.title,
      mediaCover: targetItem.coverUrl,
      type: targetItem.type,
      chapterId,
      chapterTitle: targetChapter.title,
      chapterNumber: targetChapter.number,
      progressPercent,
      lastReadTimestamp: Date.now(),
      readingDurationMs: elapsedMs
    };

    const index = history.findIndex((h) => h.mediaId === mediaId && h.chapterId === chapterId);
    let updated: HistoryEntry[] = [];

    if (index >= 0) {
      updated = [...history];
      updated[index] = {
        ...updated[index],
        progressPercent,
        lastReadTimestamp: Date.now(),
        readingDurationMs: updated[index].readingDurationMs + elapsedMs
      };
    } else {
      updated = [newHistoryEntry, ...history];
    }

    setHistory(updated);
    localStorage.setItem("lumen_history", JSON.stringify(updated));

    // Automatically mark matching updates as read
    const activeUpdate = updates.find((u) => u.mediaId === mediaId && u.chapterId === chapterId);
    if (activeUpdate && !activeUpdate.isRead) {
      handleMarkAsRead(activeUpdate.id);
    }
  };

  // Quick resume click on feed update or timeline item
  const handleResumeReading = (mediaId: string, chapterId: string) => {
    const targetItem = library.find((c) => c.id === mediaId);
    const targetChapter = targetItem?.chapters.find((ch) => ch.id === chapterId);
    
    if (targetItem && targetChapter) {
      setActiveReadingItem(targetItem);
      setActiveReadingChapter(targetChapter);
      setSelectedItem(null); // Clear item overlay
    }
  };

  const handleClearHistory = () => {
    setHistory([]);
    localStorage.removeItem("lumen_history");
  };

  const activeUpdatesCount = updates.filter(u => !u.isRead).length;

  // Multi-theme custom body visual triggers
  useEffect(() => {
    const root = document.documentElement;
    if (settings.themeMode === 'light') {
      root.classList.add('light');
    } else {
      root.classList.remove('light');
    }
  }, [settings.themeMode]);

  // Tailor catalog by chosen interests only (no forced demo items)
  const tailoredHomeCatalog = useMemo(() => {
    if (genreInterests.length === 0) return library;
    return library.filter((item) =>
      item.genres.some((g) => genreInterests.includes(g))
    );
  }, [library, genreInterests]);

  if (!onboarded || !userSession) {
    return (
      <Gateway 
        onComplete={(session, genres) => {
          localStorage.setItem("lumen_onboarded", "true");
          localStorage.setItem("lumen_user_session", JSON.stringify(session));
          localStorage.setItem("lumen_genre_interests", JSON.stringify(genres));
          
          setUserSession(session);
          setGenreInterests(genres);
          setOnboarded(true);
        }}
      />
    );
  }

  const showDock = !activeReadingItem && !selectedItem && !showForge && !showSourceManager;

  return (
    <div 
      className={`min-h-screen text-white select-none relative overflow-x-hidden pb-4 md:pb-0 ${
        settings.themeMode === 'light' ? 'bg-[#F5F7FA] text-slate-900' :
        settings.themeMode === 'amoled' ? 'bg-[#000000]' : 'bg-[#0A0C0F]'
      }`}
      style={{
        fontFamily: 'Inter, ui-sans-serif, system-ui, sans-serif'
      }}
    >
      
      {/* RENDER SCREENS IN PARALLEL AND TOGGLE VISIBILITY ONLY - PRESERVES EXACT SCROLL OFFSETS AND STATE INSIDE TABS */}
      {!showSourceManager && (
        <div className="relative">
          <div className={activeTab === 'home' ? 'block animate-fade-in' : 'hidden'}>
            <HomeView 
              catalog={tailoredHomeCatalog} 
              history={history} 
              onSelectItem={setSelectedItem}
              onResumeReading={handleResumeReading}
              onOpenForge={() => setShowForge(true)}
              userSession={userSession || undefined}
            />
          </div>

          <div className={activeTab === 'library' ? 'block animate-fade-in' : 'hidden'}>
            <LibraryView 
              library={library} 
              favorites={favorites}
              onToggleFavorite={handleToggleFavorite}
              onSelectItem={setSelectedItem}
              onDeleteItem={handleDeleteMedia}
            />
          </div>

          <div className={activeTab === 'history' ? 'block animate-fade-in' : 'hidden'}>
            <HistoryView 
              history={history} 
              onResumeReading={handleResumeReading}
              onClearHistory={handleClearHistory}
            />
          </div>

          <div className={activeTab === 'updates' ? 'block animate-fade-in' : 'hidden'}>
            <UpdatesView 
              updates={updates} 
              catalog={library}
              onMarkAsRead={handleMarkAsRead}
              onMarkAllAsRead={handleMarkAllAsRead}
              onSelectItem={setSelectedItem}
              onResumeReading={handleResumeReading}
            />
          </div>

          <div className={activeTab === 'more' ? 'block animate-fade-in' : 'hidden'}>
            <MoreView 
              settings={settings}
              onChangeSettings={(newS) => {
                const up = { ...settings, ...newS };
                setSettings(up);
                localStorage.setItem("lumen_settings", JSON.stringify(up));
              }}
              userEmail={userSession?.email}
              onOpenSourceManager={() => setShowSourceManager(true)}
              activeSourcesCount={activeSourceIds.length}
            />
          </div>
        </div>
      )}

      {/* CONDITIONAL INGRESTION MANAGER BOARD */}
      {showSourceManager && (
        <SourceManagerView 
          activeSourceIds={activeSourceIds}
          onToggleSource={(id) => {
            const next = activeSourceIds.includes(id)
              ? activeSourceIds.filter(x => x !== id)
              : [...activeSourceIds, id];
            setActiveSourceIds(next);
            localStorage.setItem("lumen_active_sources", JSON.stringify(next));
          }}
          onClose={() => setShowSourceManager(false)}
        />
      )}

      {/* OVERVIEW PRE-READING INFO SHEET DRAWER */}
      <AnimatePresence>
        {selectedItem && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/85 backdrop-blur-md z-40 flex items-center justify-center p-4"
            onClick={() => setSelectedItem(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 30 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 30 }}
              className="w-full max-w-2xl bg-slate-950/95 border border-white/10 rounded-3xl overflow-hidden p-6 md:p-8 space-y-6 shadow-2xl relative"
              style={{
                boxShadow: '0 24px 64px 0 rgba(0,0,0,0.65), inset 0 1px 1.5px 0 rgba(255,255,255,0.1)'
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Close handles */}
              <button
                id="btn-close-overview-sheet"
                onClick={() => setSelectedItem(null)}
                className="absolute top-5 right-5 p-1.5 border border-white/5 bg-white/5 hover:bg-white/10 rounded-full cursor-pointer text-slate-400 hover:text-white transition-all z-10"
              >
                <X className="w-4 h-4" />
              </button>

              {/* Background cover art blurring effect */}
              <div 
                className="absolute -top-16 inset-x-0 h-44 blur-2xl opacity-20 pointer-events-none"
                style={{
                  backgroundImage: `url(${selectedItem.coverUrl})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center'
                }}
              />

              <div className="flex flex-col sm:flex-row gap-6 items-start relative z-10 pt-4">
                {/* Large high-craft Cover frame */}
                <div className="w-28 sm:w-36 aspect-[3/4] rounded-2xl overflow-hidden shadow-lg border border-white/5 flex-shrink-0 bg-slate-900">
                  <img src={selectedItem.coverUrl} alt={selectedItem.title} className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                </div>

                {/* Meta details labels */}
                <div className="space-y-3 flex-1 min-w-0">
                  <div className="flex flex-wrap gap-2 items-center">
                    <span className="px-2.5 py-0.5 bg-[#7BC6FF]/10 text-[#7BC6FF] border border-[#7BC6FF]/25 rounded text-[10px] uppercase tracking-wider font-semibold font-mono">
                      {selectedItem.type}
                    </span>
                    {selectedItem.isCustom && (
                      <span className="px-2 py-0.5 bg-purple-500/10 text-purple-400 border border-purple-500/25 rounded text-[10px] uppercase tracking-wider font-semibold font-mono">
                        GENAI FORGE Original
                      </span>
                    )}
                  </div>
                  
                  <div className="space-y-0.5">
                    <h2 className="text-2xl font-bold tracking-tight text-white leading-tight">{selectedItem.title}</h2>
                    {selectedItem.originalTitle && (
                      <p className="text-xs text-slate-500 font-mono italic">{selectedItem.originalTitle}</p>
                    )}
                  </div>

                  <p className="text-sm text-slate-300 font-semibold font-sans">
                    By {selectedItem.author} {selectedItem.artist && ` • Artist ${selectedItem.artist}`}
                  </p>

                  <div className="flex gap-4 items-center text-xs text-slate-400 font-mono">
                    <span className="text-[#7BC6FF] flex items-center gap-1 font-bold">
                      <Star className="w-3.5 h-3.5 fill-current" /> {selectedItem.rating} Rating
                    </span>
                    <span>•</span>
                    <span>{selectedItem.chaptersCount} Chapters</span>
                  </div>

                  <p className="text-sm text-slate-400 leading-relaxed line-clamp-4">
                    {selectedItem.description}
                  </p>

                  <div className="flex flex-wrap gap-2 pt-1">
                    {selectedItem.genres.map((g) => (
                      <span key={g} className="px-2 py-0.5 bg-white/5 text-slate-400 text-[10px] rounded-md font-mono">{g}</span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Chapter list */}
              <div className="space-y-3 relative z-10">
                <h3 className="text-xs uppercase font-mono tracking-[0.2em] text-slate-400 font-semibold">Chapters</h3>
                <div className="max-h-64 overflow-y-auto space-y-2 pr-1">
                  {selectedItem.chapters.map((ch) => {
                    const readEntry = history.find(h => h.mediaId === selectedItem.id && h.chapterId === ch.id);
                    return (
                      <button
                        key={ch.id}
                        onClick={() => {
                          setActiveReadingItem(selectedItem);
                          setActiveReadingChapter(ch);
                          setSelectedItem(null);
                        }}
                        className="w-full flex items-center justify-between gap-4 p-3 rounded-xl border border-white/5 bg-white/5 hover:bg-white/10 hover:border-[#7BC6FF]/30 transition-all cursor-pointer text-left"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <span className="text-xs font-mono text-[#7BC6FF] font-bold w-6">{ch.number}</span>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-white truncate">{ch.title}</p>
                            {readEntry ? (
                              <span className="text-[8px] font-mono text-slate-500 font-semibold">{readEntry.progressPercent}% progress</span>
                            ) : null}
                          </div>
                        </div>
                        <span className="text-[10px] font-mono text-slate-500 uppercase shrink-0">{ch.uploadedAt}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* IMMERSIVE GENESIS WORKSPACE FORGE DRAWER */}
      {showForge && (
        <LumenForge 
          onSettleBook={handleSettleForgedBook}
          onClose={() => setShowForge(false)}
          onLaunchReader={(item) => {
            if (item.chapters && item.chapters.length > 0) {
              setActiveReadingItem(item);
              setActiveReadingChapter(item.chapters[0]);
            }
          }}
        />
      )}

      {/* MASTER IMMERSIVE ACTIVE READER ENGINE VIEW */}
      {activeReadingItem && activeReadingChapter && (
        <ReaderEngine 
          item={activeReadingItem}
          chapter={activeReadingChapter}
          settings={settings}
          onChangeSettings={(newSettings) => {
            const up = { ...settings, ...newSettings };
            setSettings(up);
            localStorage.setItem("lumen_settings", JSON.stringify(up));
          }}
          onClose={() => {
            setActiveReadingItem(null);
            setActiveReadingChapter(null);
          }}
          onSaveHistoryPercent={handleSaveHistoryPercent}
        />
      )}

      {/* FLOATING NAVIGATION CONTROL DOCK */}
      <AnimatePresence>
        {showDock && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.95 }}
            transition={{ type: "spring", stiffness: 350, damping: 28 }}
          >
            <GlassDock 
              activeTab={activeTab} 
              setActiveTab={setActiveTab} 
              unreadUpdatesCount={activeUpdatesCount}
            />
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
