import { Sliders, Sun, Moon, Sparkles, Tv, Type, EyeOff, User, Layers, ShieldCheck, RefreshCw } from "lucide-react";
import { motion } from "motion/react";
import { ReaderSettings } from "../types";

interface MoreViewProps {
  settings: ReaderSettings;
  onChangeSettings: (newSettings: Partial<ReaderSettings>) => void;
  userEmail?: string;
  onOpenSourceManager: () => void;
  activeSourcesCount?: number;
}

export default function MoreView({ settings, onChangeSettings, userEmail, onOpenSourceManager, activeSourcesCount = 5 }: MoreViewProps) {
  
  const toggleAMOLED = () => {
    onChangeSettings({ themeMode: settings.themeMode === 'amoled' ? 'dark' : 'amoled' });
  };

  const toggleMotion = () => {
    onChangeSettings({ motionReduction: !settings.motionReduction });
  };

  const setLiquidIntensity = (val: number) => {
    onChangeSettings({ liquidIntensity: val });
  };

  const handleFontFamily = (font: 'serif' | 'sans' | 'mono') => {
    onChangeSettings({ fontFamily: font });
  };

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      
      {/* Background neon dust highlight */}
      <div className="absolute top-10 left-12 w-[350px] h-[350px] rounded-full bg-violet-500/5 blur-[120px] pointer-events-none" />

      <div className="max-w-2xl mx-auto px-6 space-y-8 relative z-10">
        
        {/* Title metadata */}
        <div className="space-y-1">
          <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">User Environment</span>
          <h1 className="text-3xl font-sans font-semibold tracking-tight">System Settings</h1>
        </div>

        {/* SECTION 1: USER CONSOLE TILE */}
        <div 
          className="p-5 rounded-3xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex items-center gap-4 shadow-md"
          style={{ boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.03)' }}
        >
          <div className="w-12 h-12 rounded-full bg-[#7BC6FF]/10 border border-[#7BC6FF]/35 flex items-center justify-center text-[#7BC6FF] text-lg font-bold">
            <User className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <p className="text-xs font-mono uppercase text-slate-500">Authenticated Session</p>
            <h3 className="font-semibold text-[#F5F7FA] font-sans text-sm tracking-wide">
              {userEmail || "benjaminchume@gmail.com"}
            </h3>
            <p className="text-[10px] text-[#7BC6FF] font-mono flex items-center gap-1 leading-none pt-0.5">
              <ShieldCheck className="w-3.5 h-3.5" /> Synchronized with Cloud Sync Engine
            </p>
          </div>
        </div>

        {/* SECTION 2: GRAPHICS & THEME */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">Aesthetics Engine</h3>
          
          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-5 space-y-6">
            
            {/* Theme picker tactile */}
            <div className="flex justify-between items-center gap-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Device Canvas Theme</span>
                <p className="text-xs text-slate-500">Pick light reflection style</p>
              </div>

              <div className="flex p-0.5 rounded-2xl bg-slate-950/50 border border-white/10">
                {(['light', 'dark', 'amoled'] as const).map((mode) => {
                  const active = settings.themeMode === mode;
                  return (
                    <button
                      key={mode}
                      onClick={() => onChangeSettings({ themeMode: mode })}
                      className={`px-3.5 py-1.5 rounded-xl text-xs uppercase font-mono tracking-wider font-semibold cursor-pointer transition-all ${
                        active 
                          ? 'bg-[#7BC6FF] text-slate-950 shadow-inner' 
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {mode === 'amoled' ? 'AMOLED' : mode}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Liquid glass slide bar */}
            <div className="space-y-3 border-t border-white/5 pt-4">
              <div className="flex justify-between items-center">
                <div className="space-y-0.5">
                  <span className="text-sm font-semibold text-slate-200 flex items-center gap-1.5">
                    <Layers className="w-4 h-4 text-[#7BC6FF]" />
                    Liquid Backdrop Blur
                  </span>
                  <p className="text-xs text-slate-500">Adjust glass morphism depth</p>
                </div>
                <span className="text-xs font-mono text-slate-400 bg-white/5 border border-white/5 px-2 py-0.5 rounded-md font-semibold font-mono">
                  {settings.liquidIntensity}px
                </span>
              </div>
              <input
                id="slider-liquid-intensity"
                type="range"
                min="0"
                max="40"
                value={settings.liquidIntensity}
                onChange={(e) => setLiquidIntensity(Number(e.target.value))}
                className="w-full accent-[#7BC6FF] h-1.5 bg-white/10 rounded-lg appearance-none cursor-pointer outline-none"
              />
            </div>

            {/* Motion Reduction Sliding Switch */}
            <div className="flex justify-between items-center border-t border-white/5 pt-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Reduce Transition Motion</span>
                <p className="text-xs text-slate-500">Disables spring physics on list items</p>
              </div>

              <button
                id="toggle-motion"
                onClick={toggleMotion}
                className={`relative w-12 h-6 rounded-full transition-all duration-300 pointer-events-auto cursor-pointer flex items-center ${
                  settings.motionReduction ? 'bg-[#7BC6FF]' : 'bg-white/10'
                }`}
              >
                <div 
                  className={`w-5 h-5 rounded-full bg-slate-950 shadow-md transform transition-all duration-300 ${
                    settings.motionReduction ? 'translate-x-[26px]' : 'translate-x-[2px]'
                  }`}
                />
              </button>
            </div>

          </div>
        </div>

        {/* SECTION 3: READER CONFIGURATION */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">Reader default core</h3>
          
          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-5 space-y-6">
            
            {/* Font settings */}
            <div className="flex justify-between items-center gap-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200 flex items-center gap-1.5">
                  <Type className="w-4 h-4 text-[#7BC6FF]" />
                  Typography Font Face
                </span>
                <p className="text-xs text-slate-500">Default novel body typeface</p>
              </div>

              <div className="flex p-0.5 rounded-2xl bg-slate-950/50 border border-white/10">
                {(['serif', 'sans', 'mono'] as const).map((font) => {
                  const active = settings.fontFamily === font;
                  return (
                    <button
                      key={font}
                      onClick={() => handleFontFamily(font)}
                      className={`px-3 py-1.5 rounded-xl text-xs uppercase font-mono tracking-wider font-semibold cursor-pointer transition-all ${
                        active 
                          ? 'bg-[#7BC6FF] text-slate-950 shadow-inner' 
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {font}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Slider: Font Scaler */}
            <div className="space-y-3 border-t border-white/5 pt-4">
              <div className="flex justify-between items-center">
                <div className="space-y-0.5">
                  <span className="text-sm font-semibold text-slate-200">Default Text Size</span>
                  <p className="text-xs text-slate-500">Prose reading dimension ratio</p>
                </div>
                <span className="text-xs font-mono text-slate-400 bg-white/5 px-2 py-0.5 rounded-md font-mono">
                  {settings.fontSize}%
                </span>
              </div>
              <input
                id="slider-font-size"
                type="range"
                min="80"
                max="180"
                step="5"
                value={settings.fontSize}
                onChange={(e) => onChangeSettings({ fontSize: Number(e.target.value) })}
                className="w-full accent-[#7BC6FF] h-1.5 bg-white/10 rounded-lg appearance-none cursor-pointer outline-none"
              />
            </div>

            {/* Adaptive background blur toggle */}
            <div className="flex justify-between items-center border-t border-white/5 pt-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Manga Adaptive Back-glow</span>
                <p className="text-xs text-slate-500">Projects dynamic page atmospheric lighting</p>
              </div>

              <button
                id="toggle-ambient-manga"
                onClick={() => onChangeSettings({ adaptiveAmbientBlur: !settings.adaptiveAmbientBlur })}
                className={`relative w-12 h-6 rounded-full transition-all duration-300 pointer-events-auto cursor-pointer flex items-center ${
                  settings.adaptiveAmbientBlur ? 'bg-[#7BC6FF]' : 'bg-white/10'
                }`}
              >
                <div 
                  className={`w-5 h-5 rounded-full bg-slate-950 shadow-md transform transition-all duration-300 ${
                    settings.adaptiveAmbientBlur ? 'translate-x-[26px]' : 'translate-x-[2px]'
                  }`}
                />
              </button>
            </div>

          </div>
        </div>

        {/* SOLID RECOVERY PLATFORM TOOLS */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">OS Utilities</h3>
          
          {/* Universal Source Manager Action entry */}
          <div 
            onClick={onOpenSourceManager}
            className="group p-5 rounded-3xl border border-white/5 hover:border-[#7BC6FF]/35 bg-gradient-to-tr from-slate-900/40 to-slate-950/40 backdrop-blur-md flex justify-between items-center transition-all cursor-pointer shadow-md"
            style={{ boxShadow: 'inset 0 1px 1.5px 0 rgba(255,255,255,0.03)' }}
          >
            <div className="space-y-1">
              <span className="text-sm font-semibold text-slate-200 group-hover:text-[#7BC6FF] transition-colors">Universal Ingestion Sources</span>
              <p className="text-xs text-slate-500">Enable, check latency, or sandbox repository extensions</p>
            </div>
            
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-1 bg-[#7BC6FF]/15 border border-[#7BC6FF]/25 text-[#7BC6FF] rounded-xl text-[10px] font-mono font-bold uppercase tracking-wider">
                {activeSourcesCount} Active
              </span>
              <span className="text-slate-400 group-hover:translate-x-1 duration-300 transition-transform font-mono font-bold">&rarr;</span>
            </div>
          </div>

          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-4 flex justify-between items-center">
            <span className="text-xs font-mono text-slate-400 font-normal">Force environment rebuild cache</span>
            <button
              onClick={() => location.reload()}
              className="px-3.5 py-1.5 bg-white/10 hover:bg-[#7BC6FF]/10 hover:text-[#7BC6FF] font-mono border border-white/5 rounded-xl text-xs flex items-center gap-1.5 cursor-pointer transition-all"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>RELOAD SHELL</span>
            </button>
          </div>
        </div>

        {/* MINIMAL FOOTER LABEL */}
        <div className="text-center pt-8 pb-10 space-y-1 z-10 relative">
          <p className="text-[10px] font-mono text-slate-600 uppercase tracking-[0.35em] font-semibold">LUMEN READING PLATFORM — v3.5.0</p>
          <p className="text-[11px] text-slate-500 italic font-medium font-sans opacity-70">
            Made by Glassline Studio
          </p>
        </div>

      </div>
    </div>
  );
}
