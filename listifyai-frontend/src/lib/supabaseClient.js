import { createClient } from "@supabase/supabase-js";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnon = import.meta.env.VITE_SUPABASE_ANON_KEY;
const missingConfigMessage =
  "Supabase auth is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in your frontend .env file.";

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseAnon);
export const supabaseConfigErrorMessage = missingConfigMessage;

const fallbackSupabaseClient = {
  auth: {
    async getSession() {
      return { data: { session: null }, error: null };
    },
    async exchangeCodeForSession() {
      return {
        data: { session: null },
        error: new Error(missingConfigMessage),
      };
    },
    async setSession() {
      return {
        data: { session: null },
        error: new Error(missingConfigMessage),
      };
    },
    onAuthStateChange() {
      return {
        data: {
          subscription: {
            unsubscribe() {},
          },
        },
      };
    },
    async signOut() {
      return { error: null };
    },
    async signInWithOtp() {
      return { data: null, error: new Error(missingConfigMessage) };
    },
    async signInWithOAuth() {
      return { data: null, error: new Error(missingConfigMessage) };
    },
  },
};

function getSupabaseClient() {
  if (!isSupabaseConfigured) {
    return fallbackSupabaseClient;
  }

  if (typeof window === "undefined") {
    return createClient(supabaseUrl, supabaseAnon);
  }

  if (!window.__listifySupabaseClient) {
    window.__listifySupabaseClient = createClient(supabaseUrl, supabaseAnon);
  }

  return window.__listifySupabaseClient;
}

export const supabase = getSupabaseClient();
