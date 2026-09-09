/**
 * Lumen — Extension providers (repos only).
 * Sources are discovered from official indexes and installed on demand.
 * Never ships fake demo content. Only the index JSON is fetched until a source is installed.
 */

import { MediaItem } from "../types";

export type RepoKind = "manga" | "novel" | "both";

export interface ExtensionRepoProvider {
  id: string;
  name: string;
  kind: RepoKind;
  /** Index URL — only this is fetched until user installs a source */
  indexUrl: string;
  format: "mihon_index_json" | "mihon_index_min" | "lnreader_plugins_min";
  /** Optional human note */
  note?: string;
}

/**
 * Official providers — repos only, zero source APKs/JS until user installs.
 * Keiyoushi: full index.json (protobuf-json) with real extensions.
 * LNReader: official plugins.min.json.
 */
export const EXTENSION_REPO_PROVIDERS: ExtensionRepoProvider[] = [
  {
    id: "keiyoushi",
    name: "Keiyoushi (Mihon)",
    kind: "manga",
    indexUrl: "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json",
    format: "mihon_index_json",
    note: "Official Mihon extension index. Only index is downloaded; APKs on demand.",
  },
  {
    id: "lnreader",
    name: "LNReader Official",
    kind: "novel",
    indexUrl:
      "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json",
    format: "lnreader_plugins_min",
    note: "Official LNReader plugin manifest. Plugin JS downloaded only on install.",
  },
];

/** @deprecated use EXTENSION_REPO_PROVIDERS — kept for SourceManager compatibility */
export interface SourceExtension {
  id: string;
  name: string;
  version: string;
  type: "manga" | "novel";
  repositoryUrl: string;
  healthStatus: "healthy" | "degraded" | "offline" | "unknown";
  pingMs: number;
  sourceCount?: number;
}

export const MANGA_REPOSITORIES: SourceExtension[] = EXTENSION_REPO_PROVIDERS.filter(
  (r) => r.kind === "manga" || r.kind === "both"
).map((r) => ({
  id: r.id,
  name: r.name,
  version: "index",
  type: "manga" as const,
  repositoryUrl: r.indexUrl,
  healthStatus: "unknown" as const,
  pingMs: 0,
}));

export const NOVEL_REPOSITORIES: SourceExtension[] = EXTENSION_REPO_PROVIDERS.filter(
  (r) => r.kind === "novel" || r.kind === "both"
).map((r) => ({
  id: r.id,
  name: r.name,
  version: "index",
  type: "novel" as const,
  repositoryUrl: r.indexUrl,
  healthStatus: "unknown" as const,
  pingMs: 0,
}));

/** Data-saver helper for covers / images */
export function optimizeContentItem(item: MediaItem): MediaItem {
  const cover = item.coverUrl || "";
  let optimized = cover;
  try {
    if (cover.includes("unsplash") || cover.includes("?")) {
      const u = new URL(cover);
      if (!u.searchParams.has("w")) u.searchParams.set("w", "400");
      if (!u.searchParams.has("q")) u.searchParams.set("q", "70");
      optimized = u.toString();
    }
  } catch {
    /* keep original */
  }
  return {
    ...item,
    coverUrl: optimized,
    syncedAt: new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    }),
  };
}
