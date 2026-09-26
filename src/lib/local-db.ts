/**
 * Local / guest persistence layer.
 * Works offline without Supabase. When user signs in, same shapes can sync to RDB.
 */

import type { MediaItem, HistoryEntry } from "../types";
import { stableSeriesId } from "./series-id";

const K = {
  library: "lumen_library_v2",
  likes: "lumen_likes",
  collections: "lumen_collections",
  downloads: "lumen_downloads",
  installedSources: "lumen_active_sources",
  history: "lumen_history",
  session: "lumen_user_session",
} as const;

export type LumenCollection = {
  id: string;
  name: string;
  seriesIds: string[];
  createdAt: number;
};

export type DownloadRecord = {
  id: string;
  seriesId: string;
  seriesTitle: string;
  chapterId: string;
  chapterTitle: string;
  status: "queued" | "running" | "paused" | "done" | "failed";
  progress: number;
  updatedAt: number;
};

function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function write<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value));
}

export const localDb = {
  getLibrary(): MediaItem[] {
    return read(K.library, []);
  },
  setLibrary(items: MediaItem[]) {
    write(K.library, items);
  },
  upsertSeries(item: MediaItem) {
    const lib = this.getLibrary();
    const i = lib.findIndex((x) => x.id === item.id);
    if (i >= 0) lib[i] = item;
    else lib.unshift(item);
    this.setLibrary(lib);
    return lib;
  },

  getLikes(): string[] {
    return read(K.likes, []);
  },
  toggleLike(seriesId: string): string[] {
    const likes = this.getLikes();
    const next = likes.includes(seriesId)
      ? likes.filter((id) => id !== seriesId)
      : [...likes, seriesId];
    write(K.likes, next);
    return next;
  },
  isLiked(seriesId: string): boolean {
    return this.getLikes().includes(seriesId);
  },

  getCollections(): LumenCollection[] {
    return read(K.collections, []);
  },
  createCollection(name: string): LumenCollection {
    const c: LumenCollection = {
      id: `col_${Date.now()}`,
      name: name.trim() || "Untitled",
      seriesIds: [],
      createdAt: Date.now(),
    };
    const all = this.getCollections();
    write(K.collections, [c, ...all]);
    return c;
  },
  addToCollection(collectionId: string, seriesId: string) {
    const all = this.getCollections().map((c) =>
      c.id === collectionId && !c.seriesIds.includes(seriesId)
        ? { ...c, seriesIds: [...c.seriesIds, seriesId] }
        : c
    );
    write(K.collections, all);
  },

  getDownloads(): DownloadRecord[] {
    return read(K.downloads, []);
  },
  upsertDownload(rec: DownloadRecord) {
    const all = this.getDownloads();
    const i = all.findIndex((d) => d.id === rec.id);
    if (i >= 0) all[i] = rec;
    else all.unshift(rec);
    write(K.downloads, all);
  },

  getHistory(): HistoryEntry[] {
    return read(K.history, []);
  },
  setHistory(h: HistoryEntry[]) {
    write(K.history, h);
  },

  getInstalledSourceIds(): string[] {
    return read(K.installedSources, []);
  },
  setInstalledSourceIds(ids: string[]) {
    write(K.installedSources, ids);
  },
};

/** Build a library MediaItem from an external source entry */
export function externalToMediaItem(opts: {
  sourceId: string;
  externalKey: string;
  title: string;
  author?: string;
  coverUrl?: string;
  description?: string;
  type: "manga" | "novel";
  genres?: string[];
}): MediaItem {
  const id = stableSeriesId(opts.sourceId, opts.externalKey);
  return {
    id,
    title: opts.title,
    type: opts.type,
    author: opts.author || "Unknown",
    coverUrl: opts.coverUrl || "",
    description: opts.description || "",
    genres: opts.genres || [],
    status: "ongoing",
    rating: 0,
    chaptersCount: 0,
    chapters: [],
    syncedAt: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
  };
}
