import { useState } from "react";
import {
  Type,
  User,
  Layers,
  ShieldCheck,
  RefreshCw,
  Download,
  CheckCircle2,
  AlertTriangle,
} from "lucide-react";
import { ReaderSettings } from "../types";
import {
  APP_VERSION,
  checkForUpdate,
  downloadReleaseApk,
  type UpdateStatus,
} from "../core/update-checker";

interface MoreViewProps {
  settings: ReaderSettings;
  onChangeSettings: (newSettings: Partial<ReaderSettings>) => void;
  userEmail?: string;
  onOpenSourceManager: () => void;
  activeSourcesCount?: number;
}

export default function MoreView({
  settings,
  onChangeSettings,
  userEmail,
  onOpenSourceManager,
  activeSourcesCount = 0,
}: MoreViewProps) {
  const [updateStatus, setUpdateStatus] = useState<UpdateStatus>({ state: "idle" });

  const toggleMotion = () => {
    onChangeSettings({ motionReduction: !settings.motionReduction });
  };

  const setLiquidIntensity = (val: number) => {
    onChangeSettings({ liquidIntensity: val });
  };

  const handleFontFamily = (font: "serif" | "sans" | "mono") => {
    onChangeSettings({ fontFamily: font });
  };

  const handleCheckUpdate = async () => {
    setUpdateStatus({ state: "checking" });
    const result = await checkForUpdate(APP_VERSION, true);
    setUpdateStatus(result);
  };

  const handleInstallUpdate = async () => {
    if (updateStatus.state !== "available") return;
    const release = updateStatus.release;
    setUpdateStatus({ state: "downloading", release, progress: 0, speedKBps: 0 });
    try {
      const blobUrl = await downloadReleaseApk(release, (progress, speedKBps) => {
        setUpdateStatus({ state: "downloading", release, progress, speedKBps });
      });
      setUpdateStatus({ state: "ready", release, blobUrl });
      // Trigger download for the user (web). Android will use PackageInstaller.
      const a = document.createElement("a");
      a.href = blobUrl;
      a.download = release.apkName || `lumen-${release.tag}.apk`;
      a.click();
    } catch (e: any) {
      setUpdateStatus({ state: "error", message: e?.message || String(e) });
    }
  };

  return (
    <div className="relative min-h-screen text-white bg-[#0A0C0F] pb-32 pt-12">
      <div className="absolute top-10 left-12 w-[350px] h-[350px] rounded-full bg-violet-500/5 blur-[120px] pointer-events-none" />

      <div className="max-w-2xl mx-auto px-6 space-y-8 relative z-10">
        <div className="space-y-1">
          <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">
            User Environment
          </span>
          <h1 className="text-3xl font-sans font-semibold tracking-tight">System Settings</h1>
        </div>

        {/* User tile */}
        <div
          className="p-5 rounded-3xl border border-white/5 bg-slate-900/40 backdrop-blur-md flex items-center gap-4 shadow-md"
          style={{ boxShadow: "inset 0 1px 1px 0 rgba(255,255,255,0.03)" }}
        >
          <div className="w-12 h-12 rounded-full bg-[#7BC6FF]/10 border border-[#7BC6FF]/35 flex items-center justify-center text-[#7BC6FF]">
            <User className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <p className="text-xs font-mono uppercase text-slate-500">Authenticated Session</p>
            <h3 className="font-semibold text-[#F5F7FA] font-sans text-sm tracking-wide">
              {userEmail || "guest@lumen.local"}
            </h3>
            <p className="text-[10px] text-[#7BC6FF] font-mono flex items-center gap-1 leading-none pt-0.5">
              <ShieldCheck className="w-3.5 h-3.5" /> Cloud sync via Supabase Lumen
            </p>
          </div>
        </div>

        {/* Updates — idempotent */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">
            App updates
          </h3>
          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-5 space-y-4">
            <div className="flex justify-between items-center gap-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Check for updates</span>
                <p className="text-xs text-slate-500">
                  Current v{APP_VERSION}. Idempotent — same release never re-installed.
                </p>
              </div>
              <button
                id="btn-check-updates"
                onClick={handleCheckUpdate}
                disabled={updateStatus.state === "checking" || updateStatus.state === "downloading"}
                className="px-3.5 py-1.5 bg-[#7BC6FF]/15 hover:bg-[#7BC6FF]/25 text-[#7BC6FF] font-mono border border-[#7BC6FF]/30 rounded-xl text-xs flex items-center gap-1.5 cursor-pointer transition-all disabled:opacity-50"
              >
                <RefreshCw
                  className={`w-3.5 h-3.5 ${
                    updateStatus.state === "checking" ? "animate-spin" : ""
                  }`}
                />
                <span>{updateStatus.state === "checking" ? "CHECKING…" : "CHECK"}</span>
              </button>
            </div>

            {updateStatus.state === "up_to_date" && (
              <div className="flex items-center gap-2 text-xs text-emerald-400 font-mono">
                <CheckCircle2 className="w-4 h-4" />
                Up to date ({updateStatus.latest})
              </div>
            )}

            {updateStatus.state === "available" && (
              <div className="space-y-3 p-3 rounded-2xl border border-[#7BC6FF]/20 bg-[#7BC6FF]/5">
                <div className="flex justify-between items-start gap-2">
                  <div>
                    <p className="text-sm font-semibold text-white">{updateStatus.release.name}</p>
                    <p className="text-[10px] font-mono text-slate-400">
                      {updateStatus.current} → {updateStatus.release.tag}
                    </p>
                  </div>
                  <button
                    onClick={handleInstallUpdate}
                    className="px-3 py-1.5 bg-[#7BC6FF] text-slate-950 rounded-xl text-xs font-mono font-bold flex items-center gap-1 cursor-pointer"
                  >
                    <Download className="w-3.5 h-3.5" />
                    INSTALL
                  </button>
                </div>
                {updateStatus.release.body && (
                  <p className="text-[11px] text-slate-400 line-clamp-4 whitespace-pre-wrap">
                    {updateStatus.release.body.slice(0, 400)}
                  </p>
                )}
              </div>
            )}

            {updateStatus.state === "downloading" && (
              <div className="space-y-2">
                <div className="h-2 rounded-full bg-white/10 overflow-hidden">
                  <div
                    className="h-full bg-[#7BC6FF] transition-all"
                    style={{ width: `${updateStatus.progress}%` }}
                  />
                </div>
                <p className="text-[10px] font-mono text-slate-400">
                  {updateStatus.progress}% · {updateStatus.speedKBps} KB/s · downloading APK…
                </p>
              </div>
            )}

            {updateStatus.state === "ready" && (
              <div className="flex items-center gap-2 text-xs text-emerald-400 font-mono">
                <CheckCircle2 className="w-4 h-4" />
                APK ready — install from downloads. Tag marked installed (idempotent).
              </div>
            )}

            {updateStatus.state === "error" && (
              <div className="flex items-center gap-2 text-xs text-rose-400 font-mono">
                <AlertTriangle className="w-4 h-4" />
                {updateStatus.message}
              </div>
            )}
          </div>
        </div>

        {/* Aesthetics */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">
            Aesthetics Engine
          </h3>
          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-5 space-y-6">
            <div className="flex justify-between items-center gap-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Device Canvas Theme</span>
                <p className="text-xs text-slate-500">Pick light reflection style</p>
              </div>
              <div className="flex p-0.5 rounded-2xl bg-slate-950/50 border border-white/10">
                {(["light", "dark", "amoled"] as const).map((mode) => {
                  const active = settings.themeMode === mode;
                  return (
                    <button
                      key={mode}
                      onClick={() => onChangeSettings({ themeMode: mode })}
                      className={`px-3.5 py-1.5 rounded-xl text-xs uppercase font-mono tracking-wider font-semibold cursor-pointer transition-all ${
                        active
                          ? "bg-[#7BC6FF] text-slate-950 shadow-inner"
                          : "text-slate-400 hover:text-white"
                      }`}
                    >
                      {mode === "amoled" ? "AMOLED" : mode}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="space-y-3 border-t border-white/5 pt-4">
              <div className="flex justify-between items-center">
                <div className="space-y-0.5">
                  <span className="text-sm font-semibold text-slate-200 flex items-center gap-1.5">
                    <Layers className="w-4 h-4 text-[#7BC6FF]" />
                    Liquid Backdrop Blur
                  </span>
                  <p className="text-xs text-slate-500">Adjust glass morphism depth</p>
                </div>
                <span className="text-xs font-mono text-slate-400 bg-white/5 border border-white/5 px-2 py-0.5 rounded-md font-semibold">
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

            <div className="flex justify-between items-center border-t border-white/5 pt-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Reduce Transition Motion</span>
                <p className="text-xs text-slate-500">Disables spring physics on list items</p>
              </div>
              <button
                id="toggle-motion"
                onClick={toggleMotion}
                className={`relative w-12 h-6 rounded-full transition-all duration-300 cursor-pointer flex items-center ${
                  settings.motionReduction ? "bg-[#7BC6FF]" : "bg-white/10"
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-slate-950 shadow-md transform transition-all duration-300 ${
                    settings.motionReduction ? "translate-x-[26px]" : "translate-x-[2px]"
                  }`}
                />
              </button>
            </div>
          </div>
        </div>

        {/* Reader defaults */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">
            Reader default core
          </h3>
          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-5 space-y-6">
            <div className="flex justify-between items-center gap-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200 flex items-center gap-1.5">
                  <Type className="w-4 h-4 text-[#7BC6FF]" />
                  Typography Font Face
                </span>
                <p className="text-xs text-slate-500">Default novel body typeface</p>
              </div>
              <div className="flex p-0.5 rounded-2xl bg-slate-950/50 border border-white/10">
                {(["serif", "sans", "mono"] as const).map((font) => {
                  const active = settings.fontFamily === font;
                  return (
                    <button
                      key={font}
                      onClick={() => handleFontFamily(font)}
                      className={`px-3 py-1.5 rounded-xl text-xs uppercase font-mono tracking-wider font-semibold cursor-pointer transition-all ${
                        active
                          ? "bg-[#7BC6FF] text-slate-950 shadow-inner"
                          : "text-slate-400 hover:text-white"
                      }`}
                    >
                      {font}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="space-y-3 border-t border-white/5 pt-4">
              <div className="flex justify-between items-center">
                <div className="space-y-0.5">
                  <span className="text-sm font-semibold text-slate-200">Default Text Size</span>
                  <p className="text-xs text-slate-500">Prose reading dimension ratio</p>
                </div>
                <span className="text-xs font-mono text-slate-400 bg-white/5 px-2 py-0.5 rounded-md">
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

            <div className="flex justify-between items-center border-t border-white/5 pt-4">
              <div className="space-y-0.5">
                <span className="text-sm font-semibold text-slate-200">Manga Adaptive Back-glow</span>
                <p className="text-xs text-slate-500">Projects dynamic page atmospheric lighting</p>
              </div>
              <button
                id="toggle-ambient-manga"
                onClick={() =>
                  onChangeSettings({ adaptiveAmbientBlur: !settings.adaptiveAmbientBlur })
                }
                className={`relative w-12 h-6 rounded-full transition-all duration-300 cursor-pointer flex items-center ${
                  settings.adaptiveAmbientBlur ? "bg-[#7BC6FF]" : "bg-white/10"
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-slate-950 shadow-md transform transition-all duration-300 ${
                    settings.adaptiveAmbientBlur ? "translate-x-[26px]" : "translate-x-[2px]"
                  }`}
                />
              </button>
            </div>
          </div>
        </div>

        {/* OS utilities */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-mono uppercase tracking-[0.2em] text-slate-500 font-bold pl-1">
            OS Utilities
          </h3>
          <div
            onClick={onOpenSourceManager}
            className="group p-5 rounded-3xl border border-white/5 hover:border-[#7BC6FF]/35 bg-gradient-to-tr from-slate-900/40 to-slate-950/40 backdrop-blur-md flex justify-between items-center transition-all cursor-pointer shadow-md"
          >
            <div className="space-y-1">
              <span className="text-sm font-semibold text-slate-200 group-hover:text-[#7BC6FF] transition-colors">
                Universal Ingestion Sources
              </span>
              <p className="text-xs text-slate-500">
                Keiyoushi + LNReader indexes · install sources on demand
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-1 bg-[#7BC6FF]/15 border border-[#7BC6FF]/25 text-[#7BC6FF] rounded-xl text-[10px] font-mono font-bold uppercase tracking-wider">
                {activeSourcesCount} Active
              </span>
              <span className="text-slate-400 group-hover:translate-x-1 duration-300 transition-transform font-mono font-bold">
                →
              </span>
            </div>
          </div>

          <div className="rounded-3xl border border-white/5 bg-slate-900/20 backdrop-blur-md p-4 flex justify-between items-center">
            <span className="text-xs font-mono text-slate-400">Force environment rebuild cache</span>
            <button
              onClick={() => location.reload()}
              className="px-3.5 py-1.5 bg-white/10 hover:bg-[#7BC6FF]/10 hover:text-[#7BC6FF] font-mono border border-white/5 rounded-xl text-xs flex items-center gap-1.5 cursor-pointer transition-all"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>RELOAD SHELL</span>
            </button>
          </div>
        </div>

        <div className="text-center pt-8 pb-10 space-y-1 z-10 relative">
          <p className="text-[10px] font-mono text-slate-600 uppercase tracking-[0.35em] font-semibold">
            LUMEN READING PLATFORM — v{APP_VERSION}
          </p>
          <p className="text-[11px] text-slate-500 italic font-medium font-sans opacity-70">
            Made by Glassline Studio
          </p>
        </div>
      </div>
    </div>
  );
}
