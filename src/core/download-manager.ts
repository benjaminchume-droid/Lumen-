/**
 * Chapter download manager — data-saver first.
 * Pause / resume, progress %, pages done, bytes, speed (KB/s), ETA.
 */

export type DownloadStatus =
  | "queued"
  | "downloading"
  | "completed"
  | "failed"
  | "paused";

export interface DownloadJob {
  id: string;
  seriesId: string;
  chapterId: string;
  chapterNumber: number;
  title: string;
  pageUrls: string[];
  status: DownloadStatus;
  /** 0–100 */
  progress: number;
  pagesDone: number;
  pagesTotal: number;
  bytesDone: number;
  bytesTotal?: number;
  /** Instantaneous / smoothed KB/s */
  speedKBps: number;
  /** Estimated seconds remaining */
  etaSeconds?: number;
  dataSaver: boolean;
  error?: string;
  startedAt?: number;
  finishedAt?: number;
}

export interface DownloadOptions {
  dataSaver?: boolean;
  maxConcurrent?: number;
  skipExisting?: boolean;
  maxImageWidth?: number;
  quality?: number;
}

type Listener = (jobs: DownloadJob[]) => void;

function formatSpeed(kbps: number): string {
  if (kbps < 1024) return `${kbps.toFixed(0)} KB/s`;
  return `${(kbps / 1024).toFixed(1)} MB/s`;
}

export function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(2)} MB`;
}

export function formatEta(seconds?: number): string {
  if (seconds == null || !Number.isFinite(seconds)) return "—";
  if (seconds < 60) return `${Math.ceil(seconds)}s`;
  const m = Math.floor(seconds / 60);
  const s = Math.ceil(seconds % 60);
  return `${m}m ${s}s`;
}

export { formatSpeed };

export class DownloadManager {
  private jobs = new Map<string, DownloadJob>();
  private listeners = new Set<Listener>();
  private running = 0;
  private maxConcurrent = 2;
  private abortControllers = new Map<string, AbortController>();

  subscribe(fn: Listener): () => void {
    this.listeners.add(fn);
    fn(this.list());
    return () => this.listeners.delete(fn);
  }

  private emit() {
    const snapshot = this.list();
    for (const fn of this.listeners) fn(snapshot);
  }

  list(): DownloadJob[] {
    return Array.from(this.jobs.values());
  }

  get(id: string): DownloadJob | undefined {
    return this.jobs.get(id);
  }

  enqueue(
    partial: Omit<
      DownloadJob,
      | "status"
      | "progress"
      | "bytesDone"
      | "pagesDone"
      | "pagesTotal"
      | "speedKBps"
    > &
      Partial<Pick<DownloadJob, "dataSaver">>,
    opts: DownloadOptions = {}
  ): string {
    const id = partial.id || `dl-${partial.seriesId}-${partial.chapterId}`;
    const existing = this.jobs.get(id);
    if (existing?.status === "completed") return id;
    if (existing?.status === "downloading" || existing?.status === "queued") return id;

    const pagesTotal = partial.pageUrls?.length || 0;
    const job: DownloadJob = {
      ...partial,
      id,
      status: "queued",
      progress: 0,
      pagesDone: 0,
      pagesTotal,
      bytesDone: 0,
      speedKBps: 0,
      dataSaver: opts.dataSaver ?? partial.dataSaver ?? true,
    };
    this.jobs.set(id, job);
    this.emit();
    this.pump(opts);
    return id;
  }

  pause(id: string) {
    const j = this.jobs.get(id);
    if (!j) return;
    if (j.status === "queued" || j.status === "downloading") {
      j.status = "paused";
      this.abortControllers.get(id)?.abort();
      this.abortControllers.delete(id);
      this.emit();
    }
  }

  resume(id: string, opts: DownloadOptions = {}) {
    const j = this.jobs.get(id);
    if (!j) return;
    if (j.status === "paused" || j.status === "failed") {
      j.status = "queued";
      j.error = undefined;
      this.emit();
      this.pump(opts);
    }
  }

  cancel(id: string) {
    this.abortControllers.get(id)?.abort();
    this.abortControllers.delete(id);
    this.jobs.delete(id);
    this.emit();
  }

  pauseAll() {
    for (const j of this.jobs.values()) {
      if (j.status === "queued" || j.status === "downloading") {
        j.status = "paused";
        this.abortControllers.get(j.id)?.abort();
        this.abortControllers.delete(j.id);
      }
    }
    this.emit();
  }

  resumeAll(opts: DownloadOptions = {}) {
    for (const j of this.jobs.values()) {
      if (j.status === "paused" || j.status === "failed") {
        j.status = "queued";
        j.error = undefined;
      }
    }
    this.emit();
    this.pump(opts);
  }

  private async pump(opts: DownloadOptions) {
    this.maxConcurrent = opts.maxConcurrent ?? 2;
    while (this.running < this.maxConcurrent) {
      const next = Array.from(this.jobs.values()).find((j) => j.status === "queued");
      if (!next) break;
      this.running++;
      next.status = "downloading";
      next.startedAt = next.startedAt ?? Date.now();
      this.emit();
      const ac = new AbortController();
      this.abortControllers.set(next.id, ac);
      try {
        await this.downloadJob(next, opts, ac.signal);
        if (next.status === "paused") {
          /* leave paused */
        } else {
          next.status = "completed";
          next.progress = 100;
          next.pagesDone = next.pagesTotal;
          next.finishedAt = Date.now();
          next.speedKBps = 0;
          next.etaSeconds = 0;
        }
      } catch (e: any) {
        if (next.status === "paused" || ac.signal.aborted) {
          next.status = "paused";
        } else {
          next.status = "failed";
          next.error = e?.message || String(e);
        }
      } finally {
        this.abortControllers.delete(next.id);
        this.running--;
        this.emit();
        this.pump(opts);
      }
    }
  }

  private async downloadJob(
    job: DownloadJob,
    opts: DownloadOptions,
    signal: AbortSignal
  ) {
    const total = job.pageUrls.length || 1;
    job.pagesTotal = total;
    let done = job.pagesDone || 0;
    let bytes = job.bytesDone || 0;
    const maxW = opts.maxImageWidth ?? (job.dataSaver ? 720 : 1600);
    const q = opts.quality ?? (job.dataSaver ? 65 : 85);

    // Resume from last completed page
    const urls = job.pageUrls.slice(done);
    const windowStart = Date.now();
    let windowBytes = 0;

    for (const rawUrl of urls) {
      if (signal.aborted || job.status === "paused") {
        job.status = "paused";
        throw new Error("Paused");
      }

      const url = job.dataSaver ? this.applyDataSaver(rawUrl, maxW, q) : rawUrl;
      const t0 = performance.now();
      const res = await fetch(url, {
        signal,
        headers: {
          Accept: "image/*,*/*;q=0.8",
          "Accept-Encoding": "gzip, deflate, br",
        },
      });
      if (!res.ok) throw new Error(`Page fetch failed: ${res.status}`);
      const buf = await res.arrayBuffer();
      const dt = Math.max((performance.now() - t0) / 1000, 0.001);
      const pageBytes = buf.byteLength;
      bytes += pageBytes;
      windowBytes += pageBytes;
      done++;

      const elapsedWindow = Math.max((Date.now() - windowStart) / 1000, 0.001);
      const speedKBps = windowBytes / 1024 / elapsedWindow;
      const remaining = total - done;
      const avgPerPage = bytes / done;
      const etaSeconds =
        speedKBps > 0 ? (remaining * avgPerPage) / 1024 / speedKBps : undefined;

      job.pagesDone = done;
      job.bytesDone = bytes;
      job.progress = Math.round((done / total) * 100);
      job.speedKBps = Math.round(speedKBps * 10) / 10;
      job.etaSeconds = etaSeconds != null ? Math.max(0, Math.round(etaSeconds)) : undefined;
      this.emit();

      // Tiny yield so UI can paint
      await new Promise((r) => setTimeout(r, 0));
    }

    job.bytesTotal = bytes;
  }

  private applyDataSaver(url: string, maxWidth: number, quality: number): string {
    try {
      const u = new URL(url);
      if (!u.searchParams.has("w") && !u.searchParams.has("width")) {
        u.searchParams.set("w", String(maxWidth));
      }
      if (!u.searchParams.has("q") && !u.searchParams.has("quality")) {
        u.searchParams.set("q", String(quality));
      }
      if (!u.searchParams.has("format") && !u.searchParams.has("fm")) {
        u.searchParams.set("format", "webp");
      }
      return u.toString();
    } catch {
      return url;
    }
  }
}

export const downloadManager = new DownloadManager();
