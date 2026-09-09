import React, { useState, useEffect, useRef, useMemo } from "react";
import { ChevronLeft, ChevronRight, SlidersHorizontal, Maximize2, Minimize2, Eye, AlignLeft, Sliders, Type, Columns, ArrowLeft, MoreHorizontal, HelpCircle } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { MediaItem, Chapter, ReaderSettings, MangaPanel, HybridElement } from "../types";

interface ReaderEngineProps {
  item: MediaItem;
  chapter: Chapter;
  settings: ReaderSettings;
  onChangeSettings: (settings: Partial<ReaderSettings>) => void;
  onClose: () => void;
  onSaveHistoryPercent: (mediaId: string, chapterId: string, progressPercent: number, elapsedMs: number) => void;
}

export default function ReaderEngine({ item, chapter, settings, onChangeSettings, onClose, onSaveHistoryPercent }: ReaderEngineProps) {
  const [showHUD, setShowHUD] = useState(true);
  const [activePanelIndex, setActivePanelIndex] = useState<number | null>(null);
  const [currentMangaPageIndex, setCurrentMangaPageIndex] = useState(0);
  const [isFullscreen, setIsfullscreen] = useState(false);
  const [showQuickSettings, setShowQuickSettings] = useState(false);
  const [activeParagraphIndex, setActiveParagraphIndex] = useState<number>(0);

  const startTimestampRef = useRef<number>(Date.now());
  const trackerRef = useRef<HTMLDivElement>(null);

  // Close HUD on timer
  useEffect(() => {
    if (!showHUD) return;
    const timeout = setTimeout(() => setShowHUD(false), 3500);
    return () => clearTimeout(timeout);
  }, [showHUD]);

  // Track scroll position to update read progress in vertical scroll modes
  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const target = e.currentTarget;
    const scrollPercent = Math.min(
      100,
      Math.round((target.scrollTop / (target.scrollHeight - target.clientHeight)) * 100)
    );
    
    // Save timer elapsed
    const elapsed = Date.now() - startTimestampRef.current;
    onSaveHistoryPercent(item.id, chapter.id, scrollPercent || 1, elapsed);
  };

  // Safe save progress in page-based operations
  const saveProgressPage = (index: number, total: number) => {
    const percent = Math.min(100, Math.round(((index + 1) / total) * 100));
    const elapsed = Date.now() - startTimestampRef.current;
    onSaveHistoryPercent(item.id, chapter.id, percent, elapsed);
  };

  // Exit full screen handler
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsfullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsfullscreen(false);
    }
  };

  // Extract panel coloring for ambient background blur back-projection
  const activeAmbientBg = useMemo(() => {
    if (!settings.adaptiveAmbientBlur) return "#0A0C0F";
    
    if (item.type === 'manga' && chapter.mangaPanels) {
      const activeIdx = activePanelIndex !== null ? activePanelIndex : 0;
      const currentPanel = chapter.mangaPanels[activeIdx || 0];
      return currentPanel?.atmosphericColor || "#1b263b";
    }
    
    if (item.type === 'hybrid' && chapter.hybridElements) {
      const activeIdx = Math.min(currentMangaPageIndex, chapter.hybridElements.length - 1);
      const elem = chapter.hybridElements[activeIdx];
      return elem?.atmosphericColor || "#0d1b2a";
    }

    if (settings.warmMode) return "#342211";

    return "#0A0C0F";
  }, [item, chapter, activePanelIndex, settings.adaptiveAmbientBlur, settings.warmMode, currentMangaPageIndex]);

  // Novel Paragraph focus splitter
  const novelParagraphs = useMemo(() => {
    if (!chapter.content) return [];
    return chapter.content.split("\n\n").filter(p => p.trim().length > 0);
  }, [chapter.content]);

  return (
    <div 
      id="master-reader-viewport"
      className="fixed inset-0 z-50 bg-[#0A0C0F] transition-all duration-700 ease-out flex flex-col overflow-hidden"
      style={{
        backgroundColor: activeAmbientBg,
        transition: 'background-color 1s ease'
      }}
    >
      
      {/* Immersive adaptive blur layer background */}
      {settings.adaptiveAmbientBlur && (
        <div 
          className="absolute inset-x-0 -top-40 h-[600px] blur-[150px] opacity-30 pointer-events-none mix-blend-screen transition-all duration-1000"
          style={{
            background: `radial-gradient(circle, ${activeAmbientBg} 0%, transparent 80%)`
          }}
        />
      )}

      {/* TOP HEAD UP DISPLAY MENU BAR (Hides automatically) */}
      <AnimatePresence>
        {showHUD && (
          <motion.div
            initial={{ y: -60, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -60, opacity: 0 }}
            className="absolute top-0 inset-x-0 h-16 bg-slate-950/45 dark:bg-black/45 backdrop-blur-xl border-b border-white/10 flex items-center justify-between px-6 z-40"
            style={{
              boxShadow: '0 8px 32px 0 rgba(0,0,0,0.25), inset 0 -1px 1px 0 rgba(255,255,255,0.05)'
            }}
          >
            <div className="flex items-center gap-3">
              <button
                id="btn-reader-exit"
                onClick={() => {
                  const elapsed = Date.now() - startTimestampRef.current;
                  onSaveHistoryPercent(item.id, chapter.id, 100, elapsed);
                  onClose();
                }}
                className="p-2 border border-white/10 hover:border-white/20 hover:bg-white/10 rounded-full cursor-pointer text-white/80 hover:text-white transition-all"
              >
                <ArrowLeft className="w-4 h-4" />
              </button>
              
              <div className="min-w-0">
                <span className="text-[10px] font-mono uppercase tracking-wider text-[#7BC6FF] font-semibold">
                  {item.type.toUpperCase()} READER
                </span>
                <h2 className="text-xs text-slate-300 font-sans truncate max-w-sm md:max-w-md font-medium">
                  {item.title} — {chapter.title}
                </h2>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                id="btn-reader-quick-settings"
                onClick={() => setShowQuickSettings(!showQuickSettings)}
                className={`p-2 border rounded-full cursor-pointer transition-all ${
                  showQuickSettings 
                    ? 'bg-[#7BC6FF] border-transparent text-slate-950' 
                    : 'border-white/10 hover:border-white/20 text-white/80 hover:bg-white/5'
                }`}
                title="Format Preferences"
              >
                <SlidersHorizontal className="w-4 h-4" />
              </button>

              <button
                onClick={toggleFullscreen}
                className="p-2 border border-white/10 hover:border-white/20 rounded-full cursor-pointer text-white/80 hover:text-white hover:bg-white/5 transition-all"
                title="Fullscreen Toggle"
              >
                {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* MID SECTION: DYNAMIC READ PANEL RENDER */}
      <div 
        id="reader-prose-container"
        ref={trackerRef}
        onScroll={handleScroll}
        onClick={() => setShowHUD(!showHUD)}
        className="flex-1 w-full overflow-y-auto overflow-x-hidden pt-16 pb-24 scroll-smooth"
      >
        
        {/* CASE 1: NOVEL READER ENGINE */}
        {item.type === 'novel' && (
          <div 
            className={`mx-auto px-6 py-8 transition-all duration-300 ${
              settings.columnWidth === 'narrow' ? 'max-w-xl' :
              settings.columnWidth === 'medium' ? 'max-w-3xl' :
              settings.columnWidth === 'wide' ? 'max-w-5xl' : 'max-w-7xl'
            }`}
            style={{
              fontFamily: settings.fontFamily === 'serif' ? 'Georgia, serif' : 
                          settings.fontFamily === 'mono' ? 'Courier, monospace' : 'system-ui, sans-serif'
            }}
          >
            {/* Novel Main title presentation card */}
            <div className="text-center py-10 space-y-4 max-w-lg mx-auto border-b border-white/5 mb-12">
              <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold block">CHAPTER {chapter.number}</span>
              <h1 className="text-3xl font-semibold tracking-tight leading-tight text-white mb-2">{chapter.title}</h1>
              <p className="text-xs text-slate-400 font-mono">By {item.author}</p>
              <div className="w-8 h-1 bg-[#7BC6FF] mx-auto rounded-full" />
            </div>

            {/* Paragraph lines block */}
            <div 
              className="space-y-6 text-[#D9E1EA] leading-relaxed select-text"
              style={{
                fontSize: `${settings.fontSize}%`,
                lineHeight: settings.lineHeight
              }}
            >
              {novelParagraphs.map((para, pIdx) => {
                const isFocused = activeParagraphIndex === pIdx;
                const cleansed = para.startsWith("### ") ? para.replace("### ", "") : para;
                const isHeader = para.startsWith("### ");

                return (
                  <p
                    id={`novel-para-${pIdx}`}
                    key={pIdx}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveParagraphIndex(pIdx);
                    }}
                    className={`transition-all duration-500 rounded-xl px-4 py-3 cursor-pointer ${
                      isHeader
                        ? "text-xl font-bold tracking-tight text-[#7BC6FF] border-l-2 border-[#7BC6FF] bg-[#7BC6FF]/5 mt-8 mb-4 font-sans"
                        : isFocused
                        ? "text-white bg-white/5 border border-white/10 font-medium scale-101"
                        : "opacity-60 hover:opacity-100"
                    }`}
                  >
                    {cleansed}
                  </p>
                );
              })}
            </div>

            {/* Completing node */}
            <div className="text-center py-14 max-w-md mx-auto space-y-3">
              <span className="text-2xl">✨</span>
              <h4 className="font-semibold text-slate-400 text-sm">Chapter Completed</h4>
              <p className="text-xs text-slate-500">Your reading index progress synced to user session cloud profile.</p>
            </div>
          </div>
        )}

        {/* CASE 2: MANGA READER ENGINE */}
        {item.type === 'manga' && chapter.mangaPanels && (
          <div className="max-w-3xl mx-auto px-6 py-6 space-y-12">
            
            {/* Header branding */}
            <div className="text-center space-y-2 py-4 mb-4 border-b border-white/5">
              <span className="text-xs uppercase font-mono tracking-widest text-[#7BC6FF]">MANGA PANEL FLOW</span>
              <h1 className="text-2xl font-bold tracking-tight text-[#F5F7FA]">{chapter.title}</h1>
              <p className="text-xs text-slate-500">Tap individual panels to capture visual focus and focus dialogues.</p>
            </div>

            {/* Manga Panels display */}
            <div className="space-y-12 select-none">
              {chapter.mangaPanels.map((panel, pIdx) => {
                const isSelected = activePanelIndex === pIdx;
                
                return (
                  <div
                    id={`manga-panel-${panel.id}`}
                    key={panel.id}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActivePanelIndex(isSelected ? null : pIdx);
                    }}
                    className={`relative overflow-hidden rounded-3xl border cursor-pointer group transition-all duration-500 ${
                      panel.layoutType === 'hero' ? 'min-h-[420px]' :
                      panel.layoutType === 'cinematic' ? 'min-h-[300px]' : 'min-h-[360px]'
                    } ${
                      isSelected 
                        ? 'border-[#7BC6FF] scale-102 ring-1 ring-[#7BC6FF]/20 shadow-[0_0_32px_0_rgba(123,198,255,0.25)]' 
                        : 'border-white/10 hover:border-white/25 bg-[#000]/65 hover:bg-[#000]/45 shadow-lg'
                    }`}
                  >
                    
                    {/* Visual sketch simulation / Cinematic graphic overlay */}
                    <div className="absolute inset-0 bg-gradient-to-tr from-slate-950/95 via-transparent to-slate-950/80 pointer-events-none z-10" />
                    
                    {/* Artistic atmosphere light source indicator inside sketch outline */}
                    <div className="absolute inset-x-0 bottom-0 h-48 bg-gradient-to-t pointer-events-none z-0" 
                      style={{
                        backgroundImage: `linear-gradient(to top, ${panel.atmosphericColor}aa 0%, transparent 100%)`
                      }}
                    />

                    {/* Highly polished active-wireframe graphic mockup representer with details to avoid blank space */}
                    <div className="absolute inset-0 z-0 flex flex-col justify-center items-center p-8 opacity-40 group-hover:opacity-60 transition-opacity">
                      
                      {/* Technical drawing alignment markers */}
                      <span className="absolute top-4 left-4 font-mono text-[8px] text-slate-500 tracking-widest uppercase">PANEL {pIdx + 1} // {panel.layoutType.toUpperCase()}</span>
                      <span className="absolute bottom-4 right-4 font-mono text-[8px] text-slate-500 tracking-widest">LUMEN CORE v3.5</span>
                      
                      {/* Central dramatic icon sketch */}
                      <div className="relative w-28 h-28 border border-white/5 rounded-full flex items-center justify-center bg-slate-950/40 backdrop-blur-sm shadow-[inset_0_1px_6px_0_rgba(255,255,255,0.1)]">
                        <Maximize2 className="w-8 h-8 text-[#7BC6FF] opacity-50 animate-pulse" />
                      </div>

                      {/* Descriptive layout summary (visual art direction concept) */}
                      <p className="text-xs text-slate-300 font-sans tracking-tight text-center max-w-md mt-6 italic bg-slate-950/85 px-4 py-2 border border-white/5 rounded-xl">
                        Visual Direction: &quot;{panel.visualConcept}&quot;
                      </p>
                    </div>

                    {/* NARRATIVE CAPTION (Boxed captions on margins) */}
                    {panel.narrativeText && (
                      <div className="absolute top-4 left-4 z-20 max-w-sm px-4 py-2.5 bg-slate-950 border border-white/10 rounded-xl shadow-md">
                        <p className="text-[11px] font-sans text-slate-300 uppercase font-bold tracking-wider leading-relaxed">
                          {panel.narrativeText}
                        </p>
                      </div>
                    )}

                    {/* SPEECH BUBBAL DIALOGUES */}
                    {panel.dialogues.map((dlg, dIdx) => (
                      <div
                        key={dIdx}
                        className="absolute z-20 group"
                        style={{
                          left: `${dlg.positionX}%`,
                          top: `${dlg.positionY}%`
                        }}
                      >
                        {/* Glass dialogue bubble */}
                        <div 
                          className="px-4 py-2 bg-white text-slate-950 rounded-2xl shadow-xl max-w-[170px] border border-white/20 select-text transform group-hover:scale-103 duration-300 relative"
                          style={{
                            boxShadow: '0 8px 16px rgba(0,0,0,0.3)',
                            borderRadius: dIdx % 2 === 0 ? '16px 16px 16px 2px' : '16px 16px 2px 16px'
                          }}
                        >
                          <p className="text-[9px] font-bold text-[#7BC6FF] uppercase font-sans mb-0.5 tracking-wide">{dlg.character}</p>
                          <p className="text-xs font-medium leading-tight font-sans tracking-tight text-slate-900">{dlg.text}</p>
                          
                          {/* Triangle indicator tail */}
                          <div 
                            className={`absolute -bottom-1 w-2.5 h-2.5 bg-white transform rotate-45 ${
                              dIdx % 2 === 0 ? 'left-3' : 'right-3'
                            }`} 
                          />
                        </div>
                      </div>
                    ))}

                  </div>
                );
              })}
            </div>

            {/* Completing node */}
            <div className="text-center py-10 max-w-sm mx-auto space-y-3">
              <span className="text-2xl">⚡</span>
              <h4 className="font-semibold text-slate-400 text-sm">Fin de Volume</h4>
              <p className="text-xs text-slate-500">Manga scroll tracking logged to platform timeline.</p>
            </div>
          </div>
        )}

        {/* CASE 3: HYBRID READER ENGINE */}
        {item.type === 'hybrid' && chapter.hybridElements && (
          <div className="max-w-4xl mx-auto px-6 py-8 space-y-12">
            
            {/* Introductory screen */}
            <div className="text-center space-y-3 py-10 max-w-sm mx-auto mb-10 border-b border-white/5">
              <span className="px-2.5 py-1 bg-[#7BC6FF]/10 text-[#7BC6FF] border border-[#7BC6FF]/30 text-[10px] font-mono tracking-widest rounded-md uppercase font-semibold">Cinematic Hybrid Expedition</span>
              <h1 className="text-3xl font-semibold tracking-tight leading-tight text-white">{chapter.title}</h1>
              <p className="text-xs text-slate-400">Integrated prose, multimedia overlays, and directional splashes.</p>
            </div>

            {/* Hybrid elements listing */}
            <div className="space-y-12">
              {chapter.hybridElements.map((elem, eIdx) => {
                
                return (
                  <div
                    id={`hybrid-elem-${elem.id}`}
                    key={elem.id}
                    className="space-y-4"
                  >
                    {/* Element Type 1: Text prose */}
                    {elem.type === 'text' && (
                      <div className="p-3 bg-white/5 border border-white/10 rounded-2xl hover:bg-white/10 duration-300">
                        <p className="text-base text-[#D9E1EA] leading-relaxed select-text font-serif p-3">
                          {elem.content}
                        </p>
                      </div>
                    )}

                    {/* Element Type 2: Cinematic Splash block */}
                    {elem.type === 'cinematic-splash' && (
                      <div 
                        className="relative h-[250px] md:h-[350px] w-full rounded-3xl overflow-hidden border border-white/10 flex flex-col justify-end p-6 md:p-10"
                        style={{
                          backgroundImage: `linear-gradient(to top, ${elem.atmosphericColor || "#000"}ee 30%, transparent 100%)`
                        }}
                      >
                        {/* Cinematic alignment indicators */}
                        <div className="absolute top-4 right-4 font-mono text-[8px] uppercase text-slate-400 tracking-wider">Cinematic Sequence // Splash</div>
                        
                        <div className="relative z-10 max-w-xl space-y-2">
                          <span className="px-2 py-0.5 bg-[#7BC6FF]/15 text-[#7BC6FF] text-[9px] font-mono rounded font-semibold uppercase font-mono">Splash view Direction</span>
                          <p className="text-sm font-semibold tracking-tight text-slate-100 italic bg-black/45 p-3 rounded-xl border border-white/5 backdrop-blur-sm">
                            &quot;{elem.content}&quot;
                          </p>
                          {elem.assetPrompt && (
                            <p className="text-[9px] text-slate-500 font-mono">Render reference: {elem.assetPrompt}</p>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Element Type 3: Image Spread block */}
                    {elem.type === 'image-spread' && (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center bg-white/5 p-6 rounded-3xl border border-white/5">
                        <div className="space-y-4">
                          <span className="px-2.5 py-0.5 bg-indigo-500/10 text-indigo-400 text-[9px] uppercase font-mono font-semibold rounded font-mono">Visual Spread Companion</span>
                          <p className="text-sm font-medium leading-relaxed text-slate-200">
                            &quot;{elem.content}&quot;
                          </p>
                          {elem.assetPrompt && (
                            <p className="text-[9px] text-slate-500 font-mono bg-slate-950/40 p-2 rounded-lg border border-white/5">Art Guide: {elem.assetPrompt}</p>
                          )}
                        </div>
                        <div className="h-44 rounded-2xl bg-neutral-950 border border-white/5 shadow-inner flex flex-col items-center justify-center p-4 text-center text-xs text-slate-500">
                          <Eye className="w-6 h-6 text-[#7BC6FF]/40 mb-2 animate-bounce" />
                          <p className="font-semibold font-mono text-[10px]">CINEMATIC COMPOSER</p>
                          <p className="text-[9px] text-slate-500 max-w-xs mt-1">Refracting illustration frame matches paragraph ambient settings. {elem.atmosphericColor}</p>
                        </div>
                      </div>
                    )}

                    {/* Element Type 4: Narrative Overlay lines */}
                    {elem.type === 'overlay-narration' && (
                      <div className="py-6 border-y border-white/5 my-8 text-center max-w-xl mx-auto">
                        <p className="text-xs uppercase font-mono tracking-widest text-[#7BC6FF] font-semibold mb-2">Narrative Chorus</p>
                        <p className="text-lg font-serif italic text-slate-200 leading-relaxed font-semibold">
                          &quot;{elem.content}&quot;
                        </p>
                      </div>
                    )}

                  </div>
                );
              })}
            </div>

            {/* Completing node */}
            <div className="text-center py-10 max-w-sm mx-auto space-y-3">
              <span className="text-2xl">🌌</span>
              <h4 className="font-semibold text-slate-400 text-sm">Sequence Terminated</h4>
              <p className="text-xs text-slate-500">Hybrid segment duration synced to cloud telemetry.</p>
            </div>
          </div>
        )}

      </div>

      {/* FLOAT HUD QUICK PREFS MENU Overlays panels */}
      <AnimatePresence>
        {showQuickSettings && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 30 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 30 }}
            className="absolute bottom-6 right-6 z-40 w-full max-w-sm rounded-3xl border border-white/10 bg-slate-950/85 backdrop-blur-xl p-5 shadow-2xl space-y-5 select-none"
            style={{
              boxShadow: '0 20px 40px rgba(0,0,0,0.5), inset 0 1px 1.5px 0 rgba(255,255,255,0.1)'
            }}
          >
            <div className="flex justify-between items-center pb-2 border-b border-white/5">
              <span className="text-xs font-mono uppercase tracking-wider text-slate-400 flex items-center gap-1.5 font-bold">
                <Sliders className="w-3.5 h-3.5 text-[#7BC6FF]" /> Format Preferences
              </span>
              <button 
                onClick={() => setShowQuickSettings(false)}
                className="text-[10px] font-mono text-[#7BC6FF] hover:text-white cursor-pointer px-2 py-0.5"
              >
                DONE
              </button>
            </div>

            {/* Preferences sliders depending on types */}
            <div className="space-y-4">
              
              {/* Common: Warm reading theme toggle */}
              <div className="flex justify-between items-center">
                <span className="text-xs text-slate-300 font-medium">Warm contrast glow parchment</span>
                <button
                  id="reader-toggle-warm"
                  onClick={() => onChangeSettings({ warmMode: !settings.warmMode })}
                  className={`relative w-11 h-5.5 rounded-full pointer-events-auto cursor-pointer transition-colors duration-200 flex items-center ${
                    settings.warmMode ? 'bg-amber-400' : 'bg-white/10'
                  }`}
                >
                  <div 
                    className={`w-4.5 h-4.5 rounded-full bg-slate-950 transform transition-transform duration-200 ${
                      settings.warmMode ? 'translate-x-[22px]' : 'translate-x-[2px]'
                    }`}
                  />
                </button>
              </div>

              {/* Unique stats based on reader type */}
              {item.type === 'novel' ? (
                <>
                  {/* Column widths */}
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-300 font-medium">Layout Grid Column Width</span>
                    <div className="flex p-0.5 rounded-xl bg-slate-900 border border-white/5">
                      {(['narrow', 'medium', 'wide'] as const).map((width) => (
                        <button
                          key={width}
                          onClick={() => onChangeSettings({ columnWidth: width })}
                          className={`px-2.5 py-1 text-[10px] rounded-lg tracking-normal font-mono uppercase ${
                            settings.columnWidth === width ? 'bg-[#7BC6FF] text-slate-950 font-bold' : 'text-slate-400'
                          }`}
                        >
                          {width.slice(0, 3)}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Fonts selector */}
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-300 font-medium font-sans">Font family typeface</span>
                    <div className="flex p-0.5 rounded-xl bg-slate-900 border border-white/5">
                      {(['serif', 'sans', 'mono'] as const).map((font) => (
                        <button
                          key={font}
                          onClick={() => onChangeSettings({ fontFamily: font })}
                          className={`px-2 py-1 text-[10px] rounded-lg font-mono uppercase ${
                            settings.fontFamily === font ? 'bg-[#7BC6FF] text-slate-950 font-bold' : 'text-slate-400'
                          }`}
                        >
                          {font}
                        </button>
                      ))}
                    </div>
                  </div>
                </>
              ) : (
                <>
                  {/* Manga adaptive back-glow toggle */}
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-300 font-medium">Adaptive Background projection glow</span>
                    <button
                      id="reader-toggle-ambient"
                      onClick={() => onChangeSettings({ adaptiveAmbientBlur: !settings.adaptiveAmbientBlur })}
                      className={`relative w-11 h-5.5 rounded-full pointer-events-auto cursor-pointer transition-colors duration-200 flex items-center ${
                        settings.adaptiveAmbientBlur ? 'bg-[#7BC6FF]' : 'bg-white/10'
                      }`}
                    >
                      <div 
                        className={`w-4.5 h-4.5 rounded-full bg-slate-950 transform transition-transform duration-200 ${
                          settings.adaptiveAmbientBlur ? 'translate-x-[22px]' : 'translate-x-[2px]'
                        }`}
                      />
                    </button>
                  </div>
                </>
              )}

              {/* FontSize Scaler */}
              <div className="space-y-2 pt-2 border-t border-white/5">
                <div className="flex justify-between text-[11px] text-slate-400">
                  <span>Prose FontSize ratio</span>
                  <span>{settings.fontSize}%</span>
                </div>
                <input
                  id="reader-slider-font"
                  type="range"
                  min="80"
                  max="180"
                  step="5"
                  value={settings.fontSize}
                  onChange={(e) => onChangeSettings({ fontSize: Number(e.target.value) })}
                  className="w-full h-1 bg-white/10 rounded-lg appearance-none cursor-pointer accent-[#7BC6FF]"
                />
              </div>

            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* FOOTER INFORMATION HUD (Displays progress & close option) */}
      <div className="absolute bottom-0 inset-x-0 h-10 bg-slate-950/20 backdrop-blur-sm border-t border-white/5 px-6 flex justify-between items-center text-[10px] font-mono text-slate-500 z-30">
        <span>Chapter {chapter.number} Content Engine // ACTIVE</span>
        <span>LUMEN OS v3.5</span>
      </div>

    </div>
  );
}
