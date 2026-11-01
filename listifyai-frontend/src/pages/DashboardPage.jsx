import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowPathIcon,
  ArrowRightIcon,
  BellAlertIcon,
  ClockIcon,
  SparklesIcon,
} from "@heroicons/react/24/outline";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import EmptyState from "../components/ui/EmptyState";
import { useAuth } from "../context/AuthContext";
import { useUsage } from "../context/UsageContext";
import api from "../lib/api";

const PLATFORM_BADGE = {
  etsy: "bg-orange-500/10 text-orange-300 border-orange-500/25",
  shopify: "bg-emerald-500/10 text-emerald-300 border-emerald-500/25",
  both: "bg-[#1A2235] text-[#A998FF] border-[#7C5CFF]/25",
};

const PLAN_BADGE = {
  free: "bg-[#202B42] text-[#94A3B8]",
  starter: "bg-[#4DA3FF]/12 text-[#9DCCFF]",
  growth: "bg-[#1A2235] text-[#A998FF]",
  pro: "bg-[#7C5CFF]/14 text-[#D8D0FF]",
};

function greetingForHour(hour) {
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function capitalize(value) {
  if (!value) return "Free";
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function displayNameForUser(user, profile) {
  if (profile?.displayName?.trim()) {
    return profile.displayName.trim().split(/\s+/)[0];
  }

  const metadataName =
    user?.user_metadata?.full_name ||
    user?.user_metadata?.name ||
    user?.user_metadata?.display_name;

  if (metadataName?.trim()) {
    return metadataName.trim().split(/\s+/)[0];
  }

  const emailPrefix = user?.email?.split("@")[0];
  if (!emailPrefix) return "there";

  return (
    emailPrefix
      .split(/[._-]+/)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ") || "there"
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user, profile } = useAuth();
  const {
    plan = "free",
    listingsUsed: used = 0,
    monthlyLimit: limit = 5,
    unlimited = false,
  } = useUsage();
  const [recentListings, setRecentListings] = useState([]);
  const [monitoredListings, setMonitoredListings] = useState([]);
  const [monitoringError, setMonitoringError] = useState("");
  const [totalListings, setTotalListings] = useState(0);
  const [loading, setLoading] = useState(true);

  const displayName = displayNameForUser(user, profile);
  const greeting = greetingForHour(new Date().getHours());
  const upgradeText =
    plan === "free"
      ? "Give your workflow more room with 30 listings each month"
      : "Generate Etsy and Shopify copy together on Growth";
  const nextStep = (() => {
    if (used === 0)
      return { label: "Create your first listing", to: "/app/generate" };
    if (used >= limit && !unlimited)
      return { label: "Upgrade to keep going", to: "/pricing" };
    if (totalListings > 0)
      return { label: "Audit a live listing", to: "/app/audit" };
    return { label: "Improve a listing", to: "/app/generate" };
  })();

  useEffect(() => {
    let cancelled = false;

    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        const [listingsResponse, monitoringResponse] = await Promise.allSettled(
          [
            api.get("/api/listings", { params: { page: 0, size: 5 } }),
            api.get("/api/monitoring/listings"),
          ],
        );

        if (!cancelled) {
          if (listingsResponse.status === "fulfilled") {
            const { data } = listingsResponse.value;
            setRecentListings(data.content ?? []);
            setTotalListings(data.totalElements ?? 0);
          } else {
            setRecentListings([]);
            setTotalListings(0);
          }

          if (monitoringResponse.status === "fulfilled") {
            setMonitoredListings(monitoringResponse.value.data ?? []);
            setMonitoringError("");
          } else {
            setMonitoredListings([]);
            setMonitoringError("Monitoring data is not available right now.");
          }
        }
      } catch {
        if (!cancelled) {
          setRecentListings([]);
          setMonitoredListings([]);
          setTotalListings(0);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchDashboardData();

    return () => {
      cancelled = true;
    };
  }, []);

  const activeMonitoredCount = monitoredListings.filter(
    (listing) => listing.monitoringEnabled,
  ).length;
  const openMonitoringRecommendations = monitoredListings.reduce(
    (total, listing) => total + (listing.openRecommendations ?? 0),
    0,
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <section className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
        <p className="text-sm font-bold text-[#A998FF]">
          {greeting}, {displayName}
        </p>
        <h1 className="mt-2 text-3xl font-black text-[#F3F6FF]">
          Your listing workspace command center
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Track listing capacity, return to saved work, and keep product
          preparation moving without rebuilding from scratch.
        </p>
      </section>

      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
          <p className="text-xs font-black uppercase tracking-wide text-slate-500">
            Listings this month
          </p>
          <p className="mt-3 text-2xl font-black text-[#F3F6FF]">
            {unlimited ? "Unlimited" : `${used} / ${limit}`}
          </p>
          {unlimited ? (
            <p className="mt-1 text-xs text-[#A998FF]">
              No monthly cap on this plan
            </p>
          ) : (
            <div className="mt-3">
              <div className="flex justify-between text-xs text-slate-500 mb-1.5">
                <span>{used} used</span>
                <span>{limit - used} left</span>
              </div>
              <div className="h-1.5 rounded-full bg-[#202B42] overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-700 ${
                    used / limit > 0.8
                      ? "bg-rose-500"
                      : used / limit > 0.5
                        ? "bg-amber-500"
                        : "bg-[#7C5CFF]"
                  }`}
                  style={{ width: `${Math.min(100, (used / limit) * 100)}%` }}
                />
              </div>
            </div>
          )}
        </div>

        <div className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
          <p className="text-xs font-black uppercase tracking-wide text-slate-500">
            Current plan
          </p>
          <span
            className={`mt-3 inline-flex rounded-full px-3 py-1 text-sm font-bold ${PLAN_BADGE[plan] ?? PLAN_BADGE.free}`}
          >
            {capitalize(plan)}
          </span>
        </div>

        <div className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
          <p className="text-xs font-black uppercase tracking-wide text-slate-500">
            Next best step
          </p>
          <button
            type="button"
            onClick={() => navigate(nextStep.to)}
            className="mt-3 inline-flex items-center gap-2 rounded-xl bg-[#7C5CFF] px-4 py-2.5 text-sm font-bold text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
          >
            {nextStep.label}
            <ArrowRightIcon className="h-4 w-4" />
          </button>
        </div>
      </section>

      <section className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
        <div className="mb-4 flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-black text-[#F3F6FF]">
              Live monitoring
            </h2>
            <p className="mt-1 text-sm text-slate-400">
              {activeMonitoredCount} active listings watched,{" "}
              {openMonitoringRecommendations} open recommendations
            </p>
          </div>
          <Link
            to="/app/audit"
            className="text-sm font-bold text-[#A998FF] hover:text-[#D8D0FF]"
          >
            Add listing
          </Link>
        </div>

        {monitoringError ? (
          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
            {monitoringError}
          </div>
        ) : monitoredListings.length === 0 ? (
          <EmptyState
            icon={ArrowPathIcon}
            title="No live listings monitored yet"
            description="Enable monitoring from an Etsy listing URL to start tracking score changes and optimization signals."
            actionLabel="Audit a live listing"
            onAction={() => navigate("/app/audit")}
          />
        ) : (
          <div className="divide-y divide-white/10">
            {monitoredListings.slice(0, 5).map((listing) => (
              <div
                key={listing.id}
                className="grid grid-cols-1 gap-2 py-3 text-sm sm:grid-cols-[1.5fr_.5fr_.7fr_.8fr] sm:items-center"
              >
                <div>
                  <p className="font-semibold text-[#F3F6FF]">
                    {listing.title || `Etsy listing ${listing.etsyListingId}`}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {listing.monitoringEnabled ? "Monitoring active" : "Paused"}
                  </p>
                </div>
                <span className="font-bold text-[#F3F6FF]">
                  {listing.lastScore == null ? "-" : `${listing.lastScore}/100`}
                </span>
                <span className="inline-flex items-center gap-1.5 text-slate-400">
                  <BellAlertIcon className="h-4 w-4 text-[#A998FF]" />
                  {listing.openRecommendations ?? 0} open
                </span>
                <span className="text-slate-500">
                  {listing.nextCheckAt
                    ? new Date(listing.nextCheckAt).toLocaleDateString(
                        undefined,
                        {
                          month: "short",
                          day: "numeric",
                        },
                      )
                    : "Queued"}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="rounded-3xl border border-white/10 bg-[#131A2A]/92 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
        <div className="mb-4 flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-black text-[#F3F6FF]">
              Recent listings
            </h2>
            <p className="mt-1 text-sm text-slate-400">
              {totalListings} total generated listings
            </p>
          </div>
          <Link
            to="/app/history"
            className="text-sm font-bold text-[#A998FF] hover:text-[#D8D0FF]"
          >
            View history
          </Link>
        </div>

        {loading ? (
          <div className="py-10">
            <LoadingSpinner label="Loading your recent listing work..." />
          </div>
        ) : recentListings.length === 0 ? (
          <EmptyState
            icon={ClockIcon}
            title="Start with one real product"
            description="Create your first optimized listing and it will stay here for review, copying, and future edits."
            actionLabel="Improve a listing"
            onAction={() => navigate("/app/generate")}
          />
        ) : (
          <div className="divide-y divide-white/10">
            {recentListings.map((listing) => (
              <Link
                key={listing.id}
                to={`/app/history?highlight=${listing.id}`}
                className="grid grid-cols-1 gap-2 py-3 text-sm transition-colors hover:bg-[#1A2235] sm:grid-cols-[1.5fr_.7fr_.7fr_.8fr] sm:items-center"
              >
                <span className="font-semibold text-[#F3F6FF]">
                  {listing.productName}
                </span>
                <span>
                  <span
                    className={`inline-flex rounded-full border px-2 py-0.5 text-xs font-semibold capitalize ${PLATFORM_BADGE[listing.platform] ?? "bg-[#1A2235] text-[#94A3B8] border-slate-700"}`}
                  >
                    {listing.platform}
                  </span>
                </span>
                <span className="text-slate-400 capitalize">
                  {listing.tone}
                </span>
                <span className="text-slate-500">
                  {listing.createdAt
                    ? new Date(listing.createdAt).toLocaleDateString(
                        undefined,
                        {
                          month: "short",
                          day: "numeric",
                          year: "numeric",
                        },
                      )
                    : "-"}
                </span>
              </Link>
            ))}
          </div>
        )}
      </section>

      {(plan === "free" || plan === "starter") && (
        <section className="rounded-3xl border border-[#7C5CFF]/25 bg-[#1A2235]/90 backdrop-blur p-6 shadow-xl shadow-[#050816]/32">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-2xl bg-[#7C5CFF] text-[#F3F6FF]">
                <SparklesIcon className="h-5 w-5" />
              </span>
              <div>
                <p className="font-black text-[#F3F6FF]">{upgradeText}</p>
                <p className="mt-1 text-sm text-[#A998FF]">
                  More capacity means less stopping mid-session when you are
                  preparing products.
                </p>
              </div>
            </div>
            <Link
              to="/pricing"
              className="inline-flex items-center justify-center rounded-xl bg-[#7C5CFF] px-4 py-2.5 text-sm font-bold text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
            >
              Compare workflow capacity
            </Link>
          </div>
        </section>
      )}
    </div>
  );
}
