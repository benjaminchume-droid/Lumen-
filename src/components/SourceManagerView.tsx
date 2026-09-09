import { useState } from "react";
import { 
  ShieldCheck, RefreshCw, AlertTriangle, ToggleLeft, ToggleRight, 
  Search, Play, CheckCircle, Wifi, Globe, Server, Cpu
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MANGA_REPOSITORIES, NOVEL_REPOSITORIES, SourceExtension } from "../data/sources";

interface SourceManagerViewProps {
  activeSourceIds: string[];
  onToggleSource: (id: string) => void;
  onClose?: () => void;
}

export default function SourceManagerView({ activeSourceIds, onToggleSource, onClose }: SourceManagerViewProps) {
  const [mangaSources, setMangaSources] = useState<SourceExtension[]>(MANGA_REPOSITORIES);
  const [novelSources, setNovelSources] = useState<SourceExtension[]>(NOVEL_REPOSITORIES);
  const [filterType, setFilterType] = useState<"all" | "manga" | "novel">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [pingingId, setPingingId] = useState<string | null>(null);

  // Trigger simulated registry ping diagnostic
  const handleTestDiagnostic = (id: string, isManga: boolean) => {
    setPingingId(id);
    setTimeout(() => {
      const randomLatency = Math.floor(Math.random() * 120) + 15;
      const randomStatus = Math.random() > 0.15 ? "healthy" : "degraded";
      
      const mapper = (src: SourceExtension) => {
        if (src.id === id) {
          return {
            ...src,
            pingMs: randomLatency,
            healthStatus: randomStatus as any
          };
        }
        return src;
      };

      if (isManga) {
        setMangaSources(prev => prev.map(mapper));
      } else {
        setNovelSources(prev => prev.map(mapper));
      }
      setPingingId(null);
    }, 1200);
  };

  const filteredManga = mangaSources.filter(s => 
    s.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    s.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredNovels = novelSources.filter(s => 
    s.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    s.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12 select-none">
      {/* Background radial atmosphere glow */}
      <div className="absolute top-1/4 right-[5%] w-[450px] h-[450px] rounded-full bg-sky-500/5 blur-[160px] pointer-events-none" />

      <div className="max-w-4xl mx-auto px-6 space-y-8 relative z-10">
        
        {onClose && (
          <button
            id="btn-source-manager-back"
            onClick={onClose}
            className="group flex items-center gap-2 p-2 px-4 border border-white/5 bg-white/5 hover:bg-white/10 rounded-full cursor-pointer text-xs text-slate-300 hover:text-white transition-all self-start font-mono"
          >
            &larr; BACK
          </button>
        )}

        {/* Upper Header Section */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-white/5">
          <div className="space-y-1">
            <span className="text-xs uppercase font-mono tracking-[0.25em] text-[#7BC6FF] flex items-center gap-1.5 font-bold">
              <Cpu className="w-3.5 h-3.5 animate-spin" style={{ animationDuration: '4s' }} />
              Universal Parser Core
            </span>
            <h1 className="text-3xl font-sans font-semibold tracking-tight text-white">Source Manager</h1>
            <p className="text-xs text-slate-500 max-w-lg leading-relaxed">
              Integrate, sandbox, and track external catalog extensions. Toggle repositories to inject original series directly into your reading ecosystem.
            </p>
          </div>

          {/* Filtering Buttons */}
          <div className="flex bg-slate-900/60 p-1 border border-white/5 rounded-2xl self-start md:self-auto shadow-inner">
            {(["all", "manga", "novel"] as const).map((type) => (
              <button
                key={type}
                onClick={() => setFilterType(type)}
                className={`px-4 py-1.5 text-xs font-mono rounded-xl uppercase tracking-wider font-semibold cursor-pointer transition-all ${
                  filterType === type 
                    ? "bg-[#7BC6FF] text-slate-950 shadow-md" 
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>

        {/* Extensions Diagnostics Banner */}
        <div 
          className="p-4 rounded-2xl border border-[#7BC6FF]/10 bg-[#7BC6FF]/5 flex items-center justify-between gap-4 shadow-sm"
          style={{ boxShadow: 'inset 0 1px 1px 0 rgba(123,198,255,0.05)' }}
        >
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-5 h-5 text-[#7BC6FF] shrink-0" />
            <div className="space-y-0.5">
              <span className="text-xs font-mono font-bold text-[#7BC6FF] uppercase">SANDBOX ISOLATION ACTIVE</span>
              <p className="text-[10px] text-slate-400">All external parser adapters are fully sandboxed using virtual container isolated script executors.</p>
            </div>
          </div>
          <span className="text-[9px] font-mono text-[#7BC6FF] bg-white/5 border border-white/5 px-2.5 py-1 rounded-full font-bold">LUMEN SANDBOX v2.1</span>
        </div>

        {/* Search Input bar */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -to-translate-y-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search active repository manifests..."
            className="w-full pl-11 pr-5 py-3 bg-white/5 border border-white/5 rounded-2xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-[#7BC6FF]/30 transition-all shadow-inner backdrop-blur-md"
          />
        </div>

        {/* CATEGORY 1: MANGA REPOSITORIES (Strict Separation) */}
        {(filterType === "all" || filterType === "manga") && (
          <div className="space-y-4">
            <div className="flex items-center gap-2 px-1">
              <Globe className="w-4 h-4 text-[#7BC6FF]" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-[0.2em] text-slate-400">Manga Sources ({filteredManga.length})</h2>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredManga.map((src) => {
                const isActive = activeSourceIds.includes(src.id);
                return (
                  <div
                    id={`source-card-${src.id}`}
                    key={src.id}
                    className="p-5 rounded-2xl border border-white/5 bg-slate-900/30 backdrop-blur-md flex flex-col justify-between gap-4 hover:border-white/10 transition-all group"
                  >
                    <div className="flex justify-between items-start gap-3">
                      <div className="min-w-0 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="px-2 py-0.5 bg-[#7BC6FF]/10 text-[#7BC6FF] text-[9px] font-mono rounded font-semibold">MANGA</span>
                          <span className="text-[10px] text-slate-500 font-mono">v{src.version}</span>
                        </div>
                        <h3 className="font-semibold text-sm text-white truncate group-hover:text-[#7BC6FF] transition-colors">{src.name}</h3>
                        <p className="text-[10px] text-slate-500 font-mono truncate">{src.repositoryUrl}</p>
                      </div>

                      {/* Tactile Toggle option */}
                      <button
                        id={`btn-toggle-source-${src.id}`}
                        onClick={() => onToggleSource(src.id)}
                        className="text-slate-500 hover:text-white transition-colors cursor-pointer"
                      >
                        {isActive ? (
                          <ToggleRight className="w-9 h-6 text-[#7BC6FF]" />
                        ) : (
                          <ToggleLeft className="w-9 h-6 text-slate-600" />
                        )}
                      </button>
                    </div>

                    <div className="flex items-center justify-between pt-3 border-t border-white/5 text-[10px] font-mono text-slate-400">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-1.5">
                          <Wifi className={`w-3.5 h-3.5 ${
                            src.healthStatus === "healthy" ? "text-emerald-500" :
                            src.healthStatus === "degraded" ? "text-amber-500" : "text-rose-500"
                          }`} />
                          <span className="capitalize">{src.healthStatus}</span>
                        </div>
                        <span>Ping {src.pingMs}ms</span>
                      </div>

                      {/* Diagnostic trigger */}
                      <button
                        onClick={() => handleTestDiagnostic(src.id, true)}
                        disabled={pingingId === src.id}
                        className="text-xs text-[#7BC6FF] hover:underline flex items-center gap-1 cursor-pointer font-bold"
                      >
                        {pingingId === src.id ? (
                          <RefreshCw className="w-3 h-3 animate-spin text-[#7BC6FF]" />
                        ) : (
                          <Play className="w-3 h-3 text-[#7BC6FF]" />
                        )}
                        <span>{pingingId === src.id ? "TESTING" : "TEST"}</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* CATEGORY 2: NOVEL REPOSITORIES (Strict Separation) */}
        {(filterType === "all" || filterType === "novel") && (
          <div className="space-y-4">
            <div className="flex items-center gap-2 px-1">
              <Server className="w-4 h-4 text-purple-400" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-[0.2em] text-slate-400">Novel Sources ({filteredNovels.length})</h2>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredNovels.map((src) => {
                const isActive = activeSourceIds.includes(src.id);
                return (
                  <div
                    id={`source-card-${src.id}`}
                    key={src.id}
                    className="p-5 rounded-2xl border border-white/5 bg-slate-900/30 backdrop-blur-md flex flex-col justify-between gap-4 hover:border-white/10 transition-all group"
                  >
                    <div className="flex justify-between items-start gap-3">
                      <div className="min-w-0 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="px-2 py-0.5 bg-purple-500/10 text-purple-400 text-[9px] font-mono rounded font-semibold">NOVEL</span>
                          <span className="text-[10px] text-slate-500 font-mono">v{src.version}</span>
                        </div>
                        <h3 className="font-semibold text-sm text-white truncate group-hover:text-purple-400 transition-colors">{src.name}</h3>
                        <p className="text-[10px] text-slate-500 font-mono truncate">{src.repositoryUrl}</p>
                      </div>

                      <button
                        id={`btn-toggle-source-${src.id}`}
                        onClick={() => onToggleSource(src.id)}
                        className="text-slate-500 hover:text-white transition-colors cursor-pointer"
                      >
                        {isActive ? (
                          <ToggleRight className="w-9 h-6 text-purple-400" />
                        ) : (
                          <ToggleLeft className="w-9 h-6 text-slate-600" />
                        )}
                      </button>
                    </div>

                    <div className="flex items-center justify-between pt-3 border-t border-white/5 text-[10px] font-mono text-slate-400">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-1.5">
                          <Wifi className={`w-3.5 h-3.5 ${
                            src.healthStatus === "healthy" ? "text-emerald-500" :
                            src.healthStatus === "degraded" ? "text-amber-500" : "text-rose-500"
                          }`} />
                          <span className="capitalize">{src.healthStatus}</span>
                        </div>
                        <span>Ping {src.pingMs}ms</span>
                      </div>

                      <button
                        onClick={() => handleTestDiagnostic(src.id, false)}
                        disabled={pingingId === src.id}
                        className="text-xs text-purple-400 hover:underline flex items-center gap-1 cursor-pointer font-bold"
                      >
                        {pingingId === src.id ? (
                          <RefreshCw className="w-3 h-3 animate-spin text-purple-400" />
                        ) : (
                          <Play className="w-3 h-3 text-purple-400" />
                        )}
                        <span>{pingingId === src.id ? "TESTING" : "TEST"}</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
