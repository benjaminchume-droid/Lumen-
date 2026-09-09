/**
 * Lumen Reading OS — Source Integration System
 * Production state: no simulated repositories or demo adaptations.
 */

import { MediaItem } from "../types";

export interface SourceExtension {
  id: string;
  name: string;
  version: string;
  type: "manga" | "novel";
  repositoryUrl: string;
  healthStatus: "healthy" | "degraded" | "offline";
  pingMs: number;
}

/** Manga repository adapters — empty until real extensions are registered */
export const MANGA_REPOSITORIES: SourceExtension[] = [];

/** Novel repository adapters — empty until real extensions are registered */
export const NOVEL_REPOSITORIES: SourceExtension[] = [];

/**
 * Lightweight cover URL optimizer (kept for any future real content).
 */
export function optimizeContentItem(item: MediaItem): MediaItem {
  return {
    ...item,
    coverUrl: item.coverUrl.includes("unsplash")
      ? `${item.coverUrl}&q=70&w=400`
      : item.coverUrl,
    syncedAt: new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    }),
  };
}
