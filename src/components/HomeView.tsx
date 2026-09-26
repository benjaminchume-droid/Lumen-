import { useState, useMemo, useEffect } from "react";
import { Search, Clock, BookOpen, Star, Heart, FolderOpen, Download } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MediaItem, HistoryEntry } from "../types";
import LiquidGlass, { LiquidGlassBar } from "./LiquidGlass";
import { localDb } from "../lib/local-db";
import { fetchRecommended } from "../lib/rdb";

interface HomeViewProps {
  catalog: MediaItem[];
  history: HistoryEntry[];
  onSelectItem: (item: MediaItem) => void;
  onResumeReading: (mediaId: string, chapterId: string) => void;
  onOpenForge?: () => void;
  onOpenSources?: () => void;
  userSession?: { name: string; email: string; isGuest: boolean };
  liquidIntensity?: number;
}

export default function HomeView({
  catalog,
  history,
  onSelectItem,
  onResumeReading,
  onOpenSources,
  userSession,
  liquidIntensity = 18,
}: HomeViewProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [likes, setLikes] = useState<string[]>(() => localDb.getLikes());
  const [collections, setCollections] = useState(() => localDb.getCollections());
  const [recommended, setRecommended] = useState<MediaItem[]>([]);

  useEffect(() => {
    fetchRecommended(20).then(setRecommended).catch(() => {});
  }, []);

  const greeting = useMemo(() => {
    const h = new Date().getHours();
    if (h < 12) return "Good morning";
    if (h < 18) return "Good afternoon";
    return "Good evening";
  }, []);

  const name = userSession?.isGuest ? "there" : userSession?.name || "there";

  const manga = useMemo(
    () => catalog.filter((i) => i.type === "manga"),
    [catalog]
  );
  const novels = useMemo(
    () => catalog.filter((i) => i.type === "novel"),
    [catalog]
  );
  const likedItems = useMemo(
    () => catalog.filter((i) => likes.includes(i.id)),
    [catalog, likes]
  );

  const filtered = useMemo(() => {
    if (!searchQuery.trim()) return [];
    const q = searchQuery.toLowerCase();
    return catalog.filter(
      (i) =>
        i.title.toLowerCase().includes(q) ||
        i.author?.toLowerCase().includes(q)
    );
  }, [catalog, searchQuery]);

  const toggleLike = (id: string) => {
    setLikes(localDb.toggleLike(id));
  };

  const ensureCollection = () => {
    if (collections.length === 0) {
      const c = localDb.createCollection("Favorites");
      setCollections(localDb.getCollections());
      return c;
    }
    return collections[0];
  };

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 overflow-hidden">
      <div className="absolute top-0 inset-x-0 h-[480px] pointer-events-none">
        <div className="absolute -top-[10%] left-[8%] w-[320px] h-[320px] rounded-full bg-[#7BC6FF]/8 blur-[120px]" />
        <div className="absolute top-[5%] right-[5%] w-[280px] h-[280px] rounded-full bg-sky-500/5 blur-[100px]" />
      </div>

      <div className="relative max-w-7xl mx-auto px-5 pt-12 z-10 space-y-10">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div className="space-y-1">
            <p className="text-[11px] tracking-[0.2em] uppercase text-[#7BC6FF]/70">Reading</p>
            <h1 className="text-3xl font-semibold tracking-tight text-[#F5F7FA]">
              {greeting}, {name}
            </h1>
          </div>
          <div className="flex gap-2">
            <LiquidGlassBar intensity={liquidIntensity} className="flex-1 sm:flex-none">
              <button
                onClick={() => setShowSearch(true)}
                className="flex items-center gap-2 px-4 py-2.5 text-sm text-slate-400 w-full sm:w-auto"
              >
                <Search className="w-4 h-4 text-[#7BC6FF]" />
                Search library…
              </button>
            </LiquidGlassBar>
            {onOpenSources && (
              <button
                onClick={onOpenSources}
                className="px-4 py-2.5 rounded-full bg-[#7BC6FF] text-slate-950 text-sm font-semibold"
              >
                Sources
              </button>
            )}
          </div>
        </div>

        {/* Continue */}
        {history.length > 0 && (
          <section className="space-y-3">
            <SectionTitle icon={Clock} label="Continue reading" />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {history.slice(0, 4).map((h) => (
                <LiquidGlass
                  key={h.id}
                  intensity={liquidIntensity}
                  className="p-3 cursor-pointer"
                  onClick={() => onResumeReading(h.mediaId, h.chapterId)}
                >
                  <div className="flex gap-3 items-center">
                    <div className="w-12 h-16 rounded-lg bg-slate-800 overflow-hidden shrink-0">
                      {h.mediaCover ? (
                        <img src={h.mediaCover} alt="" className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                      ) : null}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-[#F5F7FA] truncate">{h.mediaTitle}</p>
                      <p className="text-xs text-slate-400 truncate">
                        Ch. {h.chapterNumber}: {h.chapterTitle}
                      </p>
                      <div className="mt-2 h-1 rounded-full bg-white/10 overflow-hidden">
                        <div
                          className="h-full bg-[#7BC6FF] rounded-full"
                          style={{ width: `${h.progressPercent}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </LiquidGlass>
              ))}
            </div>
          </section>
        )}

        {/* Your library from sources */}
        <section className="space-y-3">
          <SectionTitle icon={BookOpen} label="From your sources" />
          {catalog.length === 0 ? (
            <LiquidGlass intensity={liquidIntensity} className="p-6 text-center space-y-2">
              <p className="text-sm text-[#F5F7FA]">No series yet</p>
              <p className="text-xs text-slate-500">
                Install Mihon or LNReader sources, then add titles. Each book gets a stable ID in your library.
              </p>
              {onOpenSources && (
                <button
                  onClick={onOpenSources}
                  className="mt-2 px-4 py-2 rounded-xl bg-[#7BC6FF] text-slate-950 text-xs font-semibold"
                >
                  Open sources
                </button>
              )}
            </LiquidGlass>
          ) : (
            <HorizontalCards
              items={catalog.slice(0, 24)}
              likes={likes}
              onSelect={onSelectItem}
              onToggleLike={toggleLike}
              intensity={liquidIntensity}
            />
          )}
        </section>

        {manga.length > 0 && (
          <section className="space-y-3">
            <SectionTitle icon={BookOpen} label="Manga" />
            <HorizontalCards items={manga} likes={likes} onSelect={onSelectItem} onToggleLike={toggleLike} intensity={liquidIntensity} />
          </section>
        )}

        {novels.length > 0 && (
          <section className="space-y-3">
            <SectionTitle icon={BookOpen} label="Novels" />
            <HorizontalCards items={novels} likes={likes} onSelect={onSelectItem} onToggleLike={toggleLike} intensity={liquidIntensity} />
          </section>
        )}

        {/* Likes */}
        <section className="space-y-3">
          <SectionTitle icon={Heart} label="Likes" />
          {likedItems.length === 0 ? (
            <p className="text-xs text-slate-500 px-1">Tap the heart on a series to save it here.</p>
          ) : (
            <HorizontalCards items={likedItems} likes={likes} onSelect={onSelectItem} onToggleLike={toggleLike} intensity={liquidIntensity} />
          )}
        </section>

        {/* Collections */}
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <SectionTitle icon={FolderOpen} label="Collections" />
            <button
              onClick={() => {
                localDb.createCollection(`List ${collections.length + 1}`);
                setCollections(localDb.getCollections());
              }}
              className="text-xs text-[#7BC6FF]"
            >
              New list
            </button>
          </div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {collections.length === 0 && (
              <LiquidGlass intensity={liquidIntensity} className="px-4 py-3 shrink-0">
                <button
                  className="text-xs text-slate-300"
                  onClick={() => {
                    ensureCollection();
                    setCollections(localDb.getCollections());
                  }}
                >
                  Create a collection
                </button>
              </LiquidGlass>
            )}
            {collections.map((c) => (
              <LiquidGlass key={c.id} intensity={liquidIntensity} className="px-4 py-3 shrink-0 min-w-[140px]">
                <p className="text-sm text-[#F5F7FA]">{c.name}</p>
                <p className="text-[10px] text-slate-500">{c.seriesIds.length} titles</p>
              </LiquidGlass>
            ))}
          </div>
        </section>

        {/* Recommended from RDB when available */}
        {recommended.length > 0 && (
          <section className="space-y-3">
            <SectionTitle icon={Star} label="Recommended" />
            <HorizontalCards
              items={recommended}
              likes={likes}
              onSelect={onSelectItem}
              onToggleLike={toggleLike}
              intensity={liquidIntensity}
            />
          </section>
        )}

        {/* Downloads shortcut */}
        <section className="space-y-3 pb-8">
          <SectionTitle icon={Download} label="Downloads" />
          <LiquidGlass intensity={liquidIntensity} className="p-4">
            <p className="text-xs text-slate-400">
              Offline chapters appear here after you download from a series page. Guests keep downloads on this device only.
            </p>
          </LiquidGlass>
        </section>
      </div>

      <AnimatePresence>
        {showSearch && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-md p-4 flex flex-col"
          >
            <div className="max-w-lg mx-auto w-full mt-16 space-y-4">
              <LiquidGlass intensity={24} className="p-2">
                <input
                  autoFocus
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search your library…"
                  className="w-full bg-transparent px-3 py-2 text-sm outline-none text-white"
                />
              </LiquidGlass>
              <button onClick={() => setShowSearch(false)} className="text-xs text-slate-400">
                Close
              </button>
              <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                {filtered.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => {
                      onSelectItem(item);
                      setShowSearch(false);
                    }}
                    className="w-full text-left p-3 rounded-xl bg-white/5 border border-white/5"
                  >
                    <p className="text-sm text-white">{item.title}</p>
                    <p className="text-[10px] text-slate-500">{item.type} · {item.author}</p>
                  </button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function SectionTitle({ icon: Icon, label }: { icon: any; label: string }) {
  return (
    <div className="flex items-center gap-2">
      <Icon className="w-3.5 h-3.5 text-[#7BC6FF]/80" />
      <h2 className="text-[11px] uppercase tracking-[0.18em] text-slate-500 font-medium">{label}</h2>
    </div>
  );
}

function HorizontalCards({
  items,
  likes,
  onSelect,
  onToggleLike,
  intensity,
}: {
  items: MediaItem[];
  likes: string[];
  onSelect: (i: MediaItem) => void;
  onToggleLike: (id: string) => void;
  intensity: number;
}) {
  return (
    <div className="flex gap-4 overflow-x-auto pb-2 snap-x">
      {items.map((item) => {
        const liked = likes.includes(item.id);
        return (
          <div key={item.id} className="w-36 shrink-0 snap-start space-y-2">
            <LiquidGlass
              intensity={intensity}
              radius={16}
              className="aspect-[3/4] cursor-pointer overflow-hidden"
              onClick={() => onSelect(item)}
            >
              {item.coverUrl ? (
                <img
                  src={item.coverUrl}
                  alt=""
                  className="absolute inset-0 w-full h-full object-cover opacity-90"
                  referrerPolicy="no-referrer"
                />
              ) : (
                <div className="absolute inset-0 flex items-center justify-center text-[#7BC6FF]/40 text-2xl font-semibold">
                  {item.title.slice(0, 1)}
                </div>
              )}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onToggleLike(item.id);
                }}
                className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40"
              >
                <Heart
                  className={`w-3.5 h-3.5 ${
                    liked ? "fill-rose-400 text-rose-400" : "text-white/80"
                  }`}
                />
              </button>
              <span className="absolute bottom-2 left-2 text-[9px] uppercase tracking-wide text-[#7BC6FF]">
                {item.type}
              </span>
            </LiquidGlass>
            <p className="text-xs text-[#F5F7FA] truncate px-0.5">{item.title}</p>
            <p className="text-[10px] text-slate-500 truncate px-0.5">{item.author}</p>
          </div>
        );
      })}
    </div>
  );
}
