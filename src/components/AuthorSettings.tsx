import { useState } from "react";
import { User, Loader2, CheckCircle2, AlertTriangle } from "lucide-react";
import { getSupabase } from "../lib/supabase";

const GENRES = [
  "Action", "Adventure", "Fantasy", "Sci-Fi", "Romance", "Horror",
  "Mystery", "Drama", "Comedy", "Slice of Life", "Psychological",
];

export default function AuthorSettings({
  userId,
  isGuest,
  email,
  onUpgraded,
}: {
  userId?: string;
  isGuest: boolean;
  email?: string;
  onUpgraded?: () => void;
}) {
  const [penName, setPenName] = useState("");
  const [bio, setBio] = useState("");
  const [genres, setGenres] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  const toggleGenre = (g: string) => {
    setGenres((prev) => (prev.includes(g) ? prev.filter((x) => x !== g) : [...prev, g]));
  };

  const save = async () => {
    const name = penName.trim();
    if (name.length < 3) {
      setMsg({ ok: false, text: "Author name must be at least 3 characters" });
      return;
    }
    if (isGuest || !userId) {
      setMsg({ ok: false, text: "Sign in with email (not guest) to claim an author name." });
      return;
    }
    setBusy(true);
    setMsg(null);
    try {
      const sb = getSupabase();

      // Unique pen_name check
      const { data: existing } = await sb
        .from("creator_profiles")
        .select("user_id")
        .eq("pen_name", name)
        .maybeSingle();

      if (existing && existing.user_id !== userId) {
        setMsg({ ok: false, text: "That author name is already taken" });
        setBusy(false);
        return;
      }

      await sb.from("profiles").upsert({
        id: userId,
        username: name.toLowerCase().replace(/\s+/g, "_").slice(0, 24),
        display_name: name,
        account_type: "both",
        creator_status: "pending",
        bio: bio.trim() || null,
        updated_at: new Date().toISOString(),
      });

      await sb.from("creator_profiles").upsert({
        user_id: userId,
        pen_name: name,
        creator_bio: bio.trim() || null,
        verified: false,
        updated_at: new Date().toISOString(),
      });

      localStorage.setItem("lumen_author_genres", JSON.stringify(genres));
      setMsg({ ok: true, text: "Author profile saved. You can publish from Publish." });
      onUpgraded?.();
    } catch (e: any) {
      setMsg({ ok: false, text: e?.message || String(e) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-4 p-5 rounded-3xl border border-white/5 bg-slate-900/40 backdrop-blur-md">
      <div className="flex items-center gap-2">
        <User className="w-4 h-4 text-[#7BC6FF]" />
        <h3 className="text-sm font-semibold text-white">Author profile</h3>
      </div>
      <p className="text-xs text-slate-500">
        {isGuest
          ? "You are a guest. Sign in with email in Settings to set a unique author name and publish."
          : `Signed in as ${email || "user"}. Choose a unique author name.`}
      </p>

      <input
        value={penName}
        onChange={(e) => setPenName(e.target.value)}
        disabled={isGuest}
        placeholder="Author name (unique)"
        className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm outline-none focus:border-[#7BC6FF]/40 disabled:opacity-40"
      />
      <textarea
        value={bio}
        onChange={(e) => setBio(e.target.value)}
        disabled={isGuest}
        rows={3}
        placeholder="Short bio"
        className="w-full px-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-sm outline-none focus:border-[#7BC6FF]/40 resize-none disabled:opacity-40"
      />

      <div className="flex flex-wrap gap-2">
        {GENRES.map((g) => (
          <button
            key={g}
            type="button"
            disabled={isGuest}
            onClick={() => toggleGenre(g)}
            className={`px-2.5 py-1 rounded-lg text-[10px] font-mono border ${
              genres.includes(g)
                ? "bg-[#7BC6FF]/20 border-[#7BC6FF]/40 text-[#7BC6FF]"
                : "bg-white/5 border-white/10 text-slate-400"
            } disabled:opacity-40`}
          >
            {g}
          </button>
        ))}
      </div>

      {msg && (
        <div
          className={`flex items-center gap-2 text-xs ${
            msg.ok ? "text-emerald-400" : "text-rose-400"
          }`}
        >
          {msg.ok ? <CheckCircle2 className="w-3.5 h-3.5" /> : <AlertTriangle className="w-3.5 h-3.5" />}
          {msg.text}
        </div>
      )}

      <button
        onClick={save}
        disabled={busy || isGuest}
        className="w-full py-3 rounded-2xl bg-[#7BC6FF] text-slate-950 text-xs font-bold tracking-wide flex items-center justify-center gap-2 disabled:opacity-40"
      >
        {busy ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : null}
        Save author profile
      </button>
    </div>
  );
}
