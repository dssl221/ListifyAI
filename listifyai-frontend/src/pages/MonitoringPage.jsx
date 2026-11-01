import { Fragment, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowPathIcon,
  BellAlertIcon,
  ChevronDownIcon,
  ChevronRightIcon,
  ClipboardIcon,
  LinkIcon,
  PauseIcon,
  PlayIcon,
  SparklesIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import EmptyState from "../components/ui/EmptyState";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import { useUsage } from "../context/UsageContext";
import api from "../lib/api";

const STATUS_STYLES = {
  ACTIVE: "border-emerald-400/25 bg-emerald-400/10 text-emerald-300",
  PAUSED: "border-amber-400/25 bg-amber-400/10 text-amber-300",
  ERROR: "border-rose-400/25 bg-rose-400/10 text-rose-300",
};

const SEVERITY_DOT = {
  HIGH: "bg-rose-400",
  MEDIUM: "bg-amber-400",
  LOW: "bg-[#4DA3FF]",
  INFO: "bg-slate-400",
};

function truncate(value, max = 50) {
  if (!value) return "Untitled Etsy listing";
  return value.length > max ? `${value.slice(0, max - 1)}...` : value;
}

function timeAgo(value) {
  if (!value) return "-";
  const then = new Date(value).getTime();
  if (Number.isNaN(then)) return "-";
  const diffMs = then - Date.now();
  const absMs = Math.abs(diffMs);
  const minutes = Math.round(absMs / 60000);
  const hours = Math.round(absMs / 3600000);
  const days = Math.round(absMs / 86400000);
  const suffix = diffMs >= 0 ? "from now" : "ago";

  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ${suffix}`;
  if (hours < 24) return `${hours}h ${suffix}`;
  return `${days}d ${suffix}`;
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function MiniScoreRing({ score }) {
  const value = score == null ? 0 : Math.max(0, Math.min(100, score));
  const degree = value * 3.6;
  const ringColor =
    value >= 75 ? "#6EE7B7" : value >= 50 ? "#f59e0b" : "#e11d48";

  return (
    <div
      className="grid h-8 w-8 shrink-0 place-items-center rounded-full"
      style={{
        background: `conic-gradient(${ringColor} ${degree}deg, rgba(148,163,184,.18) 0deg)`,
      }}
      title={score == null ? "No score yet" : `${score}/100`}
    >
      <div className="grid h-6 w-6 place-items-center rounded-full bg-[#111827]">
        <span className="text-[10px] font-black text-[#F3F6FF]">
          {score == null ? "-" : score}
        </span>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  const label = status || "ERROR";
  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-black ${STATUS_STYLES[label] ?? STATUS_STYLES.ERROR}`}
    >
      {label}
    </span>
  );
}

function OpportunityBar({ score }) {
  const value = Math.max(0, Math.min(100, score ?? 0));
  return (
    <div className="mt-3">
      <div className="flex items-center justify-between text-xs text-slate-500">
        <span>Opportunity</span>
        <span>{value}/100</span>
      </div>
      <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-[#202B42]">
        <div
          className="h-full rounded-full bg-[#7C5CFF]"
          style={{ width: `${value}%` }}
        />
      </div>
    </div>
  );
}

function ExpandedListing({
  expandedData,
  loading,
  error,
  aiFixer,
  onRequestAiFix,
  onCopyFix,
}) {
  if (loading) {
    return (
      <div className="rounded-lg border border-white/10 bg-[#0B1020]/70 p-5">
        <LoadingSpinner label="Loading listing monitor details..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm font-semibold text-rose-200">
        {error}
      </div>
    );
  }

  const snapshots = expandedData?.history ?? [];
  const recommendations = expandedData?.recommendations ?? [];

  return (
    <div className="grid gap-4 rounded-lg border border-white/10 bg-[#0B1020]/70 p-4 lg:grid-cols-[1.1fr_.9fr]">
      <section>
        <h3 className="text-sm font-black text-[#F3F6FF]">Snapshot history</h3>
        {snapshots.length === 0 ? (
          <p className="mt-3 text-sm text-slate-400">
            No snapshots have been captured for this listing yet.
          </p>
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full min-w-[520px] text-left text-sm">
              <thead className="text-xs font-black uppercase tracking-wide text-slate-500">
                <tr className="border-b border-white/10">
                  <th className="py-2 pr-3">Captured</th>
                  <th className="py-2 pr-3">Score</th>
                  <th className="py-2 pr-3">Views</th>
                  <th className="py-2 pr-3">Favorites</th>
                  <th className="py-2 pr-3">Tags</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/10 text-slate-300">
                {snapshots.slice(0, 10).map((snapshot) => (
                  <tr key={snapshot.id}>
                    <td className="py-2 pr-3 text-slate-400">
                      {formatDate(snapshot.capturedAt)}
                    </td>
                    <td className="py-2 pr-3 font-bold text-[#F3F6FF]">
                      {snapshot.score}
                    </td>
                    <td className="py-2 pr-3">{snapshot.views}</td>
                    <td className="py-2 pr-3">{snapshot.favorites}</td>
                    <td className="py-2 pr-3">{snapshot.tags?.length ?? 0}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section>
        <h3 className="text-sm font-black text-[#F3F6FF]">
          Open recommendations
        </h3>
        {recommendations.length === 0 ? (
          <p className="mt-3 text-sm text-slate-400">
            No open recommendations for this listing right now.
          </p>
        ) : (
          <div className="mt-3 space-y-3">
            {recommendations.map((recommendation) => (
              <article
                key={recommendation.id}
                className="rounded-lg border border-white/10 bg-[#131A2A]/90 p-4"
              >
                <div className="flex items-start gap-3">
                  <span
                    className={`mt-1 h-2.5 w-2.5 shrink-0 rounded-full ${SEVERITY_DOT[recommendation.severity] ?? SEVERITY_DOT.INFO}`}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="text-sm font-black text-[#F3F6FF]">
                        {recommendation.title}
                      </h4>
                      {recommendation.aiCandidate && (
                        <span className="rounded-full bg-[#7C5CFF]/14 px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-[#D8D0FF]">
                          AI ready
                        </span>
                      )}
                    </div>
                    <p className="mt-2 text-sm leading-6 text-slate-300">
                      {recommendation.message}
                    </p>
                    {recommendation.aiCandidate && (
                      <div className="mt-3">
                        {recommendation.aiFixText ? (
                          <div className="rounded-lg border border-[#7C5CFF]/25 bg-[#7C5CFF]/5 p-3">
                            <div className="mb-1.5 flex items-center gap-1.5">
                              <SparklesIcon className="h-3.5 w-3.5 text-[#A998FF]" />
                              <span className="text-xs font-semibold text-[#A998FF]">
                                AI-generated fix
                              </span>
                            </div>
                            <p className="text-sm leading-relaxed text-[#F3F6FF]">
                              {recommendation.aiFixText}
                            </p>
                            <button
                              type="button"
                              onClick={() =>
                                onCopyFix(recommendation.aiFixText)
                              }
                              className="mt-2 flex items-center gap-1 text-xs text-[#7C5CFF] transition-colors hover:text-[#A998FF]"
                            >
                              <ClipboardIcon className="h-3 w-3" />
                              Copy fix
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            onClick={() => onRequestAiFix(recommendation.id)}
                            disabled={aiFixer === recommendation.id}
                            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                              aiFixer === recommendation.id
                                ? "cursor-not-allowed bg-[#1A2235] text-slate-500"
                                : "bg-[#7C5CFF]/10 text-[#A998FF] hover:bg-[#7C5CFF]/20"
                            }`}
                          >
                            {aiFixer === recommendation.id ? (
                              <>
                                <div className="h-3 w-3 rounded-full border border-[#A998FF] border-t-transparent animate-spin" />
                                Generating fix...
                              </>
                            ) : (
                              <>
                                <SparklesIcon className="h-3.5 w-3.5" />
                                Get AI fix
                              </>
                            )}
                          </button>
                        )}
                      </div>
                    )}
                    <OpportunityBar score={recommendation.opportunityScore} />
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default function MonitoringPage() {
  const { plan = "free" } = useUsage();
  const [url, setUrl] = useState("");
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [togglingId, setTogglingId] = useState("");
  const [error, setError] = useState("");
  const [expandedId, setExpandedId] = useState("");
  const [expandedData, setExpandedData] = useState({});
  const [expandedLoading, setExpandedLoading] = useState({});
  const [expandedError, setExpandedError] = useState({});
  const [aiFixer, setAiFixer] = useState(null);

  const isFree = plan === "free";
  const freeLimit = 3;
  const monitoredCount = listings.length;
  const freeAtLimit = isFree && monitoredCount >= freeLimit;

  const loadListings = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/api/monitoring/listings");
      setListings(data ?? []);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Could not load monitored listings right now.",
      );
      setListings([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadListings();
  }, []);

  const openRecommendationCount = useMemo(
    () =>
      listings.reduce(
        (total, listing) => total + (listing.openRecommendations ?? 0),
        0,
      ),
    [listings],
  );

  const startMonitoring = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const { data } = await api.post("/api/monitoring/enable", { url });
      setListings((current) => {
        const withoutExisting = current.filter(
          (listing) => listing.id !== data.id,
        );
        return [data, ...withoutExisting];
      });
      setUrl("");
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Paste a valid Etsy listing URL to start monitoring.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const toggleMonitoring = async (listing) => {
    setError("");
    setTogglingId(listing.id);
    try {
      const endpoint = listing.monitoringEnabled
        ? "/api/monitoring/disable"
        : "/api/monitoring/enable";
      const payload = listing.monitoringEnabled
        ? { listingId: listing.id }
        : { url: listing.listingUrl };
      const { data } = await api.post(endpoint, payload);
      setListings((current) =>
        current.map((item) => (item.id === data.id ? data : item)),
      );
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Could not update monitoring for this listing.",
      );
    } finally {
      setTogglingId("");
    }
  };

  const toggleExpanded = async (listingId) => {
    if (expandedId === listingId) {
      setExpandedId("");
      return;
    }

    setExpandedId(listingId);
    if (expandedData[listingId]) {
      return;
    }

    setExpandedLoading((current) => ({ ...current, [listingId]: true }));
    setExpandedError((current) => ({ ...current, [listingId]: "" }));
    try {
      const [historyResponse, recommendationsResponse] = await Promise.all([
        api.get(`/api/monitoring/history/${listingId}`),
        api.get(`/api/monitoring/recommendations/${listingId}`),
      ]);
      setExpandedData((current) => ({
        ...current,
        [listingId]: {
          history: historyResponse.data ?? [],
          recommendations: recommendationsResponse.data ?? [],
        },
      }));
    } catch (err) {
      setExpandedError((current) => ({
        ...current,
        [listingId]:
          err.response?.data?.message ||
          "Could not load listing monitor details.",
      }));
    } finally {
      setExpandedLoading((current) => ({ ...current, [listingId]: false }));
    }
  };

  const handleRequestAiFix = async (recommendationId) => {
    setAiFixer(recommendationId);
    try {
      const { data } = await api.post(
        `/api/monitoring/recommendations/${recommendationId}/ai-fix`,
      );
      setExpandedData((current) =>
        Object.fromEntries(
          Object.entries(current).map(([currentListingId, listingData]) => [
            currentListingId,
            {
              ...listingData,
              recommendations: (listingData.recommendations ?? []).map(
                (recommendation) =>
                  recommendation.id === recommendationId
                    ? {
                        ...recommendation,
                        aiFixText: data.aiFixText,
                        status: "OPEN",
                      }
                    : recommendation,
              ),
            },
          ]),
        ),
      );
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Could not generate an AI fix right now.",
      );
    } finally {
      setAiFixer(null);
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
        <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm font-bold text-[#A998FF]">
              Listing Health Monitor
            </p>
            <h1 className="mt-2 text-3xl font-black text-[#F3F6FF]">
              Watch live Etsy listings for SEO drift
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
              Add a public Etsy listing URL and Tagwing will track snapshots,
              score movement, and rule-based optimization signals.
            </p>
          </div>
          <div className="rounded-lg border border-[#7C5CFF]/25 bg-[#1A2235] px-4 py-3 text-sm font-bold text-[#D8D0FF]">
            {monitoredCount} monitored, {openRecommendationCount} open fixes
          </div>
        </div>

        <form
          onSubmit={startMonitoring}
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
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 transition-colors hover:text-slate-200"
              >
                <XMarkIcon className="h-4 w-4" />
              </button>
            )}
          </label>
          <button
            type="submit"
            disabled={submitting || !url.trim()}
            className="inline-flex h-12 items-center justify-center gap-2 rounded-lg bg-[#7C5CFF] px-5 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? (
              <ArrowPathIcon className="h-5 w-5 animate-spin" />
            ) : (
              <BellAlertIcon className="h-5 w-5" />
            )}
            Start monitoring
          </button>
        </form>

        {error && (
          <div className="mt-4 rounded-lg border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm font-semibold text-rose-200">
            {error}
          </div>
        )}
      </section>

      <section className="rounded-lg border border-white/10 bg-[#131A2A]/92 p-5 shadow-xl shadow-[#050816]/32 md:p-6">
        <div className="mb-4 flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-black text-[#F3F6FF]">
              Monitored listings
            </h2>
            <p className="mt-1 text-sm text-slate-400">
              Pause, resume, and expand listings to review history and
              recommendations.
            </p>
          </div>
          <button
            type="button"
            onClick={loadListings}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-[#1A2235] px-3 py-2 text-sm font-bold text-slate-300 transition-colors hover:bg-[#202B42] hover:text-[#F3F6FF] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <ArrowPathIcon
              className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
            />
            Refresh
          </button>
        </div>

        {loading ? (
          <div className="py-12">
            <LoadingSpinner label="Loading monitored listings..." />
          </div>
        ) : listings.length === 0 ? (
          <EmptyState
            icon={BellAlertIcon}
            title="No listings monitored yet"
            description="Paste an Etsy listing URL above to start tracking health, snapshots, and optimization opportunities."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[940px] text-left text-sm">
              <thead className="text-xs font-black uppercase tracking-wide text-slate-500">
                <tr className="border-b border-white/10">
                  <th className="py-3 pr-3">Title</th>
                  <th className="py-3 pr-3">Status</th>
                  <th className="py-3 pr-3">Last score</th>
                  <th className="py-3 pr-3">Last checked</th>
                  <th className="py-3 pr-3">Next check</th>
                  <th className="py-3 pr-3">Open recommendations</th>
                  <th className="py-3 pr-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/10">
                {listings.map((listing) => (
                  <Fragment key={listing.id}>
                    <tr className="align-middle">
                      <td className="py-3 pr-3">
                        <div>
                          <p className="font-bold text-[#F3F6FF]">
                            {truncate(listing.title)}
                          </p>
                          <p className="mt-0.5 text-xs text-slate-500">
                            Listing {listing.etsyListingId}
                          </p>
                        </div>
                      </td>
                      <td className="py-3 pr-3">
                        <StatusBadge status={listing.status} />
                      </td>
                      <td className="py-3 pr-3">
                        <MiniScoreRing score={listing.lastScore} />
                      </td>
                      <td className="py-3 pr-3 text-slate-400">
                        {timeAgo(listing.lastCheckedAt)}
                      </td>
                      <td className="py-3 pr-3 text-slate-400">
                        {timeAgo(listing.nextCheckAt)}
                      </td>
                      <td className="py-3 pr-3">
                        {(listing.openRecommendations ?? 0) > 0 ? (
                          <span className="inline-flex rounded-full bg-rose-400/12 px-2.5 py-1 text-xs font-black text-rose-300">
                            {listing.openRecommendations}
                          </span>
                        ) : (
                          <span className="text-slate-500">0</span>
                        )}
                      </td>
                      <td className="py-3 pr-3">
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => toggleMonitoring(listing)}
                            disabled={togglingId === listing.id}
                            className="inline-flex h-9 items-center gap-2 rounded-lg border border-white/10 bg-[#1A2235] px-3 text-xs font-bold text-slate-300 transition-colors hover:bg-[#202B42] hover:text-[#F3F6FF] disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {listing.monitoringEnabled ? (
                              <PauseIcon className="h-4 w-4" />
                            ) : (
                              <PlayIcon className="h-4 w-4" />
                            )}
                            {listing.monitoringEnabled ? "Pause" : "Resume"}
                          </button>
                          <button
                            type="button"
                            onClick={() => toggleExpanded(listing.id)}
                            className="inline-flex h-9 items-center gap-1 rounded-lg border border-[#7C5CFF]/35 bg-[#7C5CFF]/12 px-3 text-xs font-bold text-[#D8D0FF] transition-colors hover:bg-[#7C5CFF]/20"
                          >
                            {expandedId === listing.id ? (
                              <ChevronDownIcon className="h-4 w-4" />
                            ) : (
                              <ChevronRightIcon className="h-4 w-4" />
                            )}
                            Details
                          </button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === listing.id && (
                      <tr>
                        <td colSpan={7} className="pb-4 pt-1">
                          <ExpandedListing
                            expandedData={expandedData[listing.id]}
                            loading={expandedLoading[listing.id]}
                            error={expandedError[listing.id]}
                            aiFixer={aiFixer}
                            onRequestAiFix={handleRequestAiFix}
                            onCopyFix={copyToClipboard}
                          />
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {isFree && (
          <div
            className={`mt-5 rounded-lg border px-4 py-3 text-sm font-semibold ${
              freeAtLimit
                ? "border-amber-400/25 bg-amber-400/10 text-amber-200"
                : "border-white/10 bg-[#0B1020]/70 text-slate-300"
            }`}
          >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <span>
                Monitoring {monitoredCount} of 3 listings on Free plan.
              </span>
              {freeAtLimit && (
                <Link
                  to="/pricing"
                  className="inline-flex items-center justify-center rounded-lg bg-[#7C5CFF] px-4 py-2 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
                >
                  Upgrade
                </Link>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
