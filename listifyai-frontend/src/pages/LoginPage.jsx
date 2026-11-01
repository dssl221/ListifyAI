import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ArrowRightIcon, CheckCircleIcon } from "@heroicons/react/24/outline";
import {
  isSupabaseConfigured,
  supabase,
  supabaseConfigErrorMessage,
} from "../lib/supabaseClient";
import { useAuth } from "../context/AuthContext";
import api, { DEV_AUTH_TOKEN_KEY } from "../lib/api";
import LogoMark from "../components/ui/LogoMark";

const POST_LOGIN_PATH = "/app/dashboard";
const DEV_LOGIN_ENABLED =
  import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEV_LOGIN === "true";

const LOGIN_STATS = [
  ["13", "tag fields"],
  ["3", "priority fixes"],
  ["0-100", "quality score"],
];

function getSafeNext(value) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return POST_LOGIN_PATH;
  }
  return value;
}

function LoginPreview() {
  return (
    <div className="relative hidden lg:block">
      <div className="absolute -inset-5 rounded-[2rem] bg-[#7C5CFF]/18 blur-3xl" />
      <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#131A2A]/90 p-5 shadow-2xl shadow-[#050816]/30 backdrop-blur">
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-rose-400" />
            <span className="h-3 w-3 rounded-full bg-amber-400" />
            <span className="h-3 w-3 rounded-full bg-[#4DA3FF]" />
          </div>
          <span className="rounded-full bg-[#1A2235] px-3 py-1 text-xs font-bold text-[#A998FF]">
            secure workspace
          </span>
        </div>

        <div className="mt-5 space-y-3">
          {[
            "Copper stacking ring",
            "Organic linen tote",
            "Ceramic latte mug",
          ].map((item, index) => (
            <div
              key={item}
              className="spreadsheet-row grid grid-cols-[1fr_.5fr_.7fr] items-center gap-3 rounded-2xl border border-white/10 bg-[#1A2235]/80 p-3 text-sm"
              style={{ animationDelay: `${index * 140}ms` }}
            >
              <div>
                <p className="font-bold text-[#F3F6FF]">{item}</p>
                <p className="text-xs text-slate-400">workspace result</p>
              </div>
              <span className="rounded-full bg-[#1A2235] px-2 py-1 text-center text-xs font-black text-[#A998FF]">
                {88 + index}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/10 px-2 py-1 text-xs font-bold text-amber-300">
                <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                medium
              </span>
            </div>
          ))}
        </div>

        <div className="mt-5 grid grid-cols-3 gap-3">
          {LOGIN_STATS.map(([value, label]) => (
            <div
              key={label}
              className="rounded-2xl border border-white/10 bg-[#131A2A]/88 p-3"
            >
              <p className="text-lg font-black text-[#F3F6FF]">{value}</p>
              <p className="text-xs font-medium text-slate-400">{label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const nextPath = getSafeNext(searchParams.get("next"));

  useEffect(() => {
    if (user) navigate(nextPath, { replace: true });
  }, [user, navigate, nextPath]);

  const handleMagicLink = async (e) => {
    e.preventDefault();
    if (!isSupabaseConfigured) {
      setError(supabaseConfigErrorMessage);
      return;
    }
    setLoading(true);
    setError("");
    const redirectTo = `${window.location.origin}/auth/callback?next=${encodeURIComponent(nextPath)}`;
    const { error: err } = await supabase.auth.signInWithOtp({
      email,
      options: { emailRedirectTo: redirectTo },
    });
    setLoading(false);
    if (err) {
      setError(err.message);
    } else {
      setSent(true);
    }
  };

  const handleGoogle = async () => {
    if (!isSupabaseConfigured) {
      setError(supabaseConfigErrorMessage);
      return;
    }
    setError("");
    const redirectTo = `${window.location.origin}/auth/callback?next=${encodeURIComponent(nextPath)}`;
    const { error: err } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo },
    });
    if (err) {
      setError(err.message);
    }
  };

  const handleDevLogin = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.post("/api/auth/dev-login");
      window.localStorage.setItem(DEV_AUTH_TOKEN_KEY, data.accessToken);
      navigate(nextPath, { replace: true });
      window.location.reload();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Developer login could not reach the local backend. Start the backend with the dev profile and try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_18%_8%,rgba(124,92,255,0.18),transparent_32%),radial-gradient(circle_at_88%_0%,rgba(77,163,255,0.13),transparent_30%),linear-gradient(135deg,#0B1020_0%,#101729_48%,#0B1020_100%)] px-6 py-6">
      <header className="mx-auto flex max-w-6xl items-center justify-between">
        <Link to="/" className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <LogoMark size={24} />
            <span className="text-lg font-black tracking-tight text-[#F3F6FF]">
              Tagwing
            </span>
          </div>
        </Link>
        <Link
          to="/pricing"
          className="text-sm font-bold text-[#94A3B8] transition-colors hover:text-[#F3F6FF]"
        >
          Pricing
        </Link>
      </header>

      <main className="mx-auto grid min-h-[calc(100vh-88px)] max-w-6xl items-center gap-12 lg:grid-cols-[.9fr_1.1fr]">
        <section className="feature-card">
          <h1 className="mt-6 text-5xl font-black leading-tight tracking-tight text-[#F3F6FF]">
            Access the workspace built for stronger marketplace listings.
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-8 text-[#94A3B8]">
            Sign in to generate, review, and manage Etsy and Shopify listing
            work with saved history, quality signals, and guided
            recommendations.
          </p>

          <div className="mt-7 space-y-3">
            {[
              "Secure passwordless access",
              "Saved listing history and reusable drafts",
              "Scores and recommendations preserved with each result",
            ].map((item) => (
              <div
                key={item}
                className="flex items-center gap-3 text-sm font-semibold text-slate-300"
              >
                <CheckCircleIcon className="h-5 w-5 text-[#7C5CFF]" />
                {item}
              </div>
            ))}
          </div>
        </section>

        <section className="grid gap-6 lg:grid-cols-[1fr_.95fr] lg:items-center">
          <div className="rounded-3xl border border-white/10 bg-[#131A2A]/92 p-8 shadow-2xl shadow-[#050816]/38 backdrop-blur">
            <h2 className="text-2xl font-black text-[#F3F6FF]">Welcome back</h2>
            <p className="mt-2 text-sm text-slate-400">
              Sign in to continue your listing work.
            </p>

            {!isSupabaseConfigured && (
              <p className="mt-5 rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs font-medium text-amber-200">
                Auth is unavailable locally until Supabase env vars are added.
              </p>
            )}

            {sent ? (
              <div className="mt-8 rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235] p-5">
                <p className="text-sm font-black text-[#D8D0FF]">
                  Check your email
                </p>
                <p className="mt-1 text-sm text-[#A998FF]">
                  Open the secure link sent to <strong>{email}</strong> to
                  access your Tagwing workspace.
                </p>
              </div>
            ) : (
              <form onSubmit={handleMagicLink} className="mt-7 space-y-4">
                <div>
                  <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                    Email address
                  </label>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                  />
                </div>
                {error && (
                  <p className="text-xs font-medium text-rose-400">{error}</p>
                )}
                <button
                  type="submit"
                  disabled={loading || !isSupabaseConfigured}
                  className="group flex w-full items-center justify-center gap-2 rounded-2xl bg-[#7C5CFF] py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8] disabled:opacity-60"
                >
                  {loading
                    ? "Sending secure link..."
                    : "Email secure sign-in link"}
                  <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                </button>
              </form>
            )}

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
              onClick={handleGoogle}
              disabled={!isSupabaseConfigured}
              className="flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-700 bg-[#131A2A] py-3 text-sm font-bold text-slate-300 transition-all hover:-translate-y-0.5 hover:shadow-lg hover:shadow-[#050816]/35 disabled:opacity-60"
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
                onClick={handleDevLogin}
                disabled={loading}
                className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 py-3 text-sm font-black text-[#D8D0FF] transition-all hover:-translate-y-0.5 hover:bg-[#7C5CFF]/20 disabled:opacity-60"
              >
                Developer access
                <ArrowRightIcon className="h-4 w-4" />
              </button>
            )}
          </div>

          <LoginPreview />
        </section>
      </main>
    </div>
  );
}
