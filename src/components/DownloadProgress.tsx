import { Pause, Play, X, Download, Wifi } from "lucide-react";
import {
  downloadManager,
  formatBytes,
  formatEta,
  formatSpeed,
  type DownloadJob,
} from "../core/download-manager";

interface DownloadProgressProps {
  job: DownloadJob;
  onClose?: () => void;
}

export default function DownloadProgress({ job, onClose }: DownloadProgressProps) {
  const statusColor =
    job.status === "completed"
      ? "text-emerald-400"
      : job.status === "failed"
      ? "text-rose-400"
      : job.status === "paused"
      ? "text-amber-400"
      : "text-[#7BC6FF]";

  return (
    <div className="p-4 rounded-2xl border border-white/10 bg-slate-900/70 backdrop-blur-md space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-0.5">
          <div className="flex items-center gap-2">
            <Download className={`w-4 h-4 shrink-0 ${statusColor}`} />
            <h4 className="text-sm font-semibold text-white truncate">{job.title}</h4>
          </div>
          <p className="text-[10px] font-mono text-slate-500">
            Ch. {job.chapterNumber} · {job.pagesDone}/{job.pagesTotal} pages ·{" "}
            <span className={`uppercase ${statusColor}`}>{job.status}</span>
          </p>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {(job.status === "downloading" || job.status === "queued") && (
            <button
              onClick={() => downloadManager.pause(job.id)}
              className="p-2 rounded-full border border-white/10 hover:bg-white/10 cursor-pointer"
              title="Pause"
            >
              <Pause className="w-3.5 h-3.5 text-amber-300" />
            </button>
          )}
          {(job.status === "paused" || job.status === "failed") && (
            <button
              onClick={() => downloadManager.resume(job.id)}
              className="p-2 rounded-full border border-white/10 hover:bg-white/10 cursor-pointer"
              title="Resume"
            >
              <Play className="w-3.5 h-3.5 text-emerald-300" />
            </button>
          )}
          <button
            onClick={() => {
              downloadManager.cancel(job.id);
              onClose?.();
            }}
            className="p-2 rounded-full border border-white/10 hover:bg-white/10 cursor-pointer"
            title="Cancel"
          >
            <X className="w-3.5 h-3.5 text-slate-400" />
          </button>
        </div>
      </div>

      {/* Progress bar */}
      <div className="h-2 rounded-full bg-white/10 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-300 ${
            job.status === "failed"
              ? "bg-rose-500"
              : job.status === "paused"
              ? "bg-amber-400"
              : job.status === "completed"
              ? "bg-emerald-400"
              : "bg-[#7BC6FF]"
          }`}
          style={{ width: `${Math.min(100, job.progress)}%` }}
        />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-[10px] font-mono text-slate-400">
        <span className="text-white font-semibold">{job.progress}%</span>
        <span className="flex items-center gap-1">
          <Wifi className="w-3 h-3" />
          {job.status === "downloading" ? formatSpeed(job.speedKBps) : "—"}
        </span>
        <span>
          {formatBytes(job.bytesDone)}
          {job.bytesTotal ? ` / ${formatBytes(job.bytesTotal)}` : ""}
        </span>
        <span>ETA {formatEta(job.etaSeconds)}</span>
      </div>

      {job.error && (
        <p className="text-[10px] text-rose-300 font-mono break-all">{job.error}</p>
      )}

      {job.dataSaver && (
        <p className="text-[9px] uppercase tracking-wider text-[#7BC6FF]/80 font-mono">
          Data-saver on · max width 720 · webp preferred
        </p>
      )}
    </div>
  );
}
