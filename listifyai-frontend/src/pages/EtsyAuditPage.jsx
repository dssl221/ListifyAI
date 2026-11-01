import { useEffect, useMemo, useState } from "react";
import {
  ArrowPathIcon,
  ClockIcon,
  CheckCircleIcon,
  ClipboardDocumentIcon,
  DocumentTextIcon,
  GiftIcon,
  LinkIcon,
  MagnifyingGlassIcon,
  SparklesIcon,
  TagIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import api from "../lib/api";

const progressSteps = [
  "Reading the Etsy listing",
  "Checking shopper clarity",
  "Reviewing tags and title",
  "Preparing practical fixes",
];

const sectionLabels = [
  ["weakTagAnalysis", "Tag space", TagIcon],
  ["titleClarityAnalysis", "Title clarity", MagnifyingGlassIcon],
  ["buyerIntentAnalysis", "Buyer intent", CheckCircleIcon],
  ["personalizationAnalysis", "Personalization", SparklesIcon],
  ["giftingIntentAnalysis", "Gifting fit", GiftIcon],
  ["readabilityReview", "Readability", DocumentTextIcon],
];

const auditChecks = [
  "Listing score",
  "Plain-English summary",
  "Weak tags",
  "Title clarity",
  "Buyer intent",
  "Personalization",
  "Gifting fit",
  "Readability",
  "3 next fixes",
  "Better title",
  "Better tags",
  "Better description",
];

function ScoreRing({ score = 0 }) {
  const degree = Math.max(0, Math.min(100, score)) * 3.6;
  const ringColor =
    score >= 75 ? "#6EE7B7" : score >= 50 ? "#f59e0b" : "#e11d48";
  return (
    <div
      className="grid h-32 w-32 shrink-0 place-items-center rounded-full"
      style={{
        background: `conic-gradient(${ringColor} ${degree}deg, rgba(148,163,184,.18) 0deg)`,
      }}
    >
      <div className="grid h-24 w-24 place-items-center rounded-full bg-[#111827]">
        <div className="text-center">
          <p className="text-3xl font-black text-[#F3F6FF]">{score}</p>
          <p className="text-xs font-bold text-slate-400">/100</p>
        </div>
      </div>
    </div>
  );
}

function CopyButton({ value }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    await navigator.clipboard.writeText(value || "");
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1400);
  };

  return (
    <button
      type="button"
      onClick={copy}
      className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-white/10 bg-[#1A2235] text-slate-300 transition-colors hover:bg-[#202B42] hover:text-[#F3F6FF]"
      title={copied ? "Copied" : "Copy"}
    >
      <ClipboardDocumentIcon className="h-4 w-4" />
    </button>
  );
}

function InsightSection({ insight, label, Icon }) {
  const strong = insight?.status === "strong";
  return (
    <article className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-4">
      <div className="flex items-start gap-3">
        <span
          className={`grid h-9 w-9 shrink-0 place-items-center rounded-lg ${strong ? "bg-emerald-400/12 text-emerald-300" : "bg-amber-400/12 text-amber-300"}`}
        >
          <Icon className="h-5 w-5" />
        </span>
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm font-black text-[#F3F6FF]">{label}</h3>
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-bold ${strong ? "bg-emerald-400/10 text-emerald-300" : "bg-amber-400/10 text-amber-300"}`}
            >
              {strong ? "Looks good" : "Needs work"}
            </span>
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-300">
            {insight?.message}
          </p>
          <p className="mt-3 text-sm font-semibold text-[#A998FF]">
            {insight?.fix}
          </p>
        </div>
      </div>
    </article>
  );
}

function TextCompare({ label, before, after, onRegenerate, regenerating }) {
  return (
    <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-base font-black text-[#F3F6FF]">{label}</h2>
        <button
          type="button"
          onClick={onRegenerate}
          disabled={regenerating}
          className="inline-flex items-center gap-2 rounded-lg border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 px-3 py-2 text-sm font-bold text-[#D8D0FF] transition-colors hover:bg-[#7C5CFF]/20 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <ArrowPathIcon
            className={`h-4 w-4 ${regenerating ? "animate-spin" : ""}`}
          />
          Improve
        </button>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <div className="rounded-lg border border-white/10 bg-[#0B1020]/80 p-4">
          <p className="text-xs font-black uppercase tracking-wide text-slate-500">
            Current
          </p>
          <p className="mt-2 text-sm leading-6 text-slate-300">
            {before || "No current version found."}
          </p>
        </div>
        <div className="rounded-lg border border-emerald-400/20 bg-emerald-400/8 p-4">
          <div className="flex items-center justify-between gap-3">
            <p className="text-xs font-black uppercase tracking-wide text-emerald-300">
              Suggested
            </p>
            <CopyButton value={after} />
          </div>
          <p className="mt-2 text-sm leading-6 text-[#F3F6FF]">{after}</p>
        </div>
      </div>
    </section>
  );
}

function TagsCompare({ before = [], after = [], onRegenerate, regenerating }) {
  return (
    <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-base font-black text-[#F3F6FF]">Tags</h2>
        <button
          type="button"
          onClick={onRegenerate}
          disabled={regenerating}
          className="inline-flex items-center gap-2 rounded-lg border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 px-3 py-2 text-sm font-bold text-[#D8D0FF] transition-colors hover:bg-[#7C5CFF]/20 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <ArrowPathIcon
            className={`h-4 w-4 ${regenerating ? "animate-spin" : ""}`}
          />
          Improve
        </button>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <div className="rounded-lg border border-white/10 bg-[#0B1020]/80 p-4">
          <p className="text-xs font-black uppercase tracking-wide text-slate-500">
            Current
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            {before.map((tag) => (
              <span
                key={tag}
                className="rounded-full bg-[#202B42] px-3 py-1 text-xs font-semibold text-slate-300"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
        <div className="rounded-lg border border-emerald-400/20 bg-emerald-400/8 p-4">
          <div className="flex items-center justify-between gap-3">
            <p className="text-xs font-black uppercase tracking-wide text-emerald-300">
              Suggested
            </p>
            <CopyButton value={after.join(", ")} />
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {after.map((tag) => (
              <span
                key={tag}
                className="rounded-full bg-emerald-400/12 px-3 py-1 text-xs font-semibold text-emerald-100"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

export default function EtsyAuditPage() {
  const [url, setUrl] = useState("");
  const [result, setResult] = useState(null);
  const [usage, setUsage] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  const [regenerating, setRegenerating] = useState("");

  useEffect(() => {
    api
      .get("/api/audit-usage")
      .then(({ data }) => setUsage(data))
      .catch(() => setUsage(null));

    api
      .get("/api/audit-history", { params: { page: 0, size: 5 } })
      .then(({ data }) => setHistory(data.content ?? []))
      .catch(() => setHistory([]));
  }, []);

  useEffect(() => {
    if (!loading) return undefined;
    const id = window.setInterval(() => {
      setStep((value) => (value + 1) % progressSteps.length);
    }, 1300);
    return () => window.clearInterval(id);
  }, [loading]);

  const audit = result?.audit;
  const listing = result?.listing;
  const remainingText = useMemo(() => {
    const activeUsage = result?.usage || usage;
    if (!activeUsage) return "Audit credits loading";
    return `${activeUsage.remaining} of ${activeUsage.limit} audits left`;
  }, [result, usage]);
  const remainingAudits = result?.usage?.remaining ?? usage?.remaining;

  const analyze = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    setStep(0);
    try {
      const { data } = await api.post("/api/analyze-etsy-listing", { url });
      setResult(data);
      setUsage(data.usage);
      setHistory((items) =>
        [
          {
            analysisId: data.analysisId,
            listingId: data.listing?.etsyListingId,
            listingUrl: data.listing?.listingUrl,
            title: data.listing?.title,
            score: data.audit?.score,
            createdAt: new Date().toISOString(),
          },
          ...items,
        ].slice(0, 5),
      );
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "The audit could not be completed right now.",
      );
    } finally {
      setLoading(false);
    }
  };

  const regenerate = async (section) => {
    if (!result?.analysisId) return;
    setRegenerating(section);
    setError("");
    try {
      const endpoint = {
        title: "/api/regenerate-title",
        tags: "/api/regenerate-tags",
        description: "/api/regenerate-description",
      }[section];
      const { data } = await api.post(endpoint, {
        analysisId: result.analysisId,
      });
      setResult((current) => ({ ...current, audit: data.audit }));
    } catch (err) {
      setError(
        err.response?.data?.message || `Could not improve the ${section}.`,
      );
    } finally {
      setRegenerating("");
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
        <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm font-bold text-[#A998FF]">
              Etsy Listing Audit
            </p>
            <h1 className="mt-2 text-3xl font-black text-[#F3F6FF]">
              See what shoppers may miss
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
              Paste a public Etsy listing and get a calm, practical review with
              three next steps.
            </p>
          </div>
          <div
            className={`rounded-lg border px-4 py-3 text-sm font-bold ${
              remainingAudits <= 1
                ? "border-amber-500/30 bg-amber-500/10 text-amber-300"
                : "border-[#7C5CFF]/25 bg-[#1A2235] text-[#D8D0FF]"
            }`}
          >
            {remainingText}
          </div>
        </div>

        <form
          onSubmit={analyze}
          className="mt-6 flex flex-col gap-3 md:flex-row"
        >
          <label className="relative flex-1">
            <LinkIcon className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
            <input
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              placeholder="https://www.etsy.com/listing/123456789/example-product"
              className="h-12 w-full rounded-lg border border-white/10 bg-[#0B1020] pl-10 pr-10 text-sm text-[#F3F6FF] outline-none transition-colors placeholder:text-slate-600 focus:border-[#7C5CFF]"
            />
            {url && (
              <button
                type="button"
                onClick={() => setUrl("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-200 transition-colors"
              >
                <XMarkIcon className="h-4 w-4" />
              </button>
            )}
          </label>
          <button
            type="submit"
            disabled={loading || !url.trim()}
            className="inline-flex h-12 items-center justify-center gap-2 rounded-lg bg-[#7C5CFF] px-5 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <MagnifyingGlassIcon className="h-5 w-5" />
            )}
            Audit listing
          </button>
        </form>

        {loading && (
          <div className="mt-5 rounded-lg border border-white/10 bg-[#0B1020]/80 p-4">
            <div className="flex items-center justify-between gap-4 text-sm">
              <span className="font-bold text-[#F3F6FF]">
                {progressSteps[step]}
              </span>
              <span className="text-slate-500">
                {step + 1}/{progressSteps.length}
              </span>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-[#202B42]">
              <div
                className="h-full rounded-full bg-[#6EE7B7] transition-all duration-500"
                style={{
                  width: `${((step + 1) / progressSteps.length) * 100}%`,
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

      {!audit && (
        <section className="grid gap-4 md:grid-cols-[1.25fr_.75fr]">
          <div className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5">
            <h2 className="text-base font-black text-[#F3F6FF]">
              What the audit checks
            </h2>
            <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
              {auditChecks.map((check) => (
                <div
                  key={check}
                  className="flex items-center gap-2 rounded-lg border border-white/10 bg-[#0B1020]/70 px-3 py-2 text-sm font-semibold text-slate-300"
                >
                  <CheckCircleIcon className="h-4 w-4 shrink-0 text-[#6EE7B7]" />
                  <span>{check}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5">
            <div className="flex items-center gap-2">
              <ClockIcon className="h-5 w-5 text-[#A998FF]" />
              <h2 className="text-base font-black text-[#F3F6FF]">
                Recent audits
              </h2>
            </div>
            {history.length === 0 ? (
              <p className="mt-4 text-sm leading-6 text-slate-400">
                Your audited Etsy listings will appear here after the first
                review.
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {history.map((item) => (
                  <a
                    key={item.analysisId}
                    href={item.listingUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="block rounded-lg border border-white/10 bg-[#0B1020]/70 p-3 transition-colors hover:border-[#7C5CFF]/40"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <p className="line-clamp-2 text-sm font-bold text-[#F3F6FF]">
                        {item.title}
                      </p>
                      <span className="rounded-full bg-[#6EE7B7]/12 px-2 py-0.5 text-xs font-black text-[#6EE7B7]">
                        {item.score}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      Listing {item.listingId}
                    </p>
                  </a>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {audit && (
        <div className="space-y-6 animate-[row-rise_520ms_ease_both]">
          <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
            <div className="flex flex-col gap-5 md:flex-row md:items-center">
              <ScoreRing score={audit.score} />
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-2xl font-black text-[#F3F6FF]">
                    Listing score
                  </h2>
                  {result.cached && (
                    <span className="rounded-full bg-[#202B42] px-2.5 py-1 text-xs font-bold text-slate-300">
                      Recent audit reused
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm leading-6 text-slate-300">
                  {audit.scoreExplanation}
                </p>
                <p className="mt-4 text-base leading-7 text-[#F3F6FF]">
                  {audit.summary}
                </p>
              </div>
            </div>
          </section>

          <section className="grid gap-3 md:grid-cols-2">
            {sectionLabels.map(([key, label, Icon]) => (
              <InsightSection
                key={key}
                insight={audit[key]}
                label={label}
                Icon={Icon}
              />
            ))}
          </section>

          <section className="rounded-lg border border-[#6EE7B7]/20 bg-[#10231F]/80 p-5">
            <h2 className="text-base font-black text-[#F3F6FF]">
              Your 3 next fixes
            </h2>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
              {audit.actionableFixes.map((fix, index) => (
                <div
                  key={fix}
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

          <TextCompare
            label="Title"
            before={listing?.title}
            after={audit.optimizedTitle}
            onRegenerate={() => regenerate("title")}
            regenerating={regenerating === "title"}
          />

          <TagsCompare
            before={listing?.tags}
            after={audit.optimizedTags}
            onRegenerate={() => regenerate("tags")}
            regenerating={regenerating === "tags"}
          />

          <TextCompare
            label="Description"
            before={listing?.description}
            after={audit.optimizedDescription}
            onRegenerate={() => regenerate("description")}
            regenerating={regenerating === "description"}
          />
        </div>
      )}
    </div>
  );
}
