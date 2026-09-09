import { BookOpen, Clock, Home, Bell, Settings } from "lucide-react";
import { motion } from "motion/react";

interface GlassDockProps {
  activeTab: 'home' | 'library' | 'history' | 'updates' | 'more';
  setActiveTab: (tab: 'home' | 'library' | 'history' | 'updates' | 'more') => void;
  unreadUpdatesCount: number;
}

export default function GlassDock({ activeTab, setActiveTab, unreadUpdatesCount }: GlassDockProps) {
  const tabs = [
    { id: 'library', label: 'Library', icon: BookOpen, badge: false },
    { id: 'history', label: 'History', icon: Clock, badge: false },
    { id: 'home', label: 'Home', icon: Home, badge: false },
    { id: 'updates', label: 'Updates', icon: Bell, badge: true },
    { id: 'more', label: 'More', icon: Settings, badge: false },
  ] as const;

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 md:px-0 w-full max-w-lg">
      <div 
        className="relative overflow-hidden rounded-full border border-white/10 dark:border-white/5 bg-slate-900/40 dark:bg-black/45 backdrop-blur-xl shadow-[0_8px_32px_0_rgba(0,0,0,0.37)] px-2 py-2 flex justify-between items-center"
        style={{
          boxShadow: 'inset 0 1px 1px 0 rgba(255, 255, 255, 0.15), 0 8px 32px 0 rgba(0,0,0,0.37)'
        }}
      >
        {/* Soft liquid light refraction highlight overlay */}
        <div className="absolute inset-0 bg-gradient-to-tr from-white/0 via-white/5 to-white/0 pointer-events-none rounded-full" />
        
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          const Icon = tab.icon;
          
          return (
            <button
              id={`nav-tab-${tab.id}`}
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className="relative flex-1 py-2 flex flex-col items-center justify-center cursor-pointer transition-colors duration-300 select-none group focus:outline-none"
            >
              {/* Active Backglow refraction */}
              {isActive && (
                <motion.div
                  layoutId="active-tab-glow"
                  className="absolute inset-0 -z-10 bg-[#7BC6FF]/10 rounded-full blur-md"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}

              {/* Slide-tracker pill */}
              {isActive && (
                <motion.div
                  layoutId="active-tab-pill"
                  className="absolute bottom-0 w-8 h-[3px] bg-[#7BC6FF] rounded-full"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                  style={{
                    boxShadow: '0 0 10px 2px rgba(123, 198, 255, 0.5)'
                  }}
                />
              )}

              <div className="relative">
                <Icon
                  className={`w-5 h-5 transition-all duration-300 ${
                    isActive 
                      ? 'text-[#7BC6FF] scale-110 drop-shadow-[0_0_8px_rgba(123,198,255,0.6)]' 
                      : 'text-slate-400 dark:text-gray-400 group-hover:text-white group-hover:scale-105'
                  }`}
                  style={{ strokeWidth: 1.5 }}
                />

                {/* Badge component */}
                {tab.badge && unreadUpdatesCount > 0 && (
                  <span className="absolute -top-1.5 -right-2 px-1.5 py-0.5 text-[9px] font-bold bg-[#7BC6FF] text-slate-950 rounded-full min-w-4 text-center leading-none ring-1 ring-slate-950">
                    {unreadUpdatesCount}
                  </span>
                )}
              </div>
              
              <span 
                className={`text-[9px] mt-1 font-medium tracking-wide transition-all ${
                  isActive 
                    ? 'text-white font-semibold opacity-100' 
                    : 'text-slate-400 dark:text-gray-500 opacity-80 group-hover:opacity-100 group-hover:text-white'
                }`}
              >
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
