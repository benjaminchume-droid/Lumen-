import { useState, useEffect, useMemo } from "react";
import { 
  X, Heart, Sparkles, BookOpen, Star, 
  Trash2, Play, ChevronLeft, Calendar, UserCheck 
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

import { MediaItem, Chapter, HistoryEntry, UpdateRecord, ReaderSettings } from "./types";

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
  
  const [onboarded, setOnboarded] = useState<boolean>(false);
  const [userSession, setUserSession] = useState<{ name: string, email: string, isGuest: boolean, userId?: string } | null>(null);
  const [genreInterests, setGenreInterests] = useState<string[]>([]);
  
  const [activeSourceIds, setActiveSourceIds] = useState<string[]>([]);
  const [showSourceManager, setShowSourceManager] = useState<boolean>(false);

  const [library, setLibrary] = useState<MediaItem[]>([]);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [updates, setUpdates] = useState<UpdateRecord[]>([]);
  const [favorites, setFavorites] = useState<string[]>([]);
  const [settings, setSettings] = useState<ReaderSettings>(INITIAL_SETTINGS);

  const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null);
  const [activeReadingItem, setActiveReadingItem] = useState<MediaItem | null>(null);
  const [activeReadingChapter, setActiveReadingChapter] = useState<Chapter | null>(null);
  const [showForge, setShowForge] = useState(false);

  useEffect(() => {
    try {
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
        localStorage.setItem("lumen_library_v2", JSON.stringify([]));
        setLibrary([]);
      }

      const persistedHistory = localStorage.getItem("lumen_history");
      if (persistedHistory) setHistory(JSON.parse(persistedHistory));

      const persistedUpdates = localStorage.getItem("lumen_updates");
      if (persistedUpdates) {
        setUpdates(JSON.parse(persistedUpdates));
      } else {
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

  const syncLibrary = (newLibrary: MediaItem[]) => {
    setLibrary(newLibrary);
    localStorage.setItem("lumen_library_v2", JSON.stringify(newLibrary));
  };

  const handleToggleFavorite = (mediaId: string) => {
    const updated = favorites.includes(mediaId)
      ? favorites.filter((id) => id !== mediaId)
      : [...favorites, mediaId];
    setFavorites(updated);
    localStorage.setItem("lumen_favorites", JSON.stringify(updated));
  };

  const handleDeleteMedia = (mediaId: string) => {
    const updated = library.filter((item) => item.id !== mediaId);
    syncLibrary(updated);
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

  const handleSettleForgedBook = (series: MediaItem) => {
    const exists = library.some((item) => item.id === series.id);
    if (!exists) {
      const updated = [series, ...library];
      syncLibrary(updated);
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

    const activeUpdate = updates.find((u) => u.mediaId === mediaId && u.chapterId === chapterId);
    if (activeUpdate && !activeUpdate.isRead) {
      handleMarkAsRead(activeUpdate.id);
    }
  };

  const handleResumeReading = (mediaId: string, chapterId: string) => {
    const targetItem = library.find((c) => c.id === mediaId);
    const targetChapter = targetItem?.chapters.find((ch) => ch.id === chapterId);
    if (targetItem && targetChapter) {
      setActiveReadingItem(targetItem);
      setActiveReadingChapter(targetChapter);
      setSelectedItem(null);
    }
  };

  const handleClearHistory = () => {
    setHistory([]);
    localStorage.removeItem("lumen_history");
  };

  const activeUpdatesCount = updates.filter(u => !u.isRead).length;

  useEffect(() => {
    const root = document.documentElement;
    if (settings.themeMode === 'light') {
      root.classList.add('light');
    } else {
      root.classList.remove('light');
    }
  }, [settings.themeMode]);

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
      style={{ fontFamily: 'Inter, ui-sans-serif, system-ui, sans-serif' }}
    >
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
              userId={userSession?.userId}
              isGuest={userSession?.isGuest ?? true}
              onOpenSourceManager={() => setShowSourceManager(true)}
              activeSourcesCount={activeSourceIds.length}
            />
          </div>
        </div>
      )}

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
              onClick={(e) => e.stopPropagation()}
            >
              <button
                onClick={() => setSelectedItem(null)}
                className="absolute top-5 right-5 p-1.5 border border-white/5 bg-white/5 hover:bg-white/10 rounded-full cursor-pointer text-slate-400 hover:text-white z-10"
              >
                <X className="w-4 h-4" />
              </button>

              <div className="flex flex-col sm:flex-row gap-6 items-start relative z-10 pt-4">
                <div className="w-28 sm:w-36 aspect-[3/4] rounded-2xl overflow-hidden shadow-lg border border-white/5 flex-shrink-0 bg-slate-900">
                  <img src={selectedItem.coverUrl} alt={selectedItem.title} className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                </div>
                <div className="space-y-3 flex-1 min-w-0">
                  <span className="px-2.5 py-0.5 bg-[#7BC6FF]/10 text-[#7BC6FF] border border-[#7BC6FF]/25 rounded text-[10px] uppercase tracking-wider font-semibold font-mono">
                    {selectedItem.type}
                  </span>
                  <h2 className="text-2xl font-bold tracking-tight text-white leading-tight">{selectedItem.title}</h2>
                  <p className="text-sm text-slate-300 font-semibold">By {selectedItem.author}</p>
                  <p className="text-sm text-slate-400 leading-relaxed line-clamp-4">{selectedItem.description}</p>
                </div>
              </div>

              <div className="space-y-3 relative z-10">
                <h3 className="text-xs uppercase font-mono tracking-[0.2em] text-slate-400 font-semibold">Chapters</h3>
                <div className="max-h-64 overflow-y-auto space-y-2 pr-1">
                  {selectedItem.chapters.map((ch) => (
                    <button
                      key={ch.id}
                      onClick={() => {
                        setActiveReadingItem(selectedItem);
                        setActiveReadingChapter(ch);
                        setSelectedItem(null);
                      }}
                      className="w-full flex items-center justify-between gap-4 p-3 rounded-xl border border-white/5 bg-white/5 hover:bg-white/10 text-left"
                    >
                      <span className="text-sm text-white">{ch.title}</span>
                      <Play className="w-4 h-4 text-[#7BC6FF]" />
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-3 relative z-10">
                <button
                  onClick={() => handleToggleFavorite(selectedItem.id)}
                  className="flex-1 py-3 rounded-xl border border-white/10 bg-white/5 text-sm flex items-center justify-center gap-2"
                >
                  <Heart className={`w-4 h-4 ${favorites.includes(selectedItem.id) ? 'fill-rose-400 text-rose-400' : 'text-slate-400'}`} />
                  Favorite
                </button>
                <button
                  onClick={() => {
                    if (selectedItem.chapters[0]) {
                      setActiveReadingItem(selectedItem);
                      setActiveReadingChapter(selectedItem.chapters[0]);
                      setSelectedItem(null);
                    }
                  }}
                  className="flex-[2] py-3 rounded-xl bg-[#7BC6FF] text-slate-950 font-bold text-sm flex items-center justify-center gap-2"
                >
                  <BookOpen className="w-4 h-4" /> Read
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {activeReadingItem && activeReadingChapter && (
        <ReaderEngine
          media={activeReadingItem}
          chapter={activeReadingChapter}
          settings={settings}
          onClose={() => {
            setActiveReadingItem(null);
            setActiveReadingChapter(null);
          }}
          onProgress={(pct, ms) =>
            handleSaveHistoryPercent(activeReadingItem.id, activeReadingChapter.id, pct, ms)
          }
          onChangeSettings={(s) => {
            const up = { ...settings, ...s };
            setSettings(up);
            localStorage.setItem("lumen_settings", JSON.stringify(up));
          }}
        />
      )}

      {showForge && (
        <LumenForge
          onClose={() => setShowForge(false)}
          onSettle={handleSettleForgedBook}
        />
      )}

      {showDock && (
        <GlassDock
          activeTab={activeTab}
          onChangeTab={setActiveTab}
          updatesBadge={activeUpdatesCount}
        />
      )}
    </div>
  );
}
