import axios from "axios";
import { supabase } from "./supabaseClient";

export const DEV_AUTH_TOKEN_KEY = "tagwing.devAccessToken";

const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();
const hasProductionApi = (() => {
  try {
    const url = new URL(configuredApiUrl);
    return url.protocol === "https:" &&
      !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname);
  } catch {
    return false;
  }
})();

const api = axios.create({
  baseURL: import.meta.env.DEV
    ? configuredApiUrl || "http://127.0.0.1:8080"
    : hasProductionApi ? configuredApiUrl : undefined,
});

api.interceptors.request.use(async (config) => {
  if (import.meta.env.PROD && !hasProductionApi) {
    throw new Error("Backend is not configured. Set VITE_API_URL to your hosted HTTPS backend URL and rebuild the frontend.");
  }
  const {
    data: { session },
  } = await supabase.auth.getSession();
  if (session?.access_token) {
    config.headers.Authorization = `Bearer ${session.access_token}`;
  } else {
    const devToken = window.localStorage.getItem(DEV_AUTH_TOKEN_KEY);
    if (devToken) {
      config.headers.Authorization = `Bearer ${devToken}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      const devToken = window.localStorage.getItem(DEV_AUTH_TOKEN_KEY);
      if (!session && !devToken) {
        const loginPath = window.location.pathname.startsWith("/admin")
          ? "/tw-secure-admin-access"
          : "/login";
        const next = `${window.location.pathname}${window.location.search}`;
        window.location.href = `${loginPath}?next=${encodeURIComponent(next)}`;
      }
    }

    return Promise.reject(error);
  },
);

export default api;
