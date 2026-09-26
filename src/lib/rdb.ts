/**
 * Remote DB (Supabase) helpers for series that already have stable lumen IDs.
 * Guests stay local-only; signed-in users can mirror library rows.
 */

import { getSupabase } from "./supabase";
import type { MediaItem } from "../types";

export async function upsertSeriesRemote(item: MediaItem, userId?: string): Promise<{ ok: boolean; message?: string }> {
  if (!userId) return { ok: false, message: "Sign in to sync" };
  try {
    const sb = getSupabase();
    const { error } = await sb.from("series").upsert(
      {
        // Prefer external_id column if present; fall back to title match fields
        title: item.title,
        description: item.description || null,
        content_type: item.type === "novel" ? "novel" : "manga",
        origin_type: "external",
        status: item.status,
        language: "en",
        cover_url: item.coverUrl || null,
        external_key: item.id,
        updated_at: new Date().toISOString(),
      },
      { onConflict: "external_key" }
    );
    if (error) {
      // Table may not have external_key yet — soft fail, local remains source of truth
      return { ok: false, message: error.message };
    }
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
