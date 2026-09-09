/**
 * Idempotent update checker against GitHub Releases.
 * - Fetches latest release once per session / cooldown
 * - Compares semver to current app version
 * - Installs only when a newer release exists (no re-download of same tag)
 */

const OWNER = "benjaminchume-droid";
const REPO = "Lumen-";
const LATEST_URL = `https://api.github.com/repos/${OWNER}/${REPO}/releases/latest`;

export interface ReleaseInfo {
  tag: string;
  name: string;
  body: string;
  publishedAt: string;
  htmlUrl: string;
  apkUrl?: string;
  apkName?: string;
  apkSize?: number;
}

export type UpdateStatus =
  | { state: "idle" }
  | { state: "checking" }
  | { state: "up_to_date"; current: string; latest: string }
  | { state: "available"; current: string; release: ReleaseInfo }
  | { state: "downloading"; release: ReleaseInfo; progress: number; speedKBps: number }
  | { state: "ready"; release: ReleaseInfo; blobUrl: string }
  | { state: "error"; message: string };

const STORAGE_KEY = "lumen.lastInstalledReleaseTag";
const COOLDOWN_MS = 5 * 60 * 1000; // 5 min between automatic checks

let lastCheckAt = 0;
let cachedLatest: ReleaseInfo | null = null;

function parseSemver(v: string): number[] {
  const clean = v.replace(/^v/i, "").split("-")[0];
  return clean.split(".").map((n) => parseInt(n, 10) || 0);
}

/** Returns true if a > b */
export function isNewer(a: string, b: string): boolean {
  const pa = parseSemver(a);
  const pb = parseSemver(b);
  const len = Math.max(pa.length, pb.length);
  for (let i = 0; i < len; i++) {
    const x = pa[i] ?? 0;
    const y = pb[i] ?? 0;
    if (x > y) return true;
    if (x < y) return false;
  }
  return false;
}

export function getInstalledTag(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function markInstalled(tag: string) {
  try {
    localStorage.setItem(STORAGE_KEY, tag);
  } catch {
    /* ignore */
  }
}

export async function fetchLatestRelease(force = false): Promise<ReleaseInfo> {
  const now = Date.now();
  if (!force && cachedLatest && now - lastCheckAt < COOLDOWN_MS) {
    return cachedLatest;
  }

  const res = await fetch(LATEST_URL, {
    headers: {
      Accept: "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
    },
  });
  if (!res.ok) {
    throw new Error(`GitHub releases API ${res.status}`);
  }
  const data = (await res.json()) as {
    tag_name?: string;
    name?: string;
    body?: string;
    published_at?: string;
    html_url?: string;
    assets?: Array<{ name: string; browser_download_url: string; size: number }>;
  };

  const apk =
    data.assets?.find((a) => a.name.toLowerCase().endsWith(".apk")) ??
    data.assets?.[0];

  const info: ReleaseInfo = {
    tag: data.tag_name || "",
    name: data.name || data.tag_name || "Release",
    body: data.body || "",
    publishedAt: data.published_at || "",
    htmlUrl: data.html_url || `https://github.com/${OWNER}/${REPO}/releases`,
    apkUrl: apk?.browser_download_url,
    apkName: apk?.name,
    apkSize: apk?.size,
  };

  cachedLatest = info;
  lastCheckAt = now;
  return info;
}

/**
 * Idempotent check: only reports "available" when latest > current
 * AND latest tag was not already marked installed.
 */
export async function checkForUpdate(
  currentVersion: string,
  force = false
): Promise<UpdateStatus> {
  try {
    const release = await fetchLatestRelease(force);
    const installed = getInstalledTag();

    // Same tag already applied → up to date (idempotent)
    if (installed && installed === release.tag) {
      return { state: "up_to_date", current: currentVersion, latest: release.tag };
    }

    if (isNewer(release.tag, currentVersion)) {
      return { state: "available", current: currentVersion, release };
    }

    return { state: "up_to_date", current: currentVersion, latest: release.tag };
  } catch (e: any) {
    return { state: "error", message: e?.message || String(e) };
  }
}

/**
 * Download APK blob with progress. Marks tag installed only after success
 * so retries are safe (idempotent).
 */
export async function downloadReleaseApk(
  release: ReleaseInfo,
  onProgress?: (pct: number, speedKBps: number) => void
): Promise<string> {
  if (!release.apkUrl) throw new Error("No APK asset on this release");

  const res = await fetch(release.apkUrl);
  if (!res.ok) throw new Error(`APK download failed: ${res.status}`);

  const total = Number(res.headers.get("content-length") || release.apkSize || 0);
  const reader = res.body?.getReader();
  if (!reader) {
    const blob = await res.blob();
    markInstalled(release.tag);
    return URL.createObjectURL(blob);
  }

  const chunks: Uint8Array[] = [];
  let received = 0;
  const t0 = performance.now();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
    received += value.length;
    const elapsed = Math.max((performance.now() - t0) / 1000, 0.001);
    const speedKBps = received / 1024 / elapsed;
    const pct = total > 0 ? Math.min(100, Math.round((received / total) * 100)) : 0;
    onProgress?.(pct, Math.round(speedKBps * 10) / 10);
  }

  const blob = new Blob(chunks as BlobPart[], {
    type: "application/vnd.android.package-archive",
  });
  markInstalled(release.tag);
  return URL.createObjectURL(blob);
}

export const APP_VERSION = "1.0.0";
