import React, { useState } from "react";
import {
  BookOpen, Layers, Mail, ArrowRight, Check,
  AlertCircle, RefreshCw, KeyRound, Sparkles,
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

  const slides = [
    {
      title: "Your library",
      desc: "Install manga and novel sources, then keep everything in one place.",
      icon: Layers,
    },
    {
      title: "Read comfortably",
      desc: "Clear typography for novels, smooth paging for manga, with soft ambient lighting.",
      icon: BookOpen,
    },
    {
      title: "Optional account",
      desc: "Browse as a guest, or sign in with email when you want to publish.",
      icon: Sparkles,
    },
  ];

  const genresPool = [
    "Action", "Adventure", "Fantasy", "Sci-Fi", "Romance", "Horror",
    "Mystery", "Drama", "Comedy", "Slice of Life", "Psychological", "Classic",
  ];

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

  const handleComplete = () => {
    if (selectedGenres.length < 2) return;
    onComplete(pendingSession || guestSession(), selectedGenres);
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
            className="flex flex-col items-center text-center space-y-10 max-w-md w-full"
          >
            <div className="space-y-4">
              <div className="w-24 h-24 border border-white/15 bg-slate-900/60 rounded-full flex items-center justify-center mx-auto">
                <Layers className="w-10 h-10 text-[#7BC6FF]" />
              </div>
              <h1 className="text-4xl font-bold tracking-[0.2em] text-[#F5F7FA] uppercase pl-3">Lumen</h1>
              <p className="text-[10px] uppercase tracking-[0.35em] text-[#7BC6FF]/80 font-medium">Reading</p>
            </div>
            <button
              onClick={() => setStep("onboarding")}
              className="w-full py-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl flex items-center justify-center gap-3"
            >
              <span className="text-sm tracking-wide text-[#F5F7FA]/90">Get started</span>
              <ArrowRight className="w-4 h-4 text-[#7BC6FF]" />
            </button>
            <p className="text-[10px] text-slate-600 tracking-wide">v{import.meta.env?.VITE_APP_VERSION || "1.2.0"}</p>
          </motion.div>
        )}

        {step === "onboarding" && (
          <motion.div
            key="onboarding"
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -24 }}
            className="w-full max-w-md bg-slate-950/50 border border-white/5 rounded-3xl p-6 space-y-8 backdrop-blur-md"
          >
            <div className="flex justify-between text-[10px] text-slate-500 tracking-wide">
              <span>Introduction</span>
              <span>{onboardIndex + 1} / 3</span>
            </div>
            <div className="text-center space-y-4 py-6">
              {(() => {
                const Icon = slides[onboardIndex].icon;
                return <Icon className="w-8 h-8 text-[#7BC6FF] mx-auto" />;
              })()}
              <h2 className="text-xl font-semibold text-[#F5F7FA]">{slides[onboardIndex].title}</h2>
              <p className="text-xs text-slate-400 leading-relaxed max-w-sm mx-auto">{slides[onboardIndex].desc}</p>
            </div>
            <div className="flex gap-3">
              <button onClick={() => setStep("auth")} className="px-4 py-3 text-xs text-slate-400 border border-white/5 rounded-xl">
                Skip
              </button>
              <button
                onClick={() => (onboardIndex < 2 ? setOnboardIndex((i) => i + 1) : setStep("auth"))}
                className="flex-1 py-3.5 bg-[#7BC6FF] text-slate-950 text-xs font-semibold rounded-xl"
              >
                Continue
              </button>
            </div>
          </motion.div>
        )}

        {step === "auth" && (
          <motion.div
            key="auth"
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            className="w-full max-w-md bg-slate-950/50 border border-white/5 rounded-3xl p-6 space-y-5 backdrop-blur-md"
          >
            <div className="text-center space-y-1">
              <p className="text-[10px] tracking-wide text-[#7BC6FF]/80">Sign in</p>
              <h2 className="text-2xl font-semibold text-[#F5F7FA]">Welcome</h2>
              <p className="text-xs text-slate-500">Guest to browse · Email to publish</p>
            </div>

            <div className="flex bg-slate-900 border border-white/5 rounded-2xl p-1">
              {(["guest", "email"] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => { setAuthMethod(m); setAuthError(""); }}
                  className={`flex-1 py-2 text-xs rounded-xl ${
                    authMethod === m ? "bg-[#7BC6FF] text-slate-950 font-semibold" : "text-slate-400"
                  }`}
                >
                  {m === "guest" ? "Guest" : "Email"}
                </button>
              ))}
            </div>

            {authError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl flex gap-2 text-xs text-rose-400">
                <AlertCircle className="w-4 h-4 shrink-0" /> {authError}
              </div>
            )}

            {authMethod === "guest" && (
              <button
                onClick={handleGuestEntry}
                className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl text-xs font-semibold flex items-center justify-center gap-2"
              >
                Continue as guest <Check className="w-4 h-4" />
              </button>
            )}

            {authMethod === "email" && !isOtpSent && (
              <form onSubmit={handleSendOTP} className="space-y-4">
                <div className="relative">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    type="email"
                    required
                    value={emailInput}
                    onChange={(e) => setEmailInput(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full pl-11 pr-4 py-3.5 bg-white/5 border border-white/10 rounded-2xl text-sm outline-none focus:border-[#7BC6FF]/40"
                  />
                </div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl text-xs font-semibold flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {isLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <>Send code <KeyRound className="w-4 h-4" /></>}
                </button>
              </form>
            )}

            {authMethod === "email" && isOtpSent && (
              <form onSubmit={handleVerifyOTP} className="space-y-4">
                <p className="text-xs text-slate-400 text-center">
                  Enter the 6-digit code sent to <span className="text-[#7BC6FF]">{emailInput}</span>
                </p>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={otpInput}
                  onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder="000000"
                  className="w-full py-4 bg-white/5 border border-white/10 rounded-2xl text-center text-xl tracking-[0.4em] text-[#7BC6FF] outline-none focus:border-[#7BC6FF]/40"
                />
                <button
                  type="submit"
                  disabled={isLoading || otpInput.length !== 6}
                  className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl text-xs font-semibold disabled:opacity-50"
                >
                  {isLoading ? "Checking…" : "Verify"}
                </button>
              </form>
            )}
          </motion.div>
        )}

        {step === "genres" && (
          <motion.div
            key="genres"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            className="w-full max-w-md bg-slate-950/50 border border-white/5 rounded-3xl p-6 space-y-6 backdrop-blur-md"
          >
            <div className="text-center">
              <h2 className="text-xl font-semibold text-[#F5F7FA]">Choose a few interests</h2>
              <p className="text-xs text-slate-500 mt-1">Pick at least two</p>
            </div>
            <div className="flex flex-wrap gap-2 justify-center">
              {genresPool.map((g) => (
                <button
                  key={g}
                  type="button"
                  onClick={() =>
                    setSelectedGenres((prev) =>
                      prev.includes(g) ? prev.filter((x) => x !== g) : [...prev, g]
                    )
                  }
                  className={`px-3 py-1.5 rounded-xl text-xs border ${
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
              onClick={handleComplete}
              disabled={selectedGenres.length < 2}
              className="w-full py-4 bg-[#7BC6FF] text-slate-950 rounded-2xl text-xs font-semibold disabled:opacity-40"
            >
              Enter Lumen
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
