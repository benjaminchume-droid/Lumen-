/**
 * Lumen Supabase client
 * Project: Lumen (ryoewtikgwmyejrpjgnw) — eu-west-1
 * Connected via Supabase2. Prefer VITE_ env vars; keys below are safe publishable/anon fallbacks.
 */

import { createClient, type SupabaseClient } from "@supabase/supabase-js";

const SUPABASE_URL =
  import.meta.env.VITE_SUPABASE_URL ||
  process.env.VITE_SUPABASE_URL ||
  "https://ryoewtikgwmyejrpjgnw.supabase.co";

/** Legacy anon JWT (still valid) */
const ANON_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ5b2V3dGlrZ3dteWVqcnBqZ253Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MDg3MzYsImV4cCI6MjEwMjI4NDczNn0.KNa80UeXpIuPlVEt5sFIq4TyYtIc2ykWe-k24m7O7Pg";

/** Modern publishable key */
const PUBLISHABLE_KEY = "sb_publishable_L-ks4UN5chbWNQvuwqgzQQ_MtifvX4M";

const SUPABASE_ANON_KEY =
  import.meta.env.VITE_SUPABASE_ANON_KEY ||
  process.env.VITE_SUPABASE_ANON_KEY ||
  ANON_KEY;

let client: SupabaseClient | null = null;

export function getSupabase(): SupabaseClient {
  if (!client) {
    client = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    });
  }
  return client;
}

export const LUMEN_SUPABASE = {
  projectId: "ryoewtikgwmyejrpjgnw",
  url: SUPABASE_URL,
  region: "eu-west-1",
  publishableKey: PUBLISHABLE_KEY,
} as const;
