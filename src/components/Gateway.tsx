import React, { useState, useMemo } from "react";
import { 
  Sparkles, BookOpen, Layers, Cpu, ShieldCheck, Mail, ArrowRight, Check,
  BookMarked, HelpCircle, AlertCircle, RefreshCw, KeyRound, Star, Hourglass
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface GatewayProps {
  onComplete: (session: { name: string, email: string, isGuest: boolean }, genres: string[]) => void;
}

export default function Gateway({ onComplete }: GatewayProps) {
  const [step, setStep] = useState<"splash" | "onboarding" | "auth" | "genres">("splash");
  const [onboardIndex, setOnboardIndex] = useState(0);
  
  // Auth Form states
  const [authMethod, setAuthMethod] = useState<"guest" | "email">("guest");
  const [emailInput, setEmailInput] = useState("");
  const [otpInput, setOtpInput] = useState("");
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [authError, setAuthError] = useState("");

  // Genre interests choices state
  const [selectedGenres, setSelectedGenres] = useState<string[]>([]);

  const onboardingSlides = [
    {
      title: "Tactile Liquid Morphism",
      desc: "Immerse in a communication-tier interface featuring organic spring physics, backdrop refractions, and real-time environment back-lighting.",
      icon: Layers,
      highlight: "#7BC6FF"
    },
    {
      title: "Unified Hybrid Ingestion",
      desc: "Seamlessly cross-read Manga and Light Novel configurations under unified library mappings, and jump adaptations with shared progress.",
      icon: BookOpen,
      highlight: "#a29bfe"
    },
    {
      title: "Lumen Forge Synthesis",
      desc: "Co-author original sagas dynamically using multi-chapter Gemini AI proxy orchestration. Dictate theme concepts on the fly.",
      icon: Sparkles,
      highlight: "#ffeaa7"
    }
  ];

  const genresPool = [
    "Existential", "Absurdist", "Classic Literature", "Psychological", 
    "Sci-Fi", "Manga Adapt", "Dystopian", "Adventure", "Cosmic", 
    "Hybrid Literary", "Arthouse", "Fantasy", "Philosophical"
  ];

  const handleToggleGenre = (genre: string) => {
    if (selectedGenres.includes(genre)) {
      setSelectedGenres(prev => prev.filter(g => g !== genre));
    } else {
      setSelectedGenres(prev => [...prev, genre]);
    }
  };

  const handleSendOTP = (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailInput || !emailInput.includes("@")) {
      setAuthError("Please input an operational email sequence.");
      return;
    }
    setAuthError("");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setIsOtpSent(true);
    }, 1200);
  };

  const handleVerifyOTP = (e: React.FormEvent) => {
    e.preventDefault();
    if (otpInput.length < 4) {
      setAuthError("OTP contains a 4-digit security code matrix.");
      return;
    }
    setAuthError("");
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setStep("genres");
    }, 1100);
  };

  const skipToAuth = () => setStep("auth");

  const handleGuestEntry = () => {
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setStep("genres");
    }, 1000);
  };

  const handleCompleteGateway = () => {
    if (selectedGenres.length < 2) {
      return;
    }
    const fakeSession = {
      name: emailInput ? emailInput.split("@")[0] : "Guest Pilot",
      email: emailInput || "guest@lumen.os",
      isGuest: !emailInput
    };
    onComplete(fakeSession, selectedGenres);
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0C0F] text-white flex items-center justify-center p-6 overflow-hidden select-none">
      
      {/* Decorative environment layout spotlights */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] aspect-square rounded-full bg-sky-500/5 blur-[140px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] aspect-square rounded-full bg-violet-500/5 blur-[140px] pointer-events-none" />

      <AnimatePresence mode="wait">
        
        {/* PHASE 1: SPLASH SCREEN OVERLAY */}
        {step === "splash" && (
          <motion.div
            key="splash"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col items-center justify-center text-center space-y-10 max-w-md w-full"
          >
            <div className="space-y-4">
              <motion.div 
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ duration: 1.2, ease: "easeOut" }}
                className="w-24 h-24 border border-white/15 bg-slate-900/60 rounded-full flex items-center justify-center shadow-2xl relative"
                style={{
                  boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.1), 0 24px 48px rgba(0,0,0,0.5)'
                }}
              >
                {/* Soft refraction aura */}
                <div className="absolute inset-0 bg-gradient-to-tr from-[#7BC6FF]/0 via-[#7BC6FF]/10 to-[#7BC6FF]/0 pointer-events-none rounded-full animate-pulse" />
                <Layers className="w-10 h-10 text-[#7BC6FF] animate-bounce" style={{ animationDuration: '3s' }} />
              </motion.div>

              <div className="space-y-2">
                <h1 className="text-4xl font-sans font-bold tracking-[0.22em] text-[#F5F7FA] uppercase pl-4">
                  Lumen
                </h1>
                <p className="text-[10px] uppercase font-mono tracking-[0.45em] text-[#7BC6FF] font-semibold">
                  Reading OS Matrix
                </p>
              </div>
            </div>

            <div className="space-y-4 w-full pt-6">
              <button
                id="btn-splash-awaken"
                onClick={() => setStep("onboarding")}
                className="w-full py-4 bg-white/5 hover:bg-white/10 border border-white/10 hover:border-[#7BC6FF]/35 rounded-2xl flex items-center justify-center gap-3 group transition-all duration-300 shadow-xl cursor-pointer font-sans"
                style={{
                  boxShadow: 'inset 0 1px 1px 0 rgba(255,255,255,0.05)'
                }}
              >
                <span className="font-semibold text-sm tracking-widest text-[#F5F7FA] group-hover:text-[#7BC6FF] transition-colors">
                  INITIALIZE ODYSSEY
                </span>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-[#7BC6FF] group-hover:translate-x-1.5 duration-300 transition-all font-bold" />
              </button>
              
              <div className="font-mono text-[9px] text-slate-500 tracking-wider">
                CORE SYSTEM ADAPTER v3.5.0 // READY
              </div>
            </div>
          </motion.div>
        )}

        {/* PHASE 2: ONBOARDING CAROUSEL */}
        {step === "onboarding" && (
          <motion.div
            key="onboarding"
            initial={{ opacity: 0, x: 100 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -100 }}
            transition={{ type: "spring", stiffness: 350, damping: 28 }}
            className="w-full max-w-md bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-8 shadow-2xl relative backdrop-blur-md"
            style={{
              boxShadow: 'inset 0 1px 1.5px 0 rgba(255,255,255,0.05)'
            }}
          >
            {/* Upper stepper count */}
            <div className="flex justify-between items-center text-[10px] font-mono pl-1 text-slate-500">
              <span className="font-bold tracking-widest">TRANSMISSION CHANNEL</span>
              <span>INDEXING {onboardIndex + 1}/3</span>
            </div>

            {/* Slides details block */}
            <AnimatePresence mode="wait">
              <motion.div
                key={onboardIndex}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -15 }}
                className="space-y-5 text-center py-6"
              >
                <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/5 mx-auto flex items-center justify-center">
                  {(() => {
                    const SlideIcon = onboardingSlides[onboardIndex].icon;
                    return <SlideIcon className="w-8 h-8" style={{ color: onboardingSlides[onboardIndex].highlight }} />;
                  })()}
                </div>

                <div className="space-y-2">
                  <h2 className="text-xl font-sans font-bold tracking-tight text-[#F5F7FA]">
                    {onboardingSlides[onboardIndex].title}
                  </h2>
                  <p className="text-xs text-slate-400 leading-relaxed font-normal max-w-sm mx-auto">
                    {onboardingSlides[onboardIndex].desc}
                  </p>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* Bullets control grid */}
            <div className="flex justify-center gap-1.5 pt-2">
              {onboardingSlides.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setOnboardIndex(i)}
                  className={`h-1.5 rounded-full transition-all cursor-pointer ${
                    onboardIndex === i ? "w-6 bg-[#7BC6FF]" : "w-1.5 bg-white/15"
                  }`}
                />
              ))}
            </div>

            {/* Step navigation buttons */}
            <div className="flex gap-4 items-center">
              <button
                onClick={skipToAuth}
                className="px-4 py-3 border border-white/5 bg-white/5 hover:bg-white/10 text-xs font-mono tracking-wider rounded-xl cursor-pointer text-slate-400 font-bold transition-all"
              >
                SKIP
              </button>

              <button
                onClick={() => {
                  if (onboardIndex < 2) {
                    setOnboardIndex(prev => prev + 1);
                  } else {
                    setStep("auth");
                  }
                }}
                className="flex-1 py-3.5 bg-[#7BC6FF] text-slate-950 font-sans font-bold text-xs rounded-xl flex items-center justify-center gap-2 tracking-widest cursor-pointer hover:brightness-110 shadow-[0_4px_20px_rgba(123,198,255,0.25)] transition-all"
              >
                <span>CONTINUE</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </motion.div>
        )}

        {/* PHASE 3: MULTI-OPTION AUTHENTICATION */}
        {step === "auth" && (
          <motion.div
            key="auth"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="w-full max-w-md bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-6 shadow-2xl backdrop-blur-md relative"
            style={{
              boxShadow: 'inset 0 1px 1.5px 0 rgba(255,255,255,0.05)'
            }}
          >
            <div className="text-center space-y-1">
              <span className="text-[10px] font-mono text-[#7BC6FF] uppercase tracking-widest font-bold">SECURITY GATEWAY</span>
              <h2 className="text-2xl font-bold tracking-tight text-white leading-none pt-0.5">Awaken Core Profile</h2>
              <p className="text-xs text-slate-500 font-sans pt-1">Establish your localized reading ecosystem configuration</p>
            </div>

            {/* Auth selector rails */}
            <div className="flex bg-slate-900 border border-white/5 rounded-2xl p-1 shadow-inner">
              <button
                onClick={() => {
                  setAuthMethod("guest");
                  setAuthError("");
                }}
                className={`flex-1 py-2 text-xs font-mono font-bold rounded-xl tracking-wider transition-all cursor-pointer ${
                  authMethod === "guest" ? "bg-[#7BC6FF] text-slate-950" : "text-slate-400 hover:text-white"
                }`}
              >
                GUEST EXPRESS
              </button>
              <button
                onClick={() => {
                  setAuthMethod("email");
                  setAuthError("");
                }}
                className={`flex-1 py-2 text-xs font-mono font-bold rounded-xl tracking-wider transition-all cursor-pointer ${
                  authMethod === "email" ? "bg-[#7BC6FF] text-slate-950" : "text-slate-400 hover:text-white"
                }`}
              >
                SECURE EMAIL
              </button>
            </div>

            {/* ERROR DISPATCH */}
            {authError && (
              <div className="p-3.5 bg-rose-500/10 border border-rose-500/25 rounded-xl flex items-center gap-2 text-xs text-rose-400">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span className="font-medium font-mono">{authError}</span>
              </div>
            )}

            <AnimatePresence mode="wait">
              
              {/* GUEST MODE COMPONENT */}
              {authMethod === "guest" && (
                <motion.div
                  key="guest-form"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="space-y-4 py-3"
                >
                  <div className="p-4 rounded-2xl bg-white/5 border border-white/5 text-xs text-slate-400 leading-relaxed text-center space-y-2">
                    <p>Guest Access establishes a localized session on this browser. Sync data matches physical state memory.</p>
                    <p className="text-[#7BC6FF] font-semibold text-[10px] font-mono">⚡ No email validations required.</p>
                  </div>

                  <button
                    id="btn-guest-proceed"
                    onClick={handleGuestEntry}
                    disabled={isLoading}
                    className="w-full py-4.5 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-xs tracking-widest hover:brightness-110 active:scale-98 cursor-pointer shadow-[0_4px_20px_rgba(123,198,255,0.3)] transition-all flex items-center justify-center gap-2"
                  >
                    {isLoading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <span>LAUNCH INSTANT SESSION</span>
                        <Check className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </motion.div>
              )}

              {/* EMAIL METHOD COMPONENT */}
              {authMethod === "email" && (
                <motion.div
                  key="email-form"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="space-y-4 py-1"
                >
                  {!isOtpSent ? (
                    <form onSubmit={handleSendOTP} className="space-y-4">
                      <div className="space-y-2">
                        <label className="text-[10px] font-mono uppercase tracking-wider text-slate-500 pl-1">Email Coordinates</label>
                        <div className="relative">
                          <Mail className="absolute left-4 top-1/2 -to-translate-y-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                          <input
                            type="email"
                            required
                            value={emailInput}
                            onChange={(e) => setEmailInput(e.target.value)}
                            placeholder="pilot@lumen-system.io"
                            className="w-full pl-11 pr-4 py-3.5 bg-white/5 border border-white/10 rounded-2xl text-sm focus:outline-none focus:border-[#7BC6FF]/35 transition-all text-white"
                          />
                        </div>
                      </div>

                      <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-4 bg-[#7BC6FF] text-slate-950 font-sans font-bold text-xs tracking-widest rounded-2xl hover:brightness-110 transition-all cursor-pointer flex items-center justify-center gap-2"
                      >
                        {isLoading ? (
                          <RefreshCw className="w-4 h-4 animate-spin" />
                        ) : (
                          <>
                            <span>TRANSMIT SECURITY DISPATCH</span>
                            <KeyRound className="w-4 h-4" />
                          </>
                        )}
                      </button>
                    </form>
                  ) : (
                    <form onSubmit={handleVerifyOTP} className="space-y-4">
                      <div className="p-4 rounded-xl bg-[#7BC6FF]/5 border border-[#7BC6FF]/10 text-xs text-slate-400 text-center space-y-1">
                        <p>We transmitted a 4-digit confirmation token to:</p>
                        <p className="font-mono text-[#7BC6FF] font-bold">{emailInput}</p>
                      </div>

                      <div className="space-y-2">
                        <label className="text-[10px] font-mono uppercase tracking-wider text-slate-500 pl-1 font-bold">Enter OTP Code</label>
                        <input
                          type="text"
                          maxLength={4}
                          required
                          value={otpInput}
                          onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ""))}
                          placeholder="0000"
                          className="w-full py-4 bg-white/5 border border-white/10 rounded-2xl text-center text-xl font-mono tracking-[1em] focus:outline-none focus:border-[#7BC6FF]/40 text-[#7BC6FF] font-bold"
                        />
                      </div>

                      <div className="flex gap-4">
                        <button
                          type="button"
                          onClick={() => setIsOtpSent(false)}
                          className="px-4 py-3 border border-white/5 bg-white/5 hover:bg-white/10 text-[10px] font-mono rounded-xl cursor-pointer text-slate-400 font-bold"
                        >
                          BACK
                        </button>

                        <button
                          type="submit"
                          disabled={isLoading}
                          className="flex-1 py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-xs tracking-widest cursor-pointer hover:brightness-110 transition-all flex items-center justify-center gap-1.5"
                        >
                          {isLoading ? (
                            <RefreshCw className="w-4 h-4 animate-spin" />
                          ) : (
                            <>
                              <span>AUTHENTICATE</span>
                              <Check className="w-4 h-4 font-bold" />
                            </>
                          )}
                        </button>
                      </div>
                    </form>
                  )}
                </motion.div>
              )}

            </AnimatePresence>

          </motion.div>
        )}

        {/* PHASE 4: GENRE / INTEREST SELECTION */}
        {step === "genres" && (
          <motion.div
            key="genres"
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className="w-full max-w-lg bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-6 shadow-2xl backdrop-blur-md relative"
            style={{
              boxShadow: 'inset 0 1px 1.5px 0 rgba(255,255,255,0.05)'
            }}
          >
            <div className="text-center space-y-1">
              <span className="text-[10px] font-mono text-[#7BC6FF] uppercase tracking-widest font-bold">ALGORITHMIC TAILORING</span>
              <h2 className="text-2xl font-bold tracking-tight text-[#F5F7FA]">Choose Interest Spheres</h2>
              <p className="text-xs text-slate-500 font-sans pt-1">Select at least 2 literary vectors to calibrate the homepage database.</p>
            </div>

            {/* Selection pool buttons grids */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 max-h-56 overflow-y-auto pr-1">
              {genresPool.map((genre) => {
                const isSelected = selectedGenres.includes(genre);
                return (
                  <button
                    id={`btn-genre-select-${genre.toLowerCase().replace(" ", "-")}`}
                    key={genre}
                    onClick={() => handleToggleGenre(genre)}
                    className={`p-3.5 border rounded-2xl text-xs font-semibold cursor-pointer select-none transition-all duration-300 text-center uppercase tracking-wide flex items-center justify-center gap-1.5 ${
                      isSelected 
                        ? "bg-[#7BC6FF]/10 text-[#7BC6FF] border-[#7BC6FF]/35 shadow-[inset_0_1px_8px_0_rgba(123,198,255,0.15)] scale-102"
                        : "bg-white/5 text-slate-400 border-white/5 hover:bg-white/10 hover:text-white"
                    }`}
                  >
                    <span>{genre}</span>
                    {isSelected && <Check className="w-3.5 h-3.5" />}
                  </button>
                );
              })}
            </div>

            {/* Complete trigger button */}
            <div className="pt-2">
              <button
                id="btn-genres-finalize"
                disabled={selectedGenres.length < 2}
                onClick={handleCompleteGateway}
                className={`w-full py-4 rounded-2xl font-bold text-xs tracking-widest cursor-pointer shadow-lg transition-all duration-300 flex items-center justify-center gap-2 ${
                  selectedGenres.length >= 2 
                    ? "bg-[#7BC6FF] text-slate-950 hover:brightness-110 shadow-[0_4px_24px_rgba(123,198,255,0.3)] animate-pulse" 
                    : "bg-white/5 text-slate-500 border border-white/5 cursor-not-allowed"
                }`}
                style={{ animationDuration: '3.5s' }}
              >
                <span>ESTABLISH ODYSSEY CONSOLE</span>
                <Sparkles className="w-4 h-4" />
              </button>
              
              <p className="text-center text-[9px] text-slate-500 font-mono mt-3 uppercase tracking-wider">
                {selectedGenres.length < 2 
                  ? `Calibrate ${2 - selectedGenres.length} more genre tags` 
                  : "CALIBRATION PROTOCOL SECURED"
                }
              </p>
            </div>

          </motion.div>
        )}

      </AnimatePresence>

    </div>
  );
}
