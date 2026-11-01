import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Cog6ToothIcon,
  ArrowTopRightOnSquareIcon,
  CheckIcon,
} from "@heroicons/react/24/outline";
import { useAuth } from "../context/AuthContext";
import { useUsage } from "../context/UsageContext";
import api from "../lib/api";

const PLAN_LABELS = {
  free: { label: "Free", color: "bg-[#202B42] text-[#94A3B8]" },
  starter: { label: "Starter", color: "bg-[#4DA3FF]/12 text-[#9DCCFF]" },
  growth: { label: "Growth", color: "bg-[#1A2235] text-[#A998FF]" },
  pro: { label: "Pro", color: "bg-[#7C5CFF]/14 text-[#D8D0FF]" },
};

export default function SettingsPage() {
  const { user, signOut } = useAuth();
  const { plan, listingsUsed, monthlyLimit, unlimited, refreshUsage } =
    useUsage();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showSuccessBanner, setShowSuccessBanner] = useState(
    () => searchParams.get("success") === "true",
  );
  const [bannerDismissed, setBannerDismissed] = useState(false);
  const [portalLoading, setPortalLoading] = useState(false);
  const [portalError, setPortalError] = useState("");

  useEffect(() => {
    if (searchParams.get("success") === "true" && !bannerDismissed) {
      setShowSuccessBanner(true);
      refreshUsage();
      // Remove ?success=true from URL so refresh doesn't re-show banner
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, bannerDismissed, refreshUsage, setSearchParams]);

  const planMeta = PLAN_LABELS[plan] ?? PLAN_LABELS.free;
  const pct =
    unlimited || monthlyLimit === 0
      ? 0
      : Math.min(100, (listingsUsed / monthlyLimit) * 100);

  const openPortal = async () => {
    setPortalLoading(true);
    setPortalError("");
    try {
      const { data } = await api.post("/api/billing/portal");
      window.location.href = data.portalUrl;
    } catch (err) {
      setPortalError(
        err.response?.data?.message ?? "Could not open billing portal.",
      );
      setPortalLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#F3F6FF] flex items-center gap-2">
          <Cog6ToothIcon className="w-6 h-6 text-[#7C5CFF]" /> Settings
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Manage your account and billing.
        </p>
      </div>

      {showSuccessBanner && !bannerDismissed && (
        <div className="flex items-start gap-3 rounded-xl border border-[#4DA3FF]/35 bg-[#1A2235] px-4 py-3 text-[#A998FF] shadow-sm">
          <svg
            className="mt-0.5 h-5 w-5 flex-shrink-0"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm3.857-9.809a.75.75 0 0 0-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 1 0-1.06 1.061l2.5 2.5a.75.75 0 0 0 1.137-.089l4-5.5Z"
              clipRule="evenodd"
            />
          </svg>
          <p className="flex-1 text-sm font-medium">
            Your workflow capacity is updated. You can keep preparing listings
            without losing momentum.
          </p>
          <button
            type="button"
            onClick={() => setBannerDismissed(true)}
            className="rounded-lg p-1 text-[#A998FF] hover:bg-[#24304A]"
          >
            <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
              <path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" />
            </svg>
          </button>
        </div>
      )}

      <section className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm divide-y divide-white/10">
        <div className="px-6 py-4">
          <h2 className="text-sm font-semibold text-slate-300 mb-4">Account</h2>
          <div className="space-y-3">
            <Row label="Email" value={user?.email} />
            <Row label="User ID" value={user?.id} mono />
          </div>
        </div>
        <div className="px-6 py-4">
          <button
            onClick={signOut}
            className="text-sm text-rose-400 hover:text-rose-300 font-medium transition-colors"
          >
            Sign out
          </button>
        </div>
      </section>

      <section className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm divide-y divide-white/10">
        <div className="px-6 py-4">
          <h2 className="text-sm font-semibold text-slate-300 mb-4">
            Plan &amp; Usage
          </h2>
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <span
                className={`text-xs font-semibold px-2.5 py-1 rounded-full ${planMeta.color}`}
              >
                {planMeta.label}
              </span>
              <span className="text-sm text-slate-400">plan</span>
            </div>
            {plan !== "free" && (
              <button
                onClick={openPortal}
                disabled={portalLoading}
                className="flex items-center gap-1.5 text-xs font-medium text-[#A998FF] border border-[#4DA3FF]/70 bg-[#1A2235] hover:bg-[#24304A] px-3 py-1.5 rounded-lg transition-colors disabled:opacity-60"
              >
                <ArrowTopRightOnSquareIcon className="w-3.5 h-3.5" />
                {portalLoading ? "Opening..." : "Manage billing"}
              </button>
            )}
          </div>

          <div>
            <div className="flex justify-between text-xs text-slate-400 mb-1.5">
              <span>Listings this month</span>
              <span>
                {listingsUsed} / {unlimited ? "unlimited" : monthlyLimit}
              </span>
            </div>
            {!unlimited && (
              <div className="w-full bg-[#202B42] rounded-full h-2">
                <div
                  className={`h-2 rounded-full transition-all duration-500 ${
                    pct >= 90
                      ? "bg-rose-500"
                      : pct >= 70
                        ? "bg-amber-400"
                        : "bg-[#4DA3FF]"
                  }`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            )}
          </div>

          {portalError && (
            <p className="mt-3 text-xs text-rose-400">{portalError}</p>
          )}
        </div>

        {plan === "free" && (
          <div className="px-6 py-4 bg-[#1A2235] rounded-b-2xl">
            <p className="text-sm font-medium text-[#D8D0FF] mb-3">
              Upgrade when your product work needs more monthly listings,
              both-platform output, or bulk catalog processing.
            </p>
            <div className="flex flex-wrap gap-2">
              {["starter", "growth", "pro"].map((upgradePlan) => (
                <UpgradeButton key={upgradePlan} plan={upgradePlan} />
              ))}
            </div>
          </div>
        )}
      </section>

      <section className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm px-6 py-4">
        <h2 className="text-sm font-semibold text-slate-300 mb-3">
          What&apos;s included
        </h2>
        <ul className="space-y-2">
          {planFeatures(plan).map((feature) => (
            <li
              key={feature}
              className="flex items-start gap-2 text-sm text-[#94A3B8]"
            >
              <CheckIcon className="w-4 h-4 text-[#4DA3FF] flex-shrink-0 mt-0.5" />
              {feature}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function Row({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-sm text-slate-400 flex-shrink-0">{label}</span>
      <span
        className={`text-sm text-[#F3F6FF] text-right break-all ${mono ? "font-mono text-xs" : ""}`}
      >
        {value ?? "-"}
      </span>
    </div>
  );
}

function UpgradeButton({ plan }) {
  const [loading, setLoading] = useState(false);
  const labels = {
    starter: "Starter $15/mo",
    growth: "Growth $20/mo",
    pro: "Pro $29/mo",
  };

  const handleClick = async () => {
    setLoading(true);
    try {
      const { data } = await api.post(`/api/billing/checkout?plan=${plan}`);
      window.location.href = data.checkoutUrl;
    } catch {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleClick}
      disabled={loading}
      className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-[#7C5CFF] text-[#F3F6FF] hover:bg-[#6848E8] transition-colors disabled:opacity-60"
    >
      {loading ? "Redirecting..." : labels[plan]}
    </button>
  );
}

function planFeatures(plan) {
  if (plan === "free") {
    return [
      "5 listing workflows per month",
      "2 Etsy listing audits per month",
      "Try Etsy and Shopify output",
      "Saved listing history",
      "Quality score cards",
      "Improvement tips",
    ];
  }
  if (plan === "starter") {
    return [
      "30 listing workflows per month",
      "10 Etsy listing audits per month",
      "Titles, descriptions, and tags",
      "Etsy or Shopify workflow",
      "Quality score before publishing",
      "Shipping warning for Etsy",
      "Saved recent listing history",
    ];
  }
  if (plan === "growth") {
    return [
      "100 listing workflows per month",
      "100 Etsy listing audits per month",
      "Generate Etsy and Shopify together",
      "Platform-specific SEO fields",
      "Quality score and prioritized fixes",
      "Deeper saved listing history",
      "Shop health score and improvement signals",
    ];
  }
  if (plan === "pro") {
    return [
      "Unlimited listing generation",
      "500 Etsy listing audits per month",
      "Bulk CSV workflow",
      "Shop health score and improvement signals",
      "Catalog refresh support",
      "Discovery optimization (coming soon)",
      "Multi-language (coming soon)",
    ];
  }
  return [];
}
