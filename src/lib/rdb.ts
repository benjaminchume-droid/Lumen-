/**
 * Remote DB helpers against resumed Lumen project (ryoewtikgwmyejrpjgnw).
 */

import { getSupabase } from "./supabase";
import type { MediaItem } from "../types";

export async function upsertSeriesRemote(item: MediaItem): Promise<{ ok: boolean; message?: string }> {
  try {
    const sb = getSupabase();
    const { error } = await sb.from("series").upsert(
      {
        title: item.title,
        description: item.description || null,
        content_type: item.type === "novel" ? "novel" : "manga",
        origin_type: "external",
        status: item.status === "completed" ? "completed" : "ongoing",
        language: "en",
        cover_url: item.coverUrl || null,
        external_key: item.id,
        updated_at: new Date().toISOString(),
      },
      { onConflict: "external_key" }
    );
    if (error) return { ok: false, message: error.message };
    return { ok: true };
  } catch (e: any) {
    return { ok: false, message: e?.message || String(e) };
  }
}

export async function fetchRecommended(limit = 24): Promise<MediaItem[]> {
  try {
    const sb = getSupabase();
    const { data, error } = await sb
      .from("series")
      .select("id,title,description,content_type,cover_url,status,external_key")
      .order("updated_at", { ascending: false })
      .limit(limit);
    if (error || !data) return [];
    return data.map((row: any) => ({
      id: row.external_key || `rdb:${row.id}`,
      title: row.title,
      type: row.content_type === "novel" ? "novel" : "manga",
      author: "",
      coverUrl: row.cover_url || "",
      description: row.description || "",
      genres: [],
      status: (row.status === "completed" ? "completed" : "ongoing") as "ongoing" | "completed",
      rating: 0,
      chaptersCount: 0,
      chapters: [],
    }));
  } catch {
    return [];
  }
}

export async function addToLibrary(opts: {
  userId?: string;
  guestKey?: string;
  externalSeriesId: string;
}) {
  const sb = getSupabase();
  return sb.from("user_library").insert({
    user_id: opts.userId || null,
    guest_key: opts.guestKey || null,
    external_series_id: opts.externalSeriesId,
    status: "reading",
  });
}

export async function upsertReaction(opts: {
  userKey: string;
  externalSeriesId: string;
  chapterId: string;
  vote: "like" | "dislike";
}) {
  const sb = getSupabase();
  return sb.from("lumen_reactions").upsert(
    {
      user_key: opts.userKey,
      external_series_id: opts.externalSeriesId,
      chapter_id: opts.chapterId,
      vote: opts.vote,
    },
    { onConflict: "user_key,external_series_id,chapter_id" }
  );
}

export async function addCommentRemote(opts: {
  userId?: string;
  externalSeriesId: string;
  chapterId: string;
  author: string;
  body: string;
}) {
  const sb = getSupabase();
  return sb.from("lumen_comments").insert({
    user_id: opts.userId || null,
    external_series_id: opts.externalSeriesId,
    chapter_id: opts.chapterId,
    author: opts.author,
    body: opts.body,
  });
}

export async function fetchComments(externalSeriesId: string, chapterId: string) {
  const sb = getSupabase();
  return sb
    .from("lumen_comments")
    .select("id,author,body,created_at")
    .eq("external_series_id", externalSeriesId)
    .eq("chapter_id", chapterId)
    .order("created_at", { ascending: false })
    .limit(50);
}

export async function recordDownload(opts: {
  userId?: string;
  guestKey?: string;
  externalSeriesId: string;
  chapterId: string;
  seriesTitle?: string;
  chapterTitle?: string;
  status?: string;
  progress?: number;
}) {
  const sb = getSupabase();
  return sb.from("user_downloads").insert({
    user_id: opts.userId || null,
    guest_key: opts.guestKey || null,
    external_series_id: opts.externalSeriesId,
    chapter_id: opts.chapterId,
    series_title: opts.seriesTitle || null,
    chapter_title: opts.chapterTitle || null,
    status: opts.status || "queued",
    progress: opts.progress ?? 0,
  });
}
