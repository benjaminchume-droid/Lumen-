import { useState, useMemo } from "react";
import { BookOpen, Star, Trash2, Heart, LayoutGrid, ListFilter, Sparkles, FolderHeart, Info } from "lucide-react";
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
  const [activeType, setActiveType] = useState<MediaType | 'all'>('all');
  const [libraryFilter, setLibraryFilter] = useState('');
  const [sortBy, setSortBy] = useState<'title' | 'rating' | 'chapters' | 'custom'>('title');
  const [activeFolder, setActiveFolder] = useState<'all' | 'favorites' | 'forge'>('all');

  // Multi-type selection
  const typesList: { id: MediaType | 'all'; label: string }[] = [
    { id: 'all', label: 'All Shelves' },
    { id: 'manga', label: 'Manga' },
    { id: 'novel', label: 'Novels' },
    { id: 'hybrid', label: 'Hybrids' },
  ];

  // Stats
  const stats = useMemo(() => {
    const total = library.length;
    const mangas = library.filter((item) => item.type === "manga").length;
    const novels = library.filter((item) => item.type === "novel").length;
    const custom = library.filter((item) => item.isCustom).length;
    const loved = favorites.length;
    return { total, mangas, novels, custom, loved };
  }, [library, favorites]);

  // Folder filtering & Sorting & Search Matching
  const filteredLibrary = useMemo(() => {
    return library
      .filter((item) => {
        // Type filter
        if (activeType !== 'all' && item.type !== activeType) return false;
        
        // Folder filter
        if (activeFolder === 'favorites' && !favorites.includes(item.id)) return false;
        if (activeFolder === 'forge' && !item.isCustom) return false;
        
        // Search filter
        if (libraryFilter) {
          return item.title.toLowerCase().includes(libraryFilter.toLowerCase()) ||
            item.author.toLowerCase().includes(libraryFilter.toLowerCase());
        }
        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'rating') return b.rating - a.rating;
        if (sortBy === 'chapters') return b.chaptersCount - a.chaptersCount;
        if (sortBy === 'custom') return (b.isCustom ? 1 : 0) - (a.isCustom ? 1 : 0);
        return a.title.localeCompare(b.title);
      });
  }, [library, activeType, activeFolder, libraryFilter, sortBy, favorites]);

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      
      {/* Background radial lighting */}
      <div className="absolute top-0 right-10 w-[400px] h-[400px] rounded-full bg-indigo-500/5 blur-[120px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-6 space-y-8 relative z-10">
        
        {/* Core Header section */}
        <div className="space-y-1">
          <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">User Repository</span>
          <h1 className="text-3xl font-sans font-semibold tracking-tight">Reading Library</h1>
        </div>

        {/* STATISTICS BOARD */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div 
            className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex flex-col justify-between space-y-1"
            style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
          >
            <span className="text-[10px] font-mono text-slate-500 uppercase">Collection scope</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-[#F5F7FA]">{stats.total}</span>
              <span className="text-xs text-slate-400">shelved</span>
            </div>
          </div>
          <div 
            className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex flex-col justify-between space-y-1"
            style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
          >
            <span className="text-[10px] font-mono text-slate-500 uppercase">Interactive Manga</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-[#7BC6FF]">{stats.mangas}</span>
              <span className="text-xs text-slate-400">adaptations</span>
            </div>
          </div>
          <div 
            className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex flex-col justify-between space-y-1"
            style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
          >
            <span className="text-[10px] font-mono text-slate-500 uppercase">Loved Series</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-rose-400">{stats.loved}</span>
              <span className="text-xs text-slate-400">starred</span>
            </div>
          </div>
          <div 
            className="p-4 rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex flex-col justify-between space-y-1"
            style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
          >
            <span className="text-[10px] font-mono text-slate-500 uppercase">Synthesis Forge</span>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-semibold text-purple-400">{stats.custom}</span>
              <span className="text-xs text-slate-400">Forged AI</span>
            </div>
          </div>
        </div>

        {/* CONTROLS BAR: Segmented glass tab + Filter collections folders + Sorting selectors */}
        <div className="flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between">
          
          {/* Glass Segmented tab control */}
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

          {/* Folder Category Selectors (Favorites, Forge) */}
          <div className="flex items-center gap-2">
            <button
              id="lib-folder-all"
              onClick={() => setActiveFolder('all')}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === 'all'
                  ? 'bg-white/10 border-[#7BC6FF]/30 text-[#7BC6FF]'
                  : 'bg-white/5 border-white/5 text-slate-400 hover:bg-white/10'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>ALL</span>
            </button>
            <button
              id="lib-folder-favorites"
              onClick={() => setActiveFolder('favorites')}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === 'favorites'
                  ? 'bg-rose-950/20 border-rose-500/30 text-rose-400'
                  : 'bg-white/5 border-white/5 text-slate-400 hover:bg-white/10'
              }`}
            >
              <Heart className="w-3.5 h-3.5 fill-current" />
              <span>FAVORITES</span>
            </button>
            <button
              id="lib-folder-forge"
              onClick={() => setActiveFolder('forge')}
              className={`px-3 py-1.5 rounded-xl border text-xs font-mono tracking-wide flex items-center gap-1.5 transition-all cursor-pointer ${
                activeFolder === 'forge'
                  ? 'bg-purple-950/20 border-purple-500/30 text-purple-400'
                  : 'bg-white/5 border-white/5 text-slate-400 hover:bg-white/10'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>AI FORGE</span>
            </button>
          </div>

          {/* Sorting controls & Local Shelve Filter */}
          <div className="flex items-center gap-3">
            <input
              id="library-filter-field"
              type="text"
              placeholder="Filter list..."
              value={libraryFilter}
              onChange={(e) => setLibraryFilter(e.target.value)}
              className="px-4 py-2 bg-white/5 border border-white/10 focus:border-[#7BC6FF]/30 rounded-full text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-[#7BC6FF]/20 backdrop-blur-sm shadow-inner"
            />

            <div className="flex items-center gap-1.5">
              <ListFilter className="w-3.5 h-3.5 text-slate-400" />
              <select
                id="library-sort-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="bg-transparent text-slate-300 font-mono text-xs border border-white/10 hover:border-white/20 px-3 py-1.5 rounded-xl select-none"
              >
                <option className="bg-slate-900 text-white" value="title">Alphabetical</option>
                <option className="bg-slate-900 text-white" value="rating">Stars Rating</option>
                <option className="bg-slate-900 text-white" value="chapters">Chapters Volume</option>
                <option className="bg-slate-900 text-white" value="custom">Forged Only</option>
              </select>
            </div>
          </div>

        </div>

        {/* SHELVES CARDS GRID */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6 pt-2">
          <AnimatePresence mode="popLayout">
            {filteredLibrary.map((item) => {
              const loved = favorites.includes(item.id);
              return (
                <motion.div
                  layout
                  id={`shelf-card-${item.id}`}
                  key={item.id}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  transition={{ duration: 0.3 }}
                  className="group flex flex-col gap-3 relative cursor-pointer"
                >
                  <div 
                    onClick={() => onSelectItem(item)}
                    className="relative aspect-[3/4] w-full rounded-2xl overflow-hidden border border-white/5 bg-slate-900 shadow-md group-hover:shadow-[0_0_24px_4px_rgba(123,198,255,0.15)] group-hover:border-[#7BC6FF]/30 transition-all duration-300"
                  >
                    <img 
                      src={item.coverUrl} 
                      alt={item.title} 
                      className="w-full h-full object-cover group-hover:scale-104 transition-all duration-500" 
                      referrerPolicy="no-referrer"
                    />
                    
                    {/* Shadow visual layer */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-transparent to-transparent opacity-60 group-hover:opacity-80 transition-opacity" />

                    {/* Left overlay tag: AI Forged badge */}
                    {item.isCustom && (
                      <span className="absolute top-2 left-2 px-1.5 py-0.5 bg-purple-500 text-slate-950 font-bold font-mono text-[8px] rounded-md shadow-lg flex items-center gap-0.5 uppercase tracking-wide">
                        <Sparkles className="w-2.5 h-2.5 fill-current" />
                        AI Forge
                      </span>
                    )}

                    {/* Status badges */}
                    <div className="absolute bottom-3 left-3 right-3 flex justify-between items-center text-[10px] font-mono text-slate-300 pointer-events-none">
                      <span className="capitalize">{item.type}</span>
                      <span>Ch. {item.chaptersCount}</span>
                    </div>
                  </div>

                  {/* Fast Interactive Hover Overlay options */}
                  <div className="absolute top-2 right-2 flex flex-col gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10">
                    <button
                      id={`lib-card-fav-${item.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleFavorite(item.id);
                      }}
                      className={`p-1.5 rounded-full backdrop-blur-md cursor-pointer transition-all border ${
                        loved 
                          ? 'bg-rose-500 border-transparent text-white' 
                          : 'bg-slate-950/60 border-white/15 text-slate-300 hover:text-white'
                      }`}
                    >
                      <Heart className="w-3.5 h-3.5 fill-current" />
                    </button>
                    
                    {/* Permit deletion of custom or non-essential books */}
                    <button
                      id={`lib-card-del-${item.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteItem(item.id);
                      }}
                      className="p-1.5 rounded-full backdrop-blur-md bg-slate-950/60 border border-white/10 text-slate-400 hover:text-rose-400 cursor-pointer transition-all"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Title labels */}
                  <div onClick={() => onSelectItem(item)} className="space-y-0.5 px-1 min-w-0">
                    <h3 className="font-semibold text-sm text-[#F5F7FA] tracking-tight group-hover:text-[#7BC6FF] transition-colors truncate">
                      {item.title}
                    </h3>
                    <p className="text-xs text-slate-400 truncate tracking-wide">{item.author}</p>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>

        {/* Empty shelf state fallback */}
        {filteredLibrary.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center space-y-4">
            <span className="text-4xl">📚</span>
            <div>
              <h2 className="text-lg font-semibold text-slate-400">Library sector appears empty</h2>
              <p className="text-xs text-slate-500 max-w-sm mt-1">No shelved elements match your category or sorting metrics. Create a series inside home or adjust the filters above.</p>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
