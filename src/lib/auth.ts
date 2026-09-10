/**
 * Lumen auth — Supabase magic-link / email OTP + guest mode.
 * Project: ryoewtikgwmyejrpjgnw
 */

import { getSupabase } from "./supabase";

export type LumenSession = {
  name: string;
  email: string;
  isGuest: boolean;
  userId?: string;
};

/** Send a 6-digit OTP to email via Supabase Auth. */
export async function sendEmailOtp(email: string): Promise<{ ok: true } | { ok: false; message: string }> {
  const trimmed = email.trim().toLowerCase();
  if (!trimmed.includes("@")) {
    return { ok: false, message: "Enter a valid email address." };
  }
  try {
    const { error } = await getSupabase().auth.signInWithOtp({
      email: trimmed,
      options: {
        shouldCreateUser: true,
      },
    });
    if (error) return { ok: false, message: error.message };
    return { ok: true };
  } catch (e: any) {
    return { ok: false, message: e?.message || "Failed to send code" };
  }
}

/** Verify 6-digit email OTP. */
export async function verifyEmailOtp(
  email: string,
  token: string
): Promise<{ ok: true; session: LumenSession } | { ok: false; message: string }> {
  const trimmed = email.trim().toLowerCase();
  const code = token.replace(/\D/g, "");
  if (code.length !== 6) {
    return { ok: false, message: "Enter the 6-digit code from your email." };
  }
  try {
    const { data, error } = await getSupabase().auth.verifyOtp({
      email: trimmed,
      token: code,
      type: "email",
    });
    if (error) return { ok: false, message: error.message };
    const user = data.user;
    const name =
      user?.user_metadata?.display_name ||
      user?.email?.split("@")[0] ||
      trimmed.split("@")[0];
    return {
      ok: true,
      session: {
        name,
        email: user?.email || trimmed,
        isGuest: false,
        userId: user?.id,
      },
    };
  } catch (e: any) {
    return { ok: false, message: e?.message || "Verification failed" };
  }
}

export function guestSession(): LumenSession {
  const id =
    typeof crypto !== "undefined" && crypto.randomUUID
      ? crypto.randomUUID()
      : `guest-${Date.now()}`;
  return {
    name: "Guest",
    email: `guest-${id.slice(0, 8)}@lumen.local`,
    isGuest: true,
    userId: undefined,
  };
}

/**
 * Email template copy for Supabase Dashboard → Authentication → Email Templates → Magic Link / OTP
 */
export const LUMEN_OTP_EMAIL_TEMPLATE = `
<h2>Your Lumen code</h2>
<p>Use this 6-digit code to sign in to Lumen Reading OS:</p>
<p style="font-size:28px;letter-spacing:8px;font-weight:bold;">{{ .Token }}</p>
<p>This code expires in 1 hour. If you did not request it, you can ignore this email.</p>
<p>— Lumen</p>
`.trim();
