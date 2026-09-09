import { useState, useMemo } from "react";
import { Search, Sparkles, Clock, BookOpen, Star, ArrowRight, X } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MediaItem, HistoryEntry } from "../types";

interface HomeViewProps {
  catalog: MediaItem[];
  history: HistoryEntry[];
  onSelectItem: (item: MediaItem) => void;
  onResumeReading: (mediaId: string, chapterId: string) => void;
  onOpenForge: () => void;
  userSession?: { name: string, email: string, isGuest: boolean };
}

export default function HomeView({ catalog, history, onSelectItem, onResumeReading, onOpenForge, userSession }: HomeViewProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearchOverlay, setShowSearchOverlay] = useState(false);
  const [activeSearchGenre, setActiveSearchGenre] = useState<string | null>(null);

  // Greeting based on time
  const greeting = useMemo(() => {
    const hours = new Date().getHours();
    if (hours < 12) return "Good morning";
    if (hours < 18) return "Good afternoon";
    return "Good evening";
  }, []);

  // Filter lists based on rating / types
  const trendingManga = useMemo(() => {
    return catalog.filter((item) => item.type === "manga").sort((a,b) => b.rating - a.rating);
  }, [catalog]);

  const trendingNovels = useMemo(() => {
    return catalog.filter((item) => item.type === "novel").sort((a,b) => b.rating - a.rating);
  }, [catalog]);

  const hybridPicks = useMemo(() => {
    return catalog.filter((item) => item.type === "hybrid");
  }, [catalog]);

  // All extracted genres for searching
  const allGenres = useMemo(() => {
    const genresSet = new Set<string>();
    catalog.forEach((item) => item.genres.forEach((g) => genresSet.add(g)));
    return Array.from(genresSet);
  }, [catalog]);

  // Search filter
  const filteredSearch = useMemo(() => {
    if (!searchQuery && !activeSearchGenre) return [];
    return catalog.filter((item) => {
      const matchesText = searchQuery 
        ? item.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
          (item.author && item.author.toLowerCase().includes(searchQuery.toLowerCase())) ||
          item.description.toLowerCase().includes(searchQuery.toLowerCase())
        : true;
      const matchesGenre = activeSearchGenre ? item.genres.includes(activeSearchGenre) : true;
      return matchesText && matchesGenre;
    });
  }, [catalog, searchQuery, activeSearchGenre]);

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 overflow-hidden select-none">
      
      {/* Cinematic Ambient Lighting - Fluid Backdrop Glows */}
      <div className="absolute top-0 left-0 right-0 h-[600px] pointer-events-none overflow-hidden z-0">
        <div 
          className="absolute -top-[20%] left-[10%] w-[350px] h-[350px] rounded-full bg-sky-500/10 blur-[130px] animate-pulse" 
          style={{ animationDuration: '8s' }} 
        />
        <div 
          className="absolute top-[10%] right-[10%] w-[450px] h-[450px] rounded-full bg-[#7BC6FF]/5 blur-[160px] animate-pulse" 
          style={{ animationDuration: '12s' }} 
        />
      </div>

      <div className="relative max-w-7xl mx-auto px-6 pt-12 md:pt-16 z-10 space-y-12">
        
        {/* Top Header Block */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div className="space-y-1">
            <span className="text-xs uppercase font-mono tracking-[0.25em] text-[#7BC6FF] flex items-center gap-1.5 font-semibold">
              <Sparkles className="w-3.5 h-3.5 animate-spin" style={{ animationDuration: '3s' }} />
              Reading OS Active
            </span>
            <h1 className="text-3xl md:text-4xl font-sans font-semibold tracking-tight text-[#F5F7FA]">
              {greeting}, {userSession ? userSession.name : "Reader"}
            </h1>
          </div>

          {/* Mini Interactive Actions bar */}
          <div className="flex items-center gap-3 w-full md:w-auto">
            {/* Custom Search capsule trigger */}
            <button 
              id="btn-search-trigger"
              onClick={() => setShowSearchOverlay(true)}
              className="flex-1 md:flex-initial flex items-center gap-3 px-4 py-2 bg-white/5 border border-white/10 rounded-full text-slate-400 hover:text-white hover:border-white/20 hover:bg-white/10 transition-all cursor-pointer backdrop-blur-sm self-stretch text-left text-sm font-medium"
            >
              <Search className="w-4 h-4 text-[#7BC6FF]" />
              <span className="pr-8">Explore catalog...</span>
              <kbd className="hidden md:inline-block text-[10px] font-mono bg-white/10 px-1.5 py-0.5 rounded text-white/50 border border-white/5 shadow-inner">Ctrl K</kbd>
            </button>

            {/* Lumen Forge Catalyst Trigger */}
            <button 
              id="btn-forge-shortcut"
              onClick={onOpenForge}
              className="flex items-center gap-2 px-5 py-2.5 bg-[#7BC6FF] text-slate-950 rounded-full font-semibold text-sm hover:brightness-110 shadow-[0_4px_20px_0_rgba(123,198,255,0.3)] hover:scale-102 active:scale-98 transition-all cursor-pointer"
            >
              <Sparkles className="w-4 h-4" />
              <span>Forge</span>
            </button>
          </div>
        </div>

        {/* SECTION: CONTINUE READING */}
        {history.length > 0 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-[#7BC6FF]" />
              <h2 className="text-sm uppercase font-mono tracking-[0.2em] font-semibold text-slate-400">Continue Reading</h2>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {history.slice(0, 3).map((hist) => {
                const item = catalog.find((c) => c.id === hist.mediaId);
                return (
                  <div
                    id={`continue-item-${hist.id}`}
                    key={hist.id}
                    onClick={() => onResumeReading(hist.mediaId, hist.chapterId)}
                    className="relative overflow-hidden rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur-md p-4 flex gap-4 hover:border-[#7BC6FF]/30 hover:bg-slate-900/60 duration-300 transition-all cursor-pointer shadow-lg group"
                    style={{
                      boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.05)'
                    }}
                  >
                    <div className="relative w-16 h-20 rounded-lg overflow-hidden flex-shrink-0 shadow-md">
                      <img src={hist.mediaCover} alt={hist.mediaTitle} className="w-full h-full object-cover group-hover:scale-105 transition-all duration-300" referrerPolicy="no-referrer" />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
                      <div className="absolute bottom-1 right-1 px-1 py-0.5 bg-[#000]/60 rounded text-[8px] font-mono text-slate-200">
                        {hist.type.toUpperCase()}
                      </div>
                    </div>
                    
                    <div className="flex-1 flex flex-col justify-between min-w-0">
                      <div>
                        <h3 className="font-semibold text-sm leading-tight text-white group-hover:text-[#7BC6FF] transition-colors truncate">
                          {hist.mediaTitle}
                        </h3>
                        <p className="text-xs text-slate-400 mt-0.5 truncate">
                          Chapter {hist.chapterNumber}: {hist.chapterTitle}
                        </p>
                      </div>

                      <div className="space-y-1.5">
                        <div className="flex justify-between items-center text-[10px] text-slate-400">
                          <span className="font-mono">{hist.progressPercent}% read</span>
                          <span>Resume</span>
                        </div>
                        {/* Custom visual glass progress tracker */}
                        <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-gradient-to-r from-[#7BC6FF] to-sky-500 rounded-full"
                            style={{ width: `${hist.progressPercent}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* SECTION: TRENDING MANGA adaptive horizontal grid */}
        {trendingManga.length > 0 && (
          <div className="space-y-4">
            <div className="flex justify-between items-end">
              <div className="flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-[#7BC6FF]" />
                <h2 className="text-sm uppercase font-mono tracking-[0.2em] font-semibold text-slate-400">Trending Manga</h2>
              </div>
            </div>

            <div className="flex overflow-x-auto gap-6 pb-4 scrollbar-thin scrollbar-thumb-white/10 scrollbar-track-transparent snap-x">
              {trendingManga.map((manga) => (
                <div
                  id={`manga-card-${manga.id}`}
                  key={manga.id}
                  onClick={() => onSelectItem(manga)}
                  className="w-44 flex-shrink-0 flex flex-col gap-3 snap-start group cursor-pointer"
                >
                  <div className="relative aspect-[3/4] w-full rounded-2xl overflow-hidden border border-white/5 bg-slate-900 shadow-md group-hover:shadow-[0_0_20px_2px_rgba(123,198,255,0.15)] duration-300 group-hover:border-[#7BC6FF]/30 transition-all">
                    <img 
                      src={manga.coverUrl} 
                      alt={manga.title} 
                      className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-500"
                      referrerPolicy="no-referrer"
                    />
                    
                    {/* Shadow overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-60 group-hover:opacity-80 transition-opacity duration-300" />
                    
                    {/* Floating statistics widget */}
                    <div className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-slate-950/60 backdrop-blur-md rounded-full border border-white/5 text-[10px] flex items-center gap-1 font-semibold text-[#7BC6FF]">
                      <Star className="w-2.5 h-2.5 fill-current" />
                      <span>{manga.rating}</span>
                    </div>

                    <div className="absolute bottom-3 left-3 right-3 text-[10px] font-mono flex justify-between items-center text-slate-300">
                      <span className="capitalize text-[#7BC6FF] font-semibold">{manga.status}</span>
                      <span>Ch. {manga.chaptersCount}</span>
                    </div>
                  </div>

                  <div className="space-y-0.5 px-1">
                    <h3 className="font-semibold text-sm tracking-tight text-[#F5F7FA] group-hover:text-[#7BC6FF] transition-colors truncate">
                      {manga.title}
                    </h3>
                    <p className="text-xs text-slate-400 truncate">{manga.author}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* SECTION: TRENDING LIGHT NOVELS */}
        {trendingNovels.length > 0 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-[#7BC6FF]" />
              <h2 className="text-sm uppercase font-mono tracking-[0.2em] font-semibold text-slate-400">Trending Light Novels</h2>
            </div>

            <div className="flex overflow-x-auto gap-6 pb-4 scrollbar-thin scrollbar-thumb-white/10 scrollbar-track-transparent snap-x">
              {trendingNovels.map((novel) => (
                <div
                  id={`novel-card-${novel.id}`}
                  key={novel.id}
                  onClick={() => onSelectItem(novel)}
                  className="w-44 flex-shrink-0 flex flex-col gap-3 snap-start group cursor-pointer"
                >
                  <div className="relative aspect-[3/4] w-full rounded-2xl overflow-hidden border border-white/5 bg-slate-900 shadow-md group-hover:shadow-[0_0_20px_2px_rgba(123,198,255,0.15)] duration-300 group-hover:border-[#7BC6FF]/30 transition-all">
                    <img 
                      src={novel.coverUrl} 
                      alt={novel.title} 
                      className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-500"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-60 group-hover:opacity-85 transition-opacity duration-300" />
                    
                    <div className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-slate-950/60 backdrop-blur-md rounded-full border border-white/5 text-[10px] flex items-center gap-1 font-semibold text-[#7BC6FF]">
                      <Star className="w-2.5 h-2.5 fill-current" />
                      <span>{novel.rating}</span>
                    </div>

                    <div className="absolute bottom-3 left-3 right-3 text-[10px] font-mono flex justify-between items-center text-slate-300">
                      <span className="capitalize text-sky-400 font-semibold">{novel.status}</span>
                      <span>{novel.chaptersCount} Chapters</span>
                    </div>
                  </div>

                  <div className="space-y-0.5 px-1">
                    <h3 className="font-semibold text-sm tracking-tight text-[#F5F7FA] group-hover:text-[#7BC6FF] transition-colors truncate">
                      {novel.title}
                    </h3>
                    <p className="text-xs text-slate-400 truncate">{novel.author}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* SECTION: HYBRID SPECIFIC PICKS */}
        {hybridPicks.length > 0 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-[#7BC6FF]" />
              <h2 className="text-sm uppercase font-mono tracking-[0.2em] font-semibold text-slate-400">Cinematic Hybrid Picks</h2>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {hybridPicks.map((hybrid) => (
                <div
                  id={`hybrid-bento-${hybrid.id}`}
                  key={hybrid.id}
                  onClick={() => onSelectItem(hybrid)}
                  className="relative overflow-hidden rounded-3xl border border-white/5 bg-slate-900/30 hover:border-[#7BC6FF]/30 hover:bg-slate-900/50 backdrop-blur-md flex flex-col sm:flex-row h-72 cursor-pointer group transition-all duration-500 shadow-xl"
                  style={{
                    boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.05)'
                  }}
                >
                  <div className="sm:w-2/5 h-full relative overflow-hidden">
                    <img 
                      src={hybrid.coverUrl} 
                      alt={hybrid.title} 
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t sm:bg-gradient-to-r from-black/90 via-black/40 to-transparent" />
                  </div>

                  <div className="sm:w-3/5 p-6 flex flex-col justify-between h-full space-y-4">
                    <div className="space-y-2">
                      <div className="flex flex-wrap gap-2">
                        <span className="px-2 py-0.5 bg-[#7BC6FF]/10 text-[#7BC6FF] border border-[#7BC6FF]/20 text-[9px] uppercase font-mono tracking-wider rounded-md font-semibold font-mono">HYBRID</span>
                        {hybrid.genres.slice(0, 2).map((g) => (
                          <span key={g} className="px-2 py-0.5 bg-white/5 text-slate-400 text-[9px] rounded-md font-mono">{g}</span>
                        ))}
                      </div>

                      <h3 className="text-lg font-semibold text-[#F5F7FA] tracking-tight group-hover:text-[#7BC6FF] transition-all truncate pr-4">
                        {hybrid.title}
                      </h3>
                      
                      <p className="text-xs text-slate-400 line-clamp-3 leading-relaxed">
                        {hybrid.description}
                      </p>
                    </div>

                    <div className="flex justify-between items-center pt-2">
                      <div className="space-y-0.5">
                        <p className="text-[10px] text-slate-500 uppercase font-mono">Director</p>
                        <p className="text-xs text-slate-300 font-medium truncate max-w-[120px]">{hybrid.author}</p>
                      </div>
                      
                      <span className="flex items-center gap-1 text-xs text-[#7BC6FF] font-medium group-hover:translate-x-1 duration-300 transition-all font-mono">
                        Begin Odyssey 
                        <ArrowRight className="w-3.5 h-3.5" />
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Catalog Empty Fallback visual element */}
        {catalog.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20 bg-slate-900/10 border border-white/5 rounded-3xl backdrop-blur-sm space-y-4 text-center">
            <Sparkles className="w-8 h-8 text-slate-600 animate-bounce" />
            <h3 className="text-base font-semibold text-slate-400">Pristine OS Platform initialized</h3>
            <p className="text-xs text-slate-500 max-w-sm">No library elements present in active memory. Click &quot;Forge&quot; above to synthesize a customized literary volume instantly using Gemini AI.</p>
          </div>
        )}

      </div>

      {/* SEARCH / ADVANCED EXPLORATION OVERLAY */}
      <AnimatePresence>
        {showSearchOverlay && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-[#0A0C0F]/90 backdrop-blur-lg flex flex-col"
          >
            {/* Top Close bar */}
            <div className="max-w-4xl mx-auto w-full px-6 pt-12 flex justify-between items-center">
              <span className="text-xs font-mono uppercase tracking-[0.2em] text-slate-400">Dynamic Index search</span>
              <button 
                id="btn-close-search"
                onClick={() => {
                  setShowSearchOverlay(false);
                  setSearchQuery("");
                  setActiveSearchGenre(null);
                }}
                className="p-2 border border-white/10 rounded-full hover:bg-white/10 text-white/70 hover:text-white transition-all cursor-pointer self-end"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Main Search capsule content */}
            <div className="max-w-4xl mx-auto w-full flex-1 px-6 pt-8 pb-32 flex flex-col space-y-8 overflow-hidden">
              <div className="space-y-2">
                <div className="relative">
                  <Search className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-[#7BC6FF]" />
                  <input
                    id="search-input"
                    type="text"
                    autoFocus
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search titles, authors, creators..."
                    className="w-full pl-14 pr-6 py-4 bg-white/5 border border-white/10 focus:border-[#7BC6FF]/40 rounded-2xl text-white text-lg placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-[#7BC6FF]/20 shadow-inner backdrop-blur-md"
                  />
                </div>
              </div>

              {/* Filtering Genres */}
              <div className="space-y-3">
                <span className="text-[10px] font-mono uppercase tracking-wider text-slate-500">Filter by category</span>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => setActiveSearchGenre(null)}
                    className={`px-3 py-1.5 rounded-full text-xs font-mono border transition-all cursor-pointer ${
                      !activeSearchGenre 
                        ? 'bg-[#7BC6FF] text-slate-950 border-transparent font-semibold shadow-md' 
                        : 'bg-white/5 text-slate-400 border-white/5 hover:bg-white/10 hover:text-white'
                    }`}
                  >
                    ALL GENRES
                  </button>
                  {allGenres.map((g) => (
                    <button
                      key={g}
                      onClick={() => setActiveSearchGenre(activeSearchGenre === g ? null : g)}
                      className={`px-3 py-1.5 rounded-full text-xs font-mono border transition-all cursor-pointer ${
                        activeSearchGenre === g 
                          ? 'bg-[#7BC6FF] text-slate-950 border-transparent font-semibold shadow-md' 
                          : 'bg-white/5 text-slate-400 border-white/5 hover:bg-white/10'
                      }`}
                    >
                      {g.toUpperCase()}
                    </button>
                  ))}
                </div>
              </div>

              {/* SEARCH RESULTS VIEW */}
              <div className="flex-1 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-white/10 space-y-4">
                {filteredSearch.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filteredSearch.map((item) => (
                      <div
                        id={`search-result-card-${item.id}`}
                        key={item.id}
                        onClick={() => {
                          setShowSearchOverlay(false);
                          onSelectItem(item);
                        }}
                        className="p-3 bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 rounded-2xl flex gap-4 cursor-pointer transition-all duration-300 group"
                      >
                        <img 
                          src={item.coverUrl} 
                          alt={item.title} 
                          className="w-14 h-18 rounded-lg object-cover bg-slate-800"
                          referrerPolicy="no-referrer"
                        />
                        <div className="flex-1 flex flex-col justify-center min-w-0">
                          <span className="text-[9px] font-mono text-[#7BC6FF] font-semibold tracking-wider uppercase">{item.type}</span>
                          <h4 className="font-semibold text-sm text-white group-hover:text-[#7BC6FF] transition-colors truncate">{item.title}</h4>
                          <p className="text-xs text-slate-400 truncate mt-0.5">{item.author}</p>
                          <p className="text-[10px] text-slate-500 truncate mt-1">Genres: {item.genres.join(", ")}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-16 text-center space-y-4">
                    {searchQuery || activeSearchGenre ? (
                      <>
                        <span className="text-3xl">🌌</span>
                        <h4 className="font-semibold text-slate-400">No matching indexed elements</h4>
                        <p className="text-xs text-slate-500 max-w-xs">Refine your query or category filters to find customized novels, manga, or hybrids.</p>
                      </>
                    ) : (
                      <>
                        <Search className="w-8 h-8 text-slate-700 font-light" />
                        <h4 className="text-slate-500 text-sm font-mono">Index database waiting for query</h4>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
