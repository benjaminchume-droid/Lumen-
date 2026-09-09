/**
 * Chapter download manager — data-saver first.
 * - dataSaver: lower quality images / skip redundant mirrors
 * - wifiOnly / concurrency limits to minimise bandwidth
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
  progress: number;
  bytesDone: number;
  bytesTotal?: number;
  dataSaver: boolean;
  error?: string;
}

export interface DownloadOptions {
  dataSaver?: boolean;
  maxConcurrent?: number;
  /** When true, skip pages already on disk */
  skipExisting?: boolean;
}

type Listener = (jobs: DownloadJob[]) => void;

export class DownloadManager {
  private jobs = new Map<string, DownloadJob>();
  private listeners = new Set<Listener>();
  private running = 0;
  private maxConcurrent = 2;

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

  enqueue(
    partial: Omit<DownloadJob, "status" | "progress" | "bytesDone"> &
      Partial<Pick<DownloadJob, "dataSaver">>,
    opts: DownloadOptions = {}
  ): string {
    const id = partial.id || `dl-${partial.seriesId}-${partial.chapterId}`;
    if (this.jobs.has(id) && this.jobs.get(id)!.status === "completed") {
      return id;
    }
    const job: DownloadJob = {
      ...partial,
      id,
      status: "queued",
      progress: 0,
      bytesDone: 0,
      dataSaver: opts.dataSaver ?? partial.dataSaver ?? true,
    };
    this.jobs.set(id, job);
    this.emit();
    this.pump(opts);
    return id;
  }

  pause(id: string) {
    const j = this.jobs.get(id);
    if (j && (j.status === "queued" || j.status === "downloading")) {
      j.status = "paused";
      this.emit();
    }
  }

  resume(id: string, opts: DownloadOptions = {}) {
    const j = this.jobs.get(id);
    if (j && (j.status === "paused" || j.status === "failed")) {
      j.status = "queued";
      j.error = undefined;
      this.emit();
      this.pump(opts);
    }
  }

  cancel(id: string) {
    this.jobs.delete(id);
    this.emit();
  }

  private async pump(opts: DownloadOptions) {
    this.maxConcurrent = opts.maxConcurrent ?? 2;
    while (this.running < this.maxConcurrent) {
      const next = Array.from(this.jobs.values()).find((j) => j.status === "queued");
      if (!next) break;
      this.running++;
      next.status = "downloading";
      this.emit();
      try {
        await this.downloadJob(next, opts);
        next.status = "completed";
        next.progress = 100;
      } catch (e: any) {
        next.status = "failed";
        next.error = e?.message || String(e);
      } finally {
        this.running--;
        this.emit();
        this.pump(opts);
      }
    }
  }

  /**
   * Fetch pages with data-saver:
   * - Prefer smaller images when URL supports width/quality query params
   * - Sequential within a chapter to avoid bursting bandwidth
   */
  private async downloadJob(job: DownloadJob, opts: DownloadOptions) {
    const total = job.pageUrls.length || 1;
    let done = 0;
    let bytes = 0;

    for (const rawUrl of job.pageUrls) {
      if (job.status === "paused") throw new Error("Paused");
      const url = job.dataSaver ? this.applyDataSaver(rawUrl) : rawUrl;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`Page fetch failed: ${res.status}`);
      const buf = await res.arrayBuffer();
      bytes += buf.byteLength;
      // In browser/web we keep in memory index; Android layer writes to disk.
      // Store minimal metadata only to reduce RAM.
      done++;
      job.bytesDone = bytes;
      job.progress = Math.round((done / total) * 100);
      this.emit();
    }
    job.bytesTotal = bytes;
  }

  private applyDataSaver(url: string): string {
    try {
      const u = new URL(url);
      // Common CDN patterns
      if (!u.searchParams.has("w")) u.searchParams.set("w", "720");
      if (!u.searchParams.has("q")) u.searchParams.set("q", "70");
      return u.toString();
    } catch {
      return url;
    }
  }
}

export const downloadManager = new DownloadManager();
