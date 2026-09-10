import { useState, useMemo } from "react";
import { Star, Trash2, Heart, LayoutGrid, ListFilter, Sparkles } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MediaItem, MediaType } from "../types";

interface LibraryViewProps {
  library: MediaItem[];
  favorites: string[];
  onToggleFavorite: (mediaId: string) => void;
  onSelectItem: (item: MediaItem) => void;
  onDeleteItem: (mediaId: string) => void;
}

export default function LibraryView({ library, favorites, onToggleFavorite, onSelectItem, onDeleteItem }: LibraryViewProps) {
  const [activeType, setActiveType] = useState<MediaType | "all">("all");
  const [libraryFilter, setLibraryFilter] = useState("");
  const [sortBy, setSortBy] = useState<"title" | "rating" | "chapters" | "custom">("title");
  const [activeFolder, setActiveFolder] = useState<"all" | "favorites" | "forge">("all");

  const typesList: { id: MediaType | "all"; label: string }[] = [
    { id: "all", label: "All Shelves" },
    { id: "manga", label: "Manga" },
    { id: "novel", label: "Novels" },
  ];

  const stats = useMemo(() => {
    const total = library.length;
    const mangas = library.filter((item) => item.type === "manga").length;
    const novels = library.filter((item) => item.type === "novel").length;
    const custom = library.filter((item) => item.isCustom).length;
    const loved = favorites.length;
    return { total, mangas, novels, custom, loved };
  }, [library, favorites]);

  const filteredLibrary = useMemo(() => {
    return library
      .filter((item) => {
        if (item.type === "hybrid") return false;
        if (activeType !== "all" && item.type !== activeType) return false;
        if (activeFolder === "favorites" && !favorites.includes(item.id)) return false;
        if (activeFolder === "forge" && !item.isCustom) return false;
        if (libraryFilter) {
          return (
            item.title.toLowerCase().includes(libraryFilter.toLowerCase()) ||
            item.author.toLowerCase().includes(libraryFilter.toLowerCase())
          );
        }
        return true;
      })
      .sort((a, b) => {
        if (sortBy === "rating") return b.rating - a.rating;
        if (sortBy === "chapters") return b.chaptersCount - a.chaptersCount;
        if (sortBy === "custom") return (b.isCustom ? 1 : 0) - (a.isCustom ? 1 : 0);
        return a.title.localeCompare(b.title);
      });
  }, [library, activeType, activeFolder, libraryFilter, sortBy, favorites]);

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      <div className="absolute top-0 right-10 w-[400px] h-[400px] rounded-full bg-indigo-500/5 blur-[120px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-6 space-y-8 relative z-10">
        <div className="space-y-1">
          <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">User Repository</span>
          <h1 className="text-3xl font-sans font-semibold tracking-tight">Reading Library</h1>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md" style={{ boxShadow: "inset 0 1px 1px 0 rgba(255,255,255,0.03)" }}>
            <span className="text-[10px] font-mono text-slate-500 uppercase">Collection</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-[#F5F7FA]">{stats.total}</span>
              <span className="text-xs text-slate-400">shelved</span>
            </div>
          </div>
          <div className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md" style={{ boxShadow: "inset 0 1px 1px 0 rgba(255,255,255,0.03)" }}>
            <span className="text-[10px] font-mono text-slate-500 uppercase">Manga</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-[#7BC6FF]">{stats.mangas}</span>
              <span className="text-xs text-slate-400">titles</span>
            </div>
          </div>
          <div className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md" style={{ boxShadow: "inset 0 1px 1px 0 rgba(255,255,255,0.03)" }}>
            <span className="text-[10px] font-mono text-slate-500 uppercase">Novels</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-sky-400">{stats.novels}</span>
              <span className="text-xs text-slate-400">titles</span>
            </div>
          </div>
          <div className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md" style={{ boxShadow: "inset 0 1px 1px 0 rgba(255,255,255,0.03)" }}>
            <span className="text-[10px] font-mono text-slate-500 uppercase">Loved</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-rose-400">{stats.loved}</span>
              <span className="text-xs text-slate-400">starred</span>
            </div>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between">
          <div className="flex p-1 rounded-full bg-slate-950/45 border border-white/10 flex-wrap backdrop-blur-md">
            {typesList.map((type) => (
              <button
                key={type.id}
                onClick={() => setActiveType(type.id)}
                className={`px-4 py-1.5 rounded-full text-xs font-medium cursor-pointer transition-all ${
                  activeType === type.id
                    ? "bg-[#7BC6FF] text-slate-950 font-semibold shadow-inner"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {type.label}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveFolder("all")}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === "all"
                  ? "bg-white/10 border-[#7BC6FF]/30 text-[#7BC6FF]"
                  : "bg-white/5 border-white/5 text-slate-400 hover:bg-white/10"
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>ALL</span>
            </button>
            <button
              onClick={() => setActiveFolder("favorites")}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === "favorites"
                  ? "bg-rose-950/20 border-rose-500/30 text-rose-400"
                  : "bg-white/5 border-white/5 text-slate-400 hover:bg-white/10"
              }`}
            >
              <Heart className="w-3.5 h-3.5 fill-current" />
              <span>FAVORITES</span>
            </button>
            <button
              onClick={() => setActiveFolder("forge")}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === "forge"
                  ? "bg-purple-950/20 border-purple-500/30 text-purple-400"
                  : "bg-white/5 border-white/5 text-slate-400 hover:bg-white/10"
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>PUBLISHED</span>
            </button>
          </div>

          <div className="flex items-center gap-3">
            <input
              type="text"
              placeholder="Filter list..."
              value={libraryFilter}
              onChange={(e) => setLibraryFilter(e.target.value)}
              className="px-4 py-2 bg-white/5 border border-white/10 focus:border-[#7BC6FF]/30 rounded-full text-xs text-white placeholder-slate-500 focus:outline-none backdrop-blur-md"
            />
            <div className="flex items-center gap-1.5">
              <ListFilter className="w-3.5 h-3.5 text-slate-400" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="bg-transparent text-slate-300 font-mono text-xs border border-white/10 px-3 py-1.5 rounded-xl"
              >
                <option className="bg-slate-900" value="title">Alphabetical</option>
                <option className="bg-slate-900" value="rating">Rating</option>
                <option className="bg-slate-900" value="chapters">Chapters</option>
              </select>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6 pt-2">
          <AnimatePresence mode="popLayout">
            {filteredLibrary.map((item) => {
              const loved = favorites.includes(item.id);
              return (
                <motion.div
                  layout
                  key={item.id}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  className="group flex flex-col gap-3 relative cursor-pointer"
                >
                  <div
                    onClick={() => onSelectItem(item)}
                    className="relative aspect-[3/4] w-full rounded-2xl overflow-hidden border border-white/5 bg-slate-900 shadow-md group-hover:border-[#7BC6FF]/30 transition-all"
                  >
                    <img src={item.coverUrl} alt={item.title} className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-transparent to-transparent opacity-60" />
                    <div className="absolute bottom-3 left-3 right-3 flex justify-between text-[10px] font-mono text-slate-300">
                      <span className="capitalize">{item.type}</span>
                      <span>Ch. {item.chaptersCount}</span>
                    </div>
                  </div>
                  <div className="absolute top-2 right-2 flex flex-col gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleFavorite(item.id);
                      }}
                      className={`p-1.5 rounded-full backdrop-blur-md border ${
                        loved ? "bg-rose-500 border-transparent text-white" : "bg-slate-950/60 border-white/15 text-slate-300"
                      }`}
                    >
                      <Heart className="w-3.5 h-3.5 fill-current" />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteItem(item.id);
                      }}
                      className="p-1.5 rounded-full backdrop-blur-md bg-slate-950/60 border border-white/10 text-slate-400 hover:text-rose-400"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  <div onClick={() => onSelectItem(item)} className="space-y-0.5 px-1 min-w-0">
                    <h3 className="font-semibold text-sm text-[#F5F7FA] truncate group-hover:text-[#7BC6FF]">{item.title}</h3>
                    <p className="text-xs text-slate-400 truncate">{item.author}</p>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>

        {filteredLibrary.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center space-y-4">
            <span className="text-4xl">📚</span>
            <div>
              <h2 className="text-lg font-semibold text-slate-400">Library is empty</h2>
              <p className="text-xs text-slate-500 max-w-sm mt-1">
                Install sources from More → Sources, then browse and add series. No placeholder content.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
