import type { CSSProperties, ReactNode } from "react";

type Props = {
  children: ReactNode;
  className?: string;
  intensity?: number; // 8–40 typical
  radius?: number;
  onClick?: () => void;
  style?: CSSProperties;
};

/**
 * Liquid glass panel: backdrop blur + inner highlight (refraction edge).
 * Intensity maps to blur px; keep readable contrast on Soft Black.
 */
export default function LiquidGlass({
  children,
  className = "",
  intensity = 18,
  radius = 24,
  onClick,
  style,
}: Props) {
  const blur = Math.max(8, Math.min(40, intensity));
  return (
    <div
      onClick={onClick}
      className={`relative overflow-hidden border border-white/10 bg-white/[0.04] ${className}`}
      style={{
        borderRadius: radius,
        backdropFilter: `blur(${blur}px) saturate(1.35)`,
        WebkitBackdropFilter: `blur(${blur}px) saturate(1.35)`,
        boxShadow:
          "inset 0 1px 1px rgba(255,255,255,0.12), inset 0 -1px 1px rgba(0,0,0,0.25), 0 8px 32px rgba(0,0,0,0.35)",
        ...style,
      }}
    >
      {/* Refraction edge */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "linear-gradient(135deg, rgba(255,255,255,0.14) 0%, rgba(255,255,255,0.03) 35%, transparent 55%, rgba(123,198,255,0.06) 100%)",
        }}
      />
      <div className="relative z-[1]">{children}</div>
    </div>
  );
}

/** Search bar / nav strip variant */
export function LiquidGlassBar({
  children,
  className = "",
  intensity = 20,
}: {
  children: ReactNode;
  className?: string;
  intensity?: number;
}) {
  return (
    <LiquidGlass className={className} intensity={intensity} radius={999}>
      {children}
    </LiquidGlass>
  );
}
