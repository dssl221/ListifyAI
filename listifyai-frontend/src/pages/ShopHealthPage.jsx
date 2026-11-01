import { useEffect, useState } from "react";
import {
  ArrowPathIcon,
  ChartBarIcon,
  HeartIcon,
  SparklesIcon,
} from "@heroicons/react/24/outline";
import api from "../lib/api";
import { useUsage } from "../context/UsageContext";
import UpgradeModal from "../components/ui/UpgradeModal";

const progressSteps = [
  "Reading your listing history",
  "Checking tag patterns",
  "Reviewing quality scores",
  "Building your health report",
];

const gradeClasses = {
  A: "bg-emerald-500/10 text-emerald-300",
  B: "bg-[#7C5CFF]/10 text-[#A998FF]",
  C: "bg-amber-500/10 text-amber-300",
  D: "bg-rose-500/10 text-rose-300",
  "?": "bg-slate-500/10 text-slate-400",
};

const statusClasses = {
  strong: "bg-emerald-500/10 text-emerald-300",
  needs_work: "bg-amber-500/10 text-amber-300",
  critical: "bg-rose-500/10 text-rose-300",
};

function formatStatus(status) {
  if (status === "needs_work") return "Needs work";
  if (status === "critical") return "Critical";
  return "Strong";
}

function timeAgo(value) {
  if (!value) return "not computed yet";
  const diff = Date.now() - new Date(value).getTime();
  const minutes = Math.max(0, Math.floor(diff / 60000));
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}

function ScoreRing({ score = 0 }) {
  if (score < 0) {
    return (
      <div
        className="grid h-36 w-36 shrink-0 place-items-center rounded-full"
        style={{ background: "conic-gradient(#374151 360deg)" }}
      >
        <div className="grid h-28 w-28 place-items-center rounded-full bg-[#111827]">
          <p className="text-3xl font-black text-slate-600">?</p>
        </div>
      </div>
    );
  }

  const safeScore = Math.max(0, Math.min(100, score));
  const ringColor =
    safeScore >= 75 ? "#6EE7B7" : safeScore >= 50 ? "#f59e0b" : "#e11d48";

  return (
    <div
      className="grid h-36 w-36 shrink-0 place-items-center rounded-full"
      style={{
        background: `conic-gradient(${ringColor} ${safeScore * 3.6}deg, rgba(148,163,184,.18) 0deg)`,
      }}
    >
      <div className="grid h-28 w-28 place-items-center rounded-full bg-[#111827]">
        <div className="text-center">
          <p className="text-4xl font-black text-[#F3F6FF]">{safeScore}</p>
          <p className="text-xs font-bold text-slate-400">/100</p>
        </div>
      </div>
    </div>
  );
}

function SignalCard({ signal }) {
  return (
    <article className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-black text-[#F3F6FF]">{signal.area}</p>
          <p className="mt-1 text-xs font-bold text-slate-500">
            {signal.score}/100
          </p>
        </div>
        <span
          className={`rounded-full px-2.5 py-1 text-xs font-bold ${statusClasses[signal.status] ?? statusClasses.needs_work}`}
        >
          {formatStatus(signal.status)}
        </span>
      </div>
      <div className="mt-2 h-1.5 rounded-full bg-[#202B42] overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ${
            signal.score >= 75
              ? "bg-emerald-400"
              : signal.score >= 50
                ? "bg-amber-400"
                : "bg-rose-400"
          }`}
          style={{ width: `${signal.score}%` }}
        />
      </div>
      <p className="mt-4 text-sm leading-6 text-slate-300">{signal.finding}</p>
      <p className="mt-3 text-sm font-semibold leading-6 text-[#A998FF]">
        {signal.suggestion}
      </p>
    </article>
  );
}

export default function ShopHealthPage() {
  const { plan } = useUsage();
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [computing, setComputing] = useState(false);
  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  const [showUpgrade, setShowUpgrade] = useState(false);

  const gated = plan === "free" || plan === "starter";

  useEffect(() => {
    if (gated) {
      setLoading(false);
      return undefined;
    }

    let active = true;
    setLoading(true);
    setError("");
    api
      .get("/api/shop-health")
      .then((response) => {
        if (!active) return;
        setHealth(response.status === 200 ? response.data : null);
      })
      .catch((err) => {
        if (!active) return;
        setError(
          err.response?.data?.message ||
            "Could not load your shop health score.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [gated]);

  useEffect(() => {
    if (!computing) return undefined;
    const id = window.setInterval(() => {
      setStep((value) => (value + 1) % progressSteps.length);
    }, 1300);
    return () => window.clearInterval(id);
  }, [computing]);

  const compute = async () => {
    setComputing(true);
    setStep(0);
    setError("");
    try {
      const { data } = await api.post("/api/shop-health/compute");
      setHealth(data);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Could not compute your shop health score right now.",
      );
    } finally {
      setComputing(false);
    }
  };

  if (gated) {
    return (
      <div className="mx-auto max-w-3xl">
        <div className="rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235] p-8 text-center">
          <p className="text-lg font-bold text-[#F3F6FF]">Shop Health Score</p>
          <p className="mt-2 text-sm text-slate-400 max-w-sm mx-auto">
            See patterns across all your listings - quality scores, tag mix,
            tone consistency, and platform coverage - in one place.
          </p>
          <button
            type="button"
            onClick={() => setShowUpgrade(true)}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-[#7C5CFF] px-5 py-2.5 text-sm font-bold text-[#F3F6FF] hover:bg-[#6848E8] transition-colors"
          >
            Unlock on Growth plan
          </button>
        </div>
        {showUpgrade && <UpgradeModal onClose={() => setShowUpgrade(false)} />}
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
        <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-sm font-bold text-[#A998FF]">
              Shop Health Score
            </p>
            <h1 className="mt-2 text-3xl font-black text-[#F3F6FF]">
              Understand your listing patterns
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
              Review quality scores, tags, titles, tone, and platform coverage
              from the listings you already generated in Tagwing.
            </p>
          </div>
          <button
            type="button"
            onClick={compute}
            disabled={computing}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <ArrowPathIcon
              className={`h-5 w-5 ${computing ? "animate-spin" : ""}`}
            />
            {health ? "Recompute" : "Compute first health score"}
          </button>
        </div>

        {(loading || computing) && (
          <div className="mt-5 rounded-lg border border-white/10 bg-[#0B1020]/80 p-4">
            <div className="flex items-center justify-between gap-4 text-sm">
              <span className="font-bold text-[#F3F6FF]">
                {computing
                  ? progressSteps[step]
                  : "Loading your latest health report"}
              </span>
              {computing && (
                <span className="text-slate-500">
                  {step + 1}/{progressSteps.length}
                </span>
              )}
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-[#202B42]">
              <div
                className="h-full rounded-full bg-[#6EE7B7] transition-all duration-500"
                style={{
                  width: computing
                    ? `${((step + 1) / progressSteps.length) * 100}%`
                    : "45%",
                }}
              />
            </div>
          </div>
        )}

        {error && (
          <div className="mt-4 rounded-lg border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm font-semibold text-rose-200">
            {error}
          </div>
        )}
      </section>

      {!loading && !computing && !health && (
        <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-8 text-center">
          <div className="mx-auto grid h-12 w-12 place-items-center rounded-lg bg-[#7C5CFF]/12 text-[#A998FF]">
            <HeartIcon className="h-6 w-6" />
          </div>
          <h2 className="mt-4 text-xl font-black text-[#F3F6FF]">
            No health snapshot yet
          </h2>
          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
            Compute your first report from your recent Tagwing listing history.
            No external marketplace connection needed.
          </p>
          <button
            type="button"
            onClick={compute}
            className="mt-5 inline-flex items-center gap-2 rounded-lg bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
          >
            <SparklesIcon className="h-5 w-5" />
            Compute first health score
          </button>
        </section>
      )}

      {health && (
        <div className="space-y-6 animate-[row-rise_520ms_ease_both]">
          <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
            <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
              <div className="flex flex-col gap-5 md:flex-row md:items-center">
                <ScoreRing score={health.overallScore} />
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="text-2xl font-black text-[#F3F6FF]">
                      Overall health
                    </h2>
                    <span
                      className={`rounded-full px-3 py-1 text-sm font-black ${gradeClasses[health.grade] ?? gradeClasses.D}`}
                    >
                      Grade {health.grade}
                    </span>
                  </div>
                  <p className="mt-2 max-w-2xl text-base leading-7 text-slate-300">
                    {health.headline}
                  </p>
                  <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-slate-500">
                    <span>{health.listingsAnalysed} listings analysed</span>
                    <span className="h-1 w-1 rounded-full bg-slate-700" />
                    <span>Last computed {timeAgo(health.computedAt)}</span>
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={compute}
                disabled={computing}
                className="inline-flex items-center justify-center gap-2 rounded-lg border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 px-4 py-2.5 text-sm font-bold text-[#D8D0FF] transition-colors hover:bg-[#7C5CFF]/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <ArrowPathIcon
                  className={`h-4 w-4 ${computing ? "animate-spin" : ""}`}
                />
                Recompute
              </button>
            </div>
          </section>

          <section className="grid gap-3 md:grid-cols-2">
            {health.signals.map((signal) => (
              <SignalCard key={signal.area} signal={signal} />
            ))}
          </section>

          <section className="rounded-lg border border-[#6EE7B7]/20 bg-[#10231F]/80 p-5">
            <div className="flex items-center gap-2">
              <ChartBarIcon className="h-5 w-5 text-emerald-300" />
              <h2 className="text-base font-black text-[#F3F6FF]">
                Your 3 top fixes
              </h2>
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
              {health.topFixes.map((fix, index) => (
                <div
                  key={`${fix}-${index}`}
                  className="rounded-lg border border-emerald-400/15 bg-emerald-400/8 p-4"
                >
                  <p className="text-xs font-black text-emerald-300">
                    Fix {index + 1}
                  </p>
                  <p className="mt-2 text-sm leading-6 text-emerald-50">
                    {fix}
                  </p>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
