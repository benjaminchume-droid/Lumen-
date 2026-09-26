import { useState, useEffect, useMemo } from "react";
import { X, Heart, BookOpen, Play } from "lucide-react";
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
import { localDb } from "./lib/local-db";

const INITIAL_SETTINGS: ReaderSettings = {
  motionReduction: false,
  themeMode: "dark",
  liquidIntensity: 18,
  fontSize: 105,
  lineHeight: 1.65,
  fontFamily: "serif",
  columnWidth: "medium",
  warmMode: false,
  mangaMode: "vertical-scroll",
  panelZoomEnabled: true,
  adaptiveAmbientBlur: true,
};

export default function App() {
  const [activeTab, setActiveTab] = useState<
    "home" | "library" | "history" | "updates" | "more"
  >("home");
  const [onboarded, setOnboarded] = useState(false);
  const [userSession, setUserSession] = useState<{
    name: string;
    email: string;
    isGuest: boolean;
    userId?: string;
  } | null>(null);
  const [genreInterests, setGenreInterests] = useState<string[]>([]);
  const [activeSourceIds, setActiveSourceIds] = useState<string[]>([]);
  const [showSourceManager, setShowSourceManager] = useState(false);
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
      setOnboarded(localStorage.getItem("lumen_onboarded") === "true");
      const s = localStorage.getItem("lumen_user_session");
      if (s) setUserSession(JSON.parse(s));
      const g = localStorage.getItem("lumen_genre_interests");
      if (g) setGenreInterests(JSON.parse(g));
      setActiveSourceIds(localDb.getInstalledSourceIds());
      setLibrary(localDb.getLibrary());
      setHistory(localDb.getHistory());
      const u = localStorage.getItem("lumen_updates");
      if (u) setUpdates(JSON.parse(u));
      setFavorites(localDb.getLikes());
      const st = localStorage.getItem("lumen_settings");
      if (st) setSettings(JSON.parse(st));
    } catch (e) {
      console.error(e);
    }
  }, []);

  const syncLibrary = (items: MediaItem[]) => {
    setLibrary(items);
    localDb.setLibrary(items);
  };

  const handleToggleFavorite = (mediaId: string) => {
    const next = localDb.toggleLike(mediaId);
    setFavorites(next);
  };

  const handleDeleteMedia = (mediaId: string) => {
    syncLibrary(library.filter((i) => i.id !== mediaId));
    const h = history.filter((x) => x.mediaId !== mediaId);
    setHistory(h);
    localDb.setHistory(h);
    if (selectedItem?.id === mediaId) setSelectedItem(null);
  };

  const handleSettleForgedBook = (series: MediaItem) => {
    if (!library.some((i) => i.id === series.id)) {
      syncLibrary([series, ...library]);
    }
    setShowForge(false);
  };

  const handleMarkAsRead = (updateId: string) => {
    const next = updates.map((u) => (u.id === updateId ? { ...u, isRead: true } : u));
    setUpdates(next);
    localStorage.setItem("lumen_updates", JSON.stringify(next));
  };

  const handleMarkAllAsRead = () => {
    const next = updates.map((u) => ({ ...u, isRead: true }));
    setUpdates(next);
    localStorage.setItem("lumen_updates", JSON.stringify(next));
  };

  const handleSaveHistoryPercent = (
    mediaId: string,
    chapterId: string,
    progressPercent: number,
    elapsedMs: number
  ) => {
    const targetItem = library.find((c) => c.id === mediaId);
    const targetChapter = targetItem?.chapters.find((ch) => ch.id === chapterId);
    if (!targetItem || !targetChapter) return;
    const entry: HistoryEntry = {
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
      readingDurationMs: elapsedMs,
    };
    const idx = history.findIndex((h) => h.mediaId === mediaId && h.chapterId === chapterId);
    let next: HistoryEntry[];
    if (idx >= 0) {
      next = [...history];
      next[idx] = {
        ...next[idx],
        progressPercent,
        lastReadTimestamp: Date.now(),
        readingDurationMs: next[idx].readingDurationMs + elapsedMs,
      };
    } else {
      next = [entry, ...history];
    }
    setHistory(next);
    localDb.setHistory(next);
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

  const activeUpdatesCount = updates.filter((u) => !u.isRead).length;

  useEffect(() => {
    document.documentElement.classList.toggle("light", settings.themeMode === "light");
  }, [settings.themeMode]);

  const tailoredHomeCatalog = useMemo(() => {
    if (genreInterests.length === 0) return library;
    return library.filter((item) => item.genres.some((g) => genreInterests.includes(g)));
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

  const showDock =
    !activeReadingItem && !selectedItem && !showForge && !showSourceManager;

  return (
    <div
      className={`min-h-screen text-white select-none relative overflow-x-hidden pb-4 ${
        settings.themeMode === "light"
          ? "bg-[#F5F7FA] text-slate-900"
          : settings.themeMode === "amoled"
          ? "bg-[#000000]"
          : "bg-[#0A0C0F]"
      }`}
    >
      {!showSourceManager && (
        <div className="relative">
          <div className={activeTab === "home" ? "block" : "hidden"}>
            <HomeView
              catalog={tailoredHomeCatalog}
              history={history}
              onSelectItem={setSelectedItem}
              onResumeReading={handleResumeReading}
              onOpenForge={() => setShowForge(true)}
              onOpenSources={() => setShowSourceManager(true)}
              userSession={userSession || undefined}
              liquidIntensity={settings.liquidIntensity}
            />
          </div>
          <div className={activeTab === "library" ? "block" : "hidden"}>
            <LibraryView
              library={library}
              favorites={favorites}
              onToggleFavorite={handleToggleFavorite}
              onSelectItem={setSelectedItem}
              onDeleteItem={handleDeleteMedia}
            />
          </div>
          <div className={activeTab === "history" ? "block" : "hidden"}>
            <HistoryView
              history={history}
              onResumeReading={handleResumeReading}
              onClearHistory={() => {
                setHistory([]);
                localDb.setHistory([]);
              }}
            />
          </div>
          <div className={activeTab === "updates" ? "block" : "hidden"}>
            <UpdatesView
              updates={updates}
              catalog={library}
              onMarkAsRead={handleMarkAsRead}
              onMarkAllAsRead={handleMarkAllAsRead}
              onSelectItem={setSelectedItem}
              onResumeReading={handleResumeReading}
            />
          </div>
          <div className={activeTab === "more" ? "block" : "hidden"}>
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
              ? activeSourceIds.filter((x) => x !== id)
              : [...activeSourceIds, id];
            setActiveSourceIds(next);
            localDb.setInstalledSourceIds(next);
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
            className="fixed inset-0 bg-black/80 backdrop-blur-md z-40 flex items-center justify-center p-4"
            onClick={() => setSelectedItem(null)}
          >
            <motion.div
              initial={{ scale: 0.96, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.96, y: 20 }}
              className="w-full max-w-2xl bg-slate-950/95 border border-white/10 rounded-3xl p-6 space-y-5 relative"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                onClick={() => setSelectedItem(null)}
                className="absolute top-4 right-4 p-1.5 rounded-full bg-white/5"
              >
                <X className="w-4 h-4 text-slate-400" />
              </button>
              <div className="flex gap-4">
                <div className="w-28 aspect-[3/4] rounded-2xl overflow-hidden bg-slate-900 shrink-0">
                  {selectedItem.coverUrl && (
                    <img
                      src={selectedItem.coverUrl}
                      alt=""
                      className="w-full h-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  )}
                </div>
                <div className="space-y-2 min-w-0">
                  <span className="text-[10px] uppercase tracking-wide text-[#7BC6FF]">
                    {selectedItem.type}
                  </span>
                  <h2 className="text-xl font-semibold text-white">{selectedItem.title}</h2>
                  <p className="text-sm text-slate-400">{selectedItem.author}</p>
                  <p className="text-sm text-slate-500 line-clamp-3">{selectedItem.description}</p>
                </div>
              </div>
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {selectedItem.chapters.map((ch) => (
                  <button
                    key={ch.id}
                    onClick={() => {
                      setActiveReadingItem(selectedItem);
                      setActiveReadingChapter(ch);
                      setSelectedItem(null);
                    }}
                    className="w-full flex justify-between items-center p-3 rounded-xl bg-white/5 text-left"
                  >
                    <span className="text-sm text-white">{ch.title}</span>
                    <Play className="w-4 h-4 text-[#7BC6FF]" />
                  </button>
                ))}
                {selectedItem.chapters.length === 0 && (
                  <p className="text-xs text-slate-500">No chapters loaded for this title yet.</p>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handleToggleFavorite(selectedItem.id)}
                  className="flex-1 py-3 rounded-xl border border-white/10 text-sm flex items-center justify-center gap-2"
                >
                  <Heart
                    className={`w-4 h-4 ${
                      favorites.includes(selectedItem.id)
                        ? "fill-rose-400 text-rose-400"
                        : "text-slate-400"
                    }`}
                  />
                  Like
                </button>
                <button
                  onClick={() => {
                    if (selectedItem.chapters[0]) {
                      setActiveReadingItem(selectedItem);
                      setActiveReadingChapter(selectedItem.chapters[0]);
                      setSelectedItem(null);
                    }
                  }}
                  className="flex-[2] py-3 rounded-xl bg-[#7BC6FF] text-slate-950 font-semibold text-sm flex items-center justify-center gap-2"
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
          item={activeReadingItem}
          chapter={activeReadingChapter}
          settings={settings}
          onClose={() => {
            setActiveReadingItem(null);
            setActiveReadingChapter(null);
          }}
          onSaveHistoryPercent={handleSaveHistoryPercent}
          onChangeSettings={(s) => {
            const up = { ...settings, ...s };
            setSettings(up);
            localStorage.setItem("lumen_settings", JSON.stringify(up));
          }}
        />
      )}

      {showForge && (
        <LumenForge onClose={() => setShowForge(false)} onSettle={handleSettleForgedBook} />
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
