import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ArrowRightIcon } from "@heroicons/react/24/outline";
import {
  isSupabaseConfigured,
  supabase,
  supabaseConfigErrorMessage,
} from "../../lib/supabaseClient";
import { useAuth } from "../../context/AuthContext";
import api, { DEV_AUTH_TOKEN_KEY } from "../../lib/api";
import LogoMark from "../../components/ui/LogoMark";

const ACCESS_DENIED_MESSAGE = "You are not an admin. Request denied.";
const DEV_LOGIN_ENABLED =
  import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEV_LOGIN === "true";
const ADMIN_LOGIN_INTENT_KEY = "tagwing.adminLoginIntent";

export default function AdminLoginPage() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    if (searchParams.get("denied") === "1") {
      setError(ACCESS_DENIED_MESSAGE);
      navigate("/tw-secure-admin-access", { replace: true });
    }
  }, [navigate, searchParams]);

  useEffect(() => {
    let active = true;

    const verifyAdmin = async () => {
      if (!user) return;

      setChecking(true);
      setError("");
      try {
        const { data } = await api.get("/api/me");
        if (!active) return;

        if (data.role === "admin") {
          navigate("/admin/dashboard", { replace: true });
          return;
        }

        await signOut();
        if (active) {
          setError(ACCESS_DENIED_MESSAGE);
        }
      } catch {
        await signOut();
        if (active) {
          setError(ACCESS_DENIED_MESSAGE);
        }
      } finally {
        if (active) {
          setChecking(false);
        }
      }
    };

    verifyAdmin();

    return () => {
      active = false;
    };
  }, [navigate, signOut, user]);

  const handleMagicLink = async (event) => {
    event.preventDefault();
    setSent(false);
    if (!isSupabaseConfigured) {
      setError(supabaseConfigErrorMessage);
      return;
    }

    setLoading(true);
    setError("");
    await supabase.auth.signOut();
    window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
    window.sessionStorage.setItem(ADMIN_LOGIN_INTENT_KEY, "1");
    const redirectTo = `${window.location.origin}/auth/callback?admin=1`;
    const { error: signInError } = await supabase.auth.signInWithOtp({
      email,
      options: { emailRedirectTo: redirectTo },
    });
    setLoading(false);

    if (signInError) {
      setError(signInError.message);
    } else {
      setSent(true);
    }
  };

  const handleGoogle = async () => {
    setSent(false);
    if (!isSupabaseConfigured) {
      setError(supabaseConfigErrorMessage);
      return;
    }

    setLoading(true);
    setError("");
    await supabase.auth.signOut();
    window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
    window.sessionStorage.setItem(ADMIN_LOGIN_INTENT_KEY, "1");
    const redirectTo = `${window.location.origin}/auth/callback?admin=1`;
    const { error: signInError } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo,
        queryParams: { prompt: "select_account" },
      },
    });

    if (signInError) {
      setError(signInError.message);
      setLoading(false);
    }
  };

  const handleDevAdminLogin = async () => {
    setLoading(true);
    setError("");
    try {
      let response;
      try {
        response = await api.post("/api/auth/dev-login?admin=true");
      } catch (err) {
        if (err.response?.status !== 404) {
          throw err;
        }
        response = await api.post("/api/auth/dev-admin-login");
      }

      const { data } = response;
      window.localStorage.setItem(DEV_AUTH_TOKEN_KEY, data.accessToken);
      const profileResponse = await api.get("/api/me");

      if (profileResponse.data.role !== "admin") {
        window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
        setError(ACCESS_DENIED_MESSAGE);
        setLoading(false);
        return;
      }

      window.location.href = "/admin/dashboard";
    } catch (err) {
      window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
      const backendMessage =
        err.response?.status === 404 ? "" : err.response?.data?.message;
      setError(
        backendMessage ||
          "Developer admin access is unavailable. Start the backend with the dev profile and try again.",
      );
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0B1020] px-4 text-[#F3F6FF]">
      <div className="mx-auto w-full max-w-sm rounded-2xl border border-white/10 bg-[#131A2A] p-8 shadow-2xl shadow-black/30">
        <div className="flex justify-center">
          <LogoMark size={28} />
        </div>

        <p className="mt-5 text-center text-sm font-bold uppercase tracking-wide text-slate-500">
          Admin Access
        </p>

        {checking ? (
          <p className="mt-6 text-center text-sm font-semibold text-slate-400">
            Checking access...
          </p>
        ) : sent ? (
          <div className="mt-6 rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235] p-4">
            <p className="text-sm font-black text-[#D8D0FF]">
              Check your email
            </p>
            <p className="mt-1 text-sm text-[#A998FF]">
              Open the secure link sent to <strong>{email}</strong>.
            </p>
          </div>
        ) : (
          <>
            <form onSubmit={handleMagicLink} className="mt-6 space-y-4">
              <div>
                <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                  Email address
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  className="w-full rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                />
              </div>

              {error && (
                <p className="rounded-xl border border-rose-400/20 bg-rose-500/10 px-3 py-2 text-xs font-semibold text-rose-300">
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={loading || !isSupabaseConfigured}
                className="group flex w-full items-center justify-center gap-2 rounded-2xl bg-[#7C5CFF] py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? "Sending..." : "Send Magic Link"}
                <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </button>
            </form>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-white/10" />
              </div>
              <div className="relative flex justify-center">
                <span className="bg-[#131A2A] px-3 text-xs font-bold text-slate-500">
                  or
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={handleGoogle}
              disabled={loading || !isSupabaseConfigured}
              className="flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-700 bg-[#131A2A] py-3 text-sm font-bold text-slate-300 transition-all hover:-translate-y-0.5 hover:shadow-lg hover:shadow-[#050816]/35 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <img
                src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
                alt=""
                className="h-4 w-4"
              />
              Continue with Google
            </button>

            {DEV_LOGIN_ENABLED && (
              <button
                type="button"
                onClick={handleDevAdminLogin}
                disabled={loading}
                className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 py-3 text-sm font-black text-[#D8D0FF] transition-all hover:-translate-y-0.5 hover:bg-[#7C5CFF]/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                Developer admin access
                <ArrowRightIcon className="h-4 w-4" />
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}
