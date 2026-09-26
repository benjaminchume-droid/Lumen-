import React, { useState } from "react";
import {
  Sparkles, BookOpen, Layers, Mail, ArrowRight, Check,
  AlertCircle, RefreshCw, KeyRound,
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { sendEmailOtp, verifyEmailOtp, guestSession, type LumenSession } from "../lib/auth";

interface GatewayProps {
  onComplete: (session: LumenSession, genres: string[]) => void;
}

export default function Gateway({ onComplete }: GatewayProps) {
  const [step, setStep] = useState<"splash" | "onboarding" | "auth" | "genres">("splash");
  const [onboardIndex, setOnboardIndex] = useState(0);

  const [authMethod, setAuthMethod] = useState<"guest" | "email">("guest");
  const [emailInput, setEmailInput] = useState("");
  const [otpInput, setOtpInput] = useState("");
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [pendingSession, setPendingSession] = useState<LumenSession | null>(null);

  const [selectedGenres, setSelectedGenres] = useState<string[]>([]);

  const onboardingSlides = [
    {
      title: "Extensions · Sources",
      desc: "Install manga sources from Keiyoushi and novel plugins from LNReader — same model as Mihon.",
      icon: Layers,
      highlight: "#7BC6FF",
    },
    {
      title: "Manga · Novels · Comics",
      desc: "Browse installed sources, aggregate chapters, and read with data-saver downloads.",
      icon: BookOpen,
      highlight: "#a29bfe",
    },
    {
      title: "Publish & Author",
      desc: "Sign in with email to claim a unique author name and publish series to Lumen.",
      icon: Sparkles,
      highlight: "#ffeaa7",
    },
  ];

  const genresPool = [
    "Action", "Adventure", "Fantasy", "Sci-Fi", "Romance", "Horror",
    "Mystery", "Drama", "Comedy", "Slice of Life", "Psychological", "Classic",
  ];

  const handleToggleGenre = (genre: string) => {
    setSelectedGenres((prev) =>
      prev.includes(genre) ? prev.filter((g) => g !== genre) : [...prev, genre]
    );
  };

  const handleSendOTP = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError("");
    setIsLoading(true);
    const result = await sendEmailOtp(emailInput);
    setIsLoading(false);
    if (!result.ok) {
      setAuthError(result.message);
      return;
    }
    setIsOtpSent(true);
  };

  const handleVerifyOTP = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError("");
    setIsLoading(true);
    const result = await verifyEmailOtp(emailInput, otpInput);
    setIsLoading(false);
    if (!result.ok) {
      setAuthError(result.message);
      return;
    }
    setPendingSession(result.session);
    setStep("genres");
  };

  const handleGuestEntry = () => {
    setPendingSession(guestSession());
    setStep("genres");
  };

  const handleCompleteGateway = () => {
    if (selectedGenres.length < 2) return;
    const session = pendingSession || guestSession();
    onComplete(session, selectedGenres);
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0C0F] text-white flex items-center justify-center p-6 overflow-hidden select-none">
      <div className="absolute top-[-10%] left-[-10%] w-[50%] aspect-square rounded-full bg-sky-500/5 blur-[140px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] aspect-square rounded-full bg-violet-500/5 blur-[140px] pointer-events-none" />

      <AnimatePresence mode="wait">
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
                className="w-24 h-24 border border-white/15 bg-slate-900/60 rounded-full flex items-center justify-center shadow-2xl relative mx-auto"
              >
                <div className="absolute inset-0 bg-gradient-to-tr from-[#7BC6FF]/0 via-[#7BC6FF]/10 to-[#7BC6FF]/0 pointer-events-none rounded-full animate-pulse" />
                <Layers className="w-10 h-10 text-[#7BC6FF]" />
              </motion.div>
              <div className="space-y-2">
                <h1 className="text-4xl font-sans font-bold tracking-[0.22em] text-[#F5F7FA] uppercase pl-4">
                  Lumen
                </h1>
                <p className="text-[10px] uppercase font-mono tracking-[0.45em] text-[#7BC6FF] font-semibold">
                  Reading OS
                </p>
              </div>
            </div>
            <button
              onClick={() => setStep("onboarding")}
              className="w-full py-4 bg-white/5 hover:bg-white/10 border border-white/10 hover:border-[#7BC6FF]/35 rounded-2xl flex items-center justify-center gap-3 transition-all"
            >
              <span className="font-semibold text-sm tracking-widest">GET STARTED</span>
              <ArrowRight className="w-4 h-4 text-[#7BC6FF]" />
            </button>
          </motion.div>
        )}

        {step === "onboarding" && (
          <motion.div
            key="onboarding"
            initial={{ opacity: 0, x: 40 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -40 }}
            className="w-full max-w-md bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-8 backdrop-blur-md"
          >
            <div className="flex justify-between text-[10px] font-mono text-slate-500">
              <span className="font-bold tracking-widest">ONBOARDING</span>
              <span>{onboardIndex + 1}/3</span>
            </div>
            <AnimatePresence mode="wait">
              <motion.div
                key={onboardIndex}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -12 }}
                className="space-y-5 text-center py-6"
              >
                <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/5 mx-auto flex items-center justify-center">
                  {(() => {
                    const Icon = onboardingSlides[onboardIndex].icon;
                    return (
                      <Icon
                        className="w-8 h-8"
                        style={{ color: onboardingSlides[onboardIndex].highlight }}
                      />
                    );
                  })()}
                </div>
                <h2 className="text-xl font-bold tracking-tight">
                  {onboardingSlides[onboardIndex].title}
                </h2>
                <p className="text-xs text-slate-400 leading-relaxed max-w-sm mx-auto">
                  {onboardingSlides[onboardIndex].desc}
                </p>
              </motion.div>
            </AnimatePresence>
            <div className="flex justify-center gap-1.5">
              {onboardingSlides.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setOnboardIndex(i)}
                  className={`h-1.5 rounded-full transition-all ${
                    onboardIndex === i ? "w-6 bg-[#7BC6FF]" : "w-1.5 bg-white/15"
                  }`}
                />
              ))}
            </div>
            <div className="flex gap-4">
              <button
                onClick={() => setStep("auth")}
                className="px-4 py-3 border border-white/5 bg-white/5 text-xs font-mono rounded-xl text-slate-400"
              >
                SKIP
              </button>
              <button
                onClick={() => {
                  if (onboardIndex < 2) setOnboardIndex((p) => p + 1);
                  else setStep("auth");
                }}
                className="flex-1 py-3.5 bg-[#7BC6FF] text-slate-950 font-bold text-xs rounded-xl flex items-center justify-center gap-2 tracking-widest"
              >
                CONTINUE <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </motion.div>
        )}

        {step === "auth" && (
          <motion.div
            key="auth"
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            className="w-full max-w-md bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-6 backdrop-blur-md"
          >
            <div className="text-center space-y-1">
              <span className="text-[10px] font-mono text-[#7BC6FF] uppercase tracking-widest font-bold">
                Sign in
              </span>
              <h2 className="text-2xl font-bold tracking-tight">Welcome to Lumen</h2>
              <p className="text-xs text-slate-500">Guest to browse · Email to publish</p>
            </div>

            <div className="flex bg-slate-900 border border-white/5 rounded-2xl p-1">
              <button
                onClick={() => {
                  setAuthMethod("guest");
                  setAuthError("");
                }}
                className={`flex-1 py-2 text-xs font-mono font-bold rounded-xl ${
                  authMethod === "guest" ? "bg-[#7BC6FF] text-slate-950" : "text-slate-400"
                }`}
              >
                GUEST
              </button>
              <button
                onClick={() => {
                  setAuthMethod("email");
                  setAuthError("");
                }}
                className={`flex-1 py-2 text-xs font-mono font-bold rounded-xl ${
                  authMethod === "email" ? "bg-[#7BC6FF] text-slate-950" : "text-slate-400"
                }`}
              >
                EMAIL OTP
              </button>
            </div>

            {authError && (
              <div className="p-3.5 bg-rose-500/10 border border-rose-500/25 rounded-xl flex items-center gap-2 text-xs text-rose-400">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span className="font-mono">{authError}</span>
              </div>
            )}

            <AnimatePresence mode="wait">
              {authMethod === "guest" && (
                <motion.div
                  key="guest"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  className="space-y-4"
                >
                  <p className="text-xs text-slate-400 text-center leading-relaxed">
                    Browse and install sources locally. Upgrade later in Settings to publish.
                  </p>
                  <button
                    onClick={handleGuestEntry}
                    className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-xs tracking-widest flex items-center justify-center gap-2"
                  >
                    CONTINUE AS GUEST <Check className="w-4 h-4" />
                  </button>
                </motion.div>
              )}

              {authMethod === "email" && (
                <motion.div
                  key="email"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  className="space-y-4"
                >
                  {!isOtpSent ? (
                    <form onSubmit={handleSendOTP} className="space-y-4">
                      <div className="space-y-2">
                        <label className="text-[10px] font-mono uppercase text-slate-500 pl-1">
                          Email
                        </label>
                        <div className="relative">
                          <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                          <input
                            type="email"
                            required
                            value={emailInput}
                            onChange={(e) => setEmailInput(e.target.value)}
                            placeholder="you@example.com"
                            className="w-full pl-11 pr-4 py-3.5 bg-white/5 border border-white/10 rounded-2xl text-sm focus:outline-none focus:border-[#7BC6FF]/40 text-white"
                          />
                        </div>
                      </div>
                      <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-4 bg-[#7BC6FF] text-slate-950 font-bold text-xs tracking-widest rounded-2xl flex items-center justify-center gap-2 disabled:opacity-50"
                      >
                        {isLoading ? (
                          <RefreshCw className="w-4 h-4 animate-spin" />
                        ) : (
                          <>
                            SEND 6-DIGIT CODE <KeyRound className="w-4 h-4" />
                          </>
                        )}
                      </button>
                    </form>
                  ) : (
                    <form onSubmit={handleVerifyOTP} className="space-y-4">
                      <div className="p-4 rounded-xl bg-[#7BC6FF]/5 border border-[#7BC6FF]/10 text-xs text-slate-400 text-center space-y-1">
                        <p>Enter the 6-digit code sent to</p>
                        <p className="font-mono text-[#7BC6FF] font-bold">{emailInput}</p>
                      </div>
                      <input
                        type="text"
                        inputMode="numeric"
                        maxLength={6}
                        required
                        value={otpInput}
                        onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, "").slice(0, 6))}
                        placeholder="000000"
                        className="w-full py-4 bg-white/5 border border-white/10 rounded-2xl text-center text-xl font-mono tracking-[0.5em] focus:outline-none focus:border-[#7BC6FF]/40 text-[#7BC6FF] font-bold"
                      />
                      <div className="flex gap-3">
                        <button
                          type="button"
                          onClick={() => {
                            setIsOtpSent(false);
                            setOtpInput("");
                            setAuthError("");
                          }}
                          className="px-4 py-3 border border-white/5 bg-white/5 text-[10px] font-mono rounded-xl text-slate-400"
                        >
                          BACK
                        </button>
                        <button
                          type="submit"
                          disabled={isLoading || otpInput.length !== 6}
                          className="flex-1 py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-xs tracking-widest flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                          {isLoading ? (
                            <RefreshCw className="w-4 h-4 animate-spin" />
                          ) : (
                            "VERIFY & CONTINUE"
                          )}
                        </button>
                      </div>
                      <button
                        type="button"
                        onClick={handleSendOTP as any}
                        className="w-full text-[10px] font-mono text-slate-500 hover:text-[#7BC6FF]"
                      >
                        Resend code
                      </button>
                    </form>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )}

        {step === "genres" && (
          <motion.div
            key="genres"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="w-full max-w-md bg-slate-950/45 border border-white/5 rounded-3xl p-6 md:p-8 space-y-6 backdrop-blur-md"
          >
            <div className="text-center space-y-1">
              <span className="text-[10px] font-mono text-[#7BC6FF] uppercase tracking-widest font-bold">
                Preferences
              </span>
              <h2 className="text-xl font-bold">Pick at least 2 genres</h2>
            </div>
            <div className="flex flex-wrap gap-2 justify-center">
              {genresPool.map((g) => (
                <button
                  key={g}
                  type="button"
                  onClick={() => handleToggleGenre(g)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-mono border transition-all ${
                    selectedGenres.includes(g)
                      ? "bg-[#7BC6FF]/20 border-[#7BC6FF]/40 text-[#7BC6FF]"
                      : "bg-white/5 border-white/10 text-slate-400"
                  }`}
                >
                  {g}
                </button>
              ))}
            </div>
            <button
              onClick={handleCompleteGateway}
              disabled={selectedGenres.length < 2}
              className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl font-bold text-xs tracking-widest disabled:opacity-40"
            >
              ENTER LUMEN
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
