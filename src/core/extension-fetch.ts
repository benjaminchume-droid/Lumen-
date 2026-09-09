/**
 * Fetch + parse official Mihon (Keiyoushi) / LNReader indexes.
 * Data-saver: only downloads index JSON; APK/JS only when user installs a source.
 */

import type { ExtensionManifest } from "./extension-core";
import {
  EXTENSION_REPO_PROVIDERS,
  type ExtensionRepoProvider,
} from "../data/sources";

export interface DiscoveredSource {
  id: string;
  repoId: string;
  name: string;
  lang: string;
  kind: "manga" | "novel";
  baseUrl?: string;
  version?: string;
  iconUrl?: string;
  nsfw: boolean;
  packageName?: string;
  apkUrl?: string;
  pluginUrl?: string;
}

async function fetchJson(url: string, signal?: AbortSignal): Promise<unknown> {
  const res = await fetch(url, {
    signal,
    headers: { Accept: "application/json" },
  });
  if (!res.ok) throw new Error(`Failed to fetch ${url}: ${res.status}`);
  return res.json();
}

/** Keiyoushi full index.json (protobuf-json shape) */
function parseMihonIndexJson(data: unknown, repoId: string): DiscoveredSource[] {
  const root = data as {
    extensionList?: {
      extensions?: Array<{
        name?: string;
        packageName?: string;
        versionName?: string;
        contentWarning?: string;
        resources?: { apkUrl?: string; iconUrl?: string; jarUrl?: string };
        sources?: Array<{
          id?: string | number;
          name?: string;
          language?: string;
          homeUrl?: string;
        }>;
      }>;
    };
  };

  const out: DiscoveredSource[] = [];
  const list = root.extensionList?.extensions ?? [];

  for (const ext of list) {
    const nsfw = (ext.contentWarning || "").toUpperCase().includes("NSFW");
    for (const s of ext.sources ?? []) {
      const id = String(s.id ?? `${ext.packageName}:${s.language}`);
      out.push({
        id,
        repoId,
        name: s.name || ext.name || id,
        lang: s.language || "en",
        kind: "manga",
        baseUrl: s.homeUrl,
        version: ext.versionName,
        iconUrl: ext.resources?.iconUrl,
        nsfw,
        packageName: ext.packageName,
        apkUrl: ext.resources?.apkUrl,
      });
    }
  }
  return out;
}

/** Legacy index.min.json array format (still supported) */
function parseMihonIndexMin(data: unknown, repoId: string): DiscoveredSource[] {
  const list = data as Array<{
    name?: string;
    pkg?: string;
    version?: string;
    nsfw?: number;
    apk?: string;
    sources?: Array<{ id?: string; name?: string; lang?: string; baseUrl?: string }>;
  }>;

  const out: DiscoveredSource[] = [];
  for (const ext of list || []) {
    for (const s of ext.sources ?? []) {
      const id = String(s.id ?? `${ext.pkg}:${s.name}`);
      out.push({
        id,
        repoId,
        name: s.name || ext.name || id,
        lang: s.lang || "en",
        kind: "manga",
        baseUrl: s.baseUrl,
        version: ext.version,
        nsfw: Boolean(ext.nsfw),
        packageName: ext.pkg,
        apkUrl: ext.apk
          ? `https://raw.githubusercontent.com/keiyoushi/extensions/repo/apk/${ext.apk}`
          : undefined,
      });
    }
  }
  return out;
}

/** LNReader plugins.min.json */
function parseLnReaderPlugins(data: unknown, repoId: string): DiscoveredSource[] {
  const list = data as Array<{
    id?: string;
    name?: string;
    site?: string;
    lang?: string;
    version?: string;
    url?: string;
    iconUrl?: string;
  }>;

  return (list || []).map((p) => ({
    id: p.id || p.name || "unknown",
    repoId,
    name: p.name || p.id || "unknown",
    lang: (p.lang || "en").split(",")[0].trim() || "en",
    kind: "novel" as const,
    baseUrl: p.site,
    version: p.version,
    iconUrl: p.iconUrl,
    nsfw: false,
    pluginUrl: p.url,
  }));
}

export async function syncRepoIndex(
  provider: ExtensionRepoProvider,
  signal?: AbortSignal
): Promise<DiscoveredSource[]> {
  const data = await fetchJson(provider.indexUrl, signal);
  switch (provider.format) {
    case "mihon_index_json":
      return parseMihonIndexJson(data, provider.id);
    case "mihon_index_min":
      return parseMihonIndexMin(data, provider.id);
    case "lnreader_plugins_min":
      return parseLnReaderPlugins(data, provider.id);
    default:
      return [];
  }
}

export async function syncAllProviders(signal?: AbortSignal): Promise<DiscoveredSource[]> {
  const results = await Promise.allSettled(
    EXTENSION_REPO_PROVIDERS.map((p) => syncRepoIndex(p, signal))
  );
  const merged: DiscoveredSource[] = [];
  for (const r of results) {
    if (r.status === "fulfilled") merged.push(...r.value);
  }
  return merged;
}

export function toManifest(s: DiscoveredSource): ExtensionManifest {
  return {
    id: s.id,
    name: s.name,
    version: s.version || "0.0.0",
    lang: s.lang,
    kind: s.kind,
    baseUrl: s.baseUrl || "",
    nsfw: s.nsfw,
    iconUrl: s.iconUrl,
  };
}

/** Measure index latency for health display (data-saver friendly HEAD/GET small) */
export async function pingIndex(url: string): Promise<{ ok: boolean; ms: number }> {
  const t0 = performance.now();
  try {
    const res = await fetch(url, { method: "GET", headers: { Accept: "application/json" } });
    const ms = Math.round(performance.now() - t0);
    return { ok: res.ok, ms };
  } catch {
    return { ok: false, ms: Math.round(performance.now() - t0) };
  }
}
