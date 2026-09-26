import { BookOpen, Clock, Home, Bell, Settings } from "lucide-react";
import { motion } from "motion/react";

interface GlassDockProps {
  activeTab: "home" | "library" | "history" | "updates" | "more";
  setActiveTab?: (tab: "home" | "library" | "history" | "updates" | "more") => void;
  onChangeTab?: (tab: "home" | "library" | "history" | "updates" | "more") => void;
  unreadUpdatesCount?: number;
  updatesBadge?: number;
}

export default function GlassDock({
  activeTab,
  setActiveTab,
  onChangeTab,
  unreadUpdatesCount = 0,
  updatesBadge = 0,
}: GlassDockProps) {
  const setTab = setActiveTab || onChangeTab || (() => {});
  const badge = unreadUpdatesCount || updatesBadge;

  const tabs = [
    { id: "library" as const, label: "Library", icon: BookOpen },
    { id: "history" as const, label: "History", icon: Clock },
    { id: "home" as const, label: "Home", icon: Home },
    { id: "updates" as const, label: "Updates", icon: Bell },
    { id: "more" as const, label: "More", icon: Settings },
  ];

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 w-full max-w-lg">
      <div
        className="relative overflow-hidden rounded-full border border-white/10 px-2 py-2 flex justify-between items-center"
        style={{
          background: "rgba(10, 12, 15, 0.45)",
          backdropFilter: "blur(22px) saturate(1.4)",
          WebkitBackdropFilter: "blur(22px) saturate(1.4)",
          boxShadow:
            "inset 0 1px 1px rgba(255,255,255,0.14), inset 0 -0.5px 0 rgba(0,0,0,0.4), 0 12px 40px rgba(0,0,0,0.45)",
        }}
      >
        <div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{
            background:
              "linear-gradient(120deg, rgba(255,255,255,0.12) 0%, transparent 40%, rgba(123,198,255,0.08) 100%)",
          }}
        />

        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setTab(tab.id)}
              className="relative flex-1 py-2 flex flex-col items-center justify-center focus:outline-none"
            >
              {isActive && (
                <motion.div
                  layoutId="dock-glow"
                  className="absolute inset-1 rounded-full bg-[#7BC6FF]/10"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}
              <div className="relative">
                <Icon
                  className={`w-5 h-5 transition-colors ${
                    isActive ? "text-[#7BC6FF]" : "text-slate-400"
                  }`}
                  strokeWidth={1.5}
                />
                {tab.id === "updates" && badge > 0 && (
                  <span className="absolute -top-1.5 -right-2 px-1 text-[9px] font-bold bg-[#7BC6FF] text-slate-950 rounded-full min-w-4 text-center">
                    {badge}
                  </span>
                )}
              </div>
              <span
                className={`text-[9px] mt-1 tracking-wide ${
                  isActive ? "text-white font-medium" : "text-slate-500"
                }`}
              >
                {tab.label}
              </span>
              {isActive && (
                <motion.div
                  layoutId="dock-pill"
                  className="absolute bottom-0 w-6 h-[2px] bg-[#7BC6FF] rounded-full"
                  style={{ boxShadow: "0 0 8px rgba(123,198,255,0.6)" }}
                />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
