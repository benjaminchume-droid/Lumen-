/**
 * Publish / upload series + chapters to Lumen Supabase.
 * Uses existing tables: series, chapters, chapter_content, chapter_pages, creator_profiles.
 */
import { useState } from "react";
import { Upload, BookOpen, Image as ImageIcon, Loader2, CheckCircle2, AlertTriangle } from "lucide-react";
import { getSupabase } from "../lib/supabase";

type ContentKind = "novel" | "manga" | "comic";

export default function PublishView({ userId }: { userId?: string }) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [kind, setKind] = useState<ContentKind>("novel");
  const [chapterTitle, setChapterTitle] = useState("Chapter 1");
  const [chapterBody, setChapterBody] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  const submit = async () => {
    if (!title.trim()) {
      setMsg({ ok: false, text: "Title is required" });
      return;
    }
    if (!userId) {
      setMsg({ ok: false, text: "Sign in with email to publish (guest cannot publish)." });
      return;
    }
    setBusy(true);
    setMsg(null);
    try {
      const sb = getSupabase();
      const contentType =
        kind === "novel" ? "novel" : kind === "manga" ? "manga" : "comic";

      const { data: series, error: sErr } = await sb
        .from("series")
        .insert({
          title: title.trim(),
          description: description.trim() || null,
          content_type: contentType,
          origin_type: "lumen",
          status: "ongoing",
          language: "en",
        })
        .select("id")
        .single();

      if (sErr) throw sErr;

      // Link creator if profile exists
      await sb.from("series_creators").insert({
        series_id: series.id,
        creator_id: userId,
        role: "author",
        display_name: null,
        credit_order: 0,
      });

      const { data: chapter, error: cErr } = await sb
        .from("chapters")
        .insert({
          series_id: series.id,
          chapter_number: 1,
          title: chapterTitle.trim() || "Chapter 1",
          content_type: kind === "novel" ? "text" : "images",
          status: "published",
          published_at: new Date().toISOString(),
        })
        .select("id")
        .single();

      if (cErr) throw cErr;

      if (kind === "novel" && chapterBody.trim()) {
        const words = chapterBody.trim().split(/\s+/).length;
        await sb.from("chapter_content").upsert({
          chapter_id: chapter.id,
          content: chapterBody,
          word_count: words,
          content_version: 1,
        });
      }

      setMsg({ ok: true, text: `Published “${title}” with chapter 1.` });
      setTitle("");
      setDescription("");
      setChapterBody("");
    } catch (e: any) {
      setMsg({ ok: false, text: e?.message || String(e) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0A0C0F] text-white pb-32 pt-12 px-6 max-w-2xl mx-auto space-y-6">
      <div>
        <span className="text-xs uppercase font-mono tracking-[0.2em] text-[#7BC6FF] font-semibold">Creator</span>
        <h1 className="text-3xl font-semibold tracking-tight">Publish</h1>
        <p className="text-xs text-slate-500 mt-1">Upload novel / manga / comic to your Lumen library</p>
      </div>

      <div className="flex gap-2 p-1 rounded-2xl bg-slate-950 border border-white/10">
        {(["novel", "manga", "comic"] as ContentKind[]).map((k) => (
          <button
            key={k}
            onClick={() => setKind(k)}
            className={`flex-1 py-2 rounded-xl text-xs font-mono uppercase tracking-wider ${
              kind === k ? "bg-[#7BC6FF] text-slate-950 font-bold" : "text-slate-400"
            }`}
          >
            {k}
          </button>
        ))}
      </div>

      <label className="block space-y-1">
        <span className="text-[10px] font-mono text-slate-500 uppercase">Title</span>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm focus:border-[#7BC6FF]/40 outline-none"
          placeholder="Series title"
        />
      </label>

      <label className="block space-y-1">
        <span className="text-[10px] font-mono text-slate-500 uppercase">Description</span>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm focus:border-[#7BC6FF]/40 outline-none resize-none"
          placeholder="Synopsis"
        />
      </label>

      <label className="block space-y-1">
        <span className="text-[10px] font-mono text-slate-500 uppercase">First chapter title</span>
        <input
          value={chapterTitle}
          onChange={(e) => setChapterTitle(e.target.value)}
          className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm focus:border-[#7BC6FF]/40 outline-none"
        />
      </label>

      {kind === "novel" && (
        <label className="block space-y-1">
          <span className="text-[10px] font-mono text-slate-500 uppercase flex items-center gap-1">
            <BookOpen className="w-3 h-3" /> Chapter text
          </span>
          <textarea
            value={chapterBody}
            onChange={(e) => setChapterBody(e.target.value)}
            rows={8}
            className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm focus:border-[#7BC6FF]/40 outline-none resize-y font-serif"
            placeholder="Paste chapter content…"
          />
        </label>
      )}

      {kind !== "novel" && (
        <div className="p-4 rounded-2xl border border-dashed border-white/15 bg-white/5 text-center text-xs text-slate-400 space-y-2">
          <ImageIcon className="w-6 h-6 mx-auto text-[#7BC6FF]" />
          <p>Page image upload uses Supabase Storage (`chapter_pages.storage_path`). Wire file picker after Storage bucket `lumen-pages` is created.</p>
        </div>
      )}

      {msg && (
        <div
          className={`flex items-center gap-2 text-xs p-3 rounded-xl border ${
            msg.ok
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              : "border-rose-500/30 bg-rose-500/10 text-rose-400"
          }`}
        >
          {msg.ok ? <CheckCircle2 className="w-4 h-4" /> : <AlertTriangle className="w-4 h-4" />}
          {msg.text}
        </div>
      )}

      <button
        onClick={submit}
        disabled={busy}
        className="w-full py-4 rounded-2xl bg-[#7BC6FF] text-slate-950 font-bold text-sm tracking-wide flex items-center justify-center gap-2 disabled:opacity-50"
      >
        {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
        {busy ? "Publishing…" : "Publish series"}
      </button>
    </div>
  );
}
