import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import { supabase } from "../lib/supabaseClient";
import api from "../lib/api";

const CALLBACK_TIMEOUT_MS = 8000;
const ADMIN_LOGIN_INTENT_KEY = "tagwing.adminLoginIntent";

function getHashParams() {
  return new URLSearchParams(window.location.hash.replace(/^#/, ""));
}

function withTimeout(promise) {
  return Promise.race([
    promise,
    new Promise((_, reject) => {
      window.setTimeout(() => {
        reject(new Error("Sign in is taking too long. Please try again."));
      }, CALLBACK_TIMEOUT_MS);
    }),
  ]);
}

export default function AuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState("");
  const isAdminLogin =
    searchParams.get("admin") === "1" ||
    window.sessionStorage.getItem(ADMIN_LOGIN_INTENT_KEY) === "1";

  useEffect(() => {
    let mounted = true;

    const completeSignIn = async () => {
      const hashParams = getHashParams();
      const authError =
        searchParams.get("error_description") ||
        searchParams.get("error") ||
        hashParams.get("error_description") ||
        hashParams.get("error");
      if (authError) {
        throw new Error(authError);
      }

      const code = searchParams.get("code");
      const accessToken = hashParams.get("access_token");
      const refreshToken = hashParams.get("refresh_token");

      if (code) {
        const { error: exchangeError } =
          await supabase.auth.exchangeCodeForSession(code);
        if (exchangeError) {
          const { data } = await supabase.auth.getSession();
          if (!data.session) {
            throw exchangeError;
          }
        }
      } else if (accessToken && refreshToken) {
        const { error: setSessionError } = await supabase.auth.setSession({
          access_token: accessToken,
          refresh_token: refreshToken,
        });
        if (setSessionError) {
          throw setSessionError;
        }
      }

      const { data, error: sessionError } = await supabase.auth.getSession();
      if (sessionError) {
        throw sessionError;
      }

      return data.session;
    };

    withTimeout(completeSignIn())
      .then(async (session) => {
        if (!mounted) return;
        if (!session) {
          navigate("/login", { replace: true });
          return;
        }

        const { data: profile } = await api.get("/api/me");
        if (!mounted) return;

        if (profile.role === "admin") {
          window.sessionStorage.removeItem(ADMIN_LOGIN_INTENT_KEY);
          navigate("/admin/dashboard", { replace: true });
        } else if (isAdminLogin) {
          await supabase.auth.signOut();
          window.sessionStorage.removeItem(ADMIN_LOGIN_INTENT_KEY);
          navigate("/tw-secure-admin-access?denied=1", { replace: true });
        } else {
          navigate("/app/generate", { replace: true });
        }
      })
      .catch(async (err) => {
        if (!mounted) return;
        if (isAdminLogin) {
          await supabase.auth.signOut();
          window.sessionStorage.removeItem(ADMIN_LOGIN_INTENT_KEY);
          navigate("/tw-secure-admin-access?denied=1", { replace: true });
          return;
        }
        setError(err.message || "Unable to complete sign in.");
      });

    return () => {
      mounted = false;
    };
  }, [isAdminLogin, navigate, searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#1A2235] px-4">
      <div className="rounded-3xl border border-slate-700 bg-[#131A2A] p-8 text-center shadow-sm">
        {error ? (
          <>
            <h1 className="text-xl font-black text-[#F3F6FF]">
              Sign in could not finish
            </h1>
            <p className="mt-2 max-w-sm text-sm text-slate-400">{error}</p>
            <button
              type="button"
              onClick={() =>
                navigate(isAdminLogin ? "/tw-secure-admin-access" : "/login", {
                  replace: true,
                })
              }
              className="mt-5 rounded-2xl bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF]"
            >
              Back to login
            </button>
          </>
        ) : (
          <>
            <LoadingSpinner label="Completing sign in..." />
            <p className="mt-4 text-sm font-semibold text-slate-400">
              Completing sign in...
            </p>
          </>
        )}
      </div>
    </div>
  );
}
