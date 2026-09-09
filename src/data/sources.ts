/**
 * Lumen — Extension providers (repos only).
 * Sources are discovered from indexes and installed on demand.
 * Never ships fake demo content.
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
}

/** Official providers — repos only, zero source APKs until user installs */
export const EXTENSION_REPO_PROVIDERS: ExtensionRepoProvider[] = [
  {
    id: "keiyoushi",
    name: "Keiyoushi (Mihon)",
    kind: "manga",
    indexUrl: "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json",
    format: "mihon_index_json",
  },
  {
    id: "lnreader",
    name: "LNReader Official",
    kind: "novel",
    indexUrl:
      "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json",
    format: "lnreader_plugins_min",
  },
];

/** @deprecated use EXTENSION_REPO_PROVIDERS */
export interface SourceExtension {
  id: string;
  name: string;
  version: string;
  type: "manga" | "novel";
  repositoryUrl: string;
  healthStatus: "healthy" | "degraded" | "offline";
  pingMs: number;
}

export const MANGA_REPOSITORIES: SourceExtension[] = EXTENSION_REPO_PROVIDERS.filter(
  (r) => r.kind === "manga" || r.kind === "both"
).map((r) => ({
  id: r.id,
  name: r.name,
  version: "index",
  type: "manga" as const,
  repositoryUrl: r.indexUrl,
  healthStatus: "healthy" as const,
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
  healthStatus: "healthy" as const,
  pingMs: 0,
}));

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
