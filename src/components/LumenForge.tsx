import { useState, useEffect } from "react";
import { Sparkles, ArrowRight, Dna, HelpCircle, X, Layers, AlertCircle, BookOpen, Clock, PenTool } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MediaType, MediaItem } from "../types";

interface LumenForgeProps {
  onSettleBook: (series: MediaItem) => void;
  onClose: () => void;
  onLaunchReader: (item: MediaItem) => void;
}

export default function LumenForge({ onSettleBook, onClose, onLaunchReader }: LumenForgeProps) {
  const [promptValue, setPromptValue] = useState("");
  const [activeType, setActiveType] = useState<MediaType>('manga');
  const [activeGenre, setActiveGenre] = useState("Sci-Fi");
  const [loadingState, setLoadingState] = useState(false);
  const [loadingStepIndex, setLoadingStepIndex] = useState(0);
  const [errorString, setErrorString] = useState<string | null>(null);
  const [forgedResult, setForgedResult] = useState<MediaItem | null>(null);

  // Loading metrics steps
  const loadingSteps = [
    "Contacting Gemini AI synthesis layer...",
    "Allocating neural narrative channels...",
    "Drafting conceptual visual outlines...",
    "Plotting structural chapter coordinates...",
    "Formatting prose typography rules...",
    "Polishing responsive dialogue balloons...",
    "Structuring panels in liquid glass geometry...",
    "Lumen Genesis compilation final-baking..."
  ];

  useEffect(() => {
    if (!loadingState) return;
    const interval = setInterval(() => {
      setLoadingStepIndex((prev) => (prev + 1) % loadingSteps.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [loadingState]);

  // Prompt templates
  const inspirationCatalysts = [
    { text: "A deep-space astronaut finding a monolith made of pure refracting glass.", type: 'hybrid', genre: 'Sci-Fi' },
    { text: "An ancient chronologist caught inside a revolving grandfather clock tower.", type: 'novel', genre: 'Psychological' },
    { text: "A futuristic detective solving cases using ink-lines that physically manifest.", type: 'manga', genre: 'Detective' }
  ] as const;

  const handleStartForge = async () => {
    if (!promptValue.trim()) {
      setErrorString("Inquire an outline topic first before starting the forge.");
      return;
    }

    setLoadingState(true);
    setLoadingStepIndex(0);
    setErrorString(null);
    setForgedResult(null);

    try {
      const resp = await fetch("/api/gemini/generate-series", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          prompt: promptValue,
          type: activeType,
          genre: activeGenre
        })
      });

      const body = await resp.json();
      if (!resp.ok || !body.success) {
        throw new Error(body.error || "Failed to organize structured book synthesis. Verify API Key settings.");
      }

      const generated: MediaItem = body.series;
      generated.isCustom = true; // Mark as generated
      setForgedResult(generated);
    } catch (err: any) {
      console.error(err);
      setErrorString(err.message || "An error occurred during series compilation.");
    } finally {
      setLoadingState(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0C0F]/95 backdrop-blur-xl flex flex-col justify-center items-center overflow-y-auto p-4 md:p-6 select-none text-white">
      
      {/* Visual neon light beams */}
      <div className="absolute top-[10%] left-1/4 w-[400px] h-[400px] rounded-full bg-[#7BC6FF]/5 blur-[150px] pointer-events-none" />
      <div className="absolute bottom-[10%] right-1/4 w-[400px] h-[400px] rounded-full bg-purple-500/5 blur-[150px] pointer-events-none" />

      <div className="w-full max-w-2xl bg-slate-900/40 border border-white/5 rounded-3xl p-6 md:p-10 relative z-10 space-y-8 backdrop-blur-md shadow-2xl overflow-hidden">
        
        {/* Top close header */}
        <div className="flex justify-between items-center pb-4 border-b border-white/5">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-[#7BC6FF] animate-pulse" />
            <h1 className="text-xl font-sans font-semibold tracking-tight text-[#F5F7FA]">Lumen Forge Workspace</h1>
          </div>
          <button
            id="btn-close-forge"
            onClick={onClose}
            className="p-2 border border-white/5 bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white rounded-full cursor-pointer transition-all"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* SHOW ONLY IF WE COMPLETED CORE FORGE */}
        {forgedResult ? (
          <div className="space-y-6 pt-2">
            
            <div className="text-center space-y-2">
              <span className="text-3xl">🌌</span>
              <h2 className="text-xl font-bold tracking-tight text-[#7BC6FF] uppercase">Synthesis Succeeded!</h2>
              <p className="text-xs text-slate-400">Your custom structured reading volume has been fully baked by Gemini.</p>
            </div>

            {/* Generated results review card */}
            <div 
              className="p-5 border border-[#7BC6FF]/20 bg-[#7BC6FF]/5 rounded-3xl flex gap-5 shadow-lg relative"
              style={{ boxShadow: 'inset 0 1px 1px 0 rgba(123, 198, 255, 0.1)' }}
            >
              <div className="w-20 h-28 bg-slate-950 border border-white/10 rounded-xl overflow-hidden shadow-sm flex-shrink-0 flex items-center justify-center text-4xl">
                📖
              </div>
              <div className="flex-1 space-y-1.5 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 bg-purple-400/20 text-purple-400 rounded-md font-mono text-[9px] uppercase font-bold">
                    {forgedResult.type}
                  </span>
                  <span className="text-xs font-mono text-slate-400">By {forgedResult.author}</span>
                </div>
                
                <h3 className="text-lg font-bold text-white truncate leading-tight">{forgedResult.title}</h3>
                <p className="text-xs text-slate-300 line-clamp-3 leading-relaxed">{forgedResult.description}</p>
                <p className="text-[10px] text-slate-500 font-mono pt-1">Genres: {forgedResult.genres.join(", ") || activeGenre}</p>
              </div>
            </div>

            {/* Actions: Save or Read */}
            <div className="flex flex-col sm:flex-row gap-4 pt-2">
              <button
                id="btn-forge-settle"
                onClick={() => onSettleBook(forgedResult)}
                className="flex-1 py-3 border border-[#7BC6FF]/35 bg-[#7BC6FF]/10 text-[#7BC6FF] rounded-2xl font-semibold text-sm hover:bg-[#7BC6FF]/15 transition-all cursor-pointer text-center"
              >
                Add to Shelves Archive
              </button>
              <button
                id="btn-forge-read-now"
                onClick={() => {
                  onSettleBook(forgedResult);
                  onLaunchReader(forgedResult);
                }}
                className="flex-1 py-3 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-sm hover:brightness-110 shadow-lg shadow-[#7BC6FF]/25 hover:scale-102 transition-all cursor-pointer text-center"
              >
                Begin Reading Instantly
              </button>
            </div>

          </div>
        ) : (
          /* CORE INPUT WORKSPACE */
          <div className="space-y-6">
            
            {/* Outline Topic Prompt textarea */}
            <div className="space-y-2">
              <span className="text-[10px] font-mono uppercase tracking-[0.15em] text-slate-400 pl-1 font-bold">Universe Vision Outline</span>
              <textarea
                id="forge-prompt-input"
                value={promptValue}
                onChange={(e) => setPromptValue(e.target.value)}
                rows={3}
                placeholder="Ex. A gothic lighthouse caretaker whose reflection lives 10 seconds ahead of him..."
                className="w-full px-5 py-4 bg-slate-950/60 border border-white/5 focus:border-[#7BC6FF]/35 rounded-2xl text-white placeholder-slate-500 font-sans leading-relaxed focus:outline-none focus:ring-1 focus:ring-[#7BC6FF]/20 backdrop-blur-md shadow-inner text-sm"
              />
            </div>

            {/* Quick Catalyst presets */}
            <div className="space-y-2">
              <span className="text-[10px] font-mono uppercase tracking-wider text-slate-500 pl-1">Inspiration Catalysts</span>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {inspirationCatalysts.map((cat, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      setPromptValue(cat.text);
                      setActiveType(cat.type);
                      setActiveGenre(cat.genre);
                    }}
                    className="p-3 bg-white/5 hover:bg-white/10 border border-white/5 rounded-2xl hover:border-[#7BC6FF]/20 text-left cursor-pointer duration-300 transition-all text-xs space-y-1 group"
                  >
                    <div className="flex justify-between items-center text-[9px] font-mono text-[#7BC6FF] font-semibold">
                      <span>{cat.type.toUpperCase()}</span>
                      <PenTool className="w-3 h-3 opacity-50 group-hover:scale-110" />
                    </div>
                    <p className="text-slate-300 leading-normal line-clamp-2">{cat.text}</p>
                  </button>
                ))}
              </div>
            </div>

            {/* Choice: Blueprint layout selection */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
              <div className="space-y-2">
                <span className="text-[10px] font-mono uppercase tracking-[0.15em] text-slate-400 pl-1 font-bold">Layout Blueprint blueprint</span>
                <div className="grid grid-cols-3 p-0.5 rounded-2xl bg-slate-950/60 border border-white/5">
                  {(['manga', 'novel', 'hybrid'] as const).map((type) => (
                    <button
                      key={type}
                      onClick={() => setActiveType(type)}
                      className={`py-2 rounded-xl text-xs uppercase font-mono tracking-wider font-semibold cursor-pointer transition-all ${
                        activeType === type 
                          ? 'bg-[#7BC6FF] text-slate-950 font-bold shadow-inner' 
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>

              {/* Choice: Genre matrices */}
              <div className="space-y-2">
                <span className="text-[10px] font-mono uppercase tracking-[0.15em] text-slate-400 pl-1 font-bold">Primary Genre context</span>
                <select
                  id="forge-genre-select"
                  value={activeGenre}
                  onChange={(e) => setActiveGenre(e.target.value)}
                  className="w-full bg-slate-950/60 text-slate-200 border border-white/5 focus:border-[#7BC6FF]/30 px-4 py-2.5 rounded-2xl font-mono text-xs focus:outline-none"
                >
                  <option className="bg-slate-950 text-white" value="Sci-Fi">Sci-Fi Cyberpunk</option>
                  <option className="bg-slate-950 text-white" value="Psychological">Psychological Thriller</option>
                  <option className="bg-slate-950 text-white" value="Existential">Existential Drama</option>
                  <option className="bg-slate-950 text-white" value="Fantasy">Magic High Fantasy</option>
                  <option className="bg-slate-950 text-white" value="Detective">Art Deco Detective</option>
                </select>
              </div>
            </div>

            {/* Interactive Alert messages */}
            {errorString && (
              <div className="p-3 bg-rose-950/15 border border-rose-500/25 rounded-2xl theme-error flex items-center gap-2.5 text-xs text-rose-400">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{errorString}</span>
              </div>
            )}

            {/* Catalyst Trigger button */}
            <button
              id="btn-forge-process"
              onClick={handleStartForge}
              className="w-full py-4.5 bg-[#7BC6FF] text-slate-950 font-bold rounded-2xl text-center flex items-center justify-center gap-2 shadow-[0_4px_28px_0_rgba(123,198,255,0.4)] hover:brightness-110 font-sans cursor-pointer active:scale-[0.99] transition-all text-sm uppercase tracking-wider"
            >
              <Dna className="w-5 h-5" />
              <span>Catalyze Synthesis</span>
            </button>

          </div>
        )}

      </div>

      {/* COMPLEX PROCESS LOADING SCREEN OVERLAY */}
      <AnimatePresence>
        {loadingState && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-[#0A0C0F]/95 backdrop-blur-xl z-50 flex flex-col justify-center items-center p-6 text-center space-y-8"
          >
            {/* Spinning liquid light torus reactor */}
            <div className="relative w-28 h-28 border border-white/5 rounded-full flex items-center justify-center">
              <div className="absolute inset-2 border border-dashed border-[#7BC6FF] rounded-full animate-spin" style={{ animationDuration: '4s' }} />
              <div className="absolute inset-4 border border-indigo-500 rounded-full animate-spin" style={{ animationDuration: '7s' }} />
              
              <Sparkles className="w-8 h-8 text-[#7BC6FF] animate-pulse" />
            </div>

            <div className="space-y-2 max-w-sm">
              <h3 className="text-sm uppercase font-mono tracking-[0.25em] text-[#7BC6FF] font-bold">Lumen Catalyst Active</h3>
              <p className="text-xl font-semibold text-white tracking-tight animate-pulse transition-all duration-500">
                {loadingSteps[loadingStepIndex]}
              </p>
              <p className="text-[10px] text-slate-500 font-mono pt-4 leading-normal">
                Gemini 3.5 is compiling dialogue geometry and drafting literary narrative panels. This usually settles in 15 seconds.
              </p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
