/**
 * Lumen Supabase client
 * Project: Lumen (ryoewtikgwmyejrpjgnw) — eu-west-1
 */

import { createClient, type SupabaseClient } from "@supabase/supabase-js";

const SUPABASE_URL =
  import.meta.env.VITE_SUPABASE_URL ||
  process.env.VITE_SUPABASE_URL ||
  "https://ryoewtikgwmyejrpjgnw.supabase.co";

const SUPABASE_ANON_KEY =
  import.meta.env.VITE_SUPABASE_ANON_KEY ||
  process.env.VITE_SUPABASE_ANON_KEY ||
  "";

let client: SupabaseClient | null = null;

export function getSupabase(): SupabaseClient {
  if (!client) {
    if (!SUPABASE_ANON_KEY) {
      console.warn(
        "[Lumen] VITE_SUPABASE_ANON_KEY missing — set it in .env / secrets"
      );
    }
    client = createClient(SUPABASE_URL, SUPABASE_ANON_KEY || "public-anon-placeholder");
  }
  return client;
}

export const LUMEN_SUPABASE = {
  projectId: "ryoewtikgwmyejrpjgnw",
  url: SUPABASE_URL,
  region: "eu-west-1",
};
