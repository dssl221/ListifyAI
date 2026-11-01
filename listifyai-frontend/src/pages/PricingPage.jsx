import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRightIcon, CheckIcon } from "@heroicons/react/24/outline";
import api from "../lib/api";
import { useAuth } from "../context/AuthContext";
import LogoMark from "../components/ui/LogoMark";

const PLANS = [
  {
    id: "free",
    name: "Explore",
    price: "$0",
    period: "",
    limit: "5 listings / month",
    description: "Evaluate the core workflow before moving to a paid plan.",
    features: [
      "Access Etsy and Shopify generation",
      "2 Etsy listing audits per month",
      "Saved listing history",
      "Quality scoring",
      "Guided improvement suggestions",
    ],
    cta: null,
    highlight: false,
  },
  {
    id: "starter",
    name: "Starter",
    price: "$15",
    period: "/mo",
    limit: "30 listings / month",
    description:
      "For sellers who publish consistently and need a dependable day-to-day listing workflow.",
    features: [
      "30 marketplace-ready listings each month",
      "10 Etsy listing audits per month",
      "Titles, descriptions, and tags",
      "Single-platform workflow per generation",
      "Quality score before publishing",
      "Etsy shipping visibility warnings",
      "Saved history for recent work",
    ],
    cta: "Choose Starter",
    highlight: false,
  },
  {
    id: "growth",
    name: "Growth",
    price: "$20",
    period: "/mo",
    limit: "100 listings / month",
    description:
      "For growing shops that publish across channels and need more output without duplicating effort.",
    features: [
      "100 optimized listings each month",
      "100 Etsy listing audits per month",
      "Generate Etsy and Shopify together",
      "Platform-specific SEO fields",
      "Quality score and prioritized recommendations",
      "Extended saved listing history",
      "Shop health score and portfolio signals",
    ],
    cta: "Choose Growth",
    highlight: true,
  },
  {
    id: "pro",
    name: "Pro",
    price: "$29",
    period: "/mo",
    limit: "Unlimited listings",
    description:
      "For high-volume teams managing launches, refresh cycles, and larger product catalogs.",
    features: [
      "Unlimited listing generation",
      "500 Etsy listing audits per month",
      "Bulk CSV workflow",
      "Shop health score and portfolio signals",
      "Catalog refresh support",
      "Discovery optimization (coming soon)",
      "Multi-language support (coming soon)",
    ],
    cta: "Choose Pro",
    highlight: false,
  },
];

const COMPARISON = [
  ["Monthly listing capacity", "5", "30", "100", "Unlimited"],
  ["Etsy listing audits / mo", "2", "10", "100", "500"],
  ["Listing quality score", "Included", "Included", "Included", "Included"],
  ["Shop Health Score", "-", "-", "Included", "Included"],
  ["Shipping warning", "Included", "Included", "Included", "Included"],
  ["Platform workflow", "Both", "Single platform per run", "Both", "Both"],
  ["Bulk CSV workflow", "-", "-", "-", "Included"],
  ["History saved", "Last 10", "Last 10", "Last 50", "Unlimited"],
];

export default function PricingPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(null);

  const handleUpgrade = async (planId) => {
    if (!user) {
      window.location.href = "/login";
      return;
    }
    setLoading(planId);
    try {
      const { data } = await api.post(`/api/billing/checkout?plan=${planId}`);
      window.location.href = data.checkoutUrl;
    } catch {
      setLoading(null);
    }
  };

  return (
    <div className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_18%_8%,rgba(124,92,255,0.18),transparent_32%),radial-gradient(circle_at_88%_0%,rgba(77,163,255,0.13),transparent_30%),linear-gradient(135deg,#0B1020_0%,#101729_48%,#0B1020_100%)] text-[#F3F6FF]">
      <header className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5">
        <Link to="/" className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <LogoMark size={24} />
            <span className="text-lg font-black tracking-tight text-[#F3F6FF]">
              Tagwing
            </span>
          </div>
        </Link>
        <Link
          to={user ? "/app" : "/login"}
          className="rounded-full bg-[#0B1020] px-4 py-2 text-sm font-bold text-[#F3F6FF] shadow-lg shadow-[#050816]/35 transition-transform hover:-translate-y-0.5"
        >
          {user ? "Dashboard" : "Sign in"}
        </Link>
      </header>

      <main className="px-6 pb-20 pt-10">
        <section className="mx-auto max-w-4xl text-center">
          <h1 className="mt-6 text-5xl font-black leading-tight tracking-tight text-[#F3F6FF] sm:text-6xl">
            Select the plan that matches your
            <span className="animated-gradient-text block">
              listing operation.
            </span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-[#94A3B8]">
            Start with core generation and audits, then scale into broader
            publishing capacity, both-platform output, and bulk catalog
            execution as your business expands.
          </p>
        </section>

        <section className="mx-auto mt-14 grid max-w-7xl grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {PLANS.map((plan, index) => (
            <div
              key={plan.id}
              className={`feature-card relative flex flex-col rounded-3xl border p-6 shadow-xl backdrop-blur ${
                plan.highlight
                  ? "border-[#4DA3FF]/70 bg-[#0B1020] text-[#F3F6FF] shadow-[#050816]/40"
                  : "border-white/10 bg-[#131A2A]/90 text-[#F3F6FF] shadow-[#050816]/32"
              }`}
              style={{ animationDelay: `${index * 100}ms` }}
            >
              {plan.highlight && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-[#4DA3FF] px-3 py-1 text-[10px] font-black uppercase tracking-widest text-[#F3F6FF] shadow-lg shadow-[#4DA3FF]/25">
                  Most popular
                </span>
              )}

              <p
                className={`text-sm font-black ${plan.highlight ? "text-[#D8D0FF]" : "text-[#A998FF]"}`}
              >
                {plan.name}
              </p>
              <p className="mt-4 text-5xl font-black tracking-tight">
                {plan.price}
                <span
                  className={`text-base font-bold ${plan.highlight ? "text-slate-500" : "text-slate-500"}`}
                >
                  {plan.period}
                </span>
              </p>
              <p
                className={`mt-2 text-sm font-bold ${plan.highlight ? "text-[#F3F6FF]" : "text-slate-300"}`}
              >
                {plan.limit}
              </p>
              <p
                className={`mt-3 text-sm leading-6 ${plan.highlight ? "text-slate-600" : "text-slate-400"}`}
              >
                {plan.description}
              </p>

              <ul className="mt-6 flex-1 space-y-3">
                {plan.features.map((feature) => {
                  const isSoon = feature.endsWith("(coming soon)");
                  return (
                    <li
                      key={feature}
                      className={`flex items-start gap-2 text-sm font-medium ${
                        plan.highlight
                          ? isSoon
                            ? "text-slate-600"
                            : "text-slate-100"
                          : isSoon
                            ? "text-slate-600"
                            : "text-[#94A3B8]"
                      }`}
                    >
                      <CheckIcon
                        className={`mt-0.5 h-4 w-4 flex-shrink-0 ${
                          isSoon
                            ? "text-slate-700"
                            : plan.highlight
                              ? "text-[#BFB5FF]"
                              : "text-[#7C5CFF]"
                        }`}
                      />
                      {feature}
                    </li>
                  );
                })}
              </ul>

              {plan.cta ? (
                <button
                  onClick={() => handleUpgrade(plan.id)}
                  disabled={loading === plan.id}
                  className={`group mt-7 flex w-full items-center justify-center gap-2 rounded-2xl py-3 text-sm font-black transition-all hover:-translate-y-0.5 disabled:opacity-60 ${
                    plan.highlight
                      ? "bg-[#131A2A] text-[#F3F6FF] hover:bg-[#1A2235]"
                      : "bg-[#0B1020] text-[#F3F6FF] hover:bg-[#6848E8]"
                  }`}
                >
                  {loading === plan.id ? "Redirecting..." : plan.cta}
                  <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                </button>
              ) : (
                <Link
                  to="/login"
                  className="mt-7 flex w-full items-center justify-center rounded-2xl border border-slate-700 bg-[#131A2A] py-3 text-sm font-black text-slate-200 transition-all hover:-translate-y-0.5 hover:shadow-lg hover:shadow-[#050816]/35"
                >
                  Start free
                </Link>
              )}
            </div>
          ))}
        </section>

        <section className="mx-auto mt-16 max-w-6xl overflow-hidden rounded-3xl border border-white/10 bg-[#131A2A]/88 shadow-xl shadow-[#050816]/32 backdrop-blur">
          <div className="grid grid-cols-[1.2fr_repeat(4,.7fr)] border-b border-white/10 bg-[#1A2235]/80 text-xs font-black uppercase tracking-wide text-slate-400">
            {["Feature", "Free", "Starter", "Growth", "Pro"].map((header) => (
              <div key={header} className="px-4 py-3">
                {header}
              </div>
            ))}
          </div>
          {COMPARISON.map((row, index) => (
            <div
              key={row[0]}
              className="spreadsheet-row grid grid-cols-[1.2fr_repeat(4,.7fr)] border-b border-white/10 text-sm last:border-b-0"
              style={{ animationDelay: `${index * 90}ms` }}
            >
              {row.map((cell, cellIndex) => (
                <div
                  key={`${row[0]}-${cellIndex}`}
                  className={`px-4 py-4 ${cellIndex === 0 ? "font-black text-[#F3F6FF]" : "font-semibold text-[#94A3B8]"}`}
                >
                  {cell}
                </div>
              ))}
            </div>
          ))}
        </section>
      </main>
    </div>
  );
}
